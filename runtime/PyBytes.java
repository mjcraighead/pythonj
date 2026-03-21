// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

final class PyBytesType extends PyBuiltinType {
// BEGIN GENERATED CODE: PyBytesType
    public static final PyBytesType singleton = new PyBytesType();
    private static final PyMethodDescriptor pyattr_capitalize = new PyMethodDescriptor(singleton, "capitalize", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "capitalize"));
    private static final PyMethodDescriptor pyattr_center = new PyMethodDescriptor(singleton, "center", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "center"));
    private static final PyMethodDescriptor pyattr_count = new PyMethodDescriptor(singleton, "count", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "count"));
    private static final PyMethodDescriptor pyattr_decode = new PyMethodDescriptor(singleton, "decode", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "decode"));
    private static final PyMethodDescriptor pyattr_endswith = new PyMethodDescriptor(singleton, "endswith", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "endswith"));
    private static final PyMethodDescriptor pyattr_expandtabs = new PyMethodDescriptor(singleton, "expandtabs", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "expandtabs"));
    private static final PyMethodDescriptor pyattr_find = new PyMethodDescriptor(singleton, "find", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "find"));
    private static final PyClassMethodDescriptor pyattr_fromhex = new PyClassMethodDescriptor(singleton, "fromhex", PyBytesType.PyBytesClassMethod_fromhex::new);
    private static final PyMethodDescriptor pyattr_hex = new PyMethodDescriptor(singleton, "hex", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "hex"));
    private static final PyMethodDescriptor pyattr_index = new PyMethodDescriptor(singleton, "index", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "index"));
    private static final PyMethodDescriptor pyattr_isalnum = new PyMethodDescriptor(singleton, "isalnum", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "isalnum"));
    private static final PyMethodDescriptor pyattr_isalpha = new PyMethodDescriptor(singleton, "isalpha", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "isalpha"));
    private static final PyMethodDescriptor pyattr_isascii = new PyMethodDescriptor(singleton, "isascii", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "isascii"));
    private static final PyMethodDescriptor pyattr_isdigit = new PyMethodDescriptor(singleton, "isdigit", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "isdigit"));
    private static final PyMethodDescriptor pyattr_islower = new PyMethodDescriptor(singleton, "islower", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "islower"));
    private static final PyMethodDescriptor pyattr_isspace = new PyMethodDescriptor(singleton, "isspace", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "isspace"));
    private static final PyMethodDescriptor pyattr_istitle = new PyMethodDescriptor(singleton, "istitle", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "istitle"));
    private static final PyMethodDescriptor pyattr_isupper = new PyMethodDescriptor(singleton, "isupper", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "isupper"));
    private static final PyMethodDescriptor pyattr_join = new PyMethodDescriptor(singleton, "join", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "join"));
    private static final PyMethodDescriptor pyattr_ljust = new PyMethodDescriptor(singleton, "ljust", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "ljust"));
    private static final PyMethodDescriptor pyattr_lower = new PyMethodDescriptor(singleton, "lower", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "lower"));
    private static final PyMethodDescriptor pyattr_lstrip = new PyMethodDescriptor(singleton, "lstrip", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "lstrip"));
    private static final PyStaticMethod pyattr_maketrans = new PyStaticMethod(singleton, "maketrans", new PyBytesType.PyBytesStaticMethod_maketrans(singleton));
    private static final PyMethodDescriptor pyattr_partition = new PyMethodDescriptor(singleton, "partition", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "partition"));
    private static final PyMethodDescriptor pyattr_replace = new PyMethodDescriptor(singleton, "replace", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "replace"));
    private static final PyMethodDescriptor pyattr_removeprefix = new PyMethodDescriptor(singleton, "removeprefix", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "removeprefix"));
    private static final PyMethodDescriptor pyattr_removesuffix = new PyMethodDescriptor(singleton, "removesuffix", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "removesuffix"));
    private static final PyMethodDescriptor pyattr_rfind = new PyMethodDescriptor(singleton, "rfind", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "rfind"));
    private static final PyMethodDescriptor pyattr_rindex = new PyMethodDescriptor(singleton, "rindex", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "rindex"));
    private static final PyMethodDescriptor pyattr_rjust = new PyMethodDescriptor(singleton, "rjust", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "rjust"));
    private static final PyMethodDescriptor pyattr_rpartition = new PyMethodDescriptor(singleton, "rpartition", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "rpartition"));
    private static final PyMethodDescriptor pyattr_rsplit = new PyMethodDescriptor(singleton, "rsplit", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "rsplit"));
    private static final PyMethodDescriptor pyattr_rstrip = new PyMethodDescriptor(singleton, "rstrip", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "rstrip"));
    private static final PyMethodDescriptor pyattr_split = new PyMethodDescriptor(singleton, "split", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "split"));
    private static final PyMethodDescriptor pyattr_splitlines = new PyMethodDescriptor(singleton, "splitlines", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "splitlines"));
    private static final PyMethodDescriptor pyattr_startswith = new PyMethodDescriptor(singleton, "startswith", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "startswith"));
    private static final PyMethodDescriptor pyattr_strip = new PyMethodDescriptor(singleton, "strip", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "strip"));
    private static final PyMethodDescriptor pyattr_swapcase = new PyMethodDescriptor(singleton, "swapcase", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "swapcase"));
    private static final PyMethodDescriptor pyattr_title = new PyMethodDescriptor(singleton, "title", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "title"));
    private static final PyMethodDescriptor pyattr_translate = new PyMethodDescriptor(singleton, "translate", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "translate"));
    private static final PyMethodDescriptor pyattr_upper = new PyMethodDescriptor(singleton, "upper", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "upper"));
    private static final PyMethodDescriptor pyattr_zfill = new PyMethodDescriptor(singleton, "zfill", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "zfill"));
    private static final PyString pyattr___doc__ = new PyString("bytes(iterable_of_ints) -> bytes\nbytes(string, encoding[, errors]) -> bytes\nbytes(bytes_or_buffer) -> immutable copy of bytes_or_buffer\nbytes(int) -> bytes object of size given by the parameter initialized with null bytes\nbytes() -> empty bytes object\n\nConstruct an immutable array of bytes from:\n  - an iterable yielding integers in range(256)\n  - a text string encoded using the specified encoding\n  - any object implementing the buffer API.\n  - an integer");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(43);
    static {
        attrs.put(new PyString("capitalize"), pyattr_capitalize);
        attrs.put(new PyString("center"), pyattr_center);
        attrs.put(new PyString("count"), pyattr_count);
        attrs.put(new PyString("decode"), pyattr_decode);
        attrs.put(new PyString("endswith"), pyattr_endswith);
        attrs.put(new PyString("expandtabs"), pyattr_expandtabs);
        attrs.put(new PyString("find"), pyattr_find);
        attrs.put(new PyString("fromhex"), pyattr_fromhex);
        attrs.put(new PyString("hex"), pyattr_hex);
        attrs.put(new PyString("index"), pyattr_index);
        attrs.put(new PyString("isalnum"), pyattr_isalnum);
        attrs.put(new PyString("isalpha"), pyattr_isalpha);
        attrs.put(new PyString("isascii"), pyattr_isascii);
        attrs.put(new PyString("isdigit"), pyattr_isdigit);
        attrs.put(new PyString("islower"), pyattr_islower);
        attrs.put(new PyString("isspace"), pyattr_isspace);
        attrs.put(new PyString("istitle"), pyattr_istitle);
        attrs.put(new PyString("isupper"), pyattr_isupper);
        attrs.put(new PyString("join"), pyattr_join);
        attrs.put(new PyString("ljust"), pyattr_ljust);
        attrs.put(new PyString("lower"), pyattr_lower);
        attrs.put(new PyString("lstrip"), pyattr_lstrip);
        attrs.put(new PyString("maketrans"), pyattr_maketrans);
        attrs.put(new PyString("partition"), pyattr_partition);
        attrs.put(new PyString("replace"), pyattr_replace);
        attrs.put(new PyString("removeprefix"), pyattr_removeprefix);
        attrs.put(new PyString("removesuffix"), pyattr_removesuffix);
        attrs.put(new PyString("rfind"), pyattr_rfind);
        attrs.put(new PyString("rindex"), pyattr_rindex);
        attrs.put(new PyString("rjust"), pyattr_rjust);
        attrs.put(new PyString("rpartition"), pyattr_rpartition);
        attrs.put(new PyString("rsplit"), pyattr_rsplit);
        attrs.put(new PyString("rstrip"), pyattr_rstrip);
        attrs.put(new PyString("split"), pyattr_split);
        attrs.put(new PyString("splitlines"), pyattr_splitlines);
        attrs.put(new PyString("startswith"), pyattr_startswith);
        attrs.put(new PyString("strip"), pyattr_strip);
        attrs.put(new PyString("swapcase"), pyattr_swapcase);
        attrs.put(new PyString("title"), pyattr_title);
        attrs.put(new PyString("translate"), pyattr_translate);
        attrs.put(new PyString("upper"), pyattr_upper);
        attrs.put(new PyString("zfill"), pyattr_zfill);
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyBytesType() { super("bytes", PyBytes.class); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "capitalize": return pyattr_capitalize;
            case "center": return pyattr_center;
            case "count": return pyattr_count;
            case "decode": return pyattr_decode;
            case "endswith": return pyattr_endswith;
            case "expandtabs": return pyattr_expandtabs;
            case "find": return pyattr_find;
            case "fromhex": return pyattr_fromhex;
            case "hex": return pyattr_hex;
            case "index": return pyattr_index;
            case "isalnum": return pyattr_isalnum;
            case "isalpha": return pyattr_isalpha;
            case "isascii": return pyattr_isascii;
            case "isdigit": return pyattr_isdigit;
            case "islower": return pyattr_islower;
            case "isspace": return pyattr_isspace;
            case "istitle": return pyattr_istitle;
            case "isupper": return pyattr_isupper;
            case "join": return pyattr_join;
            case "ljust": return pyattr_ljust;
            case "lower": return pyattr_lower;
            case "lstrip": return pyattr_lstrip;
            case "maketrans": return pyattr_maketrans;
            case "partition": return pyattr_partition;
            case "replace": return pyattr_replace;
            case "removeprefix": return pyattr_removeprefix;
            case "removesuffix": return pyattr_removesuffix;
            case "rfind": return pyattr_rfind;
            case "rindex": return pyattr_rindex;
            case "rjust": return pyattr_rjust;
            case "rpartition": return pyattr_rpartition;
            case "rsplit": return pyattr_rsplit;
            case "rstrip": return pyattr_rstrip;
            case "split": return pyattr_split;
            case "splitlines": return pyattr_splitlines;
            case "startswith": return pyattr_startswith;
            case "strip": return pyattr_strip;
            case "swapcase": return pyattr_swapcase;
            case "title": return pyattr_title;
            case "translate": return pyattr_translate;
            case "upper": return pyattr_upper;
            case "zfill": return pyattr_zfill;
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
// END GENERATED CODE: PyBytesType

    @Override public PyBytes call(PyObject[] args, PyDict kwargs) {
        if (args.length > 1) {
            throw new IllegalArgumentException("bytes() takes 0 or 1 arguments");
        }
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("bytes() does not accept kwargs");
        }
        if (args.length == 0) {
            return new PyBytes(new byte[0]);
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
                throw new IllegalArgumentException("invalid byte value");
            }
            b.write((int)i);
        }
        return new PyBytes(b.toByteArray());
    }

    protected static final class PyBytesClassMethod_fromhex extends PyBuiltinMethod<PyType> {
        PyBytesClassMethod_fromhex(PyType self) { super(self); }
        @Override public String methodName() { return "fromhex"; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("bytes.fromhex() unimplemented");
        }
    }
    protected static final class PyBytesStaticMethod_maketrans extends PyBuiltinMethod<PyType> {
        PyBytesStaticMethod_maketrans(PyType self) { super(self); }
        @Override public String methodName() { return "maketrans"; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("bytes.maketrans() unimplemented");
        }
    }
}

public final class PyBytes extends PyObject {
    static final class PyBytesIter extends PyIter {
        private static final PyBuiltinType type_singleton = new PyBuiltinType("bytes_iterator", PyBytesIter.class);

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
        @Override public PyBuiltinType type() { return type_singleton; }
    };

// BEGIN GENERATED CODE: PyBytes
    protected static final class PyBytesMethodUnimplemented extends PyBuiltinMethod<PyBytes> {
        private final String name;
        PyBytesMethodUnimplemented(PyObject _self, String _name) { super((PyBytes)_self); name = _name; }
        @Override public String methodName() { return name; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("bytes." + name + "() unimplemented");
        }
    }
// END GENERATED CODE: PyBytes

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
    @Override public PyBuiltinType type() { return PyBytesType.singleton; }

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
