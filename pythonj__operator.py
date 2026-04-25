# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

def contains(a, b) -> bool:
    return b in a

def delitem(a, b) -> None:
    del a[b]
    return None

def getitem(a, b):
    return a[b]

def index(a) -> int:
    index_func = __pythonj_lookup_attr__(type(a), '__index__')
    if index_func is __pythonj_null__:
        raise TypeError(f'{type(a).__name__!r} object cannot be interpreted as an integer')
    ret = index_func(a)
    if not isinstance(ret, int):
        raise TypeError(f'__index__ returned non-int (type {type(ret).__name__})')
    return ret

def setitem(a, b, c) -> None:
    a[b] = c
    return None
