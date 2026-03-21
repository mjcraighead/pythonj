// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.ArrayList;

public final class PyZip extends PyIter {
    private final PyIter[] iters;

    PyZip(PyIter[] _iters) {
        iters = _iters; // WARNING: takes ownership of _iters from caller, does not copy
    }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("zip() does not accept kwargs");
        }
        PyIter iters[] = new PyIter[args.length];
        for (int i = 0; i < args.length; i++) {
            iters[i] = args[i].iter();
        }
        return new PyZip(iters);
    }

    @Override public PyTuple next() {
        if (iters.length == 0) {
            return null; // special case: zip of no iterators yields nothing
        }
        var array = new PyObject[iters.length];
        for (int i = 0; i < iters.length; i++) {
            var item = iters[i].next();
            if (item == null) {
                return null; // XXX understand corner cases with iterators of different lengths
            }
            array[i] = item;
        }
        return new PyTuple(array);
    }

    @Override public boolean contains(PyObject rhs) { return defaultContains(rhs); }
    @Override public String repr() { return defaultRepr(); }
    @Override public PyBuiltinType type() { return PyZipType.singleton; }
}
