// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

// BEGIN GENERATED CODE: PyByteArrayType
final class PyByteArrayType extends PyBuiltinType {
    public static final PyByteArrayType singleton = new PyByteArrayType();
    private static final PyMethodDescriptor pyattr_append = new PyMethodDescriptor(singleton, "append", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "append"));
    private static final PyMethodDescriptor pyattr_capitalize = new PyMethodDescriptor(singleton, "capitalize", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "capitalize"));
    private static final PyMethodDescriptor pyattr_center = new PyMethodDescriptor(singleton, "center", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "center"));
    private static final PyMethodDescriptor pyattr_clear = new PyMethodDescriptor(singleton, "clear", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "clear"));
    private static final PyMethodDescriptor pyattr_copy = new PyMethodDescriptor(singleton, "copy", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "copy"));
    private static final PyMethodDescriptor pyattr_count = new PyMethodDescriptor(singleton, "count", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "count"));
    private static final PyMethodDescriptor pyattr_decode = new PyMethodDescriptor(singleton, "decode", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "decode"));
    private static final PyMethodDescriptor pyattr_endswith = new PyMethodDescriptor(singleton, "endswith", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "endswith"));
    private static final PyMethodDescriptor pyattr_expandtabs = new PyMethodDescriptor(singleton, "expandtabs", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "expandtabs"));
    private static final PyMethodDescriptor pyattr_extend = new PyMethodDescriptor(singleton, "extend", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "extend"));
    private static final PyMethodDescriptor pyattr_find = new PyMethodDescriptor(singleton, "find", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "find"));
    private static final PyClassMethodDescriptor pyattr_fromhex = new PyClassMethodDescriptor(singleton, "fromhex", PyByteArrayClassMethod_fromhex::new);
    private static final PyMethodDescriptor pyattr_hex = new PyMethodDescriptor(singleton, "hex", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "hex"));
    private static final PyMethodDescriptor pyattr_index = new PyMethodDescriptor(singleton, "index", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "index"));
    private static final PyMethodDescriptor pyattr_insert = new PyMethodDescriptor(singleton, "insert", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "insert"));
    private static final PyMethodDescriptor pyattr_isalnum = new PyMethodDescriptor(singleton, "isalnum", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "isalnum"));
    private static final PyMethodDescriptor pyattr_isalpha = new PyMethodDescriptor(singleton, "isalpha", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "isalpha"));
    private static final PyMethodDescriptor pyattr_isascii = new PyMethodDescriptor(singleton, "isascii", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "isascii"));
    private static final PyMethodDescriptor pyattr_isdigit = new PyMethodDescriptor(singleton, "isdigit", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "isdigit"));
    private static final PyMethodDescriptor pyattr_islower = new PyMethodDescriptor(singleton, "islower", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "islower"));
    private static final PyMethodDescriptor pyattr_isspace = new PyMethodDescriptor(singleton, "isspace", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "isspace"));
    private static final PyMethodDescriptor pyattr_istitle = new PyMethodDescriptor(singleton, "istitle", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "istitle"));
    private static final PyMethodDescriptor pyattr_isupper = new PyMethodDescriptor(singleton, "isupper", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "isupper"));
    private static final PyMethodDescriptor pyattr_join = new PyMethodDescriptor(singleton, "join", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "join"));
    private static final PyMethodDescriptor pyattr_ljust = new PyMethodDescriptor(singleton, "ljust", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "ljust"));
    private static final PyMethodDescriptor pyattr_lower = new PyMethodDescriptor(singleton, "lower", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "lower"));
    private static final PyMethodDescriptor pyattr_lstrip = new PyMethodDescriptor(singleton, "lstrip", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "lstrip"));
    private static final PyStaticMethod pyattr_maketrans = new PyStaticMethod(singleton, "maketrans", new PyByteArrayStaticMethod_maketrans(singleton));
    private static final PyMethodDescriptor pyattr_partition = new PyMethodDescriptor(singleton, "partition", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "partition"));
    private static final PyMethodDescriptor pyattr_pop = new PyMethodDescriptor(singleton, "pop", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "pop"));
    private static final PyMethodDescriptor pyattr_remove = new PyMethodDescriptor(singleton, "remove", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "remove"));
    private static final PyMethodDescriptor pyattr_replace = new PyMethodDescriptor(singleton, "replace", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "replace"));
    private static final PyMethodDescriptor pyattr_removeprefix = new PyMethodDescriptor(singleton, "removeprefix", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "removeprefix"));
    private static final PyMethodDescriptor pyattr_removesuffix = new PyMethodDescriptor(singleton, "removesuffix", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "removesuffix"));
    private static final PyMethodDescriptor pyattr_resize = new PyMethodDescriptor(singleton, "resize", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "resize"));
    private static final PyMethodDescriptor pyattr_reverse = new PyMethodDescriptor(singleton, "reverse", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "reverse"));
    private static final PyMethodDescriptor pyattr_rfind = new PyMethodDescriptor(singleton, "rfind", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "rfind"));
    private static final PyMethodDescriptor pyattr_rindex = new PyMethodDescriptor(singleton, "rindex", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "rindex"));
    private static final PyMethodDescriptor pyattr_rjust = new PyMethodDescriptor(singleton, "rjust", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "rjust"));
    private static final PyMethodDescriptor pyattr_rpartition = new PyMethodDescriptor(singleton, "rpartition", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "rpartition"));
    private static final PyMethodDescriptor pyattr_rsplit = new PyMethodDescriptor(singleton, "rsplit", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "rsplit"));
    private static final PyMethodDescriptor pyattr_rstrip = new PyMethodDescriptor(singleton, "rstrip", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "rstrip"));
    private static final PyMethodDescriptor pyattr_split = new PyMethodDescriptor(singleton, "split", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "split"));
    private static final PyMethodDescriptor pyattr_splitlines = new PyMethodDescriptor(singleton, "splitlines", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "splitlines"));
    private static final PyMethodDescriptor pyattr_startswith = new PyMethodDescriptor(singleton, "startswith", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "startswith"));
    private static final PyMethodDescriptor pyattr_strip = new PyMethodDescriptor(singleton, "strip", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "strip"));
    private static final PyMethodDescriptor pyattr_swapcase = new PyMethodDescriptor(singleton, "swapcase", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "swapcase"));
    private static final PyMethodDescriptor pyattr_title = new PyMethodDescriptor(singleton, "title", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "title"));
    private static final PyMethodDescriptor pyattr_translate = new PyMethodDescriptor(singleton, "translate", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "translate"));
    private static final PyMethodDescriptor pyattr_upper = new PyMethodDescriptor(singleton, "upper", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "upper"));
    private static final PyMethodDescriptor pyattr_zfill = new PyMethodDescriptor(singleton, "zfill", obj -> new PyByteArray.PyByteArrayMethodUnimplemented(obj, "zfill"));
    private static final PyString pyattr___doc__ = new PyString("bytearray(iterable_of_ints) -> bytearray\nbytearray(string, encoding[, errors]) -> bytearray\nbytearray(bytes_or_buffer) -> mutable copy of bytes_or_buffer\nbytearray(int) -> bytes array of size given by the parameter initialized with null bytes\nbytearray() -> empty bytes array\n\nConstruct a mutable bytearray object from:\n  - an iterable yielding integers in range(256)\n  - a text string encoded using the specified encoding\n  - a bytes or a buffer object\n  - any object implementing the buffer API.\n  - an integer");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(52);
    static {
        attrs.put(new PyString("append"), pyattr_append);
        attrs.put(new PyString("capitalize"), pyattr_capitalize);
        attrs.put(new PyString("center"), pyattr_center);
        attrs.put(new PyString("clear"), pyattr_clear);
        attrs.put(new PyString("copy"), pyattr_copy);
        attrs.put(new PyString("count"), pyattr_count);
        attrs.put(new PyString("decode"), pyattr_decode);
        attrs.put(new PyString("endswith"), pyattr_endswith);
        attrs.put(new PyString("expandtabs"), pyattr_expandtabs);
        attrs.put(new PyString("extend"), pyattr_extend);
        attrs.put(new PyString("find"), pyattr_find);
        attrs.put(new PyString("fromhex"), pyattr_fromhex);
        attrs.put(new PyString("hex"), pyattr_hex);
        attrs.put(new PyString("index"), pyattr_index);
        attrs.put(new PyString("insert"), pyattr_insert);
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
        attrs.put(new PyString("pop"), pyattr_pop);
        attrs.put(new PyString("remove"), pyattr_remove);
        attrs.put(new PyString("replace"), pyattr_replace);
        attrs.put(new PyString("removeprefix"), pyattr_removeprefix);
        attrs.put(new PyString("removesuffix"), pyattr_removesuffix);
        attrs.put(new PyString("resize"), pyattr_resize);
        attrs.put(new PyString("reverse"), pyattr_reverse);
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

    private PyByteArrayType() { super("bytearray", PyByteArray.class, PyByteArray::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "append": return pyattr_append;
            case "capitalize": return pyattr_capitalize;
            case "center": return pyattr_center;
            case "clear": return pyattr_clear;
            case "copy": return pyattr_copy;
            case "count": return pyattr_count;
            case "decode": return pyattr_decode;
            case "endswith": return pyattr_endswith;
            case "expandtabs": return pyattr_expandtabs;
            case "extend": return pyattr_extend;
            case "find": return pyattr_find;
            case "fromhex": return pyattr_fromhex;
            case "hex": return pyattr_hex;
            case "index": return pyattr_index;
            case "insert": return pyattr_insert;
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
            case "pop": return pyattr_pop;
            case "remove": return pyattr_remove;
            case "replace": return pyattr_replace;
            case "removeprefix": return pyattr_removeprefix;
            case "removesuffix": return pyattr_removesuffix;
            case "resize": return pyattr_resize;
            case "reverse": return pyattr_reverse;
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
}
// END GENERATED CODE: PyByteArrayType

final class PyByteArrayClassMethod_fromhex extends PyBuiltinMethod<PyType> {
    PyByteArrayClassMethod_fromhex(PyType self) { super(self); }
    @Override public String methodName() { return "fromhex"; }
    @Override public PyObject call(PyObject[] args, PyDict kwargs) {
        throw new UnsupportedOperationException("bytearray.fromhex() unimplemented");
    }
}
final class PyByteArrayStaticMethod_maketrans extends PyBuiltinMethod<PyType> {
    PyByteArrayStaticMethod_maketrans(PyType self) { super(self); }
    @Override public String methodName() { return "maketrans"; }
    @Override public PyObject call(PyObject[] args, PyDict kwargs) {
        throw new UnsupportedOperationException("bytearray.maketrans() unimplemented");
    }
}

public final class PyByteArray extends PyObject {
    static final class PyByteArrayIter extends PyIter {
        private static final PyBuiltinType type_singleton = new PyBuiltinType("bytearray_iterator", PyByteArrayIter.class);

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
        @Override public PyBuiltinType type() { return type_singleton; }
    };

// BEGIN GENERATED CODE: PyByteArray
    protected static final class PyByteArrayMethodUnimplemented extends PyBuiltinMethod<PyByteArray> {
        private final String name;
        PyByteArrayMethodUnimplemented(PyObject _self, String _name) { super((PyByteArray)_self); name = _name; }
        @Override public String methodName() { return name; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("bytearray." + name + "() unimplemented");
        }
    }
// END GENERATED CODE: PyByteArray

    protected byte[] value;

    PyByteArray(byte[] _value) { value = _value; }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        if (args.length > 1) {
            throw new IllegalArgumentException("bytearray() takes 0 or 1 arguments");
        }
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("bytearray() does not accept kwargs");
        }
        if (args.length == 0) {
            return new PyByteArray(new byte[0]);
        }
        PyObject arg = args[0];
        if (arg.hasIndex()) {
            return new PyByteArray(new byte[Math.toIntExact(arg.indexValue())]);
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
        return new PyByteArray(b.toByteArray());
    }

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
    @Override public PyBuiltinType type() { return PyByteArrayType.singleton; }

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
