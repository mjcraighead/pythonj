// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Arrays;
import java.util.ArrayList;

final class PyTupleIter extends PyIter {
    private static final PyConcreteType type_singleton = new PyConcreteType("tuple_iterator", PyTupleIter.class);

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
    @Override public PyConcreteType type() { return type_singleton; }
};

public final class PyTuple extends PyObject {
    public static final PyTuple empty_singleton = new PyTuple();

    public final PyObject[] items;

    PyTuple() { items = new PyObject[] {}; }
    PyTuple(PyObject[] args) { items = args; } // WARNING: takes ownership of args from caller, does not copy
    PyTuple(ArrayList<PyObject> list) { items = Runtime.arrayListToArray(list); }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireMinMaxPositional(args, kwargs, type.name(), 0, 1);
        return newObjPositional((args.length == 0) ? null : args[0]);
    }
    public static PyTuple newObjPositional(PyObject arg) {
        if (arg == null) {
            return empty_singleton;
        }
        var list = new ArrayList<PyObject>();
        Runtime.addIterableToCollection(list, arg);
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
    @Override public PyConcreteType type() { return PyTupleType.singleton; }

    @Override public boolean boolValue() { return items.length != 0; }
    @Override public boolean contains(PyObject rhs) { return PyRuntime.pyfunc_tuple____contains__(this, rhs).boolValue(); }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PyTuple rhsTuple) {
            return Arrays.equals(items, rhsTuple.items);
        }
        return false;
    }
    @Override public int hashCode() { return Arrays.hashCode(items); }
    @Override public long len() { return items.length; }
    @Override public String repr() { return PyRuntime.pyfunc_tuple____repr__(this).value; }
}
