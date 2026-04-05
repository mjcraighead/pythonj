# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

def _init_bound_args(args, max_total):
    args_len = len(args)
    bound_args: list
    bound_args = []
    i = 0
    while i < max_total:
        if i < args_len:
            bound_args.append(args[i])
        else:
            bound_args.append(__pythonj_null__)
        i += 1
    return bound_args

def _find_name(names, kw, start):
    names_len = len(names)
    i = start
    while i < names_len:
        if kw == names[i]:
            return i
        i += 1
    return __pythonj_null__

def _type_error_at_most_args(positional_name, max_total, given) -> TypeError:
    suffix = '' if max_total == 1 else 's'
    return TypeError(f'{positional_name}() takes at most {max_total} argument{suffix} ({given} given)')

def _type_error_at_most_keyword_args(kw_name, max_total, kwargs_len) -> TypeError:
    suffix = '' if max_total == 1 else 's'
    return TypeError(f'{kw_name}() takes at most {max_total} keyword argument{suffix} ({kwargs_len} given)')

def _type_error_unexpected_kw_arg(kw_name, unknown_kw) -> TypeError:
    return TypeError(f'{kw_name}() got an unexpected keyword argument {unknown_kw!r}')

def _type_error_user_missing_args(name, missing_arg_names) -> TypeError:
    missing = len(missing_arg_names)
    ret = f'{name}() missing {missing} required positional argument'
    if missing != 1:
        ret += 's'
    ret += ':'
    i = 0
    while i < missing:
        ret += f' {missing_arg_names[i]!r}'
        if missing >= 3 and i != missing - 1:
            ret += ','
        if i == missing - 2:
            ret += ' and'
        i += 1
    return TypeError(ret)

def _type_error_user_posonly_as_keyword(name, arg_names) -> TypeError:
    joined = ''
    i = 0
    while i < len(arg_names):
        if i != 0:
            joined += ', '
        joined += arg_names[i]
        i += 1
    return TypeError(f"{name}() got some positional-only arguments passed as keyword arguments: {joined!r}")

def bind_exact_positional(args, kwargs, kw_name, positional_name, n, generic_exact_args_style) -> tuple:
    if kwargs is not __pythonj_null__ and kwargs:
        raise TypeError(kw_name + '() takes no keyword arguments')
    args_len = len(args)
    if args_len != n:
        if n == 0:
            raise TypeError(f'{positional_name}() takes no arguments ({args_len} given)')
        if n == 1 and not generic_exact_args_style:
            raise TypeError(f'{positional_name}() takes exactly one argument ({args_len} given)')
        suffix = '' if n == 1 else 's'
        raise TypeError(f'{positional_name} expected {n} argument{suffix}, got {args_len}')
    return args

def bind_min_max_positional(args, kwargs, kw_name, positional_name, min_args, max_args) -> tuple:
    if kwargs is not __pythonj_null__ and kwargs:
        raise TypeError(kw_name + '() takes no keyword arguments')
    args_len = len(args)
    if args_len < min_args:
        suffix = '' if min_args == 1 else 's'
        raise TypeError(f'{positional_name} expected at least {min_args} argument{suffix}, got {args_len}')
    if args_len > max_args:
        suffix = '' if max_args == 1 else 's'
        raise TypeError(f'{positional_name} expected at most {max_args} argument{suffix}, got {args_len}')
    return args

def bind_min_max_positional_or_keyword(args, kwargs, kw_name, positional_name, positional_names, posonly_count, kwonly_names, min_args, max_positional, max_total, min_positional_style, exact_args_style) -> list:
    args_len = len(args)
    if exact_args_style and args_len != min_args:
        suffix = '' if min_args == 1 else 's'
        raise TypeError(f'{positional_name} expected {min_args} argument{suffix}, got {args_len}')
    if min_positional_style and args_len < min_args:
        suffix = '' if min_args == 1 else 's'
        raise TypeError(f'{positional_name}() takes at least {min_args} positional argument{suffix} ({args_len} given)')
    if args_len > max_total:
        raise _type_error_at_most_args(positional_name, max_total, args_len)

    bound_args = _init_bound_args(args, max_total)

    unknown_kw = None
    if kwargs is not __pythonj_null__ and kwargs:
        kwargs_len = len(kwargs)
        positional_names_len = len(positional_names)
        if args_len + kwargs_len > max_total:
            if args_len == 0:
                raise _type_error_at_most_keyword_args(kw_name, max_total, kwargs_len)
            raise _type_error_at_most_args(positional_name, max_total, args_len + kwargs_len)
        for (kw, value) in kwargs.items():
            matched_index = _find_name(positional_names, kw, posonly_count)
            if matched_index is not __pythonj_null__:
                if bound_args[matched_index] is not __pythonj_null__:
                    raise TypeError(f'argument for {kw_name}() given by name ({positional_names[matched_index]!r}) and position ({matched_index + 1})')
                bound_args[matched_index] = value
            else:
                kwonly_index = _find_name(kwonly_names, kw, 0)
                if kwonly_index is not __pythonj_null__:
                    bound_args[positional_names_len + kwonly_index] = value
                elif unknown_kw is None:
                    unknown_kw = kw

    if args_len > max_positional:
        if max_positional == 0:
            raise TypeError(f'{positional_name}() takes no positional arguments')
        raise TypeError(f'{positional_name}() takes at most {max_positional} positional arguments ({args_len} given)')

    if not min_positional_style:
        i = 0
        while i < min_args:
            if bound_args[i] is __pythonj_null__:
                raise TypeError(f'{positional_name}() missing required argument {positional_names[i]!r} (pos {i + 1})')
            i += 1

    if unknown_kw is not None:
        raise _type_error_unexpected_kw_arg(kw_name, unknown_kw)

    return bound_args

def bind_varargs_and_kwonly(kwargs, kw_name, kwonly_names) -> list:
    bound_args = [__pythonj_null__] * len(kwonly_names)

    if kwargs is not __pythonj_null__ and kwargs:
        for (kw, value) in kwargs.items():
            kwonly_index = _find_name(kwonly_names, kw, 0)
            if kwonly_index is not __pythonj_null__:
                bound_args[kwonly_index] = value
            else:
                raise _type_error_unexpected_kw_arg(kw_name, kw)

    return bound_args

def bind_user_exact_positional(args, kwargs, name, arg_names) -> tuple:
    args_len = len(args)
    n_args = len(arg_names)
    posonly_kw_arg_names: list

    if kwargs is not __pythonj_null__ and kwargs:
        posonly_kw_arg_names = []
        unknown_kw = None
        for kw in kwargs:
            if _find_name(arg_names, kw, 0) is not __pythonj_null__:
                posonly_kw_arg_names.append(kw)
            elif unknown_kw is None:
                unknown_kw = kw
        if posonly_kw_arg_names:
            raise _type_error_user_posonly_as_keyword(name, posonly_kw_arg_names)
        if unknown_kw is not None:
            raise _type_error_unexpected_kw_arg(name, unknown_kw)

    if args_len != n_args:
        if args_len > n_args:
            arg_s = '' if n_args == 1 else 's'
            was_were = 'was' if args_len == 1 else 'were'
            raise TypeError(f'{name}() takes {n_args} positional argument{arg_s} but {args_len} {was_were} given')
        raise _type_error_user_missing_args(name, arg_names[args_len:n_args])
    return args

def bind_user_min_max_positional(args, kwargs, name, arg_names, n_required) -> tuple:
    args_len = len(args)
    n_args = len(arg_names)
    posonly_kw_arg_names: list

    if kwargs is not __pythonj_null__ and kwargs:
        posonly_kw_arg_names = []
        unknown_kw = None
        for kw in kwargs:
            if _find_name(arg_names, kw, 0) is not __pythonj_null__:
                posonly_kw_arg_names.append(kw)
            elif unknown_kw is None:
                unknown_kw = kw
        if posonly_kw_arg_names:
            raise _type_error_user_posonly_as_keyword(name, posonly_kw_arg_names)
        if unknown_kw is not None:
            raise _type_error_unexpected_kw_arg(name, unknown_kw)

    if args_len < n_required:
        raise _type_error_user_missing_args(name, arg_names[args_len:n_required])
    if args_len > n_args:
        was_were = 'was' if args_len == 1 else 'were'
        raise TypeError(f'{name}() takes from {n_required} to {n_args} positional arguments but {args_len} {was_were} given')
    return args

def bind_user_function(args, kwargs, name, arg_names, n_required, posonly_count) -> list:
    args_len = len(args)
    n_args = len(arg_names)
    missing_arg_names: list
    bound_args = _init_bound_args(args, n_args)

    if kwargs is __pythonj_null__ or not kwargs:
        if n_required == n_args:
            if args_len > n_args:
                arg_s = '' if n_args == 1 else 's'
                was_were = 'was' if args_len == 1 else 'were'
                raise TypeError(f'{name}() takes {n_args} positional argument{arg_s} but {args_len} {was_were} given')
            if args_len != n_args:
                raise _type_error_user_missing_args(name, arg_names[args_len:n_args])
        else:
            if args_len < n_required:
                raise _type_error_user_missing_args(name, arg_names[args_len:n_required])
            if args_len > n_args:
                was_were = 'was' if args_len == 1 else 'were'
                raise TypeError(f'{name}() takes from {n_required} to {n_args} positional arguments but {args_len} {was_were} given')
        return bound_args

    posonly_kw_arg_names: list
    posonly_kw_arg_names = []
    unknown_kw = None
    for (kw, value) in kwargs.items():
        matched_index = _find_name(arg_names, kw, posonly_count)
        if matched_index is __pythonj_null__:
            posonly_index = _find_name(arg_names, kw, 0)
            if posonly_index is not __pythonj_null__:
                posonly_kw_arg_names.append(kw)
            elif unknown_kw is None:
                unknown_kw = kw
            continue
        if bound_args[matched_index] is not __pythonj_null__:
            raise TypeError(f'{name}() got multiple values for argument {arg_names[matched_index]!r}')
        bound_args[matched_index] = value

    if posonly_kw_arg_names:
        raise _type_error_user_posonly_as_keyword(name, posonly_kw_arg_names)
    if unknown_kw is not None:
        raise _type_error_unexpected_kw_arg(name, unknown_kw)

    if n_args != 0:
        if args_len > n_args:
            was_were = 'was' if args_len == 1 else 'were'
            raise TypeError(f'{name}() takes from {n_required} to {n_args} positional arguments but {args_len} {was_were} given')
        if n_required != 0:
            missing_arg_names = []
            i = 0
            while i < n_required:
                if bound_args[i] is __pythonj_null__:
                    missing_arg_names.append(arg_names[i])
                i += 1
            if missing_arg_names:
                raise _type_error_user_missing_args(name, missing_arg_names)

    return bound_args

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

def _pyj_format_parse_common(spec) -> tuple:
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

def _pyj_unknown_format_code(type_char, type_name) -> ValueError:
    return ValueError(f'Unknown format code {type_char!r} for object of type {type_name!r}')

def _pyj_invalid_format_spec(spec, type_name) -> ValueError:
    return ValueError(f'Invalid format specifier {spec!r} for object of type {type_name!r}')

def _pyj_format_split_sign(text) -> tuple:
    if text and text[0] in '+- ':
        return (text[0], text[1:])
    return ('', text)

def _pyj_format_apply_width(text, fill, align, width, default_align) -> str:
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

def _pyj_format_group_digits(digits, grouping, group_size) -> str:
    if grouping is None:
        return digits
    parts: list
    parts = []
    i = len(digits)
    while i > group_size:
        parts.append(digits[i - group_size:i])
        i -= group_size
    parts.append(digits[:i])
    parts.reverse()
    return grouping.join(parts)

def _pyj_format_zero_fill_grouped(sign, prefix, digits, grouping, group_size, suffix, width) -> str:
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

def _pyj_int_base_digits(value, base, alphabet) -> str:
    if value == 0:
        return '0'
    ret: list
    ret = []
    while value:
        ret.append(alphabet[value % base])
        value //= base
    ret.reverse()
    return ''.join(ret)

def pyj_float_parse_spec(spec) -> tuple:
    fill, align, sign, z, alt, zero, width, grouping, precision, type_char, i, n = _pyj_format_parse_common(spec)

    if type_char and type_char not in 'eEfFgGn%':
        if i != n:
            raise _pyj_invalid_format_spec(spec, 'float')
        raise _pyj_unknown_format_code(type_char, 'float')

    if i != n:
        raise _pyj_invalid_format_spec(spec, 'float')

    if zero and align is None:
        fill = '0'
        align = '='

    return (fill, align, sign, z, alt, width, grouping, precision, type_char)

def pyj_int_parse_spec(spec) -> tuple:
    return _pyj_int_like_parse_spec(spec, 'int')

def _pyj_int_like_parse_spec(spec, type_name) -> tuple:
    fill, align, sign, z, alt, zero, width, grouping, precision, type_char, i, n = _pyj_format_parse_common(spec)

    if z:
        raise ValueError('Negative zero coercion (z) not allowed in integer format specifier')
    if precision is not None:
        raise ValueError('Precision not allowed in integer format specifier')

    if type_char and type_char not in 'boxXdn':
        if i != n:
            raise _pyj_invalid_format_spec(spec, type_name)
        raise _pyj_unknown_format_code(type_char, type_name)

    if i != n:
        raise _pyj_invalid_format_spec(spec, type_name)

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

def pyj_str_parse_spec(spec) -> tuple:
    fill, align, sign, z, alt, zero, width, grouping, precision, type_char, i, n = _pyj_format_parse_common(spec)

    if sign != '-':
        raise ValueError('Sign not allowed in string format specifier')
    if z:
        raise _pyj_invalid_format_spec(spec, 'str')
    if alt:
        raise ValueError('Alternate form (#) not allowed in string format specifier')
    if grouping is not None:
        raise ValueError('Cannot specify grouping in string format specifier')
    if align == '=':
        raise ValueError("'=' alignment not allowed in string format specifier")

    if type_char and type_char != 's':
        if i != n:
            raise _pyj_invalid_format_spec(spec, 'str')
        raise _pyj_unknown_format_code(type_char, 'str')

    if i != n:
        raise _pyj_invalid_format_spec(spec, 'str')

    if zero and align is None:
        fill = '0'
        align = '<'

    return (fill, align, width, precision)

def pyj_float_special_text(value, type_char):
    import math
    if math.isfinite(value):
        return None
    if math.isnan(value):
        text = 'nan'
    else:
        text = 'inf'
    if type_char in ('E', 'F', 'G'):
        text = text.upper()
    if type_char == '%':
        text += '%'
    return text

def _pyj_float_is_zero_result(magnitude_text) -> bool:
    for c in magnitude_text:
        if c in '0.,_%':
            continue
        if c in 'eE':
            continue
        if c in '+-':
            continue
        return False
    return True

def _pyj_float_sign_prefix(value, sign, z, magnitude_text) -> str:
    import math
    if not math.isfinite(value):
        if not math.isinf(value):
            if sign == '+':
                return '+'
            if sign == ' ':
                return ' '
            return ''
        if math.copysign(1.0, value) < 0.0:
            return '-'
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

def _pyj_float_apply_zero_fill(text, grouping, width) -> str:
    magnitude: str
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

def pyj_float_finish_text(fill, align, sign, z, width, grouping, value, magnitude_text) -> str:
    text = _pyj_float_sign_prefix(value, sign, z, magnitude_text) + magnitude_text
    if align == '=' and fill == '0' and width is not None:
        return _pyj_float_apply_zero_fill(text, grouping, width)
    return _pyj_format_apply_width(text, fill, align, width, '>')

def pyj_int_format(value, spec) -> str:
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

def pyj_bool_format(value, spec) -> str:
    if spec == '':
        return 'True' if value else 'False'
    _pyj_int_like_parse_spec(spec, 'bool')
    return pyj_int_format(1 if value else 0, spec)

def pyj_str_format(value, spec) -> str:
    fill, align, width, precision = pyj_str_parse_spec(spec)
    text = value if precision is None else value[:precision]
    return _pyj_format_apply_width(text, fill, align, width, '<')

def _pyj_percent_arg_seq(args) -> tuple:
    if __pythonj_isinstance__(args, tuple):
        return args
    return (args,)

def _pyj_percent_real_arg(arg, conv) -> float:
    if __pythonj_isinstance__(arg, (bool, int, float)):
        return float(arg)
    raise TypeError(f'must be real number, not {type(arg).__name__}')

def _pyj_percent_signed_int_arg(arg, conv) -> int:
    if __pythonj_isinstance__(arg, (bool, int, float)):
        return int(arg)
    raise TypeError(f'%{conv} format: a real number is required, not {type(arg).__name__}')

def _pyj_percent_index_arg(arg, conv) -> int:
    if __pythonj_isinstance__(arg, (int, bool)):
        return int(arg)
    raise TypeError(f'%{conv} format: an integer is required, not {type(arg).__name__}')

def _pyj_percent_apply_width(text, flags, width) -> str:
    if width is None:
        return text
    fill = ' '
    if ('0' in flags) and ('-' not in flags):
        fill = '0'
    align = '<' if '-' in flags else '>'
    return _pyj_format_apply_width(text, fill, align, width, '>')

def _pyj_percent_without_zero_flag(flags) -> str:
    ret: list
    ret = []
    for c in flags:
        if c != '0':
            ret.append(c)
    return ''.join(ret)

def _pyj_percent_int_text(value, flags, width, precision, conv) -> str:
    is_negative = value < 0
    abs_value = -value if is_negative else value
    sign_prefix = '-'
    if not is_negative:
        sign_prefix = ''
        if '+' in flags:
            sign_prefix = '+'
        elif ' ' in flags:
            sign_prefix = ' '

    prefix = ''
    if conv in 'diu':
        digits = str(abs_value)
    elif conv == 'o':
        digits = _pyj_int_base_digits(abs_value, 8, '01234567')
        if '#' in flags:
            prefix = '0o'
    elif conv == 'x':
        digits = _pyj_int_base_digits(abs_value, 16, '0123456789abcdef')
        if '#' in flags:
            prefix = '0x'
    elif conv == 'X':
        digits = _pyj_int_base_digits(abs_value, 16, '0123456789ABCDEF')
        if '#' in flags:
            prefix = '0X'
    else:
        assert False, conv

    if precision is not None and len(digits) < precision:
        digits = ('0' * (precision - len(digits))) + digits

    text = sign_prefix + prefix + digits
    if width is None or len(text) >= width:
        return text

    if ('0' in flags) and ('-' not in flags):
        return sign_prefix + prefix + ('0' * (width - len(text))) + digits
    return _pyj_percent_apply_width(text, flags, width)

def _pyj_percent_float_text(flags, width, precision, conv, arg) -> str:
    spec = ''
    if '-' in flags:
        spec += '<'
    if '+' in flags:
        spec += '+'
    elif ' ' in flags:
        spec += ' '
    if '#' in flags:
        spec += '#'
    if ('0' in flags) and ('-' not in flags) and (width is not None):
        spec += '0' + str(width)
    elif width is not None:
        spec += str(width)
    if precision is not None:
        spec += '.' + str(precision)
    spec += conv
    return format(_pyj_percent_real_arg(arg, conv), spec)

def _pyj_percent_char_text(arg) -> str:
    if __pythonj_isinstance__(arg, str):
        if len(arg) != 1:
            raise TypeError(f'%c requires an int or a unicode character, not a string of length {len(arg)}')
        return arg
    if not __pythonj_isinstance__(arg, (int, bool)):
        raise TypeError(f'%c requires an int or a unicode character, not {type(arg).__name__}')
    code = int(arg)
    if code < 0 or code > 0x10FFFF:
        raise OverflowError('%c arg not in range(0x110000)')
    return chr(code)

def _pyj_percent_item_text(conv, flags, width, precision, arg) -> str:
    if conv == 's':
        text = str(arg)
        if precision is not None:
            text = text[:precision]
        return _pyj_percent_apply_width(text, _pyj_percent_without_zero_flag(flags), width)
    if conv == 'r':
        text = repr(arg)
        if precision is not None:
            text = text[:precision]
        return _pyj_percent_apply_width(text, _pyj_percent_without_zero_flag(flags), width)
    if conv == 'a':
        text = ascii(arg)
        if precision is not None:
            text = text[:precision]
        return _pyj_percent_apply_width(text, _pyj_percent_without_zero_flag(flags), width)
    if conv == 'c':
        return _pyj_percent_apply_width(_pyj_percent_char_text(arg), _pyj_percent_without_zero_flag(flags), width)
    if conv in 'diu':
        return _pyj_percent_int_text(_pyj_percent_signed_int_arg(arg, conv), flags, width, precision, conv)
    if conv in 'oxX':
        return _pyj_percent_int_text(_pyj_percent_index_arg(arg, conv), flags, width, precision, conv)
    if conv in 'eEfFgG':
        if precision is None and conv in 'eEfF':
            precision = 6
        return _pyj_percent_float_text(flags, width, precision, conv, arg)
    raise ValueError(conv)

def _pyj_percent_take_star_value(arg_seq, arg_index) -> tuple:
    if arg_index >= len(arg_seq):
        raise TypeError('not enough arguments for format string')
    value = arg_seq[arg_index]
    arg_index += 1
    if not __pythonj_isinstance__(value, (int, bool)):
        raise TypeError('* wants int')
    return (int(value), arg_index)

def pyj_percent_format(fmt, args) -> str:
    arg_seq = _pyj_percent_arg_seq(args)
    arg_index = 0
    used_mapping = False
    out: list
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
            if not hasattr(args, 'keys'):
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

        width = None
        if i < n and fmt[i] == '*':
            width, arg_index = _pyj_percent_take_star_value(arg_seq, arg_index)
            i += 1
            if width < 0:
                width = -width
                if '-' not in flags:
                    flags += '-'
        else:
            width_start = i
            while i < n and fmt[i].isdecimal():
                i += 1
            width = int(fmt[width_start:i]) if i > width_start else None

        precision = None
        if i < n and fmt[i] == '.':
            i += 1
            if i < n and fmt[i] == '*':
                precision, arg_index = _pyj_percent_take_star_value(arg_seq, arg_index)
                i += 1
            else:
                precision_start = i
                while i < n and fmt[i].isdecimal():
                    i += 1
                if i == precision_start:
                    precision = 0
                else:
                    precision = int(fmt[precision_start:i])
            if precision < 0:
                precision = 0

        if i < n and fmt[i] in 'hlL':
            i += 1

        if i == n:
            raise ValueError('incomplete format')
        conv = fmt[i]
        i += 1
        if conv not in 'diuoxXeEfFgGcrsa':
            raise ValueError(f"unsupported format character {conv!r} (0x{ord(conv):x}) at index {i - 1}")

        if mapping_key is None:
            if arg_index >= len(arg_seq):
                raise TypeError('not enough arguments for format string')
            arg = arg_seq[arg_index]
            arg_index += 1
        else:
            arg = args[mapping_key]
        out.append(_pyj_percent_item_text(conv, flags, width, precision, arg))

    if (not used_mapping) and (arg_index != len(arg_seq)):
        raise TypeError('not all arguments converted during string formatting')
    return ''.join(out)
