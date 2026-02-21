// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyEnumerate extends PyIter {
    public final PyObject iter;
    public long i = 0;

    PyEnumerate(PyObject _iter, long start) {
        iter = _iter;
        i = start;
    }

    @Override public PyTuple next() {
        var item = iter.next();
        if (item == null) {
            return null;
        }
        return new PyTuple(new PyObject[] {new PyInt(i++), item});
    }
    @Override public String repr() { return defaultRepr(); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_enumerate; }
}
