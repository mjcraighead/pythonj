# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

def all(iterable):
    for item in iterable:
        if not item:
            return False
    return True

def any(iterable):
    for item in iterable:
        if item:
            return True
    return False

def hasattr(obj, name):
    try:
        getattr(obj, name)
    except AttributeError:
        return False
    return True

def next(iterator, default):
    ret = __pythonj_next__(iterator)
    if ret is __pythonj_null__:
        if default is not __pythonj_null__:
            return default
        raise StopIteration()
    return ret
