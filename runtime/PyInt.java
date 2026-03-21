// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

final class PyIntType extends PyBuiltinType {
// BEGIN GENERATED CODE: PyIntType
    public static final PyIntType singleton = new PyIntType();
    private static final PyMethodDescriptor pydesc_conjugate = new PyMethodDescriptor(singleton, "conjugate", obj -> new PyInt.PyIntMethodUnimplemented(obj, "conjugate"));
    private static final PyMethodDescriptor pydesc_bit_length = new PyMethodDescriptor(singleton, "bit_length", obj -> new PyInt.PyIntMethodUnimplemented(obj, "bit_length"));
    private static final PyMethodDescriptor pydesc_bit_count = new PyMethodDescriptor(singleton, "bit_count", obj -> new PyInt.PyIntMethodUnimplemented(obj, "bit_count"));
    private static final PyMethodDescriptor pydesc_to_bytes = new PyMethodDescriptor(singleton, "to_bytes", obj -> new PyInt.PyIntMethodUnimplemented(obj, "to_bytes"));
    private static final PyClassMethodDescriptor pydesc_from_bytes = new PyClassMethodDescriptor(singleton, "from_bytes", PyIntType.PyIntClassMethod_from_bytes::new);
    private static final PyMethodDescriptor pydesc_as_integer_ratio = new PyMethodDescriptor(singleton, "as_integer_ratio", obj -> new PyInt.PyIntMethodUnimplemented(obj, "as_integer_ratio"));
    private static final PyMethodDescriptor pydesc_is_integer = new PyMethodDescriptor(singleton, "is_integer", obj -> new PyInt.PyIntMethodUnimplemented(obj, "is_integer"));
    private static final PyGetSetDescriptor pydesc_real = new PyGetSetDescriptor(singleton, "real", PyInt::pygetset_real);
    private static final PyGetSetDescriptor pydesc_imag = new PyGetSetDescriptor(singleton, "imag", PyInt::pygetset_imag);
    private static final PyGetSetDescriptor pydesc_numerator = new PyGetSetDescriptor(singleton, "numerator", PyInt::pygetset_numerator);
    private static final PyGetSetDescriptor pydesc_denominator = new PyGetSetDescriptor(singleton, "denominator", PyInt::pygetset_denominator);
    private static final PyString pydesc___doc__ = new PyString("int([x]) -> integer\nint(x, base=10) -> integer\n\nConvert a number or string to an integer, or return 0 if no arguments\nare given.  If x is a number, return x.__int__().  For floating-point\nnumbers, this truncates towards zero.\n\nIf x is not a number or if base is given, then x must be a string,\nbytes, or bytearray instance representing an integer literal in the\ngiven base.  The literal can be preceded by '+' or '-' and be surrounded\nby whitespace.  The base defaults to 10.  Valid bases are 0 and 2-36.\nBase 0 means to interpret the base from the string as an integer literal.\n>>> int('0b100', base=0)\n4");
    private static final PyAttr attrs[] = new PyAttr[] {
        new PyAttr("conjugate", pydesc_conjugate),
        new PyAttr("bit_length", pydesc_bit_length),
        new PyAttr("bit_count", pydesc_bit_count),
        new PyAttr("to_bytes", pydesc_to_bytes),
        new PyAttr("from_bytes", pydesc_from_bytes),
        new PyAttr("as_integer_ratio", pydesc_as_integer_ratio),
        new PyAttr("is_integer", pydesc_is_integer),
        new PyAttr("real", pydesc_real),
        new PyAttr("imag", pydesc_imag),
        new PyAttr("numerator", pydesc_numerator),
        new PyAttr("denominator", pydesc_denominator),
        new PyAttr("__doc__", pydesc___doc__)
    };
    @Override public PyAttr[] getAttributes() { return attrs; }
    @Override public PyDescriptor getDescriptor(String name) {
        switch (name) {
            case "conjugate": return pydesc_conjugate;
            case "bit_length": return pydesc_bit_length;
            case "bit_count": return pydesc_bit_count;
            case "to_bytes": return pydesc_to_bytes;
            case "from_bytes": return pydesc_from_bytes;
            case "as_integer_ratio": return pydesc_as_integer_ratio;
            case "is_integer": return pydesc_is_integer;
            case "real": return pydesc_real;
            case "imag": return pydesc_imag;
            case "numerator": return pydesc_numerator;
            case "denominator": return pydesc_denominator;
            default: return null;
        }
    }
// END GENERATED CODE: PyIntType

    private PyIntType() { super("int", PyInt.class); }
    @Override public PyInt call(PyObject[] args, PyDict kwargs) {
        Runtime.requireMaxArgs(args, 2, typeName);
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("int() does not accept kwargs");
        }
        if (args.length == 0) {
            return PyInt.singleton_0;
        }
        PyObject arg0 = args[0];
        // XXX should always call intValue when length is 1
        if (arg0 instanceof PyInt arg0_int) {
            if (args.length > 1) {
                throw new IllegalArgumentException("int() cannot accept a base when passed an int");
            }
            return arg0_int;
        }
        if (arg0 instanceof PyBool) {
            if (args.length > 1) {
                throw new IllegalArgumentException("int() cannot accept a base when passed a bool");
            }
            return new PyInt(arg0.intValue());
        }
        if (arg0 instanceof PyString arg0_str) {
            long base = 10;
            if (args.length > 1) {
                PyObject arg1 = args[1];
                if (arg1.hasIndex()) {
                    base = arg1.indexValue();
                } else {
                    throw new IllegalArgumentException("base must be an int");
                }
                if ((base < 0) || (base == 1) || (base > 36)) {
                    throw new IllegalArgumentException("base must be 0 or 2-36");
                }
                if (base == 0) {
                    throw new UnsupportedOperationException("base 0 unsupported at present");
                }
            }
            String s = arg0_str.value;
            int i = 0;
            while (s.charAt(i) == ' ') {
                i++;
            }
            long sign = 1;
            if (s.charAt(i) == '-') {
                i++;
                sign = -1;
            } else if (s.charAt(i) == '+') {
                i++;
            }
            long value = 0;
            while (i < s.length()) {
                long digit;
                char c = s.charAt(i++);
                if ((c >= '0') && (c <= '9')) {
                    digit = c - '0';
                } else if ((c >= 'a') && (c <= 'z')) {
                    digit = c - 'a' + 10;
                } else if ((c >= 'A') && (c <= 'Z')) {
                    digit = c - 'A' + 10;
                } else {
                    throw new IllegalArgumentException("unexpected digit");
                }
                if (digit >= base) {
                    throw new IllegalArgumentException("digit not valid in base");
                }
                value = value*base + digit;
            }
            return new PyInt(sign*value);
        }
        throw new UnsupportedOperationException("don't know how to handle argument to int()");
    }
    protected static final class PyIntClassMethod_from_bytes extends PyBuiltinMethod<PyType> {
        PyIntClassMethod_from_bytes(PyType self) { super(self); }
        @Override public String methodName() { return "from_bytes"; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("int.from_bytes() unimplemented");
        }
    }
}

// XXX Should probably pre-intern [-5,256] to match CPython
public final class PyInt extends PyObject {
    public static final PyInt singleton_neg1 = new PyInt(-1);
    public static final PyInt singleton_0 = new PyInt(0);
    public static final PyInt singleton_1 = new PyInt(1);

// BEGIN GENERATED CODE: PyInt
    protected static final class PyIntMethodUnimplemented extends PyBuiltinMethod<PyInt> {
        private final String name;
        PyIntMethodUnimplemented(PyObject _self, String _name) { super((PyInt)_self); name = _name; }
        @Override public String methodName() { return name; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("int." + name + "() unimplemented");
        }
    }
// END GENERATED CODE: PyInt

    public final long value;

    PyInt(long _value) { value = _value; }

    public static long floorDiv(long lhs, long rhs) {
        if ((lhs == Long.MIN_VALUE) && (rhs == -1)) {
            throw new ArithmeticException("integer overflow");
        }
        if (rhs == 0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        return Math.floorDiv(lhs, rhs);
    }
    public static long lshift(long lhs, long rhs) {
        if (rhs < 0) {
            throw PyValueError.raise("negative shift count");
        }
        if (rhs >= 64) {
            if (lhs == 0) {
                return 0; // 0 << N -> 0 for any N >= 0
            }
            throw new ArithmeticException("shift count too large");
        }
        long ret = lhs << rhs;
        if ((ret >> rhs) != lhs) {
            throw new ArithmeticException("integer overflow");
        }
        return ret;
    }
    public static long mod(long lhs, long rhs) {
        if ((lhs == Long.MIN_VALUE) && (rhs == -1)) {
            throw new ArithmeticException("integer overflow");
        }
        if (rhs == 0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        return Math.floorMod(lhs, rhs);
    }
    public static long pow(long lhs, long rhs) {
        if (rhs < 0) {
            throw new ArithmeticException("negative exponent");
        }
        long ret = 1; // note 0 ** 0 -> 1
        while (rhs > 0) {
            if ((rhs & 1) == 1) {
                ret = Math.multiplyExact(ret, lhs);
            }
            lhs = Math.multiplyExact(lhs, lhs);
            rhs >>= 1;
        }
        return ret;
    }
    public static long rshift(long lhs, long rhs) {
        if (rhs < 0) {
            throw PyValueError.raise("negative shift count");
        }
        if (rhs > 63) {
            rhs = 63; // for all longs, rshift of >=63 yields the sign bit replicated into all bits
        }
        return lhs >> rhs;
    }

    // XXX Make sure that all of these throw an exception if the value wraps/overflows/etc.
    @Override public PyInt invert() { return new PyInt(~value); }
    @Override public PyInt pos() { return this; }
    @Override public PyInt neg() { return new PyInt(Math.negateExact(value)); }
    @Override public PyInt abs() { return (value >= 0) ? this : new PyInt(Math.negateExact(value)); }

    @Override public PyObject add(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(Math.addExact(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(Math.addExact(value, rhsBool.asInt()));
        } else {
            return super.add(rhs);
        }
    }
    @Override public PyObject and(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(value & rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(value & rhsBool.asInt());
        } else {
            return super.and(rhs);
        }
    }
    @Override public PyObject floorDiv(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(floorDiv(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(floorDiv(value, rhsBool.asInt()));
        } else {
            return super.floorDiv(rhs);
        }
    }
    @Override public PyObject lshift(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(lshift(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(lshift(value, rhsBool.asInt()));
        } else {
            return super.lshift(rhs);
        }
    }
    @Override public PyObject mod(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(mod(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(mod(value, rhsBool.asInt()));
        } else {
            return super.mod(rhs);
        }
    }
    @Override public PyObject mul(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(Math.multiplyExact(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(value * rhsBool.asInt());
        } else {
            return super.mul(rhs);
        }
    }
    @Override public PyObject or(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(value | rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(value | rhsBool.asInt());
        } else {
            return super.or(rhs);
        }
    }
    @Override public PyObject pow(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(pow(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(pow(value, rhsBool.asInt()));
        } else {
            return super.pow(rhs);
        }
    }
    @Override public PyObject rshift(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(rshift(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(rshift(value, rhsBool.asInt()));
        } else {
            return super.rshift(rhs);
        }
    }
    @Override public PyObject sub(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(Math.subtractExact(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(Math.subtractExact(value, rhsBool.asInt()));
        } else {
            return super.sub(rhs);
        }
    }
    @Override public PyObject trueDiv(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            throw unimplementedMethod("trueDiv");
        } else {
            return super.trueDiv(rhs);
        }
    }
    @Override public PyObject xor(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(value ^ rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(value ^ rhsBool.asInt());
        } else {
            return super.xor(rhs);
        }
    }

    @Override public boolean ge(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value >= rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value >= rhsBool.asInt();
        } else {
            return super.ge(rhs);
        }
    }
    @Override public boolean gt(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value > rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value > rhsBool.asInt();
        } else {
            return super.gt(rhs);
        }
    }
    @Override public boolean le(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value <= rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value <= rhsBool.asInt();
        } else {
            return super.le(rhs);
        }
    }
    @Override public boolean lt(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value < rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value < rhsBool.asInt();
        } else {
            return super.lt(rhs);
        }
    }

    @Override public PyBuiltinType type() { return PyIntType.singleton; }

    @Override public boolean boolValue() { return value != 0; }
    @Override public boolean contains(PyObject rhs) { return defaultContains(rhs); }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value == rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value == rhsBool.asInt();
        } else {
            return false;
        }
    }
    @Override public double floatValue() { return value; }
    @Override public String format(String formatSpec) {
        if (formatSpec.isEmpty()) {
            return String.valueOf(value);
        } else if (formatSpec.equals("x")) {
            if (value < 0) {
                return String.format("-%x", Math.negateExact(value));
            } else {
                return String.format("%x", value);
            }
        } else {
            throw new UnsupportedOperationException(String.format("formatSpec=%s unimplemented", PyString.reprOf(formatSpec)));
        }
    }
    @Override public int hashCode() { return (int)(value ^ (value >>> 32)); }
    @Override public boolean hasIndex() { return true; }
    @Override public long indexValue() { return value; }
    @Override public long intValue() { return value; }
    @Override public String repr() { return String.valueOf(value); }

    static PyObject pygetset_denominator(PyObject obj) { return PyInt.singleton_1; }
    static PyObject pygetset_imag(PyObject obj) { return PyInt.singleton_0; }
    static PyObject pygetset_numerator(PyObject obj) { return obj; }
    static PyObject pygetset_real(PyObject obj) { return obj; }
}
