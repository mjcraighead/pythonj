// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class PyBytes extends PyObject {
    public static final PyBytes empty_singleton = new PyBytes(new byte[0]);

    static final class PyBytesIter extends PyIter {
        private static final PyConcreteType type_singleton = new PyConcreteType("bytes_iterator", PyBytesIter.class);

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
        @Override public PyConcreteType type() { return type_singleton; }
    };

    public final byte[] value;

    PyBytes(byte[] _value) { value = _value; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        if (args.length > 1) {
            throw new IllegalArgumentException("bytes() takes 0 or 1 arguments");
        }
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("bytes() does not accept kwargs");
        }
        if (args.length == 0) {
            return empty_singleton;
        }
        PyObject arg = args[0];
        if (arg.hasIndex()) {
            return new PyBytes(new byte[Math.toIntExact(arg.indexValue())]);
        }
        var b = new ByteArrayOutputStream();
        var iter = arg.iter();
        for (var item = iter.next(); item != null; item = iter.next()) {
            long i = item.indexValue();
            if ((i < 0) || (i >= 256)) {
                throw PyValueError.raise("bytes must be in range(0, 256)");
            }
            b.write((int)i);
        }
        return new PyBytes(b.toByteArray());
    }

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
        } else if (key.hasIndex()) {
            int index = Math.toIntExact(key.indexValue());
            if (index < 0) {
                index += value.length;
            }
            try {
                return new PyInt(value[index] & 0xFF);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw PyIndexError.raise("index out of range");
            }
        } else {
            throw PyTypeError.raise("byte indices must be integers or slices, not " + key.type().name());
        }
    }
    @Override public void delItem(PyObject key) {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object doesn't support item deletion");
    }

    @Override public final boolean hasIter() { return true; }
    @Override public PyBytesIter iter() { return new PyBytesIter(this); }
    @Override public PyConcreteType type() { return PyBytesType.singleton; }

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

    public PyBytes pymethod_join(PyObject arg) {
        var out = new ByteArrayOutputStream();
        if (!arg.hasIter()) {
            throw PyTypeError.raise("can only join an iterable");
        }
        var iter = arg.iter();
        long index = 0;
        for (var item = iter.next(); item != null; item = iter.next(), index++) {
            if (index != 0) {
                out.writeBytes(value);
            }
            if (item instanceof PyBytes itemBytes) {
                out.writeBytes(itemBytes.value);
            } else if (item instanceof PyByteArray itemByteArray) {
                out.writeBytes(itemByteArray.value);
            } else {
                throw PyTypeError.raiseFormat("sequence item %d: expected a bytes-like object, %s found", index, item.type().name());
            }
        }
        return new PyBytes(out.toByteArray());
    }
    public PyObject pymethod_center(PyObject width, PyObject fillchar) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_decode(PyObject encoding, PyObject errors) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_expandtabs(PyObject tabsize) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_hex(PyObject sep, PyObject bytes_per_sep) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_ljust(PyObject width, PyObject fillchar) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_lstrip(PyObject bytes) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_partition(PyObject sep) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_replace(PyObject old, PyObject _new, PyObject count) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_rjust(PyObject width, PyObject fillchar) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_rpartition(PyObject sep) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_rsplit(PyObject sep, PyObject maxsplit) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_rstrip(PyObject bytes) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_split(PyObject sep, PyObject maxsplit) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_splitlines(PyObject keepends) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_strip(PyObject bytes) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_translate(PyObject table, PyObject delete) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_zfill(PyObject width) { throw new UnsupportedOperationException(); }
    public static PyObject pymethod_maketrans(PyObject frm, PyObject to) { throw new UnsupportedOperationException(); }
}
