// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyNone extends PyObject {
    public static final PyNone singleton = new PyNone();

    private static final PyBuiltinClass class_singleton = new PyNoneType();
    private static final class PyNoneType extends PyBuiltinClass {
        PyNoneType() { super("NoneType", PyNone.class); }
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

    private PyNone() {}

    @Override public PyBuiltinClass type() { return class_singleton; }

    @Override public boolean boolValue() { return false; }
    @Override public boolean equals(Object rhs) { return this == rhs; } // always works because this is a singleton
    @Override public int hashCode() { return 0; }
    @Override public String repr() { return "None"; }
}
