# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

def abs(arg):
    return __pythonj_abs__(arg)

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

def delattr(obj, name):
    if not __pythonj_isinstance_single__(name, str):
        raise TypeError('attribute name must be string, not ' + repr(type(name).__name__))
    return __pythonj_delattr__(obj, name)

def format(value, format_spec):
    if not __pythonj_isinstance_single__(format_spec, str):
        raise TypeError('format() argument 2 must be str, not ' + type(format_spec).__name__)
    return __pythonj_format__(value, format_spec)

def getattr(obj, name, default):
    if not __pythonj_isinstance_single__(name, str):
        raise TypeError('attribute name must be string, not ' + repr(type(name).__name__))
    try:
        return __pythonj_getattr__(obj, name)
    except AttributeError as e:
        if default is not __pythonj_null__:
            return default
        raise e

def hash(arg):
    return __pythonj_hash__(arg)

def hasattr(obj, name):
    try:
        getattr(obj, name)
    except AttributeError:
        return False
    return True

def isinstance(obj, class_or_tuple):
    if __pythonj_isinstance_single__(class_or_tuple, tuple):
        for x in class_or_tuple:
            if isinstance(obj, x):
                return True
        return False
    return __pythonj_isinstance_single__(obj, class_or_tuple)

def issubclass(obj, class_or_tuple):
    if __pythonj_isinstance_single__(class_or_tuple, tuple):
        for x in class_or_tuple:
            if issubclass(obj, x):
                return True
        return False
    return __pythonj_issubclass_single__(obj, class_or_tuple)

def len(arg):
    return __pythonj_len__(arg)

def next(iterator, default):
    ret = __pythonj_next__(iterator)
    if ret is __pythonj_null__:
        if default is not __pythonj_null__:
            return default
        raise StopIteration()
    return ret

def repr(arg):
    return __pythonj_repr__(arg)

def setattr(obj, name, value):
    if not __pythonj_isinstance_single__(name, str):
        raise TypeError('attribute name must be string, not ' + repr(type(name).__name__))
    return __pythonj_setattr__(obj, name, value)

def sum(iterable, start):
    if __pythonj_isinstance_single__(start, str):
        raise TypeError("sum() can't sum strings [use ''.join(seq) instead]")
    if __pythonj_isinstance_single__(start, bytes):
        raise TypeError("sum() can't sum bytes [use b''.join(seq) instead]")
    if __pythonj_isinstance_single__(start, bytearray):
        raise TypeError("sum() can't sum bytearray [use b''.join(seq) instead]")
    for item in iterable:
        start = start + item
    return start
