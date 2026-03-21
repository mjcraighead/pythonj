// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

final class PyReversedType extends PyBuiltinClass {
    public static final PyReversedType singleton = new PyReversedType();

    PyReversedType() { super("reversed", PyReversed.class); }
    @Override public PyIter call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        Runtime.requireExactArgs(args, 1, typeName);
        return args[0].reversed();
    }
}

public final class PyReversed extends PyIter {
    private final PyObject obj;
    private long i = 0;
    private final long len;

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
    @Override public String repr() { return defaultRepr(); }
    @Override public PyBuiltinClass type() { return PyReversedType.singleton; }
}
