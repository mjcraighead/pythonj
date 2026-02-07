// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyReversed extends PyIter {
    public final PyObject obj;
    public long i = 0;
    public final long len;

    PyReversed(PyObject _obj) {
        obj = _obj;
        len = _obj.len();
    }

    @Override public PyObject next() {
        if (i >= len) {
            return null;
        }
        long cur = i++;
        return obj.getItem(new PyInt(len - 1 - cur));
    }
    @Override public String repr() { return "<reversed object>"; }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_reversed; }
}
