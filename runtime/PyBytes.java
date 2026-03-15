// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Arrays;

public final class PyBytes extends PyObject {
    private static final PyBuiltinClass iter_class_singleton = new PyBuiltinClass("bytes_iterator", PyBytesIter.class);
    static final class PyBytesIter extends PyIter {
        private final byte[] b;
        private int index = 0;

        PyBytesIter(PyBytes _b) { b = _b.value; }

        @Override public PyInt next() {
            if (index >= b.length) {
                return null;
            }
            return new PyInt(b[index++] & 0xFF);
        }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyBuiltinClass type() { return iter_class_singleton; }
    };

    protected static final class PyBytesMethodUnimplemented extends PyBuiltinMethod<PyBytes> {
        private final String name;
        PyBytesMethodUnimplemented(PyBytes self, String name) { super(self); this.name = name; }
        @Override public String methodName() { return name; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("bytes." + name + "() unimplemented");
        }
    }

    public final byte[] value;

    PyBytes(byte[] _value) { value = _value; }

    public static byte[] add(byte[] lhs, byte[] rhs) {
        int newLength = Math.addExact(lhs.length, rhs.length);
        byte[] ret = Arrays.copyOf(lhs, newLength);
        System.arraycopy(rhs, 0, ret, lhs.length, rhs.length);
        return ret;
    }
    public static byte[] mul(byte[] lhs, long rhs) {
        int count = Math.toIntExact(rhs);
        if (count <= 0) {
            return new byte[0];
        }
        int newLength = Math.multiplyExact(lhs.length, count);
        byte[] ret = new byte[newLength];
        for (int i = 0; i < count; i++) {
            System.arraycopy(lhs, 0, ret, i * lhs.length, lhs.length);
        }
        return ret;
    }
    public static boolean contains(byte[] lhs, byte[] rhs) {
        int lhsLen = lhs.length;
        int rhsLen = rhs.length;
        if (rhsLen == 0) {
            return true;
        }
        if (rhsLen > lhsLen) {
            return false;
        }
        for (int i = 0; i <= lhsLen - rhsLen; i++) {
            if (Arrays.mismatch(lhs, i, i + rhsLen, rhs, 0, rhsLen) == -1) {
                return true;
            }
        }
        return false;
    }
    public static boolean contains(byte[] lhs, byte rhs) {
        for (byte b: lhs) {
            if (b == rhs) {
                return true;
            }
        }
        return false;
    }

    @Override public PyBytes add(PyObject rhs) {
        if (rhs instanceof PyBytes rhsBytes) {
            return new PyBytes(add(value, rhsBytes.value));
        } else if (rhs instanceof PyByteArray rhsByteArray) {
            return new PyBytes(add(value, rhsByteArray.value));
        } else {
            throw PyTypeError.raise("can't concat " + rhs.type().name() + " to bytes");
        }
    }
    @Override public PyString mod(PyObject rhs) { throw unimplementedMethod("mod"); }
    @Override public PyBytes mul(PyObject rhs) {
        if (!rhs.hasIndex()) {
            throw PyTypeError.raise("can't multiply sequence by non-int of type " + PyString.reprOf(rhs.type().name()));
        }
        return new PyBytes(mul(value, rhs.indexValue()));
    }
    @Override public PyBytes rmul(PyObject rhs) { return mul(rhs); }

    @Override public boolean ge(PyObject rhs) {
        if (rhs instanceof PyBytes rhsBytes) {
            return Arrays.compareUnsigned(value, rhsBytes.value) >= 0;
        } else if (rhs instanceof PyByteArray rhsByteArray) {
            return Arrays.compareUnsigned(value, rhsByteArray.value) >= 0;
        } else {
            return super.ge(rhs);
        }
    }
    @Override public boolean gt(PyObject rhs) {
        if (rhs instanceof PyBytes rhsBytes) {
            return Arrays.compareUnsigned(value, rhsBytes.value) > 0;
        } else if (rhs instanceof PyByteArray rhsByteArray) {
            return Arrays.compareUnsigned(value, rhsByteArray.value) > 0;
        } else {
            return super.gt(rhs);
        }
    }
    @Override public boolean le(PyObject rhs) {
        if (rhs instanceof PyBytes rhsBytes) {
            return Arrays.compareUnsigned(value, rhsBytes.value) <= 0;
        } else if (rhs instanceof PyByteArray rhsByteArray) {
            return Arrays.compareUnsigned(value, rhsByteArray.value) <= 0;
        } else {
            return super.le(rhs);
        }
    }
    @Override public boolean lt(PyObject rhs) {
        if (rhs instanceof PyBytes rhsBytes) {
            return Arrays.compareUnsigned(value, rhsBytes.value) < 0;
        } else if (rhs instanceof PyByteArray rhsByteArray) {
            return Arrays.compareUnsigned(value, rhsByteArray.value) < 0;
        } else {
            return super.lt(rhs);
        }
    }

    @Override public PyObject getItem(PyObject key) {
        if (key instanceof PySlice slice) {
            PySlice.Indices indices = slice.computeIndices(value.length);
            int index = indices.start();
            int step = indices.step();
            int n = indices.length();
            if (step == 1) {
                return new PyBytes(Arrays.copyOfRange(value, index, index + n));
            } else {
                byte[] result = new byte[n];
                for (int i = 0; i < n; i++) {
                    result[i] = value[index];
                    index += step;
                }
                return new PyBytes(result);
            }
        } else {
            int index = Math.toIntExact(key.indexValue());
            if (index < 0) {
                index += value.length;
            }
            try {
                return new PyInt(value[index] & 0xFF);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw PyIndexError.raise("index out of range");
            }
        }
    }
    @Override public void delItem(PyObject key) {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object doesn't support item deletion");
    }

    @Override public final boolean hasIter() { return true; }
    @Override public PyBytesIter iter() { return new PyBytesIter(this); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_bytes; }

    @Override public boolean boolValue() { return value.length != 0; }
    @Override public boolean contains(PyObject rhs) {
        if (rhs instanceof PyBytes rhsBytes) {
            return contains(value, rhsBytes.value);
        } else if (rhs instanceof PyByteArray rhsByteArray) {
            return contains(value, rhsByteArray.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return contains(value, (byte)rhsBool.asInt());
        } else if (rhs instanceof PyInt rhsInt) {
            long v = rhsInt.value;
            if ((v < 0) || (v > 255)) {
                throw PyValueError.raise("byte must be in range(0, 256)");
            }
            return contains(value, (byte)v);
        } else {
            throw PyTypeError.raise("a bytes-like object is required, not " + PyString.reprOf(rhs.type().name()));
        }
    }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PyBytes rhsBytes) {
            return Arrays.equals(value, rhsBytes.value);
        } else if (rhs instanceof PyByteArray rhsByteArray) {
            return Arrays.equals(value, rhsByteArray.value);
        }
        return false;
    }
    @Override public int hashCode() { return Arrays.hashCode(value); }
    @Override public long len() { return value.length; }
    @Override public String repr() {
        var s = new StringBuilder("b'");
        for (byte x: value) {
            if (x == '\n') {
                s.append("\\n");
            } else if (x == '\r') {
                s.append("\\r");
            } else if (x == '\t') {
                s.append("\\t");
            } else if (x == '\'') {
                s.append("\\'");
            } else if (x == '\\') {
                s.append("\\\\");
            } else if ((x >= 0x20) && (x < 0x7F)) {
                s.append((char)x);
            } else {
                s.append("\\x");
                int c = x & 0xFF;
                s.append("0123456789abcdef".charAt(c >> 4));
                s.append("0123456789abcdef".charAt(c & 15));
            }
        }
        return s + "'";
    }
}
