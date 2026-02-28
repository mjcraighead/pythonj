// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyEnumerate extends PyIter {
    private final PyObject iter;
    private long i;

    PyEnumerate(PyObject _iter, long start) {
        iter = _iter;
        i = start;
    }

    @Override public PyTuple next() {
        var item = iter.next();
        if (item == null) {
            return null;
        }
        var ret = new PyTuple(new PyObject[] {new PyInt(i), item});
        i = Math.incrementExact(i);
        return ret;
    }
    @Override public String repr() { return defaultRepr(); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_enumerate; }
}
