// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;

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

    @Override public PyTuple mul(PyObject rhs) {
        long count = rhs.indexValue();
        if (count <= 0) {
            return new PyTuple();
        }
        var list = new ArrayList<PyObject>();
        for (long i = 0; i < count; i++) {
            Collections.addAll(list, items);
        }
        return new PyTuple(list);
    }

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
    @Override public boolean equals(Object rhsArg) {
        if (rhsArg instanceof PyTuple rhs) {
            return Arrays.equals(items, rhs.items);
        }
        return false;
    }
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
