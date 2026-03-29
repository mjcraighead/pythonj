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

def _pyj_format_parse_common(spec):
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
    while i < n and spec[i].isdecimal():
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
        while i < n and spec[i].isdecimal():
            i += 1
        if i == precision_start:
            raise ValueError('Format specifier missing precision')
        precision = int(spec[precision_start:i])

    type_char = ''
    if i < n:
        type_char = spec[i]
        i += 1
    return (fill, align, sign, z, alt, zero, width, grouping, precision, type_char, i, n)

def _pyj_raise_unknown_format_code(type_char, type_name):
    raise ValueError(f"Unknown format code {type_char!r} for object of type '{type_name}'")

def _pyj_raise_invalid_format_spec(spec, type_name):
    raise ValueError(f"Invalid format specifier {spec!r} for object of type '{type_name}'")

def _pyj_format_split_sign(text):
    if text and text[0] in '+- ':
        return (text[0], text[1:])
    return ('', text)

def _pyj_format_apply_width(text, fill, align, width, default_align):
    if width is None or len(text) >= width:
        return text

    if align is None:
        align = default_align

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
        sign, rest = _pyj_format_split_sign(text)
        return sign + pad + rest
    assert False, align

def _pyj_format_group_digits(digits, grouping, group_size):
    if grouping is None:
        return digits
    parts = []
    i = len(digits)
    while i > group_size:
        parts.append(digits[i - group_size:i])
        i -= group_size
    parts.append(digits[:i])
    parts.reverse()
    return grouping.join(parts)

def _pyj_format_zero_fill_grouped(sign, prefix, digits, grouping, group_size, suffix, width):
    if grouping is None:
        return sign + prefix + ('0' * (width - len(sign) - len(prefix) - len(digits) - len(suffix))) + digits + suffix

    digits_len = len(digits)
    total_len = len(sign) + len(prefix) + digits_len + ((digits_len - 1) // group_size) + len(suffix)
    while total_len < width:
        digits_len += 1
        total_len = len(sign) + len(prefix) + digits_len + ((digits_len - 1) // group_size) + len(suffix)
    if digits_len > len(digits):
        digits = ('0' * (digits_len - len(digits))) + digits
    return sign + prefix + _pyj_format_group_digits(digits, grouping, group_size) + suffix

def _pyj_int_base_digits(value, base, alphabet):
    if value == 0:
        return '0'
    ret = []
    while value:
        ret.append(alphabet[value % base])
        value //= base
    ret.reverse()
    return ''.join(ret)

def pyj_float_parse_spec(spec):
    fill, align, sign, z, alt, zero, width, grouping, precision, type_char, i, n = _pyj_format_parse_common(spec)

    if type_char and type_char not in 'eEfFgGn%':
        if i != n:
            _pyj_raise_invalid_format_spec(spec, 'float')
        _pyj_raise_unknown_format_code(type_char, 'float')

    if i != n:
        _pyj_raise_invalid_format_spec(spec, 'float')

    if zero and align is None:
        fill = '0'
        align = '='

    return (fill, align, sign, z, alt, width, grouping, precision, type_char)

def pyj_int_parse_spec(spec):
    fill, align, sign, z, alt, zero, width, grouping, precision, type_char, i, n = _pyj_format_parse_common(spec)

    if z:
        raise ValueError('Negative zero coercion (z) not allowed in integer format specifier')
    if precision is not None:
        raise ValueError('Precision not allowed in integer format specifier')

    if type_char and type_char not in 'boxXdn':
        if i != n:
            _pyj_raise_invalid_format_spec(spec, 'int')
        _pyj_raise_unknown_format_code(type_char, 'int')

    if i != n:
        _pyj_raise_invalid_format_spec(spec, 'int')

    if zero and align is None:
        fill = '0'
        align = '='

    type_char = type_char or 'd'
    if type_char == 'n':
        if grouping is not None:
            raise ValueError(f"Cannot specify {grouping!r} with 'n'.")
        type_char = 'd'

    if grouping == ',' and type_char != 'd':
        raise ValueError(f"Cannot specify ',' with {type_char!r}.")

    return (fill, align, sign, alt, width, grouping, type_char)

def pyj_str_parse_spec(spec):
    fill, align, sign, z, alt, zero, width, grouping, precision, type_char, i, n = _pyj_format_parse_common(spec)

    if sign != '-':
        raise ValueError('Sign not allowed in string format specifier')
    if z:
        _pyj_raise_invalid_format_spec(spec, 'str')
    if alt:
        raise ValueError('Alternate form (#) not allowed in string format specifier')
    if grouping is not None:
        raise ValueError('Cannot specify grouping in string format specifier')
    if align == '=':
        raise ValueError("'=' alignment not allowed in string format specifier")

    if type_char and type_char != 's':
        if i != n:
            _pyj_raise_invalid_format_spec(spec, 'str')
        _pyj_raise_unknown_format_code(type_char, 'str')

    if i != n:
        _pyj_raise_invalid_format_spec(spec, 'str')

    if zero and align is None:
        fill = '0'
        align = '<'

    return (fill, align, width, precision)

def pyj_float_special_text(value, type_char):
    import math
    if math.isnan(value):
        text = 'nan'
    elif math.isinf(value):
        text = 'inf'
    else:
        return None
    if type_char in ('E', 'F', 'G'):
        text = text.upper()
    if type_char == '%':
        text += '%'
    return text

def _pyj_float_is_zero_result(magnitude_text):
    for c in magnitude_text:
        if c in '0.,_%':
            continue
        if c in 'eE':
            continue
        if c in '+-':
            continue
        return False
    return True

def _pyj_float_sign_prefix(value, sign, z, magnitude_text):
    import math
    is_nan = math.isnan(value)
    if is_nan:
        if sign == '+':
            return '+'
        if sign == ' ':
            return ' '
        return ''
    is_negative = math.copysign(1.0, value) < 0.0
    if is_negative and z and _pyj_float_is_zero_result(magnitude_text):
        is_negative = False
    if is_negative:
        return '-'
    if sign == '+':
        return '+'
    if sign == ' ':
        return ' '
    return ''

def _pyj_float_apply_zero_fill(text, grouping, width):
    sign, magnitude = _pyj_format_split_sign(text)
    if grouping is None:
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

    if not integer_digits.isdigit():
        return sign + ('0' * (width - len(sign) - len(magnitude))) + magnitude

    return _pyj_format_zero_fill_grouped(sign, '', integer_digits, grouping, 3, fractional_suffix + exp_suffix, width)

def pyj_float_finish_text(fill, align, sign, z, width, grouping, value, magnitude_text):
    text = _pyj_float_sign_prefix(value, sign, z, magnitude_text) + magnitude_text
    if align == '=' and fill == '0' and width is not None:
        return _pyj_float_apply_zero_fill(text, grouping, width)
    return _pyj_format_apply_width(text, fill, align, width, '>')

def pyj_int_format(value, spec):
    fill, align, sign, alt, width, grouping, type_char = pyj_int_parse_spec(spec)

    is_negative = value < 0
    abs_value = -value if is_negative else value
    sign_prefix = '-'
    if not is_negative:
        sign_prefix = ''
        if sign == '+':
            sign_prefix = '+'
        elif sign == ' ':
            sign_prefix = ' '

    prefix = ''
    group_size = 3
    if type_char == 'd':
        digits = str(abs_value)
    elif type_char == 'x':
        digits = _pyj_int_base_digits(abs_value, 16, '0123456789abcdef')
        if alt:
            prefix = '0x'
        group_size = 4
    elif type_char == 'X':
        digits = _pyj_int_base_digits(abs_value, 16, '0123456789ABCDEF')
        if alt:
            prefix = '0X'
        group_size = 4
    elif type_char == 'b':
        digits = _pyj_int_base_digits(abs_value, 2, '01')
        if alt:
            prefix = '0b'
        group_size = 4
    elif type_char == 'o':
        digits = _pyj_int_base_digits(abs_value, 8, '01234567')
        if alt:
            prefix = '0o'
        group_size = 4
    else:
        assert False, type_char

    if align == '=' and fill == '0' and width is not None:
        return _pyj_format_zero_fill_grouped(sign_prefix, prefix, digits, grouping, group_size, '', width)

    grouped_digits = _pyj_format_group_digits(digits, grouping, group_size)
    if align == '=' and width is not None:
        text = sign_prefix + prefix + grouped_digits
        if len(text) >= width:
            return text
        return sign_prefix + prefix + (fill * (width - len(text))) + grouped_digits

    text = sign_prefix + prefix + grouped_digits
    return _pyj_format_apply_width(text, fill, align, width, '>')

def pyj_str_format(value, spec):
    fill, align, width, precision = pyj_str_parse_spec(spec)
    text = value if precision is None else value[:precision]
    return _pyj_format_apply_width(text, fill, align, width, '<')

def _pyj_percent_arg_seq(args):
    if isinstance(args, tuple):
        return args
    return (args,)

def _pyj_percent_int_arg(arg, conv):
    try:
        return int(arg)
    except BaseException:
        if conv == 'd':
            raise TypeError(f'%d format: a real number is required, not {type(arg).__name__}')
        raise TypeError(f'%{conv} format: an integer is required, not {type(arg).__name__}')

def _pyj_percent_item_text(conv, flags, width, arg):
    if conv == 's':
        text = str(arg)
    elif conv == 'r':
        text = repr(arg)
    elif conv == 'd':
        spec = '+' if '+' in flags else (' ' if ' ' in flags else '')
        if ('0' in flags) and ('-' not in flags) and (width is not None):
            spec += '0' + str(width)
            width = None
        spec += 'd'
        text = pyj_int_format(_pyj_percent_int_arg(arg, conv), spec)
    elif conv == 'x':
        spec = '+' if '+' in flags else (' ' if ' ' in flags else '')
        if '#' in flags:
            spec += '#'
        if ('0' in flags) and ('-' not in flags) and (width is not None):
            spec += '0' + str(width)
            width = None
        spec += 'x'
        text = pyj_int_format(_pyj_percent_int_arg(arg, conv), spec)
    elif conv == 'X':
        spec = '+' if '+' in flags else (' ' if ' ' in flags else '')
        if '#' in flags:
            spec += '#'
        if ('0' in flags) and ('-' not in flags) and (width is not None):
            spec += '0' + str(width)
            width = None
        spec += 'X'
        text = pyj_int_format(_pyj_percent_int_arg(arg, conv), spec)
    else:
        raise ValueError(conv)

    if width is not None:
        align = '<' if '-' in flags else '>'
        text = _pyj_format_apply_width(text, ' ', align, width, '>')
    return text

def pyj_percent_format(fmt, args):
    arg_seq = _pyj_percent_arg_seq(args)
    arg_index = 0
    used_mapping = False
    out = []
    i = 0
    n = len(fmt)
    while i < n:
        c = fmt[i]
        if c != '%':
            out.append(c)
            i += 1
            continue
        i += 1
        if i == n:
            raise ValueError('incomplete format')
        if fmt[i] == '%':
            out.append('%')
            i += 1
            continue

        mapping_key = None
        if fmt[i] == '(':
            if not isinstance(args, dict):
                raise TypeError('format requires a mapping')
            used_mapping = True
            i += 1
            key_start = i
            while i < n and fmt[i] != ')':
                i += 1
            if i == n:
                raise ValueError('incomplete format key')
            mapping_key = fmt[key_start:i]
            i += 1

        flags = ''
        while i < n and fmt[i] in '-+ 0#':
            if fmt[i] not in flags:
                flags += fmt[i]
            i += 1

        width_start = i
        while i < n and fmt[i].isdecimal():
            i += 1
        width = int(fmt[width_start:i]) if i > width_start else None

        if i == n:
            raise ValueError('incomplete format')
        conv = fmt[i]
        i += 1

        if mapping_key is None:
            if arg_index >= len(arg_seq):
                raise TypeError('not enough arguments for format string')
            arg = arg_seq[arg_index]
            arg_index += 1
        else:
            arg = args[mapping_key]
        out.append(_pyj_percent_item_text(conv, flags, width, arg))

    if (not used_mapping) and (arg_index != len(arg_seq)):
        raise TypeError('not all arguments converted during string formatting')
    return ''.join(out)
