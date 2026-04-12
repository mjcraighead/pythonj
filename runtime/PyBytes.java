// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

final class PyBytesIter extends PyIter {
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

public final class PyBytes extends PyObject {
    public static final PyBytes empty_singleton = new PyBytes(new byte[0]);

    public final byte[] value;

    PyBytes(byte[] _value) { value = _value; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireMinMaxPositional(args, kwargs, type.name(), 0, 3);
        PyObject arg = (args.length >= 1) ? args[0] : null;
        PyObject encoding = (args.length >= 2) ? args[1] : null;
        PyObject errors = (args.length >= 3) ? args[2] : null;
        return newObjPositional(arg, encoding, errors);
    }
    public static PyBytes newObjPositional(PyObject arg, PyObject encodingObj, PyObject errorsObj) {
        if (arg == null) {
            return empty_singleton;
        }
        if (encodingObj != null) {
            if (!(arg instanceof PyString argStr)) {
                throw PyTypeError.raise("encoding without a string argument");
            }
            String encoding = Runtime.requireEncodingArg(encodingObj, "bytes");
            if (errorsObj != null) {
                Runtime.requireErrorsArg(errorsObj, "bytes");
            }
            Charset charset = Runtime.lookupCharset(encoding);
            return new PyBytes(argStr.value.getBytes(charset));
        }
        if (arg.hasIndex()) {
            return new PyBytes(new byte[Math.toIntExact(arg.indexValue())]);
        }
        if (arg instanceof PyString) {
            throw PyTypeError.raise("string argument without an encoding");
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
        byte[] rhsBuffer = Runtime.getBytesLikeBuffer(rhs);
        if (rhsBuffer == null) {
            throw PyTypeError.raise("can't concat " + rhs.type().name() + " to bytes");
        }
        return new PyBytes(add(value, rhsBuffer));
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
        if (rhs instanceof PyBool rhsBool) {
            return contains(value, (byte)rhsBool.asInt());
        } else if (rhs instanceof PyInt rhsInt) {
            long v = rhsInt.value;
            if ((v < 0) || (v > 255)) {
                throw PyValueError.raise("byte must be in range(0, 256)");
            }
            return contains(value, (byte)v);
        } else {
            return contains(value, Runtime.requireBytesLikeBuffer(rhs));
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
    @Override public String repr() { return PyRuntime.pyfunc_bytes____repr__(this).value; }

    public static PyObject pymethod_maketrans(PyObject frm, PyObject to) { throw new UnsupportedOperationException(); }
}
