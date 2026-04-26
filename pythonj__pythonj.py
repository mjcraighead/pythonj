# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

import sys

def parse_args(specs: list, argv) -> dict:
    if argv is __pythonj_null__ or argv is None:
        argv = sys.argv[1:]

    positionals = []
    options = {}
    result = {}
    required_options = set()

    for spec in specs:
        spec_len = len(spec)
        if spec_len != 2 and spec_len != 3:
            raise SystemExit('argument error: argument spec must have 2 or 3 items')
        name = spec[0]
        kind = spec[1]
        if not isinstance(name, str):
            raise SystemExit('argument error: argument name must be str')
        if not isinstance(kind, str):
            raise SystemExit('argument error: argument kind must be str')
        required = spec_len == 2
        default = None if required else spec[2]

        dest = name
        if dest.startswith('--'):
            dest = dest[2:]
        while '-' in dest:
            dash_index = dest.find('-')
            dest = dest[:dash_index] + '_' + dest[dash_index + 1:]

        if name.startswith('--') and len(name) > 2:
            if name in options:
                raise SystemExit('argument error: duplicate option ' + name)
            options[name] = (dest, kind)
            if required:
                required_options.add(dest)
            elif kind == 'int-list':
                result[dest] = []
            elif kind == 'bool':
                result[dest] = bool(default)
            else:
                result[dest] = default
        else:
            if not required:
                raise SystemExit('argument error: positional arguments cannot have defaults')
            positionals.append((dest, kind))

    positional_index = 0
    i = 0
    n = len(argv)
    while i < n:
        arg = argv[i]
        if arg == '--':
            i += 1
            break
        if arg.startswith('--') and len(arg) > 2:
            name = arg
            value = None
            equals_index = arg.find('=')
            if equals_index >= 0:
                name = arg[:equals_index]
                value = arg[equals_index + 1:]
            if name not in options:
                raise SystemExit('argument error: unrecognized option ' + name)
            dest, kind = options[name]
            if kind == 'bool':
                if value is not None:
                    raise SystemExit('argument error: ' + name + ' does not take a value')
                result[dest] = True
            else:
                if value is None:
                    i += 1
                    if i >= n:
                        raise SystemExit('argument error: ' + name + ' expected one argument')
                    value = argv[i]
                if kind == 'str':
                    parsed = value
                elif kind == 'int' or kind == 'int-list':
                    parsed = int(value, 0)
                else:
                    raise SystemExit('argument error: unsupported argument kind ' + repr(kind))
                if kind == 'int-list':
                    result[dest].append(parsed)
                else:
                    result[dest] = parsed
            if dest in required_options:
                required_options.remove(dest)
        else:
            if positional_index >= len(positionals):
                raise SystemExit('argument error: too many positional arguments')
            dest, kind = positionals[positional_index]
            if kind == 'str':
                result[dest] = arg
            elif kind == 'int':
                result[dest] = int(arg, 0)
            else:
                raise SystemExit('argument error: unsupported positional argument kind ' + repr(kind))
            positional_index += 1
        i += 1

    while i < n:
        if positional_index >= len(positionals):
            raise SystemExit('argument error: too many positional arguments')
        dest, kind = positionals[positional_index]
        if kind == 'str':
            result[dest] = argv[i]
        elif kind == 'int':
            result[dest] = int(argv[i], 0)
        else:
            raise SystemExit('argument error: unsupported positional argument kind ' + repr(kind))
        positional_index += 1
        i += 1

    if positional_index < len(positionals):
        raise SystemExit('argument error: missing required positional argument ' + positionals[positional_index][0])
    for dest in required_options:
        raise SystemExit('argument error: missing required option --' + dest)

    return result
