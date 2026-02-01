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
        // XXX Might be nice for this to use PyObject[], but no PyTuple constructor for that
        var list = new ArrayList<PyObject>();
        for (int i = 0; i < iters.length; i++) {
            var item = iters[i].next();
            if (item == null) {
                return null; // XXX understand corner cases with iterators of different lengths
            }
            list.add(item);
        }
        return new PyTuple(list);
    }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_zip; }
}
