// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

// XXX Should probably pre-intern [-5,256] to match CPython
public final class PyInt extends PyObject {
    public static final PyInt singleton_neg1 = new PyInt(-1);
    public static final PyInt singleton_0 = new PyInt(0);
    public static final PyInt singleton_1 = new PyInt(1);

    protected static final class PyIntMethod_as_integer_ratio extends PyBuiltinMethod<PyInt> {
        PyIntMethod_as_integer_ratio(PyObject _self) { super((PyInt)_self); }
        @Override public String methodName() { return "as_integer_ratio"; }
        @Override public PyTuple call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "int.as_integer_ratio");
            Runtime.requireExactArgsAlt(args, 0, "int.as_integer_ratio");
            return self.pymethod_as_integer_ratio();
        }
    }
    protected static final class PyIntMethod_bit_count extends PyBuiltinMethod<PyInt> {
        PyIntMethod_bit_count(PyObject _self) { super((PyInt)_self); }
        @Override public String methodName() { return "bit_count"; }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "int.bit_count");
            Runtime.requireExactArgsAlt(args, 0, "int.bit_count");
            return self.pymethod_bit_count();
        }
    }
    protected static final class PyIntMethod_bit_length extends PyBuiltinMethod<PyInt> {
        PyIntMethod_bit_length(PyObject _self) { super((PyInt)_self); }
        @Override public String methodName() { return "bit_length"; }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "int.bit_length");
            Runtime.requireExactArgsAlt(args, 0, "int.bit_length");
            return self.pymethod_bit_length();
        }
    }
    protected static final class PyIntMethod_conjugate extends PyBuiltinMethod<PyInt> {
        PyIntMethod_conjugate(PyObject _self) { super((PyInt)_self); }
        @Override public String methodName() { return "conjugate"; }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "int.conjugate");
            Runtime.requireExactArgsAlt(args, 0, "int.conjugate");
            return self.pymethod_conjugate();
        }
    }
    protected static final class PyIntMethod_is_integer extends PyBuiltinMethod<PyInt> {
        PyIntMethod_is_integer(PyObject _self) { super((PyInt)_self); }
        @Override public String methodName() { return "is_integer"; }
        @Override public PyBool call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "int.is_integer");
            Runtime.requireExactArgsAlt(args, 0, "int.is_integer");
            return self.pymethod_is_integer();
        }
    }
    protected static final class PyIntMethod_to_bytes extends PyBuiltinMethod<PyInt> {
        PyIntMethod_to_bytes(PyObject _self) { super((PyInt)_self); }
        @Override public String methodName() { return "to_bytes"; }
        @Override public PyBytes call(PyObject[] args, PyDict kwargs) {
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("int.to_bytes() does not accept kwargs");
            }
            if (args.length > 3) {
                throw PyTypeError.raiseFormat("to_bytes() takes at most 3 arguments (%d given)", args.length);
            } else if (args.length > 2) {
                throw PyTypeError.raiseFormat("to_bytes() takes at most 2 positional arguments (%d given)", args.length);
            }
            PyObject length = (args.length >= 1) ? args[0] : PyInt.singleton_1;
            PyObject byteorder = (args.length >= 2) ? args[1] : null;
            return self.pymethod_to_bytes(length, byteorder);
        }
    }

    public final long value;

    PyInt(long _value) { value = _value; }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireMaxArgs(args, 2, type.name());
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

    public PyTuple pymethod_as_integer_ratio() {
        return new PyTuple(new PyObject[]{this, PyInt.singleton_1});
    }
    public PyInt pymethod_bit_count() {
        if (value == Long.MIN_VALUE) {
            return PyInt.singleton_1;
        }
        return new PyInt(Long.bitCount(Math.abs(value)));
    }
    public PyInt pymethod_bit_length() {
        if (value == Long.MIN_VALUE) {
            return new PyInt(64);
        }
        return new PyInt(64 - Long.numberOfLeadingZeros(Math.abs(value)));
    }
    public PyInt pymethod_conjugate() {
        return this;
    }
    public PyBool pymethod_is_integer() {
        return PyBool.true_singleton;
    }
    public PyBytes pymethod_to_bytes(PyObject length, PyObject byteorder) {
        if (!length.hasIndex()) {
            throw PyTypeError.raise(PyString.reprOf(length.type().name()) + " object cannot be interpreted as an integer");
        }
        int len = Math.toIntExact(length.indexValue());
        boolean littleEndian = false;
        if (byteorder != null) {
            if (!(byteorder instanceof PyString byteorderStr)) {
                throw PyTypeError.raise("to_bytes() argument 'byteorder' must be str, not " + byteorder.type().name());
            }
            String s = byteorderStr.value;
            if (s.equals("big")) {
                littleEndian = false;
            } else if (s.equals("little")) {
                littleEndian = true;
            } else {
                throw PyValueError.raise("byteorder must be either 'little' or 'big'");
            }
        }
        if (len < 0) {
            throw PyValueError.raise("length argument must be non-negative");
        }
        long v = value;
        if (v < 0) {
            throw PyOverflowError.raise("can't convert negative int to unsigned");
        }
        if (len < 8) { // len >= 8 is always representible
            long max = (1L << (len * 8)) - 1;
            if (v > max) {
                throw PyOverflowError.raise("int too big to convert");
            }
        }
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            int idx = littleEndian ? i : (len - 1 - i);
            out[idx] = (byte)(v & 0xFF);
            v >>= 8;
        }
        return new PyBytes(out);
    }
}

final class PyIntClassMethod_from_bytes extends PyBuiltinMethod<PyType> {
    PyIntClassMethod_from_bytes(PyType self) { super(self); }
    @Override public String methodName() { return "from_bytes"; }
    @Override public PyObject call(PyObject[] args, PyDict kwargs) {
        throw new UnsupportedOperationException("int.from_bytes() unimplemented");
    }
}
