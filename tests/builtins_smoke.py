# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

print('builtin objects:')
for x in [abs, bool, dict, int, list, print, range, str]:
    print(x, type(x).__name__)
print()

print('numbers:', abs(-5), int('42'), float('1.5'), divmod(17, 5))
print('truth:', bool(0), bool(1), bool([]), bool([1]))
print('types:', type(123).__name__, type('abc').__name__, type([]).__name__)
print('format:', format(255, '#x'), format(1234, ',d'), repr(ascii('caf\xe9')))
print()

print('range/list:', list(range(5)))
print('tuple:', tuple(range(3)))
print('dict:', dict([(1, 2), (3, 4)]))
print('set:', sorted(set([3, 1, 3, 2])))
print('slice:', [10, 20, 30, 40][1:3])
print()

print('str:', str(123), 'hello'.upper(), 'banana'.replace('na', 'NA', 1))
print('split/join:', 'a,b,c'.split(','), '-'.join(['a', 'b', 'c']))
print('bytes:', bytes([65, 66, 67]), b'hello'.upper())
print('bytearray:', bytearray(b'abc'), len(bytearray(b'abc')))
print()

print('enumerate:', list(enumerate(['a', 'b'])))
print('zip:', list(zip([1, 2], ['a', 'b'])))
print('sorted:', sorted(['aaa', 'b', 'cc'], key=len))
print('sum/min/max:', sum([1, 2, 3]), min([3, 1, 2]), max([3, 1, 2]))
print('any/all:', any([False, True]), all([True, True]))
