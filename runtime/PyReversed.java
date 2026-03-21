// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

// BEGIN GENERATED CODE: PyReversedType
final class PyReversedType extends PyBuiltinType {
    public static final PyReversedType singleton = new PyReversedType();
    private static final PyString pyattr___doc__ = new PyString("Return a reverse iterator over the values of the given sequence.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyReversedType() { super("reversed", PyReversed.class, PyReversed::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyReversedType

public final class PyReversed extends PyIter {
    private final PyObject obj;
    private long i = 0;
    private final long len;

    PyReversed(PyObject _obj) {
        obj = _obj;
        len = _obj.len();
    }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
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
    @Override public PyBuiltinType type() { return PyReversedType.singleton; }
}
