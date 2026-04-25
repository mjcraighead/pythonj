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

def imatmul(a, b):
    lhs_type = type(a)
    imatmul_func = __pythonj_lookup_attr__(lhs_type, '__imatmul__')
    if imatmul_func is not __pythonj_null__:
        ret = imatmul_func(a, b)
        if ret is not NotImplemented:
            return ret

    rhs_type = type(b)
    matmul_func = __pythonj_lookup_attr__(lhs_type, '__matmul__')
    rmatmul_func = __pythonj_lookup_attr__(rhs_type, '__rmatmul__')
    lhs_rmatmul_func = __pythonj_lookup_attr__(lhs_type, '__rmatmul__')

    reflected_first = rhs_type is not lhs_type and issubclass(rhs_type, lhs_type) and rmatmul_func is not __pythonj_null__ and rmatmul_func is not lhs_rmatmul_func
    if reflected_first:
        ret = rmatmul_func(b, a)
        if ret is not NotImplemented:
            return ret

    if matmul_func is not __pythonj_null__:
        ret = matmul_func(a, b)
        if ret is not NotImplemented:
            return ret

    if not reflected_first and rhs_type is not lhs_type and rmatmul_func is not __pythonj_null__:
        ret = rmatmul_func(b, a)
        if ret is not NotImplemented:
            return ret

    raise TypeError(f'unsupported operand type(s) for @=: {lhs_type.__name__!r} and {rhs_type.__name__!r}')

def index(a) -> int:
    index_func = __pythonj_lookup_attr__(type(a), '__index__')
    if index_func is __pythonj_null__:
        raise TypeError(f'{type(a).__name__!r} object cannot be interpreted as an integer')
    ret = index_func(a)
    if not isinstance(ret, int):
        raise TypeError(f'__index__ returned non-int (type {type(ret).__name__})')
    return ret

def ipow(a, b):
    lhs_type = type(a)
    ipow_func = __pythonj_lookup_attr__(lhs_type, '__ipow__')
    if ipow_func is not __pythonj_null__:
        ret = ipow_func(a, b)
        if ret is not NotImplemented:
            return ret

    rhs_type = type(b)
    pow_func = __pythonj_lookup_attr__(lhs_type, '__pow__')
    rpow_func = __pythonj_lookup_attr__(rhs_type, '__rpow__')
    lhs_rpow_func = __pythonj_lookup_attr__(lhs_type, '__rpow__')

    reflected_first = rhs_type is not lhs_type and issubclass(rhs_type, lhs_type) and rpow_func is not __pythonj_null__ and rpow_func is not lhs_rpow_func
    if reflected_first:
        ret = rpow_func(b, a)
        if ret is not NotImplemented:
            return ret

    if pow_func is not __pythonj_null__:
        ret = pow_func(a, b)
        if ret is not NotImplemented:
            return ret

    if not reflected_first and rhs_type is not lhs_type and rpow_func is not __pythonj_null__:
        ret = rpow_func(b, a)
        if ret is not NotImplemented:
            return ret

    raise TypeError(f"unsupported operand type(s) for ** or pow(): {lhs_type.__name__!r} and {rhs_type.__name__!r}")

def matmul(a, b):
    lhs_type = type(a)
    rhs_type = type(b)
    matmul_func = __pythonj_lookup_attr__(lhs_type, '__matmul__')
    rmatmul_func = __pythonj_lookup_attr__(rhs_type, '__rmatmul__')
    lhs_rmatmul_func = __pythonj_lookup_attr__(lhs_type, '__rmatmul__')

    reflected_first = rhs_type is not lhs_type and issubclass(rhs_type, lhs_type) and rmatmul_func is not __pythonj_null__ and rmatmul_func is not lhs_rmatmul_func
    if reflected_first:
        ret = rmatmul_func(b, a)
        if ret is not NotImplemented:
            return ret

    if matmul_func is not __pythonj_null__:
        ret = matmul_func(a, b)
        if ret is not NotImplemented:
            return ret

    if not reflected_first and rhs_type is not lhs_type and rmatmul_func is not __pythonj_null__:
        ret = rmatmul_func(b, a)
        if ret is not NotImplemented:
            return ret

    raise TypeError(f'unsupported operand type(s) for @: {lhs_type.__name__!r} and {rhs_type.__name__!r}')

def pow(a, b):
    lhs_type = type(a)
    rhs_type = type(b)
    pow_func = __pythonj_lookup_attr__(lhs_type, '__pow__')
    rpow_func = __pythonj_lookup_attr__(rhs_type, '__rpow__')
    lhs_rpow_func = __pythonj_lookup_attr__(lhs_type, '__rpow__')

    reflected_first = rhs_type is not lhs_type and issubclass(rhs_type, lhs_type) and rpow_func is not __pythonj_null__ and rpow_func is not lhs_rpow_func
    if reflected_first:
        ret = rpow_func(b, a)
        if ret is not NotImplemented:
            return ret

    if pow_func is not __pythonj_null__:
        ret = pow_func(a, b)
        if ret is not NotImplemented:
            return ret

    if not reflected_first and rhs_type is not lhs_type and rpow_func is not __pythonj_null__:
        ret = rpow_func(b, a)
        if ret is not NotImplemented:
            return ret

    raise TypeError(f"unsupported operand type(s) for ** or pow(): {lhs_type.__name__!r} and {rhs_type.__name__!r}")

def setitem(a, b, c) -> None:
    a[b] = c
    return None
