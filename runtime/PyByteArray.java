// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Arrays;

public final class PyByteArray extends PyObject {
    static final class PyByteArrayIter extends PyIter {
        private static final PyBuiltinClass type_singleton = new PyBuiltinClass("bytearray_iterator", PyByteArrayIter.class);

        private byte[] b;
        private int index = 0;

        PyByteArrayIter(PyByteArray _b) { b = _b.value; }

        @Override public PyInt next() {
            if (index >= b.length) {
                return null;
            }
            return new PyInt(b[index++] & 0xFF);
        }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyBuiltinClass type() { return type_singleton; }
    };

    protected byte[] value;

    PyByteArray(byte[] _value) { value = _value; }

    @Override public PyByteArray add(PyObject rhs) {
        if (rhs instanceof PyBytes rhsBytes) {
            return new PyByteArray(PyBytes.add(value, rhsBytes.value));
        } else if (rhs instanceof PyByteArray rhsByteArray) {
            return new PyByteArray(PyBytes.add(value, rhsByteArray.value));
        } else {
            throw PyTypeError.raise("can't concat " + rhs.type().name() + " to bytearray");
        }
    }
    @Override public PyByteArray addInPlace(PyObject rhs) {
        if (rhs instanceof PyBytes rhsBytes) {
            value = PyBytes.add(value, rhsBytes.value);
        } else if (rhs instanceof PyByteArray rhsByteArray) {
            value = PyBytes.add(value, rhsByteArray.value);
        } else {
            throw PyTypeError.raise("can't concat " + rhs.type().name() + " to bytearray");
        }
        return this;
    }
    @Override public PyString mod(PyObject rhs) { throw unimplementedMethod("mod"); }
    @Override public PyByteArray mul(PyObject rhs) {
        if (!rhs.hasIndex()) {
            throw PyTypeError.raise("can't multiply sequence by non-int of type " + PyString.reprOf(rhs.type().name()));
        }
        return new PyByteArray(PyBytes.mul(value, rhs.indexValue()));
    }
    @Override public PyByteArray mulInPlace(PyObject rhs) {
        if (!rhs.hasIndex()) {
            throw PyTypeError.raise("can't multiply sequence by non-int of type " + PyString.reprOf(rhs.type().name()));
        }
        value = PyBytes.mul(value, rhs.indexValue());
        return this;
    }
    @Override public PyByteArray rmul(PyObject rhs) { return mul(rhs); }

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
                return new PyByteArray(Arrays.copyOfRange(value, index, index + n));
            } else {
                byte[] result = new byte[n];
                for (int i = 0; i < n; i++) {
                    result[i] = value[index];
                    index += step;
                }
                return new PyByteArray(result);
            }
        } else {
            int index = Math.toIntExact(key.indexValue());
            if (index < 0) {
                index += value.length;
            }
            try {
                return new PyInt(value[index] & 0xFF);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw PyIndexError.raise("bytearray index out of range");
            }
        }
    }
    @Override public void setItem(PyObject key, PyObject val) {
        if (key instanceof PySlice) {
            throw unimplementedMethod("slice assignment");
        } else {
            int index = Math.toIntExact(key.indexValue());
            if (index < 0) {
                index += value.length;
            }
            long new_val = val.indexValue();
            if ((new_val < 0) || (new_val > 255)) {
                throw PyValueError.raise("byte must be in range(0, 256)");
            }
            try {
                value[index] = (byte)new_val;
            } catch (ArrayIndexOutOfBoundsException e) {
                throw PyIndexError.raise("bytearray index out of range");
            }
        }
    }
    @Override public void delItem(PyObject key) {
        throw unimplementedMethod("delItem");
    }

    @Override public final boolean hasIter() { return true; }
    @Override public PyByteArrayIter iter() { return new PyByteArrayIter(this); }
    @Override public PyBuiltinClass type() { return Runtime.pyclass_bytearray.singleton; }

    @Override public boolean boolValue() { return value.length != 0; }
    @Override public boolean contains(PyObject rhs) {
        if (rhs instanceof PyBytes rhsBytes) {
            return PyBytes.contains(value, rhsBytes.value);
        } else if (rhs instanceof PyByteArray rhsByteArray) {
            return PyBytes.contains(value, rhsByteArray.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return PyBytes.contains(value, (byte)rhsBool.asInt());
        } else if (rhs instanceof PyInt rhsInt) {
            long v = rhsInt.value;
            if ((v < 0) || (v > 255)) {
                throw PyValueError.raise("byte must be in range(0, 256)");
            }
            return PyBytes.contains(value, (byte)v);
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
    @Override public int hashCode() { throw raiseUnhashable(); }
    @Override public long len() { return value.length; }
    @Override public String repr() {
        var s = new StringBuilder("bytearray(b'");
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
        return s + "')";
    }
}
