# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

import math
import _operator

# Builtin functions
def abs(arg):
    return __pythonj_abs__(arg)

def all(iterable) -> bool:
    for item in iterable:
        if not item:
            return False
    return True

def any(iterable) -> bool:
    for item in iterable:
        if item:
            return True
    return False

def bin(arg) -> str:
    value: int = _operator.index(arg)
    return f'{value:#b}'

def delattr(obj, name) -> None:
    if not isinstance(name, str):
        raise TypeError(f'attribute name must be string, not {type(name).__name__!r}')
    __pythonj_lookup_attr__(type(obj), '__delattr__')(obj, name)
    return None

def divmod(a, b) -> tuple:
    return (a // b, a % b)

def format(value, format_spec) -> str:
    if not isinstance(format_spec, str):
        raise TypeError(f'format() argument 2 must be str, not {type(format_spec).__name__}')
    ret = __pythonj_lookup_attr__(type(value), '__format__')(value, format_spec)
    if not isinstance(ret, str):
        raise TypeError(f'__format__ must return a str, not {type(ret).__name__}')
    return ret

def getattr(obj, name, default):
    if not isinstance(name, str):
        raise TypeError(f'attribute name must be string, not {type(name).__name__!r}')
    try:
        return __pythonj_lookup_attr__(type(obj), '__getattribute__')(obj, name)
    except AttributeError as e:
        if default is not __pythonj_null__:
            return default
        raise e

def hasattr(obj, name) -> bool:
    try:
        getattr(obj, name)
    except AttributeError:
        return False
    return True

def hash(arg) -> int:
    hash_func = __pythonj_lookup_attr__(type(arg), '__hash__')
    if hash_func is __pythonj_null__ or hash_func is None:
        raise TypeError(f'unhashable type: {type(arg).__name__!r}')
    ret = hash_func(arg)
    if not isinstance(ret, int):
        raise TypeError('__hash__ method should return an integer')
    return ret

def hex(arg) -> str:
    value: int = _operator.index(arg)
    return f'{value:#x}'

def isinstance(obj, class_or_tuple) -> bool:
    if __pythonj_isinstance__(class_or_tuple, tuple):
        for x in class_or_tuple:
            if isinstance(obj, x):
                return True
        return False
    return __pythonj_isinstance__(obj, class_or_tuple)

def issubclass(obj, class_or_tuple) -> bool:
    if isinstance(class_or_tuple, tuple):
        for x in class_or_tuple:
            if issubclass(obj, x):
                return True
        return False
    return __pythonj_issubclass__(obj, class_or_tuple)

def len(arg) -> int:
    return __pythonj_len__(arg)

def next(iterator, default):
    ret = __pythonj_next__(iterator)
    if ret is __pythonj_null__:
        if default is not __pythonj_null__:
            return default
        raise StopIteration()
    return ret

def oct(arg) -> str:
    value: int = _operator.index(arg)
    return f'{value:#o}'

def repr(arg) -> str:
    ret = __pythonj_lookup_attr__(type(arg), '__repr__')(arg)
    if not isinstance(ret, str):
        raise TypeError(f'__repr__ returned non-string (type {type(ret).__name__})')
    return ret

def setattr(obj, name, value) -> None:
    if not isinstance(name, str):
        raise TypeError(f'attribute name must be string, not {type(name).__name__!r}')
    __pythonj_lookup_attr__(type(obj), '__setattr__')(obj, name, value)
    return None

def sum(iterable, start):
    if isinstance(start, str):
        raise TypeError("sum() can't sum strings [use ''.join(seq) instead]")
    if isinstance(start, bytes):
        raise TypeError("sum() can't sum bytes [use b''.join(seq) instead]")
    if isinstance(start, bytearray):
        raise TypeError("sum() can't sum bytearray [use b''.join(seq) instead]")
    for item in iterable:
        start = start + item
    return start

# Builtin class constructors
def enumerate__newobj(type, args: tuple, kwargs: dict):
    total_args: int = len(args)
    if kwargs:
        total_args += len(kwargs)
    if total_args == 1 or total_args == 2:
        iterable = args[0] if len(args) >= 1 else None
        start = args[1] if len(args) >= 2 else None
        if kwargs:
            for (kw, value) in kwargs.items():
                if kw == 'iterable' and iterable is None:
                    iterable = value
                elif total_args == 2 and kw == 'start' and start is None:
                    start = value
                else:
                    raise TypeError(f'{kw!r} is an invalid keyword argument for enumerate()')
        if iterable is None:
            raise TypeError("enumerate() missing required argument 'iterable'")
        return enumerate(iterable) if start is None else enumerate(iterable, start)
    if len(args) == 0:
        raise TypeError("enumerate() missing required argument 'iterable'")
    raise TypeError(f'enumerate() takes at most 2 arguments ({total_args} given)')

def zip__newobj(type, args: tuple, kwargs: dict):
    strict = None
    if kwargs:
        kwargs_len = len(kwargs)
        args_len = len(args)
        if kwargs_len > 1:
            if args_len == 0:
                raise TypeError(f'zip() takes at most 1 keyword argument ({kwargs_len} given)')
            raise TypeError(f'zip() takes at most 1 arguments ({args_len + kwargs_len} given)')
        for (kw, value) in kwargs.items():
            if kw != 'strict':
                raise TypeError(f'zip() got an unexpected keyword argument {kw!r}')
            strict = value
    return __pythonj_zip_new__(args, strict)

# Builtin classes
class object:
    def __getattribute__(self, name):
        if not isinstance(name, str):
            raise TypeError(f'attribute name must be string, not {type(name).__name__!r}')
        desc = __pythonj_lookup_attr__(type(self), name)
        if desc is not __pythonj_null__ and __pythonj_is_data_descriptor__(desc):
            return __pythonj_get__(desc, self, type(self))
        d = __pythonj_instance_dict__(self)
        if d is not __pythonj_null__:
            value = __pythonj_dict_get__(d, name)
            if value is not __pythonj_null__:
                return value
        if desc is not __pythonj_null__:
            return __pythonj_get__(desc, self, type(self))
        raise AttributeError(f'{type(self).__name__!r} object has no attribute {name!r}')

    def __setattr__(self, name, value) -> None:
        if not isinstance(name, str):
            raise TypeError(f'attribute name must be string, not {type(name).__name__!r}')
        desc = __pythonj_lookup_attr__(type(self), name)
        if desc is not __pythonj_null__ and __pythonj_is_data_descriptor__(desc):
            __pythonj_set__(desc, self, value)
            return None
        d = __pythonj_instance_dict__(self)
        if d is not __pythonj_null__:
            d[name] = value
            return None
        if desc is not __pythonj_null__:
            raise AttributeError(f'{type(self).__name__!r} object attribute {name!r} is read-only')
        raise AttributeError(f'{type(self).__name__!r} object has no attribute {name!r} and no __dict__ for setting new attributes')

    def __delattr__(self, name) -> None:
        if not isinstance(name, str):
            raise TypeError(f'attribute name must be string, not {type(name).__name__!r}')
        desc = __pythonj_lookup_attr__(type(self), name)
        if desc is not __pythonj_null__ and __pythonj_is_data_descriptor__(desc):
            __pythonj_delete__(desc, self)
            return None
        d = __pythonj_instance_dict__(self)
        if d is not __pythonj_null__:
            if __pythonj_dict_remove__(d, name) is not __pythonj_null__:
                return None
        if desc is not __pythonj_null__:
            raise AttributeError(f'{type(self).__name__!r} object attribute {name!r} is read-only')
        if d is not __pythonj_null__:
            raise AttributeError(f'{type(self).__name__!r} object has no attribute {name!r}')
        raise AttributeError(f'{type(self).__name__!r} object has no attribute {name!r} and no __dict__ for setting new attributes')

    def __format__(self, format_spec) -> str:
        if not isinstance(format_spec, str):
            raise TypeError(f'__format__() argument must be str, not {type(format_spec).__name__}')
        if format_spec != '':
            raise TypeError(f'unsupported format string passed to {type(self).__name__}.__format__')
        return str(self)

    def __repr__(self) -> str:
        return f'<{type(self).__name__} object>'

class type:
    def __getattribute__(self: type, name):
        if not isinstance(name, str):
            raise TypeError(f'attribute name must be string, not {type(name).__name__!r}')
        meta_desc = __pythonj_lookup_attr__(type(self), name)
        if meta_desc is not __pythonj_null__ and __pythonj_is_data_descriptor__(meta_desc):
            return __pythonj_get__(meta_desc, self, type(self))
        desc = __pythonj_lookup_attr__(self, name)
        if desc is not __pythonj_null__:
            return __pythonj_get__(desc, __pythonj_null__, self)
        if meta_desc is not __pythonj_null__:
            return __pythonj_get__(meta_desc, self, type(self))
        raise AttributeError(f'type object {self.__name__!r} has no attribute {name!r}')

    def __setattr__(self: type, name, value):
        raise TypeError(f'cannot set {name!r} attribute of immutable type {self.__name__!r}')

    def __delattr__(self: type, name):
        raise TypeError(f'cannot set {name!r} attribute of immutable type {self.__name__!r}')

class bool:
    def __bool__(self: bool) -> bool:
        return self

    def __format__(self: bool, format_spec) -> str:
        if not isinstance(format_spec, str):
            raise TypeError(f'__format__() argument must be str, not {type(format_spec).__name__}')
        return pyj_bool_format(self, format_spec)

    def __repr__(self: bool) -> str:
        return 'True' if self else 'False'

class bytearray:
    def __repr__(self: bytearray) -> str:
        return 'bytearray(' + repr(bytes(self)) + ')'

    def fromhex(self, string): __pythonj_unsupported__()
    def append(self, item): __pythonj_unsupported__()
    def capitalize(self): __pythonj_unsupported__()
    def center(self, width, fillchar): __pythonj_unsupported__()
    def clear(self): __pythonj_unsupported__()
    def copy(self): __pythonj_unsupported__()
    def count(self, sub, start, end): __pythonj_unsupported__()
    def decode(self, encoding, errors): __pythonj_unsupported__()
    def endswith(self, suffix, start, end): __pythonj_unsupported__()
    def expandtabs(self, tabsize): __pythonj_unsupported__()
    def extend(self, iterable_of_ints): __pythonj_unsupported__()
    def find(self, sub, start, end): __pythonj_unsupported__()
    def hex(self, sep, bytes_per_sep): __pythonj_unsupported__()
    def index(self, sub, start, end): __pythonj_unsupported__()
    def insert(self, index, item): __pythonj_unsupported__()
    def isalnum(self): __pythonj_unsupported__()
    def isalpha(self): __pythonj_unsupported__()
    def isascii(self): __pythonj_unsupported__()
    def isdigit(self): __pythonj_unsupported__()
    def islower(self): __pythonj_unsupported__()
    def isspace(self): __pythonj_unsupported__()
    def istitle(self): __pythonj_unsupported__()
    def isupper(self): __pythonj_unsupported__()
    def join(self, iterable_of_bytes): __pythonj_unsupported__()
    def ljust(self, width, fillchar): __pythonj_unsupported__()
    def lower(self): __pythonj_unsupported__()
    def lstrip(self, bytes): __pythonj_unsupported__()
    def partition(self, sep): __pythonj_unsupported__()
    def pop(self, index): __pythonj_unsupported__()
    def remove(self, value): __pythonj_unsupported__()
    def replace(self, old, new, count): __pythonj_unsupported__()
    def removeprefix(self, prefix): __pythonj_unsupported__()
    def removesuffix(self, suffix): __pythonj_unsupported__()
    def resize(self, size): __pythonj_unsupported__()
    def reverse(self): __pythonj_unsupported__()
    def rfind(self, sub, start, end): __pythonj_unsupported__()
    def rindex(self, sub, start, end): __pythonj_unsupported__()
    def rjust(self, width, fillchar): __pythonj_unsupported__()
    def rpartition(self, sep): __pythonj_unsupported__()
    def rsplit(self, sep, maxsplit): __pythonj_unsupported__()
    def rstrip(self, bytes): __pythonj_unsupported__()
    def split(self, sep, maxsplit): __pythonj_unsupported__()
    def splitlines(self, keepends): __pythonj_unsupported__()
    def startswith(self, prefix, start, end): __pythonj_unsupported__()
    def strip(self, bytes): __pythonj_unsupported__()
    def swapcase(self): __pythonj_unsupported__()
    def title(self): __pythonj_unsupported__()
    def translate(self, table, delete): __pythonj_unsupported__()
    def upper(self): __pythonj_unsupported__()
    def zfill(self, width): __pythonj_unsupported__()

class bytes:
    def __repr__(self: bytes) -> str:
        ret = __pythonj_str_builder__(len(self) + 3)
        __pythonj_str_builder_append__(ret, "b'")
        c: int
        for c in self:
            if c == 10:
                __pythonj_str_builder_append__(ret, '\\n')
            elif c == 13:
                __pythonj_str_builder_append__(ret, '\\r')
            elif c == 9:
                __pythonj_str_builder_append__(ret, '\\t')
            elif c == 39:
                __pythonj_str_builder_append__(ret, "\\'")
            elif c == 92:
                __pythonj_str_builder_append__(ret, '\\\\')
            elif 32 <= c < 127:
                __pythonj_str_builder_append__(ret, chr(c))
            else:
                __pythonj_str_builder_append__(ret, '\\x')
                __pythonj_str_builder_append__(ret, format(c, '02x'))
        __pythonj_str_builder_append__(ret, "'")
        return __pythonj_str_builder_finish__(ret)

    def capitalize(self: bytes) -> bytes:
        ret = __pythonj_bytes_builder__(__pythonj_len__(self))
        seen_alpha = False
        c: int
        for c in self:
            if 97 <= c <= 122:
                if not seen_alpha:
                    __pythonj_bytes_builder_append_int__(ret, c - 32)
                    seen_alpha = True
                else:
                    __pythonj_bytes_builder_append_int__(ret, c)
            elif 65 <= c <= 90:
                if not seen_alpha:
                    __pythonj_bytes_builder_append_int__(ret, c)
                    seen_alpha = True
                else:
                    __pythonj_bytes_builder_append_int__(ret, c + 32)
            else:
                __pythonj_bytes_builder_append_int__(ret, c)
        return __pythonj_bytes_builder_finish__(ret)

    def count(self: bytes, sub, start, end) -> int:
        indices = slice(start, end).indices(len(self))
        start = indices[0]
        end = indices[1]
        ret: int
        i: int
        if isinstance(sub, int):
            if sub < 0 or sub > 255:
                raise ValueError('byte must be in range(0, 256)')
            ret = 0
            for i in range(start, end):
                if self[i] == sub:
                    ret += 1
            return ret
        if not __pythonj_isinstance__(sub, (bytes, bytearray)):
            raise TypeError(f'argument should be integer or bytes-like object, not {type(sub).__name__!r}')
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

    def endswith(self: bytes, suffix, start, end) -> bool:
        if isinstance(suffix, tuple):
            suffix_tuple: tuple = suffix
            for item in suffix_tuple:
                if self.endswith(item, start, end):
                    return True
            return False
        if not __pythonj_isinstance__(suffix, (bytes, bytearray)):
            raise TypeError(f'endswith first arg must be bytes or a tuple of bytes, not {type(suffix).__name__}')
        if len(suffix) == 0 and start is not None and start > len(self):
            return False
        indices = slice(start, end).indices(len(self))
        start = indices[0]
        end = indices[1]
        suffix_len = len(suffix)
        if suffix_len > end - start:
            return False
        return self[end - suffix_len:end] == suffix

    def find(self: bytes, sub, start, end) -> int:
        indices = slice(start, end).indices(len(self))
        start = indices[0]
        end = indices[1]
        if isinstance(sub, int):
            if sub < 0 or sub > 255:
                raise ValueError('byte must be in range(0, 256)')
            for i in range(start, end):
                if self[i] == sub:
                    return i
            return -1
        if not __pythonj_isinstance__(sub, (bytes, bytearray)):
            raise TypeError(f'argument should be integer or bytes-like object, not {type(sub).__name__!r}')
        sub_len = len(sub)
        if sub_len == 0:
            return start
        limit = end - sub_len
        for i in range(start, limit + 1):
            if self[i:i + sub_len] == sub:
                return i
        return -1

    def fromhex(self, string) -> bytes:
        is_str = isinstance(string, str)
        is_bytes_like = __pythonj_isinstance__(string, (bytes, bytearray))
        if not (is_str or is_bytes_like):
            raise TypeError(f'fromhex() argument must be str or bytes-like, not {type(string).__name__}')
        ret = __pythonj_bytes_builder__(len(string) // 2)
        i: int = 0
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
            hi: int
            lo: int
            if is_str:
                hi = ord(string[i])
                lo = ord(string[i + 1])
            else:
                hi = string[i]
                lo = string[i + 1]
            value: int
            if 48 <= hi <= 57:
                value = hi - 48
            elif 97 <= hi <= 102:
                value = hi - 97 + 10
            elif 65 <= hi <= 70:
                value = hi - 65 + 10
            else:
                raise ValueError(f'non-hexadecimal number found in fromhex() arg at position {i}')
            value *= 16
            if 48 <= lo <= 57:
                value += lo - 48
            elif 97 <= lo <= 102:
                value += lo - 97 + 10
            elif 65 <= lo <= 70:
                value += lo - 65 + 10
            else:
                raise ValueError(f'non-hexadecimal number found in fromhex() arg at position {i+1}')
            __pythonj_bytes_builder_append_int__(ret, value)
            i += 2
        return __pythonj_bytes_builder_finish__(ret)

    def hex(self: bytes, sep, bytes_per_sep) -> str:
        if sep is __pythonj_null__:
            sep = ''
        elif isinstance(sep, bytes):
            if len(sep) != 1:
                raise ValueError('sep must be length 1.')
            sep = chr(sep[0])
        elif isinstance(sep, str):
            if len(sep) != 1:
                raise ValueError('sep must be length 1.')
        else:
            if len(sep) != 1:
                raise ValueError('sep must be length 1.')
            raise TypeError('sep must be str or bytes.')
        bytes_per_sep = _operator.index(bytes_per_sep)
        sep_str: str = sep
        ret = __pythonj_str_builder__(len(self) * 2)
        n = len(self)
        i: int
        if sep and bytes_per_sep != 0:
            group_size = abs(bytes_per_sep)
            if bytes_per_sep > 0:
                i = 0
                while i < n:
                    if i != 0 and ((n - i) % group_size == 0):
                        __pythonj_str_builder_append__(ret, sep_str)
                    __pythonj_str_builder_append__(ret, format(self[i], '02x'))
                    i += 1
            else:
                i = 0
                while i < n:
                    if i != 0:
                        __pythonj_str_builder_append__(ret, sep_str)
                    group_end = i + group_size
                    while i < group_end and i < n:
                        __pythonj_str_builder_append__(ret, format(self[i], '02x'))
                        i += 1
        else:
            c: int
            for c in self:
                __pythonj_str_builder_append__(ret, format(c, '02x'))
        return __pythonj_str_builder_finish__(ret)

    def index(self: bytes, sub, start, end) -> int:
        ret = self.find(sub, start, end)
        if ret == -1:
            raise ValueError('subsection not found')
        return ret

    def join(self: bytes, iterable) -> bytes:
        ret = __pythonj_bytes_builder__(None)
        if not __pythonj_hasiter__(iterable):
            raise TypeError('can only join an iterable')
        i: int = 0
        for item in iterable:
            if i != 0:
                __pythonj_bytes_builder_append_bytes__(ret, self)
            if isinstance(item, bytes):
                __pythonj_bytes_builder_append_bytes__(ret, item)
            elif isinstance(item, bytearray):
                __pythonj_bytes_builder_append_bytearray__(ret, item)
            else:
                raise TypeError(f'sequence item {i}: expected a bytes-like object, {type(item).__name__} found')
            i += 1
        return __pythonj_bytes_builder_finish__(ret)

    def isalnum(self: bytes) -> bool:
        if not self:
            return False
        c: int
        for c in self:
            if not (48 <= c <= 57 or 65 <= c <= 90 or 97 <= c <= 122):
                return False
        return True

    def isalpha(self: bytes) -> bool:
        if not self:
            return False
        c: int
        for c in self:
            if not (65 <= c <= 90 or 97 <= c <= 122):
                return False
        return True

    def isascii(self: bytes) -> bool:
        c: int
        for c in self:
            if c >= 128:
                return False
        return True

    def isdigit(self: bytes) -> bool:
        if not self:
            return False
        c: int
        for c in self:
            if not (48 <= c <= 57):
                return False
        return True

    def islower(self: bytes) -> bool:
        has_cased = False
        c: int
        for c in self:
            if 65 <= c <= 90:
                return False
            if 97 <= c <= 122:
                has_cased = True
        return has_cased

    def isspace(self: bytes) -> bool:
        if not self:
            return False
        c: int
        for c in self:
            if c not in b' \t\n\r\x0b\x0c':
                return False
        return True

    def istitle(self: bytes) -> bool:
        has_cased = False
        in_word = False
        c: int
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

    def isupper(self: bytes) -> bool:
        has_cased = False
        c: int
        for c in self:
            if 97 <= c <= 122:
                return False
            if 65 <= c <= 90:
                has_cased = True
        return has_cased

    def lower(self: bytes) -> bytes:
        ret = __pythonj_bytes_builder__(__pythonj_len__(self))
        c: int
        for c in self:
            if 65 <= c <= 90:
                __pythonj_bytes_builder_append_int__(ret, c + 32)
            else:
                __pythonj_bytes_builder_append_int__(ret, c)
        return __pythonj_bytes_builder_finish__(ret)

    def lstrip(self: bytes, bytes_arg) -> bytes:
        if bytes_arg is None:
            strip_set = b' \t\n\r\x0b\x0c'
        elif __pythonj_isinstance__(bytes_arg, (bytes, bytearray)):
            strip_set = bytes_arg
        else:
            raise TypeError(f'a bytes-like object is required, not {type(bytes_arg).__name__!r}')
        i: int = 0
        n = len(self)
        while i < n and self[i] in strip_set:
            i += 1
        return self[i:]

    def partition(self: bytes, sep) -> tuple:
        if not __pythonj_isinstance__(sep, (bytes, bytearray)):
            raise TypeError(f'a bytes-like object is required, not {type(sep).__name__!r}')
        if len(sep) == 0:
            raise ValueError('empty separator')
        i = self.find(sep)
        if i == -1:
            return (self, b'', b'')
        return (self[:i], sep, self[i + len(sep):])

    def removeprefix(self: bytes, prefix) -> bytes:
        if self.startswith(prefix):
            return self[len(prefix):]
        return self

    def removesuffix(self: bytes, suffix) -> bytes:
        if suffix and self.endswith(suffix):
            return self[:-len(suffix)]
        return self

    def rfind(self: bytes, sub, start, end) -> int:
        indices = slice(start, end).indices(len(self))
        start = indices[0]
        end = indices[1]
        if isinstance(sub, int):
            if sub < 0 or sub > 255:
                raise ValueError('byte must be in range(0, 256)')
            for i in range(end - 1, start - 1, -1):
                if self[i] == sub:
                    return i
            return -1
        if not __pythonj_isinstance__(sub, (bytes, bytearray)):
            raise TypeError(f'argument should be integer or bytes-like object, not {type(sub).__name__!r}')
        sub_len = len(sub)
        if sub_len == 0:
            return end
        limit = end - sub_len
        for i in range(limit, start - 1, -1):
            if self[i:i + sub_len] == sub:
                return i
        return -1

    def rindex(self: bytes, sub, start, end) -> int:
        ret = self.rfind(sub, start, end)
        if ret == -1:
            raise ValueError('subsection not found')
        return ret

    def rpartition(self: bytes, sep) -> tuple:
        if not __pythonj_isinstance__(sep, (bytes, bytearray)):
            raise TypeError(f'a bytes-like object is required, not {type(sep).__name__!r}')
        if len(sep) == 0:
            raise ValueError('empty separator')
        i = self.rfind(sep, None, None)
        if i == -1:
            return (b'', b'', self)
        return (self[:i], sep, self[i + len(sep):])

    def rstrip(self: bytes, bytes_arg) -> bytes:
        if bytes_arg is None:
            strip_set = b' \t\n\r\x0b\x0c'
        elif __pythonj_isinstance__(bytes_arg, (bytes, bytearray)):
            strip_set = bytes_arg
        else:
            raise TypeError(f'a bytes-like object is required, not {type(bytes_arg).__name__!r}')
        i: int = len(self)
        while i > 0 and self[i - 1] in strip_set:
            i -= 1
        return self[:i]

    def startswith(self: bytes, prefix, start, end) -> bool:
        if isinstance(prefix, tuple):
            prefix_tuple: tuple = prefix
            for item in prefix_tuple:
                if self.startswith(item, start, end):
                    return True
            return False
        if not __pythonj_isinstance__(prefix, (bytes, bytearray)):
            raise TypeError(f'startswith first arg must be bytes or a tuple of bytes, not {type(prefix).__name__}')
        if len(prefix) == 0 and start is not None and start > len(self):
            return False
        indices = slice(start, end).indices(len(self))
        start = indices[0]
        end = indices[1]
        prefix_len = len(prefix)
        if prefix_len > end - start:
            return False
        return self[start:start + prefix_len] == prefix

    def strip(self: bytes, bytes_arg) -> bytes:
        return self.lstrip(bytes_arg).rstrip(bytes_arg)

    def swapcase(self: bytes) -> bytes:
        ret = __pythonj_bytes_builder__(__pythonj_len__(self))
        c: int
        for c in self:
            if 65 <= c <= 90:
                __pythonj_bytes_builder_append_int__(ret, c + 32)
            elif 97 <= c <= 122:
                __pythonj_bytes_builder_append_int__(ret, c - 32)
            else:
                __pythonj_bytes_builder_append_int__(ret, c)
        return __pythonj_bytes_builder_finish__(ret)

    def title(self: bytes) -> bytes:
        ret = __pythonj_bytes_builder__(__pythonj_len__(self))
        in_word = False
        c: int
        for c in self:
            if 97 <= c <= 122:
                if in_word:
                    __pythonj_bytes_builder_append_int__(ret, c)
                else:
                    __pythonj_bytes_builder_append_int__(ret, c - 32)
                    in_word = True
            elif 65 <= c <= 90:
                if in_word:
                    __pythonj_bytes_builder_append_int__(ret, c + 32)
                else:
                    __pythonj_bytes_builder_append_int__(ret, c)
                    in_word = True
            else:
                __pythonj_bytes_builder_append_int__(ret, c)
                in_word = False
        return __pythonj_bytes_builder_finish__(ret)

    def upper(self: bytes) -> bytes:
        ret = __pythonj_bytes_builder__(__pythonj_len__(self))
        c: int
        for c in self:
            if 97 <= c <= 122:
                __pythonj_bytes_builder_append_int__(ret, c - 32)
            else:
                __pythonj_bytes_builder_append_int__(ret, c)
        return __pythonj_bytes_builder_finish__(ret)

    def center(self, width, fillchar): __pythonj_unsupported__()
    def decode(self, encoding, errors): __pythonj_unsupported__()
    def expandtabs(self, tabsize): __pythonj_unsupported__()
    def ljust(self, width, fillchar): __pythonj_unsupported__()
    def replace(self, old, new, count): __pythonj_unsupported__()
    def rjust(self, width, fillchar): __pythonj_unsupported__()
    def rsplit(self, sep, maxsplit): __pythonj_unsupported__()
    def split(self, sep, maxsplit): __pythonj_unsupported__()
    def splitlines(self, keepends): __pythonj_unsupported__()
    def translate(self, table, delete): __pythonj_unsupported__()
    def zfill(self, width): __pythonj_unsupported__()

class dict:
    def __contains__(self: dict, key) -> bool:
        return key in self

    def __repr__(self: dict) -> str:
        ret = __pythonj_str_builder__(8*len(self) + 2)
        __pythonj_str_builder_append__(ret, '{')
        first = True
        for (key, value) in self.items():
            if not first:
                __pythonj_str_builder_append__(ret, ', ')
            first = False
            __pythonj_str_builder_append__(ret, repr(key))
            __pythonj_str_builder_append__(ret, ': ')
            __pythonj_str_builder_append__(ret, repr(value))
        __pythonj_str_builder_append__(ret, '}')
        return __pythonj_str_builder_finish__(ret)

    def fromkeys(self, iterable, value) -> dict:
        ret = {}
        for key in iterable:
            ret[key] = value
        return ret

    def pop(self: dict, key, default):
        value = __pythonj_dict_remove__(self, key)
        if value is not __pythonj_null__:
            return value
        if default is not __pythonj_null__:
            return default
        raise KeyError(key)

    def setdefault(self: dict, key, default):
        value = __pythonj_dict_get__(self, key)
        if value is not __pythonj_null__:
            return value
        self[key] = default
        return default

class float:
    @__pythonj_getter__
    def imag(self: float) -> float:
        return 0.0

    @__pythonj_getter__
    def real(self: float) -> float:
        return self

    def __bool__(self: float) -> bool:
        return self != 0.0

    def __format__(self: float, format_spec) -> str:
        if not isinstance(format_spec, str):
            raise TypeError(f'__format__() argument must be str, not {type(format_spec).__name__}')
        return pyj_float_format(self, format_spec)

    def __repr__(self: float) -> str:
        return pyj_float_str(self)

    def as_integer_ratio(self) -> tuple:
        if math.isnan(self):
            raise ValueError('cannot convert NaN to integer ratio')
        if not math.isfinite(self):
            raise OverflowError('cannot convert Infinity to integer ratio')
        bits = __pythonj_float_java_bits__(self)
        negative: bool = bits < 0
        exponent_bits = (bits >> 52) & 0x7FF
        numerator: int = bits & ((1 << 52) - 1)
        exponent: int
        if exponent_bits == 0:
            exponent = -1022 - 52
        else:
            numerator |= (1 << 52)
            exponent = exponent_bits - 1023 - 52
        while (numerator & 1) == 0 and exponent < 0:
            numerator >>= 1
            exponent += 1
        denominator: int = 1
        if exponent > 0:
            numerator <<= exponent
        elif exponent < 0:
            denominator <<= -exponent
        if negative:
            numerator = -numerator
        return (numerator, denominator)

    def conjugate(self) -> float:
        return self

    def from_number(self, number) -> float:
        if not __pythonj_isinstance__(number, (float, int, bool)):
            raise TypeError(f'must be real number, not {type(number).__name__}')
        return float(number)

    def is_integer(self) -> bool:
        return math.isfinite(self) and self == __pythonj_float_java_rint__(self)

    def fromhex(self, s): __pythonj_unsupported__()
    def hex(self):__pythonj_unsupported__()

class int:
    @__pythonj_getter__
    def denominator(self: int) -> int:
        return 1

    @__pythonj_getter__
    def imag(self: int) -> int:
        return 0

    @__pythonj_getter__
    def numerator(self: int) -> int:
        return self

    @__pythonj_getter__
    def real(self: int) -> int:
        return self

    def __bool__(self: int) -> bool:
        return self != 0

    def __format__(self: int, format_spec) -> str:
        if not isinstance(format_spec, str):
            raise TypeError(f'__format__() argument must be str, not {type(format_spec).__name__}')
        return pyj_int_format(self, format_spec)

    def __repr__(self: int) -> str:
        return __pythonj_int_java_str__(self)

    def as_integer_ratio(self) -> tuple:
        return (self, 1)

    def bit_count(self) -> int:
        if self == (-1 << 63):
            return 1
        return __pythonj_int_java_bit_count__(abs(self))

    def bit_length(self) -> int:
        if self == (-1 << 63):
            return 64
        return 64 - __pythonj_int_java_leading_zeros__(abs(self))

    def conjugate(self) -> int:
        return self

    def from_bytes(self, bytes_arg, byteorder, signed) -> int:
        little_endian = False
        if byteorder is not None:
            if not isinstance(byteorder, str):
                raise TypeError(f"from_bytes() argument 'byteorder' must be str, not {type(byteorder).__name__}")
            if byteorder == 'big':
                little_endian = False
            elif byteorder == 'little':
                little_endian = True
            else:
                raise ValueError("byteorder must be either 'little' or 'big'")

        if __pythonj_isinstance__(bytes_arg, (bytes, bytearray)):
            data = bytes_arg
        else:
            if isinstance(bytes_arg, int):
                raise TypeError(f"cannot convert '{type(bytes_arg).__name__}' object to bytes")
            data = bytes(bytes_arg)

        if len(data) == 0:
            return 0

        if little_endian:
            big_endian_data = data[::-1]
        else:
            big_endian_data = data

        data_len: int = len(big_endian_data)
        result: int = 0
        i: int = 0
        if signed:
            negative: bool = (big_endian_data[0] & 0x80) != 0
            pad = 0xFF if negative else 0x00
            start: int = 0
            while (data_len - start > 8) and (big_endian_data[start] == pad):
                start += 1
            data_len -= start
            if data_len > 8 or (data_len == 8 and (((big_endian_data[start] & 0x80) != 0) != negative)):
                raise OverflowError('int too big to convert')
            i = start
            if negative:
                result = big_endian_data[start] - 0x100
                i = start + 1
            while i < len(big_endian_data):
                result = (result << 8) | big_endian_data[i]
                i += 1
            return result

        start = 0
        while (data_len - start > 8) and (big_endian_data[start] == 0):
            start += 1
        data_len -= start
        if data_len > 8 or (data_len == 8 and ((big_endian_data[start] & 0x80) != 0)):
            raise OverflowError('int too big to convert')
        result = 0
        i = start
        while i < len(big_endian_data):
            result = (result << 8) | big_endian_data[i]
            i += 1
        return result

    def is_integer(self) -> bool:
        return True

    def to_bytes(self: int, length, byteorder, signed) -> bytes:
        length = _operator.index(length)
        little_endian = False
        if byteorder is not None:
            if not isinstance(byteorder, str):
                raise TypeError(f"to_bytes() argument 'byteorder' must be str, not {type(byteorder).__name__}")
            if byteorder == 'big':
                little_endian = False
            elif byteorder == 'little':
                little_endian = True
            else:
                raise ValueError("byteorder must be either 'little' or 'big'")
        if length < 0:
            raise ValueError('length argument must be non-negative')
        value: int = self
        if not signed:
            if value < 0:
                raise OverflowError("can't convert negative int to unsigned")
            if length < 8:
                max_value = (1 << (length * 8)) - 1
                if value > max_value:
                    raise OverflowError('int too big to convert')
        elif length < 8:
            if length == 0:
                if value != 0:
                    raise OverflowError('int too big to convert')
            else:
                bits = length * 8
                min_value = -(1 << (bits - 1))
                max_value = (1 << (bits - 1)) - 1
                if value < min_value or value > max_value:
                    raise OverflowError('int too big to convert')
        out = bytearray(length)
        i: int = 0
        while i < length:
            idx = i if little_endian else (length - 1 - i)
            out[idx] = value & 0xFF
            value >>= 8
            i += 1
        return bytes(out)

class list:
    def __repr__(self: list) -> str:
        ret = __pythonj_str_builder__(4*len(self) + 2)
        __pythonj_str_builder_append__(ret, '[')
        first = True
        for item in self:
            if not first:
                __pythonj_str_builder_append__(ret, ', ')
            first = False
            __pythonj_str_builder_append__(ret, repr(item))
        __pythonj_str_builder_append__(ret, ']')
        return __pythonj_str_builder_finish__(ret)

class range:
    @__pythonj_getter__
    def start(self: range) -> int:
        return __pythonj_range_start__(self)

    @__pythonj_getter__
    def step(self: range) -> int:
        return __pythonj_range_step__(self)

    @__pythonj_getter__
    def stop(self: range) -> int:
        return __pythonj_range_stop__(self)

    def __contains__(self: range, rhs) -> bool:
        value: int
        if isinstance(rhs, float):
            if not math.isfinite(rhs) or rhs != __pythonj_float_java_rint__(rhs):
                return False
            value = int(rhs)
        elif __pythonj_isinstance__(rhs, (int, bool)):
            value = int(rhs)
        else:
            return False
        if self.step > 0:
            if value < self.start or value >= self.stop:
                return False
        else:
            if value > self.start or value <= self.stop:
                return False
        return (value - self.start) % self.step == 0

    def __repr__(self: range) -> str:
        if self.step == 1:
            return f'range({self.start}, {self.stop})'
        return f'range({self.start}, {self.stop}, {self.step})'

    def count(self: range, value) -> int:
        return 1 if value in self else 0

    def index(self: range, value) -> int:
        if isinstance(value, float):
            if not math.isfinite(value) or value != __pythonj_float_java_rint__(value):
                raise ValueError('sequence.index(x): x not in sequence')
            value = int(value)
            if value not in self:
                raise ValueError('sequence.index(x): x not in sequence')
            return (value - self.start) // self.step
        elif not __pythonj_isinstance__(value, (int, bool)):
            raise ValueError('sequence.index(x): x not in sequence')
        if value not in self:
            raise ValueError('range.index(x): x not in range')
        return (value - self.start) // self.step

class set:
    def __contains__(self: set, key) -> bool:
        return key in self

    def __repr__(self: set) -> str:
        if not self:
            return 'set()'
        ret = __pythonj_str_builder__(4*len(self) + 2)
        __pythonj_str_builder_append__(ret, '{')
        first = True
        for item in self:
            if not first:
                __pythonj_str_builder_append__(ret, ', ')
            first = False
            __pythonj_str_builder_append__(ret, repr(item))
        __pythonj_str_builder_append__(ret, '}')
        return __pythonj_str_builder_finish__(ret)

class slice:
    @__pythonj_getter__
    def start(self: slice):
        return __pythonj_slice_start__(self)

    @__pythonj_getter__
    def step(self: slice):
        return __pythonj_slice_step__(self)

    @__pythonj_getter__
    def stop(self: slice):
        return __pythonj_slice_stop__(self)

    def __repr__(self: slice) -> str:
        return f'slice({self.start!r}, {self.stop!r}, {self.step!r})'

class str:
    def __format__(self: str, format_spec) -> str:
        if not isinstance(format_spec, str):
            raise TypeError(f'__format__() argument must be str, not {type(format_spec).__name__}')
        return pyj_str_format(self, format_spec)

    def join(self: str, iterable) -> str:
        ret = __pythonj_str_builder__(4*len(self) + 2)
        if not __pythonj_hasiter__(iterable):
            raise TypeError('can only join an iterable')
        i: int = 0
        for item in iterable:
            if i != 0:
                __pythonj_str_builder_append__(ret, self)
            if not isinstance(item, str):
                raise TypeError(f'sequence item {i}: expected str instance, {type(item).__name__} found')
            __pythonj_str_builder_append__(ret, item)
            i += 1
        return __pythonj_str_builder_finish__(ret)

    def removeprefix(self: str, prefix) -> str:
        if not isinstance(prefix, str):
            raise TypeError(f'removeprefix() argument must be str, not {type(prefix).__name__}')
        if self.startswith(prefix):
            return self[len(prefix):]
        return self

    def removesuffix(self: str, suffix) -> str:
        if not isinstance(suffix, str):
            raise TypeError(f'removesuffix() argument must be str, not {type(suffix).__name__}')
        if suffix and self.endswith(suffix):
            return self[:-len(suffix)]
        return self

    def replace(self: str, old, new, count) -> str:
        if not isinstance(old, str):
            raise TypeError(f'replace() argument 1 must be str, not {type(old).__name__}')
        if not isinstance(new, str):
            raise TypeError(f'replace() argument 2 must be str, not {type(new).__name__}')
        max_count: int = _operator.index(count)
        if max_count == 0:
            return self
        replace_all: bool = max_count < 0
        replacements_left: int = -1 if replace_all else max_count
        self_len: int = len(self)
        old_len: int = len(old)
        i: int = 0
        if old_len == 0:
            max_inserts: int = self_len + 1 if replace_all or max_count > self_len + 1 else max_count
            ret = __pythonj_str_builder__(self_len + max_inserts * len(new))
            while i < self_len:
                if replacements_left != 0:
                    __pythonj_str_builder_append__(ret, new)
                    if replacements_left > 0:
                        replacements_left -= 1
                __pythonj_str_builder_append__(ret, self[i])
                i += 1
            if replacements_left != 0:
                __pythonj_str_builder_append__(ret, new)
            return __pythonj_str_builder_finish__(ret)
        ret = __pythonj_str_builder__(self_len + len(new))
        limit: int = self_len - old_len
        while i <= limit:
            if replacements_left != 0 and self[i:i + old_len] == old:
                __pythonj_str_builder_append__(ret, new)
                i += old_len
                if replacements_left > 0:
                    replacements_left -= 1
            else:
                __pythonj_str_builder_append__(ret, self[i])
                i += 1
        while i < self_len:
            __pythonj_str_builder_append__(ret, self[i])
            i += 1
        return __pythonj_str_builder_finish__(ret)

    def rstrip(self: str, chars) -> str:
        if chars is None:
            strip_chars: str = ' \t\n\v\f\r\x1c\x1d\x1e\x1f\x85\xa0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u2028\u2029\u202f\u205f\u3000'
        elif isinstance(chars, str):
            strip_chars = chars
        else:
            raise TypeError('rstrip arg must be None or str')
        end: int = len(self)
        while end > 0 and self[end - 1] in strip_chars:
            end -= 1
        return self[:end]

    def capitalize(self): __pythonj_unsupported__()
    def casefold(self): __pythonj_unsupported__()
    def center(self, width, fillchar): __pythonj_unsupported__()
    def encode(self, encoding, errors): __pythonj_unsupported__()
    def expandtabs(self, tabsize): __pythonj_unsupported__()
    def format_map(self, mapping): __pythonj_unsupported__()
    def isalnum(self): __pythonj_unsupported__()
    def isalpha(self): __pythonj_unsupported__()
    def isascii(self): __pythonj_unsupported__()
    def isidentifier(self): __pythonj_unsupported__()
    def islower(self): __pythonj_unsupported__()
    def isnumeric(self): __pythonj_unsupported__()
    def isprintable(self): __pythonj_unsupported__()
    def isspace(self): __pythonj_unsupported__()
    def istitle(self): __pythonj_unsupported__()
    def isupper(self): __pythonj_unsupported__()
    def ljust(self, width, fillchar): __pythonj_unsupported__()
    def lstrip(self, chars): __pythonj_unsupported__()
    def partition(self, sep): __pythonj_unsupported__()
    def rfind(self, sub, start, end): __pythonj_unsupported__()
    def rindex(self, sub, start, end): __pythonj_unsupported__()
    def rjust(self, width, fillchar): __pythonj_unsupported__()
    def rpartition(self, sep): __pythonj_unsupported__()
    def rsplit(self, sep, maxsplit): __pythonj_unsupported__()
    def splitlines(self, keepends): __pythonj_unsupported__()
    def strip(self, chars): __pythonj_unsupported__()
    def swapcase(self): __pythonj_unsupported__()
    def title(self): __pythonj_unsupported__()
    def translate(self, table): __pythonj_unsupported__()
    def zfill(self, width): __pythonj_unsupported__()

class tuple:
    def __contains__(self: tuple, rhs) -> bool:
        for item in self:
            if item == rhs:
                return True
        return False

    def __repr__(self: tuple) -> str:
        ret = __pythonj_str_builder__(4*len(self) + 2)
        __pythonj_str_builder_append__(ret, '(')
        first = True
        for item in self:
            if not first:
                __pythonj_str_builder_append__(ret, ', ')
            first = False
            __pythonj_str_builder_append__(ret, repr(item))
        if len(self) == 1:
            __pythonj_str_builder_append__(ret, ',')
        __pythonj_str_builder_append__(ret, ')')
        return __pythonj_str_builder_finish__(ret)

    def count(self: tuple, value) -> int:
        n: int = 0
        for item in self:
            if item == value:
                n += 1
        return n

    def index(self: tuple, value, start, stop) -> int:
        n = len(self)
        start_index: int = pyj_slice_index_allow_null(start, 0, n)
        stop_index: int = pyj_slice_index_allow_null(stop, n, n)
        i: int
        for i in range(start_index, stop_index):
            if self[i] == value:
                return i
        raise ValueError('tuple.index(x): x not in tuple')
