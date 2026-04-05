// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.io.ByteArrayOutputStream;

// XXX Should probably pre-intern [-5,256] to match CPython
public final class PyInt extends PyObject {
    public static final PyInt singleton_neg1 = new PyInt(-1);
    public static final PyInt singleton_0 = new PyInt(0);
    public static final PyInt singleton_1 = new PyInt(1);

    public final long value;

    PyInt(long _value) { value = _value; }

    private static PyInt parseStringLike(String s, String repr, long baseObj) {
        long base = baseObj;
        int i = 0;
        int end = s.length();
        while ((i < end) && (s.charAt(i) == ' ')) {
            i++;
        }
        while ((end > i) && (s.charAt(end - 1) == ' ')) {
            end--;
        }
        long sign = 1;
        if (i < end) {
            if (s.charAt(i) == '-') {
                i++;
                sign = -1;
            } else if (s.charAt(i) == '+') {
                i++;
            }
        }
        if (i == end) {
            throw PyValueError.raiseFormat("invalid literal for int() with base %d: %s", base, repr);
        }
        if ((end - i) >= 2 && (s.charAt(i) == '0')) {
            char prefix = s.charAt(i + 1);
            if ((prefix == 'x') || (prefix == 'X')) {
                if ((base == 0) || (base == 16)) {
                    base = 16;
                    i += 2;
                }
            } else if ((prefix == 'o') || (prefix == 'O')) {
                if ((base == 0) || (base == 8)) {
                    base = 8;
                    i += 2;
                }
            } else if ((prefix == 'b') || (prefix == 'B')) {
                if ((base == 0) || (base == 2)) {
                    base = 2;
                    i += 2;
                }
            }
        }
        if (base == 0) {
            base = 10;
            if ((i < end) && (s.charAt(i) == '0') && ((end - i) > 1)) {
                boolean allZero = true;
                for (int j = i + 1; j < end; j++) {
                    if (s.charAt(j) != '0') {
                        allZero = false;
                        break;
                    }
                }
                if (!allZero) {
                    throw PyValueError.raiseFormat("invalid literal for int() with base %d: %s", baseObj, repr);
                }
            }
        }
        if (i == end) {
            throw PyValueError.raiseFormat("invalid literal for int() with base %d: %s", base, repr);
        }
        long value = 0;
        while (i < end) {
            long digit;
            char c = s.charAt(i++);
            if ((c >= '0') && (c <= '9')) {
                digit = c - '0';
            } else if ((c >= 'a') && (c <= 'z')) {
                digit = c - 'a' + 10;
            } else if ((c >= 'A') && (c <= 'Z')) {
                digit = c - 'A' + 10;
            } else {
                digit = 36;
            }
            if (digit >= base) {
                throw PyValueError.raiseFormat("invalid literal for int() with base %d: %s", base, repr);
            }
            value = Math.addExact(Math.multiplyExact(value, base), digit);
        }
        return new PyInt(Math.multiplyExact(sign, value));
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireMinMaxPositional(args, kwargs, type.name(), 0, 2);
        PyObject arg0 = (args.length >= 1) ? args[0] : null;
        PyObject arg1 = (args.length >= 2) ? args[1] : null;
        return newObjPositional(arg0, arg1);
    }
    public static PyObject newObjPositional(PyObject arg0, PyObject arg1) {
        if (arg0 == null) {
            return PyInt.singleton_0;
        }
        // XXX should also try intValue when length is 1
        if (arg0.hasIndex()) {
            if (arg1 != null) {
                throw PyTypeError.raise("int() can't convert non-string with explicit base");
            }
            return new PyInt(arg0.indexValue());
        }
        if (arg0 instanceof PyString arg0Str) {
            long base = 10;
            if (arg1 != null) {
                base = arg1.indexValue();
                if ((base < 0) || (base == 1) || (base > 36)) {
                    throw PyValueError.raise("int() base must be >= 2 and <= 36, or 0");
                }
            }
            return parseStringLike(arg0Str.value, PyString.reprOf(arg0Str.value), base);
        }
        if (arg0 instanceof PyBytes arg0Bytes) {
            long base = (arg1 != null) ? arg1.indexValue() : 10;
            if ((base < 0) || (base == 1) || (base > 36)) {
                throw PyValueError.raise("int() base must be >= 2 and <= 36, or 0");
            }
            return parseStringLike(new String(arg0Bytes.value), arg0.repr(), base);
        }
        if (arg0 instanceof PyByteArray arg0ByteArray) {
            long base = (arg1 != null) ? arg1.indexValue() : 10;
            if ((base < 0) || (base == 1) || (base > 36)) {
                throw PyValueError.raise("int() base must be >= 2 and <= 36, or 0");
            }
            return parseStringLike(new String(arg0ByteArray.value), arg0.repr(), base);
        }
        throw PyTypeError.raise("int() argument must be a string, a bytes-like object or a number, not " + PyString.reprOf(arg0.type().name()));
    }

    public static PyInt add(long lhs, long rhs) {
        return new PyInt(Math.addExact(lhs, rhs));
    }
    public static PyInt and(long lhs, long rhs) {
        return new PyInt(lhs & rhs);
    }
    public static PyInt floorDiv(long lhs, long rhs) {
        if ((lhs == Long.MIN_VALUE) && (rhs == -1)) {
            throw new ArithmeticException("integer overflow");
        }
        if (rhs == 0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        return new PyInt(Math.floorDiv(lhs, rhs));
    }
    public static PyInt lshift(long lhs, long rhs) {
        if (rhs < 0) {
            throw PyValueError.raise("negative shift count");
        }
        if (rhs >= 64) {
            if (lhs == 0) {
                return PyInt.singleton_0; // 0 << N -> 0 for any N >= 0
            }
            throw new ArithmeticException("shift count too large");
        }
        long ret = lhs << rhs;
        if ((ret >> rhs) != lhs) {
            throw new ArithmeticException("integer overflow");
        }
        return new PyInt(ret);
    }
    public static PyInt mod(long lhs, long rhs) {
        if ((lhs == Long.MIN_VALUE) && (rhs == -1)) {
            throw new ArithmeticException("integer overflow");
        }
        if (rhs == 0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        return new PyInt(Math.floorMod(lhs, rhs));
    }
    public static PyInt mul(long lhs, long rhs) {
        return new PyInt(Math.multiplyExact(lhs, rhs));
    }
    public static PyInt or(long lhs, long rhs) {
        return new PyInt(lhs | rhs);
    }
    public static PyInt pow(long lhs, long rhs) {
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
        return new PyInt(ret);
    }
    public static PyInt rshift(long lhs, long rhs) {
        if (rhs < 0) {
            throw PyValueError.raise("negative shift count");
        }
        if (rhs > 63) {
            rhs = 63; // for all longs, rshift of >=63 yields the sign bit replicated into all bits
        }
        return new PyInt(lhs >> rhs);
    }
    public static PyInt sub(long lhs, long rhs) {
        return new PyInt(Math.subtractExact(lhs, rhs));
    }
    public static PyInt xor(long lhs, long rhs) {
        return new PyInt(lhs ^ rhs);
    }

    // XXX Make sure that all of these throw an exception if the value wraps/overflows/etc.
    @Override public PyInt invert() { return new PyInt(~value); }
    @Override public PyInt pos() { return this; }
    @Override public PyInt neg() { return new PyInt(Math.negateExact(value)); }
    @Override public PyInt abs() { return (value >= 0) ? this : new PyInt(Math.negateExact(value)); }

    @Override public PyObject add(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return add(value, rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return add(value, rhsBool.asInt());
        } else {
            return super.add(rhs);
        }
    }
    @Override public PyObject and(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return and(value, rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return and(value, rhsBool.asInt());
        } else {
            return super.and(rhs);
        }
    }
    @Override public PyObject floorDiv(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return floorDiv(value, rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return floorDiv(value, rhsBool.asInt());
        } else {
            return super.floorDiv(rhs);
        }
    }
    @Override public PyObject lshift(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return lshift(value, rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return lshift(value, rhsBool.asInt());
        } else {
            return super.lshift(rhs);
        }
    }
    @Override public PyObject mod(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return mod(value, rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return mod(value, rhsBool.asInt());
        } else {
            return super.mod(rhs);
        }
    }
    @Override public PyObject mul(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return mul(value, rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return mul(value, rhsBool.asInt());
        } else {
            return super.mul(rhs);
        }
    }
    @Override public PyObject or(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return or(value, rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return or(value, rhsBool.asInt());
        } else {
            return super.or(rhs);
        }
    }
    @Override public PyObject pow(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return pow(value, rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return pow(value, rhsBool.asInt());
        } else {
            return super.pow(rhs);
        }
    }
    @Override public PyObject rshift(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return rshift(value, rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return rshift(value, rhsBool.asInt());
        } else {
            return super.rshift(rhs);
        }
    }
    @Override public PyObject sub(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return sub(value, rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return sub(value, rhsBool.asInt());
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
            return xor(value, rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return xor(value, rhsBool.asInt());
        } else {
            return super.xor(rhs);
        }
    }

    @Override public boolean ge(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value >= rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value >= rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return value >= rhsFloat.value;
        } else {
            return super.ge(rhs);
        }
    }
    @Override public boolean gt(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value > rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value > rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return value > rhsFloat.value;
        } else {
            return super.gt(rhs);
        }
    }
    @Override public boolean le(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value <= rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value <= rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return value <= rhsFloat.value;
        } else {
            return super.le(rhs);
        }
    }
    @Override public boolean lt(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value < rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value < rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return value < rhsFloat.value;
        } else {
            return super.lt(rhs);
        }
    }

    @Override public PyConcreteType type() { return PyIntType.singleton; }

    @Override public boolean boolValue() { return value != 0; }
    @Override public boolean contains(PyObject rhs) { return defaultContains(rhs); }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value == rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value == rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return floatValue() == rhsFloat.value;
        } else {
            return false;
        }
    }
    @Override public double floatValue() { return value; }
    @Override public String format(String formatSpec) {
        return PyRuntime.pyfunc_pyj_int_format(this, new PyString(formatSpec)).value;
    }
    @Override public int hashCode() { return Runtime.hashRational(value, 1); }
    @Override public boolean hasIndex() { return true; }
    @Override public long indexValue() { return value; }
    @Override public long intValue() { return value; }
    @Override public String repr() { return String.valueOf(value); }

    static PyObject pygetset_denominator(PyObject obj) { return PyInt.singleton_1; }
    static PyObject pygetset_imag(PyObject obj) { return PyInt.singleton_0; }
    static PyObject pygetset_numerator(PyObject obj) { return obj; }
    static PyObject pygetset_real(PyObject obj) { return obj; }

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
    public PyBytes pymethod_to_bytes(PyObject length, PyObject byteorder, PyObject signedObj) {
        int len = Math.toIntExact(length.indexValue());
        boolean littleEndian = false;
        boolean signed = signedObj.boolValue();
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
        if (!signed) {
            if (v < 0) {
                throw PyOverflowError.raise("can't convert negative int to unsigned");
            }
            if (len < 8) { // len >= 8 is always representable
                long max = (1L << (len * 8)) - 1;
                if (v > max) {
                    throw PyOverflowError.raise("int too big to convert");
                }
            }
        } else if (len < 8) {
            if (len == 0) {
                if (v != 0) {
                    throw PyOverflowError.raise("int too big to convert");
                }
            } else {
                int bits = len * 8;
                long min = -(1L << (bits - 1));
                long max = (1L << (bits - 1)) - 1;
                if ((v < min) || (v > max)) {
                    throw PyOverflowError.raise("int too big to convert");
                }
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
    public static PyInt pymethod_from_bytes(PyType self, PyObject bytes, PyObject byteorder, PyObject signedObj) {
        boolean signed = signedObj.boolValue();
        boolean littleEndian = false;
        if (byteorder != null) {
            if (!(byteorder instanceof PyString byteorderStr)) {
                throw PyTypeError.raise("from_bytes() argument 'byteorder' must be str, not " + byteorder.type().name());
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

        byte[] data;
        if (bytes instanceof PyBytes bytesBytes) {
            data = bytesBytes.value;
        } else if (bytes instanceof PyByteArray bytesByteArray) {
            data = bytesByteArray.value;
        } else {
            if (!bytes.hasIter()) {
                throw PyTypeError.raise("cannot convert " + PyString.reprOf(bytes.type().name()) + " object to bytes");
            }
            var b = new ByteArrayOutputStream();
            var iter = bytes.iter();
            PyObject item;
            while ((item = iter.next()) != null) {
                long v = item.indexValue();
                if ((v < 0) || (v >= 256)) {
                    throw PyValueError.raise("bytes must be in range(0, 256)");
                }
                b.write((byte)v);
            }
            data = b.toByteArray();
        }

        if (data.length == 0) {
            return PyInt.singleton_0;
        }
        byte[] bigEndianData;
        if (littleEndian) {
            bigEndianData = new byte[data.length];
            for (int i = 0; i < data.length; i++) {
                bigEndianData[i] = data[data.length - 1 - i];
            }
        } else {
            bigEndianData = data;
        }

        int len = bigEndianData.length;
        if (signed) {
            boolean negative = (bigEndianData[0] & 0x80) != 0;
            int pad = negative ? 0xFF : 0x00;
            int start = 0;
            while ((len - start > 8) && ((bigEndianData[start] & 0xFF) == pad)) {
                start++;
            }
            len -= start;
            if ((len > 8) || ((len == 8) && (((bigEndianData[start] & 0x80) != 0) != negative))) {
                throw PyOverflowError.raise("int too big to convert");
            }
            long result = 0;
            for (int i = start; i < bigEndianData.length; i++) {
                result = (result << 8) | (bigEndianData[i] & 0xFF);
            }
            if ((len < 8) && negative) {
                result |= (-1L << (len * 8));
            }
            return new PyInt(result);
        }

        int start = 0;
        while ((len - start > 8) && (bigEndianData[start] == 0)) {
            start++;
        }
        len -= start;
        if ((len > 8) || ((len == 8) && ((bigEndianData[start] & 0x80) != 0))) {
            throw PyOverflowError.raise("int too big to convert");
        }
        long result = 0;
        for (int i = start; i < bigEndianData.length; i++) {
            result = (result << 8) | (bigEndianData[i] & 0xFF);
        }
        return new PyInt(result);
    }
}
