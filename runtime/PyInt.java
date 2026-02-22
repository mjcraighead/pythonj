// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

// XXX Should probably pre-intern [-5,256] to match CPython
public final class PyInt extends PyObject {
    public static final PyInt singleton_0 = new PyInt(0);
    public static final PyInt singleton_1 = new PyInt(1);

    public final long value;

    PyInt(long _value) { value = _value; }

    public static long floorDiv(long lhs, long rhs) {
        if ((lhs == Long.MIN_VALUE) && (rhs == -1)) {
            throw new ArithmeticException("integer overflow");
        }
        if (rhs == 0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        return Math.floorDiv(lhs, rhs);
    }
    public static long lshift(long lhs, long rhs) {
        if (rhs < 0) {
            throw PyValueError.raise("negative shift count");
        }
        if (rhs >= 64) {
            if (lhs == 0) {
                return 0; // 0 << N -> 0 for any N >= 0
            }
            throw new ArithmeticException("shift count too large");
        }
        long ret = lhs << rhs;
        if ((ret >> rhs) != lhs) {
            throw new ArithmeticException("integer overflow");
        }
        return ret;
    }
    public static long mod(long lhs, long rhs) {
        if ((lhs == Long.MIN_VALUE) && (rhs == -1)) {
            throw new ArithmeticException("integer overflow");
        }
        if (rhs == 0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        return Math.floorMod(lhs, rhs);
    }
    public static long rshift(long lhs, long rhs) {
        if (rhs < 0) {
            throw PyValueError.raise("negative shift count");
        }
        if (rhs > 63) {
            rhs = 63; // for all longs, rshift of >=63 yields the sign bit replicated into all bits
        }
        return lhs >> rhs;
    }

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
    @Override public PyObject floordiv(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(floorDiv(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(floorDiv(value, rhsBool.asInt()));
        } else {
            return super.floordiv(rhs);
        }
    }
    @Override public PyObject lshift(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(lshift(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(lshift(value, rhsBool.asInt()));
        } else {
            return super.lshift(rhs);
        }
    }
    @Override public PyObject mod(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(mod(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(mod(value, rhsBool.asInt()));
        } else {
            return super.mod(rhs);
        }
    }
    @Override public PyObject mul(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(Math.multiplyExact(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(value * rhsBool.asInt());
        } else {
            return super.mul(rhs);
        }
    }
    @Override public PyInt or(PyObject rhs) {
        return new PyInt(value | ((PyInt)rhs).value);
    }
    @Override public PyObject rshift(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(rshift(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(rshift(value, rhsBool.asInt()));
        } else {
            return super.rshift(rhs);
        }
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
    @Override public boolean equals(Object rhsArg) {
        if (rhsArg instanceof PyInt rhs) {
            return value == rhs.value;
        }
        return false;
    }
    @Override public double floatValue() { return value; }
    @Override public String format(String formatSpec) {
        if (formatSpec.isEmpty()) {
            return String.valueOf(value);
        } else if (formatSpec.equals("x")) {
            if (value < 0) {
                return String.format("-%x", Math.negateExact(value));
            } else {
                return String.format("%x", value);
            }
        } else {
            throw new UnsupportedOperationException(String.format("formatSpec=%s unimplemented", PyString.reprOf(formatSpec)));
        }
    }
    @Override public int hashCode() { return (int)(value ^ (value >>> 32)); }
    @Override public boolean hasIndex() { return true; }
    @Override public long indexValue() { return value; }
    @Override public long intValue() { return value; }
    @Override public String repr() { return String.valueOf(value); }
}
