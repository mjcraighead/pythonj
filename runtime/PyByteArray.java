// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

final class PyByteArrayIter extends PyIter {
    private static final PyConcreteType type_singleton = new PyConcreteType("bytearray_iterator", "bytearray_iterator", "builtins", PyByteArrayIter.class, PyObjectType.singleton, null);

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
    @Override public PyConcreteType type() { return type_singleton; }
};

public final class PyByteArray extends PyObject {
    protected byte[] value;

    PyByteArray(byte[] _value) { value = _value; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireMinMaxPositional(args, kwargs, type.name(), 0, 3);
        PyObject arg = (args.length >= 1) ? args[0] : null;
        PyObject encoding = (args.length >= 2) ? args[1] : null;
        PyObject errors = (args.length >= 3) ? args[2] : null;
        return newObjPositional(arg, encoding, errors);
    }
    public static PyByteArray newObjPositional(PyObject arg, PyObject encodingObj, PyObject errorsObj) {
        if (arg == null) {
            return new PyByteArray(new byte[0]);
        }
        if (encodingObj != null) {
            if (!(arg instanceof PyString argStr)) {
                throw PyTypeError.raise("encoding without a string argument");
            }
            String encoding = Runtime.requireEncodingArg(encodingObj, "bytearray");
            if (errorsObj != null) {
                Runtime.requireErrorsArg(errorsObj, "bytearray");
            }
            Charset charset = Runtime.lookupCharset(encoding);
            return new PyByteArray(argStr.value.getBytes(charset));
        }
        if (arg.hasIndex()) {
            return new PyByteArray(new byte[Math.toIntExact(arg.indexValue())]);
        }
        if (arg instanceof PyString) {
            throw PyTypeError.raise("string argument without an encoding");
        }
        var b = new ByteArrayOutputStream();
        var iter = arg.iter();
        for (var item = iter.next(); item != null; item = iter.next()) {
            long i = item.indexValue();
            if ((i < 0) || (i >= 256)) {
                throw PyValueError.raise("byte must be in range(0, 256)");
            }
            b.write((int)i);
        }
        return new PyByteArray(b.toByteArray());
    }

    @Override public PyByteArray add(PyObject rhs) {
        byte[] rhsBuffer = Runtime.getBytesLikeBuffer(rhs);
        if (rhsBuffer == null) {
            throw PyTypeError.raise("can't concat " + rhs.type().name() + " to bytearray");
        }
        return new PyByteArray(PyBytes.add(value, rhsBuffer));
    }
    @Override public PyByteArray addInPlace(PyObject rhs) {
        byte[] rhsBuffer = Runtime.getBytesLikeBuffer(rhs);
        if (rhsBuffer == null) {
            throw PyTypeError.raise("can't concat " + rhs.type().name() + " to bytearray");
        }
        value = PyBytes.add(value, rhsBuffer);
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
        byte[] rhsBuffer = Runtime.getBytesLikeBuffer(rhs);
        if (rhsBuffer == null) {
            return super.ge(rhs);
        }
        return Arrays.compareUnsigned(value, rhsBuffer) >= 0;
    }
    @Override public boolean gt(PyObject rhs) {
        byte[] rhsBuffer = Runtime.getBytesLikeBuffer(rhs);
        if (rhsBuffer == null) {
            return super.gt(rhs);
        }
        return Arrays.compareUnsigned(value, rhsBuffer) > 0;
    }
    @Override public boolean le(PyObject rhs) {
        byte[] rhsBuffer = Runtime.getBytesLikeBuffer(rhs);
        if (rhsBuffer == null) {
            return super.le(rhs);
        }
        return Arrays.compareUnsigned(value, rhsBuffer) <= 0;
    }
    @Override public boolean lt(PyObject rhs) {
        byte[] rhsBuffer = Runtime.getBytesLikeBuffer(rhs);
        if (rhsBuffer == null) {
            return super.lt(rhs);
        }
        return Arrays.compareUnsigned(value, rhsBuffer) < 0;
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
        } else if (key.hasIndex()) {
            int index = Math.toIntExact(key.indexValue());
            if (index < 0) {
                index += value.length;
            }
            try {
                return new PyInt(value[index] & 0xFF);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw PyIndexError.raise("bytearray index out of range");
            }
        } else {
            throw PyTypeError.raise("bytearray indices must be integers or slices, not " + key.type().name());
        }
    }
    @Override public void setItem(PyObject key, PyObject val) {
        if (key instanceof PySlice) {
            throw unimplementedMethod("slice assignment");
        } else if (key.hasIndex()) {
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
        } else {
            throw PyTypeError.raise("bytearray indices must be integers or slices, not " + key.type().name());
        }
    }
    @Override public void delItem(PyObject key) {
        throw unimplementedMethod("delItem");
    }

    @Override public final boolean hasIter() { return true; }
    @Override public PyByteArrayIter iter() { return new PyByteArrayIter(this); }
    @Override public PyConcreteType type() { return PyByteArrayType.singleton; }

    @Override public boolean boolValue() { return value.length != 0; }
    @Override public boolean contains(PyObject rhs) {
        if (rhs instanceof PyBool rhsBool) {
            return PyBytes.contains(value, (byte)rhsBool.asInt());
        } else if (rhs instanceof PyInt rhsInt) {
            long v = rhsInt.value;
            if ((v < 0) || (v > 255)) {
                throw PyValueError.raise("byte must be in range(0, 256)");
            }
            return PyBytes.contains(value, (byte)v);
        } else {
            return PyBytes.contains(value, Runtime.requireBytesLikeBuffer(rhs));
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
    @Override public int hashCode() { return slotBasedHashCode(); }
    @Override public long len() { return value.length; }
    @Override public String repr() { return slotBasedRepr(); }

    public static PyObject pymethod_maketrans(PyObject frm, PyObject to) { throw new UnsupportedOperationException(); }
}
