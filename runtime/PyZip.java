// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.ArrayList;

public final class PyZip extends PyIter {
    private final PyIter[] iters;
    private final boolean strict;

    PyZip(PyIter[] _iters, boolean _strict) {
        iters = _iters; // WARNING: takes ownership of _iters from caller, does not copy
        strict = _strict;
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        return PyRuntime.pyfunc_zip__newobj(type, args, kwargs);
    }
    public static PyObject newObjPositional(PyObject[] args, PyObject strict) {
        boolean strictBool = (strict != null) && strict.boolValue();
        PyIter[] iters = new PyIter[args.length];
        for (int i = 0; i < args.length; i++) {
            iters[i] = args[i].iter();
        }
        return new PyZip(iters, strictBool);
    }

    @Override public PyTuple next() {
        if (iters.length == 0) {
            return null; // special case: zip of no iterators yields nothing
        }
        var array = new PyObject[iters.length];
        for (int i = 0; i < iters.length; i++) {
            var item = iters[i].next();
            if (item == null) {
                if (!strict) {
                    return null;
                }
                if (i != 0) {
                    throw PyValueError.raiseFormat("zip() argument %d is shorter than argument 1", i + 1);
                }
                for (int j = 1; j < iters.length; j++) {
                    if (iters[j].next() != null) {
                        throw PyValueError.raiseFormat("zip() argument %d is longer than argument 1", j + 1);
                    }
                }
                return null;
            }
            array[i] = item;
        }
        return new PyTuple(array);
    }

    @Override public boolean contains(PyObject rhs) { return defaultContains(rhs); }
    @Override public String repr() { return defaultRepr(); }
    @Override public PyConcreteType type() { return PyZipType.singleton; }
}
