// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyNone extends PyObject {
    public static final PyNone singleton = new PyNone();

    private PyNone() {}

    private static final PyBuiltinClass class_singleton = new PyBuiltinClass("NoneType");
    @Override public PyBuiltinClass type() { return class_singleton; }

    @Override public boolean boolValue() { return false; }
    @Override public boolean equals(Object rhs) { return this == rhs; } // always works because this is a singleton
    @Override public int hashCode() { return 0; }
    @Override public String repr() { return "None"; }
}
