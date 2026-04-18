#!/usr/bin/env python3
# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

import argparse
import ast
import inspect
import json
import os
from dataclasses import dataclass
from typing import Optional

import extract_spec
import ir
import pythonj

RAW_ARGS_KWARGS_BUILTINS = {'max', 'min'}
ALL_PYTHON_AUTHORED_IMPLS = {'bool', 'bytearray', 'bytes', 'float', 'int', 'object', 'range', 'tuple'}
PYTHON_AUTHORED_IMPLS = {
    'builtins': {
        'abs', 'all', 'any', 'bin', 'delattr', 'divmod', 'format', 'getattr', 'hasattr', 'hash', 'hex',
        'isinstance', 'issubclass', 'len', 'next', 'oct', 'repr', 'setattr', 'sum',
    },
    'dict': {'__contains__', 'fromkeys', 'setdefault'},
    'set': {'__contains__'},
    'str': {
        '__format__', 'capitalize', 'casefold', 'center', 'encode', 'expandtabs', 'format_map', 'isalnum',
        'isalpha', 'isascii', 'isidentifier', 'islower', 'isnumeric', 'isprintable', 'isspace', 'istitle',
        'isupper', 'join', 'ljust', 'lstrip', 'partition', 'replace', 'rfind', 'rindex', 'rjust', 'rpartition',
        'rsplit', 'removeprefix', 'removesuffix', 'rstrip', 'splitlines', 'strip', 'swapcase', 'title',
        'translate', 'zfill',
    },
}
PYTHON_AUTHORED_CONSTRUCTOR_IMPLS = {'enumerate', 'zip'}
SUPPORTED_HELPER_RETURN_TYPES = {'bool', 'bytes', 'dict', 'float', 'int', 'list', 'str', 'tuple'}

def is_pythonj_getter(func: ast.FunctionDef) -> bool:
    return any(isinstance(decorator, ast.Name) and decorator.id == '__pythonj_getter__' for decorator in func.decorator_list)

def get_slot_wrapper_arity(attr_spec: dict[str, object]) -> int:
    params = extract_spec.decode_signature(attr_spec.get('signature'))
    assert params is not None, attr_spec
    return len(params)

def _annotation_to_metadata(annotation: Optional[ast.expr]) -> Optional[str]:
    if annotation is None:
        return None
    return ast.unparse(annotation)

def _collect_params_annotation_metadata(params: ast.arguments) -> list[dict[str, Optional[str]]]:
    ret = []
    for arg in [*params.posonlyargs, *params.args]:
        ret.append({'name': arg.arg, 'kind': 'positional', 'annotation': _annotation_to_metadata(arg.annotation)})
    if params.vararg is not None:
        ret.append({'name': params.vararg.arg, 'kind': 'vararg', 'annotation': _annotation_to_metadata(params.vararg.annotation)})
    for arg in params.kwonlyargs:
        ret.append({'name': arg.arg, 'kind': 'kwonly', 'annotation': _annotation_to_metadata(arg.annotation)})
    if params.kwarg is not None:
        ret.append({'name': params.kwarg.arg, 'kind': 'varkw', 'annotation': _annotation_to_metadata(params.kwarg.annotation)})
    return ret

def collect_top_level_function_annotation_metadata(node: ast.Module) -> dict[str, dict[str, object]]:
    ret = {}
    for func in node.body:
        if not isinstance(func, ast.FunctionDef):
            continue
        ret[func.name] = {
            'params': _collect_params_annotation_metadata(func.args),
            'return': _annotation_to_metadata(func.returns),
        }
    return ret

def collect_builtin_method_annotation_metadata(node: ast.Module) -> dict[str, dict[str, dict[str, object]]]:
    ret = {}
    for class_node in node.body:
        if not isinstance(class_node, ast.ClassDef):
            continue
        class_ret = {}
        for func in class_node.body:
            if not isinstance(func, ast.FunctionDef):
                continue
            class_ret[func.name] = {
                'params': _collect_params_annotation_metadata(func.args),
                'return': _annotation_to_metadata(func.returns),
            }
        if class_ret:
            ret[class_node.name] = class_ret
    return ret

@dataclass(frozen=True)
class WrapperBindingPlan:
    mode: str
    call_positional_shape: Optional['SignatureShape']
    exact_positional_arity: Optional[int] = None
    posonly_min_max_range: Optional[tuple[int, int]] = None

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

def collect_builtin_method_return_java_types(node: ast.Module) -> dict[tuple[str, str], str]:
    ret: dict[tuple[str, str], str] = {}
    for class_node in [x for x in node.body if isinstance(x, ast.ClassDef)]:
        for func in [x for x in class_node.body if isinstance(x, ast.FunctionDef)]:
            try:
                ret_type = decode_helper_return_annotation(func.returns)
            except ValueError:
                continue
            ret[(class_node.name, func.name)] = ret_type
    return ret

def is_special_method_wrapper(type_name: str, method_name: str) -> bool:
    return type_name == 'type' and method_name == 'mro'

def java_local_name(name: str) -> str:
    if name in ir.JAVA_FORBIDDEN_IDENTIFIERS:
        return f'pyarg_{name}'
    return name

def emit_python_function_static(visitor: pythonj.LoweringVisitor, func_name: str, arg_names: list[str], body: list[ir.Statement],
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
        func_code.append(ir.LocalDecl('PyCell', f'pycell_{name}', ir.CreateObject('PyCell', [ir.Null()])))
    for name in sorted(visitor.scope.info.locals - visitor.scope.info.cell_vars - set(arg_names) - set(visitor.scope.info.initial_builtin_module_locals)):
        func_code.append(ir.LocalDecl(visitor.java_local_type(name), f'pylocal_{name}', ir.Null()))
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
                               scope_infos: Optional[dict[ast.AST, pythonj.ScopeInfo]] = None,
                               python_helper_names: Optional[set[str]] = None,
                               python_helper_class: Optional[str] = None,
                               metadata: Optional[pythonj.TranslatorMetadata] = None,
                               python_helper_return_java_types: Optional[dict[str, str]] = None) -> tuple[list[ir.ClassDecl], ir.MethodDecl]:
    assert metadata is not None, emitted_name
    if scope_infos is None:
        (node, scope_infos) = pythonj.analyze_and_simplify(node, metadata=metadata)
    visitor = pythonj.LoweringVisitor(display_name, scope_infos, scope_infos[node], metadata)
    visitor.pool = pool
    visitor.allow_intrinsics = True
    if python_helper_names is not None:
        visitor.python_helper_names = python_helper_names
        assert python_helper_class is not None, python_helper_class
        visitor.python_helper_class = python_helper_class
        if python_helper_return_java_types is not None:
            visitor.python_helper_return_java_types = python_helper_return_java_types
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
                                  scope_infos: Optional[dict[ast.AST, pythonj.ScopeInfo]] = None,
                                  metadata: Optional[pythonj.TranslatorMetadata] = None) -> tuple[list[ir.ClassDecl], ir.MethodDecl]:
    if funcs is None:
        funcs = pythonj.get_top_level_functions(node)
    func = funcs[func_name]
    return translate_python_impl_node(node, func, func_name, f'<builtin {func_name}>', 'builtin function', pool, scope_infos=scope_infos, metadata=metadata)

def translate_python_method_impl(node: ast.Module, type_name: str, method_name: str, pool: ir.ConstantPool,
                                 class_funcs: Optional[dict[str, dict[str, ast.FunctionDef]]] = None,
                                 scope_infos: Optional[dict[ast.AST, pythonj.ScopeInfo]] = None,
                                 metadata: Optional[pythonj.TranslatorMetadata] = None,
                                 python_helper_names: Optional[set[str]] = None,
                                 python_helper_class: Optional[str] = None,
                                 python_helper_return_java_types: Optional[dict[str, str]] = None) -> tuple[list[ir.ClassDecl], ir.MethodDecl]:
    func_name = f'{type_name}__{method_name}'
    func = pythonj.get_class_functions(node, type_name)[method_name] if class_funcs is None else class_funcs[type_name][method_name]
    return translate_python_impl_node(
        node, func, func_name, f'<method {type_name}.{method_name}>', 'builtin method', pool,
        scope_infos=scope_infos,
        python_helper_names=python_helper_names,
        python_helper_class=python_helper_class,
        metadata=metadata,
        python_helper_return_java_types=python_helper_return_java_types,
    )

def translate_python_constructor_impl(node: ast.Module, type_name: str, pool: ir.ConstantPool,
                                      funcs: Optional[dict[str, ast.FunctionDef]] = None,
                                      scope_infos: Optional[dict[ast.AST, pythonj.ScopeInfo]] = None,
                                      metadata: Optional[pythonj.TranslatorMetadata] = None) -> tuple[list[ir.ClassDecl], ir.MethodDecl]:
    if funcs is None:
        funcs = pythonj.get_top_level_functions(node)
    func_name = f'{type_name}__newobj'
    func = funcs[func_name]
    return translate_python_impl_node(node, func, f'{func_name}_impl', f'<constructor {type_name}>', 'builtin constructor', pool, scope_infos=scope_infos, metadata=metadata)

def translate_python_runtime_impl(node: ast.Module, func_name: str, pool: ir.ConstantPool,
                                  funcs: Optional[dict[str, ast.FunctionDef]] = None,
                                  scope_infos: Optional[dict[ast.AST, pythonj.ScopeInfo]] = None,
                                  metadata: Optional[pythonj.TranslatorMetadata] = None,
                                  python_helper_return_java_types: Optional[dict[str, str]] = None) -> tuple[list[ir.ClassDecl], ir.MethodDecl]:
    if funcs is None:
        funcs = pythonj.get_top_level_functions(node)
    return translate_python_impl_node(
        node, funcs[func_name], func_name, f'<runtime {func_name}>', 'runtime helper', pool,
        scope_infos=scope_infos,
        python_helper_names=set(funcs), python_helper_class='PyRuntime',
        metadata=metadata,
        python_helper_return_java_types=python_helper_return_java_types,
    )

def get_wrapper_binding_plan(shape: Optional[SignatureShape]) -> WrapperBindingPlan:
    call_positional_shape = None
    if shape is not None and shape.varkw_param is None and (shape.vararg_param is None or (not shape.posonly_params and not shape.poskw_params)):
        call_positional_shape = shape
    exact_positional_arity = None if shape is None else extract_spec.get_exact_positional_call_arity(shape.params)
    if exact_positional_arity is not None:
        return WrapperBindingPlan('exact_positional', call_positional_shape, exact_positional_arity=exact_positional_arity)
    posonly_min_max_range = None if shape is None else extract_spec.get_posonly_min_max_call_range(shape.params)
    if posonly_min_max_range is not None:
        return WrapperBindingPlan('posonly_min_max', call_positional_shape, posonly_min_max_range=posonly_min_max_range)
    if shape is None:
        return WrapperBindingPlan('fallback_raw', call_positional_shape)
    call_range = extract_spec.get_positional_call_range(shape.params)
    if shape.poskw_params and not shape.kwonly_params and shape.vararg_param is None and shape.varkw_param is None and call_range is not None and call_range[0] != call_range[1]:
        return WrapperBindingPlan('poskw_min_max_python', call_positional_shape)
    if shape.kwonly_params and shape.vararg_param is None and shape.varkw_param is None and all(param.kind in {inspect.Parameter.POSITIONAL_ONLY, inspect.Parameter.POSITIONAL_OR_KEYWORD, inspect.Parameter.KEYWORD_ONLY} for param in shape.params) and all(param.default is not inspect.Parameter.empty for param in shape.kwonly_params):
        return WrapperBindingPlan('pos_kwonly_python', call_positional_shape)
    if shape.vararg_param is not None and shape.kwonly_params and shape.varkw_param is None and not shape.posonly_params and not shape.poskw_params and all(param.default is not inspect.Parameter.empty for param in shape.kwonly_params):
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
            ir.CreateObject('PyInt', [ir.Field(ir.Identifier('args'), 'length')]), ir.Identifier('kwargs'),
            ir.PyConstant(exact_kw_name), ir.PyConstant(exact_positional_name), ir.PyConstant(plan.exact_positional_arity), ir.PyConstant(False),
        ]))
        bind_args = [ir.ArrayAccess(ir.Identifier('args'), ir.IntLiteral(i)) for i in range(plan.exact_positional_arity)]
    elif plan.mode == 'posonly_min_max':
        assert plan.posonly_min_max_range is not None
        statements.append(ir.method_call_statement(ir.Identifier('PyRuntime'), 'pyfunc_require_min_max_positional', [
            ir.CreateObject('PyInt', [ir.Field(ir.Identifier('args'), 'length')]), ir.Identifier('kwargs'),
            ir.PyConstant(posonly_kw_name), ir.PyConstant(posonly_positional_name),
            ir.PyConstant(plan.posonly_min_max_range[0]), ir.PyConstant(plan.posonly_min_max_range[1]),
        ]))
        bind_args = [ir.ArrayAccess(ir.Identifier('args'), ir.IntLiteral(i)) for i in range(plan.posonly_min_max_range[0])]
        bind_args.extend(ir.CondOp(ir.BinaryOp('>', ir.Field(ir.Identifier('args'), 'length'), ir.IntLiteral(i)), ir.ArrayAccess(ir.Identifier('args'), ir.IntLiteral(i)), ir.Null()) for i in range(plan.posonly_min_max_range[0], plan.posonly_min_max_range[1]))
    elif plan.mode == 'poskw_min_max_python':
        assert kwarg_params is not None
        (min_args, max_args) = extract_spec.get_positional_call_range(kwarg_params.params)
        assert min_args is not None and max_args is not None
        statements.append(ir.LocalDecl('var', 'boundArgs', ir.StaticMethodCall('Runtime', 'bindMinMaxPositionalOrKeyword', [
            ir.Identifier('args'), ir.Identifier('kwargs'), ir.PyConstant(poskw_kw_name), ir.PyConstant(poskw_positional_name),
            ir.PyConstant(tuple(param.name for param in kwarg_params.posonly_params + kwarg_params.poskw_params)),
            ir.PyConstant(len(kwarg_params.posonly_params)), ir.PyConstant(tuple()), ir.PyConstant(min_args), ir.PyConstant(max_args),
            ir.PyConstant(max_args), ir.PyConstant(kwarg_params.missing_style == 'min_positional'), ir.PyConstant(False),
        ])))
        bind_args = [ir.MethodCall(ir.Identifier('boundArgs'), 'get', [ir.IntLiteral(i)]) for i in range(max_args)]
    elif plan.mode == 'pos_kwonly_python':
        assert kwarg_params is not None
        statements.append(ir.LocalDecl('var', 'boundArgs', ir.StaticMethodCall('Runtime', 'bindMinMaxPositionalOrKeyword', [
            ir.Identifier('args'), ir.Identifier('kwargs'), ir.PyConstant(poskw_kw_name), ir.PyConstant(poskw_positional_name),
            ir.PyConstant(tuple(param.name for param in kwarg_params.posonly_params + kwarg_params.poskw_params)),
            ir.PyConstant(len(kwarg_params.posonly_params)), ir.PyConstant(tuple(param.name for param in kwarg_params.kwonly_params)),
            ir.PyConstant(sum(param.default is inspect.Parameter.empty for param in kwarg_params.posonly_params + kwarg_params.poskw_params)),
            ir.PyConstant(len(kwarg_params.posonly_params) + len(kwarg_params.poskw_params)), ir.PyConstant(len(kwarg_params.params)),
            ir.PyConstant(kwarg_params.missing_style == 'min_positional'), ir.PyConstant(kwarg_params.missing_style == 'exact_args'),
        ])))
        bind_args = [ir.MethodCall(ir.Identifier('boundArgs'), 'get', [ir.IntLiteral(i)]) for i in range(len(kwarg_params.params))]
    elif plan.mode == 'vararg_kwonly_python':
        assert kwarg_params is not None
        statements.append(ir.LocalDecl('var', 'boundArgs', ir.Field(ir.StaticMethodCall('PyRuntime', 'pyfunc_bind_varargs_and_kwonly', [
            ir.Identifier('kwargs'), ir.PyConstant(general_kw_name), ir.PyConstant(tuple(param.name for param in kwarg_params.kwonly_params)),
        ]), 'items')))
        bind_args = [ir.Identifier('args'), *(ir.MethodCall(ir.Identifier('boundArgs'), 'get', [ir.IntLiteral(i)]) for i in range(len(kwarg_params.kwonly_params)))]
    elif plan.mode == 'fallback_raw':
        bind_args = [ir.Identifier('args'), ir.Identifier('kwargs')]
    else:
        assert kwarg_params is not None
        if not kwarg_params.posonly_params and not kwarg_params.poskw_params and not kwarg_params.kwonly_params and kwarg_params.vararg_param is not None and kwarg_params.varkw_param is not None:
            bind_args = [ir.Identifier('args'), ir.Identifier('kwargs')]
        elif not kwarg_params.posonly_params and not kwarg_params.poskw_params and not kwarg_params.kwonly_params and kwarg_params.vararg_param is not None:
            statements.append(ir.method_call_statement(ir.Identifier('Runtime'), 'requireNoKwArgs', [ir.Identifier('kwargs'), ir.StrLiteral(noarg_name)]))
            bind_args = [ir.Identifier('args')]
        else:
            assert False, (kwarg_params, noarg_name)
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
            statements.append(ir.IfStatement(ir.BinaryOp('==', local, ir.Null()), [ir.AssignStatement(local, pythonj.emit_default_java_expr(shape.params[i].default))], []))
            bind_args.append(local)
        else:
            bind_args.append(ir.Identifier(f'arg{i}'))
    return (call_args, statements, bind_args)
def write_semantics_json(path: str, data: dict[str, object]) -> None:
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, sort_keys=True)
        f.write('\n')

def build_semantics_data(pythonj_builtins_node: ast.Module, pythonj_runtime_node: ast.Module) -> dict[str, object]:
    builtin_function_return_types = pythonj.collect_top_level_exact_return_types(pythonj_builtins_node)
    builtin_method_return_types = pythonj.collect_builtin_method_exact_return_types(pythonj_builtins_node)
    return {
        'builtin_function_annotations': collect_top_level_function_annotation_metadata(pythonj_builtins_node),
        'builtin_method_annotations': collect_builtin_method_annotation_metadata(pythonj_builtins_node),
        'runtime_function_annotations': collect_top_level_function_annotation_metadata(pythonj_runtime_node),
        'builtin_function_return_types': builtin_function_return_types,
        'builtin_method_return_types': builtin_method_return_types,
        'builtin_method_return_java_types': {
            f'{class_name}.{method_name}': return_type
            for ((class_name, method_name), return_type) in collect_builtin_method_return_java_types(pythonj_builtins_node).items()
        },
        'runtime_function_return_java_types': {
            func_name: decode_helper_return_annotation(func.returns)
            for (func_name, func) in pythonj.get_top_level_functions(pythonj_runtime_node).items()
        },
    }

def gen_runtime_artifacts(spec_path: str, java_path: str, semantics_path: str) -> None:
    pythonj_builtins_node = pythonj.parse_python_module('pythonj_builtins.py')
    pythonj_runtime_node = pythonj.parse_python_module('pythonj_runtime.py')
    semantics_data = build_semantics_data(pythonj_builtins_node, pythonj_runtime_node)
    metadata = pythonj.resolve_translator_metadata(pythonj.load_spec_json(spec_path), semantics_data)
    spec = metadata.spec
    (pythonj_builtins_node, builtins_scope_infos) = pythonj.analyze_and_simplify(
        pythonj_builtins_node, metadata=metadata)
    (pythonj_runtime_node, runtime_scope_infos) = pythonj.analyze_and_simplify(
        pythonj_runtime_node, metadata=metadata)
    pythonj_builtins_funcs = pythonj.get_top_level_functions(pythonj_builtins_node)
    pythonj_builtins_classes = {
        class_name: {x.name: x for x in class_node.body if isinstance(x, ast.FunctionDef)}
        for (class_name, class_node) in {x.name: x for x in pythonj_builtins_node.body if isinstance(x, ast.ClassDef)}.items()
    }
    pythonj_runtime_funcs = pythonj.get_top_level_functions(pythonj_runtime_node)
    write_semantics_json(semantics_path, semantics_data)
    pool = ir.ConstantPool('PyRuntime')
    with open(java_path, 'w') as f:
        top_level_decls: list[ir.Decl] = []
        python_helper_classes: list[ir.ClassDecl] = []
        python_helper_methods: list[ir.Decl] = []
        emitted_python_getter_helpers: set[tuple[str, str]] = set()
        emitted_python_method_helpers: set[tuple[str, str]] = set()
        for (name, obj_spec) in spec.items():
            if obj_spec['kind'] != 'type':
                continue
            attrs = extract_spec.get_type_attrs(spec, name)
            java_name = pythonj.get_java_name(name)
            match name:
                case '_types.BuiltinFunctionType': py_name = 'builtin_function_or_method'
                case '_types.ClassMethodDescriptorType': py_name = 'classmethod_descriptor'
                case '_types.FunctionType': py_name = 'function'
                case '_types.GeneratorType': py_name = 'generator'
                case '_types.GetSetDescriptorType': py_name = 'getset_descriptor'
                case '_types.MappingProxyType': py_name = 'mappingproxy'
                case '_types.MethodWrapperType': py_name = 'method-wrapper'
                case '_types.MemberDescriptorType': py_name = 'member_descriptor'
                case '_types.MethodDescriptorType': py_name = 'method_descriptor'
                case '_types.ModuleType': py_name = 'module'
                case '_types.NoneType': py_name = 'NoneType'
                case '_types.WrapperDescriptorType': py_name = 'wrapper_descriptor'
                case _: py_name = name

            type_decls: list[ir.Decl] = [
                ir.FieldDecl('public static final', f'{java_name}Type', 'singleton', ir.CreateObject(f'{java_name}Type', [])),
            ]
            attr_holder_decls: list[ir.Decl] = []
            for (k, v) in extract_spec.iter_type_attrs(spec, name):
                doc_value = v.get('doc')
                if v['kind'] == 'none':
                    value = ir.Field(ir.Identifier('PyNone'), 'singleton', 'PyNone')
                elif v['kind'] == 'string':
                    value = ir.CreateObject('PyString', [ir.StrLiteral(v['value'])])
                elif v['kind'] == 'member':
                    getter_target_class = java_name
                    getter_target_method = f'pyget_{k}'
                    getter_func = pythonj_builtins_classes.get(name, {}).get(k)
                    if getter_func is not None and is_pythonj_getter(getter_func):
                        helper_key = (name, k)
                        helper_name = f'{name.replace(".", "_")}__{k}'
                        getter_target_class = 'PyRuntime'
                        getter_target_method = f'pyfunc_{helper_name}'
                        if helper_key not in emitted_python_getter_helpers:
                            (helper_classes, helper_method) = translate_python_method_impl(
                                pythonj_builtins_node, name, k, pool,
                                class_funcs=pythonj_builtins_classes, scope_infos=builtins_scope_infos,
                                metadata=metadata,
                                python_helper_names=set(pythonj_runtime_funcs),
                                python_helper_class='PyRuntime',
                                python_helper_return_java_types=metadata.runtime_function_return_java_types,
                            )
                            python_helper_classes.extend(helper_classes)
                            python_helper_methods.append(helper_method)
                            emitted_python_getter_helpers.add(helper_key)
                    value = ir.CreateObject('PyMemberDescriptor', [
                        ir.Identifier('singleton'),
                        ir.StrLiteral(k),
                        ir.MethodRef(getter_target_class, getter_target_method),
                        ir.Null() if doc_value is None else ir.StrLiteral(doc_value),
                    ])
                elif v['kind'] == 'getset':
                    getter_target_class = java_name
                    getter_target_method = f'pyget_{k}'
                    getter_func = pythonj_builtins_classes.get(name, {}).get(k)
                    if getter_func is not None and is_pythonj_getter(getter_func):
                        helper_key = (name, k)
                        helper_name = f'{name.replace(".", "_")}__{k}'
                        getter_target_class = 'PyRuntime'
                        getter_target_method = f'pyfunc_{helper_name}'
                        if helper_key not in emitted_python_getter_helpers:
                            (helper_classes, helper_method) = translate_python_method_impl(
                                pythonj_builtins_node, name, k, pool,
                                class_funcs=pythonj_builtins_classes, scope_infos=builtins_scope_infos,
                                metadata=metadata,
                                python_helper_names=set(pythonj_runtime_funcs),
                                python_helper_class='PyRuntime',
                                python_helper_return_java_types=metadata.runtime_function_return_java_types,
                            )
                            python_helper_classes.extend(helper_classes)
                            python_helper_methods.append(helper_method)
                            emitted_python_getter_helpers.add(helper_key)
                    value = ir.CreateObject('PyGetSetDescriptor', [
                        ir.Identifier('singleton'),
                        ir.StrLiteral(k),
                        ir.MethodRef(getter_target_class, getter_target_method),
                        ir.Null() if doc_value is None else ir.StrLiteral(doc_value),
                    ])
                elif v['kind'] == 'method':
                    value = ir.CreateObject('PyMethodDescriptor', [
                        ir.Identifier('singleton'),
                        ir.StrLiteral(k),
                        ir.MethodRef(f'{java_name}Method_{k}', 'new'),
                        ir.Null() if doc_value is None else ir.StrLiteral(doc_value),
                    ])
                elif v['kind'] == 'wrapper_descriptor':
                    value = ir.CreateObject('PyWrapperDescriptor', [
                        ir.Identifier('singleton'),
                        ir.StrLiteral(k),
                        ir.MethodRef(f'{java_name}MethodWrapper_{k}', 'new'),
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
                attr_holder_decls.append(ir.FieldDecl('static final', value.java_type(), f'pyattr_{k}', value))
            attr_holder_decls.append(ir.FieldDecl(
                'static final',
                'java.util.LinkedHashMap<PyObject, PyObject>',
                'attrs',
                ir.CreateObject('java.util.LinkedHashMap<PyObject, PyObject>', [ir.IntLiteral(len(attrs))]),
            ))
            attr_holder_decls.append(ir.StaticBlock([
                ir.method_call_statement(
                    ir.Identifier('attrs'),
                    'put',
                    [ir.CreateObject('PyString', [ir.StrLiteral(k)]), ir.Identifier(f'pyattr_{k}')],
                )
                for k in attrs
            ]))
            type_decls.append(ir.ClassDecl('static final', 'AttrsHolder', None, attr_holder_decls))
            doc_string = spec[name].get('doc')
            super_args: list[ir.Expr] = [
                ir.StrLiteral(py_name),
                ir.StrLiteral(py_name),
                ir.StrLiteral('builtins'),
                ir.Field(ir.Identifier(java_name), 'class'),
            ]
            if (base_type_name := extract_spec.get_type_base_name(name)) is not None:
                base_type_java = pythonj.get_java_name(base_type_name)
                super_args.append(ir.Field(ir.Identifier(f'{base_type_java}Type'), 'singleton'))
            else:
                super_args.append(ir.Null())
            super_args.append(ir.StrLiteral(doc_string) if doc_string is not None else ir.Null())
            type_decls.append(ir.ConstructorDecl('private', f'{java_name}Type', [], [
                ir.SuperConstructorCall(super_args),
            ]))
            type_decls.append(ir.MethodDecl('@Override public', 'PyObject', 'call', ['PyObject[] args', 'PyDict kwargs'], [
                ir.ReturnStatement(
                    ir.MethodCall(
                        ir.Identifier('PyRuntime' if name in PYTHON_AUTHORED_CONSTRUCTOR_IMPLS else java_name),
                        f'pyfunc_{name}__newobj' if name in PYTHON_AUTHORED_CONSTRUCTOR_IMPLS else 'newObj',
                        [ir.This(), ir.Identifier('args'), ir.Identifier('kwargs')],
                    )
                ),
            ]))
            type_decls.append(ir.MethodDecl('@Override public', 'java.util.Map<PyObject, PyObject>', 'getAttributes', [], [
                ir.ReturnStatement(ir.Field(ir.Identifier('AttrsHolder'), 'attrs')),
            ]))
            type_decls.append(ir.MethodDecl('@Override public', 'PyObject', 'lookupAttr', ['String name'], [
                ir.SwitchStatement(ir.Identifier('name'), [
                    ir.SwitchCase(ir.StrLiteral(k), ir.Field(ir.Identifier('AttrsHolder'), f'pyattr_{k}'))
                    for k in attrs
                ], ir.MethodCall(ir.This(), 'lookupBaseAttr', [ir.Identifier('name')])),
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
                            ir.MethodDecl('@Override public', 'String', 'methodName', [], [
                                ir.ReturnStatement(ir.StrLiteral(method_name)),
                            ]),
                            ir.MethodDecl('@Override public', 'PyObject', 'call', ['PyObject[] args', 'PyDict kwargs'], [
                                ir.ThrowStatement(ir.CreateObject('UnsupportedOperationException', [ir.StrLiteral(f'{name}.{method_name}() unimplemented')])),
                            ]),
                        ],
                    ))

            for (method_name, attr_spec) in attrs.items():
                if attr_spec['kind'] != 'wrapper_descriptor':
                    continue
                arity = get_slot_wrapper_arity(attr_spec)
                if method_name in pythonj_builtins_classes.get(name, {}):
                    helper_key = (name, method_name)
                    helper_name = f'{name.replace(".", "_")}__{method_name}'
                    if helper_key not in emitted_python_method_helpers:
                        (helper_classes, helper_method) = translate_python_method_impl(
                            pythonj_builtins_node, name, method_name, pool,
                            class_funcs=pythonj_builtins_classes, scope_infos=builtins_scope_infos,
                            metadata=metadata,
                            python_helper_names=set(pythonj_runtime_funcs),
                            python_helper_class='PyRuntime',
                            python_helper_return_java_types=metadata.runtime_function_return_java_types,
                        )
                        python_helper_classes.extend(helper_classes)
                        python_helper_methods.append(helper_method)
                        emitted_python_method_helpers.add(helper_key)
                    wrapper_return_expr: ir.Expr = ir.StaticMethodCall(
                        'PyRuntime',
                        f'pyfunc_{helper_name}',
                        [ir.Identifier('self')] if arity == 0 else
                        [ir.Identifier('self'), ir.Identifier('arg0')] if arity == 1 else
                        [ir.Identifier('self'), ir.Identifier('arg0'), ir.Identifier('arg1')],
                        metadata.builtin_method_return_java_types.get((name.rsplit('.', 1)[-1], method_name), 'PyObject'),
                    )
                else:
                    if method_name == '__bool__':
                        wrapper_return_expr = ir.StaticMethodCall('PyBool', 'create', [ir.MethodCall(ir.Identifier('self'), 'boolValue', [], 'boolean')], 'PyBool')
                    elif method_name == '__repr__':
                        wrapper_return_expr = ir.CreateObject('PyString', [ir.MethodCall(ir.Identifier('self'), 'repr', [], 'String')])
                    elif method_name == '__contains__':
                        wrapper_return_expr = ir.StaticMethodCall('PyBool', 'create', [ir.MethodCall(ir.Identifier('self'), 'contains', [ir.Identifier('arg0')], 'boolean')], 'PyBool')
                    else:
                        wrapper_return_expr = None
                wrapper_call_body: list[ir.Statement] = []
                if arity == 0:
                    wrapper_call_body.append(ir.method_call_statement(ir.This(), 'requireNoArgs', [ir.Identifier('args'), ir.Identifier('kwargs')]))
                elif arity == 1:
                    wrapper_call_body.extend([
                        ir.method_call_statement(ir.This(), 'requireExactArgs', [ir.Identifier('args'), ir.Identifier('kwargs'), ir.IntLiteral(1)]),
                        ir.LocalDecl('PyObject', 'arg0', ir.ArrayAccess(ir.Identifier('args'), ir.IntLiteral(0))),
                    ])
                else:
                    wrapper_call_body.extend([
                        ir.method_call_statement(ir.Identifier('Runtime'), 'requireNoKwArgs', [ir.Identifier('kwargs'), ir.StrLiteral(method_name)]),
                        ir.IfStatement(
                            ir.BinaryOp('!=', ir.Field(ir.Identifier('args'), 'length', 'int'), ir.IntLiteral(arity)),
                            [ir.ThrowStatement(ir.StaticMethodCall('PyTypeError', 'raise', [
                                ir.BinaryOp(
                                    '+',
                                    ir.StrLiteral(f'{method_name} expected {arity} arguments, got '),
                                    ir.StaticMethodCall('String', 'valueOf', [ir.Field(ir.Identifier('args'), 'length', 'int')], 'String'),
                                )
                            ]))],
                            [],
                        ),
                    ])
                    for i in range(arity):
                        wrapper_call_body.append(ir.LocalDecl('PyObject', f'arg{i}', ir.ArrayAccess(ir.Identifier('args'), ir.IntLiteral(i))))
                if wrapper_return_expr is not None:
                    wrapper_call_body.append(ir.ReturnStatement(wrapper_return_expr))
                else:
                    wrapper_call_body.append(ir.ThrowStatement(ir.CreateObject('UnsupportedOperationException', [ir.StrLiteral(f'{name}.{method_name}() unimplemented')])))
                top_level_decls.append(ir.ClassDecl(
                    'final',
                    f'{java_name}MethodWrapper_{method_name}',
                    f'PyMethodWrapper<{java_name}>',
                    [
                        ir.ConstructorDecl('', f'{java_name}MethodWrapper_{method_name}', ['PyObject _self'], [
                            ir.SuperConstructorCall([ir.StrLiteral(method_name), ir.CastExpr(java_name, ir.Identifier('_self'))]),
                        ]),
                        ir.MethodDecl('@Override public', 'PyObject', 'call', ['PyObject[] args', 'PyDict kwargs'], wrapper_call_body),
                    ],
                ))

            gen_methods = {}
            for (method_name, kind, params) in extract_spec.iter_type_methods(spec, name):
                if is_special_method_wrapper(name, method_name):
                    continue
                gen_methods[method_name] = None if params is None else pythonj.analyze_params(params)

            if gen_methods:
                py_name = name.rsplit('.')[1] if '.' in name else name
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
                    has_python_builtin_body = method_name in pythonj_builtins_classes.get(name, {})
                    if kind in {'method', 'classmethod'} and (
                        (name in ALL_PYTHON_AUTHORED_IMPLS and has_python_builtin_body) or
                        method_name in PYTHON_AUTHORED_IMPLS.get(name, set())
                    ):
                        helper_name = f'{name.replace(".", "_")}__{method_name}'
                        method_impl_target = f'PyRuntime.pyfunc_{helper_name}'
                        (helper_classes, helper_method) = translate_python_method_impl(
                            pythonj_builtins_node, name, method_name, pool,
                            class_funcs=pythonj_builtins_classes, scope_infos=builtins_scope_infos,
                            metadata=metadata,
                            python_helper_names=set(pythonj_runtime_funcs),
                            python_helper_class='PyRuntime',
                            python_helper_return_java_types=metadata.runtime_function_return_java_types,
                        )
                        python_helper_classes.extend(helper_classes)
                        python_helper_methods.append(helper_method)
                    decls: list[ir.Decl] = [
                        ir.ConstructorDecl('', method_class_name, [ctor_arg], [
                            ir.SuperConstructorCall([ir.Identifier(super_arg) if super_arg == '_self' else ir.CastExpr(java_name, ir.Identifier('_self'))]),
                        ]),
                        ir.MethodDecl('@Override public', 'String', 'methodName', [], [ir.ReturnStatement(ir.StrLiteral(method_name))]),
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
                    call_positional_return_java_type = 'PyObject'
                    if method_impl_target is not None:
                        call_positional_return_java_type = metadata.builtin_method_return_java_types.get((name.rsplit('.', 1)[-1], method_name), 'PyObject')
                    if call_positional_shape is not None:
                        call_expr = ir.StaticMethodCall(
                            method_class_name,
                            'callPositional',
                            bind_args if kind == 'classmethod' else [ir.Identifier('self'), *bind_args],
                            call_positional_return_java_type,
                        )
                    elif method_impl_target is not None:
                        call_args = bind_args if kind == 'classmethod' else [ir.Identifier('self'), *bind_args]
                        call_expr = ir.StaticMethodCall(
                            method_impl_target.rsplit('.', 1)[0],
                            method_impl_target.rsplit('.', 1)[1],
                            call_args,
                            metadata.builtin_method_return_java_types.get((name.rsplit('.', 1)[-1], method_name), ir.JAVA_TYPE_UNKNOWN),
                        )
                    else:
                        call_expr = ir.MethodCall(ir.Identifier(method_target), f'pymethod_{method_name}', bind_args)
                    call_body.append(ir.ReturnStatement(call_expr))
                    decls.append(ir.MethodDecl('@Override public', 'PyObject', 'call', ['PyObject[] args', 'PyDict kwargs'], call_body))
                    if call_positional_shape is not None:
                        (call_positional_method_args, call_positional_statements, call_positional_args) = build_call_positional_ir(call_positional_shape)
                        call_positional_method_args = [f'{self_type} self', *call_positional_method_args]
                        if method_impl_target is not None:
                            call_args = [ir.Identifier('self'), *call_positional_args]
                            call_positional_expr = ir.StaticMethodCall(
                                method_impl_target.rsplit('.', 1)[0],
                                method_impl_target.rsplit('.', 1)[1],
                                call_args,
                                metadata.builtin_method_return_java_types.get((name.rsplit('.', 1)[-1], method_name), ir.JAVA_TYPE_UNKNOWN),
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
                            call_positional_return_java_type,
                            'callPositional',
                            call_positional_method_args,
                            [*call_positional_statements, ir.ReturnStatement(call_positional_expr)],
                        ))
                    top_level_decls.append(ir.ClassDecl('final', method_class_name, f'PyBuiltinMethod<{self_type}>', decls))

        for func_name in sorted(pythonj_runtime_funcs):
            (helper_classes, helper_method) = translate_python_runtime_impl(
                pythonj_runtime_node, func_name, pool, funcs=pythonj_runtime_funcs, scope_infos=runtime_scope_infos,
                metadata=metadata,
                python_helper_return_java_types=metadata.runtime_function_return_java_types,
            )
            python_helper_classes.extend(helper_classes)
            python_helper_methods.append(helper_method)

        for type_name in sorted(PYTHON_AUTHORED_CONSTRUCTOR_IMPLS):
            (helper_classes, helper_method) = translate_python_constructor_impl(
                pythonj_builtins_node, type_name, pool, funcs=pythonj_builtins_funcs, scope_infos=builtins_scope_infos,
                metadata=metadata,
            )
            python_helper_classes.extend(helper_classes)
            emitted_name = f'{type_name}__newobj_impl'
            python_helper_methods.append(ir.MethodDecl(
                'static',
                'PyObject',
                f'pyfunc_{type_name}__newobj',
                ['PyConcreteType type', 'PyObject[] args', 'PyDict kwargs'],
                [
                    ir.ReturnStatement(ir.MethodCall(
                        ir.Identifier('PyRuntime'),
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
            python_helper_methods.append(helper_method)

        for func_name in sorted(PYTHON_AUTHORED_IMPLS['builtins']):
            (helper_classes, helper_method) = translate_python_builtin_impl(
                pythonj_builtins_node, func_name, pool, funcs=pythonj_builtins_funcs, scope_infos=builtins_scope_infos,
                metadata=metadata,
            )
            python_helper_classes.extend(helper_classes)
            python_helper_methods.append(helper_method)
        top_level_decls.extend(python_helper_classes)

        for module_name in sorted(extract_spec.BUILTIN_MODULES):
            attrs = extract_spec.get_module_attrs(spec, module_name)
            module_java_name = extract_spec.BUILTIN_MODULES[module_name]
            module_func_prefix = module_java_name.removesuffix('Module')
            for (func_name, params) in sorted(extract_spec.iter_module_functions(spec, module_name)):
                kwarg_params = None if params is None else pythonj.analyze_params(params)
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
                call_expr = ir.StaticMethodCall(f'{module_func_prefix}Function_{func_name}', 'callPositional', bind_args) if call_positional_shape is not None else ir.StaticMethodCall('PyBuiltinFunctionsImpl', f'pyfunc_{module_name.removeprefix("_")}_{func_name}', bind_args)
                call_body.append(ir.ReturnStatement(call_expr))
                decls.append(ir.MethodDecl('@Override public', 'PyObject', 'call', ['PyObject[] args', 'PyDict kwargs'], call_body))
                if call_positional_shape is not None:
                    (call_positional_method_args, call_positional_statements, call_positional_args) = build_call_positional_ir(call_positional_shape)
                    decls.append(ir.MethodDecl(
                        'public static',
                        'PyObject',
                        'callPositional',
                        call_positional_method_args,
                        [
                            *call_positional_statements,
                            ir.ReturnStatement(
                                ir.StaticMethodCall('PyBuiltinFunctionsImpl', f'pyfunc_{module_name.removeprefix("_")}_{func_name}', call_positional_args)
                            ),
                        ],
                    ))
                top_level_decls.append(ir.ClassDecl('final', f'{module_func_prefix}Function_{func_name}', 'PyBuiltinFunction', decls))

        for func_name in sorted(extract_spec.BUILTIN_FUNCTIONS):
            params = extract_spec.get_builtin_function_params(spec, func_name)
            kwarg_params = None if params is None or func_name in RAW_ARGS_KWARGS_BUILTINS else pythonj.analyze_params(params)
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
                call_target = 'PyRuntime'
                call_positional_return_java_type = pythonj.exact_builtin_type_to_java_type(type_name) if (
                    (type_name := metadata.builtin_function_return_types.get(func_name)) is not None
                ) else 'PyObject'
            else:
                call_target = 'PyBuiltinFunctionsImpl'
                call_positional_return_java_type = 'PyObject'
            call_expr = ir.StaticMethodCall(f'PyBuiltinFunction_{func_name}', 'callPositional', bind_args, call_positional_return_java_type) if call_positional_shape is not None else ir.StaticMethodCall(call_target, f'pyfunc_{func_name}', bind_args)
            call_body.append(ir.ReturnStatement(call_expr))
            decls.append(ir.MethodDecl('@Override public', 'PyObject', 'call', ['PyObject[] args', 'PyDict kwargs'], call_body))
            if call_positional_shape is not None:
                (call_positional_method_args, call_positional_statements, call_positional_args) = build_call_positional_ir(call_positional_shape)
                decls.append(ir.MethodDecl(
                    'public static',
                    call_positional_return_java_type,
                    'callPositional',
                    call_positional_method_args,
                    [
                        *call_positional_statements,
                        ir.ReturnStatement(
                            ir.StaticMethodCall(
                                call_target,
                                f'pyfunc_{func_name}',
                                call_positional_args,
                                call_positional_return_java_type,
                            )
                        ),
                    ],
                ))
            top_level_decls.append(ir.ClassDecl('final', f'PyBuiltinFunction_{func_name}', 'PyBuiltinFunction', decls))

        py_runtime_decl = ir.ClassDecl('final', 'PyRuntime', None, python_helper_methods)
        for decl in [*top_level_decls, py_runtime_decl]:
            for _ in decl.emit_java(pool):
                pass
        top_level_decls.append(ir.with_pooled_fields(py_runtime_decl, pool))
        ir.write_decls(f, top_level_decls, pool)

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--spec', required=True)
    parser.add_argument('--semantics', required=True)
    parser.add_argument('-o', '--output', required=True)
    args = parser.parse_args()
    gen_runtime_artifacts(
        os.path.abspath(args.spec),
        os.path.abspath(args.output),
        os.path.abspath(args.semantics),
    )

if __name__ == '__main__':
    main()
