// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

// XXX Should probably pre-intern [-5,256] to match CPython
public final class PyInt extends PyNumber {
    public static final PyInt singleton_0 = new PyInt(0);
    public static final PyInt singleton_1 = new PyInt(1);

    public final long value;

    PyInt(long _value) { value = _value; }

    // XXX Make sure that all of these throw an exception if the value wraps/overflows/etc.
    @Override public PyInt invert() { return new PyInt(~value); }
    @Override public PyInt pos() { return this; }
    @Override public PyInt neg() { return new PyInt(Math.negateExact(value)); }
    @Override public PyInt abs() { return (value >= 0) ? this : new PyInt(Math.negateExact(value)); }

    @Override public PyInt add(PyObject rhs) {
        return new PyInt(Math.addExact(value, ((PyInt)rhs).value));
    }
    @Override public PyInt and(PyObject rhs) {
        return new PyInt(value & ((PyInt)rhs).value);
    }
    @Override public PyInt floordiv(PyObject rhs) {
        long rhs_value = ((PyInt)rhs).value;
        if ((value == Long.MIN_VALUE) && (rhs_value == -1)) {
            throw new ArithmeticException("integer overflow");
        }
        return new PyInt(Math.floorDiv(value, rhs_value));
    }
    @Override public PyInt lshift(PyObject rhs) {
        long rhs_value = ((PyInt)rhs).value;
        if (rhs_value < 0) {
            throw new ArithmeticException("negative shift count");
        }
        if (rhs_value >= 64) {
            if (value == 0) {
                return this; // 0 << N -> 0 for any N >= 0
            }
            throw new ArithmeticException("shift count too large");
        }
        long ret = value << rhs_value;
        if ((ret >> rhs_value) != value) {
            throw new ArithmeticException("integer overflow");
        }
        return new PyInt(ret);
    }
    @Override public PyInt mod(PyObject rhs) {
        long rhs_value = ((PyInt)rhs).value;
        if ((value == Long.MIN_VALUE) && (rhs_value == -1)) {
            throw new ArithmeticException("integer overflow");
        }
        return new PyInt(Math.floorMod(value, rhs_value));
    }
    @Override public PyInt mul(PyObject rhs) {
        return new PyInt(Math.multiplyExact(value, ((PyInt)rhs).value));
    }
    @Override public PyInt or(PyObject rhs) {
        return new PyInt(value | ((PyInt)rhs).value);
    }
    @Override public PyInt rshift(PyObject rhs) {
        long rhs_value = ((PyInt)rhs).value;
        if (rhs_value < 0) {
            throw new ArithmeticException("negative shift count");
        }
        if (rhs_value > 63) {
            rhs_value = 63; // for all longs, rshift of >=63 yields the sign bit replicated into all bits
        }
        return new PyInt(value >> rhs_value);
    }
    @Override public PyInt sub(PyObject rhs) {
        return new PyInt(Math.subtractExact(value, ((PyInt)rhs).value));
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
    @Override public String format(String formatSpec) {
        if (!formatSpec.isEmpty()) {
            throw new RuntimeException(String.format("formatSpec='%s' unimplemented", formatSpec));
        }
        return String.valueOf(value);
    }
    @Override public int hashCode() { return (int)(value ^ (value >>> 32)); }
    @Override public long indexValue() { return value; }
    @Override public long intValue() { return value; }
    @Override public String repr() { return String.valueOf(value); }
}
