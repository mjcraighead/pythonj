# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

def dict__setdefault(self, key, defaultValue):
    value = __pythonj_dict_get__(self, key)
    if value is not __pythonj_null__:
        return value
    self[key] = defaultValue
    return defaultValue

def dict__fromkeys(self, iterable, value):
    ret = self()
    for key in iterable:
        ret[key] = value
    return ret

def bytes__capitalize(self):
    ret = []
    seen_alpha = False
    for c in self:
        if 97 <= c <= 122:
            if not seen_alpha:
                ret.append(c - 32)
                seen_alpha = True
            else:
                ret.append(c)
        elif 65 <= c <= 90:
            if not seen_alpha:
                ret.append(c)
                seen_alpha = True
            else:
                ret.append(c + 32)
        else:
            ret.append(c)
    return bytes(ret)

def bytes__fromhex(self, string):
    is_str = __pythonj_isinstance_single__(string, str)
    is_bytes = __pythonj_isinstance_single__(string, bytes)
    is_bytearray = __pythonj_isinstance_single__(string, bytearray)
    if not (is_str or is_bytes or is_bytearray):
        raise TypeError('fromhex() argument must be str or bytes-like, not ' + type(string).__name__)
    ret = []
    i = 0
    n = len(string)
    while i < n:
        c = string[i]
        if is_str:
            c = ord(c)
        if c in b' \t\n\r\x0b\x0c':
            i += 1
            continue
        if i + 1 >= n:
            raise ValueError('fromhex() arg must contain an even number of hexadecimal digits')
        hi = string[i]
        lo = string[i + 1]
        if is_str:
            hi = ord(hi)
            lo = ord(lo)
        if 48 <= hi <= 57:
            value = hi - 48
        elif 97 <= hi <= 102:
            value = hi - 97 + 10
        elif 65 <= hi <= 70:
            value = hi - 65 + 10
        else:
            raise ValueError('non-hexadecimal number found in fromhex() arg at position ' + str(i))
        value *= 16
        if 48 <= lo <= 57:
            value += lo - 48
        elif 97 <= lo <= 102:
            value += lo - 97 + 10
        elif 65 <= lo <= 70:
            value += lo - 65 + 10
        else:
            raise ValueError('non-hexadecimal number found in fromhex() arg at position ' + str(i + 1))
        ret.append(value)
        i += 2
    return self(ret)

def bytes__isalnum(self):
    if not self:
        return False
    for c in self:
        if not (48 <= c <= 57 or 65 <= c <= 90 or 97 <= c <= 122):
            return False
    return True

def bytes__isalpha(self):
    if not self:
        return False
    for c in self:
        if not (65 <= c <= 90 or 97 <= c <= 122):
            return False
    return True

def bytes__isascii(self):
    for c in self:
        if c >= 128:
            return False
    return True

def bytes__isdigit(self):
    if not self:
        return False
    for c in self:
        if not (48 <= c <= 57):
            return False
    return True

def bytes__islower(self):
    has_cased = False
    for c in self:
        if 65 <= c <= 90:
            return False
        if 97 <= c <= 122:
            has_cased = True
    return has_cased

def bytes__isspace(self):
    if not self:
        return False
    for c in self:
        if c not in b' \t\n\r\x0b\x0c':
            return False
    return True

def bytes__istitle(self):
    has_cased = False
    in_word = False
    for c in self:
        if 65 <= c <= 90:
            if in_word:
                return False
            has_cased = True
            in_word = True
        elif 97 <= c <= 122:
            if not in_word:
                return False
            has_cased = True
        else:
            in_word = False
    return has_cased

def bytes__isupper(self):
    has_cased = False
    for c in self:
        if 97 <= c <= 122:
            return False
        if 65 <= c <= 90:
            has_cased = True
    return has_cased

def bytes__lower(self):
    ret = []
    for c in self:
        if 65 <= c <= 90:
            ret.append(c + 32)
        else:
            ret.append(c)
    return bytes(ret)

def bytes__swapcase(self):
    ret = []
    for c in self:
        if 65 <= c <= 90:
            ret.append(c + 32)
        elif 97 <= c <= 122:
            ret.append(c - 32)
        else:
            ret.append(c)
    return bytes(ret)

def bytes__title(self):
    ret = []
    in_word = False
    for c in self:
        if 97 <= c <= 122:
            if in_word:
                ret.append(c)
            else:
                ret.append(c - 32)
                in_word = True
        elif 65 <= c <= 90:
            if in_word:
                ret.append(c + 32)
            else:
                ret.append(c)
                in_word = True
        else:
            ret.append(c)
            in_word = False
    return bytes(ret)

def bytes__upper(self):
    ret = []
    for c in self:
        if 97 <= c <= 122:
            ret.append(c - 32)
        else:
            ret.append(c)
    return bytes(ret)

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
    if suffix and self.endswith(suffix):
        return self[:-len(suffix)]
    return self
