// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Arrays;
import java.util.ArrayList;

public final class PyTuple extends PyObject {
    private static final PyBuiltinClass iter_class_singleton = new PyBuiltinClass("tuple_iterator", PyTupleIter.class);
    static final class PyTupleIter extends PyIter {
        private final PyObject[] items;
        private int index = 0;

        PyTupleIter(PyTuple t) { items = t.items; }

        @Override public PyObject next() {
            if (index >= items.length) {
                return null;
            }
            return items[index++];
        }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyBuiltinClass type() { return iter_class_singleton; }
    };

    public final PyObject[] items;

    PyTuple() { items = new PyObject[] {}; }
    PyTuple(PyObject[] args) { items = args; } // WARNING: takes ownership of args from caller, does not copy
    PyTuple(ArrayList<PyObject> list) { items = Runtime.arrayListToArray(list); }

    public static PyObject[] add(PyObject[] lhs, PyObject[] rhs) {
        int newLength = Math.addExact(lhs.length, rhs.length);
        PyObject[] ret = Arrays.copyOf(lhs, newLength);
        System.arraycopy(rhs, 0, ret, lhs.length, rhs.length);
        return ret;
    }
    public static PyObject[] mul(PyObject[] lhs, long rhs) {
        int count = Math.toIntExact(rhs);
        if (count <= 0) {
            return new PyObject[0];
        }
        int newLength = Math.multiplyExact(lhs.length, count);
        PyObject[] ret = new PyObject[newLength];
        for (int i = 0; i < count; i++) {
            System.arraycopy(lhs, 0, ret, i * lhs.length, lhs.length);
        }
        return ret;
    }

    @Override public PyTuple add(PyObject rhs) {
        if (rhs instanceof PyTuple rhsTuple) {
            return new PyTuple(add(items, rhsTuple.items));
        } else {
            throw PyTypeError.raise("can only concatenate tuple (not \"" + rhs.type().name() + "\") to tuple");
        }
    }
    @Override public PyTuple mul(PyObject rhs) {
        if (!rhs.hasIndex()) {
            throw PyTypeError.raise("can't multiply sequence by non-int of type " + PyString.reprOf(rhs.type().name()));
        }
        return new PyTuple(mul(items, rhs.indexValue()));
    }
    @Override public PyTuple rmul(PyObject rhs) { return mul(rhs); }

    @Override public boolean ge(PyObject rhs) { throw unimplementedMethod("ge"); }
    @Override public boolean gt(PyObject rhs) { throw unimplementedMethod("gt"); }
    @Override public boolean le(PyObject rhs) { throw unimplementedMethod("le"); }
    @Override public boolean lt(PyObject rhs) { throw unimplementedMethod("lt"); }

    @Override public PyObject getItem(PyObject key) {
        int index = Math.toIntExact(key.indexValue());
        if (index < 0) {
            index += items.length;
        }
        try {
            return items[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw PyIndexError.raise("tuple index out of range");
        }
    }

    @Override public final boolean hasIter() { return true; }
    @Override public PyTupleIter iter() { return new PyTupleIter(this); }
    @Override public PyReversed reversed() { return new PyReversed(this); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_tuple; }

    @Override public boolean boolValue() { return items.length != 0; }
    @Override public boolean contains(PyObject rhs) {
        for (var x: items) {
            if (x.equals(rhs)) {
                return true;
            }
        }
        return false;
    }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PyTuple rhsTuple) {
            return Arrays.equals(items, rhsTuple.items);
        }
        return false;
    }
    @Override public int hashCode() { return Arrays.hashCode(items); }
    @Override public long len() { return items.length; }
    @Override public String repr() {
        var s = new StringBuilder("(");
        boolean first = true;
        for (var x: items) {
            if (!first) {
                s.append(", ");
            }
            first = false;
            s.append(x.repr());
        }
        if (items.length == 1) {
            s.append(",");
        }
        return s + ")";
    }
}
