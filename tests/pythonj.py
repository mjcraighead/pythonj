# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

import _pythonj

def expect_system_exit(fn, msg):
    try:
        fn()
    except SystemExit as e:
        assert str(e) == msg
        return None
    assert False

parse_spec = [
    ('input_file', 'str'),
    ('output_file', 'str'),
    ('--context', 'int', 3),
    ('--skip-line', 'int-list', []),
    ('--mode', 'str'),
    ('--verbose', 'bool', False),
]

print('basic parse_args:')
args = _pythonj.parse_args(parse_spec, ['--mode', 'fast', 'input.txt', 'output.txt'])
assert args == {
    'input_file': 'input.txt',
    'output_file': 'output.txt',
    'context': 3,
    'skip_line': [],
    'mode': 'fast',
    'verbose': False,
}
print(args)

print('mixed options:')
args = _pythonj.parse_args(parse_spec, ['input.txt', '--context=0x10', '--skip-line', '5', '--skip-line=0x20', '--verbose', '--mode=check', 'output.txt'])
assert args == {
    'input_file': 'input.txt',
    'output_file': 'output.txt',
    'context': 16,
    'skip_line': [5, 32],
    'mode': 'check',
    'verbose': True,
}
print(args)

print('literal argument after --:')
assert _pythonj.parse_args([('input', 'str')], ['--', '--not-option']) == {'input': '--not-option'}
print(_pythonj.parse_args([('input', 'str')], ['--', '--not-option']))

expect_system_exit(lambda: _pythonj.parse_args(parse_spec, []), 'argument error: missing required positional argument input_file')
expect_system_exit(lambda: _pythonj.parse_args(parse_spec, ['a']), 'argument error: missing required positional argument output_file')
expect_system_exit(lambda: _pythonj.parse_args(parse_spec, ['a', 'b', 'c']), 'argument error: too many positional arguments')
expect_system_exit(lambda: _pythonj.parse_args(parse_spec, ['a', 'b', '--missing']), 'argument error: unrecognized option --missing')
expect_system_exit(lambda: _pythonj.parse_args(parse_spec, ['a', 'b', '--verbose=1']), 'argument error: --verbose does not take a value')
expect_system_exit(lambda: _pythonj.parse_args(parse_spec, ['a', 'b', '--mode']), 'argument error: --mode expected one argument')

print('ok')
