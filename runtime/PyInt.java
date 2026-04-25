// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.io.ByteArrayOutputStream;

// XXX Should probably pre-intern [-5,256] to match CPython
public final class PyInt extends PyObject {
    public static final PyInt singleton_neg1 = new PyInt(-1);
    public static final PyInt singleton_0 = new PyInt(0);
    public static final PyInt singleton_1 = new PyInt(1);

    public final long value;

    PyInt(long _value) { value = _value; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireMinMaxPositional(args, kwargs, type.name(), 0, 2);
        PyObject arg0 = (args.length >= 1) ? args[0] : null;
        PyObject arg1 = (args.length >= 2) ? args[1] : null;
        return newObjPositional(arg0, arg1);
    }
    public static PyInt newObjPositional(PyObject arg0, PyObject arg1) {
        if (arg0 == null) {
            return PyInt.singleton_0;
        }
        // XXX should also try intValue when length is 1
        if (arg0.hasIndex()) {
            if (arg1 != null) {
                throw PyTypeError.raise("int() can't convert non-string with explicit base");
            }
            return new PyInt(arg0.indexValue());
        }
        if (arg0 instanceof PyFloat arg0Float) {
            if (arg1 != null) {
                throw PyTypeError.raise("int() can't convert non-string with explicit base");
            }
            double value = arg0Float.value;
            if (Double.isNaN(value)) {
                throw PyValueError.raise("cannot convert float NaN to integer");
            }
            if (Double.isInfinite(value)) {
                throw PyOverflowError.raise("cannot convert float infinity to integer");
            }
            if ((value > Long.MAX_VALUE) || (value < Long.MIN_VALUE)) {
                throw PyOverflowError.raise("Python int too large to convert to C long");
            }
            return new PyInt((long)value);
        }
        if (arg0 instanceof PyString arg0Str) {
            long base = 10;
            if (arg1 != null) {
                base = arg1.indexValue();
                if ((base < 0) || (base == 1) || (base > 36)) {
                    throw PyValueError.raise("int() base must be >= 2 and <= 36, or 0");
                }
            }
            return PyRuntime.pyfunc_pyj_int_parse_stringlike(new PyString(arg0Str.value), arg0, new PyInt(base));
        }
        byte[] arg0Buffer = Runtime.getBytesLikeBuffer(arg0);
        if (arg0Buffer != null) {
            long base = (arg1 != null) ? arg1.indexValue() : 10;
            if ((base < 0) || (base == 1) || (base > 36)) {
                throw PyValueError.raise("int() base must be >= 2 and <= 36, or 0");
            }
            return PyRuntime.pyfunc_pyj_int_parse_stringlike(new PyString(new String(arg0Buffer)), arg0, new PyInt(base));
        }
        throw PyTypeError.raise("int() argument must be a string, a bytes-like object or a number, not " + PyString.reprOf(arg0.type().name()));
    }

    // Some of these wrappers may seem a bit pointless, but it's important to be very careful here about int vs. long
    public static long addUnboxed(long lhs, long rhs) {
        return Math.addExact(lhs, rhs);
    }
    public static long andUnboxed(long lhs, long rhs) {
        return lhs & rhs;
    }
    public static long floorDivUnboxed(long lhs, long rhs) {
        if ((lhs == Long.MIN_VALUE) && (rhs == -1)) {
            throw new ArithmeticException("integer overflow");
        }
        if (rhs == 0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        return Math.floorDiv(lhs, rhs);
    }
    public static long lshiftUnboxed(long lhs, long rhs) {
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
    public static long modUnboxed(long lhs, long rhs) {
        if ((lhs == Long.MIN_VALUE) && (rhs == -1)) {
            throw new ArithmeticException("integer overflow");
        }
        if (rhs == 0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        return Math.floorMod(lhs, rhs);
    }
    public static long mulUnboxed(long lhs, long rhs) {
        return Math.multiplyExact(lhs, rhs);
    }
    public static long orUnboxed(long lhs, long rhs) {
        return lhs | rhs;
    }
    public static PyObject pow(long lhs, long rhs) {
        if (rhs < 0) {
            throw new ArithmeticException("negative exponent");
        }
        long ret = 1; // note 0 ** 0 -> 1
        while (rhs > 0) {
            if ((rhs & 1) == 1) {
                ret = Math.multiplyExact(ret, lhs);
            }
            lhs = Math.multiplyExact(lhs, lhs);
            rhs >>= 1;
        }
        return new PyInt(ret);
    }
    public static long rshiftUnboxed(long lhs, long rhs) {
        if (rhs < 0) {
            throw PyValueError.raise("negative shift count");
        }
        if (rhs > 63) {
            rhs = 63; // for all longs, rshift of >=63 yields the sign bit replicated into all bits
        }
        return lhs >> rhs;
    }
    public static long subUnboxed(long lhs, long rhs) {
        return Math.subtractExact(lhs, rhs);
    }
    public static double trueDivUnboxed(long lhs, long rhs) {
        if (rhs == 0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        return ((double)lhs) / rhs;
    }
    public static long xorUnboxed(long lhs, long rhs) {
        return lhs ^ rhs;
    }

    // XXX Make sure that all of these throw an exception if the value wraps/overflows/etc.
    @Override public PyInt invert() { return new PyInt(~value); }
    @Override public PyInt pos() { return this; }
    @Override public PyInt neg() { return new PyInt(Math.negateExact(value)); }

    @Override public PyObject add(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(addUnboxed(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(addUnboxed(value, rhsBool.asInt()));
        } else {
            return super.add(rhs);
        }
    }
    @Override public PyObject and(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(andUnboxed(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(andUnboxed(value, rhsBool.asInt()));
        } else {
            return super.and(rhs);
        }
    }
    @Override public PyObject floorDiv(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(floorDivUnboxed(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(floorDivUnboxed(value, rhsBool.asInt()));
        } else {
            return super.floorDiv(rhs);
        }
    }
    @Override public PyObject lshift(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(lshiftUnboxed(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(lshiftUnboxed(value, rhsBool.asInt()));
        } else {
            return super.lshift(rhs);
        }
    }
    @Override public PyObject mod(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(modUnboxed(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(modUnboxed(value, rhsBool.asInt()));
        } else {
            return super.mod(rhs);
        }
    }
    @Override public PyObject mul(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(mulUnboxed(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(mulUnboxed(value, rhsBool.asInt()));
        } else {
            return super.mul(rhs);
        }
    }
    @Override public PyObject or(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(orUnboxed(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(orUnboxed(value, rhsBool.asInt()));
        } else {
            return super.or(rhs);
        }
    }
    @Override public PyObject rawPow(PyObject rhs, PyObject mod) {
        if (mod != PyNone.singleton) {
            throw new UnsupportedOperationException("int.__pow__() with mod is unimplemented");
        }
        if (rhs instanceof PyInt rhsInt) {
            return pow(value, rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return pow(value, rhsBool.asInt());
        } else {
            return PyNotImplemented.singleton;
        }
    }
    @Override public PyObject rawRPow(PyObject lhs, PyObject mod) {
        if (mod != PyNone.singleton) {
            throw new UnsupportedOperationException("int.__rpow__() with mod is unimplemented");
        }
        if (lhs instanceof PyInt lhsInt) {
            return pow(lhsInt.value, value);
        } else if (lhs instanceof PyBool lhsBool) {
            return pow(lhsBool.asInt(), value);
        } else {
            return PyNotImplemented.singleton;
        }
    }
    @Override public PyObject rshift(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(rshiftUnboxed(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(rshiftUnboxed(value, rhsBool.asInt()));
        } else {
            return super.rshift(rhs);
        }
    }
    @Override public PyObject sub(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(subUnboxed(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(subUnboxed(value, rhsBool.asInt()));
        } else {
            return super.sub(rhs);
        }
    }
    @Override public PyObject trueDiv(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyFloat(trueDivUnboxed(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyFloat(trueDivUnboxed(value, rhsBool.asInt()));
        } else {
            return super.trueDiv(rhs);
        }
    }
    @Override public PyObject xor(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(xorUnboxed(value, rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(xorUnboxed(value, rhsBool.asInt()));
        } else {
            return super.xor(rhs);
        }
    }

    @Override public boolean ge(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value >= rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value >= rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return value >= rhsFloat.value;
        } else {
            return super.ge(rhs);
        }
    }
    @Override public boolean gt(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value > rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value > rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return value > rhsFloat.value;
        } else {
            return super.gt(rhs);
        }
    }
    @Override public boolean le(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value <= rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value <= rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return value <= rhsFloat.value;
        } else {
            return super.le(rhs);
        }
    }
    @Override public boolean lt(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value < rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value < rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return value < rhsFloat.value;
        } else {
            return super.lt(rhs);
        }
    }

    @Override public PyConcreteType type() { return PyIntType.singleton; }

    @Override public boolean boolValue() { return value != 0; }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return value == rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return value == rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return floatValue() == rhsFloat.value;
        } else {
            return false;
        }
    }
    @Override public double floatValue() { return value; }
    @Override public int hashCode() { return Long.hashCode(value); }
    @Override public boolean hasIndex() { return true; }
    @Override public long indexValue() { return value; }
    @Override public long intValue() { return value; }
    @Override public String repr() { return slotBasedRepr(); }
}
