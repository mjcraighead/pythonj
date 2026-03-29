// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Arrays;
import java.util.ArrayList;

public final class PyTuple extends PyObject {
    public static final PyTuple empty_singleton = new PyTuple();

    static final class PyTupleIter extends PyIter {
        private static final PyBuiltinType type_singleton = new PyBuiltinType("tuple_iterator", PyTupleIter.class);

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
        @Override public PyBuiltinType type() { return type_singleton; }
    };

    public final PyObject[] items;

    PyTuple() { items = new PyObject[] {}; }
    PyTuple(PyObject[] args) { items = args; } // WARNING: takes ownership of args from caller, does not copy
    PyTuple(ArrayList<PyObject> list) { items = Runtime.arrayListToArray(list); }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        Runtime.requireMaxArgs(args, 1, type.name());
        if (args.length == 0) {
            return empty_singleton;
        }
        var list = new ArrayList<PyObject>();
        Runtime.addIterableToCollection(list, args[0]);
        return new PyTuple(list);
    }

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

    @Override public boolean ge(PyObject rhs) {
        if (rhs instanceof PyTuple rhsTuple) {
            return Arrays.compare(items, rhsTuple.items) >= 0;
        } else {
            return super.ge(rhs);
        }
    }
    @Override public boolean gt(PyObject rhs) {
        if (rhs instanceof PyTuple rhsTuple) {
            return Arrays.compare(items, rhsTuple.items) > 0;
        } else {
            return super.gt(rhs);
        }
    }
    @Override public boolean le(PyObject rhs) {
        if (rhs instanceof PyTuple rhsTuple) {
            return Arrays.compare(items, rhsTuple.items) <= 0;
        } else {
            return super.le(rhs);
        }
    }
    @Override public boolean lt(PyObject rhs) {
        if (rhs instanceof PyTuple rhsTuple) {
            return Arrays.compare(items, rhsTuple.items) < 0;
        } else {
            return super.lt(rhs);
        }
    }

    @Override public PyObject getItem(PyObject key) {
        if (key instanceof PySlice slice) {
            PySlice.Indices indices = slice.computeIndices(items.length);
            int index = indices.start();
            int step = indices.step();
            int n = indices.length();
            PyObject[] result = new PyObject[n];
            for (int i = 0; i < n; i++) {
                result[i] = items[index];
                index += step;
            }
            return new PyTuple(result);
        } else if (key.hasIndex()) {
            int index = Math.toIntExact(key.indexValue());
            if (index < 0) {
                index += items.length;
            }
            try {
                return items[index];
            } catch (ArrayIndexOutOfBoundsException e) {
                throw PyIndexError.raise("tuple index out of range");
            }
        } else {
            throw PyTypeError.raise("tuple indices must be integers or slices, not " + key.type().name());
        }
    }

    @Override public final boolean hasIter() { return true; }
    @Override public PyTupleIter iter() { return new PyTupleIter(this); }
    @Override public PyReversed reversed() { return new PyReversed(this); }
    @Override public PyBuiltinType type() { return PyTupleType.singleton; }

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
        for (int i = 0; i < items.length; i++) {
            if (i != 0) {
                s.append(", ");
            }
            s.append(items[i].repr());
        }
        if (items.length == 1) {
            s.append(",");
        }
        return s + ")";
    }

    public PyInt pymethod_count(PyObject arg) {
        long n = 0;
        for (var x: items) {
            if (x.equals(arg)) {
                n++;
            }
        }
        return new PyInt(n);
    }
    public PyInt pymethod_index(PyObject value, PyObject start, PyObject stop) {
        int n = items.length;
        int startIndex = Runtime.asSliceIndexAllowNull(start, 0, n);
        int stopIndex = Runtime.asSliceIndexAllowNull(stop, n, n);
        for (int i = startIndex; i < stopIndex; i++) {
            if (items[i].equals(value)) {
                return new PyInt(i);
            }
        }
        throw PyValueError.raise("tuple.index(x): x not in tuple");
    }
}
