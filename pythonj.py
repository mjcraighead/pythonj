# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

import ast
from contextlib import contextmanager
from dataclasses import dataclass
from enum import Enum
import inspect
import itertools
import json
import types
from typing import Iterator, Optional, TextIO, cast

import extract_spec
import ir

# Keep this explicit and narrow; widen it only for builtin types we have actually
# exercised and are comfortable lowering directly to JVM instanceof checks.
ISINSTANCE_SINGLE_FASTPATH_BUILTIN_TYPES = {'bool', 'bytearray', 'bytes', 'float', 'int', 'str', 'tuple'}

RUNTIME_JAVA_FILES = (
    'PyBool.java',
    'PyBuiltinFunctions.java',
    'PyByteArray.java',
    'PyBytes.java',
    'PyDict.java',
    'PyEnumerate.java',
    'PyExceptions.java',
    'PyFile.java',
    'PyFloat.java',
    'PyInt.java',
    'PyList.java',
    'PyMappingProxy.java',
    'PyNone.java',
    'PyObject.java',
    'PyRange.java',
    'PyReversed.java',
    'PySet.java',
    'PySlice.java',
    'PyString.java',
    'PyTuple.java',
    'PyZip.java',
    'Runtime.java',
)

class ScopeKind(Enum):
    MODULE = 1
    FUNCTION = 2

class NameResolution(Enum):
    LOCAL = 1
    CELL = 2
    GLOBAL = 3

class NameBindingState(Enum):
    DEFINITELY_BOUND = 1
    DEFINITELY_UNBOUND = 2
    UNKNOWN = 3

NAME_RESOLUTION_ATTR = '_pythonj_resolution'
BINDING_STATE_ATTR = '_pythonj_binding_state'

def set_name_resolution(node: ast.Name, resolution: NameResolution) -> None:
    setattr(node, NAME_RESOLUTION_ATTR, resolution)

def get_name_resolution(node: ast.Name) -> NameResolution:
    return cast(NameResolution, getattr(node, NAME_RESOLUTION_ATTR))

def set_name_binding_state(node: ast.Name, state: NameBindingState) -> None:
    setattr(node, BINDING_STATE_ATTR, state)

def get_name_binding_state(node: ast.Name) -> NameBindingState:
    return cast(NameBindingState, getattr(node, BINDING_STATE_ATTR))

@dataclass(frozen=True)
class InitialBinding:
    name: str
    builtin_module: Optional[str] = None
    function_call_range: Optional[tuple[int, int]] = None
    function_return_type: Optional[str] = None

@dataclass
class ScopeInfo:
    kind: ScopeKind
    locals: set[str]
    explicit_globals: set[str]
    nonlocals: set[str]
    cell_vars: set[str]
    needs_from_outer: set[str]
    initial_final_bindings: set[str]
    initial_builtin_module_locals: dict[str, str]
    initial_final_function_call_ranges: dict[str, tuple[int, int]]
    initial_final_function_return_types: dict[str, str]
    exact_arg_types: dict[str, str]
    exact_local_types: dict[str, str]
    annotation_errors: list[tuple[int, str]]

@dataclass(frozen=True)
class WrapperBindingPlan:
    mode: str
    call_positional_shape: Optional['SignatureShape']
    exact_positional_arity: Optional[int] = None
    posonly_min_max_range: Optional[tuple[int, int]] = None

def _iter_scope_local_nodes(node: ast.AST) -> Iterator[ast.AST]:
    if isinstance(node, ast.Name):
        yield node # don't yield ctx (Load/Store/Del)
    elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
        yield node
        for decorator in node.decorator_list:
            yield from _iter_scope_local_nodes(decorator)
        for default in node.args.defaults:
            yield from _iter_scope_local_nodes(default)
        for default in node.args.kw_defaults:
            if default is not None:
                yield from _iter_scope_local_nodes(default)
        if node.returns is not None:
            yield from _iter_scope_local_nodes(node.returns)
    elif isinstance(node, ast.ClassDef):
        yield node
        for base in node.bases:
            yield from _iter_scope_local_nodes(base)
        for keyword in node.keywords:
            yield from _iter_scope_local_nodes(keyword.value)
        for decorator in node.decorator_list:
            yield from _iter_scope_local_nodes(decorator)
    elif isinstance(node, ast.Lambda):
        yield node
        for default in node.args.defaults:
            yield from _iter_scope_local_nodes(default)
        for default in node.args.kw_defaults:
            if default is not None:
                yield from _iter_scope_local_nodes(default)
    elif isinstance(node, (ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp)):
        yield node
        yield from _iter_scope_local_nodes(node.generators[0].iter)
    else:
        yield node
        for child in ast.iter_child_nodes(node):
            yield from _iter_scope_local_nodes(child)

def _walk_local(node: ast.AST, reads: set[str], writes: set[str], explicit_globals: set[str],
                nonlocals: set[str], children: list[ast.AST]) -> None:
    for local_node in _iter_scope_local_nodes(node):
        if isinstance(local_node, ast.Name):
            if isinstance(local_node.ctx, (ast.Store, ast.Del)):
                writes.add(local_node.id)
            else:
                reads.add(local_node.id)
        elif isinstance(local_node, ast.Global):
            explicit_globals.update(local_node.names)
        elif isinstance(local_node, ast.Nonlocal):
            nonlocals.update(local_node.names)
        elif isinstance(local_node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
            writes.add(local_node.name)
            children.append(local_node)
        elif isinstance(local_node, (ast.Lambda, ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp)):
            children.append(local_node)
        elif isinstance(local_node, ast.ExceptHandler):
            if local_node.name is not None:
                writes.add(local_node.name)
        elif isinstance(local_node, (ast.Import, ast.ImportFrom)):
            for alias in local_node.names:
                writes.add(alias.asname if alias.asname is not None else alias.name)

def _param_names(args: ast.arguments) -> Iterator[str]:
    for arg in itertools.chain(args.posonlyargs, args.args, args.kwonlyargs):
        yield arg.arg
    if args.vararg is not None:
        yield args.vararg.arg
    if args.kwarg is not None:
        yield args.kwarg.arg

def _decode_user_function_return_annotation(annotation: Optional[ast.expr]) -> Optional[str]:
    if annotation is None:
        return None
    if isinstance(annotation, ast.Name) and annotation.id in extract_spec.BUILTIN_TYPES:
        return annotation.id
    return None

def _get_supported_user_function_call_range(args: ast.arguments) -> Optional[tuple[int, int]]:
    if args.kwonlyargs or args.kw_defaults or args.vararg or args.kwarg:
        return None
    defaults: list[object] = []
    for default in args.defaults:
        if isinstance(default, ast.Constant) and is_constant_default(default.value):
            defaults.append(default.value)
        elif isinstance(default, ast.Tuple):
            try:
                value = ast.literal_eval(default)
            except (TypeError, ValueError):
                return None
            if not is_constant_default(value):
                return None
            defaults.append(value)
        else:
            return None
    n_total = len(args.posonlyargs) + len(args.args)
    n_required = n_total - len(defaults)
    return (n_required, n_total)

def _get_initial_bindings(node: ast.stmt) -> Optional[list[InitialBinding]]:
    if isinstance(node, ast.Import):
        bindings: list[InitialBinding] = []
        for alias in node.names:
            if alias.name not in extract_spec.BUILTIN_MODULES or '.' in alias.name:
                return None
            bind_name = alias.asname if alias.asname is not None else alias.name
            bindings.append(InitialBinding(bind_name, builtin_module=alias.name))
        return bindings
    if isinstance(node, ast.FunctionDef):
        if node.decorator_list:
            return None
        function_call_range = _get_supported_user_function_call_range(node.args)
        if function_call_range is None:
            return None
        return [InitialBinding(
            node.name,
            function_call_range=function_call_range,
            function_return_type=_decode_user_function_return_annotation(node.returns),
        )]
    return None

def _collect_initial_final_bindings(node: ast.AST) -> tuple[set[str], dict[str, str], dict[str, tuple[int, int]], dict[str, str]]:
    if not isinstance(node, (ast.Module, ast.FunctionDef, ast.AsyncFunctionDef)):
        return (set(), {}, {}, {})
    prefix_bindings: dict[str, InitialBinding] = {}
    prefix_end = 0
    for statement in node.body:
        bindings = _get_initial_bindings(statement)
        if bindings is None:
            break
        prefix_end += 1
        for binding in bindings:
            prefix_bindings[binding.name] = binding
    if not prefix_bindings:
        return (set(), {}, {}, {})
    later_writes: set[str] = set()
    for statement in node.body[prefix_end:]:
        _walk_local(statement, set(), later_writes, set(), set(), [])
    final_bindings = {name for name in prefix_bindings if name not in later_writes}
    return (
        final_bindings,
        {
            name: binding.builtin_module
            for (name, binding) in prefix_bindings.items()
            if name in final_bindings and binding.builtin_module is not None
        },
        {
            name: binding.function_call_range
            for (name, binding) in prefix_bindings.items()
            if name in final_bindings and binding.function_call_range is not None
        },
        {
            name: binding.function_return_type
            for (name, binding) in prefix_bindings.items()
            if name in final_bindings and binding.function_return_type is not None
        },
    )

def _collect_child_scopes(node: ast.AST) -> list[ast.AST]:
    reads: set[str] = set()
    writes: set[str] = set()
    explicit_globals: set[str] = set()
    nonlocals: set[str] = set()
    children: list[ast.AST] = []
    if isinstance(node, ast.Module):
        for statement in node.body:
            _walk_local(statement, reads, writes, explicit_globals, nonlocals, children)
    elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
        for statement in node.body:
            _walk_local(statement, reads, writes, explicit_globals, nonlocals, children)
    elif isinstance(node, ast.ClassDef):
        for statement in node.body:
            _walk_local(statement, reads, writes, explicit_globals, nonlocals, children)
    elif isinstance(node, ast.Lambda):
        _walk_local(node.body, reads, writes, explicit_globals, nonlocals, children)
    else:
        assert isinstance(node, (ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp)), node
        generators = node.generators
        elts: list[ast.expr] = [node.elt] if not isinstance(node, ast.DictComp) else [node.key, node.value]
        for (i, generator) in enumerate(generators):
            if i != 0:
                _walk_local(generator.iter, reads, writes, explicit_globals, nonlocals, children)
            _walk_local(generator.target, reads, writes, explicit_globals, nonlocals, children)
            for _if in generator.ifs:
                _walk_local(_if, reads, writes, explicit_globals, nonlocals, children)
        for elt in elts:
            _walk_local(elt, reads, writes, explicit_globals, nonlocals, children)
    return children

def _resolve_name(scope_stack: list[ScopeInfo], name: str) -> NameResolution:
    current = scope_stack[-1]
    if current.kind is ScopeKind.MODULE:
        return NameResolution.GLOBAL
    if name in current.explicit_globals:
        return NameResolution.GLOBAL
    if name in current.locals:
        if name in current.cell_vars:
            return NameResolution.CELL
        return NameResolution.LOCAL
    for parent in reversed(scope_stack[:-1]):
        if parent.kind is ScopeKind.FUNCTION and name in (parent.locals | parent.cell_vars):
            return NameResolution.CELL
    return NameResolution.GLOBAL

def _collect_statement_store_del_names(node: ast.stmt) -> tuple[set[str], set[str]]:
    stores: set[str] = set()
    dels: set[str] = set()
    for local_node in _iter_scope_local_nodes(node):
        if isinstance(local_node, ast.Name):
            if isinstance(local_node.ctx, ast.Store):
                stores.add(local_node.id)
            elif isinstance(local_node.ctx, ast.Del):
                dels.add(local_node.id)
    if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
        stores.add(node.name)
    if isinstance(node, ast.ExceptHandler) and node.name is not None:
        stores.add(node.name)
    return (stores, dels)

def _collect_exact_local_annotations(node: ast.AST) -> tuple[dict[str, str], list[tuple[int, str]]]:
    if isinstance(node, ast.Module):
        body = node.body
        arg_names: set[str] = set()
    elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
        body = node.body
        arg_names = set(_param_names(node.args))
    elif isinstance(node, ast.ClassDef):
        body = node.body
        arg_names = set()
    else:
        return ({}, [])

    exact_local_types: dict[str, str] = {}
    errors: list[tuple[int, str]] = []
    for statement in body:
        for local_node in _iter_scope_local_nodes(statement):
            if not isinstance(local_node, ast.AnnAssign):
                continue
            if not isinstance(local_node.target, ast.Name):
                errors.append((local_node.lineno, 'only simple local variable annotations are supported'))
                continue
            if local_node.target.id in arg_names:
                errors.append((local_node.lineno, f"local annotation for {local_node.target.id!r} collides with an argument name"))
                continue
            if local_node.target.id in exact_local_types:
                errors.append((local_node.lineno, f"duplicate local annotation for {local_node.target.id!r}"))
                continue
            if not isinstance(local_node.annotation, ast.Name) or local_node.annotation.id not in extract_spec.BUILTIN_TYPES:
                errors.append((local_node.lineno, 'only exact builtin-type local annotations are supported'))
                continue
            exact_local_types[local_node.target.id] = local_node.annotation.id
    return (exact_local_types, errors)

def _collect_exact_arg_annotations(node: ast.AST) -> tuple[dict[str, str], list[tuple[int, str]]]:
    if not isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
        return ({}, [])

    exact_arg_types: dict[str, str] = {}
    errors: list[tuple[int, str]] = []
    positional_args = [*node.args.posonlyargs, *node.args.args]
    first_default = len(positional_args) - len(node.args.defaults)
    for (i, arg) in enumerate(positional_args):
        if arg.annotation is None:
            continue
        if i >= first_default:
            errors.append((arg.lineno, 'annotated arguments with defaults are unsupported'))
            continue
        if not isinstance(arg.annotation, ast.Name) or arg.annotation.id not in extract_spec.BUILTIN_TYPES:
            errors.append((arg.lineno, 'only exact builtin-type argument annotations are supported'))
            continue
        exact_arg_types[arg.arg] = arg.annotation.id
    for arg in [*node.args.kwonlyargs, node.args.vararg, node.args.kwarg]:
        if arg is not None and arg.annotation is not None:
            errors.append((arg.lineno, 'only required positional argument annotations are supported'))
    return (exact_arg_types, errors)

class ScopeAnalyzer(ast.NodeVisitor):
    __slots__ = ('scope_infos',)
    scope_infos: dict[ast.AST, ScopeInfo]

    def __init__(self):
        self.scope_infos = {}

    def _analyze_scope_node(self, node: ast.AST) -> ScopeInfo:
        if node in self.scope_infos:
            return self.scope_infos[node]

        reads: set[str] = set()
        writes: set[str] = set()
        explicit_globals: set[str] = set()
        nonlocals: set[str] = set()
        children: list[ast.AST] = []

        if isinstance(node, ast.Module):
            for statement in node.body:
                _walk_local(statement, reads, writes, explicit_globals, nonlocals, children)
        elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            writes.update(_param_names(node.args))
            for statement in node.body:
                _walk_local(statement, reads, writes, explicit_globals, nonlocals, children)
        elif isinstance(node, ast.ClassDef):
            for statement in node.body:
                _walk_local(statement, reads, writes, explicit_globals, nonlocals, children)
        elif isinstance(node, ast.Lambda):
            writes.update(_param_names(node.args))
            _walk_local(node.body, reads, writes, explicit_globals, nonlocals, children)
        else:
            assert isinstance(node, (ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp)), node
            generators = node.generators
            elts: list[ast.expr] = [node.elt] if not isinstance(node, ast.DictComp) else [node.key, node.value]
            for (i, generator) in enumerate(generators):
                if i != 0:
                    _walk_local(generator.iter, reads, writes, explicit_globals, nonlocals, children)
                _walk_local(generator.target, reads, writes, explicit_globals, nonlocals, children)
                for _if in generator.ifs:
                    _walk_local(_if, reads, writes, explicit_globals, nonlocals, children)
            for elt in elts:
                _walk_local(elt, reads, writes, explicit_globals, nonlocals, children)

        locals = writes - explicit_globals - nonlocals
        needs_from_outer = (reads | nonlocals) - locals - explicit_globals
        cell_vars = set()
        for child in children:
            child_info = self._analyze_scope_node(child)
            for name in child_info.needs_from_outer:
                if name in locals:
                    cell_vars.add(name)
                elif name not in explicit_globals:
                    needs_from_outer.add(name)
        kind = ScopeKind.MODULE if isinstance(node, ast.Module) else ScopeKind.FUNCTION
        (exact_arg_types, annotation_errors) = _collect_exact_arg_annotations(node)
        (exact_local_types, local_annotation_errors) = _collect_exact_local_annotations(node)
        annotation_errors.extend(local_annotation_errors)
        for name in exact_arg_types:
            if name in cell_vars:
                annotation_errors.append((getattr(node, 'lineno', 0), f"annotated argument {name!r} may not be captured by an inner scope"))
        for name in exact_local_types:
            if name in cell_vars:
                annotation_errors.append((getattr(node, 'lineno', 0), f"annotated local {name!r} may not be captured by an inner scope"))
        (initial_final_bindings,
         initial_builtin_module_locals,
         initial_final_function_call_ranges,
         initial_final_function_return_types) = _collect_initial_final_bindings(node)
        scope_info = ScopeInfo(
            kind,
            locals,
            explicit_globals,
            nonlocals,
            cell_vars,
            needs_from_outer,
            initial_final_bindings,
            initial_builtin_module_locals,
            initial_final_function_call_ranges,
            initial_final_function_return_types,
            exact_arg_types,
            exact_local_types,
            annotation_errors,
        )
        self.scope_infos[node] = scope_info
        return scope_info

    def _analyze_scope_tree(self, node: ast.AST) -> None:
        self._analyze_scope_node(node)
        for child in _collect_child_scopes(node):
            self._analyze_scope_tree(child)

    def _tag_load_binding_states(self, node: ast.AST, scope_info: ScopeInfo) -> None:
        if isinstance(node, ast.Module):
            ordered_nodes: list[ast.AST] = list(node.body)
        elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
            ordered_nodes = list(node.body)
        elif isinstance(node, ast.Lambda):
            ordered_nodes = [node.body]
        else:
            assert isinstance(node, (ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp)), node
            ordered_nodes = []
            for generator in node.generators:
                ordered_nodes.append(generator.target)
                for _if in generator.ifs:
                    ordered_nodes.append(_if)
            if isinstance(node, ast.DictComp):
                ordered_nodes.extend([node.key, node.value])
            else:
                ordered_nodes.append(node.elt)

        states: dict[str, NameBindingState] = {}
        for name in scope_info.locals | scope_info.cell_vars | scope_info.explicit_globals:
            states[name] = NameBindingState.DEFINITELY_UNBOUND
        default_global_state = NameBindingState.DEFINITELY_UNBOUND
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.Lambda)):
            for name in _param_names(node.args):
                states[name] = NameBindingState.DEFINITELY_BOUND

        for statement in ordered_nodes:
            for local_node in _iter_scope_local_nodes(statement):
                if isinstance(local_node, ast.Name) and isinstance(local_node.ctx, ast.Load):
                    resolution = get_name_resolution(local_node)
                    if resolution is NameResolution.GLOBAL:
                        state = states.get(local_node.id, default_global_state)
                    else:
                        state = states.get(local_node.id, NameBindingState.DEFINITELY_UNBOUND)
                    set_name_binding_state(local_node, state)
                elif local_node is not statement and isinstance(local_node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef, ast.Lambda,
                                                                            ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp)):
                    self._tag_load_binding_states(local_node, self.scope_infos[local_node])
            if isinstance(statement, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef, ast.Lambda,
                                      ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp)):
                self._tag_load_binding_states(statement, self.scope_infos[statement])

            if isinstance(statement, ast.stmt):
                (stores, dels) = _collect_statement_store_del_names(statement)
                for name in stores:
                    states[name] = NameBindingState.DEFINITELY_BOUND
                for name in dels:
                    states[name] = NameBindingState.DEFINITELY_UNBOUND

    def _tag_names(self, node: ast.AST, scope_stack: list[ScopeInfo]) -> None:
        for subnode in _iter_scope_local_nodes(node):
            if isinstance(subnode, ast.Name):
                set_name_resolution(subnode, _resolve_name(scope_stack, subnode.id))
            elif subnode is not node and isinstance(subnode, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef, ast.Lambda,
                                                             ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp)):
                self._tag_names(subnode, [*scope_stack, self.scope_infos[subnode]])
        if isinstance(node, ast.Module):
            for statement in node.body:
                self._tag_names(statement, scope_stack)
        elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
            child_scope_stack = [*scope_stack, self.scope_infos[node]]
            for statement in node.body:
                self._tag_names(statement, child_scope_stack)
        elif isinstance(node, ast.Lambda):
            self._tag_names(node.body, [*scope_stack, self.scope_infos[node]])
        elif isinstance(node, (ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp)):
            child_scope_stack = [*scope_stack, self.scope_infos[node]]
            generators = node.generators
            for generator in generators:
                self._tag_names(generator.target, child_scope_stack)
                if generator is not generators[0]:
                    self._tag_names(generator.iter, child_scope_stack)
                for _if in generator.ifs:
                    self._tag_names(_if, child_scope_stack)
            if isinstance(node, ast.DictComp):
                self._tag_names(node.key, child_scope_stack)
                self._tag_names(node.value, child_scope_stack)
            else:
                self._tag_names(node.elt, child_scope_stack)

    def visit_Module(self, node) -> None:
        self._analyze_scope_tree(node)
        self._tag_names(node, [self.scope_infos[node]])
        self._tag_load_binding_states(node, self.scope_infos[node])

class Scope:
    __slots__ = ('parent', 'info', 'qualname', 'free_vars', 'locals_are_fields', 'n_temps', 'used_expr_discard',
                 'reported_annotation_errors',
                 'expected_return_java_type')
    parent: Optional['Scope']
    info: ScopeInfo
    qualname: Optional[str]
    free_vars: set[str]
    locals_are_fields: bool
    n_temps: int
    used_expr_discard: bool
    reported_annotation_errors: bool
    expected_return_java_type: Optional[str]

    def __init__(self, parent: Optional['Scope'], info: ScopeInfo, qualname: Optional[str] = None,
                 expected_return_java_type: Optional[str] = None):
        self.parent = parent
        self.info = info
        self.qualname = qualname
        self.free_vars = set()
        self.locals_are_fields = False
        self.n_temps = 0
        self.used_expr_discard = False
        self.reported_annotation_errors = False
        self.expected_return_java_type = expected_return_java_type

    def make_temp(self) -> str:
        """Assign and return a new temporary variable name."""
        temp_name = f'temp{self.n_temps}'
        self.n_temps += 1
        return temp_name

# XXX Need to design a systematic way to avoid "code too large" and "too many constants" errors.
# This is somewhat challenging, as even a single Python expression or statement can easily overflow
# the maximum code size limit, and it is somewhat unpredictable how much bytecode our translations
# will compile into.  Partial mitigations are likely to be easier than a total fix.
# XXX "invokedynamic" might help us a lot, but there is no way to access it from Java source
class LoweringVisitor(ast.NodeVisitor):
    __slots__ = ('path', 'n_errors', 'n_functions', 'n_lambdas', 'scope', 'global_code', 'code',
                 'scope_infos', 'pool', 'break_name', 'classes', 'allow_intrinsics',
                 'python_helper_names', 'python_helper_class', 'final_global_function_classes')
    path: str
    n_errors: int
    n_functions: int
    n_lambdas: int
    scope: Scope
    global_code: list[ir.Statement]
    code: list[ir.Statement]
    scope_infos: dict[ast.AST, ScopeInfo]
    pool: ir.ConstantPool
    break_name: Optional[str]
    classes: dict[str, ir.ClassDecl]
    allow_intrinsics: bool # enables special internal-only codegen features for builtins
    python_helper_names: set[str]
    python_helper_class: Optional[str]
    final_global_function_classes: dict[str, str]

    def __init__(self, path: str, scope_infos: dict[ast.AST, ScopeInfo], module_info: ScopeInfo):
        self.path = path
        self.n_errors = 0
        self.n_functions = 0
        self.n_lambdas = 0
        self.scope = Scope(None, module_info)
        self.global_code = [ir.LocalDecl('PyObject', 'expr_discard', None)] # XXX remove this variable if not needed
        self.code = self.global_code
        self.scope_infos = scope_infos
        self.pool = ir.ConstantPool()
        self.break_name = None
        self.classes = {}
        self.allow_intrinsics = False
        self.python_helper_names = set()
        self.python_helper_class = None
        self.final_global_function_classes = {}

    # Note: if n_errors > 0, the generated Java code is not expected to be valid or executable.
    def error(self, lineno: Optional[int], msg: str) -> None:
        if lineno is None:
            print(f'ERROR: {self.path}: {msg}')
        else:
            print(f'ERROR: {self.path}:{lineno}: {msg}')
        self.n_errors += 1

    def ident_expr_by_resolution(self, name: str, resolution: NameResolution) -> ir.Expr:
        if self.allow_intrinsics and name == '__pythonj_null__':
            return ir.Null()
        if resolution is NameResolution.CELL:
            return ir.Field(ir.Identifier(f'pycell_{name}'), 'obj')
        if resolution is NameResolution.LOCAL:
            if self.scope.locals_are_fields:
                return ir.Field(ir.This(), f'pylocal_{name}')
            return ir.Identifier(f'pylocal_{name}')
        if name in extract_spec.BUILTIN_TYPES:
            return ir.Field(ir.Identifier(f'{extract_spec.BUILTIN_TYPES[name]}Type'), 'singleton')
        if name in extract_spec.EXCEPTION_TYPES:
            return ir.Field(ir.Identifier(f'Py{name}Type'), 'singleton')
        if name in extract_spec.BUILTIN_FUNCTIONS:
            return ir.Field(ir.Identifier(f'PyBuiltinFunction_{name}'), 'singleton')
        return ir.Identifier(f'pyglobal_{name}')

    def ident_expr(self, name: str) -> ir.Expr:
        if self.scope.info.kind is ScopeKind.FUNCTION and (name in self.scope.info.cell_vars or name in self.scope.free_vars):
            return self.ident_expr_by_resolution(name, NameResolution.CELL)
        if self.scope.info.kind is ScopeKind.FUNCTION and name in self.scope.info.locals:
            return self.ident_expr_by_resolution(name, NameResolution.LOCAL)
        return self.ident_expr_by_resolution(name, NameResolution.GLOBAL)

    def module_scope(self) -> Scope:
        scope = self.scope
        while scope.parent is not None:
            scope = scope.parent
        return scope

    def module_binds_name(self, name: str) -> bool:
        return name in self.module_scope().info.locals

    def name_resolves_to_builtin_function(self, node: ast.Name) -> bool:
        if node.id not in extract_spec.BUILTIN_FUNCTIONS:
            return False
        if self.allow_intrinsics:
            return True
        if get_name_resolution(node) is not NameResolution.GLOBAL:
            return False
        return get_name_binding_state(node) is NameBindingState.DEFINITELY_UNBOUND

    def name_resolves_to_builtin_type(self, node: ast.Name) -> bool:
        if node.id not in extract_spec.BUILTIN_TYPES:
            return False
        if self.allow_intrinsics:
            return True
        if get_name_resolution(node) is not NameResolution.GLOBAL:
            return False
        if hasattr(node, BINDING_STATE_ATTR):
            return get_name_binding_state(node) is NameBindingState.DEFINITELY_UNBOUND
        return not self.module_binds_name(node.id)

    def name_resolves_to_final_top_level_function(self, node: ast.Name) -> bool:
        if node.id not in self.module_scope().info.initial_final_function_call_ranges:
            return False
        if node.id not in self.final_global_function_classes:
            return False
        return get_name_resolution(node) is NameResolution.GLOBAL

    def final_top_level_function_expr(self, name: str) -> ir.Expr:
        assert name in self.final_global_function_classes, name
        return self.ident_expr(name)

    def builtin_module_name_for_name(self, node: ast.Name) -> Optional[str]:
        resolution = get_name_resolution(node)
        if resolution is NameResolution.LOCAL:
            return self.scope.info.initial_builtin_module_locals.get(node.id)
        if resolution is NameResolution.GLOBAL:
            return self.module_scope().info.initial_builtin_module_locals.get(node.id)
        return None

    def report_annotation_errors(self) -> None:
        if self.scope.reported_annotation_errors:
            return
        for (lineno, msg) in self.scope.info.annotation_errors:
            self.error(lineno, msg)
        self.scope.reported_annotation_errors = True

    def local_exact_builtin_type(self, name: str) -> Optional[str]:
        type_name = self.scope.info.exact_local_types.get(name)
        if type_name is not None:
            return type_name
        return self.scope.info.exact_arg_types.get(name)

    def java_local_type(self, name: str) -> str:
        type_name = self.local_exact_builtin_type(name)
        if type_name is None:
            return 'PyObject'
        return extract_spec.BUILTIN_TYPES[type_name]

    def cast_local_assignment(self, name: str, value: ir.Expr) -> ir.Expr:
        type_name = self.local_exact_builtin_type(name)
        if type_name is None:
            return value
        return ir.CastExpr(extract_spec.BUILTIN_TYPES[type_name], value)

    def emit_exact_int_value(self, node: ast.expr) -> Optional[ir.Expr]:
        if self.infer_exact_builtin_type_expr(node) != 'int':
            return None
        if isinstance(node, ast.Constant) and isinstance(node.value, int) and not isinstance(node.value, bool):
            return ir.IntLiteral(node.value, 'L')
        expr = self.visit(node)
        if isinstance(node, ast.Name) and get_name_resolution(node) is NameResolution.LOCAL:
            return ir.Field(expr, 'value')
        return ir.Field(ir.CastExpr('PyInt', expr), 'value')

    def emit_exact_int_binop(self, op: str, lhs_node: ast.expr, rhs_node: ast.expr) -> Optional[ir.Expr]:
        lhs_int = self.emit_exact_int_value(lhs_node)
        rhs_int = self.emit_exact_int_value(rhs_node)
        if lhs_int is None or rhs_int is None:
            return None
        if op not in {'add', 'and', 'floorDiv', 'lshift', 'mod', 'mul', 'or', 'pow', 'rshift', 'sub', 'xor'}:
            return None
        return ir.MethodCall(ir.Identifier('PyInt'), op, [lhs_int, rhs_int])

    def infer_exact_builtin_type_expr(self, node: ast.expr) -> Optional[str]:
        if isinstance(node, ast.Name) and get_name_resolution(node) is NameResolution.LOCAL:
            type_name = self.local_exact_builtin_type(node.id)
            if type_name is not None:
                return type_name
        if isinstance(node, ast.BinOp):
            op = self.visit(node.op)
            if (op in {'add', 'and', 'floorDiv', 'lshift', 'mod', 'mul', 'or', 'pow', 'rshift', 'sub', 'xor'} and
                self.infer_exact_builtin_type_expr(node.left) == 'int' and
                self.infer_exact_builtin_type_expr(node.right) == 'int'):
                return 'int'
        if isinstance(node, ast.Call) and isinstance(node.func, ast.Name):
            if self.name_resolves_to_final_top_level_function(node.func):
                type_name = self.module_scope().info.initial_final_function_return_types.get(node.func.id)
                if type_name is not None and not node.keywords and not any(isinstance(arg, ast.Starred) for arg in node.args):
                    return type_name
        if (isinstance(node, ast.Call) and
            isinstance(node.func, ast.Name) and
            node.func.id in DIRECT_NEWOBJ_POSITIONAL_BUILTIN_TYPES and
            self.name_resolves_to_builtin_type(node.func) and
            not node.keywords and
            not any(isinstance(arg, ast.Starred) for arg in node.args)):
            (min_args, max_args) = DIRECT_NEWOBJ_POSITIONAL_BUILTIN_TYPES[node.func.id]
            if min_args <= len(node.args) <= max_args:
                return node.func.id
        return infer_exact_builtin_type(node)

    def resolve_free_vars(self, lineno: int, scope_info: ScopeInfo, func_type: str) -> set[str]:
        free_vars = set()
        for name in scope_info.needs_from_outer:
            parent_scope = self.scope
            found = False
            while parent_scope:
                if name in parent_scope.info.explicit_globals:
                    break
                if parent_scope.info.kind is ScopeKind.FUNCTION and name in (parent_scope.info.locals | parent_scope.info.cell_vars | parent_scope.free_vars):
                    free_vars.add(name)
                    found = True
                    break
                parent_scope = parent_scope.parent
            if name in scope_info.nonlocals and not found:
                self.error(lineno, f"no binding for nonlocal {name!r} found")
        return free_vars

    def generic_visit(self, node):
        """Print an error for all unknown constructs in translation."""
        self.error(getattr(node, 'lineno', None), f'unsupported Python construct: {type(node).__name__}')
        if isinstance(node, ast.expr):
            return ir.Identifier('__cannot_translate_expr__') # return placeholder ir.Expr

    def visit_Invert(self, node): return 'invert'
    def visit_UAdd(self, node): return 'pos'
    def visit_USub(self, node): return 'neg'
    def visit_UnaryOp(self, node) -> ir.Expr:
        if isinstance(node.op, ast.Not):
            return ir.MethodCall(ir.Identifier('PyBool'), 'create', [ir.unary_op('!', self.emit_condition(node.operand))])
        op = self.visit(node.op)
        operand = self.visit(node.operand)
        if isinstance(operand, ir.PyConstant) and isinstance(operand.value, int):
            match op:
                case 'pos': return ir.PyConstant(+operand.value)
                case 'neg': return ir.PyConstant(-operand.value)
        return ir.MethodCall(operand, op, [])

    def visit_Add(self, node): return 'add'
    def visit_BitAnd(self, node): return 'and'
    def visit_BitOr(self, node): return 'or'
    def visit_BitXor(self, node): return 'xor'
    def visit_Div(self, node): return 'trueDiv'
    def visit_FloorDiv(self, node): return 'floorDiv'
    def visit_LShift(self, node): return 'lshift'
    def visit_MatMult(self, node): return 'matmul'
    def visit_Mod(self, node): return 'mod'
    def visit_Mult(self, node): return 'mul'
    def visit_Pow(self, node): return 'pow'
    def visit_RShift(self, node): return 'rshift'
    def visit_Sub(self, node): return 'sub'
    def visit_BinOp(self, node) -> ir.Expr:
        op = self.visit(node.op)
        exact_int_expr = self.emit_exact_int_binop(op, node.left, node.right)
        if exact_int_expr is not None:
            return exact_int_expr
        lhs = self.visit(node.left)
        rhs = self.visit(node.right)
        if (isinstance(lhs, ir.PyConstant) and isinstance(lhs.value, int) and
            isinstance(rhs, ir.PyConstant) and isinstance(rhs.value, int)):
            match op: # be careful to not raise an exception here or do anything platform-dependent
                case 'add':
                    return ir.PyConstant(lhs.value + rhs.value)
                case 'and':
                    return ir.PyConstant(lhs.value & rhs.value)
                case 'or':
                    return ir.PyConstant(lhs.value | rhs.value)
                case 'xor':
                    return ir.PyConstant(lhs.value ^ rhs.value)
                case 'lshift' if rhs.value >= 0:
                    return ir.PyConstant(lhs.value << rhs.value)
                case 'mul':
                    return ir.PyConstant(lhs.value * rhs.value)
                case 'rshift' if rhs.value >= 0:
                    return ir.PyConstant(lhs.value >> rhs.value)
                case 'sub':
                    return ir.PyConstant(lhs.value - rhs.value)
        return ir.MethodCall(lhs, op, [rhs])

    def visit_Lt(self, node): return 'lt'
    def visit_LtE(self, node): return 'le'
    def visit_Gt(self, node): return 'gt'
    def visit_GtE(self, node): return 'ge'
    def visit_Compare(self, node) -> ir.Expr:
        n_compares = len(node.comparators)
        assert n_compares == len(node.ops), node # should have consistent number of these
        assert n_compares >= 1, node # should always have at least one
        if n_compares == 1 and isinstance(node.ops[0], (ast.Lt, ast.LtE, ast.Gt, ast.GtE, ast.Eq, ast.NotEq)):
            lhs_int = self.emit_exact_int_value(node.left)
            rhs_int = self.emit_exact_int_value(node.comparators[0])
            if lhs_int is not None and rhs_int is not None:
                op = {
                    ast.Lt: '<',
                    ast.LtE: '<=',
                    ast.Gt: '>',
                    ast.GtE: '>=',
                    ast.Eq: '==',
                    ast.NotEq: '!=',
                }[type(node.ops[0])]
                return ir.MethodCall(ir.Identifier('PyBool'), 'create', [ir.BinaryOp(op, lhs_int, rhs_int)])

        lhs = self.visit(node.left)
        exprs: list[ir.Expr] = []
        for (i, (op, comparator)) in enumerate(zip(node.ops, node.comparators)):
            rhs = self.visit(comparator)
            if i < n_compares - 1:
                temp_name = self.scope.make_temp()
                self.code.append(ir.LocalDecl('PyObject', temp_name, None))
                rhs = ir.AssignExpr(ir.Identifier(temp_name), rhs)
            else:
                temp_name = '__unused__'
            if isinstance(op, ast.Is):
                term = ir.BinaryOp('==', lhs, rhs)
            elif isinstance(op, ast.IsNot):
                term = ir.BinaryOp('!=', lhs, rhs)
            elif isinstance(op, ast.In):
                term = ir.MethodCall(lhs, 'in', [rhs])
            elif isinstance(op, ast.NotIn):
                term = ir.unary_op('!', ir.MethodCall(lhs, 'in', [rhs]))
            elif isinstance(op, ast.Eq):
                term = ir.MethodCall(lhs, 'equals', [rhs])
            elif isinstance(op, ast.NotEq):
                term = ir.unary_op('!', ir.MethodCall(lhs, 'equals', [rhs]))
            else:
                term = ir.MethodCall(lhs, self.visit(op), [rhs])
            exprs.append(term)
            if i < n_compares - 1:
                lhs = ir.Identifier(temp_name)
        return ir.MethodCall(ir.Identifier('PyBool'), 'create', [ir.chained_binary_op('&&', exprs)])

    def emit_bool_op(self, op: ast.boolop, values: list[ast.expr]) -> ir.Expr:
        if len(values) == 1:
            return self.visit(values[0])
        temp_name = self.scope.make_temp()
        self.code.append(ir.LocalDecl('PyObject', temp_name, None))
        lhs = self.visit(values[0])
        rhs = self.emit_bool_op(op, values[1:])
        if isinstance(op, ast.And):
            return ir.CondOp(ir.bool_value(ir.AssignExpr(ir.Identifier(temp_name), lhs)), rhs, ir.Identifier(temp_name))
        else:
            assert isinstance(op, ast.Or), op
            return ir.CondOp(ir.bool_value(ir.AssignExpr(ir.Identifier(temp_name), lhs)), ir.Identifier(temp_name), rhs)

    def visit_BoolOp(self, node) -> ir.Expr:
        assert len(node.values) >= 2, node
        return self.emit_bool_op(node.op, node.values)

    def visit_IfExp(self, node) -> ir.Expr:
        return ir.CondOp(self.emit_condition(node.test), self.visit(node.body), self.visit(node.orelse))

    def emit_condition(self, node: ast.expr) -> ir.Expr:
        if isinstance(node, ast.BoolOp):
            op = '&&' if isinstance(node.op, ast.And) else '||'
            return ir.chained_binary_op(op, [self.emit_condition(value) for value in node.values])
        return ir.bool_value(self.visit(node))

    def emit_isinstance_condition(self, obj: ast.expr, class_or_tuple: ast.expr) -> ir.Expr:
        obj_expr = self.visit(obj)
        if isinstance(class_or_tuple, ast.Name) and class_or_tuple.id in ISINSTANCE_SINGLE_FASTPATH_BUILTIN_TYPES:
            builtin_type = class_or_tuple.id
            assert builtin_type in extract_spec.BUILTIN_TYPES, builtin_type
            return ir.MethodCall(ir.Identifier('PyBool'), 'create', [
                ir.BinaryOp('instanceof', obj_expr, ir.Identifier(extract_spec.BUILTIN_TYPES[builtin_type]))
            ])
        if isinstance(class_or_tuple, ast.Tuple):
            if not class_or_tuple.elts:
                return ir.PyConstant(False)
            return ir.MethodCall(ir.Identifier('PyBool'), 'create', [
                ir.chained_binary_op('||', [ir.bool_value(self.emit_isinstance_condition(obj, elt)) for elt in class_or_tuple.elts])
            ])
        return ir.MethodCall(ir.Identifier('Runtime'), 'pythonjIsInstance', [obj_expr, self.visit(class_or_tuple)])

    def visit_Constant(self, node) -> ir.Expr:
        if isinstance(node.value, (types.NoneType, bool, int, float, str, bytes)):
            return ir.PyConstant(node.value)
        else:
            self.error(node.lineno, f'literal {node.value!r} of type {type(node.value).__name__!r} is unsupported')
            return ir.Identifier('__cannot_translate_constant__')

    def visit_JoinedStr(self, node) -> ir.Expr:
        if not node.values:
            return ir.PyConstant('')
        vals: list[ir.Expr] = []
        for val in node.values:
            if isinstance(val, ast.Constant):
                assert isinstance(val.value, str), val
                vals.append(ir.StrLiteral(val.value))
            else:
                assert isinstance(val, ast.FormattedValue), val
                # XXX Need to double check evaluation order here
                if val.format_spec is not None:
                    # Need to extract the String back out of the PyString
                    assert isinstance(val.format_spec, ast.JoinedStr), val.format_spec
                    expr = self.visit(val.format_spec)
                    if isinstance(expr, ir.PyConstant) and isinstance(expr.value, str):
                        format_spec = ir.StrLiteral(expr.value)
                    else:
                        format_spec = ir.Field(expr, 'value')
                else:
                    format_spec = ir.StrLiteral("")
                expr = self.visit(val.value)
                if val.conversion == ord('s'):
                    expr = ir.CreateObject('PyString', [ir.MethodCall(expr, 'str', [])])
                elif val.conversion == ord('r'):
                    expr = ir.CreateObject('PyString', [ir.MethodCall(expr, 'repr', [])])
                elif val.conversion == ord('a'):
                    expr = ir.MethodCall(ir.Identifier('PyBuiltinFunctionsImpl'), 'pyfunc_ascii', [expr])
                elif val.conversion != -1:
                    self.error(val.lineno, f'unsupported f string conversion type {val.conversion}')
                if isinstance(format_spec, ir.StrLiteral) and not format_spec.s and \
                    isinstance(expr, ir.CreateObject) and expr.type == 'PyString':
                    expr = expr.args[0]
                else:
                    expr = ir.MethodCall(expr, 'format', [format_spec])
                vals.append(expr)
        expr = ir.chained_binary_op('+', vals)
        if isinstance(expr, ir.StrLiteral):
            return ir.PyConstant(expr.s)
        return ir.CreateObject('PyString', [expr])

    def emit_star_expanded(self, nodes: list, *, array_list_allowed: bool = False) -> ir.Expr:
        if any(isinstance(arg, ast.Starred) for arg in nodes):
            args = ir.CreateObject('java.util.ArrayList<PyObject>', [])
            for arg in nodes:
                if isinstance(arg, ast.Starred):
                    args = ir.MethodCall(ir.Identifier('Runtime'), 'addStarToArrayList', [args, self.visit(arg.value)])
                else:
                    args = ir.MethodCall(ir.Identifier('Runtime'), 'addPyObjectToArrayList', [args, self.visit(arg)])
            if not array_list_allowed:
                args = ir.MethodCall(ir.Identifier('Runtime'), 'arrayListToArray', [args])
            return args
        else:
            return ir.CreateArray('PyObject', [self.visit(x) for x in nodes])

    def visit_List(self, node) -> ir.Expr:
        args = self.emit_star_expanded(node.elts, array_list_allowed=True)
        return ir.CreateObject('PyList', [args])

    def visit_Tuple(self, node) -> ir.Expr:
        args = self.emit_star_expanded(node.elts, array_list_allowed=True)
        return ir.CreateObject('PyTuple', [args])

    def visit_Set(self, node) -> ir.Expr:
        args = self.emit_star_expanded(node.elts, array_list_allowed=True)
        return ir.CreateObject('PySet', [args])

    def visit_Dict(self, node) -> ir.Expr:
        assert len(node.keys) == len(node.values), node
        kv_iter = itertools.chain.from_iterable(zip(node.keys, node.values))
        return ir.CreateObject('PyDict', [ir.Null() if x is None else self.visit(x) for x in kv_iter])

    def visit_Call(self, node) -> ir.Expr:
        if (isinstance(node.func, ast.Lambda) and
            not node.keywords and
            not any(isinstance(arg, ast.Starred) for arg in node.args)):
            params = self.get_supported_params(node.lineno, node.func.args)
            (min_args, max_args) = cast(tuple[int, int], get_positional_call_range(params))
            if min_args <= len(node.args) <= max_args:
                return ir.MethodCall(
                    self.visit(node.func),
                    'callPositional',
                    [*([self.visit(arg) for arg in node.args]), *([ir.Null()] * (max_args - len(node.args)))],
                )
        if (isinstance(node.func, ast.Name) and
            self.name_resolves_to_final_top_level_function(node.func) and
            not node.keywords and
            not any(isinstance(arg, ast.Starred) for arg in node.args)):
            (min_args, max_args) = self.module_scope().info.initial_final_function_call_ranges[node.func.id]
            if min_args <= len(node.args) <= max_args:
                return ir.MethodCall(
                    self.final_top_level_function_expr(node.func.id),
                    'callPositional',
                    [*([self.visit(arg) for arg in node.args]), *([ir.Null()] * (max_args - len(node.args)))],
                )
        if (isinstance(node.func, ast.Name) and
            node.func.id in DIRECT_NEWOBJ_POSITIONAL_BUILTIN_TYPES and
            self.name_resolves_to_builtin_type(node.func) and
            not node.keywords and
            not any(isinstance(arg, ast.Starred) for arg in node.args)):
            (min_args, max_args) = DIRECT_NEWOBJ_POSITIONAL_BUILTIN_TYPES[node.func.id]
            if min_args <= len(node.args) <= max_args:
                java_name = extract_spec.BUILTIN_TYPES[node.func.id]
                return ir.MethodCall(
                    ir.Identifier(java_name),
                    'newObjPositional',
                    [*([self.visit(arg) for arg in node.args]), *([ir.Null()] * (max_args - len(node.args)))],
                )
        if self.allow_intrinsics and isinstance(node.func, ast.Name):
            match node.func.id:
                case '__pythonj_abs__':
                    assert len(node.args) == 1 and not node.keywords, node.args
                    return ir.MethodCall(self.visit(node.args[0]), 'abs', [])
                case '__pythonj_delattr__':
                    assert len(node.args) == 2 and not node.keywords, node.args
                    return ir.MethodCall(ir.Identifier('Runtime'), 'pythonjDelAttr', [self.visit(node.args[0]), self.visit(node.args[1])])
                case '__pythonj_dict_get__':
                    assert len(node.args) == 2 and not node.keywords, node.args
                    return ir.MethodCall(ir.Identifier('Runtime'), 'pythonjDictGet', [self.visit(node.args[0]), self.visit(node.args[1])])
                case '__pythonj_format__':
                    assert len(node.args) == 2 and not node.keywords, node.args
                    return ir.MethodCall(ir.Identifier('Runtime'), 'pythonjFormat', [self.visit(node.args[0]), self.visit(node.args[1])])
                case '__pythonj_getattr__':
                    assert len(node.args) == 2 and not node.keywords, node.args
                    return ir.MethodCall(ir.Identifier('Runtime'), 'pythonjGetAttr', [self.visit(node.args[0]), self.visit(node.args[1])])
                case '__pythonj_hash__':
                    assert len(node.args) == 1 and not node.keywords, node.args
                    return ir.MethodCall(ir.Identifier('Runtime'), 'pythonjHash', [self.visit(node.args[0])])
                case '__pythonj_index__':
                    assert len(node.args) == 1 and not node.keywords, node.args
                    return ir.MethodCall(ir.Identifier('Runtime'), 'pythonjIndex', [self.visit(node.args[0])])
                case '__pythonj_isinstance__':
                    assert len(node.args) == 2 and not node.keywords, node.args
                    return self.emit_isinstance_condition(node.args[0], node.args[1])
                case '__pythonj_issubclass__':
                    assert len(node.args) == 2 and not node.keywords, node.args
                    return ir.MethodCall(ir.Identifier('Runtime'), 'pythonjIsSubclass', [self.visit(node.args[0]), self.visit(node.args[1])])
                case '__pythonj_iter__':
                    assert len(node.args) == 1 and not node.keywords, node.args
                    return ir.MethodCall(self.visit(node.args[0]), 'iter', [])
                case '__pythonj_len__':
                    assert len(node.args) == 1 and not node.keywords, node.args
                    return ir.MethodCall(ir.Identifier('Runtime'), 'pythonjLen', [self.visit(node.args[0])])
                case '__pythonj_next__':
                    assert len(node.args) == 1 and not node.keywords, node.args
                    return ir.MethodCall(self.visit(node.args[0]), 'next', [])
                case '__pythonj_repr__':
                    assert len(node.args) == 1 and not node.keywords, node.args
                    return ir.MethodCall(ir.Identifier('Runtime'), 'pythonjRepr', [self.visit(node.args[0])])
                case '__pythonj_setattr__':
                    assert len(node.args) == 3 and not node.keywords, node.args
                    return ir.MethodCall(ir.Identifier('Runtime'), 'pythonjSetAttr', [self.visit(node.args[0]), self.visit(node.args[1]), self.visit(node.args[2])])
                case '__pythonj_zip_new__':
                    assert len(node.args) == 2 and not node.keywords, node.args
                    return ir.MethodCall(ir.Identifier('Runtime'), 'pythonjZipNew', [self.visit(node.args[0]), self.visit(node.args[1])])
        if (isinstance(node.func, ast.Attribute) and
            not node.keywords and
            not any(isinstance(arg, ast.Starred) for arg in node.args)):
            type_name = self.infer_exact_builtin_type_expr(node.func.value)
            if type_name is not None and RUNTIME_SPEC is not None:
                attr_kind = DIRECT_GETATTR_BUILTIN_TYPE_ATTRS.get(type_name, {}).get(node.func.attr)
                if attr_kind == 'method':
                    call_range = get_positional_call_range(get_method_params(RUNTIME_SPEC, type_name, node.func.attr))
                    if call_range is not None:
                        (min_args, max_args) = call_range
                        if min_args <= len(node.args) <= max_args:
                            java_name = extract_spec.BUILTIN_TYPES[type_name]
                            receiver = self.visit(node.func.value)
                            if not (isinstance(node.func.value, ast.Name) and get_name_resolution(node.func.value) is NameResolution.LOCAL):
                                receiver = ir.CastExpr(java_name, receiver)
                            return ir.MethodCall(
                                ir.Identifier(f'{java_name}Method_{node.func.attr}'),
                                'callPositional',
                                [receiver, *([self.visit(arg) for arg in node.args]), *([ir.Null()] * (max_args - len(node.args)))],
                            )
        if (isinstance(node.func, ast.Name) and node.func.id in DIRECT_CALL_POSITIONAL_BUILTINS and
            self.name_resolves_to_builtin_function(node.func) and
            not node.keywords and
            not any(isinstance(arg, ast.Starred) for arg in node.args)):
            func_name = node.func.id
            (min_args, max_args) = DIRECT_CALL_POSITIONAL_BUILTINS[node.func.id]
            if min_args <= len(node.args) <= max_args:
                return ir.MethodCall(
                    ir.Field(ir.Identifier(f'PyBuiltinFunction_{func_name}'), 'singleton'),
                    'callPositional',
                    [*([self.visit(arg) for arg in node.args]), *([ir.Null()] * (max_args - len(node.args)))],
                )
        if isinstance(node.func, ast.Attribute) and isinstance(node.func.value, ast.Name):
            module_name = self.builtin_module_name_for_name(node.func.value)
            if (module_name is not None and
                node.func.attr in DIRECT_CALL_POSITIONAL_MODULE_FUNCTIONS.get(module_name, {}) and
                not node.keywords and
                not any(isinstance(arg, ast.Starred) for arg in node.args)):
                (min_args, max_args) = DIRECT_CALL_POSITIONAL_MODULE_FUNCTIONS[module_name][node.func.attr]
                if min_args <= len(node.args) <= max_args:
                    return ir.MethodCall(
                        get_builtin_module_attr_expr(module_name, node.func.attr, 'builtin_function'),
                        'callPositional',
                        [*([self.visit(arg) for arg in node.args]), *([ir.Null()] * (max_args - len(node.args)))],
                    )
        if isinstance(node.func, ast.Name) and node.func.id in self.python_helper_names:
            if node.keywords or any(isinstance(arg, ast.Starred) for arg in node.args):
                self.error(node.lineno, 'same-file helper calls only support plain positional args')
            assert self.python_helper_class is not None, node.func.id
            return ir.MethodCall(ir.Identifier(self.python_helper_class), f'pyfunc_{node.func.id}', [self.visit(arg) for arg in node.args])

        func = self.visit(node.func)
        args = self.emit_star_expanded(node.args)
        if node.keywords:
            kv_list: list[ir.Expr] = []
            for kwarg in node.keywords:
                if kwarg.arg is None: # **kwargs
                    kv_list.append(ir.Null())
                    kv_list.append(self.visit(kwarg.value))
                else:
                    assert isinstance(kwarg.arg, str), kwarg.arg
                    kv_list.append(ir.PyConstant(kwarg.arg))
                    kv_list.append(self.visit(kwarg.value))
            kwargs = ir.MethodCall(ir.Identifier('Runtime'), 'requireKwStrings', [ir.CreateObject('PyDict', kv_list)])
        else:
            kwargs = ir.Null()
        return ir.MethodCall(func, 'call', [args, kwargs])

    def visit_Name(self, node) -> ir.Expr:
        return self.ident_expr_by_resolution(node.id, get_name_resolution(node))

    def visit_Subscript(self, node) -> ir.Expr:
        return ir.MethodCall(self.visit(node.value), 'getItem', [self.visit(node.slice)])

    def visit_Attribute(self, node) -> ir.Expr:
        attr_kind = None
        if isinstance(node.value, ast.Name):
            module_name = self.builtin_module_name_for_name(node.value)
            if module_name is not None:
                attr_kind = DIRECT_GETATTR_BUILTIN_MODULE_ATTRS.get(module_name, {}).get(node.attr)
                if attr_kind is not None:
                    return get_builtin_module_attr_expr(module_name, node.attr, attr_kind)
                base = ir.Field(ir.Identifier(extract_spec.BUILTIN_MODULES[module_name]), 'singleton')
                return ir.MethodCall(base, 'getAttr', [ir.StrLiteral(node.attr)])
        if isinstance(node.value, ast.Name):
            attr_kind = DIRECT_GETATTR_BUILTIN_TYPE_ATTRS.get(node.value.id, {}).get(node.attr)
        if (isinstance(node.value, ast.Name) and
            self.name_resolves_to_builtin_type(node.value) and
            attr_kind is not None):
            return get_builtin_type_attr_expr(node.value.id, node.attr, attr_kind, None)
        type_name = self.infer_exact_builtin_type_expr(node.value)
        if type_name is not None:
            attr_kind = DIRECT_GETATTR_BUILTIN_TYPE_ATTRS.get(type_name, {}).get(node.attr)
            if attr_kind is not None:
                return get_builtin_type_attr_expr(type_name, node.attr, attr_kind, self.visit(node.value))
        return ir.MethodCall(self.visit(node.value), 'getAttr', [ir.StrLiteral(node.attr)])

    def visit_Import(self, node) -> None:
        for alias in node.names:
            if alias.name not in extract_spec.BUILTIN_MODULES:
                self.error(node.lineno, f"only builtin-module imports are supported (got import {alias.name!r})")
                continue
            if '.' in alias.name:
                self.error(node.lineno, f"package imports are unsupported (got import {alias.name!r})")
                continue
            bind_name = alias.asname if alias.asname is not None else alias.name
            value = ir.Field(ir.Identifier(extract_spec.BUILTIN_MODULES[alias.name]), 'singleton')
            if self.scope.info.initial_builtin_module_locals.get(bind_name) == alias.name:
                if self.scope.info.kind is ScopeKind.FUNCTION:
                    self.code.append(ir.LocalDecl('final PyObject', f'pylocal_{bind_name}', value))
                else:
                    assert self.scope.info.kind is ScopeKind.MODULE, self.scope.info.kind
                continue
            self.code.append(ir.AssignStatement(self.ident_expr(bind_name), value))

    def visit_ImportFrom(self, node) -> None:
        self.error(node.lineno, 'from ... import ... is unsupported')

    def visit_Slice(self, node) -> ir.Expr:
        lower = self.visit(node.lower) if node.lower else ir.PyConstant(None)
        upper = self.visit(node.upper) if node.upper else ir.PyConstant(None)
        step = self.visit(node.step) if node.step else ir.PyConstant(None)
        return ir.CreateObject('PySlice', [lower, upper, step])

    # XXX Change all statements to -> Iterator[ir.Statement] and yield statements?
    def visit_Pass(self, node) -> None:
        pass

    def visit_Global(self, node) -> None:
        pass # handled by SymbolTableVisitor

    def visit_Nonlocal(self, node) -> None:
        pass # handled by SymbolTableVisitor

    def emit_bind(self, target: ast.expr, value: ir.Expr) -> Iterator[ir.Statement]:
        if isinstance(target, ast.Name):
            if get_name_resolution(target) is NameResolution.LOCAL:
                value = self.cast_local_assignment(target.id, value)
            yield ir.AssignStatement(self.visit(target), value)
        elif isinstance(target, ast.Attribute):
            temp_name = self.scope.make_temp()
            yield ir.LocalDecl('var', temp_name, value)
            yield ir.method_call_statement(self.visit(target.value), 'setAttr', [ir.StrLiteral(target.attr), ir.Identifier(temp_name)])
        elif isinstance(target, ast.Subscript):
            temp_name = self.scope.make_temp()
            yield ir.LocalDecl('var', temp_name, value)
            yield ir.method_call_statement(self.visit(target.value), 'setItem', [self.visit(target.slice), ir.Identifier(temp_name)])
        elif isinstance(target, (ast.Tuple, ast.List)):
            temp_name = self.scope.make_temp()
            yield ir.LocalDecl('var', temp_name, ir.MethodCall(value, 'iter', []))
            # XXX This is not atomic if an exception is thrown; a subset of LHS's will be left assigned
            for subtarget in target.elts:
                yield from self.emit_bind(subtarget, ir.MethodCall(ir.Identifier('Runtime'), 'nextRequireNonNull', [ir.Identifier(temp_name)]))
            yield ir.method_call_statement(ir.Identifier('Runtime'), 'nextRequireNull', [ir.Identifier(temp_name)])
        else:
            self.error(target.lineno, f'binding to {type(target).__name__} is unsupported')

    def visit_Assign(self, node) -> None:
        if len(node.targets) != 1:
            self.error(node.lineno, 'chained assignment (a = b = c) is unsupported')
        target = node.targets[0]
        self.code.extend(self.emit_bind(target, self.visit(node.value)))

    def visit_AnnAssign(self, node) -> None:
        self.report_annotation_errors()
        if not isinstance(node.target, ast.Name):
            self.error(node.lineno, 'only simple local variable annotations are supported')
            if node.value is not None:
                self.visit(node.value)
            return
        if not isinstance(node.annotation, ast.Name) or node.annotation.id not in extract_spec.BUILTIN_TYPES:
            self.error(node.lineno, 'only exact builtin-type local annotations are supported')
            if node.value is not None:
                self.visit(node.value)
            return
        if any(lineno == node.lineno for (lineno, _) in self.scope.info.annotation_errors):
            return
        if node.target.id not in self.scope.info.exact_local_types:
            self.error(node.lineno, 'unsupported local annotation')
            if node.value is not None:
                self.visit(node.value)
            return
        if node.value is not None:
            self.code.extend(self.emit_bind(node.target, self.visit(node.value)))

    def visit_AugAssign(self, node) -> None:
        op = f'{self.visit(node.op)}InPlace'

        if isinstance(node.target, ast.Name):
            exact_int_expr = self.emit_exact_int_binop(self.visit(node.op), node.target, node.value)
            if exact_int_expr is not None:
                value = exact_int_expr
            else:
                value = ir.MethodCall(self.visit(node.target), op, [self.visit(node.value)])
            code = ir.AssignStatement(self.visit(node.target), self.cast_local_assignment(node.target.id, value))
        elif isinstance(node.target, ast.Attribute):
            temp_name = self.scope.make_temp()
            self.code.append(ir.LocalDecl('var', temp_name, self.visit(node.target.value)))
            code = ir.method_call_statement(ir.Identifier(temp_name), 'setAttr', [
                ir.StrLiteral(node.target.attr),
                ir.MethodCall(
                    ir.MethodCall(ir.Identifier(temp_name), 'getAttr', [ir.StrLiteral(node.target.attr)]),
                    op,
                    [self.visit(node.value)]
                )
            ])
        elif isinstance(node.target, ast.Subscript):
            temp_name0 = self.scope.make_temp()
            temp_name1 = self.scope.make_temp()
            self.code.append(ir.LocalDecl('var', temp_name0, self.visit(node.target.value)))
            self.code.append(ir.LocalDecl('var', temp_name1, self.visit(node.target.slice)))
            code = ir.method_call_statement(ir.Identifier(temp_name0), 'setItem', [
                ir.Identifier(temp_name1),
                ir.MethodCall(
                    ir.MethodCall(ir.Identifier(temp_name0), 'getItem', [ir.Identifier(temp_name1)]),
                    op,
                    [self.visit(node.value)]
                )
            ])
        else:
            self.error(node.lineno, f'augmented assignment to {type(node.target).__name__} is unsupported')
            self.visit(node.value) # recurse to find more errors
            return
        self.code.append(code)

    def visit_Assert(self, node) -> None:
        cond = ir.unary_op('!', self.emit_condition(node.test))
        msg = ir.StrLiteral(self.path + f':{node.lineno}: assertion failure')
        if node.msg:
            msg.s += ': '
            msg = ir.BinaryOp('+', msg, ir.MethodCall(self.visit(node.msg), 'repr', []))
        exception = ir.MethodCall(ir.Identifier('PyAssertionError'), 'raise', [msg])
        self.code.extend(ir.if_statement(cond, [ir.ThrowStatement(exception)], []))

    def visit_Delete(self, node) -> None:
        for target in node.targets:
            if isinstance(target, ast.Attribute):
                code = ir.method_call_statement(self.visit(target.value), 'delAttr', [ir.StrLiteral(target.attr)])
            elif isinstance(target, ast.Subscript):
                code = ir.method_call_statement(self.visit(target.value), 'delItem', [self.visit(target.slice)])
            else:
                self.error(node.lineno, f"'del' of {type(target).__name__} is unsupported")
                continue
            self.code.append(code)

    def visit_Return(self, node) -> None:
        assert self.scope.info.kind is ScopeKind.FUNCTION, node
        if self.scope.expected_return_java_type == 'NoReturn':
            self.error(node.lineno, 'NoReturn helper functions may not contain return statements')
        value = self.visit(node.value) if node.value else ir.PyConstant(None)
        if self.scope.expected_return_java_type is not None and self.scope.expected_return_java_type not in {'PyObject', 'NoReturn'}:
            value = ir.CastExpr(self.scope.expected_return_java_type, value)
        self.code.append(ir.ReturnStatement(value))

    def visit_Raise(self, node) -> None:
        if node.exc is None:
            self.error(node.lineno, "bare 'raise' is unsupported")
            return
        if node.cause is not None:
            self.error(node.lineno, "'raise ... from ...' is unsupported")
            self.visit(node.exc)
            self.visit(node.cause)
            return
        self.code.append(ir.ThrowStatement(ir.MethodCall(ir.Identifier('Runtime'), 'raiseExpr', [self.visit(node.exc)])))

    @contextmanager
    def new_block(self) -> Iterator[list[ir.Statement]]:
        saved = self.code
        self.code = []
        yield self.code
        self.code = saved

    @contextmanager
    def push_break_name(self, break_name: Optional[str]) -> Iterator[None]:
        saved = self.break_name
        self.break_name = break_name
        yield
        self.break_name = saved

    def visit_block(self, statements: list[ast.stmt]) -> list[ir.Statement]:
        with self.new_block() as body:
            for statement in statements:
                self.visit(statement)
        return body

    def visit_If(self, node) -> None:
        cond = self.emit_condition(node.test)
        body = self.visit_block(node.body)
        orelse = self.visit_block(node.orelse)
        self.code.extend(ir.if_statement(cond, body, orelse))

    def visit_While(self, node) -> None:
        cond = self.emit_condition(node.test)

        block_name = self.scope.make_temp() if node.orelse else None
        with self.push_break_name(block_name):
            body = self.visit_block(node.body)

        loop = list(ir.while_statement(cond, body))
        if node.orelse:
            orelse = self.visit_block(node.orelse)
            assert block_name is not None
            loop = [ir.LabeledBlock(block_name, [*loop, *orelse])]
        self.code.extend(loop)

    def visit_For(self, node) -> None:
        block_name = self.scope.make_temp() if node.orelse else None
        temp_name0 = self.scope.make_temp()
        temp_name1 = self.scope.make_temp()
        self.code.append(ir.LocalDecl('var', temp_name0, ir.MethodCall(self.visit(node.iter), 'iter', [])))

        with self.push_break_name(block_name):
            loop = ir.ForStatement(
                'var', temp_name1, ir.MethodCall(ir.Identifier(temp_name0), 'next', []),
                ir.BinaryOp('!=', ir.Identifier(temp_name1), ir.Null()),
                temp_name1, ir.MethodCall(ir.Identifier(temp_name0), 'next', []),
                [
                    *self.emit_bind(node.target, ir.Identifier(temp_name1)),
                    *self.visit_block(node.body),
                ]
            )
        if node.orelse:
            orelse = self.visit_block(node.orelse)
            assert block_name is not None
            loop = ir.LabeledBlock(block_name, [loop, *orelse])
        self.code.append(loop)

    def visit_With(self, node) -> None:
        # node.type_comment is ignored; we only plan to support "real" type annotations.
        if len(node.items) != 1:
            self.error(node.lineno, "multiple-item 'with' statements are unsupported")
        item = node.items[0]

        temp_name = self.scope.make_temp()
        self.code.append(ir.LocalDecl('var', temp_name, self.visit(item.context_expr)))
        if item.optional_vars is None:
            self.code.append(ir.method_call_statement(ir.Identifier(temp_name), 'enter', []))
        else:
            self.code.extend(self.emit_bind(item.optional_vars, ir.MethodCall(ir.Identifier(temp_name), 'enter', [])))

        body = self.visit_block(node.body)

        self.code.append(ir.TryStatement(body, None, None, [], [
            ir.method_call_statement(ir.Identifier(temp_name), 'exit', []),
        ]))

    def visit_Try(self, node) -> None:
        if len(node.handlers) > 1:
            self.error(node.lineno, "at most 1 'except' clause is supported in 'try' statements")
        if node.orelse:
            self.error(node.lineno, "'else' clauses in 'try' statements are unsupported")
        for handler in node.handlers:
            if handler.type is not None:
                if not isinstance(handler.type, ast.Name):
                    self.error(node.lineno, "only 'except SomeException [as e]:' with a named built-in exception is supported")
                elif handler.type.id not in extract_spec.EXCEPTION_TYPES:
                    self.error(node.lineno, "only built-in exception names are supported in 'except' clauses")

        try_body = self.visit_block(node.body)

        exc_type = None
        exc_name = None
        catch_body = []
        if node.handlers:
            exc_type = 'PyRaise'
            exc_name = self.scope.make_temp()
            handler = node.handlers[0]
            with self.new_block() as catch_body:
                if handler.type is not None:
                    caught_exc = ir.Field(ir.Identifier(exc_name), 'exc')
                    expected_exc = ir.Identifier(f'Py{cast(ast.Name, handler.type).id}')
                    catch_body.extend(ir.if_statement(
                        ir.unary_op('!', ir.BinaryOp('instanceof', caught_exc, expected_exc)),
                        [ir.ThrowStatement(ir.Identifier(exc_name))],
                        [],
                    ))
                if handler.name is not None:
                    catch_body.append(ir.AssignStatement(self.ident_expr(handler.name), ir.Field(ir.Identifier(exc_name), 'exc')))
                for statement in handler.body:
                    self.visit(statement)

        finally_body = self.visit_block(node.finalbody)

        self.code.append(ir.TryStatement(try_body, exc_type, exc_name, catch_body, finally_body))

    def visit_Break(self, node) -> None:
        self.code.append(ir.BreakStatement(self.break_name))

    def visit_Continue(self, node) -> None:
        self.code.append(ir.ContinueStatement())

    def visit_Expr(self, node) -> None:
        value = self.visit(node.value)
        if isinstance(value, (ir.MethodCall, ir.CreateObject)): # allowed by Java grammar as statements
            self.code.append(ir.ExprStatement(value))
        else:
            # Avoid "not a statement" javac errors by assigning otherwise-unused values to a temp.
            # Cannot remove these statements because we rely on javac here to catch some portion of
            # Python usage errors.
            self.code.append(ir.AssignStatement(ir.Identifier('expr_discard'), value))
            self.scope.used_expr_discard = True

    def get_supported_params(self, lineno: int, args: ast.arguments) -> list[inspect.Parameter]:
        if args.vararg:
            self.error(lineno, '*args are unsupported')
        if args.kwonlyargs:
            self.error(lineno, 'kw-only arguments are unsupported')
        if args.kw_defaults:
            self.error(lineno, 'kw-only argument defaults are unsupported')
        if args.kwarg:
            self.error(lineno, '**kwargs are unsupported')
        defaults: list[object] = []
        for default in args.defaults:
            if isinstance(default, ast.Constant) and is_constant_default(default.value):
                defaults.append(default.value)
            elif isinstance(default, ast.Tuple):
                try:
                    value = ast.literal_eval(default)
                except (TypeError, ValueError):
                    self.error(lineno, 'only constant argument defaults are supported')
                else:
                    if is_constant_default(value):
                        defaults.append(value)
                    else:
                        self.error(lineno, 'only constant argument defaults are supported')
            else:
                self.error(lineno, 'only constant argument defaults are supported')
        positional_args = [*args.posonlyargs, *args.args]
        for arg in positional_args:
            # arg.type_comment is ignored; we only plan to support "real" type annotations.
            if arg.annotation:
                if not isinstance(arg.annotation, ast.Name) or arg.annotation.id not in extract_spec.BUILTIN_TYPES:
                    self.error(lineno, 'only exact builtin-type argument annotations are supported')
        params: list[inspect.Parameter] = []
        n_args = len(positional_args)
        n_defaults = len(defaults)
        first_default = n_args - n_defaults
        for (i, arg) in enumerate(positional_args):
            default = inspect.Parameter.empty if i < first_default else defaults[i - first_default]
            kind = inspect.Parameter.POSITIONAL_ONLY if i < len(args.posonlyargs) else inspect.Parameter.POSITIONAL_OR_KEYWORD
            params.append(inspect.Parameter(arg.arg, kind, default=default))
        return params

    def check_args(self, lineno: int, args: ast.arguments) -> tuple[list[str], list[object]]:
        params = self.get_supported_params(lineno, args)
        arg_names = [param.name for param in params]
        arg_defaults = [param.default for param in params if param.default is not inspect.Parameter.empty]
        return (arg_names, arg_defaults)

    @contextmanager
    def new_function(self, scope_info: ScopeInfo, free_vars: set[str], qualname: str,
                     expected_return_java_type: Optional[str] = None) -> Iterator[None]:
        saved_scope = self.scope
        self.scope = Scope(
            self.scope,
            scope_info,
            qualname,
            expected_return_java_type,
        )
        self.scope.free_vars = free_vars.copy()
        try:
            yield
        finally:
            self.scope = saved_scope

    def qualname(self, name: str) -> str:
        return name if self.scope.qualname is None else f'{self.scope.qualname}.<locals>.{name}'

    def add_function(self, py_name: str, java_name: str, params: list[inspect.Parameter], body: list[ir.Statement],
                     invisible_args: bool = False) -> None:
        arg_names = [param.name for param in params]
        arg_defaults = [param.default for param in params if param.default is not inspect.Parameter.empty]
        n_args = len(arg_names)
        n_required = n_args - len(arg_defaults)
        bind_arg_names = [f'pyarg_{arg}' for arg in arg_names]
        shape = analyze_params(params)
        free_var_names = sorted(self.scope.free_vars)
        constructor_args = [f'PyCell _pycell_{name}' for name in free_var_names]
        func_decls: list[ir.Decl] = [
            *(ir.FieldDecl('private final', 'PyCell', f'pycell_{name}', None) for name in free_var_names),
            ir.ConstructorDecl(
                '',
                java_name,
                constructor_args,
                [
                    ir.SuperConstructorCall([ir.StrLiteral(py_name)]),
                    *(ir.AssignStatement(ir.Identifier(f'pycell_{name}'), ir.Identifier(f'_pycell_{name}')) for name in free_var_names),
                ],
            ),
        ]
        call_body: list[ir.Statement]
        if shape.posonly_params and not shape.poskw_params:
            if n_required == n_args:
                call_body = [
                    ir.method_call_statement(ir.Identifier('PyRuntime'), 'pyfunc_require_user_exact_positional', [
                        ir.CreateObject('PyInt', [ir.Field(ir.Identifier('args'), 'length')]),
                        ir.Identifier('kwargs'),
                        ir.PyConstant(py_name),
                        ir.PyConstant(tuple(arg_names)),
                    ]),
                    ir.ReturnStatement(ir.MethodCall(
                        ir.This(),
                        'callPositional',
                        [ir.ArrayAccess(ir.Identifier('args'), ir.IntLiteral(i)) for i in range(n_args)],
                    )),
                ]
            else:
                call_body = [
                    ir.LocalDecl('int', 'argsLength', ir.Field(ir.Identifier('args'), 'length')),
                    ir.method_call_statement(ir.Identifier('PyRuntime'), 'pyfunc_require_user_min_max_positional', [
                        ir.CreateObject('PyInt', [ir.Identifier('argsLength')]),
                        ir.Identifier('kwargs'),
                        ir.PyConstant(py_name),
                        ir.PyConstant(tuple(arg_names)),
                        ir.PyConstant(n_required),
                    ]),
                    ir.ReturnStatement(ir.MethodCall(ir.This(), 'callPositional', [
                        *(ir.ArrayAccess(ir.Identifier('args'), ir.IntLiteral(i)) for i in range(n_required)),
                        *(
                            ir.CondOp(
                                ir.BinaryOp('>', ir.Identifier('argsLength'), ir.IntLiteral(i)),
                                ir.ArrayAccess(ir.Identifier('args'), ir.IntLiteral(i)),
                                ir.Null(),
                            )
                            for i in range(n_required, n_args)
                        ),
                    ])),
                ]
        else:
            call_body = [
                ir.LocalDecl('var', 'boundArgs', ir.Field(ir.MethodCall(ir.Identifier('PyRuntime'), 'pyfunc_bind_user_function', [
                    ir.CreateObject('PyTuple', [ir.Identifier('args')]),
                    ir.Identifier('kwargs'),
                    ir.PyConstant(py_name),
                    ir.PyConstant(tuple(arg_names)),
                    ir.PyConstant(n_required),
                    ir.PyConstant(len(shape.posonly_params)),
                ]), 'items')),
                ir.ReturnStatement(ir.MethodCall(
                    ir.This(),
                    'callPositional',
                    [ir.MethodCall(ir.Identifier('boundArgs'), 'get', [ir.IntLiteral(i)]) for i in range(n_args)],
                )),
            ]
        func_decls.append(ir.MethodDecl('@Override public', 'PyObject', 'call', ['PyObject[] args', 'PyDict kwargs'], call_body))
        call_positional_body: list[ir.Statement] = [
            *(ir.IfStatement(
                ir.BinaryOp('==', ir.Identifier(name), ir.Null()),
                [ir.AssignStatement(ir.Identifier(name), emit_default_java_expr(arg_defaults[i]))],
                [],
            ) for (i, name) in enumerate(bind_arg_names[n_required:])),
        ]
        if self.scope.used_expr_discard:
            call_positional_body.append(ir.LocalDecl('PyObject', 'expr_discard', None))
        for (arg_name, bind_arg_name) in zip(arg_names, bind_arg_names):
            if arg_name in self.scope.info.cell_vars:
                call_positional_body.append(ir.LocalDecl('PyCell', f'pycell_{arg_name}', ir.CreateObject('PyCell', [ir.Identifier(bind_arg_name)])))
            else:
                local_arg_name = arg_name if invisible_args else f'pylocal_{arg_name}'
                call_positional_body.append(ir.LocalDecl(self.java_local_type(arg_name), local_arg_name, self.cast_local_assignment(arg_name, ir.Identifier(bind_arg_name))))
        for name in sorted(self.scope.info.cell_vars - set(arg_names)):
            call_positional_body.append(ir.LocalDecl('PyCell', f'pycell_{name}', ir.CreateObject('PyCell', [ir.PyConstant(None)])))
        for name in sorted(self.scope.info.locals - self.scope.info.cell_vars - set(arg_names) - set(self.scope.info.initial_builtin_module_locals)):
            if invisible_args or name not in arg_names:
                call_positional_body.append(ir.LocalDecl(self.java_local_type(name), f'pylocal_{name}', None))
        implicit_return: ir.Statement = ir.ReturnStatement(ir.PyConstant(None))
        if self.scope.expected_return_java_type not in {None, 'PyObject'}:
            implicit_return = ir.ReturnStatement(ir.CastExpr(self.scope.expected_return_java_type, ir.PyConstant(None)))
        call_positional_body.extend(ir.block_simplify([*body, implicit_return]))
        func_decls.append(ir.MethodDecl(
            'public',
            'PyObject',
            'callPositional',
            [f'PyObject {name}' for name in bind_arg_names],
            call_positional_body,
        ))
        assert java_name not in self.classes
        self.classes[java_name] = ir.ClassDecl('private static final', java_name, 'PyFunction', func_decls)

    def visit_FunctionDef(self, node) -> None:
        # node.type_comment is ignored; we only plan to support "real" type annotations.
        if node.decorator_list:
            self.error(node.lineno, 'function decorators are unsupported')
        if node.type_params:
            self.error(node.lineno, 'function type parameters are unsupported')
        return_java_type = 'PyObject'
        if node.returns is not None:
            type_name = _decode_user_function_return_annotation(node.returns)
            if type_name is None:
                self.error(node.lineno, 'only exact builtin-type function return annotations are supported')
            else:
                return_java_type = extract_spec.BUILTIN_TYPES[type_name]

        params = self.get_supported_params(node.lineno, node.args)
        qualname = self.qualname(node.name)
        java_name = self.final_global_function_classes.get(node.name)
        if java_name is None:
            java_name = f'pyfunc_{node.name}_{self.n_functions}'
            self.n_functions += 1
        scope_info = self.scope_infos[node]
        free_vars = self.resolve_free_vars(node.lineno, scope_info, 'nested function')
        if self.scope.info.kind is ScopeKind.MODULE and node.name in self.scope.info.initial_final_function_call_ranges:
            assert not free_vars, (node.name, free_vars)
            self.final_global_function_classes[node.name] = java_name
        else:
            self.code.append(ir.AssignStatement(self.ident_expr(node.name), ir.CreateObject(java_name, [ir.Identifier(f'pycell_{name}') for name in sorted(free_vars)])))

        with self.new_function(scope_info, free_vars, qualname, return_java_type):
            self.report_annotation_errors()
            body = self.visit_block(node.body)
            if return_java_type != 'PyObject' and not ir.block_ends_control_flow(body):
                self.error(node.lineno, f'annotated function {node.name} may implicitly return None')
            self.add_function(qualname, java_name, params, body)

    def visit_ClassDef(self, node) -> None:
        if self.scope.info.kind is not ScopeKind.MODULE:
            self.error(node.lineno, "only module-level class definitions are supported")
        if node.decorator_list:
            self.error(node.lineno, 'class decorators are unsupported')
        if node.bases:
            self.error(node.lineno, 'class inheritance is unsupported')
        if node.keywords:
            self.error(node.lineno, 'class keywords are unsupported')

        slots = None
        if len(node.body) == 1 and isinstance(node.body[0], ast.Assign):
            assign = node.body[0]
            if len(assign.targets) != 1 or not isinstance(assign.targets[0], ast.Name) or assign.targets[0].id != '__slots__':
                self.error(node.lineno, "only 'class X: pass' and 'class X: __slots__ = (...)' are supported")
            if not isinstance(assign.value, (ast.Tuple, ast.List)):
                self.error(node.lineno, "__slots__ must be a tuple or list of strings")
            else:
                slots = []
                for elt in assign.value.elts:
                    if not isinstance(elt, ast.Constant) or not isinstance(elt.value, str):
                        self.error(node.lineno, "__slots__ must be a tuple or list of strings")
                    else:
                        if elt.value in slots:
                            self.error(node.lineno, 'duplicate name in __slots__')
                        slots.append(elt.value)
        elif len(node.body) != 1 or not isinstance(node.body[0], ast.Pass):
            self.error(node.lineno, "only 'class X: pass' and 'class X: __slots__ = (...)' are supported")

        java_name = f'pyclass_{node.name}'
        type_class_name = f'pyclasstype_{node.name}'
        constructor_decls = [
            ir.ConstructorDecl('', java_name, [], [
                ir.SuperConstructorCall([ir.Identifier(f'{type_class_name}.singleton')]),
            ]),
            ir.MethodDecl('public static', 'PyObject', 'newObj', ['PyConcreteType type', 'PyObject[] args', 'PyDict kwargs'], [
                ir.method_call_statement(ir.Identifier('Runtime'), 'requireNoKwArgs', [
                    ir.Identifier('kwargs'),
                    ir.MethodCall(ir.Identifier('type'), 'name', []),
                ]),
                ir.IfStatement(
                    ir.BinaryOp('!=', ir.Field(ir.Identifier('args'), 'length'), ir.IntLiteral(0)),
                    [ir.ThrowStatement(ir.MethodCall(ir.Identifier('PyTypeError'), 'raise', [
                        ir.BinaryOp('+', ir.MethodCall(ir.Identifier('type'), 'name', []), ir.StrLiteral('() takes no arguments')),
                    ]))],
                    [],
                ),
                ir.ReturnStatement(ir.CreateObject(java_name, [])),
            ]),
        ]

        class_decls: list[ir.ClassDecl] = []
        if slots is None:
            class_decls.append(ir.ClassDecl('private static final', java_name, 'PyBagObject', constructor_decls))
        else:
            class_decls.append(ir.ClassDecl('private static final', java_name, 'PySlottedObject', [
                *(ir.FieldDecl('private', 'PyObject', f'pyslot_{name}', ir.Null()) for name in slots),
                *constructor_decls,
                *(
                    ir.MethodDecl('private', 'PyObject', f'pygetslot_{name}', [], [
                        ir.IfStatement(
                            ir.BinaryOp('==', ir.Identifier(f'pyslot_{name}'), ir.Null()),
                            [ir.ThrowStatement(ir.MethodCall(ir.This(), 'raiseMissingAttr', [ir.StrLiteral(name)]))],
                            [],
                        ),
                        ir.ReturnStatement(ir.Identifier(f'pyslot_{name}')),
                    ])
                    for name in slots
                ),
                *(
                    ir.MethodDecl('private', 'void', f'pysetslot_{name}', ['PyObject value'], [
                        ir.AssignStatement(ir.Identifier(f'pyslot_{name}'), ir.Identifier('value')),
                        ir.ReturnStatement(None),
                    ])
                    for name in slots
                ),
                *(
                    ir.MethodDecl('private', 'void', f'pydelslot_{name}', [], [
                        ir.IfStatement(
                            ir.BinaryOp('==', ir.Identifier(f'pyslot_{name}'), ir.Null()),
                            [ir.ThrowStatement(ir.MethodCall(ir.This(), 'raiseMissingAttr', [ir.StrLiteral(name)]))],
                            [],
                        ),
                        ir.AssignStatement(ir.Identifier(f'pyslot_{name}'), ir.Null()),
                        ir.ReturnStatement(None),
                    ])
                    for name in slots
                ),
                ir.MethodDecl('private', 'PyObject', 'pygetslot___dict__', [], [
                    ir.ThrowStatement(ir.MethodCall(ir.This(), 'raiseMissingAttr', [ir.StrLiteral('__dict__')])),
                ]),
                ir.MethodDecl('private', 'void', 'pyraise_readonly___class__', ['String key'], [
                    ir.ThrowStatement(ir.MethodCall(ir.Identifier('Runtime'), 'raiseNamedReadOnlyAttr', [
                        ir.MethodCall(ir.This(), 'type', []),
                        ir.Identifier('key'),
                    ])),
                ]),
                ir.MethodDecl('public', 'PyObject', 'getAttr', ['String key'], [
                    ir.SwitchStatement(ir.Identifier('key'), [
                        *(ir.SwitchCase(ir.StrLiteral(name), ir.MethodCall(ir.This(), f'pygetslot_{name}', [])) for name in slots),
                        ir.SwitchCase(ir.StrLiteral('__dict__'), ir.MethodCall(ir.This(), 'pygetslot___dict__', [])),
                    ], ir.MethodCall(ir.Super(), 'getAttr', [ir.Identifier('key')])),
                ]),
                ir.MethodDecl('public', 'void', 'setAttr', ['String key', 'PyObject value'], [
                    ir.SwitchVoidStatement(ir.Identifier('key'), [
                        *(ir.SwitchCase(ir.StrLiteral(name), ir.MethodCall(ir.This(), f'pysetslot_{name}', [ir.Identifier('value')])) for name in slots),
                        ir.SwitchCase(ir.StrLiteral('__class__'), ir.MethodCall(ir.This(), 'pyraise_readonly___class__', [ir.Identifier('key')])),
                    ], ir.MethodCall(ir.Super(), 'setAttr', [ir.Identifier('key'), ir.Identifier('value')])),
                ]),
                ir.MethodDecl('public', 'void', 'delAttr', ['String key'], [
                    ir.SwitchVoidStatement(ir.Identifier('key'), [
                        *(ir.SwitchCase(ir.StrLiteral(name), ir.MethodCall(ir.This(), f'pydelslot_{name}', [])) for name in slots),
                        ir.SwitchCase(ir.StrLiteral('__class__'), ir.MethodCall(ir.This(), 'pyraise_readonly___class__', [ir.Identifier('key')])),
                    ], ir.MethodCall(ir.Super(), 'delAttr', [ir.Identifier('key')])),
                ]),
            ]))
        class_decls.append(ir.ClassDecl('private static final', type_class_name, 'PyConcreteType', [
            ir.FieldDecl('private static final', type_class_name, 'singleton', ir.CreateObject(type_class_name, [])),
            ir.ConstructorDecl('private', type_class_name, [], [
                ir.SuperConstructorCall([ir.StrLiteral(node.name), ir.Field(ir.Identifier(java_name), 'class'), ir.MethodRef(java_name, 'newObj')]),
            ]),
        ]))
        for class_decl in class_decls:
            assert class_decl.name not in self.classes
            self.classes[class_decl.name] = class_decl
        self.code.append(ir.AssignStatement(self.ident_expr(node.name), ir.Identifier(f'{type_class_name}.singleton')))

    def visit_Lambda(self, node) -> ir.Expr:
        params = self.get_supported_params(node.lineno, node.args)
        qualname = self.qualname('<lambda>')
        java_name = f'pylambda{self.n_lambdas}'
        self.n_lambdas += 1

        scope_info = self.scope_infos[node]
        assert not scope_info.explicit_globals, scope_info.explicit_globals # should not be possible in a lambda
        assert not scope_info.nonlocals, scope_info.nonlocals # should not be possible in a lambda
        free_vars = self.resolve_free_vars(node.lineno, scope_info, 'lambda')

        with self.new_function(scope_info, free_vars, qualname):
            with self.new_block() as body:
                body.append(ir.ReturnStatement(self.visit(node.body)))
            self.add_function(qualname, java_name, params, body)

        return ir.CreateObject(java_name, [ir.Identifier(f'pycell_{name}') for name in sorted(free_vars)])

    def _lower_comp_generator(self, generator: ast.comprehension, iterable: ir.Expr, statements: list[ir.Statement]) -> list[ir.Statement]:
        for _if in reversed(generator.ifs):
            statements = list(ir.if_statement(self.emit_condition(_if), statements, []))

        temp_iter = self.scope.make_temp()
        temp_element = self.scope.make_temp()
        return [
            ir.LocalDecl('var', temp_iter, ir.MethodCall(iterable, 'iter', [])),
            ir.ForStatement(
                'var', temp_element, ir.MethodCall(ir.Identifier(temp_iter), 'next', []),
                ir.BinaryOp('!=', ir.Identifier(temp_element), ir.Null()),
                temp_element, ir.MethodCall(ir.Identifier(temp_iter), 'next', []),
                [
                    *self.emit_bind(generator.target, ir.Identifier(temp_element)),
                    *statements,
                ]
            )
        ]

    def _lower_comp(self, node: ast.expr, py_name: str, type_name: str, method_name: str, lineno: int,
                    generators: list[ast.comprehension], elts: list[ast.expr]) -> ir.Expr:
        arg_name = 'iterable'
        java_name = f'pylambda{self.n_lambdas}'
        self.n_lambdas += 1

        qualname = self.qualname(py_name)
        scope_info = self.scope_infos[node]
        assert not scope_info.explicit_globals, scope_info.explicit_globals # should not be possible in a comprehension
        assert not scope_info.nonlocals, scope_info.nonlocals # should not be possible in a comprehension
        free_vars = self.resolve_free_vars(lineno, scope_info, 'comprehension')
        with self.new_function(scope_info, free_vars, qualname):
            with self.new_block() as body:
                temp_result = self.scope.make_temp()
                statements: list[ir.Statement] = [
                    ir.method_call_statement(ir.Identifier(temp_result), method_name, [self.visit(elt) for elt in elts])
                ]
                for (i, generator) in enumerate(reversed(generators)):
                    iterable = ir.Identifier(arg_name) if i == len(generators)-1 else self.visit(generator.iter)
                    statements = self._lower_comp_generator(generator, iterable, statements)
                body += [
                    ir.LocalDecl('var', temp_result, ir.CreateObject(type_name, [])),
                    *statements,
                    ir.ReturnStatement(ir.Identifier(temp_result)),
                ]
            free_var_names = sorted(self.scope.free_vars)
            call_positional_body: list[ir.Statement] = [
                ir.LocalDecl('PyObject', arg_name, ir.Identifier(f'pyarg_{arg_name}')),
            ]
            if self.scope.used_expr_discard:
                call_positional_body.append(ir.LocalDecl('PyObject', 'expr_discard', None))
            for name in sorted(self.scope.info.cell_vars - {arg_name}):
                call_positional_body.append(ir.LocalDecl('PyCell', f'pycell_{name}', ir.CreateObject('PyCell', [ir.PyConstant(None)])))
            for name in sorted(self.scope.info.locals - self.scope.info.cell_vars - {arg_name} - set(self.scope.info.initial_builtin_module_locals)):
                call_positional_body.append(ir.LocalDecl('PyObject', f'pylocal_{name}', None))
            call_positional_body.extend(ir.block_simplify([*body, ir.ReturnStatement(ir.PyConstant(None))]))
            comp_decl = ir.ClassDecl(
                'private static final',
                java_name,
                None,
                [
                    *(ir.FieldDecl('private final', 'PyCell', f'pycell_{name}', None) for name in free_var_names),
                    ir.ConstructorDecl(
                        '',
                        java_name,
                        [f'PyCell _pycell_{name}' for name in free_var_names],
                        [*(ir.AssignStatement(ir.Identifier(f'pycell_{name}'), ir.Identifier(f'_pycell_{name}')) for name in free_var_names)],
                    ),
                    ir.MethodDecl(
                        'public',
                        'PyObject',
                        'callPositional',
                        [f'PyObject pyarg_{arg_name}'],
                        call_positional_body,
                    ),
                ],
            )
            assert java_name not in self.classes
            self.classes[java_name] = comp_decl

        return ir.MethodCall(
            ir.CreateObject(java_name, [ir.Identifier(f'pycell_{name}') for name in sorted(free_vars)]),
            'callPositional',
            [self.visit(generators[0].iter)],
        )

    def _lower_genexpr(self, node: ast.GeneratorExp) -> ir.Expr:
        if len(node.generators) != 1:
            self.error(node.lineno, 'generator expressions with multiple for clauses are unsupported')
            return ir.MethodCall(ir.CreateObject('PyTuple', []), 'iter', [])
        generator = node.generators[0]
        if generator.is_async:
            self.error(node.lineno, 'async generator expressions are unsupported')
            return ir.MethodCall(ir.CreateObject('PyTuple', []), 'iter', [])

        qualname = self.qualname('<genexpr>')
        java_name = f'pylambda{self.n_lambdas}'
        self.n_lambdas += 1

        scope_info = self.scope_infos[node]
        assert not scope_info.explicit_globals, scope_info.explicit_globals
        assert not scope_info.nonlocals, scope_info.nonlocals
        free_vars = self.resolve_free_vars(node.lineno, scope_info, 'generator expression')

        with self.new_function(scope_info, free_vars, qualname):
            self.scope.locals_are_fields = True
            with self.new_block() as next_body:
                temp_item = self.scope.make_temp()
                temp_item_expr = ir.Identifier(temp_item)
                next_body.append(ir.LocalDecl('PyObject', temp_item, None))
                body: list[ir.Statement] = [ir.ReturnStatement(self.visit(node.elt))]
                for _if in reversed(generator.ifs):
                    body = list(ir.if_statement(self.emit_condition(_if), body, [ir.ContinueStatement()]))
                body = [
                    ir.AssignStatement(temp_item_expr, ir.MethodCall(ir.Identifier('pyiter_iterable'), 'next', [])),
                    *ir.if_statement(ir.BinaryOp('==', temp_item_expr, ir.Null()), [ir.ReturnStatement(ir.Null())], []),
                    *self.emit_bind(generator.target, temp_item_expr),
                    *body,
                ]
                next_body.extend(ir.while_statement(ir.Bool(True), body))

            free_var_names = sorted(self.scope.free_vars)
            ctor_args = [*(f'PyCell _pycell_{name}' for name in free_var_names), 'PyObject iterable']
            ctor_body: list[ir.Statement] = [
                *(ir.AssignStatement(ir.Identifier(f'pycell_{name}'), ir.Identifier(f'_pycell_{name}')) for name in free_var_names),
                ir.AssignStatement(ir.Identifier('pyiter_iterable'), ir.MethodCall(ir.Identifier('iterable'), 'iter', [])),
            ]
            func_decl = ir.ClassDecl(
                'private static final',
                java_name,
                'PyIter',
                [
                    ir.FieldDecl('private static final', 'PyConcreteType', 'type_singleton', ir.CreateObject('PyConcreteType', [ir.StrLiteral('generator'), ir.Field(ir.Identifier(java_name), 'class')])),
                    *(ir.FieldDecl('private final', 'PyCell', f'pycell_{name}', None) for name in free_var_names),
                    ir.FieldDecl('private final', 'PyIter', 'pyiter_iterable', None),
                    *(ir.FieldDecl('private final', 'PyCell', f'pycell_{name}', ir.CreateObject('PyCell', [ir.PyConstant(None)])) for name in sorted(self.scope.info.cell_vars)),
                    *(ir.FieldDecl('private', 'PyObject', f'pylocal_{name}', ir.PyConstant(None)) for name in sorted(self.scope.info.locals - self.scope.info.cell_vars)),
                    ir.ConstructorDecl('', java_name, ctor_args, ctor_body),
                    ir.MethodDecl('public', 'PyObject', 'next', [], next_body),
                    ir.MethodDecl('public', 'String', 'repr', [], [ir.ReturnStatement(ir.StrLiteral(f'<generator object {qualname}>'))]),
                    ir.MethodDecl('public', 'PyConcreteType', 'type', [], [ir.ReturnStatement(ir.Identifier('type_singleton'))]),
                ],
            )
            assert java_name not in self.classes
            self.classes[java_name] = func_decl

        return ir.CreateObject(java_name, [*(ir.Identifier(f'pycell_{name}') for name in sorted(free_vars)), self.visit(generator.iter)])

    def visit_ListComp(self, node) -> ir.Expr:
        return self._lower_comp(node, '<listcomp>', 'PyList', 'pymethod_append', node.lineno, node.generators, [node.elt])

    def visit_SetComp(self, node) -> ir.Expr:
        return self._lower_comp(node, '<setcomp>', 'PySet', 'pymethod_add', node.lineno, node.generators, [node.elt])

    def visit_DictComp(self, node) -> ir.Expr:
        return self._lower_comp(node, '<dictcomp>', 'PyDict', 'setItem', node.lineno, node.generators, [node.key, node.value])

    def visit_GeneratorExp(self, node) -> ir.Expr:
        return self._lower_genexpr(node)

    def visit_Module(self, node) -> None:
        for statement in node.body:
            if isinstance(statement, ast.Import):
                continue
            if isinstance(statement, ast.FunctionDef) and statement.name in self.scope.info.initial_final_function_call_ranges:
                java_name = f'pyfunc_{statement.name}_{self.n_functions}'
                self.n_functions += 1
                self.final_global_function_classes[statement.name] = java_name
                continue
            break
        for statement in node.body:
            self.visit(statement)

    def write_java(self, f: TextIO, py_name: str) -> None:
        final_import_fields = {
            bind_name: module_name for (bind_name, module_name) in self.scope.info.initial_builtin_module_locals.items()
            if bind_name in self.scope.info.locals
        }
        final_function_fields = {
            name: java_name for (name, java_name) in self.final_global_function_classes.items()
            if name in self.scope.info.locals
        }
        body_decls: list[ir.Decl] = [
            *self.classes.values(),
            # XXX Initializing all globals to None is weird, but we don't have a better option yet
            *(ir.FieldDecl('private static final', 'PyObject', f'pyglobal_{name}', ir.Field(ir.Identifier(extract_spec.BUILTIN_MODULES[module_name]), 'singleton'))
              for (name, module_name) in sorted(final_import_fields.items())),
            *(ir.FieldDecl('private static final', java_name, f'pyglobal_{name}', ir.CreateObject(java_name, []))
              for (name, java_name) in sorted(final_function_fields.items())),
            *(ir.FieldDecl('private static', 'PyObject', f'pyglobal_{name}', ir.PyConstant(None))
              for name in sorted(self.scope.info.locals - set(final_import_fields) - set(final_function_fields))),
            ir.MethodDecl('public static', 'void', 'main', ['String[] args'], ir.block_simplify(self.global_code)),
        ]
        ir.write_decls(
            f,
            [ir.ClassDecl('public final', py_name, None, ir.with_pool_decls(body_decls, self.pool))],
            self.pool,
        )

def parse_python_module(path: str) -> ast.Module:
    with open(path, encoding='utf-8') as f:
        return ast.parse(f.read(), path)

def get_top_level_functions(node: ast.Module) -> dict[str, ast.FunctionDef]:
    return {x.name: x for x in node.body if isinstance(x, ast.FunctionDef)}

def get_class_functions(node: ast.Module, class_name: str) -> dict[str, ast.FunctionDef]:
    classes = {x.name: x for x in node.body if isinstance(x, ast.ClassDef)}
    return {x.name: x for x in classes[class_name].body if isinstance(x, ast.FunctionDef)}

NULL = object()
RAW_ARGS_KWARGS_BUILTINS = {'max', 'min'}
RUNTIME_SPEC: Optional[dict[str, object]] = None
DIRECT_CALL_POSITIONAL_BUILTINS: dict[str, tuple[int, int]] = {}
DIRECT_NEWOBJ_POSITIONAL_BUILTIN_TYPES: dict[str, tuple[int, int]] = {}
DIRECT_NEWOBJ_POSITIONAL_SUPPORTED_BUILTIN_TYPES = {
    'bool',
    'bytearray',
    'bytes',
    'enumerate',
    'float',
    'int',
    'list',
    'range',
    'reversed',
    'set',
    'slice',
    'str',
    'tuple',
}
DIRECT_GETATTR_BUILTIN_TYPE_ATTRS: dict[str, dict[str, str]] = {}
DIRECT_GETATTR_IDENTITY_KINDS = {'string', 'member', 'getset', 'method'}
DIRECT_GETATTR_BUILTIN_MODULE_ATTRS: dict[str, dict[str, str]] = {}
DIRECT_CALL_POSITIONAL_MODULE_FUNCTIONS: dict[str, dict[str, tuple[int, int]]] = {}

PYTHON_AUTHORED_IMPLS = {
    'builtins': {'abs', 'all', 'any', 'bin', 'delattr', 'format', 'getattr', 'hash', 'hasattr', 'isinstance', 'issubclass', 'len', 'next', 'oct', 'repr', 'setattr', 'sum'},
    'bytes': {'capitalize', 'count', 'endswith', 'find', 'fromhex', 'hex', 'index', 'isalnum', 'isalpha', 'isascii', 'isdigit', 'islower', 'isspace', 'istitle', 'isupper', 'lower', 'lstrip', 'partition', 'removeprefix', 'removesuffix', 'rfind', 'rindex', 'rpartition', 'rstrip', 'startswith', 'strip', 'swapcase', 'title', 'upper'},
    'dict': {'fromkeys', 'setdefault'},
    'float': {'conjugate'},
    'int': {'as_integer_ratio', 'conjugate', 'is_integer'},
    'range': {'count'},
    'str': {'removeprefix', 'removesuffix'},
}
PYTHON_AUTHORED_CONSTRUCTOR_IMPLS = {'enumerate', 'zip'}

def make_param(name: str, default: object = inspect.Parameter.empty) -> inspect.Parameter:
    return inspect.Parameter(name, inspect.Parameter.POSITIONAL_ONLY, default=default)

def java_local_name(name: str) -> str:
    if name in ir.JAVA_FORBIDDEN_IDENTIFIERS:
        return f'pyarg_{name}'
    return name

def is_constant_default(default: object) -> bool:
    if isinstance(default, (types.NoneType, bool, int, float, str, bytes)):
        return True
    if isinstance(default, tuple):
        return all(is_constant_default(x) for x in default)
    return False

def decode_default(spec: dict[str, object]) -> object:
    kind = spec['kind']
    if kind == '__pythonj_null__':
        return NULL
    if kind == 'none':
        return None
    if kind in {'bool', 'int', 'float', 'str'}:
        return spec['value']
    if kind == 'bytes':
        return cast(str, spec['value']).encode('latin1')
    if kind == 'tuple':
        return tuple(decode_default(cast(dict[str, object], x)) for x in cast(list[object], spec['items']))
    assert False, spec

def decode_param_kind(kind: str) -> inspect._ParameterKind:
    if kind == 'posonly':
        return inspect.Parameter.POSITIONAL_ONLY
    if kind == 'poskw':
        return inspect.Parameter.POSITIONAL_OR_KEYWORD
    if kind == 'kwonly':
        return inspect.Parameter.KEYWORD_ONLY
    if kind == 'vararg':
        return inspect.Parameter.VAR_POSITIONAL
    if kind == 'varkw':
        return inspect.Parameter.VAR_KEYWORD
    assert False, kind

def decode_signature(spec: Optional[dict[str, object]]) -> Optional[list[inspect.Parameter]]:
    if spec is None:
        return None
    params = []
    for raw_param in cast(list[object], spec['params']):
        param = cast(dict[str, object], raw_param)
        default = inspect.Parameter.empty
        if 'default' in param:
            default = decode_default(cast(dict[str, object], param['default']))
        params.append(inspect.Parameter(
            cast(str, param['name']),
            decode_param_kind(cast(str, param['kind'])),
            default=default,
        ))
    return params

def get_builtin_type_attr_expr(type_name: str, attr_name: str, attr_kind: str, receiver: Optional[ir.Expr]) -> ir.Expr:
    assert type_name in extract_spec.BUILTIN_TYPES, type_name
    java_name = extract_spec.BUILTIN_TYPES[type_name]
    attr_expr = ir.Field(ir.Identifier(f'{java_name}Type'), f'pyattr_{attr_name}')
    if receiver is None:
        if attr_kind in DIRECT_GETATTR_IDENTITY_KINDS:
            return attr_expr
        return ir.MethodCall(attr_expr, 'get', [ir.Null()])
    if attr_kind == 'string':
        return attr_expr
    return ir.MethodCall(attr_expr, 'get', [receiver])

def infer_exact_builtin_type(node: ast.expr) -> Optional[str]:
    if isinstance(node, ast.Constant):
        type_name = type(node.value).__name__
    elif isinstance(node, (ast.List, ast.ListComp)):
        type_name = 'list'
    elif isinstance(node, ast.Tuple):
        type_name = 'tuple'
    elif isinstance(node, (ast.Set, ast.SetComp)):
        type_name = 'set'
    elif isinstance(node, (ast.Dict, ast.DictComp)):
        type_name = 'dict'
    elif isinstance(node, ast.JoinedStr):
        type_name = 'str'
    else:
        return None
    return type_name if type_name in extract_spec.BUILTIN_TYPES else None

SUPPORTED_HELPER_RETURN_TYPES = {'bool', 'bytes', 'float', 'int', 'list', 'str', 'tuple'}

def decode_helper_return_annotation(annotation: Optional[ast.expr]) -> str:
    if annotation is None:
        return 'PyObject'
    if isinstance(annotation, ast.Constant) and annotation.value is None:
        return 'PyNone'
    if isinstance(annotation, ast.Name) and annotation.id == 'NoReturn':
        return 'NoReturn'
    if isinstance(annotation, ast.Name) and annotation.id in extract_spec.EXCEPTION_TYPES:
        return f'Py{annotation.id}'
    if isinstance(annotation, ast.Name) and annotation.id in SUPPORTED_HELPER_RETURN_TYPES:
        return extract_spec.BUILTIN_TYPES[annotation.id]
    if isinstance(annotation, ast.Attribute) and annotation.attr == 'NoReturn' and isinstance(annotation.value, ast.Name) and annotation.value.id == 'typing':
        return 'NoReturn'
    raise ValueError(f'unsupported helper return annotation: {ast.dump(annotation)}')

@dataclass(slots=True)
class SignatureShape:
    params: list[inspect.Parameter]
    posonly_params: list[inspect.Parameter]
    poskw_params: list[inspect.Parameter]
    kwonly_params: list[inspect.Parameter]
    vararg_param: Optional[inspect.Parameter]
    varkw_param: Optional[inspect.Parameter]
    max_total: int
    max_positional: int
    missing_style: Optional[str]

def analyze_params(params: list[inspect.Parameter]) -> SignatureShape:
    posonly_params = [param for param in params if param.kind is inspect.Parameter.POSITIONAL_ONLY]
    poskw_params = [param for param in params if param.kind is inspect.Parameter.POSITIONAL_OR_KEYWORD]
    kwonly_params = [param for param in params if param.kind is inspect.Parameter.KEYWORD_ONLY]
    vararg_params = [param for param in params if param.kind is inspect.Parameter.VAR_POSITIONAL]
    vararg_param = vararg_params[0] if vararg_params else None
    varkw_params = [param for param in params if param.kind is inspect.Parameter.VAR_KEYWORD]
    varkw_param = varkw_params[0] if varkw_params else None
    max_total = len(params)
    max_positional = len(posonly_params) + len(poskw_params)
    if poskw_params and poskw_params[0].default is inspect.Parameter.empty:
        missing_style = 'required_arg'
    elif posonly_params and posonly_params[0].default is inspect.Parameter.empty:
        if poskw_params:
            missing_style = 'min_positional'
        else:
            missing_style = 'exact_args'
    else:
        missing_style = None
    return SignatureShape(params, posonly_params, poskw_params, kwonly_params, vararg_param, varkw_param, max_total, max_positional, missing_style)

def get_method_params(spec: dict[str, object], name: str, method_name: str) -> Optional[list[inspect.Parameter]]:
    attrs = cast(dict[str, dict[str, object]], cast(dict[str, object], spec[name])['attrs'])
    return decode_signature(cast(Optional[dict[str, object]], attrs[method_name].get('signature')))

def get_builtin_function_params(spec: dict[str, object], name: str) -> Optional[list[inspect.Parameter]]:
    attrs = cast(dict[str, dict[str, object]], cast(dict[str, object], spec['builtins'])['attrs'])
    return decode_signature(cast(Optional[dict[str, object]], attrs[name].get('signature')))

def get_module_function_params(spec: dict[str, object], module_name: str, func_name: str) -> Optional[list[inspect.Parameter]]:
    attrs = cast(dict[str, dict[str, object]], cast(dict[str, object], spec[module_name])['attrs'])
    return decode_signature(cast(Optional[dict[str, object]], attrs[func_name].get('signature')))

def is_special_method_wrapper(type_name: str, method_name: str) -> bool:
    return type_name == 'type' and method_name == 'mro'

def emit_python_function_static(visitor: LoweringVisitor, func_name: str, arg_names: list[str], body: list[ir.Statement],
                                return_java_type: str) -> ir.MethodDecl:
    java_return_type = 'PyNone' if return_java_type == 'NoReturn' else return_java_type
    func_code: list[ir.Statement] = []
    if visitor.scope.used_expr_discard:
        func_code.append(ir.LocalDecl('PyObject', 'expr_discard', None))
    for arg_name in arg_names:
        if arg_name in visitor.scope.info.cell_vars:
            func_code.append(ir.LocalDecl('PyCell', f'pycell_{arg_name}', ir.CreateObject('PyCell', [ir.Identifier(f'pyarg_{arg_name}')])))
        else:
            func_code.append(ir.LocalDecl(visitor.java_local_type(arg_name), f'pylocal_{arg_name}', visitor.cast_local_assignment(arg_name, ir.Identifier(f'pyarg_{arg_name}'))))
    for name in sorted(visitor.scope.info.cell_vars - set(arg_names)):
        func_code.append(ir.LocalDecl('PyCell', f'pycell_{name}', ir.CreateObject('PyCell', [ir.PyConstant(None)])))
    for name in sorted(visitor.scope.info.locals - visitor.scope.info.cell_vars - set(arg_names) - set(visitor.scope.info.initial_builtin_module_locals)):
        func_code.append(ir.LocalDecl(visitor.java_local_type(name), f'pylocal_{name}', None))
    if return_java_type == 'NoReturn':
        func_code.extend(ir.block_simplify(body))
    else:
        implicit_return: ir.Statement = ir.ReturnStatement(ir.PyConstant(None))
        if return_java_type != 'PyObject':
            implicit_return = ir.ReturnStatement(ir.CastExpr(return_java_type, ir.PyConstant(None)))
        func_code.extend(ir.block_simplify([*body, implicit_return]))
    return ir.MethodDecl('static', java_return_type, f'pyfunc_{func_name}', [*(f'PyObject pyarg_{arg}' for arg in arg_names)], func_code)

def translate_python_impl_node(node: ast.Module, func: ast.FunctionDef, emitted_name: str, display_name: str,
                               role: str, pool: ir.ConstantPool,
                               scope_infos: Optional[dict[ast.AST, ScopeInfo]] = None,
                               python_helper_names: Optional[set[str]] = None,
                               python_helper_class: Optional[str] = None) -> tuple[list[ir.ClassDecl], ir.MethodDecl]:
    if scope_infos is None:
        analyzer = ScopeAnalyzer()
        analyzer.visit(node)
        scope_infos = analyzer.scope_infos
    visitor = LoweringVisitor(display_name, scope_infos, scope_infos[node])
    visitor.pool = pool
    visitor.allow_intrinsics = True
    if python_helper_names is not None:
        visitor.python_helper_names = python_helper_names
        assert python_helper_class is not None, python_helper_class
        visitor.python_helper_class = python_helper_class
    try:
        return_java_type = decode_helper_return_annotation(func.returns)
    except ValueError as e:
        visitor.error(func.lineno, str(e))
        return_java_type = 'PyObject'

    (arg_names, arg_defaults) = visitor.check_args(func.lineno, func.args)
    assert not arg_defaults, (emitted_name, arg_defaults)
    scope_info = visitor.scope_infos[func]
    free_vars = visitor.resolve_free_vars(func.lineno, scope_info, role)
    assert not free_vars, (emitted_name, free_vars)
    with visitor.new_function(scope_info, free_vars, func.name, return_java_type):
        visitor.report_annotation_errors()
        body = visitor.visit_block(func.body)
        if return_java_type == 'NoReturn' and not ir.block_ends_control_flow(body):
            visitor.error(func.lineno, f'NoReturn helper function {func.name} may implicitly fall through')
        elif return_java_type not in {'PyObject', 'PyNone'} and not ir.block_ends_control_flow(body):
            visitor.error(func.lineno, f'annotated function {func.name} may implicitly return None')
        helper_method = emit_python_function_static(visitor, emitted_name, arg_names, body, return_java_type)
    assert visitor.n_errors == 0, (emitted_name, visitor.n_errors)
    return (list(visitor.classes.values()), helper_method)

def translate_python_builtin_impl(node: ast.Module, func_name: str, pool: ir.ConstantPool,
                                  funcs: Optional[dict[str, ast.FunctionDef]] = None,
                                  scope_infos: Optional[dict[ast.AST, ScopeInfo]] = None) -> tuple[list[ir.ClassDecl], ir.MethodDecl]:
    if funcs is None:
        funcs = get_top_level_functions(node)
    func = funcs[func_name]
    return translate_python_impl_node(node, func, func_name, f'<builtin {func_name}>', 'builtin function', pool, scope_infos=scope_infos)

def translate_python_method_impl(node: ast.Module, type_name: str, method_name: str, pool: ir.ConstantPool,
                                 class_funcs: Optional[dict[str, dict[str, ast.FunctionDef]]] = None,
                                 scope_infos: Optional[dict[ast.AST, ScopeInfo]] = None) -> tuple[list[ir.ClassDecl], ir.MethodDecl]:
    func_name = f'{type_name}__{method_name}'
    if class_funcs is None:
        func = get_class_functions(node, type_name)[method_name]
    else:
        func = class_funcs[type_name][method_name]
    return translate_python_impl_node(node, func, func_name, f'<method {type_name}.{method_name}>', 'builtin method', pool, scope_infos=scope_infos)

def translate_python_constructor_impl(node: ast.Module, type_name: str, pool: ir.ConstantPool,
                                      funcs: Optional[dict[str, ast.FunctionDef]] = None,
                                      scope_infos: Optional[dict[ast.AST, ScopeInfo]] = None) -> tuple[list[ir.ClassDecl], ir.MethodDecl]:
    if funcs is None:
        funcs = get_top_level_functions(node)
    func_name = f'{type_name}__newobj'
    func = funcs[func_name]
    emitted_name = f'{func_name}_impl'
    return translate_python_impl_node(node, func, emitted_name, f'<constructor {type_name}>', 'builtin constructor', pool, scope_infos=scope_infos)

def translate_python_runtime_impl(node: ast.Module, func_name: str, pool: ir.ConstantPool,
                                  funcs: Optional[dict[str, ast.FunctionDef]] = None,
                                  scope_infos: Optional[dict[ast.AST, ScopeInfo]] = None) -> tuple[list[ir.ClassDecl], ir.MethodDecl]:
    if funcs is None:
        funcs = get_top_level_functions(node)
    return translate_python_impl_node(
        node, funcs[func_name], func_name, f'<runtime {func_name}>', 'runtime helper', pool,
        scope_infos=scope_infos,
        python_helper_names=set(funcs), python_helper_class='PyRuntime',
    )

def emit_default_java_expr(default: object) -> ir.Expr:
    if default is NULL:
        return ir.Null()
    return ir.PyConstant(default)

def runtime_throw(method: str, args_: list[ir.Expr]) -> ir.ThrowStatement:
    return ir.ThrowStatement(ir.MethodCall(ir.Identifier('Runtime'), method, args_))

def build_arg_binding_ir(shape: SignatureShape, positional_name: str,
                         kw_name: str, kw_overflow_args_length: str,
                         noarg_name: str, preserve_optional_nulls: bool = False) -> tuple[list[ir.Statement], list[ir.Expr]]:
    args = ir.Identifier('args')
    kwargs = ir.Identifier('kwargs')
    kwargs_bool = ir.BinaryOp('&&',
        ir.BinaryOp('!=', kwargs, ir.Null()),
        ir.MethodCall(kwargs, 'boolValue', []),
    )

    if not shape.posonly_params and not shape.poskw_params and not shape.kwonly_params and \
        shape.vararg_param is not None and shape.varkw_param is not None:
        return ([], [args, kwargs])
    if not shape.posonly_params and not shape.poskw_params and not shape.kwonly_params and shape.vararg_param is not None:
        return ([
            ir.IfStatement(kwargs_bool, [runtime_throw('raiseNoKwArgs', [ir.StrLiteral(noarg_name)])], []),
        ], [args])
    assert False, (shape, positional_name, kw_name, kw_overflow_args_length, noarg_name, preserve_optional_nulls)

def get_java_name(name: str) -> str:
    if name.startswith('_io.'):
        return f"Py{name.split('.', 1)[1]}" # _io.Foo -> PyFoo + PyFooType
    elif name == 'types.BuiltinFunctionType':
        return 'PyBuiltinFunctionOrMethod' # weird shared type
    elif name.startswith('types.') and name.endswith('Type'):
        return f"Py{name[:-4].split('.', 1)[1]}" # types.FooType -> PyFoo + PyFooType
    elif name in extract_spec.EXCEPTION_TYPES:
        return f'Py{name}'
    else:
        return extract_spec.BUILTIN_TYPES[name]

def get_builtin_module_attr_expr(module_name: str, attr_name: str, kind: str) -> ir.Expr:
    if kind == 'builtin_function':
        module_java_name = extract_spec.BUILTIN_MODULES[module_name]
        module_func_prefix = module_java_name.removesuffix('Module')
        return ir.Field(ir.Identifier(f'{module_func_prefix}Function_{attr_name}'), 'singleton')
    if kind == 'type':
        return ir.Field(ir.Identifier(f'{get_java_name(f"{module_name}.{attr_name}")}Type'), 'singleton')
    assert False, (module_name, attr_name, kind)

def get_positional_call_range(params: Optional[list[inspect.Parameter]]) -> Optional[tuple[int, int]]:
    if params is None:
        return None
    if not all(param.kind in {inspect.Parameter.POSITIONAL_ONLY, inspect.Parameter.POSITIONAL_OR_KEYWORD} for param in params):
        return None
    min_args = sum(param.default is inspect.Parameter.empty for param in params)
    return (min_args, len(params))

def get_exact_positional_call_arity(params: Optional[list[inspect.Parameter]]) -> Optional[int]:
    call_range = get_positional_call_range(params)
    if call_range is None:
        return None
    (min_args, max_args) = call_range
    if min_args != max_args:
        return None
    return max_args

def get_posonly_min_max_call_range(params: Optional[list[inspect.Parameter]]) -> Optional[tuple[int, int]]:
    if params is None:
        return None
    if not params:
        return None
    if not all(param.kind is inspect.Parameter.POSITIONAL_ONLY for param in params):
        return None
    call_range = get_positional_call_range(params)
    assert call_range is not None
    (min_args, max_args) = call_range
    if min_args == max_args:
        return None
    return (min_args, max_args)

def get_wrapper_binding_plan(shape: Optional[SignatureShape]) -> WrapperBindingPlan:
    call_positional_shape = None
    if shape is not None and shape.varkw_param is None and (shape.vararg_param is None or (not shape.posonly_params and not shape.poskw_params)):
        call_positional_shape = shape

    exact_positional_arity = None if shape is None else get_exact_positional_call_arity(shape.params)
    if exact_positional_arity is not None:
        return WrapperBindingPlan('exact_positional', call_positional_shape, exact_positional_arity=exact_positional_arity)

    posonly_min_max_range = None if shape is None else get_posonly_min_max_call_range(shape.params)
    if posonly_min_max_range is not None:
        return WrapperBindingPlan('posonly_min_max', call_positional_shape, posonly_min_max_range=posonly_min_max_range)

    if shape is None:
        return WrapperBindingPlan('fallback_raw', call_positional_shape)

    call_range = get_positional_call_range(shape.params)
    if (
        shape.poskw_params and
        not shape.kwonly_params and
        shape.vararg_param is None and
        shape.varkw_param is None and
        call_range is not None and
        call_range[0] != call_range[1]
    ):
        return WrapperBindingPlan('poskw_min_max_python', call_positional_shape)

    if (
        shape.kwonly_params and
        shape.vararg_param is None and
        shape.varkw_param is None and
        all(param.kind in {
            inspect.Parameter.POSITIONAL_ONLY,
            inspect.Parameter.POSITIONAL_OR_KEYWORD,
            inspect.Parameter.KEYWORD_ONLY,
        } for param in shape.params) and
        all(param.default is not inspect.Parameter.empty for param in shape.kwonly_params)
    ):
        return WrapperBindingPlan('pos_kwonly_python', call_positional_shape)

    if (
        shape.vararg_param is not None and
        shape.kwonly_params and
        shape.varkw_param is None and
        not shape.posonly_params and
        not shape.poskw_params and
        all(param.default is not inspect.Parameter.empty for param in shape.kwonly_params)
    ):
        return WrapperBindingPlan('vararg_kwonly_python', call_positional_shape)

    return WrapperBindingPlan('fallback_bound', call_positional_shape)

def build_wrapper_binding_ir(
        kwarg_params: Optional[SignatureShape], *,
        exact_kw_name: str,
        exact_positional_name_many: str,
        posonly_kw_name: str,
        posonly_positional_name: str,
        poskw_kw_name: str,
        poskw_positional_name: str,
        general_positional_name: str,
        general_kw_name: str,
        kw_overflow_args_length: str,
        noarg_name: str,
) -> tuple[list[ir.Statement], list[ir.Expr], Optional[SignatureShape]]:
    plan = get_wrapper_binding_plan(kwarg_params)
    statements: list[ir.Statement] = []
    bind_args: list[ir.Expr]

    if plan.mode == 'exact_positional':
        assert plan.exact_positional_arity is not None
        exact_positional_name = exact_kw_name if plan.exact_positional_arity <= 1 else exact_positional_name_many
        statements.append(ir.method_call_statement(ir.Identifier('PyRuntime'), 'pyfunc_require_exact_positional', [
            ir.CreateObject('PyInt', [ir.Field(ir.Identifier('args'), 'length')]),
            ir.Identifier('kwargs'),
            ir.PyConstant(exact_kw_name),
            ir.PyConstant(exact_positional_name),
            ir.PyConstant(plan.exact_positional_arity),
            ir.PyConstant(False),
        ]))
        bind_args = [ir.ArrayAccess(ir.Identifier('args'), ir.IntLiteral(i)) for i in range(plan.exact_positional_arity)]
    elif plan.mode == 'posonly_min_max':
        assert plan.posonly_min_max_range is not None
        statements.append(ir.method_call_statement(ir.Identifier('PyRuntime'), 'pyfunc_require_min_max_positional', [
            ir.CreateObject('PyInt', [ir.Field(ir.Identifier('args'), 'length')]),
            ir.Identifier('kwargs'),
            ir.PyConstant(posonly_kw_name),
            ir.PyConstant(posonly_positional_name),
            ir.PyConstant(plan.posonly_min_max_range[0]),
            ir.PyConstant(plan.posonly_min_max_range[1]),
        ]))
        bind_args = [ir.ArrayAccess(ir.Identifier('args'), ir.IntLiteral(i)) for i in range(plan.posonly_min_max_range[0])]
        bind_args.extend(
            ir.CondOp(
                ir.BinaryOp('>', ir.Field(ir.Identifier('args'), 'length'), ir.IntLiteral(i)),
                ir.ArrayAccess(ir.Identifier('args'), ir.IntLiteral(i)),
                ir.Null(),
            )
            for i in range(plan.posonly_min_max_range[0], plan.posonly_min_max_range[1])
        )
    elif plan.mode == 'poskw_min_max_python':
        assert kwarg_params is not None
        (min_args, max_args) = cast(tuple[int, int], get_positional_call_range(kwarg_params.params))
        statements.append(ir.LocalDecl('var', 'boundArgs',
            ir.MethodCall(ir.Identifier('Runtime'), 'bindMinMaxPositionalOrKeyword', [
                ir.Identifier('args'),
                ir.Identifier('kwargs'),
                ir.PyConstant(poskw_kw_name),
                ir.PyConstant(poskw_positional_name),
                ir.PyConstant(tuple(param.name for param in kwarg_params.posonly_params + kwarg_params.poskw_params)),
                ir.PyConstant(len(kwarg_params.posonly_params)),
                ir.PyConstant(tuple()),
                ir.PyConstant(min_args),
                ir.PyConstant(max_args),
                ir.PyConstant(max_args),
                ir.PyConstant(kwarg_params.missing_style == 'min_positional'),
                ir.PyConstant(False),
            ]),
        ))
        bind_args = [ir.MethodCall(ir.Identifier('boundArgs'), 'get', [ir.IntLiteral(i)]) for i in range(max_args)]
    elif plan.mode == 'pos_kwonly_python':
        assert kwarg_params is not None
        statements.append(ir.LocalDecl('var', 'boundArgs',
            ir.MethodCall(ir.Identifier('Runtime'), 'bindMinMaxPositionalOrKeyword', [
                ir.Identifier('args'),
                ir.Identifier('kwargs'),
                ir.PyConstant(poskw_kw_name),
                ir.PyConstant(poskw_positional_name),
                ir.PyConstant(tuple(param.name for param in kwarg_params.posonly_params + kwarg_params.poskw_params)),
                ir.PyConstant(len(kwarg_params.posonly_params)),
                ir.PyConstant(tuple(param.name for param in kwarg_params.kwonly_params)),
                ir.PyConstant(sum(param.default is inspect.Parameter.empty for param in kwarg_params.posonly_params + kwarg_params.poskw_params)),
                ir.PyConstant(len(kwarg_params.posonly_params) + len(kwarg_params.poskw_params)),
                ir.PyConstant(len(kwarg_params.params)),
                ir.PyConstant(kwarg_params.missing_style == 'min_positional'),
                ir.PyConstant(kwarg_params.missing_style == 'exact_args'),
            ]),
        ))
        bind_args = [ir.MethodCall(ir.Identifier('boundArgs'), 'get', [ir.IntLiteral(i)]) for i in range(len(kwarg_params.params))]
    elif plan.mode == 'vararg_kwonly_python':
        assert kwarg_params is not None
        statements.append(ir.LocalDecl('PyList', 'boundArgs',
            ir.MethodCall(ir.Identifier('PyRuntime'), 'pyfunc_bind_varargs_and_kwonly', [
                ir.Identifier('kwargs'),
                ir.PyConstant(general_kw_name),
                ir.PyConstant(tuple(param.name for param in kwarg_params.kwonly_params)),
            ]),
        ))
        bind_args = [
            ir.Identifier('args'),
            *(ir.MethodCall(ir.Field(ir.Identifier('boundArgs'), 'items'), 'get', [ir.IntLiteral(i)]) for i in range(len(kwarg_params.kwonly_params))),
        ]
    elif plan.mode == 'fallback_raw':
        bind_args = [ir.Identifier('args'), ir.Identifier('kwargs')]
    else:
        assert kwarg_params is not None
        (statements, bind_args) = build_arg_binding_ir(
            kwarg_params, general_positional_name,
            general_kw_name,
            kw_overflow_args_length,
            noarg_name,
            preserve_optional_nulls=plan.call_positional_shape is not None,
        )

    return (statements, bind_args, plan.call_positional_shape)

def build_call_positional_ir(shape: SignatureShape) -> tuple[list[str], list[ir.Statement], list[ir.Expr]]:
    assert shape.varkw_param is None, shape
    if shape.vararg_param is not None:
        assert not shape.posonly_params and not shape.poskw_params, shape
    call_args: list[str] = []
    statements: list[ir.Statement] = []
    bind_args: list[ir.Expr] = []
    for (i, param) in enumerate(shape.params):
        if param.kind is inspect.Parameter.VAR_POSITIONAL:
            call_args.append(f'PyObject[] arg{i}')
            bind_args.append(ir.Identifier(f'arg{i}'))
            continue
        call_args.append(f'PyObject arg{i}')
        if param.default is not inspect.Parameter.empty:
            local = ir.Identifier(java_local_name(shape.params[i].name))
            statements.append(ir.LocalDecl('PyObject', java_local_name(shape.params[i].name), ir.Identifier(f'arg{i}')))
            statements.append(ir.IfStatement(
                ir.BinaryOp('==', local, ir.Null()),
                [ir.AssignStatement(local, emit_default_java_expr(shape.params[i].default))],
                [],
            ))
            bind_args.append(local)
        else:
            bind_args.append(ir.Identifier(f'arg{i}'))
    return (call_args, statements, bind_args)

def gen_runtime_java(spec_path: str, java_path: str) -> None:
    global RUNTIME_SPEC
    with open(spec_path) as f:
        spec = json.load(f)
    RUNTIME_SPEC = spec
    for func_name in extract_spec.BUILTIN_FUNCTIONS:
        call_range = get_positional_call_range(get_builtin_function_params(spec, func_name))
        if call_range is not None:
            DIRECT_CALL_POSITIONAL_BUILTINS[func_name] = call_range
    for type_name in extract_spec.BUILTIN_TYPES:
        type_spec = cast(dict[str, object], spec[type_name])
        attrs = cast(dict[str, dict[str, object]], type_spec['attrs'])
        DIRECT_GETATTR_BUILTIN_TYPE_ATTRS[type_name] = {
            attr_name: cast(str, attr_spec['kind']) for (attr_name, attr_spec) in attrs.items()
        }
        if type_name in DIRECT_NEWOBJ_POSITIONAL_SUPPORTED_BUILTIN_TYPES:
            type_signature = cast(Optional[dict[str, object]], type_spec.get('signature'))
            if type_signature is not None:
                params = decode_signature(type_signature)
                if params is not None:
                    call_range = get_positional_call_range(params)
                    if call_range is not None:
                        DIRECT_NEWOBJ_POSITIONAL_BUILTIN_TYPES[type_name] = call_range
    for module_name in extract_spec.BUILTIN_MODULES:
        module_spec = cast(dict[str, object], spec[module_name])
        attrs = cast(dict[str, dict[str, object]], module_spec['attrs'])
        DIRECT_GETATTR_BUILTIN_MODULE_ATTRS[module_name] = {
            attr_name: cast(str, attr_spec['kind']) for (attr_name, attr_spec) in attrs.items()
        }
    for module_name in extract_spec.BUILTIN_MODULES:
        DIRECT_CALL_POSITIONAL_MODULE_FUNCTIONS[module_name] = {}
        for func_name in cast(dict[str, dict[str, object]], cast(dict[str, object], spec[module_name])['attrs']):
            call_range = get_positional_call_range(get_module_function_params(spec, module_name, func_name))
            if call_range is not None:
                DIRECT_CALL_POSITIONAL_MODULE_FUNCTIONS[module_name][func_name] = call_range

    pythonj_builtins_node = parse_python_module('pythonj_builtins.py')
    pythonj_runtime_node = parse_python_module('pythonj_runtime.py')
    pythonj_builtins_funcs = get_top_level_functions(pythonj_builtins_node)
    pythonj_builtins_classes = {
        class_name: {x.name: x for x in class_node.body if isinstance(x, ast.FunctionDef)}
        for (class_name, class_node) in {x.name: x for x in pythonj_builtins_node.body if isinstance(x, ast.ClassDef)}.items()
    }
    pythonj_runtime_funcs = get_top_level_functions(pythonj_runtime_node)
    builtins_analyzer = ScopeAnalyzer()
    builtins_analyzer.visit(pythonj_builtins_node)
    runtime_analyzer = ScopeAnalyzer()
    runtime_analyzer.visit(pythonj_runtime_node)

    pool = ir.ConstantPool('PyGeneratedConstants')
    with open(java_path, 'w') as f:
        top_level_decls: list[ir.Decl] = []
        python_runtime_helper_classes: list[ir.ClassDecl] = []
        python_runtime_helper_methods: list[ir.Decl] = []
        python_builtin_helper_classes: list[ir.ClassDecl] = []
        python_builtin_helper_methods: list[ir.Decl] = []
        python_constructor_helper_classes: list[ir.ClassDecl] = []
        python_constructor_helper_methods: list[ir.Decl] = []
        python_method_helper_classes: list[ir.ClassDecl] = []
        python_method_helper_methods: list[ir.Decl] = []
        for (name, obj_spec) in spec.items():
            if obj_spec['kind'] != 'type':
                continue
            attrs = obj_spec['attrs']
            java_name = get_java_name(name)
            match name:
                case 'types.BuiltinFunctionType': py_name = 'builtin_function_or_method'
                case 'types.ClassMethodDescriptorType': py_name = 'classmethod_descriptor'
                case 'types.FunctionType': py_name = 'function'
                case 'types.GetSetDescriptorType': py_name = 'getset_descriptor'
                case 'types.MappingProxyType': py_name = 'mappingproxy'
                case 'types.MemberDescriptorType': py_name = 'member_descriptor'
                case 'types.MethodDescriptorType': py_name = 'method_descriptor'
                case 'types.NoneType': py_name = 'NoneType'
                case _: py_name = name

            type_decls: list[ir.Decl] = [
                ir.FieldDecl('public static final', f'{java_name}Type', 'singleton', ir.CreateObject(f'{java_name}Type', [])),
            ]
            for (k, v) in attrs.items():
                doc_value = v.get('doc')
                if v['kind'] == 'string':
                    value = ir.CreateObject('PyString', [ir.StrLiteral(v['value'])])
                elif v['kind'] == 'member':
                    value = ir.CreateObject('PyMemberDescriptor', [
                        ir.Identifier('singleton'),
                        ir.StrLiteral(k),
                        ir.MethodRef(java_name, f'pymember_{k}'),
                        ir.Null() if doc_value is None else ir.StrLiteral(doc_value),
                    ])
                elif v['kind'] == 'getset':
                    value = ir.CreateObject('PyGetSetDescriptor', [
                        ir.Identifier('singleton'),
                        ir.StrLiteral(k),
                        ir.MethodRef(java_name, f'pygetset_{k}'),
                        ir.Null() if doc_value is None else ir.StrLiteral(doc_value),
                    ])
                elif v['kind'] == 'method':
                    value = ir.CreateObject('PyMethodDescriptor', [
                        ir.Identifier('singleton'),
                        ir.StrLiteral(k),
                        ir.MethodRef(f'{java_name}Method_{k}', 'new'),
                        ir.Null() if doc_value is None else ir.StrLiteral(doc_value),
                    ])
                elif v['kind'] == 'classmethod':
                    value = ir.CreateObject('PyClassMethodDescriptor', [
                        ir.Identifier('singleton'),
                        ir.StrLiteral(k),
                        ir.MethodRef(f'{java_name}ClassMethod_{k}', 'new'),
                        ir.Null() if doc_value is None else ir.StrLiteral(doc_value),
                    ])
                elif v['kind'] == 'staticmethod':
                    value = ir.CreateObject('PyStaticMethod', [
                        ir.Identifier('singleton'),
                        ir.StrLiteral(k),
                        ir.CreateObject(f'{java_name}StaticMethod_{k}', [ir.Identifier('singleton')]),
                    ])
                else:
                    assert False, (name, k, v)
                type_decls.append(ir.FieldDecl('static final', value.type, f'pyattr_{k}', value))
            type_decls.append(ir.ClassDecl('private static final', 'AttrsHolder', None, [
                ir.FieldDecl(
                    'static final',
                    'java.util.LinkedHashMap<PyObject, PyObject>',
                    'attrs',
                    ir.CreateObject('java.util.LinkedHashMap<PyObject, PyObject>', [ir.IntLiteral(len(attrs))]),
                ),
                ir.StaticBlock([
                    ir.method_call_statement(
                        ir.Identifier('attrs'),
                        'put',
                        [ir.CreateObject('PyString', [ir.StrLiteral(k)]), ir.Identifier(f'pyattr_{k}')],
                    )
                    for k in attrs
                ]),
            ]))
            type_decls.append(ir.ConstructorDecl('private', f'{java_name}Type', [], [
                ir.SuperConstructorCall([
                    ir.StrLiteral(py_name),
                    ir.Field(ir.Identifier(java_name), 'class'),
                    ir.MethodRef('PyBuiltinConstructorsPythonImpl', f'pyfunc_{name}__newobj') if name in PYTHON_AUTHORED_CONSTRUCTOR_IMPLS else ir.MethodRef(java_name, 'newObj'),
                ]),
            ]))
            type_decls.append(ir.MethodDecl('public', 'java.util.Map<PyObject, PyObject>', 'getAttributes', [], [
                ir.ReturnStatement(ir.Field(ir.Identifier('AttrsHolder'), 'attrs')),
            ]))
            type_decls.append(ir.MethodDecl('@Override public', 'PyObject', 'lookupAttr', ['String name'], [
                ir.SwitchStatement(ir.Identifier('name'), [
                    ir.SwitchCase(ir.StrLiteral(k), ir.Identifier(f'pyattr_{k}'))
                    for k in attrs
                ], ir.Null()),
            ]))
            top_level_decls.append(ir.ClassDecl('final', f'{java_name}Type', 'PyConcreteType', type_decls))

            if name == 'type':
                for method_name in ['mro']:
                    top_level_decls.append(ir.ClassDecl(
                        'final',
                        f'{java_name}Method_{method_name}',
                        f'PyBuiltinMethod<{java_name}>',
                        [
                            ir.ConstructorDecl('', f'{java_name}Method_{method_name}', ['PyObject _self'], [
                                ir.SuperConstructorCall([ir.CastExpr(java_name, ir.Identifier('_self'))]),
                            ]),
                            ir.MethodDecl('public', 'String', 'methodName', [], [
                                ir.ReturnStatement(ir.StrLiteral(method_name)),
                            ]),
                            ir.MethodDecl('public', 'PyObject', 'call', ['PyObject[] args', 'PyDict kwargs'], [
                                ir.ThrowStatement(ir.CreateObject('UnsupportedOperationException', [ir.StrLiteral(f'{name}.{method_name}() unimplemented')])),
                            ]),
                        ],
                    ))

            gen_methods = {}
            for (method_name, v) in attrs.items():
                if v['kind'] not in {'method', 'classmethod', 'staticmethod'}:
                    continue
                if is_special_method_wrapper(name, method_name):
                    continue
                params = get_method_params(spec, name, method_name)
                if params is None:
                    gen_methods[method_name] = None
                else:
                    gen_methods[method_name] = analyze_params(params)

            if gen_methods:
                if '.' in name:
                    py_name = name.rsplit('.')[1]
                else:
                    py_name = name
                for (method_name, kwarg_params) in gen_methods.items():
                    kind = attrs[method_name]['kind']
                    if kind == 'method':
                        method_class_name = f'{java_name}Method_{method_name}'
                        self_type = java_name
                        ctor_arg = 'PyObject _self'
                        super_arg = f'({java_name})_self'
                        method_target = 'self'
                    elif kind == 'classmethod':
                        method_class_name = f'{java_name}ClassMethod_{method_name}'
                        self_type = 'PyType'
                        ctor_arg = 'PyType _self'
                        super_arg = '_self'
                        method_target = java_name
                    elif kind == 'staticmethod':
                        method_class_name = f'{java_name}StaticMethod_{method_name}'
                        self_type = 'PyType'
                        ctor_arg = 'PyType _self'
                        super_arg = '_self'
                        method_target = java_name
                    else:
                        assert False, (name, method_name, kind)
                    method_impl_target = None
                    if kind in {'method', 'classmethod'} and method_name in PYTHON_AUTHORED_IMPLS.get(name, set()):
                        helper_name = f'{name.replace(".", "_")}__{method_name}'
                        method_impl_target = f'PyBuiltinMethodsPythonImpl.pyfunc_{helper_name}'
                        (helper_classes, helper_method) = translate_python_method_impl(
                            pythonj_builtins_node, name, method_name, pool,
                            class_funcs=pythonj_builtins_classes, scope_infos=builtins_analyzer.scope_infos,
                        )
                        python_method_helper_classes.extend(helper_classes)
                        python_method_helper_methods.append(helper_method)
                    decls: list[ir.Decl] = [
                        ir.ConstructorDecl('', method_class_name, [ctor_arg], [
                            ir.SuperConstructorCall([ir.Identifier(super_arg) if super_arg == '_self' else ir.CastExpr(java_name, ir.Identifier('_self'))]),
                        ]),
                        ir.MethodDecl('public', 'String', 'methodName', [], [ir.ReturnStatement(ir.StrLiteral(method_name))]),
                    ]
                    bind_name = f'{py_name}.{method_name}'
                    (call_body, bind_args, call_positional_shape) = build_wrapper_binding_ir(
                        kwarg_params,
                        exact_kw_name=bind_name,
                        exact_positional_name_many=method_name,
                        posonly_kw_name=bind_name,
                        posonly_positional_name=method_name,
                        poskw_kw_name=method_name,
                        poskw_positional_name=method_name,
                        general_positional_name=method_name,
                        general_kw_name=method_name,
                        kw_overflow_args_length='argsLength',
                        noarg_name=bind_name,
                    )
                    if kind == 'classmethod':
                        bind_args = [ir.Identifier('self')] + bind_args
                    if call_positional_shape is not None:
                        call_expr = ir.MethodCall(
                            ir.Identifier(method_class_name),
                            'callPositional',
                            bind_args if kind == 'classmethod' else [ir.Identifier('self'), *bind_args],
                        )
                    elif method_impl_target is not None:
                        call_args = bind_args if kind == 'classmethod' else [ir.Identifier('self'), *bind_args]
                        call_expr = ir.MethodCall(
                            ir.Identifier(method_impl_target.rsplit('.', 1)[0]),
                            method_impl_target.rsplit('.', 1)[1],
                            call_args,
                        )
                    else:
                        call_expr = ir.MethodCall(ir.Identifier(method_target), f'pymethod_{method_name}', bind_args)
                    call_body.append(ir.ReturnStatement(call_expr))
                    decls.append(ir.MethodDecl('public', 'PyObject', 'call', ['PyObject[] args', 'PyDict kwargs'], call_body))
                    if call_positional_shape is not None:
                        (call_positional_method_args, call_positional_statements, call_positional_args) = build_call_positional_ir(call_positional_shape)
                        call_positional_method_args = [f'{self_type} self', *call_positional_method_args]
                        if method_impl_target is not None:
                            call_args = [ir.Identifier('self'), *call_positional_args]
                            call_positional_expr = ir.MethodCall(
                                ir.Identifier(method_impl_target.rsplit('.', 1)[0]),
                                method_impl_target.rsplit('.', 1)[1],
                                call_args,
                            )
                        else:
                            if kind == 'method':
                                call_positional_expr = ir.MethodCall(ir.Identifier('self'), f'pymethod_{method_name}', call_positional_args)
                            elif kind == 'classmethod':
                                call_positional_expr = ir.MethodCall(ir.Identifier(method_target), f'pymethod_{method_name}', [ir.Identifier('self'), *call_positional_args])
                            else:
                                call_positional_expr = ir.MethodCall(ir.Identifier(method_target), f'pymethod_{method_name}', call_positional_args)
                        decls.append(ir.MethodDecl(
                            'public static',
                            'PyObject',
                            'callPositional',
                            call_positional_method_args,
                            [*call_positional_statements, ir.ReturnStatement(call_positional_expr)],
                        ))
                    top_level_decls.append(ir.ClassDecl('final', method_class_name, f'PyBuiltinMethod<{self_type}>', decls))

        for func_name in sorted(pythonj_runtime_funcs):
            (helper_classes, helper_method) = translate_python_runtime_impl(
                pythonj_runtime_node, func_name, pool, funcs=pythonj_runtime_funcs, scope_infos=runtime_analyzer.scope_infos,
            )
            python_runtime_helper_classes.extend(helper_classes)
            python_runtime_helper_methods.append(helper_method)
        top_level_decls.extend(python_runtime_helper_classes)
        top_level_decls.append(ir.ClassDecl('final', 'PyRuntime', None, python_runtime_helper_methods))

        top_level_decls.extend(python_method_helper_classes)
        top_level_decls.append(ir.ClassDecl('final', 'PyBuiltinMethodsPythonImpl', None, python_method_helper_methods))

        for type_name in sorted(PYTHON_AUTHORED_CONSTRUCTOR_IMPLS):
            (helper_classes, helper_method) = translate_python_constructor_impl(
                pythonj_builtins_node, type_name, pool, funcs=pythonj_builtins_funcs, scope_infos=builtins_analyzer.scope_infos,
            )
            python_constructor_helper_classes.extend(helper_classes)
            emitted_name = f'{type_name}__newobj_impl'
            python_constructor_helper_methods.append(ir.MethodDecl(
                'static',
                'PyObject',
                f'pyfunc_{type_name}__newobj',
                ['PyConcreteType type', 'PyObject[] args', 'PyDict kwargs'],
                [
                    ir.ReturnStatement(ir.MethodCall(
                        ir.Identifier('PyBuiltinConstructorsPythonImpl'),
                        f'pyfunc_{emitted_name}',
                        [
                            ir.Identifier('type'),
                            ir.CreateObject('PyTuple', [ir.Identifier('args')]),
                            ir.CondOp(
                                ir.BinaryOp('!=', ir.Identifier('kwargs'), ir.Null()),
                                ir.Identifier('kwargs'),
                                ir.CreateObject('PyDict', []),
                            ),
                        ],
                    )),
                ],
            ))
            python_constructor_helper_methods.append(helper_method)
        top_level_decls.extend(python_constructor_helper_classes)
        top_level_decls.append(ir.ClassDecl('final', 'PyBuiltinConstructorsPythonImpl', None, python_constructor_helper_methods))

        for func_name in sorted(PYTHON_AUTHORED_IMPLS['builtins']):
            (helper_classes, helper_method) = translate_python_builtin_impl(
                pythonj_builtins_node, func_name, pool, funcs=pythonj_builtins_funcs, scope_infos=builtins_analyzer.scope_infos,
            )
            python_builtin_helper_classes.extend(helper_classes)
            python_builtin_helper_methods.append(helper_method)
        top_level_decls.extend(python_builtin_helper_classes)
        top_level_decls.append(ir.ClassDecl('final', 'PyBuiltinFunctionsPythonImpl', None, python_builtin_helper_methods))

        for module_name in sorted(extract_spec.BUILTIN_MODULES):
            module_spec = cast(dict[str, object], spec[module_name])
            attrs = cast(dict[str, dict[str, object]], module_spec['attrs'])
            module_java_name = extract_spec.BUILTIN_MODULES[module_name]
            module_func_prefix = module_java_name.removesuffix('Module')
            for func_name in sorted(attrs):
                if attrs[func_name]['kind'] != 'builtin_function':
                    continue
                params = get_module_function_params(spec, module_name, func_name)
                if params is None:
                    kwarg_params = None
                else:
                    kwarg_params = analyze_params(params)
                decls: list[ir.Decl] = [
                    ir.FieldDecl('public static final', f'{module_func_prefix}Function_{func_name}', 'singleton', ir.CreateObject(f'{module_func_prefix}Function_{func_name}', [])),
                    ir.ConstructorDecl('private', f'{module_func_prefix}Function_{func_name}', [], [
                        ir.SuperConstructorCall([ir.StrLiteral(func_name)]),
                    ]),
                ]
                full_name = f'{module_name}.{func_name}'
                (call_body, bind_args, call_positional_shape) = build_wrapper_binding_ir(
                    kwarg_params,
                    exact_kw_name=func_name,
                    exact_positional_name_many=func_name,
                    posonly_kw_name=func_name,
                    posonly_positional_name=func_name,
                    poskw_kw_name=func_name,
                    poskw_positional_name=func_name,
                    general_positional_name=func_name,
                    general_kw_name=full_name,
                    kw_overflow_args_length='argsLength',
                    noarg_name=full_name,
                )
                if call_positional_shape is not None:
                    call_expr = ir.MethodCall(ir.This(), 'callPositional', bind_args)
                else:
                    call_expr = ir.MethodCall(ir.Identifier('PyBuiltinFunctionsImpl'), f'pyfunc_{module_name.removeprefix("_")}_{func_name}', bind_args)
                call_body.append(ir.ReturnStatement(call_expr))
                decls.append(ir.MethodDecl('public', 'PyObject', 'call', ['PyObject[] args', 'PyDict kwargs'], call_body))
                if call_positional_shape is not None:
                    (call_positional_method_args, call_positional_statements, call_positional_args) = build_call_positional_ir(call_positional_shape)
                    decls.append(ir.MethodDecl(
                        'public',
                        'PyObject',
                        'callPositional',
                        call_positional_method_args,
                        [
                            *call_positional_statements,
                            ir.ReturnStatement(
                                ir.MethodCall(ir.Identifier('PyBuiltinFunctionsImpl'), f'pyfunc_{module_name.removeprefix("_")}_{func_name}', call_positional_args)
                            ),
                        ],
                    ))
                top_level_decls.append(ir.ClassDecl('final', f'{module_func_prefix}Function_{func_name}', 'PyBuiltinFunction', decls))

        for func_name in sorted(extract_spec.BUILTIN_FUNCTIONS):
            params = get_builtin_function_params(spec, func_name)
            if params is None or func_name in RAW_ARGS_KWARGS_BUILTINS:
                kwarg_params = None
            else:
                kwarg_params = analyze_params(params)
            decls: list[ir.Decl] = [
                ir.FieldDecl('public static final', f'PyBuiltinFunction_{func_name}', 'singleton', ir.CreateObject(f'PyBuiltinFunction_{func_name}', [])),
                ir.ConstructorDecl('private', f'PyBuiltinFunction_{func_name}', [], [
                    ir.SuperConstructorCall([ir.StrLiteral(func_name)]),
                ]),
            ]
            kw_name = 'sort' if func_name == 'sorted' else func_name
            kw_overflow_args_length = '0' if func_name == 'sorted' else 'argsLength'
            (call_body, bind_args, call_positional_shape) = build_wrapper_binding_ir(
                kwarg_params,
                exact_kw_name=func_name,
                exact_positional_name_many=func_name,
                posonly_kw_name=func_name,
                posonly_positional_name=func_name,
                poskw_kw_name=kw_name,
                poskw_positional_name=func_name,
                general_positional_name=func_name,
                general_kw_name=kw_name,
                kw_overflow_args_length=kw_overflow_args_length,
                noarg_name=func_name,
            )
            if func_name in PYTHON_AUTHORED_IMPLS['builtins']:
                call_target = 'PyBuiltinFunctionsPythonImpl'
            else:
                call_target = 'PyBuiltinFunctionsImpl'
            if call_positional_shape is not None:
                call_expr = ir.MethodCall(ir.This(), 'callPositional', bind_args)
            else:
                call_expr = ir.MethodCall(ir.Identifier(call_target), f'pyfunc_{func_name}', bind_args)
            call_body.append(ir.ReturnStatement(call_expr))
            decls.append(ir.MethodDecl('public', 'PyObject', 'call', ['PyObject[] args', 'PyDict kwargs'], call_body))
            if call_positional_shape is not None:
                (call_positional_method_args, call_positional_statements, call_positional_args) = build_call_positional_ir(call_positional_shape)
                decls.append(ir.MethodDecl(
                    'public',
                    'PyObject',
                    'callPositional',
                    call_positional_method_args,
                    [
                        *call_positional_statements,
                        ir.ReturnStatement(
                            ir.MethodCall(ir.Identifier(call_target), f'pyfunc_{func_name}', call_positional_args)
                        ),
                    ],
                ))
            top_level_decls.append(ir.ClassDecl('final', f'PyBuiltinFunction_{func_name}', 'PyBuiltinFunction', decls))

        ir.write_decls(f, top_level_decls, pool, include_pool_decls=True)
