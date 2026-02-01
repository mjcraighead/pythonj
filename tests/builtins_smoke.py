# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

for x in [abs, int, list, dict, print, range]:
    print(x)
    print(type(x))

print(int(10))
print(list(range(5)))
print(dict([(1, 2), (3, 4)]))
print(repr(str(123)))
print(bool(0), bool(1))
print(type(123))
