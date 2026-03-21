// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

final class PyNoneType extends PyBuiltinClass {
    public static final PyBuiltinClass singleton = new PyNoneType();

    private PyNoneType() { super("NoneType", PyNone.class); }
    @Override public PyNone call(PyObject[] args, PyDict kwargs) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw PyTypeError.raise("NoneType takes no arguments");
        }
        if (args.length != 0) {
            throw PyTypeError.raise("NoneType takes no arguments");
        }
        return PyNone.singleton;
    }
}

public final class PyNone extends PyObject {
    public static final PyNone singleton = new PyNone();

    private PyNone() {}

    @Override public PyObject or(PyObject rhs) {
        if (rhs instanceof PyType) {
            throw new UnsupportedOperationException("type unions are unsupported");
        } else {
            return super.or(rhs);
        }
    }

    @Override public PyBuiltinClass type() { return PyNoneType.singleton; }

    @Override public boolean boolValue() { return false; }
    @Override public boolean contains(PyObject rhs) { return defaultContains(rhs); }
    @Override public boolean equals(Object rhs) { return this == rhs; } // always works because this is a singleton
    @Override public int hashCode() { return 0; }
    @Override public String repr() { return "None"; }
}
