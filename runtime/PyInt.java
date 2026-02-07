// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

// XXX Should probably pre-intern [-5,256] to match CPython
public final class PyInt extends PyNumber {
    public static final PyInt singleton_0 = new PyInt(0);
    public static final PyInt singleton_1 = new PyInt(1);

    public final long value;

    PyInt(long _value) { value = _value; }

    @Override public PyInt invert() { return new PyInt(~value); }
    @Override public PyInt pos() { return this; }
    @Override public PyInt neg() { return new PyInt(-value); }
    @Override public PyInt abs() { return (value >= 0) ? this : new PyInt(-value); }

    // XXX Maybe we should use Math.addExact and similar
    @Override public PyInt add(PyObject rhs) {
        return new PyInt(value + ((PyInt)rhs).value);
    }
    @Override public PyInt and(PyObject rhs) {
        return new PyInt(value & ((PyInt)rhs).value);
    }
    @Override public PyInt floordiv(PyObject rhs) {
        return new PyInt(Math.floorDiv(value, ((PyInt)rhs).value));
    }
    @Override public PyInt lshift(PyObject rhs) {
        // XXX Do we need to check for rhs_value >= 64?
        long rhs_value = ((PyInt)rhs).value;
        if (rhs_value < 0) {
            throw new RuntimeException("negative shift count");
        }
        return new PyInt(value << rhs_value);
    }
    @Override public PyInt mod(PyObject rhs) {
        return new PyInt(Math.floorMod(value, ((PyInt)rhs).value));
    }
    @Override public PyInt mul(PyObject rhs) {
        return new PyInt(value * ((PyInt)rhs).value);
    }
    @Override public PyInt or(PyObject rhs) {
        return new PyInt(value | ((PyInt)rhs).value);
    }
    @Override public PyInt rshift(PyObject rhs) {
        // XXX Do we need to check for rhs_value >= 64?
        long rhs_value = ((PyInt)rhs).value;
        if (rhs_value < 0) {
            throw new RuntimeException("negative shift count");
        }
        return new PyInt(value >> rhs_value);
    }
    @Override public PyInt sub(PyObject rhs) {
        return new PyInt(value - ((PyInt)rhs).value);
    }
    @Override public PyInt xor(PyObject rhs) {
        return new PyInt(value ^ ((PyInt)rhs).value);
    }

    @Override public boolean ge(PyObject rhs) {
        return value >= ((PyInt)rhs).value;
    }
    @Override public boolean gt(PyObject rhs) {
        return value > ((PyInt)rhs).value;
    }
    @Override public boolean le(PyObject rhs) {
        return value <= ((PyInt)rhs).value;
    }
    @Override public boolean lt(PyObject rhs) {
        return value < ((PyInt)rhs).value;
    }

    @Override public PyBuiltinClass type() { return Runtime.pyglobal_int; }

    @Override public boolean boolValue() { return value != 0; }
    @Override public boolean equals(Object rhs) {
        if (!(rhs instanceof PyInt)) {
            return false;
        }
        return value == ((PyInt)rhs).value;
    }
    @Override public double floatValue() { return value; }
    @Override public int hashCode() { return (int)(value ^ (value >>> 32)); }
    @Override public long indexValue() { return value; }
    @Override public long intValue() { return value; }
    @Override public String repr() { return String.valueOf(value); }
}
