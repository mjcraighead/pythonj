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

def bin(arg):
    value = __pythonj_index__(arg)
    if value < 0:
        return '-0b' + format(-value, 'b')
    return '0b' + format(value, 'b')

def delattr(obj, name):
    if not __pythonj_isinstance__(name, str):
        raise TypeError('attribute name must be string, not ' + repr(type(name).__name__))
    return __pythonj_delattr__(obj, name)

def format(value, format_spec):
    if not __pythonj_isinstance__(format_spec, str):
        raise TypeError('format() argument 2 must be str, not ' + type(format_spec).__name__)
    return __pythonj_format__(value, format_spec)

def getattr(obj, name, default):
    if not __pythonj_isinstance__(name, str):
        raise TypeError('attribute name must be string, not ' + repr(type(name).__name__))
    try:
        return __pythonj_getattr__(obj, name)
    except AttributeError as e:
        if default is not __pythonj_null__:
            return default
        raise e

def hasattr(obj, name):
    try:
        getattr(obj, name)
    except AttributeError:
        return False
    return True

def hash(arg):
    return __pythonj_hash__(arg)

def isinstance(obj, class_or_tuple):
    if __pythonj_isinstance__(class_or_tuple, tuple):
        for x in class_or_tuple:
            if isinstance(obj, x):
                return True
        return False
    return __pythonj_isinstance__(obj, class_or_tuple)

def issubclass(obj, class_or_tuple):
    if __pythonj_isinstance__(class_or_tuple, tuple):
        for x in class_or_tuple:
            if issubclass(obj, x):
                return True
        return False
    return __pythonj_issubclass__(obj, class_or_tuple)

def len(arg):
    return __pythonj_len__(arg)

def next(iterator, default):
    ret = __pythonj_next__(iterator)
    if ret is __pythonj_null__:
        if default is not __pythonj_null__:
            return default
        raise StopIteration()
    return ret

def oct(arg):
    value = __pythonj_index__(arg)
    if value < 0:
        return '-0o' + format(-value, 'o')
    return '0o' + format(value, 'o')

def repr(arg):
    return __pythonj_repr__(arg)

def setattr(obj, name, value):
    if not __pythonj_isinstance__(name, str):
        raise TypeError('attribute name must be string, not ' + repr(type(name).__name__))
    return __pythonj_setattr__(obj, name, value)

def sum(iterable, start):
    if __pythonj_isinstance__(start, str):
        raise TypeError("sum() can't sum strings [use ''.join(seq) instead]")
    if __pythonj_isinstance__(start, bytes):
        raise TypeError("sum() can't sum bytes [use b''.join(seq) instead]")
    if __pythonj_isinstance__(start, bytearray):
        raise TypeError("sum() can't sum bytearray [use b''.join(seq) instead]")
    for item in iterable:
        start = start + item
    return start

class bytes:
    def capitalize(self):
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

    def count(self, sub, start, end):
        indices = slice(start, end).indices(len(self))
        start = indices[0]
        end = indices[1]
        if __pythonj_isinstance__(sub, int):
            if sub < 0 or sub > 255:
                raise ValueError('byte must be in range(0, 256)')
            ret = 0
            for i in range(start, end):
                if self[i] == sub:
                    ret += 1
            return ret
        if not (__pythonj_isinstance__(sub, bytes) or __pythonj_isinstance__(sub, bytearray)):
            raise TypeError('argument should be integer or bytes-like object, not ' + repr(type(sub).__name__))
        sub_len = len(sub)
        if sub_len == 0:
            return end - start + 1
        ret = 0
        i = start
        limit = end - sub_len
        while i <= limit:
            if self[i:i + sub_len] == sub:
                ret += 1
                i += sub_len
            else:
                i += 1
        return ret

    def endswith(self, suffix, start, end):
        if __pythonj_isinstance__(suffix, tuple):
            for item in suffix:
                if self.endswith(item, start, end):
                    return True
            return False
        if not (__pythonj_isinstance__(suffix, bytes) or __pythonj_isinstance__(suffix, bytearray)):
            raise TypeError('endswith first arg must be bytes or a tuple of bytes, not ' + type(suffix).__name__)
        if len(suffix) == 0 and start is not None and start > len(self):
            return False
        indices = slice(start, end).indices(len(self))
        start = indices[0]
        end = indices[1]
        suffix_len = len(suffix)
        if suffix_len > end - start:
            return False
        return self[end - suffix_len:end] == suffix

    def find(self, sub, start, end):
        indices = slice(start, end).indices(len(self))
        start = indices[0]
        end = indices[1]
        if __pythonj_isinstance__(sub, int):
            if sub < 0 or sub > 255:
                raise ValueError('byte must be in range(0, 256)')
            for i in range(start, end):
                if self[i] == sub:
                    return i
            return -1
        if not (__pythonj_isinstance__(sub, bytes) or __pythonj_isinstance__(sub, bytearray)):
            raise TypeError('argument should be integer or bytes-like object, not ' + repr(type(sub).__name__))
        sub_len = len(sub)
        if sub_len == 0:
            return start
        limit = end - sub_len
        for i in range(start, limit + 1):
            if self[i:i + sub_len] == sub:
                return i
        return -1

    def fromhex(self, string):
        is_str = __pythonj_isinstance__(string, str)
        is_bytes = __pythonj_isinstance__(string, bytes)
        is_bytearray = __pythonj_isinstance__(string, bytearray)
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

    def hex(self, sep, bytes_per_sep):
        if sep is __pythonj_null__:
            sep = ''
        elif __pythonj_isinstance__(sep, bytes):
            if len(sep) != 1:
                raise ValueError('sep must be length 1.')
            sep = chr(sep[0])
        elif __pythonj_isinstance__(sep, str):
            if len(sep) != 1:
                raise ValueError('sep must be length 1.')
        else:
            len(sep)
            raise AssertionError(sep)
        bytes_per_sep = __pythonj_index__(bytes_per_sep)
        ret = []
        for c in self:
            ret.append(format(c, '02x'))
        if sep and bytes_per_sep != 0:
            grouped = []
            n = len(ret)
            group_size = abs(bytes_per_sep)
            if bytes_per_sep > 0:
                first_group = n % group_size
                if first_group == 0:
                    first_group = group_size
                i = 0
                grouped.append(''.join(ret[:first_group]))
                i = first_group
                while i < n:
                    grouped.append(''.join(ret[i:i + group_size]))
                    i += group_size
            else:
                i = 0
                while i < n:
                    grouped.append(''.join(ret[i:i + group_size]))
                    i += group_size
            return sep.join(grouped)
        return ''.join(ret)

    def index(self, sub, start, end):
        ret = self.find(sub, start, end)
        if ret == -1:
            raise ValueError('subsection not found')
        return ret

    def isalnum(self):
        if not self:
            return False
        for c in self:
            if not (48 <= c <= 57 or 65 <= c <= 90 or 97 <= c <= 122):
                return False
        return True

    def isalpha(self):
        if not self:
            return False
        for c in self:
            if not (65 <= c <= 90 or 97 <= c <= 122):
                return False
        return True

    def isascii(self):
        for c in self:
            if c >= 128:
                return False
        return True

    def isdigit(self):
        if not self:
            return False
        for c in self:
            if not (48 <= c <= 57):
                return False
        return True

    def islower(self):
        has_cased = False
        for c in self:
            if 65 <= c <= 90:
                return False
            if 97 <= c <= 122:
                has_cased = True
        return has_cased

    def isspace(self):
        if not self:
            return False
        for c in self:
            if c not in b' \t\n\r\x0b\x0c':
                return False
        return True

    def istitle(self):
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

    def isupper(self):
        has_cased = False
        for c in self:
            if 97 <= c <= 122:
                return False
            if 65 <= c <= 90:
                has_cased = True
        return has_cased

    def lower(self):
        ret = []
        for c in self:
            if 65 <= c <= 90:
                ret.append(c + 32)
            else:
                ret.append(c)
        return bytes(ret)

    def lstrip(self, bytes_arg):
        if bytes_arg is None:
            strip_set = b' \t\n\r\x0b\x0c'
        elif __pythonj_isinstance__(bytes_arg, bytes) or __pythonj_isinstance__(bytes_arg, bytearray):
            strip_set = bytes_arg
        else:
            raise TypeError('a bytes-like object is required, not ' + repr(type(bytes_arg).__name__))
        i = 0
        n = len(self)
        while i < n and self[i] in strip_set:
            i += 1
        return self[i:]

    def partition(self, sep):
        if not (__pythonj_isinstance__(sep, bytes) or __pythonj_isinstance__(sep, bytearray)):
            raise TypeError('a bytes-like object is required, not ' + repr(type(sep).__name__))
        if len(sep) == 0:
            raise ValueError('empty separator')
        i = self.find(sep, None, None)
        if i == -1:
            return (self, b'', b'')
        return (self[:i], sep, self[i + len(sep):])

    def removeprefix(self, prefix):
        if self.startswith(prefix, None, None):
            return self[len(prefix):]
        return self

    def removesuffix(self, suffix):
        if suffix and self.endswith(suffix, None, None):
            return self[:-len(suffix)]
        return self

    def rfind(self, sub, start, end):
        indices = slice(start, end).indices(len(self))
        start = indices[0]
        end = indices[1]
        if __pythonj_isinstance__(sub, int):
            if sub < 0 or sub > 255:
                raise ValueError('byte must be in range(0, 256)')
            for i in range(end - 1, start - 1, -1):
                if self[i] == sub:
                    return i
            return -1
        if not (__pythonj_isinstance__(sub, bytes) or __pythonj_isinstance__(sub, bytearray)):
            raise TypeError('argument should be integer or bytes-like object, not ' + repr(type(sub).__name__))
        sub_len = len(sub)
        if sub_len == 0:
            return end
        limit = end - sub_len
        for i in range(limit, start - 1, -1):
            if self[i:i + sub_len] == sub:
                return i
        return -1

    def rindex(self, sub, start, end):
        ret = self.rfind(sub, start, end)
        if ret == -1:
            raise ValueError('subsection not found')
        return ret

    def rpartition(self, sep):
        if not (__pythonj_isinstance__(sep, bytes) or __pythonj_isinstance__(sep, bytearray)):
            raise TypeError('a bytes-like object is required, not ' + repr(type(sep).__name__))
        if len(sep) == 0:
            raise ValueError('empty separator')
        i = self.rfind(sep, None, None)
        if i == -1:
            return (b'', b'', self)
        return (self[:i], sep, self[i + len(sep):])

    def rstrip(self, bytes_arg):
        if bytes_arg is None:
            strip_set = b' \t\n\r\x0b\x0c'
        elif __pythonj_isinstance__(bytes_arg, bytes) or __pythonj_isinstance__(bytes_arg, bytearray):
            strip_set = bytes_arg
        else:
            raise TypeError('a bytes-like object is required, not ' + repr(type(bytes_arg).__name__))
        i = len(self)
        while i > 0 and self[i - 1] in strip_set:
            i -= 1
        return self[:i]

    def startswith(self, prefix, start, end):
        if __pythonj_isinstance__(prefix, tuple):
            for item in prefix:
                if self.startswith(item, start, end):
                    return True
            return False
        if not (__pythonj_isinstance__(prefix, bytes) or __pythonj_isinstance__(prefix, bytearray)):
            raise TypeError('startswith first arg must be bytes or a tuple of bytes, not ' + type(prefix).__name__)
        if len(prefix) == 0 and start is not None and start > len(self):
            return False
        indices = slice(start, end).indices(len(self))
        start = indices[0]
        end = indices[1]
        prefix_len = len(prefix)
        if prefix_len > end - start:
            return False
        return self[start:start + prefix_len] == prefix

    def strip(self, bytes_arg):
        return self.lstrip(bytes_arg).rstrip(bytes_arg)

    def swapcase(self):
        ret = []
        for c in self:
            if 65 <= c <= 90:
                ret.append(c + 32)
            elif 97 <= c <= 122:
                ret.append(c - 32)
            else:
                ret.append(c)
        return bytes(ret)

    def title(self):
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

    def upper(self):
        ret = []
        for c in self:
            if 97 <= c <= 122:
                ret.append(c - 32)
            else:
                ret.append(c)
        return bytes(ret)

class dict:
    def fromkeys(self, iterable, value):
        ret = self()
        for key in iterable:
            ret[key] = value
        return ret

    def setdefault(self, key, defaultValue):
        value = __pythonj_dict_get__(self, key)
        if value is not __pythonj_null__:
            return value
        self[key] = defaultValue
        return defaultValue

class float:
    def conjugate(self):
        return self

class int:
    def as_integer_ratio(self):
        return (self, 1)

    def conjugate(self):
        return self

    def is_integer(self):
        return True

class range:
    def count(self, value):
        return 1 if value in self else 0

class str:
    def removeprefix(self, prefix):
        if not __pythonj_isinstance__(prefix, str):
            raise TypeError("removeprefix() argument must be str, not " + type(prefix).__name__)
        if self.startswith(prefix):
            return self[len(prefix):]
        return self

    def removesuffix(self, suffix):
        if not __pythonj_isinstance__(suffix, str):
            raise TypeError("removesuffix() argument must be str, not " + type(suffix).__name__)
        if suffix and self.endswith(suffix):
            return self[:-len(suffix)]
        return self
