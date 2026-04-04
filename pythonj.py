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
ISINSTANCE_SINGLE_FASTPATH_BUILTIN_TYPES = {'bytearray', 'bytes', 'str', 'tuple'}

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

@dataclass
class ScopeInfo:
    kind: ScopeKind
    locals: set[str]
    explicit_globals: set[str]
    nonlocals: set[str]
    cell_vars: set[str]
    needs_from_outer: set[str]

class ScopeAnalyzer(ast.NodeVisitor):
    __slots__ = ('scope_infos',)
    scope_infos: dict[ast.AST, ScopeInfo]

    def __init__(self):
        self.scope_infos = {}

    def _walk_local(self, node: ast.AST, reads: set[str], writes: set[str], explicit_globals: set[str],
                    nonlocals: set[str], children: list[ast.AST]) -> None:
        if isinstance(node, ast.Name):
            if isinstance(node.ctx, (ast.Store, ast.Del)):
                writes.add(node.id)
            else:
                reads.add(node.id)
            return
        if isinstance(node, ast.ExceptHandler):
            if node.type is not None:
                self._walk_local(node.type, reads, writes, explicit_globals, nonlocals, children)
            if node.name is not None:
                writes.add(node.name)
            for statement in node.body:
                self._walk_local(statement, reads, writes, explicit_globals, nonlocals, children)
            return
        if isinstance(node, ast.Global):
            explicit_globals.update(node.names)
            return
        if isinstance(node, ast.Nonlocal):
            nonlocals.update(node.names)
            return
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            writes.add(node.name)
            children.append(node)
            return
        if isinstance(node, ast.Lambda):
            children.append(node)
            return
        if isinstance(node, ast.ClassDef):
            writes.add(node.name)
            children.append(node)
            return
        if isinstance(node, ast.Import):
            for alias in node.names:
                writes.add(alias.asname if alias.asname is not None else alias.name)
            return
        if isinstance(node, ast.ImportFrom):
            for alias in node.names:
                writes.add(alias.asname if alias.asname is not None else alias.name)
            return
        if isinstance(node, (ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp)):
            self._walk_local(node.generators[0].iter, reads, writes, explicit_globals, nonlocals, children)
            children.append(node)
            return
        for child in ast.iter_child_nodes(node):
            self._walk_local(child, reads, writes, explicit_globals, nonlocals, children)

    def _param_names(self, args: ast.arguments) -> set[str]:
        out = {arg.arg for arg in args.posonlyargs}
        out.update(arg.arg for arg in args.args)
        out.update(arg.arg for arg in args.kwonlyargs)
        if args.vararg is not None:
            out.add(args.vararg.arg)
        if args.kwarg is not None:
            out.add(args.kwarg.arg)
        return out

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
                self._walk_local(statement, reads, writes, explicit_globals, nonlocals, children)
        elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            writes.update(self._param_names(node.args))
            for statement in node.body:
                self._walk_local(statement, reads, writes, explicit_globals, nonlocals, children)
        elif isinstance(node, ast.ClassDef):
            for statement in node.body:
                self._walk_local(statement, reads, writes, explicit_globals, nonlocals, children)
        elif isinstance(node, ast.Lambda):
            writes.update(self._param_names(node.args))
            self._walk_local(node.body, reads, writes, explicit_globals, nonlocals, children)
        else:
            assert isinstance(node, (ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp)), node
            generators = node.generators
            elts: list[ast.expr] = [node.elt] if not isinstance(node, ast.DictComp) else [node.key, node.value]
            for (i, generator) in enumerate(generators):
                if i != 0:
                    self._walk_local(generator.iter, reads, writes, explicit_globals, nonlocals, children)
                self._walk_local(generator.target, reads, writes, explicit_globals, nonlocals, children)
                for _if in generator.ifs:
                    self._walk_local(_if, reads, writes, explicit_globals, nonlocals, children)
            for elt in elts:
                self._walk_local(elt, reads, writes, explicit_globals, nonlocals, children)

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
        scope_info = ScopeInfo(kind, locals, explicit_globals, nonlocals, cell_vars, needs_from_outer)
        self.scope_infos[node] = scope_info
        return scope_info

    def visit_Module(self, node) -> None:
        self._analyze_scope_node(node)

class Scope:
    __slots__ = ('parent', 'info', 'qualname', 'free_vars', 'locals_are_fields', 'n_temps', 'used_expr_discard',
                 'expected_return_java_type')
    parent: Optional['Scope']
    info: ScopeInfo
    qualname: Optional[str]
    free_vars: set[str]
    locals_are_fields: bool
    n_temps: int
    used_expr_discard: bool
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
                 'scope_infos', 'pool', 'break_name', 'functions', 'allow_intrinsics',
                 'python_helper_names', 'python_helper_class')
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
    functions: dict[str, list[str]]
    allow_intrinsics: bool # enables special internal-only codegen features for builtins
    python_helper_names: set[str]
    python_helper_class: Optional[str]

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
        self.functions = {}
        self.allow_intrinsics = False
        self.python_helper_names = set()
        self.python_helper_class = None

    # Note: if n_errors > 0, the generated Java code is not expected to be valid or executable.
    def error(self, lineno: Optional[int], msg: str) -> None:
        if lineno is None:
            print(f'ERROR: {self.path}: {msg}')
        else:
            print(f'ERROR: {self.path}:{lineno}: {msg}')
        self.n_errors += 1

    def ident_expr(self, name: str) -> ir.Expr:
        if self.allow_intrinsics and name == '__pythonj_null__':
            return ir.Null()
        elif self.scope.info.kind == ScopeKind.FUNCTION and (name in self.scope.info.cell_vars or name in self.scope.free_vars):
            return ir.Field(ir.Identifier(f'pycell_{name}'), 'obj')
        elif self.scope.info.kind == ScopeKind.FUNCTION and name in self.scope.info.locals:
            if self.scope.locals_are_fields:
                return ir.Field(ir.This(), f'pylocal_{name}')
            return ir.Identifier(f'pylocal_{name}')
        elif name in extract_spec.BUILTIN_TYPES:
            return ir.Field(ir.Identifier(f'{extract_spec.BUILTIN_TYPES[name]}Type'), 'singleton')
        elif name in extract_spec.EXCEPTION_TYPES:
            return ir.Field(ir.Identifier(f'Py{name}Type'), 'singleton')
        elif name in extract_spec.BUILTIN_FUNCTIONS:
            return ir.Field(ir.Identifier(f'PyBuiltinFunction_{name}'), 'singleton')
        else:
            return ir.Identifier(f'pyglobal_{name}')

    def resolve_free_vars(self, lineno: int, scope_info: ScopeInfo, func_type: str) -> set[str]:
        free_vars = set()
        for name in scope_info.needs_from_outer:
            parent_scope = self.scope
            found = False
            while parent_scope:
                if name in parent_scope.info.explicit_globals:
                    break
                if parent_scope.info.kind == ScopeKind.FUNCTION and name in (parent_scope.info.locals | parent_scope.info.cell_vars | parent_scope.free_vars):
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
            return ir.MethodCall(ir.Identifier('PyBool'), 'create', [ir.unary_op('!', ir.bool_value(self.visit(node.operand)))])
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
        return ir.CondOp(ir.bool_value(self.visit(node.test)), self.visit(node.body), self.visit(node.orelse))

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
                vals.append(ir.MethodCall(expr, 'format', [format_spec]))
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
                    if isinstance(node.args[1], ast.Name) and node.args[1].id in ISINSTANCE_SINGLE_FASTPATH_BUILTIN_TYPES:
                        builtin_type = node.args[1].id
                        assert builtin_type in extract_spec.BUILTIN_TYPES, builtin_type
                        return ir.MethodCall(ir.Identifier('PyBool'), 'create', [
                            ir.BinaryOp('instanceof', self.visit(node.args[0]), ir.Identifier(extract_spec.BUILTIN_TYPES[builtin_type]))
                        ])
                    return ir.MethodCall(ir.Identifier('Runtime'), 'pythonjIsInstance', [self.visit(node.args[0]), self.visit(node.args[1])])
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
        return self.ident_expr(node.id)

    def visit_Subscript(self, node) -> ir.Expr:
        return ir.MethodCall(self.visit(node.value), 'getItem', [self.visit(node.slice)])

    def visit_Attribute(self, node) -> ir.Expr:
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
            self.code.extend(self.emit_bind(ast.Name(id=bind_name), ir.Field(ir.Identifier(extract_spec.BUILTIN_MODULES[alias.name]), 'singleton')))

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
            yield ir.AssignStatement(self.visit(target), value)
        elif isinstance(target, ast.Attribute):
            temp_name = self.scope.make_temp()
            yield ir.LocalDecl('var', temp_name, value)
            yield ir.ExprStatement(ir.MethodCall(self.visit(target.value), 'setAttr', [ir.StrLiteral(target.attr), ir.Identifier(temp_name)]))
        elif isinstance(target, ast.Subscript):
            temp_name = self.scope.make_temp()
            yield ir.LocalDecl('var', temp_name, value)
            yield ir.ExprStatement(ir.MethodCall(self.visit(target.value), 'setItem', [self.visit(target.slice), ir.Identifier(temp_name)]))
        elif isinstance(target, (ast.Tuple, ast.List)):
            temp_name = self.scope.make_temp()
            yield ir.LocalDecl('var', temp_name, ir.MethodCall(value, 'iter', []))
            # XXX This is not atomic if an exception is thrown; a subset of LHS's will be left assigned
            for subtarget in target.elts:
                yield from self.emit_bind(subtarget, ir.MethodCall(ir.Identifier('Runtime'), 'nextRequireNonNull', [ir.Identifier(temp_name)]))
            yield ir.ExprStatement(ir.MethodCall(ir.Identifier('Runtime'), 'nextRequireNull', [ir.Identifier(temp_name)]))
        else:
            self.error(target.lineno, f'binding to {type(target).__name__} is unsupported')

    def visit_Assign(self, node) -> None:
        if len(node.targets) != 1:
            self.error(node.lineno, 'chained assignment (a = b = c) is unsupported')
        target = node.targets[0]
        self.code.extend(self.emit_bind(target, self.visit(node.value)))

    def visit_AugAssign(self, node) -> None:
        op = f'{self.visit(node.op)}InPlace'

        if isinstance(node.target, ast.Name):
            code = ir.AssignStatement(self.visit(node.target), ir.MethodCall(self.visit(node.target), op, [self.visit(node.value)]))
        elif isinstance(node.target, ast.Attribute):
            temp_name = self.scope.make_temp()
            self.code.append(ir.LocalDecl('var', temp_name, self.visit(node.target.value)))
            code = ir.ExprStatement(ir.MethodCall(ir.Identifier(temp_name), 'setAttr', [
                ir.StrLiteral(node.target.attr),
                ir.MethodCall(
                    ir.MethodCall(ir.Identifier(temp_name), 'getAttr', [ir.StrLiteral(node.target.attr)]),
                    op,
                    [self.visit(node.value)]
                )
            ]))
        elif isinstance(node.target, ast.Subscript):
            temp_name0 = self.scope.make_temp()
            temp_name1 = self.scope.make_temp()
            self.code.append(ir.LocalDecl('var', temp_name0, self.visit(node.target.value)))
            self.code.append(ir.LocalDecl('var', temp_name1, self.visit(node.target.slice)))
            code = ir.ExprStatement(ir.MethodCall(ir.Identifier(temp_name0), 'setItem', [
                ir.Identifier(temp_name1),
                ir.MethodCall(
                    ir.MethodCall(ir.Identifier(temp_name0), 'getItem', [ir.Identifier(temp_name1)]),
                    op,
                    [self.visit(node.value)]
                )
            ]))
        else:
            self.error(node.lineno, f'augmented assignment to {type(node.target).__name__} is unsupported')
            self.visit(node.value) # recurse to find more errors
            return
        self.code.append(code)

    def visit_Assert(self, node) -> None:
        cond = ir.unary_op('!', ir.bool_value(self.visit(node.test)))
        msg = ir.StrLiteral(self.path + f':{node.lineno}: assertion failure')
        if node.msg:
            msg.s += ': '
            msg = ir.BinaryOp('+', msg, ir.MethodCall(self.visit(node.msg), 'repr', []))
        exception = ir.MethodCall(ir.Identifier('PyAssertionError'), 'raise', [msg])
        self.code.extend(ir.if_statement(cond, [ir.ThrowStatement(exception)], []))

    def visit_Delete(self, node) -> None:
        for target in node.targets:
            if isinstance(target, ast.Attribute):
                code = ir.ExprStatement(ir.MethodCall(self.visit(target.value), 'delAttr', [ir.StrLiteral(target.attr)]))
            elif isinstance(target, ast.Subscript):
                code = ir.ExprStatement(ir.MethodCall(self.visit(target.value), 'delItem', [self.visit(target.slice)]))
            else:
                self.error(node.lineno, f"'del' of {type(target).__name__} is unsupported")
                continue
            self.code.append(code)

    def visit_Return(self, node) -> None:
        assert self.scope.info.kind == ScopeKind.FUNCTION, node
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
        cond = ir.bool_value(self.visit(node.test))
        body = self.visit_block(node.body)
        orelse = self.visit_block(node.orelse)
        self.code.extend(ir.if_statement(cond, body, orelse))

    def visit_While(self, node) -> None:
        cond = ir.bool_value(self.visit(node.test))

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
            self.code.append(ir.ExprStatement(ir.MethodCall(ir.Identifier(temp_name), 'enter', [])))
        else:
            self.code.extend(self.emit_bind(item.optional_vars, ir.MethodCall(ir.Identifier(temp_name), 'enter', [])))

        body = self.visit_block(node.body)

        self.code.append(ir.TryStatement(body, None, None, [], [
            ir.ExprStatement(ir.MethodCall(ir.Identifier(temp_name), 'exit', [])),
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

    def check_args(self, lineno: int, args: ast.arguments) -> tuple[list[str], list[object]]:
        if args.posonlyargs:
            self.error(lineno, 'position-only arguments are unsupported')
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
        for arg in args.args:
            # arg.type_comment is ignored; we only plan to support "real" type annotations.
            if arg.annotation:
                self.error(lineno, 'argument type annotations are unsupported')
        return ([arg.arg for arg in args.args], defaults)

    @contextmanager
    def new_function(self, scope_info: ScopeInfo, free_vars: set[str], qualname: str,
                     expected_return_java_type: Optional[str] = None) -> Iterator[None]:
        saved_scope = self.scope
        self.scope = Scope(self.scope, scope_info, qualname, expected_return_java_type)
        self.scope.free_vars = free_vars.copy()
        try:
            yield
        finally:
            self.scope = saved_scope

    def qualname(self, name: str) -> str:
        return name if self.scope.qualname is None else f'{self.scope.qualname}.<locals>.{name}'

    def add_function(self, py_name: str, java_name: str, arg_names: list[str], arg_defaults: list[object], body: list[ir.Statement],
                     invisible_args: bool = False) -> None:
        n_args = len(arg_names)
        n_required = n_args - len(arg_defaults)
        bind_arg_names = [f'pyarg_{arg}' for arg in arg_names]
        free_var_names = sorted(self.scope.free_vars)
        py_name_java = ir.StrLiteral(py_name).emit_java(self.pool)
        arg_names_java = ', '.join(ir.StrLiteral(arg).emit_java(self.pool) for arg in arg_names)
        required_arg_names_java = ', '.join(ir.StrLiteral(arg).emit_java(self.pool) for arg in arg_names[:n_required])
        func_code = [
            f'private static final class {java_name} extends PyFunction {{',
            *(ir.FieldDecl('private final', 'PyCell', f'pycell_{name}', None).emit_java(self.pool) for name in free_var_names),
            (f'{java_name}({", ".join(f"PyCell pycell_{name}" for name in free_var_names)}) {{' if free_var_names else f'{java_name}() {{'),
            *ir.block_emit_java([ir.SuperConstructorCall([ir.StrLiteral(py_name)])], self.pool),
            *(f'this.pycell_{name} = pycell_{name};' for name in free_var_names),
            '}',
            '@Override public PyObject call(PyObject[] args, PyDict kwargs) {',
            'int argsLength = args.length;',
            *(f'PyObject {name} = (argsLength >= {i + 1}) ? args[{i}] : null;' for (i, name) in enumerate(bind_arg_names)),
            'if ((kwargs == null) || !kwargs.boolValue()) {',
        ]
        if n_required == n_args:
            func_code.extend([
                f'if (argsLength != {n_args}) {{',
                f'throw Runtime.raiseUserExactArgs(args, {n_args}, {py_name_java}{", " if arg_names_java else ""}{arg_names_java});',
                '}',
            ])
        else:
            if n_required != 0:
                func_code.extend([
                    f'if (argsLength < {n_required}) {{',
                    f'throw Runtime.raiseUserMissingArgs(argsLength, {py_name_java}, {required_arg_names_java});',
                    '}',
                ])
            func_code.extend([
                f'if (argsLength > {n_args}) {{',
                f'throw Runtime.raiseUserFromToArgs(args, {n_required}, {n_args}, {py_name_java});',
                '}',
            ])
        func_code.append('} else {')
        if arg_names:
            func_code.extend([
                'for (var x: kwargs.items.entrySet()) {',
                'String kwName = ((PyString)x.getKey()).value;',
                'PyObject kwValue = x.getValue();',
            ])
            for (i, arg_name) in enumerate(arg_names):
                prefix = '' if i == 0 else 'else '
                func_code.extend([
                    f'{prefix}if (kwName.equals({ir.StrLiteral(arg_name).emit_java(self.pool)})) {{',
                    f'if ({bind_arg_names[i]} != null) {{',
                    f'throw Runtime.raiseMultipleValues({py_name_java}, {ir.StrLiteral(arg_name).emit_java(self.pool)});',
                    '}',
                    f'{bind_arg_names[i]} = kwValue;',
                    '}',
                ])
            func_code.extend([
                'else {',
                f'throw Runtime.raiseUnexpectedKwArg({py_name_java}, kwName);',
                '}',
                '}',
            ])
        else:
            func_code.extend([
                'String kwName = ((PyString)kwargs.items.keySet().iterator().next()).value;',
                f'throw Runtime.raiseUnexpectedKwArg({py_name_java}, kwName);',
            ])
        if n_args != 0:
            func_code.extend([
                f'if (argsLength > {n_args}) {{',
                f'throw Runtime.raiseUserFromToArgs(args, {n_required}, {n_args}, {py_name_java});',
                '}',
            ])
            if n_required != 0:
                func_code.append(f'if ({ " || ".join(f"{bind_arg_names[i]} == null" for i in range(n_required)) }) {{')
                func_code.append(f'throw Runtime.raiseUserMissingKwArgs({py_name_java}, new PyObject[] {{{", ".join(bind_arg_names[:n_required])}}}, {required_arg_names_java});')
                func_code.append('}')
        func_code.append('}')
        for (i, name) in enumerate(bind_arg_names[n_required:]):
            func_code.append(f'if ({name} == null) {{ {name} = {emit_default_expr(arg_defaults[i], self.pool)}; }}')
        if self.scope.used_expr_discard:
            func_code.append('PyObject expr_discard;')
        for (arg_name, bind_arg_name) in zip(arg_names, bind_arg_names):
            if arg_name in self.scope.info.cell_vars:
                func_code.append(f'PyCell pycell_{arg_name} = new PyCell({bind_arg_name});')
            else:
                local_arg_name = arg_name if invisible_args else f'pylocal_{arg_name}'
                func_code.append(f'PyObject {local_arg_name} = {bind_arg_name};')
        for name in sorted(self.scope.info.cell_vars - set(arg_names)):
            func_code.append(f'PyCell pycell_{name} = new PyCell({ir.PyConstant(None).emit_java(self.pool)});')
        for name in sorted(self.scope.info.locals - self.scope.info.cell_vars - set(arg_names)):
            if invisible_args or name not in arg_names:
                func_code.append(f'PyObject pylocal_{name};')
        func_code.extend(ir.block_emit_java(ir.block_simplify([*body, ir.ReturnStatement(ir.PyConstant(None))]), self.pool))
        func_code.extend([
            '}',
            '}',
        ])
        assert java_name not in self.functions
        self.functions[java_name] = func_code

    def visit_FunctionDef(self, node) -> None:
        # node.type_comment is ignored; we only plan to support "real" type annotations.
        if node.decorator_list:
            self.error(node.lineno, 'function decorators are unsupported')
        if node.returns:
            self.error(node.lineno, 'function return annotations are unsupported')
        if node.type_params:
            self.error(node.lineno, 'function type parameters are unsupported')

        (arg_names, arg_defaults) = self.check_args(node.lineno, node.args)
        qualname = self.qualname(node.name)
        java_name = f'pyfunc_{node.name}_{self.n_functions}'
        self.n_functions += 1
        scope_info = self.scope_infos[node]
        free_vars = self.resolve_free_vars(node.lineno, scope_info, 'nested function')
        self.code.append(ir.AssignStatement(self.ident_expr(node.name), ir.CreateObject(java_name, [ir.Identifier(f'pycell_{name}') for name in sorted(free_vars)])))

        with self.new_function(scope_info, free_vars, qualname):
            body = self.visit_block(node.body)
            self.add_function(qualname, java_name, arg_names, arg_defaults, body)

    def visit_ClassDef(self, node) -> None:
        if self.scope.info.kind != ScopeKind.MODULE:
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
        type_name = f'{java_name}Type'
        type_class_name = f'{type_name}Class'
        class_code = []
        if slots is None:
            class_code.extend([
                f'private static final class {java_name} extends PyBagObject {{',
                f'{java_name}() {{',
                *ir.block_emit_java([ir.SuperConstructorCall([ir.Identifier(f'{type_class_name}.singleton')])], self.pool),
                '}',
                f'public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {{',
                f'Runtime.requireNoKwArgs(kwargs, type.name());',
                f'if (args.length != 0) {{',
                f'throw PyTypeError.raise(type.name() + "() takes no arguments");',
                f'}}',
                f'return new {java_name}();',
                '}',
                '}',
            ])
        else:
            class_code.extend([
                f'private static final class {java_name} extends PySlottedObject {{',
                *(ir.FieldDecl('private', 'PyObject', f'pyslot_{name}', ir.Null()).emit_java(self.pool) for name in slots),
                f'{java_name}() {{',
                *ir.block_emit_java([ir.SuperConstructorCall([ir.Identifier(f'{type_class_name}.singleton')])], self.pool),
                '}',
                '@Override public PyObject getAttr(String key) {',
                'switch (key) {',
            ])
            for name in slots:
                class_code.extend([
                    f'case {ir.StrLiteral(name).emit_java(self.pool)}:',
                    f'if (pyslot_{name} == null) {{',
                    'throw raiseMissingAttr(key);',
                    '}',
                    f'return pyslot_{name};',
                ])
            class_code.extend([
                'case "__dict__": throw raiseMissingAttr(key);',
                'default: return super.getAttr(key);',
                '}',
                '}',
                '@Override public void setAttr(String key, PyObject value) {',
                'switch (key) {',
            ])
            for name in slots:
                class_code.extend([
                    f'case {ir.StrLiteral(name).emit_java(self.pool)}:',
                    f'pyslot_{name} = value;',
                    'return;',
                ])
            class_code.extend([
                'case "__class__": throw Runtime.raiseNamedReadOnlyAttr(type(), key);',
                'default: super.setAttr(key, value);',
                '}',
                '}',
                '@Override public void delAttr(String key) {',
                'switch (key) {',
            ])
            for name in slots:
                class_code.extend([
                    f'case {ir.StrLiteral(name).emit_java(self.pool)}:',
                    f'if (pyslot_{name} == null) {{',
                    'throw raiseMissingAttr(key);',
                    '}',
                    f'pyslot_{name} = null;',
                    'return;',
                ])
            class_code.extend([
                'case "__class__": throw Runtime.raiseNamedReadOnlyAttr(type(), key);',
                'default: super.delAttr(key);',
                '}',
                '}',
                f'public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {{',
                f'Runtime.requireNoKwArgs(kwargs, type.name());',
                f'if (args.length != 0) {{',
                f'throw PyTypeError.raise(type.name() + "() takes no arguments");',
                f'}}',
                f'return new {java_name}();',
                '}',
                '}',
            ])
        class_code.extend([
            f'private static final class {type_class_name} extends PyConcreteType {{',
            ir.FieldDecl('private static final', type_class_name, 'singleton', ir.CreateObject(type_class_name, [])).emit_java(self.pool),
            f'private {type_class_name}() {{',
            *ir.block_emit_java([ir.SuperConstructorCall([ir.StrLiteral(node.name), ir.Field(ir.Identifier(java_name), 'class'), ir.MethodRef(java_name, 'newObj')])], self.pool),
            '}',
            '}',
        ])
        assert java_name not in self.functions
        self.functions[java_name] = class_code
        self.code.append(ir.AssignStatement(self.ident_expr(node.name), ir.Identifier(f'{type_class_name}.singleton')))

    def visit_Lambda(self, node) -> ir.Expr:
        (arg_names, arg_defaults) = self.check_args(node.lineno, node.args)
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
            self.add_function(qualname, java_name, arg_names, arg_defaults, body)

        return ir.CreateObject(java_name, [ir.Identifier(f'pycell_{name}') for name in sorted(free_vars)])

    def _lower_comp_generator(self, generator: ast.comprehension, iterable: ir.Expr, statements: list[ir.Statement]) -> list[ir.Statement]:
        for _if in reversed(generator.ifs):
            statements = list(ir.if_statement(ir.bool_value(self.visit(_if)), statements, []))

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
                    ir.ExprStatement(ir.MethodCall(ir.Identifier(temp_result), method_name, [self.visit(elt) for elt in elts]))
                ]
                for (i, generator) in enumerate(reversed(generators)):
                    iterable = ir.Identifier(arg_name) if i == len(generators)-1 else self.visit(generator.iter)
                    statements = self._lower_comp_generator(generator, iterable, statements)
                body += [
                    ir.LocalDecl('var', temp_result, ir.CreateObject(type_name, [])),
                    *statements,
                    ir.ReturnStatement(ir.Identifier(temp_result)),
                ]
            self.add_function(qualname, java_name, [arg_name], [], body, invisible_args=True)

        return ir.MethodCall(ir.CreateObject(java_name, [ir.Identifier(f'pycell_{name}') for name in sorted(free_vars)]), 'call', [ir.CreateArray('PyObject', [self.visit(generators[0].iter)]), ir.Null()])

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
                    body = list(ir.if_statement(ir.bool_value(self.visit(_if)), body, [ir.ContinueStatement()]))
                body = [
                    ir.AssignStatement(temp_item_expr, ir.MethodCall(ir.Identifier('pyiter_iterable'), 'next', [])),
                    *ir.if_statement(ir.BinaryOp('==', temp_item_expr, ir.Null()), [ir.ReturnStatement(ir.Null())], []),
                    *self.emit_bind(generator.target, temp_item_expr),
                    *body,
                ]
                next_body.extend(ir.while_statement(ir.Bool(True), body))

            free_var_names = sorted(self.scope.free_vars)
            func_code = [
                f'private static final class {java_name} extends PyIter {{',
                ir.FieldDecl('private static final', 'PyConcreteType', 'type_singleton', ir.CreateObject('PyConcreteType', [ir.StrLiteral('generator'), ir.Field(ir.Identifier(java_name), 'class')])).emit_java(self.pool),
                *(ir.FieldDecl('private final', 'PyCell', f'pycell_{name}', None).emit_java(self.pool) for name in free_var_names),
                ir.FieldDecl('private final', 'PyIter', 'pyiter_iterable', None).emit_java(self.pool),
                *(ir.FieldDecl('private final', 'PyCell', f'pycell_{name}', ir.CreateObject('PyCell', [ir.PyConstant(None)])).emit_java(self.pool) for name in sorted(self.scope.info.cell_vars)),
                *(ir.FieldDecl('private', 'PyObject', f'pylocal_{name}', ir.PyConstant(None)).emit_java(self.pool) for name in sorted(self.scope.info.locals - self.scope.info.cell_vars)),
                f'{java_name}({", ".join([*(f"PyCell pycell_{name}" for name in free_var_names), "PyObject iterable"])}) {{' if free_var_names else f'{java_name}(PyObject iterable) {{',
                *(f'this.pycell_{name} = pycell_{name};' for name in free_var_names),
                'this.pyiter_iterable = iterable.iter();',
                '}',
                '@Override public PyObject next() {',
                *ir.block_emit_java(ir.block_simplify(next_body), self.pool),
                '}',
                '@Override public String repr() {',
                *ir.block_emit_java([ir.ReturnStatement(ir.StrLiteral(f'<generator object {qualname}>'))], self.pool),
                '}',
                '@Override public PyConcreteType type() {',
                *ir.block_emit_java([ir.ReturnStatement(ir.Identifier('type_singleton'))], self.pool),
                '}',
                '}',
            ]
            assert java_name not in self.functions
            self.functions[java_name] = func_code

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
            self.visit(statement)

    def write_java(self, f: TextIO, py_name: str) -> None:
        writer = ir.IndentedWriter(f, 0)
        writer.write(f'public final class {py_name} {{')
        for code in self.functions.values():
            for line in code:
                writer.write(line)
            writer.write('')

        # XXX Initializing all globals to None is weird, but we don't have a better option yet
        for name in sorted(self.scope.info.locals):
            writer.write(ir.FieldDecl('private static', 'PyObject', f'pyglobal_{name}', ir.PyConstant(None)).emit_java(self.pool))
        writer.write('')

        writer.write('public static void main(String[] args) {')
        for line in ir.block_emit_java(ir.block_simplify(self.global_code), self.pool):
            writer.write(line)
        writer.write('}')

        writer.write('')
        for line in self.pool.emit_pool():
            writer.write(line)
        writer.write('}')
        assert writer.indent == 0, writer.indent

def get_top_level_function_names(path: str) -> set[str]:
    with open(path, encoding='utf-8') as f:
        node = ast.parse(f.read(), path)
    return {x.name for x in node.body if isinstance(x, ast.FunctionDef)}

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

PYTHON_AUTHORED_IMPLS = {
    'builtins': {'abs', 'all', 'any', 'bin', 'delattr', 'format', 'getattr', 'hash', 'hasattr', 'isinstance', 'issubclass', 'len', 'next', 'oct', 'repr', 'setattr', 'sum'},
    'bytes': {'capitalize', 'count', 'endswith', 'find', 'fromhex', 'hex', 'index', 'isalnum', 'isalpha', 'isascii', 'isdigit', 'islower', 'isspace', 'istitle', 'isupper', 'lower', 'lstrip', 'partition', 'removeprefix', 'removesuffix', 'rfind', 'rindex', 'rpartition', 'rstrip', 'startswith', 'strip', 'swapcase', 'title', 'upper'},
    'dict': {'fromkeys', 'setdefault'},
    'float': {'conjugate'},
    'int': {'as_integer_ratio', 'conjugate', 'is_integer'},
    'range': {'count'},
    'str': {'removeprefix', 'removesuffix'},
}

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

SUPPORTED_HELPER_RETURN_TYPES = {'bool', 'bytes', 'float', 'int', 'str', 'tuple'}

def decode_helper_return_annotation(annotation: Optional[ast.expr]) -> str:
    if annotation is None:
        return 'PyObject'
    if isinstance(annotation, ast.Constant) and annotation.value is None:
        return 'PyNone'
    if isinstance(annotation, ast.Name) and annotation.id == 'NoReturn':
        return 'NoReturn'
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
                                return_java_type: str) -> list[str]:
    java_return_type = 'PyNone' if return_java_type == 'NoReturn' else return_java_type
    func_code = [
        f'static {java_return_type} pyfunc_{func_name}({", ".join(f"PyObject pyarg_{arg}" for arg in arg_names)}) {{',
    ]
    if visitor.scope.used_expr_discard:
        func_code.append('PyObject expr_discard;')
    for arg_name in arg_names:
        if arg_name in visitor.scope.info.cell_vars:
            func_code.append(f'PyCell pycell_{arg_name} = new PyCell(pyarg_{arg_name});')
        else:
            func_code.append(f'PyObject pylocal_{arg_name} = pyarg_{arg_name};')
    for name in sorted(visitor.scope.info.cell_vars - set(arg_names)):
        func_code.append(f'PyCell pycell_{name} = new PyCell({ir.PyConstant(None).emit_java(visitor.pool)});')
    for name in sorted(visitor.scope.info.locals - visitor.scope.info.cell_vars - set(arg_names)):
        func_code.append(f'PyObject pylocal_{name};')
    if return_java_type == 'NoReturn':
        func_code.extend(ir.block_emit_java(ir.block_simplify(body), visitor.pool))
    else:
        implicit_return: ir.Statement = ir.ReturnStatement(ir.PyConstant(None))
        if return_java_type != 'PyObject':
            implicit_return = ir.ReturnStatement(ir.CastExpr(return_java_type, ir.PyConstant(None)))
        func_code.extend(ir.block_emit_java(ir.block_simplify([*body, implicit_return]), visitor.pool))
    func_code.append('}')
    return func_code

def translate_python_impl_node(node: ast.Module, func: ast.FunctionDef, emitted_name: str, display_name: str,
                               role: str, pool: ir.ConstantPool,
                               python_helper_names: Optional[set[str]] = None,
                               python_helper_class: Optional[str] = None) -> tuple[list[list[str]], list[str]]:
    analyzer = ScopeAnalyzer()
    analyzer.visit(node)
    visitor = LoweringVisitor(display_name, analyzer.scope_infos, analyzer.scope_infos[node])
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
        body = visitor.visit_block(func.body)
        if return_java_type == 'NoReturn' and not ir.block_ends_control_flow(body):
            visitor.error(func.lineno, f'NoReturn helper function {func.name} may implicitly fall through')
        elif return_java_type not in {'PyObject', 'PyNone'} and not ir.block_ends_control_flow(body):
            visitor.error(func.lineno, f'annotated function {func.name} may implicitly return None')
        helper_method = emit_python_function_static(visitor, emitted_name, arg_names, body, return_java_type)
    assert visitor.n_errors == 0, (emitted_name, visitor.n_errors)
    return (list(visitor.functions.values()), helper_method)

def translate_python_builtin_impl(func_name: str, pool: ir.ConstantPool) -> tuple[list[list[str]], list[str]]:
    node = parse_python_module('pythonj_builtins.py')
    func = get_top_level_functions(node)[func_name]
    return translate_python_impl_node(node, func, func_name, f'<builtin {func_name}>', 'builtin function', pool)

def translate_python_method_impl(type_name: str, method_name: str, pool: ir.ConstantPool) -> tuple[list[list[str]], list[str]]:
    func_name = f'{type_name}__{method_name}'
    node = parse_python_module('pythonj_builtins.py')
    func = get_class_functions(node, type_name)[method_name]
    return translate_python_impl_node(node, func, func_name, f'<method {type_name}.{method_name}>', 'builtin method', pool)

def translate_python_runtime_impl(func_name: str, pool: ir.ConstantPool) -> tuple[list[list[str]], list[str]]:
    node = parse_python_module('pythonj_runtime.py')
    funcs = get_top_level_functions(node)
    return translate_python_impl_node(
        node, funcs[func_name], func_name, f'<runtime {func_name}>', 'runtime helper', pool,
        python_helper_names=set(funcs), python_helper_class='PyRuntimePythonImpl',
    )

def infer_default_expr(default: object) -> Optional[str]:
    if default is None:
        return 'PyNone.singleton'
    elif default is NULL:
        return 'null'
    elif default is False or default is True:
        return f'PyBool.{str(default).lower()}_singleton'
    elif type(default) is int:
        if default == -1:
            return 'PyInt.singleton_neg1'
        elif default == 0:
            return 'PyInt.singleton_0'
        elif default == 1:
            return 'PyInt.singleton_1'
        else:
            return None
    elif default == '':
        return 'PyString.empty_singleton'
    else:
        return None

def emit_default_expr(default: object, pool: ir.ConstantPool) -> str:
    inferred = infer_default_expr(default)
    if inferred is not None:
        return inferred
    else:
        return ir.PyConstant(default).emit_java(pool)

def emit_default_java_expr(default: object) -> ir.Expr:
    inferred = infer_default_expr(default)
    if inferred == 'null':
        return ir.Null()
    if inferred == 'true':
        return ir.Bool(True)
    if inferred == 'false':
        return ir.Bool(False)
    if inferred is not None:
        return ir.Identifier(inferred)
    return ir.PyConstant(default)

def runtime_throw(method: str, args_: list[ir.Expr]) -> ir.ThrowStatement:
    return ir.ThrowStatement(ir.MethodCall(ir.Identifier('Runtime'), method, args_))

def type_error_throw(msg: str) -> ir.ThrowStatement:
    return ir.ThrowStatement(ir.MethodCall(ir.Identifier('PyTypeError'), 'raise', [ir.StrLiteral(msg)]))

def build_arg_binding_ir(shape: SignatureShape, positional_name: str,
                         kw_name: str, kw_overflow_args_length: str,
                         noarg_name: str) -> tuple[list[ir.Statement], list[ir.Expr]]:
    args_length = ir.Identifier('argsLength')
    args = ir.Identifier('args')
    kwargs = ir.Identifier('kwargs')
    kwargs_bool = ir.BinaryOp('&&',
        ir.BinaryOp('!=', kwargs, ir.Null()),
        ir.MethodCall(kwargs, 'boolValue', []),
    )
    kwargs_len = ir.Identifier('kwargsLen')
    unknown_kw = ir.Identifier('unknownKw')
    entry_value = ir.MethodCall(ir.Identifier('x'), 'getValue', [])
    kwargs_items = ir.MethodCall(ir.Field(kwargs, 'items'), 'entrySet', [])

    def kw_loop_body(known_params: list[inspect.Parameter], include_poskw_duplicates: bool) -> list[ir.Statement]:
        statements: list[ir.Statement] = [
            ir.LocalDecl('PyString', 'kw', ir.CastExpr('PyString', ir.MethodCall(ir.Identifier('x'), 'getKey', []))),
        ]
        conds: list[tuple[ir.Expr, list[ir.Statement]]] = []
        for (i, param) in enumerate(known_params):
            body: list[ir.Statement] = []
            if include_poskw_duplicates and i < len(shape.poskw_params):
                body.append(ir.IfStatement(
                    ir.BinaryOp('!=', ir.Identifier(java_local_name(param.name)), ir.Null()),
                    [runtime_throw('raiseArgGivenByNameAndPosition', [
                        ir.StrLiteral(kw_name),
                        ir.StrLiteral(param.name),
                        ir.IntLiteral(len(shape.posonly_params) + i + 1, ''),
                    ])],
                    [],
                ))
            body.append(ir.AssignStatement(ir.Identifier(java_local_name(param.name)), entry_value))
            conds.append((
                ir.MethodCall(ir.Field(ir.Identifier('kw'), 'value'), 'equals', [ir.StrLiteral(param.name)]),
                body,
            ))
        else_body: list[ir.Statement] = [ir.IfStatement(
            ir.BinaryOp('==', unknown_kw, ir.Null()),
            [ir.AssignStatement(unknown_kw, ir.Field(ir.Identifier('kw'), 'value'))],
            [],
        )]
        if conds:
            statements.append(ir.if_chain(conds, else_body))
        else:
            statements.extend(else_body)
        return statements

    if not shape.posonly_params and not shape.poskw_params and not shape.kwonly_params and \
        shape.vararg_param is not None and shape.varkw_param is not None:
        return ([], [args, kwargs])
    if not shape.posonly_params and not shape.poskw_params and not shape.kwonly_params and shape.vararg_param is not None:
        return ([
            ir.IfStatement(kwargs_bool, [runtime_throw('raiseNoKwArgs', [ir.StrLiteral(noarg_name)])], []),
        ], [args])
    if not shape.posonly_params and not shape.poskw_params and shape.kwonly_params and \
        shape.vararg_param is not None and shape.varkw_param is None:
        statements: list[ir.Statement] = [
            ir.LocalDecl('int', 'argsLength', ir.Field(args, 'length')),
        ]
        for param in shape.kwonly_params:
            statements.append(ir.LocalDecl('PyObject', java_local_name(param.name), ir.Null()))
        statements.append(ir.IfStatement(kwargs_bool, [
            ir.LocalDecl('long', 'kwargsLen', ir.MethodCall(kwargs, 'len', [])),
            ir.IfStatement(
                ir.BinaryOp('>', kwargs_len, ir.IntLiteral(len(shape.kwonly_params), '')),
                [runtime_throw('raiseAtMostKwArgs', [
                    ir.StrLiteral(kw_name),
                    ir.IntLiteral(len(shape.kwonly_params), ''),
                    ir.Identifier(kw_overflow_args_length),
                    kwargs_len,
                ])],
                [],
            ),
            ir.LocalDecl('String', 'unknownKw', ir.Null()),
            ir.ForEachStatement('var', 'x', kwargs_items, kw_loop_body(shape.kwonly_params, False)),
            ir.IfStatement(
                ir.BinaryOp('!=', unknown_kw, ir.Null()),
                [runtime_throw('raiseUnexpectedKwArg', [ir.StrLiteral(kw_name), unknown_kw])],
                [],
            ),
        ], []))
        bind_args: list[ir.Expr] = [args]
        for param in shape.kwonly_params:
            local = ir.Identifier(java_local_name(param.name))
            if param.default is not inspect.Parameter.empty:
                statements.append(ir.IfStatement(
                    ir.BinaryOp('==', local, ir.Null()),
                    [ir.AssignStatement(local, emit_default_java_expr(param.default))],
                    [],
                ))
            bind_args.append(local)
        return (statements, bind_args)

    statements = [ir.LocalDecl('int', 'argsLength', ir.Field(args, 'length'))]
    if shape.params and not shape.poskw_params and not shape.kwonly_params and shape.vararg_param is None:
        statements.append(ir.IfStatement(kwargs_bool, [runtime_throw('raiseNoKwArgs', [ir.StrLiteral(noarg_name)])], []))
        min_args = sum(param.default is inspect.Parameter.empty for param in shape.posonly_params)
        max_args = len(shape.posonly_params)
        bind_args: list[ir.Expr] = [ir.ArrayAccess(args, ir.IntLiteral(i)) for i in range(min_args)]
        if min_args == max_args:
            err: ir.ThrowStatement
            if max_args == 0:
                err = runtime_throw('raiseNoArgs', [args, ir.StrLiteral(noarg_name)])
            elif max_args == 1:
                err = runtime_throw('raiseOneArg', [args, ir.StrLiteral(noarg_name)])
            else:
                err = runtime_throw('raiseExactArgs', [args, ir.IntLiteral(max_args), ir.StrLiteral(kw_name)])
            statements.append(ir.IfStatement(
                ir.BinaryOp('!=', args_length, ir.IntLiteral(max_args)),
                [err],
                [],
            ))
        else:
            if min_args > 0:
                statements.append(ir.IfStatement(
                    ir.BinaryOp('<', args_length, ir.IntLiteral(min_args)),
                    [runtime_throw('raiseMinArgs', [args, ir.IntLiteral(min_args), ir.StrLiteral(kw_name)])],
                    [],
                ))
            statements.append(ir.IfStatement(
                ir.BinaryOp('>', args_length, ir.IntLiteral(max_args)),
                [runtime_throw('raiseMaxArgs', [args, ir.IntLiteral(max_args), ir.StrLiteral(kw_name)])],
                [],
            ))
            for i in range(min_args, max_args):
                statements.append(ir.LocalDecl(
                    'PyObject', f'arg{i}',
                    ir.CondOp(
                        ir.BinaryOp('>=', args_length, ir.IntLiteral(i + 1)),
                        ir.ArrayAccess(args, ir.IntLiteral(i)),
                        emit_default_java_expr(shape.posonly_params[i].default),
                    ),
                ))
                bind_args.append(ir.Identifier(f'arg{i}'))
        return (statements, bind_args)
    if not shape.params:
        statements.extend([
            ir.IfStatement(kwargs_bool, [runtime_throw('raiseNoKwArgs', [ir.StrLiteral(noarg_name)])], []),
            ir.IfStatement(
                ir.BinaryOp('!=', args_length, ir.IntLiteral(0)),
                [runtime_throw('raiseNoArgs', [args, ir.StrLiteral(noarg_name)])],
                [],
            ),
        ])
        return (statements, [])
    posonly_params = shape.posonly_params
    poskw_params = shape.poskw_params
    kwonly_params = shape.kwonly_params
    max_total = shape.max_total
    max_positional = shape.max_positional
    missing_style = shape.missing_style
    if missing_style == 'exact_args':
        statements.append(ir.IfStatement(
            ir.BinaryOp('!=', args_length, ir.IntLiteral(1)),
            [runtime_throw('raiseExactArgs', [args, ir.IntLiteral(1), ir.StrLiteral(positional_name)])],
            [],
        ))
    elif missing_style == 'min_positional':
        statements.append(ir.IfStatement(
            ir.BinaryOp('==', args_length, ir.IntLiteral(0)),
            [type_error_throw(f'{positional_name}() takes at least 1 positional argument (0 given)')],
            [],
        ))
    java_param_names = {param.name: java_local_name(param.name) for param in shape.params}
    for (i, param) in enumerate(posonly_params + poskw_params):
        statements.append(ir.LocalDecl(
            'PyObject', java_param_names[param.name],
            ir.CondOp(
                ir.BinaryOp('>=', args_length, ir.IntLiteral(i + 1)),
                ir.ArrayAccess(args, ir.IntLiteral(i)),
                ir.Null(),
            ),
        ))
    for param in kwonly_params:
        statements.append(ir.LocalDecl('PyObject', java_param_names[param.name], ir.Null()))
    if kwonly_params and not poskw_params:
        if missing_style != 'exact_args' and (max_positional != 0):
            assert False, (positional_name, shape.params, missing_style)
        statements.append(ir.IfStatement(kwargs_bool, [
            ir.LocalDecl('long', 'kwargsLen', ir.MethodCall(kwargs, 'len', [])),
            ir.IfStatement(
                ir.BinaryOp('>', kwargs_len, ir.IntLiteral(max_total)),
                [runtime_throw('raiseAtMostKwArgs', [
                    ir.StrLiteral(kw_name),
                    ir.IntLiteral(max_total),
                    ir.Identifier(kw_overflow_args_length),
                    kwargs_len,
                ])],
                [],
            ),
            ir.LocalDecl('String', 'unknownKw', ir.Null()),
            ir.ForEachStatement('var', 'x', kwargs_items, kw_loop_body(kwonly_params, False)),
            ir.IfStatement(
                ir.BinaryOp('!=', unknown_kw, ir.Null()),
                [runtime_throw('raiseUnexpectedKwArg', [ir.StrLiteral(kw_name), unknown_kw])],
                [],
            ),
        ], []))
        if max_positional == 0:
            statements.append(ir.IfStatement(
                ir.BinaryOp('>', args_length, ir.IntLiteral(max_total)),
                [runtime_throw('raiseAtMostArgs', [ir.StrLiteral(positional_name), ir.IntLiteral(max_total), args_length])],
                [ir.IfStatement(
                    ir.BinaryOp('!=', args_length, ir.IntLiteral(0)),
                    [type_error_throw(f'{positional_name}() takes no positional arguments')],
                    [],
                )],
            ))
    else:
        statements.append(ir.IfStatement(kwargs_bool, [
            ir.LocalDecl('long', 'kwargsLen', ir.MethodCall(kwargs, 'len', [])),
            ir.IfStatement(
                ir.BinaryOp('>', ir.BinaryOp('+', args_length, kwargs_len), ir.IntLiteral(max_total)),
                [runtime_throw('raiseAtMostKwArgs', [
                    ir.StrLiteral(kw_name),
                    ir.IntLiteral(max_total),
                    args_length,
                    kwargs_len,
                ])],
                [],
            ),
            ir.LocalDecl('String', 'unknownKw', ir.Null()),
            ir.ForEachStatement('var', 'x', kwargs_items, kw_loop_body(poskw_params + kwonly_params, True)),
            *([
                ir.IfStatement(
                    ir.BinaryOp('==', ir.Identifier(java_param_names[param.name]), ir.Null()),
                    [runtime_throw('raiseMissingRequiredArg', [
                        ir.StrLiteral(positional_name),
                        ir.StrLiteral(param.name),
                        ir.IntLiteral(i + 1),
                    ])],
                    [],
                )
                for (i, param) in enumerate(posonly_params + poskw_params)
                if param.default is inspect.Parameter.empty and missing_style == 'required_arg'
            ]),
            ir.IfStatement(
                ir.BinaryOp('!=', unknown_kw, ir.Null()),
                [runtime_throw('raiseUnexpectedKwArg', [ir.StrLiteral(kw_name), unknown_kw])],
                [],
            ),
        ], []))
        if max_positional < max_total:
            statements.append(ir.IfStatement(
                ir.BinaryOp('>', args_length, ir.IntLiteral(max_total)),
                [runtime_throw('raiseAtMostArgs', [ir.StrLiteral(positional_name), ir.IntLiteral(max_total), args_length])],
                [ir.IfStatement(
                    ir.BinaryOp('>', args_length, ir.IntLiteral(max_positional)),
                    [runtime_throw('raiseAtMostPosArgs', [ir.StrLiteral(positional_name), ir.IntLiteral(max_positional), args_length])],
                    [],
                )],
            ))
        else:
            statements.append(ir.IfStatement(
                ir.BinaryOp('>', args_length, ir.IntLiteral(max_total)),
                [runtime_throw('raiseAtMostArgs', [ir.StrLiteral(positional_name), ir.IntLiteral(max_total), args_length])],
                [],
            ))
        if missing_style == 'required_arg':
            for (i, param) in enumerate(posonly_params + poskw_params):
                if param.default is inspect.Parameter.empty:
                    statements.append(ir.IfStatement(
                        ir.BinaryOp('==', ir.Identifier(java_param_names[param.name]), ir.Null()),
                        [runtime_throw('raiseMissingRequiredArg', [
                            ir.StrLiteral(positional_name),
                            ir.StrLiteral(param.name),
                            ir.IntLiteral(i + 1),
                        ])],
                        [],
                    ))
    bind_args: list[ir.Expr] = []
    for param in shape.params:
        local = ir.Identifier(java_param_names[param.name])
        if param.default is not inspect.Parameter.empty:
            statements.append(ir.IfStatement(
                ir.BinaryOp('==', local, ir.Null()),
                [ir.AssignStatement(local, emit_default_java_expr(param.default))],
                [],
            ))
        bind_args.append(local)
    return (statements, bind_args)

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

def gen_runtime_java(spec_path: str, java_path: str) -> None:
    with open(spec_path) as f:
        spec = json.load(f)

    pool = ir.ConstantPool('PyGeneratedConstants')
    with open(java_path, 'w') as f:
        writer = ir.IndentedWriter(f, 0)
        python_runtime_helper_classes: list[list[str]] = []
        python_runtime_helper_methods: list[list[str]] = []
        python_builtin_helper_classes: list[list[str]] = []
        python_builtin_helper_methods: list[list[str]] = []
        python_method_helper_classes: list[list[str]] = []
        python_method_helper_methods: list[list[str]] = []
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

            writer.write(f'final class {java_name}Type extends PyConcreteType {{')
            writer.write(ir.FieldDecl('public static final', f'{java_name}Type', 'singleton', ir.CreateObject(f'{java_name}Type', [])).emit_java(pool))
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
                writer.write(ir.FieldDecl('private static final', value.type, f'pyattr_{k}', value).emit_java(pool))
            writer.write('private static final class AttrsHolder {')
            writer.write(f'static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>({len(attrs)});')
            writer.write('static {')
            for k in attrs:
                ir.emit_statement(writer, ir.ExprStatement(
                    ir.MethodCall(
                        ir.Identifier('attrs'),
                        'put',
                        [ir.CreateObject('PyString', [ir.StrLiteral(k)]), ir.Identifier(f'pyattr_{k}')],
                    )
                ), pool)
            writer.write('}')
            writer.write('}')
            writer.write('')
            writer.write(f'private {java_name}Type() {{')
            ir.emit_statement(writer, ir.SuperConstructorCall([
                ir.StrLiteral(py_name),
                ir.Field(ir.Identifier(java_name), 'class'),
                ir.MethodRef(java_name, 'newObj'),
            ]), pool)
            writer.write('}')
            writer.write('@Override public java.util.Map<PyObject, PyObject> getAttributes() {')
            ir.emit_statement(writer, ir.ReturnStatement(ir.Field(ir.Identifier('AttrsHolder'), 'attrs')), pool)
            writer.write('}')
            writer.write('@Override public PyObject lookupAttr(String name) {')
            writer.write('switch (name) {')
            for (k, v) in attrs.items():
                writer.write(f'case {ir.StrLiteral(k).emit_java(pool)}: return pyattr_{k};')
            writer.write('default: return null;')
            writer.write('}')
            writer.write('}')
            writer.write('}')
            writer.write('')

            if name == 'type':
                for method_name in ['mro']:
                    writer.write(f'final class {java_name}Method_{method_name} extends PyBuiltinMethod<{java_name}> {{')
                    writer.write(f'{java_name}Method_{method_name}(PyObject _self) {{')
                    ir.emit_statement(writer, ir.SuperConstructorCall([ir.CastExpr(java_name, ir.Identifier('_self'))]), pool)
                    writer.write('}')
                    writer.write('@Override public String methodName() {')
                    ir.emit_statement(writer, ir.ReturnStatement(ir.StrLiteral(method_name)), pool)
                    writer.write('}')
                    writer.write('@Override public PyObject call(PyObject[] args, PyDict kwargs) {')
                    ir.emit_statement(writer, ir.ThrowStatement(
                        ir.CreateObject('UnsupportedOperationException', [ir.StrLiteral(f'{name}.{method_name}() unimplemented')])
                    ), pool)
                    writer.write('}')
                    writer.write('}')
                    writer.write('')

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
                        (helper_classes, helper_method) = translate_python_method_impl(name, method_name, pool)
                        python_method_helper_classes.extend(helper_classes)
                        python_method_helper_methods.append(helper_method)
                    writer.write(f'final class {method_class_name} extends PyBuiltinMethod<{self_type}> {{')
                    writer.write(f'{method_class_name}({ctor_arg}) {{')
                    ir.emit_statement(writer, ir.SuperConstructorCall([ir.Identifier(super_arg) if super_arg == '_self' else ir.CastExpr(java_name, ir.Identifier('_self'))]), pool)
                    writer.write('}')
                    writer.write('@Override public String methodName() {')
                    ir.emit_statement(writer, ir.ReturnStatement(ir.StrLiteral(method_name)), pool)
                    writer.write('}')
                    writer.write('@Override public PyObject call(PyObject[] args, PyDict kwargs) {')
                    if kwarg_params is None:
                        bind_args = [ir.Identifier('args'), ir.Identifier('kwargs')]
                    else:
                        (bind_statements, bind_args) = build_arg_binding_ir(
                            kwarg_params, method_name,
                            method_name,
                            'argsLength',
                            f'{py_name}.{method_name}',
                        )
                        ir.emit_statements(writer, bind_statements, pool)
                    if kind == 'classmethod':
                        bind_args = [ir.Identifier('self')] + bind_args
                    if method_impl_target is not None:
                        call_args = bind_args if kind == 'classmethod' else [ir.Identifier('self'), *bind_args]
                        call_expr = ir.MethodCall(
                            ir.Identifier(method_impl_target.rsplit('.', 1)[0]),
                            method_impl_target.rsplit('.', 1)[1],
                            call_args,
                        )
                    else:
                        call_expr = ir.MethodCall(ir.Identifier(method_target), f'pymethod_{method_name}', bind_args)
                    ir.emit_statement(writer, ir.ReturnStatement(call_expr), pool)
                    writer.write('}')
                    writer.write('}')
                writer.write('')

        python_runtime_impls = get_top_level_function_names('pythonj_runtime.py')
        if python_runtime_impls:
            for func_name in sorted(python_runtime_impls):
                (helper_classes, helper_method) = translate_python_runtime_impl(func_name, pool)
                python_runtime_helper_classes.extend(helper_classes)
                python_runtime_helper_methods.append(helper_method)
            for code in python_runtime_helper_classes:
                for line in code:
                    writer.write(line)
                writer.write('')
            writer.write('final class PyRuntimePythonImpl {')
            for method in python_runtime_helper_methods:
                for line in method:
                    writer.write(line)
                writer.write('')
            writer.write('}')
            writer.write('')

        if any(PYTHON_AUTHORED_IMPLS.get(name, set()) for name in PYTHON_AUTHORED_IMPLS if name != 'builtins'):
            for code in python_method_helper_classes:
                for line in code:
                    writer.write(line)
                writer.write('')
            writer.write('final class PyBuiltinMethodsPythonImpl {')
            for method in python_method_helper_methods:
                for line in method:
                    writer.write(line)
                writer.write('')
            writer.write('}')
            writer.write('')

        if PYTHON_AUTHORED_IMPLS['builtins']:
            for func_name in sorted(PYTHON_AUTHORED_IMPLS['builtins']):
                (helper_classes, helper_method) = translate_python_builtin_impl(func_name, pool)
                python_builtin_helper_classes.extend(helper_classes)
                python_builtin_helper_methods.append(helper_method)
            for code in python_builtin_helper_classes:
                for line in code:
                    writer.write(line)
                writer.write('')
            writer.write('final class PyBuiltinFunctionsPythonImpl {')
            for method in python_builtin_helper_methods:
                for line in method:
                    writer.write(line)
                writer.write('')
            writer.write('}')
            writer.write('')

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
                writer.write(f'final class {module_func_prefix}Function_{func_name} extends PyBuiltinFunction {{')
                writer.write(ir.FieldDecl('public static final', f'{module_func_prefix}Function_{func_name}', 'singleton', ir.CreateObject(f'{module_func_prefix}Function_{func_name}', [])).emit_java(pool))
                writer.write('')
                writer.write(f'private {module_func_prefix}Function_{func_name}() {{')
                ir.emit_statement(writer, ir.SuperConstructorCall([ir.StrLiteral(func_name)]), pool)
                writer.write('}')
                writer.write('@Override public PyObject call(PyObject[] args, PyDict kwargs) {')
                full_name = f'{module_name}.{func_name}'
                if kwarg_params is None:
                    bind_args = [ir.Identifier('args'), ir.Identifier('kwargs')]
                else:
                    (bind_statements, bind_args) = build_arg_binding_ir(
                        kwarg_params, func_name,
                        full_name,
                        'argsLength',
                        full_name,
                    )
                    ir.emit_statements(writer, bind_statements, pool)
                ir.emit_statement(writer, ir.ReturnStatement(
                    ir.MethodCall(ir.Identifier('PyBuiltinFunctionsImpl'), f'pyfunc_{module_name.removeprefix("_")}_{func_name}', bind_args)
                ), pool)
                writer.write('}')
                writer.write('}')
                writer.write('')

        for func_name in sorted(extract_spec.BUILTIN_FUNCTIONS):
            params = get_builtin_function_params(spec, func_name)
            if params is None or func_name in RAW_ARGS_KWARGS_BUILTINS:
                kwarg_params = None
            else:
                kwarg_params = analyze_params(params)
            writer.write(f'final class PyBuiltinFunction_{func_name} extends PyBuiltinFunction {{')
            writer.write(ir.FieldDecl('public static final', f'PyBuiltinFunction_{func_name}', 'singleton', ir.CreateObject(f'PyBuiltinFunction_{func_name}', [])).emit_java(pool))
            writer.write('')
            writer.write(f'private PyBuiltinFunction_{func_name}() {{')
            ir.emit_statement(writer, ir.SuperConstructorCall([ir.StrLiteral(func_name)]), pool)
            writer.write('}')
            writer.write('@Override public PyObject call(PyObject[] args, PyDict kwargs) {')
            bind_args: list[ir.Expr]
            if kwarg_params is None:
                bind_args = [ir.Identifier('args'), ir.Identifier('kwargs')]
            else:
                kw_name = 'sort' if func_name == 'sorted' else func_name
                kw_overflow_args_length = '0' if func_name == 'sorted' else 'argsLength'
                (bind_statements, bind_args) = build_arg_binding_ir(
                    kwarg_params, func_name,
                    kw_name,
                    kw_overflow_args_length,
                    func_name,
                )
                ir.emit_statements(writer, bind_statements, pool)
            if func_name in PYTHON_AUTHORED_IMPLS['builtins']:
                call_target = 'PyBuiltinFunctionsPythonImpl'
            else:
                call_target = 'PyBuiltinFunctionsImpl'
            ir.emit_statement(writer, ir.ReturnStatement(
                ir.MethodCall(ir.Identifier(call_target), f'pyfunc_{func_name}', bind_args)
            ), pool)
            writer.write('}')
            writer.write('}')
            writer.write('')

        for line in pool.emit_pool():
            writer.write(line)
