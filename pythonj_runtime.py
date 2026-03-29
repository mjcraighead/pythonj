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

def pyj_float_parse_spec(spec):
    i = 0
    n = len(spec)
    fill = ' '
    align = None

    if i + 1 < n and spec[i + 1] in '<>=^':
        fill = spec[i]
        align = spec[i + 1]
        i += 2
    elif i < n and spec[i] in '<>=^':
        align = spec[i]
        i += 1

    sign = '-'
    if i < n and spec[i] in '+- ':
        sign = spec[i]
        i += 1

    z = False
    if i < n and spec[i] == 'z':
        z = True
        i += 1

    alt = False
    if i < n and spec[i] == '#':
        alt = True
        i += 1

    zero = False
    if i < n and spec[i] == '0':
        zero = True
        i += 1

    width_start = i
    while i < n and ('0' <= spec[i] <= '9'):
        i += 1
    width = int(spec[width_start:i]) if i > width_start else None

    grouping = None
    if i < n and spec[i] in ',_':
        grouping = spec[i]
        i += 1

    precision = None
    if i < n and spec[i] == '.':
        i += 1
        precision_start = i
        while i < n and ('0' <= spec[i] <= '9'):
            i += 1
        if i == precision_start:
            raise ValueError('Format specifier missing precision')
        precision = int(spec[precision_start:i])

    type_char = ''
    if i < n:
        type_char = spec[i]
        i += 1
        if type_char not in 'eEfFgGn%':
            if i != n:
                raise ValueError(f"Invalid format specifier {spec!r} for object of type 'float'")
            raise ValueError(f"Unknown format code {type_char!r} for object of type 'float'")

    if i != n:
        raise ValueError(f"Invalid format specifier {spec!r} for object of type 'float'")

    if zero and align is None:
        fill = '0'
        align = '='

    return (fill, align, sign, z, alt, width, grouping, precision, type_char)

def pyj_float_core_type_char(type_char):
    if type_char == '%':
        return 'f'
    return type_char

def pyj_float_core_grouping(fill, align, width, grouping):
    if align == '=' and fill == '0' and width is not None and grouping is not None:
        return None
    return grouping

def pyj_float_special_text(is_nan, type_char):
    if is_nan:
        text = 'nan'
    else:
        text = 'inf'
    if type_char in ('E', 'F', 'G'):
        text = text.upper()
    if type_char == '%':
        text += '%'
    return text

def pyj_float_apply_percent(text, type_char):
    if type_char == '%':
        return text + '%'
    return text

def pyj_float_sign_prefix(is_nan, is_negative, sign, z, magnitude_text):
    if is_nan:
        if sign == '+':
            return '+'
        if sign == ' ':
            return ' '
        return ''
    if is_negative:
        is_zero_result = True
        for c in magnitude_text:
            if c in '0.,_%':
                continue
            if c in 'eE':
                continue
            if c in '+-':
                continue
            if c in 'infINFnanNAN':
                is_zero_result = False
                break
            is_zero_result = False
            break
        if z and is_zero_result:
            is_negative = False
    if is_negative:
        return '-'
    if sign == '+':
        return '+'
    if sign == ' ':
        return ' '
    return ''

def pyj_float_apply_width(text, fill, align, width):
    if width is None or len(text) >= width:
        return text

    if align is None:
        align = '>'

    pad_count = width - len(text)
    pad = fill * pad_count
    if align == '<':
        return text + pad
    if align == '>':
        return pad + text
    if align == '^':
        left = pad_count // 2
        right = pad_count - left
        return (fill * left) + text + (fill * right)
    if align == '=':
        if text and text[0] in '+- ':
            sign = text[0]
            rest = text[1:]
        else:
            sign = ''
            rest = text
        return sign + pad + rest
    assert False, align

def pyj_float_apply_zero_fill(text, grouping, width):
    if text and text[0] in '+- ':
        sign = text[0]
        magnitude = text[1:]
    else:
        sign = ''
        magnitude = text
    if grouping is None:
        return sign + ('0' * (width - len(sign) - len(magnitude))) + magnitude

    has_digit = False
    for c in magnitude:
        if '0' <= c <= '9':
            has_digit = True
            break
    if not has_digit:
        return sign + ('0' * (width - len(sign) - len(magnitude))) + magnitude

    exp_index = magnitude.find('e')
    if exp_index == -1:
        exp_index = magnitude.find('E')
    if exp_index == -1:
        exp_suffix = ''
        mantissa = magnitude
    else:
        exp_suffix = magnitude[exp_index:]
        mantissa = magnitude[:exp_index]

    dot_index = mantissa.find('.')
    if dot_index == -1:
        integer_digits = mantissa
        fractional_suffix = ''
    else:
        integer_digits = mantissa[:dot_index]
        fractional_suffix = mantissa[dot_index:]

    digits_len = len(integer_digits)
    total_len = len(sign) + digits_len + ((digits_len - 1) // 3) + len(fractional_suffix) + len(exp_suffix)
    while total_len < width:
        digits_len += 1
        total_len = len(sign) + digits_len + ((digits_len - 1) // 3) + len(fractional_suffix) + len(exp_suffix)
    if digits_len > len(integer_digits):
        integer_digits = ('0' * (digits_len - len(integer_digits))) + integer_digits

    parts = []
    i = len(integer_digits)
    while i > 3:
        parts.append(integer_digits[i - 3:i])
        i -= 3
    parts.append(integer_digits[:i])
    parts.reverse()
    return sign + grouping.join(parts) + fractional_suffix + exp_suffix
