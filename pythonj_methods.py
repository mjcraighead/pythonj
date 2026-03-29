# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

def dict__setdefault(self, key, defaultValue):
    value = __pythonj_dict_get__(self, key)
    if value is not __pythonj_null__:
        return value
    self[key] = defaultValue
    return defaultValue

def float__conjugate(self):
    return self

def int__as_integer_ratio(self):
    return (self, 1)

def int__conjugate(self):
    return self

def int__is_integer(self):
    return True

def str__removeprefix(self, prefix):
    if not __pythonj_isinstance_single__(prefix, str):
        raise TypeError("removeprefix() argument must be str, not " + type(prefix).__name__)
    if self.startswith(prefix):
        return self[len(prefix):]
    return self

def str__removesuffix(self, suffix):
    if not __pythonj_isinstance_single__(suffix, str):
        raise TypeError("removesuffix() argument must be str, not " + type(suffix).__name__)
    if suffix and (len(suffix) <= len(self)) and (self[-len(suffix):] == suffix):
        return self[:-len(suffix)]
    return self
