// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyReversed extends PyIter {
    private final PyObject obj;
    private long i = 0;
    private final long len;

    PyReversed(PyObject _obj) {
        obj = _obj;
        len = _obj.len();
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        Runtime.requireExactArgs(args, 1, type.name());
        return args[0].reversed();
    }

    @Override public PyObject next() {
        if (i >= len) {
            return null;
        }
        long cur = i++;
        return obj.getItem(new PyInt(len - 1 - cur));
    }
    @Override public String repr() { return defaultRepr(); }
    @Override public PyConcreteType type() { return PyReversedType.singleton; }
}
