// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

// BEGIN GENERATED CODE: PyNoneType
final class PyNoneType extends PyBuiltinType {
    public static final PyNoneType singleton = new PyNoneType();
    private static final PyString pyattr___doc__ = new PyString("The type of the None singleton.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyNoneType() { super("NoneType", PyNone.class, PyNone::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyNoneType

public final class PyNone extends PyObject {
    public static final PyNone singleton = new PyNone();

    private PyNone() {}

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw PyTypeError.raise("NoneType takes no arguments");
        }
        if (args.length != 0) {
            throw PyTypeError.raise("NoneType takes no arguments");
        }
        return PyNone.singleton;
    }

    @Override public PyObject or(PyObject rhs) {
        if (rhs instanceof PyType) {
            throw new UnsupportedOperationException("type unions are unsupported");
        } else {
            return super.or(rhs);
        }
    }

    @Override public PyBuiltinType type() { return PyNoneType.singleton; }

    @Override public boolean boolValue() { return false; }
    @Override public boolean contains(PyObject rhs) { return defaultContains(rhs); }
    @Override public boolean equals(Object rhs) { return this == rhs; } // always works because this is a singleton
    @Override public int hashCode() { return 0; }
    @Override public String repr() { return "None"; }
}
