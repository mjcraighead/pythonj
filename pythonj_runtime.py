# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

def max_iterable(iterable, default_obj, key_func):
    it = __pythonj_iter__(iterable)
    ret = __pythonj_next__(it)
    if ret is __pythonj_null__:
        if default_obj is not __pythonj_null__:
            return default_obj
        raise ValueError('max() iterable argument is empty')
    if key_func is None:
        for item in it:
            if item > ret:
                ret = item
    else:
        ret_key = key_func(ret)
        for item in it:
            item_key = key_func(item)
            if item_key > ret_key:
                ret = item
                ret_key = item_key
    return ret

def min_iterable(iterable, default_obj, key_func):
    it = __pythonj_iter__(iterable)
    ret = __pythonj_next__(it)
    if ret is __pythonj_null__:
        if default_obj is not __pythonj_null__:
            return default_obj
        raise ValueError('min() iterable argument is empty')
    if key_func is None:
        for item in it:
            if item < ret:
                ret = item
    else:
        ret_key = key_func(ret)
        for item in it:
            item_key = key_func(item)
            if item_key < ret_key:
                ret = item
                ret_key = item_key
    return ret
