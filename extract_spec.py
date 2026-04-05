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
import _io

BUILTIN_FUNCTIONS = {
    'abs', 'all', 'any', 'ascii', 'bin', 'chr', 'delattr', 'dir', 'format', 'getattr', 'hasattr', 'hash', 'hex',
    'isinstance', 'issubclass', 'iter', 'len', 'max', 'min', 'next', 'open', 'ord', 'print', 'repr',
    'setattr', 'sorted', 'sum', 'oct',
}
BUILTIN_MODULES = {
    '_json': 'PyJsonModule',
    'math': 'PyMathModule',
    'operator': 'PyOperatorModule',
    'types': 'PyTypesModule',
    'zlib': 'PyZlibModule',
}
BUILTIN_MODULE_ATTRS = {
    '_json': {'encode_basestring_ascii', 'scanstring'},
    'math': {'copysign', 'isfinite', 'isinf', 'isnan'},
    'operator': {'contains', 'delitem', 'getitem', 'index', 'setitem'},
    'types': {'BuiltinFunctionType', 'ClassMethodDescriptorType', 'FunctionType', 'GetSetDescriptorType', 'MappingProxyType', 'MemberDescriptorType', 'MethodDescriptorType', 'NoneType'},
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

def make_param(name: str, default: object = inspect.Parameter.empty,
               kind: inspect._ParameterKind = inspect.Parameter.POSITIONAL_ONLY) -> inspect.Parameter:
    return inspect.Parameter(name, kind, default=default)

def make_poskw_param(name: str, default: object = inspect.Parameter.empty) -> inspect.Parameter:
    return make_param(name, default, inspect.Parameter.POSITIONAL_OR_KEYWORD)

SYNTHETIC_PARAMS = {
    'builtins': {
        'dir': [make_param('object', NULL)],
        'getattr': [make_param('object'), make_param('name'), make_param('default', NULL)],
        'iter': [make_param('iterable'), make_param('sentinel', NULL)],
        'next': [make_param('iterator'), make_param('default', NULL)],
    },
    '_io.TextIOWrapper': {
        'read': [make_param('size', -1)],
    },
    '_json': {
        'scanstring': [make_param('string'), make_param('end')],
    },
    'bytearray': {
        '__newobj__': [make_param('source', NULL), make_param('encoding', NULL), make_param('errors', NULL)],
        'count': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'endswith': [make_param('suffix'), make_param('start', None), make_param('end', None)],
        'find': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'hex': [make_poskw_param('sep', NULL), make_poskw_param('bytes_per_sep', 1)],
        'index': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'rfind': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'rindex': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'startswith': [make_param('prefix'), make_param('start', None), make_param('end', None)],
    },
    'bytes': {
        '__newobj__': [make_param('source', NULL), make_param('encoding', NULL), make_param('errors', NULL)],
        'count': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'endswith': [make_param('suffix'), make_param('start', None), make_param('end', None)],
        'find': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'hex': [make_poskw_param('sep', NULL), make_poskw_param('bytes_per_sep', 1)],
        'index': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'rfind': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'rindex': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'startswith': [make_param('prefix'), make_param('start', None), make_param('end', None)],
    },
    'dict': {
        '__newobj__': [make_param('mapping_or_iterable', NULL)],
        'pop': [make_param('key'), make_param('defaultValue', NULL)],
    },
    'int': {
        '__newobj__': [make_param('x', 0), make_param('base', NULL)],
    },
    'range': {
        '__newobj__': [make_param('start'), make_param('stop', NULL), make_param('step', NULL)],
    },
    'slice': {
        '__newobj__': [make_param('start'), make_param('stop', NULL), make_param('step', NULL)],
    },
    'str': {
        '__newobj__': [make_poskw_param('object', NULL), make_poskw_param('encoding', NULL), make_poskw_param('errors', NULL)],
        'count': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'endswith': [make_param('suffix'), make_param('start', None), make_param('end', None)],
        'find': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'index': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'maketrans': [make_param('x'), make_param('y', NULL), make_param('z', NULL)],
        'rfind': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'rindex': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'startswith': [make_param('prefix'), make_param('start', None), make_param('end', None)],
    },
}

def get_runtime_obj(name: str) -> object:
    if name.startswith('_io.'):
        return getattr(_io, name.split('.', 1)[1])
    elif name in BUILTIN_MODULES:
        return __import__(name)
    elif name.startswith('types.'):
        return getattr(types, name.split('.', 1)[1])
    else:
        return getattr(builtins, name)

def get_signature_params(target: object, implicit_name: str | None) -> list[inspect.Parameter] | None:
    try:
        params = list(inspect.signature(target).parameters.values())
    except (TypeError, ValueError):
        return None
    if implicit_name is not None:
        if not params or params[0].name != implicit_name or params[0].kind is not inspect.Parameter.POSITIONAL_ONLY:
            return None
        params = params[1:]
    return params

def is_supported_default(default: object) -> bool:
    if default is inspect.Parameter.empty:
        return True
    if default is NULL:
        return True
    if isinstance(default, (types.NoneType, bool, int, float, str, bytes)):
        return True
    if isinstance(default, tuple):
        return all(is_supported_default(x) for x in default)
    return False

def encode_default(default: object) -> dict[str, object]:
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
        return {'kind': 'tuple', 'items': [encode_default(x) for x in default]}
    assert False, default

def encode_param_kind(kind: inspect._ParameterKind) -> str:
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

def encode_signature(params: list[inspect.Parameter]) -> dict[str, object] | None:
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
        if not is_supported_default(param.default):
            return None
        encoded = {
            'name': param.name,
            'kind': encode_param_kind(param.kind),
        }
        if param.default is not inspect.Parameter.empty:
            encoded['default'] = encode_default(param.default)
        out.append(encoded)
    return {'params': out}

def decode_default(spec: dict[str, object]) -> object:
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

def decode_signature(spec: dict[str, object] | None) -> list[inspect.Parameter] | None:
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

def get_method_params(spec: dict[str, object], name: str, method_name: str) -> list[inspect.Parameter] | None:
    attrs = spec[name]['attrs']
    return decode_signature(attrs[method_name].get('signature'))

def get_builtin_function_params(spec: dict[str, object], name: str) -> list[inspect.Parameter] | None:
    attrs = spec['builtins']['attrs']
    return decode_signature(attrs[name].get('signature'))

def get_module_function_params(spec: dict[str, object], module_name: str, func_name: str) -> list[inspect.Parameter] | None:
    attrs = spec[module_name]['attrs']
    return decode_signature(attrs[func_name].get('signature'))

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

def get_method_signature(name: str, method_name: str) -> dict[str, object] | None:
    if (synthetic := SYNTHETIC_PARAMS.get(name, {}).get(method_name)) is not None:
        return encode_signature(synthetic)
    obj = get_runtime_obj(name)
    desc = obj.__dict__.get(method_name)
    if desc is None:
        return None
    desc_type = type(desc)
    if desc_type is types.MethodDescriptorType:
        params = get_signature_params(desc, 'self')
    elif desc_type is types.ClassMethodDescriptorType:
        params = get_signature_params(desc, 'type')
    elif desc_type is staticmethod:
        params = get_signature_params(getattr(obj, method_name), None)
    else:
        params = None
    if params is None:
        return None
    return encode_signature(params)

def get_type_signature(name: str) -> dict[str, object] | None:
    if (synthetic := SYNTHETIC_PARAMS.get(name, {}).get('__newobj__')) is not None:
        return encode_signature(synthetic)
    obj = get_runtime_obj(name)
    params = get_signature_params(obj, None)
    if params is None:
        return None
    return encode_signature(params)

def get_builtin_function_signature(name: str) -> dict[str, object] | None:
    if (synthetic := SYNTHETIC_PARAMS['builtins'].get(name)) is not None:
        return encode_signature(synthetic)
    desc = builtins.__dict__.get(name)
    if type(desc) is not types.BuiltinFunctionType:
        return None
    params = get_signature_params(desc, None)
    if params is None:
        return None
    return encode_signature(params)

def get_module_function_signature(module_name: str, func_name: str) -> dict[str, object] | None:
    if (synthetic := SYNTHETIC_PARAMS.get(module_name, {}).get(func_name)) is not None:
        return encode_signature(synthetic)
    module = get_runtime_obj(module_name)
    func = getattr(module, func_name)
    params = get_signature_params(func, None)
    if params is None:
        return None
    return encode_signature(params)

def encode_attr(kind: str, doc: str | None = None, signature: dict[str, object] | None = None,
                value: object | None = None, target: str | None = None) -> dict[str, object]:
    out = {'kind': kind}
    if doc is not None:
        out['doc'] = doc
    if signature is not None:
        out['signature'] = signature
    if value is not None:
        out['value'] = value
    if target is not None:
        out['target'] = target
    return out

def build_type_entry(name: str) -> dict[str, object]:
    obj = get_runtime_obj(name)
    attrs = {}
    for (k, v) in list(obj.__dict__.items()):
        if k.startswith('__') and k not in {'__doc__'}:
            continue
        v_type = type(v)
        if v_type is str:
            attrs[k] = encode_attr('string', value=v)
        elif v_type is types.MemberDescriptorType:
            attrs[k] = encode_attr('member', doc=v.__doc__)
        elif v_type is types.GetSetDescriptorType:
            attrs[k] = encode_attr('getset', doc=v.__doc__)
        elif v_type is types.MethodDescriptorType:
            attrs[k] = encode_attr('method', doc=v.__doc__, signature=get_method_signature(name, k))
        elif v_type is types.ClassMethodDescriptorType:
            attrs[k] = encode_attr('classmethod', doc=v.__doc__, signature=get_method_signature(name, k))
        elif v_type is staticmethod:
            attrs[k] = encode_attr('staticmethod', signature=get_method_signature(name, k))
        else:
            assert False, (name, k, v, v_type)
    return {'kind': 'type', 'signature': get_type_signature(name), 'attrs': attrs}

def build_builtin_module_entry() -> dict[str, object]:
    attrs = {}
    for name in sorted(BUILTIN_FUNCTIONS):
        desc = builtins.__dict__.get(name)
        if type(desc) is types.BuiltinFunctionType:
            attrs[name] = encode_attr('builtin_function', doc=desc.__doc__, signature=get_builtin_function_signature(name))
    return {'kind': 'module', 'attrs': attrs}

def build_module_entry(name: str) -> dict[str, object]:
    obj = get_runtime_obj(name)
    attrs = {}
    for (k, v) in list(obj.__dict__.items()):
        if k not in BUILTIN_MODULE_ATTRS[name]:
            continue
        if k.startswith('__') and k not in {'__doc__'}:
            continue
        if type(v) is types.BuiltinFunctionType:
            attrs[k] = encode_attr('builtin_function', doc=v.__doc__, signature=get_module_function_signature(name, k))
        elif isinstance(v, type):
            attrs[k] = encode_attr('type', target=v.__name__)
    return {'kind': 'module', 'attrs': attrs}

def is_scalar_json_value(value: object) -> bool:
    return isinstance(value, (types.NoneType, bool, int, float, str))

def is_leaf_json_value(value: object) -> bool:
    if is_scalar_json_value(value):
        return True
    if isinstance(value, list):
        return all(is_scalar_json_value(x) for x in value)
    if isinstance(value, dict):
        return all(isinstance(k, str) and is_scalar_json_value(v) for (k, v) in value.items())
    return False

def write_pretty_json(f, value: object, indent: int = 0) -> None:
    if is_leaf_json_value(value):
        f.write(json.dumps(value, ensure_ascii=True, separators=(', ', ': ')))
        return
    if isinstance(value, list):
        f.write('[\n')
        for (i, item) in enumerate(value):
            f.write('  ' * (indent + 1))
            write_pretty_json(f, item, indent + 1)
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
            write_pretty_json(f, v, indent + 1)
            if i + 1 != len(items):
                f.write(',')
            f.write('\n')
        f.write('  ' * indent)
        f.write('}')
        return
    assert False, value

def gen_spec(spec_path: str) -> None:
    spec = {'builtins': build_builtin_module_entry()}
    for name in [*BUILTIN_TYPES, *sorted(EXCEPTION_TYPES),
                 'types.BuiltinFunctionType', 'types.ClassMethodDescriptorType',
                 'types.FunctionType', 'types.GetSetDescriptorType', 'types.MappingProxyType', 'types.MemberDescriptorType',
                 'types.MethodDescriptorType', 'types.NoneType', '_io.BufferedReader', '_io.TextIOWrapper']:
        spec[name] = build_type_entry(name)
    for name in sorted(BUILTIN_MODULES):
        spec[name] = build_module_entry(name)

    with open(spec_path, 'w', encoding='utf-8') as f:
        write_pretty_json(f, spec)
        f.write('\n')

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('spec_path', nargs='?', default='runtime/_out/spec.json')
    args = parser.parse_args()

    spec_dir = os.path.dirname(args.spec_path)
    if spec_dir:
        os.makedirs(spec_dir, exist_ok=True)
    gen_spec(args.spec_path)

if __name__ == '__main__':
    main()
