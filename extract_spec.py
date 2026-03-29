# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

import argparse
import builtins
import json
import os
import types
import _io

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
    'KeyError', 'LookupError', 'OverflowError', 'StopIteration', 'TypeError', 'ValueError', 'ZeroDivisionError',
}

def get_runtime_obj(name: str) -> object:
    if name.startswith('_io.'):
        return getattr(_io, name.split('.', 1)[1])
    elif name.startswith('types.'):
        return getattr(types, name.split('.', 1)[1])
    else:
        return getattr(builtins, name)

def gen_spec(spec_path: str) -> None:
    spec = {}
    for name in [*BUILTIN_TYPES, *sorted(EXCEPTION_TYPES),
                 'types.BuiltinFunctionType', 'types.ClassMethodDescriptorType',
                 'types.FunctionType', 'types.GetSetDescriptorType', 'types.MemberDescriptorType',
                 'types.MethodDescriptorType', 'types.NoneType', '_io.BufferedReader', '_io.TextIOWrapper']:
        obj = get_runtime_obj(name)
        attrs = {}
        for (k, v) in obj.__dict__.items():
            if k.startswith('__') and k not in {'__doc__'}:
                continue
            v_type = type(v)
            if v_type is str:
                attrs[k] = {'kind': 'string', 'value': v}
            elif v_type is types.MemberDescriptorType:
                attrs[k] = {'kind': 'member', 'doc': v.__doc__}
            elif v_type is types.GetSetDescriptorType:
                attrs[k] = {'kind': 'getset', 'doc': v.__doc__}
            elif v_type is types.MethodDescriptorType:
                attrs[k] = {'kind': 'method', 'doc': v.__doc__}
            elif v_type is types.ClassMethodDescriptorType:
                attrs[k] = {'kind': 'classmethod', 'doc': v.__doc__}
            elif v_type is staticmethod:
                attrs[k] = {'kind': 'staticmethod'}
            else:
                assert False, (name, k, v, v_type)
        spec[name] = attrs

    with open(spec_path, 'w') as f:
        json.dump(spec, f, indent=2)
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
