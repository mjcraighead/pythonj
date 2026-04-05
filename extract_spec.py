#!/usr/bin/env python3
# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

import argparse
import builtins
import inspect
import json
import os
import types
from typing import Any
import _io

BUILTIN_FUNCTIONS = {
    'abs', 'all', 'any', 'ascii', 'bin', 'chr', 'delattr', 'dir', 'format', 'getattr', 'hasattr', 'hash', 'hex',
    'isinstance', 'issubclass', 'iter', 'len', 'max', 'min', 'next', 'open', 'ord', 'print', 'repr',
    'setattr', 'sorted', 'sum', 'oct',
}
BUILTIN_MODULES = {
    '_json': 'PyJsonModule',
    '_operator': 'PyOperatorModule',
    '_types': 'PyTypesModule',
    'math': 'PyMathModule',
    'zlib': 'PyZlibModule',
}
BUILTIN_MODULE_ATTRS = {
    '_json': {'encode_basestring_ascii', 'scanstring'},
    '_operator': {'contains', 'delitem', 'getitem', 'index', 'setitem'},
    '_types': {'BuiltinFunctionType', 'ClassMethodDescriptorType', 'FunctionType', 'GetSetDescriptorType', 'MappingProxyType', 'MemberDescriptorType', 'MethodDescriptorType', 'NoneType'},
    'math': {'copysign', 'isfinite', 'isinf', 'isnan'},
    'zlib': {'compress', 'decompress', 'error'},
}
BUILTIN_TYPES = {
    'bool': 'PyBool',
    'bytearray': 'PyByteArray',
    'bytes': 'PyBytes',
    'dict': 'PyDict',
    'enumerate': 'PyEnumerate',
    'float': 'PyFloat',
    'int': 'PyInt',
    'list': 'PyList',
    'object': 'PyObject',
    'range': 'PyRange',
    'reversed': 'PyReversed',
    'set': 'PySet',
    'slice': 'PySlice',
    'staticmethod': 'PyStaticMethod',
    'str': 'PyString',
    'tuple': 'PyTuple',
    'type': 'PyType',
    'zip': 'PyZip',
}

EXCEPTION_TYPES = {
    'ArithmeticError', 'AssertionError', 'AttributeError', 'BaseException', 'Exception', 'IndexError',
    'KeyError', 'LookupError', 'OverflowError', 'RuntimeError', 'StopIteration', 'TypeError', 'ValueError', 'ZeroDivisionError',
}

NULL = object()

def _make_param(name: str, default: object = inspect.Parameter.empty,
                kind: inspect._ParameterKind = inspect.Parameter.POSITIONAL_ONLY) -> inspect.Parameter:
    return inspect.Parameter(name, kind, default=default)

def _make_poskw_param(name: str, default: object = inspect.Parameter.empty) -> inspect.Parameter:
    return _make_param(name, default, inspect.Parameter.POSITIONAL_OR_KEYWORD)

SYNTHETIC_PARAMS = {
    'builtins': {
        'dir': [_make_param('object', NULL)],
        'getattr': [_make_param('object'), _make_param('name'), _make_param('default', NULL)],
        'iter': [_make_param('iterable'), _make_param('sentinel', NULL)],
        'next': [_make_param('iterator'), _make_param('default', NULL)],
    },
    '_io.TextIOWrapper': {
        'read': [_make_param('size', -1)],
    },
    '_json': {
        'scanstring': [_make_param('string'), _make_param('end')],
    },
    'bytearray': {
        '__newobj__': [_make_param('source', NULL), _make_param('encoding', NULL), _make_param('errors', NULL)],
        'count': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'endswith': [_make_param('suffix'), _make_param('start', None), _make_param('end', None)],
        'find': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'hex': [_make_poskw_param('sep', NULL), _make_poskw_param('bytes_per_sep', 1)],
        'index': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'rfind': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'rindex': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'startswith': [_make_param('prefix'), _make_param('start', None), _make_param('end', None)],
    },
    'bytes': {
        '__newobj__': [_make_param('source', NULL), _make_param('encoding', NULL), _make_param('errors', NULL)],
        'count': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'endswith': [_make_param('suffix'), _make_param('start', None), _make_param('end', None)],
        'find': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'hex': [_make_poskw_param('sep', NULL), _make_poskw_param('bytes_per_sep', 1)],
        'index': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'rfind': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'rindex': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'startswith': [_make_param('prefix'), _make_param('start', None), _make_param('end', None)],
    },
    'dict': {
        '__newobj__': [_make_param('mapping_or_iterable', NULL)],
        'pop': [_make_param('key'), _make_param('defaultValue', NULL)],
    },
    'int': {
        '__newobj__': [_make_param('x', 0), _make_param('base', NULL)],
    },
    'range': {
        '__newobj__': [_make_param('start'), _make_param('stop', NULL), _make_param('step', NULL)],
    },
    'slice': {
        '__newobj__': [_make_param('start'), _make_param('stop', NULL), _make_param('step', NULL)],
    },
    'str': {
        '__newobj__': [_make_poskw_param('object', NULL), _make_poskw_param('encoding', NULL), _make_poskw_param('errors', NULL)],
        'count': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'endswith': [_make_param('suffix'), _make_param('start', None), _make_param('end', None)],
        'find': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'index': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'maketrans': [_make_param('x'), _make_param('y', NULL), _make_param('z', NULL)],
        'rfind': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'rindex': [_make_param('sub'), _make_param('start', None), _make_param('end', None)],
        'startswith': [_make_param('prefix'), _make_param('start', None), _make_param('end', None)],
    },
}

def _get_runtime_obj(name: str) -> object:
    if name.startswith('_io.'):
        return getattr(_io, name.split('.', 1)[1])
    elif name in BUILTIN_MODULES:
        return __import__(name)
    elif name.startswith('_types.'):
        return getattr(types, name.split('.', 1)[1])
    else:
        return getattr(builtins, name)

def _get_signature_params(target: Any, implicit_name: str | None) -> list[inspect.Parameter] | None:
    try:
        params = list(inspect.signature(target).parameters.values())
    except (TypeError, ValueError):
        return None
    if implicit_name is not None:
        if not params or params[0].name != implicit_name or params[0].kind is not inspect.Parameter.POSITIONAL_ONLY:
            return None
        params = params[1:]
    return params

def _is_supported_default(default: object) -> bool:
    if default is inspect.Parameter.empty:
        return True
    if default is NULL:
        return True
    if isinstance(default, (types.NoneType, bool, int, float, str, bytes)):
        return True
    if isinstance(default, tuple):
        return all(_is_supported_default(x) for x in default)
    return False

def _encode_default(default: object) -> dict[str, Any]:
    if default is NULL:
        return {'kind': '__pythonj_null__'}
    if default is None:
        return {'kind': 'none'}
    if default is False or default is True:
        return {'kind': 'bool', 'value': default}
    if type(default) is int:
        return {'kind': 'int', 'value': default}
    if type(default) is float:
        return {'kind': 'float', 'value': default}
    if type(default) is str:
        return {'kind': 'str', 'value': default}
    if type(default) is bytes:
        return {'kind': 'bytes', 'value': default.decode('latin1')}
    if type(default) is tuple:
        return {'kind': 'tuple', 'items': [_encode_default(x) for x in default]}
    assert False, default

def _encode_param_kind(kind: inspect._ParameterKind) -> str:
    if kind is inspect.Parameter.POSITIONAL_ONLY:
        return 'posonly'
    if kind is inspect.Parameter.POSITIONAL_OR_KEYWORD:
        return 'poskw'
    if kind is inspect.Parameter.KEYWORD_ONLY:
        return 'kwonly'
    if kind is inspect.Parameter.VAR_POSITIONAL:
        return 'vararg'
    if kind is inspect.Parameter.VAR_KEYWORD:
        return 'varkw'
    assert False, kind

def _encode_signature(params: list[inspect.Parameter]) -> dict[str, Any] | None:
    out = []
    for param in params:
        if param.kind not in {
            inspect.Parameter.POSITIONAL_ONLY,
            inspect.Parameter.POSITIONAL_OR_KEYWORD,
            inspect.Parameter.KEYWORD_ONLY,
            inspect.Parameter.VAR_POSITIONAL,
            inspect.Parameter.VAR_KEYWORD,
        }:
            return None
        if not _is_supported_default(param.default):
            return None
        encoded: dict[str, Any] = {
            'name': param.name,
            'kind': _encode_param_kind(param.kind),
        }
        if param.default is not inspect.Parameter.empty:
            encoded['default'] = _encode_default(param.default)
        out.append(encoded)
    return {'params': out}

def decode_default(spec: dict[str, Any]) -> object:
    kind = spec['kind']
    if kind == '__pythonj_null__':
        return NULL
    if kind == 'none':
        return None
    if kind in {'bool', 'int', 'float', 'str'}:
        return spec['value']
    if kind == 'bytes':
        return spec['value'].encode('latin1')
    if kind == 'tuple':
        return tuple(decode_default(x) for x in spec['items'])
    assert False, spec

PARAM_KINDS = {
    'posonly': inspect.Parameter.POSITIONAL_ONLY,
    'poskw': inspect.Parameter.POSITIONAL_OR_KEYWORD,
    'kwonly': inspect.Parameter.KEYWORD_ONLY,
    'vararg': inspect.Parameter.VAR_POSITIONAL,
    'varkw': inspect.Parameter.VAR_KEYWORD,
}

def decode_signature(spec: dict[str, Any] | None) -> list[inspect.Parameter] | None:
    if spec is None:
        return None
    params = []
    for raw_param in spec['params']:
        default = inspect.Parameter.empty
        if 'default' in raw_param:
            default = decode_default(raw_param['default'])
        params.append(inspect.Parameter(
            raw_param['name'],
            PARAM_KINDS[raw_param['kind']],
            default=default,
        ))
    return params

def get_method_params(spec: dict[str, Any], name: str, method_name: str) -> list[inspect.Parameter] | None:
    attrs = spec[name]['attrs']
    return decode_signature(attrs[method_name].get('signature'))

def get_builtin_function_params(spec: dict[str, Any], name: str) -> list[inspect.Parameter] | None:
    attrs = spec['builtins']['attrs']
    return decode_signature(attrs[name].get('signature'))

def get_module_function_params(spec: dict[str, Any], module_name: str, func_name: str) -> list[inspect.Parameter] | None:
    attrs = spec[module_name]['attrs']
    return decode_signature(attrs[func_name].get('signature'))

def get_type_attr_kinds(spec: dict[str, Any], name: str) -> dict[str, str]:
    attrs = spec[name]['attrs']
    return {attr_name: attr_spec['kind'] for (attr_name, attr_spec) in attrs.items()}

def get_module_attr_kinds(spec: dict[str, Any], module_name: str) -> dict[str, str]:
    attrs = spec[module_name]['attrs']
    return {attr_name: attr_spec['kind'] for (attr_name, attr_spec) in attrs.items()}

def get_type_attr_kind(spec: dict[str, Any], name: str, attr_name: str) -> str | None:
    attrs = spec[name]['attrs']
    attr = attrs.get(attr_name)
    if attr is None:
        return None
    return attr['kind']

def get_module_attr_kind(spec: dict[str, Any], module_name: str, attr_name: str) -> str | None:
    attrs = spec[module_name]['attrs']
    attr = attrs.get(attr_name)
    if attr is None:
        return None
    return attr['kind']

def get_positional_call_range(params: list[inspect.Parameter] | None) -> tuple[int, int] | None:
    if params is None:
        return None
    if not all(param.kind in {inspect.Parameter.POSITIONAL_ONLY, inspect.Parameter.POSITIONAL_OR_KEYWORD} for param in params):
        return None
    min_args = sum(param.default is inspect.Parameter.empty for param in params)
    return (min_args, len(params))

def get_exact_positional_call_arity(params: list[inspect.Parameter] | None) -> int | None:
    if (call_range := get_positional_call_range(params)) is None:
        return None
    (min_args, max_args) = call_range
    if min_args != max_args:
        return None
    return max_args

def get_posonly_min_max_call_range(params: list[inspect.Parameter] | None) -> tuple[int, int] | None:
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

def get_method_call_range(spec: dict[str, Any], name: str, method_name: str) -> tuple[int, int] | None:
    return get_positional_call_range(get_method_params(spec, name, method_name))

def get_builtin_function_call_range(spec: dict[str, Any], name: str) -> tuple[int, int] | None:
    return get_positional_call_range(get_builtin_function_params(spec, name))

def get_module_function_call_range(spec: dict[str, Any], module_name: str, func_name: str) -> tuple[int, int] | None:
    return get_positional_call_range(get_module_function_params(spec, module_name, func_name))

def get_type_newobj_call_range(spec: dict[str, Any], name: str) -> tuple[int, int] | None:
    type_signature = spec[name].get('signature')
    if type_signature is None:
        return None
    return get_positional_call_range(decode_signature(type_signature))

def get_type_attrs(spec: dict[str, Any], name: str) -> dict[str, dict[str, Any]]:
    return spec[name]['attrs']

def iter_type_attrs(spec: dict[str, Any], name: str) -> list[tuple[str, dict[str, Any]]]:
    return list(get_type_attrs(spec, name).items())

def iter_type_methods(spec: dict[str, Any], name: str) -> list[tuple[str, str, list[inspect.Parameter] | None]]:
    attrs = get_type_attrs(spec, name)
    out = []
    for (attr_name, attr_spec) in attrs.items():
        kind = attr_spec['kind']
        if kind in {'method', 'classmethod', 'staticmethod'}:
            out.append((attr_name, kind, decode_signature(attr_spec.get('signature'))))
    return out

def get_module_attrs(spec: dict[str, Any], module_name: str) -> dict[str, dict[str, Any]]:
    return spec[module_name]['attrs']

def iter_module_functions(spec: dict[str, Any], module_name: str) -> list[tuple[str, list[inspect.Parameter] | None]]:
    attrs = get_module_attrs(spec, module_name)
    out = []
    for (attr_name, attr_spec) in attrs.items():
        if attr_spec['kind'] == 'builtin_function':
            out.append((attr_name, decode_signature(attr_spec.get('signature'))))
    return out

def _get_method_signature(name: str, method_name: str) -> dict[str, Any] | None:
    if (synthetic := SYNTHETIC_PARAMS.get(name, {}).get(method_name)) is not None:
        return _encode_signature(synthetic)
    obj = _get_runtime_obj(name)
    desc = obj.__dict__.get(method_name)
    if desc is None:
        return None
    desc_type = type(desc)
    if desc_type is types.MethodDescriptorType:
        params = _get_signature_params(desc, 'self')
    elif desc_type is types.ClassMethodDescriptorType:
        params = _get_signature_params(desc, 'type')
    elif desc_type is staticmethod:
        params = _get_signature_params(getattr(obj, method_name), None)
    else:
        params = None
    if params is None:
        return None
    return _encode_signature(params)

def _get_type_signature(name: str) -> dict[str, Any] | None:
    if (synthetic := SYNTHETIC_PARAMS.get(name, {}).get('__newobj__')) is not None:
        return _encode_signature(synthetic)
    obj = _get_runtime_obj(name)
    params = _get_signature_params(obj, None)
    if params is None:
        return None
    return _encode_signature(params)

def _get_builtin_function_signature(name: str) -> dict[str, Any] | None:
    if (synthetic := SYNTHETIC_PARAMS['builtins'].get(name)) is not None:
        return _encode_signature(synthetic)
    desc = builtins.__dict__.get(name)
    if type(desc) is not types.BuiltinFunctionType:
        return None
    params = _get_signature_params(desc, None)
    if params is None:
        return None
    return _encode_signature(params)

def _get_module_function_signature(module_name: str, func_name: str) -> dict[str, Any] | None:
    if (synthetic := SYNTHETIC_PARAMS.get(module_name, {}).get(func_name)) is not None:
        return _encode_signature(synthetic)
    module = _get_runtime_obj(module_name)
    func = getattr(module, func_name)
    params = _get_signature_params(func, None)
    if params is None:
        return None
    return _encode_signature(params)

def _encode_attr(kind: str, doc: str | None = None, signature: dict[str, Any] | None = None,
                 value: object | None = None, target: str | None = None) -> dict[str, Any]:
    out: dict[str, Any] = {'kind': kind}
    if doc is not None:
        out['doc'] = doc
    if signature is not None:
        out['signature'] = signature
    if value is not None:
        out['value'] = value
    if target is not None:
        out['target'] = target
    return out

def _build_type_entry(name: str) -> dict[str, Any]:
    obj = _get_runtime_obj(name)
    attrs = {}
    for (k, v) in list(obj.__dict__.items()):
        if k.startswith('__') and k not in {'__doc__'}:
            continue
        v_type = type(v)
        if v_type is str:
            attrs[k] = _encode_attr('string', value=v)
        elif v_type is types.MemberDescriptorType:
            attrs[k] = _encode_attr('member', doc=v.__doc__)
        elif v_type is types.GetSetDescriptorType:
            attrs[k] = _encode_attr('getset', doc=v.__doc__)
        elif v_type is types.MethodDescriptorType:
            attrs[k] = _encode_attr('method', doc=v.__doc__, signature=_get_method_signature(name, k))
        elif v_type is types.ClassMethodDescriptorType:
            attrs[k] = _encode_attr('classmethod', doc=v.__doc__, signature=_get_method_signature(name, k))
        elif v_type is staticmethod:
            attrs[k] = _encode_attr('staticmethod', signature=_get_method_signature(name, k))
        else:
            assert False, (name, k, v, v_type)
    return {'kind': 'type', 'signature': _get_type_signature(name), 'attrs': attrs}

def _build_builtin_module_entry() -> dict[str, Any]:
    attrs = {}
    for name in sorted(BUILTIN_FUNCTIONS):
        desc = builtins.__dict__.get(name)
        if type(desc) is types.BuiltinFunctionType:
            attrs[name] = _encode_attr('builtin_function', doc=desc.__doc__, signature=_get_builtin_function_signature(name))
    return {'kind': 'module', 'attrs': attrs}

def _build_module_entry(name: str) -> dict[str, Any]:
    obj = _get_runtime_obj(name)
    attrs = {}
    for (k, v) in list(obj.__dict__.items()):
        if k not in BUILTIN_MODULE_ATTRS[name]:
            continue
        if k.startswith('__') and k not in {'__doc__'}:
            continue
        if type(v) is types.BuiltinFunctionType:
            attrs[k] = _encode_attr('builtin_function', doc=v.__doc__, signature=_get_module_function_signature(name, k))
        elif isinstance(v, type):
            attrs[k] = _encode_attr('type', target=v.__name__)
    return {'kind': 'module', 'attrs': attrs}

def _is_scalar_json_value(value: object) -> bool:
    return isinstance(value, (types.NoneType, bool, int, float, str))

def _is_leaf_json_value(value: object) -> bool:
    if _is_scalar_json_value(value):
        return True
    if isinstance(value, list):
        return all(_is_scalar_json_value(x) for x in value)
    if isinstance(value, dict):
        return all(isinstance(k, str) and _is_scalar_json_value(v) for (k, v) in value.items())
    return False

def _write_pretty_json(f, value: object, indent: int = 0) -> None:
    if _is_leaf_json_value(value):
        f.write(json.dumps(value, ensure_ascii=True, separators=(', ', ': ')))
        return
    if isinstance(value, list):
        f.write('[\n')
        for (i, item) in enumerate(value):
            f.write('  ' * (indent + 1))
            _write_pretty_json(f, item, indent + 1)
            if i + 1 != len(value):
                f.write(',')
            f.write('\n')
        f.write('  ' * indent)
        f.write(']')
        return
    if isinstance(value, dict):
        f.write('{\n')
        items = list(value.items())
        for (i, (k, v)) in enumerate(items):
            f.write('  ' * (indent + 1))
            f.write(json.dumps(k, ensure_ascii=True))
            f.write(': ')
            _write_pretty_json(f, v, indent + 1)
            if i + 1 != len(items):
                f.write(',')
            f.write('\n')
        f.write('  ' * indent)
        f.write('}')
        return
    assert False, value

def gen_spec(spec_path: str) -> None:
    spec = {'builtins': _build_builtin_module_entry()}
    for name in [*BUILTIN_TYPES, *sorted(EXCEPTION_TYPES),
                 '_types.BuiltinFunctionType', '_types.ClassMethodDescriptorType',
                 '_types.FunctionType', '_types.GetSetDescriptorType', '_types.MappingProxyType', '_types.MemberDescriptorType',
                 '_types.MethodDescriptorType', '_types.NoneType', '_io.BufferedReader', '_io.TextIOWrapper']:
        spec[name] = _build_type_entry(name)
    for name in sorted(BUILTIN_MODULES):
        spec[name] = _build_module_entry(name)

    with open(spec_path, 'w', encoding='utf-8') as f:
        _write_pretty_json(f, spec)
        f.write('\n')

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('spec_path', nargs='?', default='_out/spec.json')
    args = parser.parse_args()

    spec_dir = os.path.dirname(args.spec_path)
    if spec_dir:
        os.makedirs(spec_dir, exist_ok=True)
    gen_spec(args.spec_path)

if __name__ == '__main__':
    main()
