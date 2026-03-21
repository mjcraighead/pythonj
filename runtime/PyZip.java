// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.ArrayList;

final class PyZipType extends PyBuiltinType {
// BEGIN GENERATED CODE: PyZipType
    public static final PyZipType singleton = new PyZipType();
    private static final PyString pyattr___doc__ = new PyString("The zip object yields n-length tuples, where n is the number of iterables\npassed as positional arguments to zip().  The i-th element in every tuple\ncomes from the i-th iterable argument to zip().  This continues until the\nshortest argument is exhausted.\n\nIf strict is true and one of the arguments is exhausted before the others,\nraise a ValueError.\n\n   >>> list(zip('abcdefg', range(3), range(4)))\n   [('a', 0, 0), ('b', 1, 1), ('c', 2, 2)]");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyZipType() { super("zip", PyZip.class); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
// END GENERATED CODE: PyZipType

    @Override public PyZip call(PyObject[] args, PyDict kwargs) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("zip() does not accept kwargs");
        }
        PyIter iters[] = new PyIter[args.length];
        for (int i = 0; i < args.length; i++) {
            iters[i] = args[i].iter();
        }
        return new PyZip(iters);
    }
}

public final class PyZip extends PyIter {
    private final PyIter[] iters;

    PyZip(PyIter[] _iters) {
        iters = _iters; // WARNING: takes ownership of _iters from caller, does not copy
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
