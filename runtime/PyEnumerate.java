// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyEnumerate extends PyIter {
    public final PyObject iter;
    public long i = 0;

    PyEnumerate(PyObject _iter) { iter = _iter; }

    @Override public PyTuple next() {
        var item = iter.next();
        if (item == null) {
            return null;
        }
        return new PyTuple(new PyObject[] {new PyInt(i++), item});
    }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_enumerate; }
}
