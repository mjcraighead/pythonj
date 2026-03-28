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

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireMaxArgs(args, 2, type.name());
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("int() does not accept kwargs");
        }
        if (args.length == 0) {
            return PyInt.singleton_0;
        }
        PyObject arg0 = args[0];
        // XXX should also try intValue when length is 1
        if (arg0.hasIndex()) {
            if (args.length > 1) {
                throw PyTypeError.raise("int() can't convert non-string with explicit base");
            }
            return new PyInt(arg0.indexValue());
        }
        if (arg0 instanceof PyString arg0Str) {
            long base = 10;
            if (args.length > 1) {
                PyObject arg1 = args[1];
                base = arg1.indexValue();
                if ((base < 0) || (base == 1) || (base > 36)) {
                    throw PyValueError.raise("int() base must be >= 2 and <= 36, or 0");
                }
                if (base == 0) {
                    throw new UnsupportedOperationException("base 0 unsupported at present");
                }
            }
            String s = arg0Str.value;
            int i = 0;
            while ((i < s.length()) && (s.charAt(i) == ' ')) {
                i++;
            }
            long sign = 1;
            if (i < s.length()) {
                if (s.charAt(i) == '-') {
                    i++;
                    sign = -1;
                } else if (s.charAt(i) == '+') {
                    i++;
                }
            }
            if (i == s.length()) {
                throw PyValueError.raiseFormat("invalid literal for int() with base %d: %s", base, PyString.reprOf(s));
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
                    digit = 36; // always invalid
                }
                if (digit >= base) {
                    throw PyValueError.raiseFormat("invalid literal for int() with base %d: %s", base, PyString.reprOf(s));
                }
                value = Math.addExact(Math.multiplyExact(value, base), digit);
            }
            return new PyInt(Math.multiplyExact(sign, value));
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
    public PyBytes toBytesImpl(PyObject length, PyObject byteorder, PyObject signedObj) {
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
    public PyBytes pymethod_to_bytes(PyObject[] args, PyDict kwargs) {
        int argsLength = args.length;
        PyObject length = (argsLength >= 1) ? args[0] : PyInt.singleton_1;
        PyObject byteorder = (argsLength >= 2) ? args[1] : null;
        PyObject signed = PyBool.false_singleton;
        if ((kwargs != null) && kwargs.boolValue()) {
            long kwargsLen = kwargs.len();
            if ((argsLength == 0) && (kwargsLen > 3)) {
                throw Runtime.raiseAtMostKwArgs("to_bytes", 3, kwargsLen);
            }
            if (argsLength + kwargsLen > 3) {
                throw Runtime.raiseAtMostArgs("to_bytes", 3, argsLength + kwargsLen);
            }
            String unknownKw = null;
            for (var x: kwargs.items.entrySet()) {
                PyString kw = (PyString)x.getKey(); // PyString validated at call site
                if (kw.value.equals("length")) {
                    if (argsLength >= 1) {
                        throw Runtime.raiseArgGivenByNameAndPosition("to_bytes", "length", 1);
                    }
                    length = x.getValue();
                } else if (kw.value.equals("byteorder")) {
                    if (argsLength >= 2) {
                        throw Runtime.raiseArgGivenByNameAndPosition("to_bytes", "byteorder", 2);
                    }
                    byteorder = x.getValue();
                } else if (kw.value.equals("signed")) {
                    signed = x.getValue();
                } else if (unknownKw == null) {
                    unknownKw = kw.value;
                }
            }
            if (unknownKw != null) {
                throw Runtime.raiseUnexpectedKwArg("to_bytes", unknownKw);
            }
        } else if (argsLength > 3) {
            throw Runtime.raiseAtMostArgs("to_bytes", 3, argsLength);
        } else if (argsLength > 2) {
            throw Runtime.raiseAtMostPosArgs("to_bytes", 2, argsLength);
        }
        return toBytesImpl(length, byteorder, signed);
    }
    public static PyInt fromBytesImpl(PyObject bytes, PyObject byteorder, PyObject signedObj) {
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
    public static PyObject pymethod_from_bytes(PyType self, PyObject[] args, PyDict kwargs) {
        int argsLength = args.length;
        PyObject bytes = (argsLength >= 1) ? args[0] : null;
        PyObject byteorder = (argsLength >= 2) ? args[1] : null;
        PyObject signedObj = PyBool.false_singleton;
        if ((kwargs != null) && kwargs.boolValue()) {
            long kwargsLen = kwargs.len();
            if ((argsLength == 0) && (kwargsLen > 3)) {
                throw Runtime.raiseAtMostKwArgs("from_bytes", 3, kwargsLen);
            }
            if (argsLength + kwargsLen > 3) {
                throw Runtime.raiseAtMostArgs("from_bytes", 3, argsLength + kwargsLen);
            }
            String unknownKw = null;
            for (var x: kwargs.items.entrySet()) {
                PyString kw = (PyString)x.getKey(); // PyString validated at call site
                if (kw.value.equals("bytes")) {
                    if (bytes != null) {
                        throw Runtime.raiseArgGivenByNameAndPosition("from_bytes", "bytes", 1);
                    }
                    bytes = x.getValue();
                } else if (kw.value.equals("byteorder")) {
                    if (byteorder != null) {
                        throw Runtime.raiseArgGivenByNameAndPosition("from_bytes", "byteorder", 2);
                    }
                    byteorder = x.getValue();
                } else if (kw.value.equals("signed")) {
                    signedObj = x.getValue();
                } else if (unknownKw == null) {
                    unknownKw = kw.value;
                }
            }
            if (bytes == null) {
                throw Runtime.raiseMissingRequiredArg("from_bytes", "bytes", 1);
            }
            if (unknownKw != null) {
                throw Runtime.raiseUnexpectedKwArg("from_bytes", unknownKw);
            }
        } else if (argsLength > 3) {
            throw Runtime.raiseAtMostArgs("from_bytes", 3, argsLength);
        } else if (argsLength > 2) {
            throw Runtime.raiseAtMostPosArgs("from_bytes", 2, argsLength);
        } else if (argsLength < 1) {
            throw Runtime.raiseMissingRequiredArg("from_bytes", "bytes", 1);
        }
        return fromBytesImpl(bytes, byteorder, signedObj);
    }
}
