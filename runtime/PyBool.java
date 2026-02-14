// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyBool extends PyObject {
    public static final PyBool false_singleton = new PyBool(false);
    public static final PyBool true_singleton = new PyBool(true);

    public final boolean value;

    private PyBool(boolean _value) { value = _value; }
    public static PyBool create(boolean value) {
        return value ? true_singleton : false_singleton;
    }

    private int asInt() { return value ? 1 : 0; }

    @Override public PyInt pos() { return new PyInt(asInt()); }
    @Override public PyInt neg() { return new PyInt(value ? -1 : 0); }
    @Override public PyInt abs() { return new PyInt(asInt()); }

    @Override public PyBool and(PyObject rhs) {
        return create(value & ((PyBool)rhs).value);
    }
    @Override public PyBool or(PyObject rhs) {
        return create(value | ((PyBool)rhs).value);
    }
    @Override public PyBool xor(PyObject rhs) {
        return create(value ^ ((PyBool)rhs).value);
    }

    @Override public boolean ge(PyObject rhs) { return asInt() >= ((PyBool)rhs).asInt(); }
    @Override public boolean gt(PyObject rhs) { return asInt() > ((PyBool)rhs).asInt(); }
    @Override public boolean le(PyObject rhs) { return asInt() <= ((PyBool)rhs).asInt(); }
    @Override public boolean lt(PyObject rhs) { return asInt() < ((PyBool)rhs).asInt(); }

    @Override public Runtime.pyfunc_bool type() { return Runtime.pyglobal_bool; }

    @Override public boolean boolValue() { return value; }
    @Override public boolean equals(Object rhs) { return value == ((PyBool)rhs).value; }
    @Override public double floatValue() { return asInt(); }
    @Override public int hashCode() { return value ? 1 : 0; }
    @Override public boolean hasIndex() { return true; }
    @Override public long indexValue() { return asInt(); }
    @Override public long intValue() { return asInt(); }
    @Override public String repr() { return value ? "True" : "False"; }
}
