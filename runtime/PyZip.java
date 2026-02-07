// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.ArrayList;

public final class PyZip extends PyIter {
    public final PyIter[] iters;

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
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_zip; }
}
