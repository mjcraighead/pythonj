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
        @Override public PyBuiltinClass type() { return iter_class_singleton; }
    };

    public final byte[] value;

    PyBytes(byte[] _value) { value = _value; }

    @Override public PyInt getItem(PyObject key) {
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

    @Override public PyBytesIter iter() { return new PyBytesIter(this); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_bytes; }

    @Override public boolean boolValue() { return value.length != 0; }
    @Override public boolean equals(Object rhs_arg) {
        if (rhs_arg instanceof PyBytes rhs) {
            return Arrays.equals(value, rhs.value);
        }
        return false;
    }
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
