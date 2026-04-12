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

    private static PyInt parseStringLike(String s, String repr, long baseObj) {
        long base = baseObj;
        int i = 0;
        int end = s.length();
        while ((i < end) && (s.charAt(i) == ' ')) {
            i++;
        }
        while ((end > i) && (s.charAt(end - 1) == ' ')) {
            end--;
        }
        long sign = 1;
        if (i < end) {
            if (s.charAt(i) == '-') {
                i++;
                sign = -1;
            } else if (s.charAt(i) == '+') {
                i++;
            }
        }
        if (i == end) {
            throw PyValueError.raiseFormat("invalid literal for int() with base %d: %s", base, repr);
        }
        if ((end - i) >= 2 && (s.charAt(i) == '0')) {
            char prefix = s.charAt(i + 1);
            if ((prefix == 'x') || (prefix == 'X')) {
                if ((base == 0) || (base == 16)) {
                    base = 16;
                    i += 2;
                }
            } else if ((prefix == 'o') || (prefix == 'O')) {
                if ((base == 0) || (base == 8)) {
                    base = 8;
                    i += 2;
                }
            } else if ((prefix == 'b') || (prefix == 'B')) {
                if ((base == 0) || (base == 2)) {
                    base = 2;
                    i += 2;
                }
            }
        }
        if (base == 0) {
            base = 10;
            if ((i < end) && (s.charAt(i) == '0') && ((end - i) > 1)) {
                boolean allZero = true;
                for (int j = i + 1; j < end; j++) {
                    if (s.charAt(j) != '0') {
                        allZero = false;
                        break;
                    }
                }
                if (!allZero) {
                    throw PyValueError.raiseFormat("invalid literal for int() with base %d: %s", baseObj, repr);
                }
            }
        }
        if (i == end) {
            throw PyValueError.raiseFormat("invalid literal for int() with base %d: %s", base, repr);
        }
        long value = 0;
        while (i < end) {
            long digit;
            char c = s.charAt(i++);
            if ((c >= '0') && (c <= '9')) {
                digit = c - '0';
            } else if ((c >= 'a') && (c <= 'z')) {
                digit = c - 'a' + 10;
            } else if ((c >= 'A') && (c <= 'Z')) {
                digit = c - 'A' + 10;
            } else {
                digit = 36;
            }
            if (digit >= base) {
                throw PyValueError.raiseFormat("invalid literal for int() with base %d: %s", base, repr);
            }
            value = Math.addExact(Math.multiplyExact(value, base), digit);
        }
        return new PyInt(Math.multiplyExact(sign, value));
    }

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
            return parseStringLike(arg0Str.value, PyString.reprOf(arg0Str.value), base);
        }
        byte[] arg0Buffer = Runtime.getBytesLikeBuffer(arg0);
        if (arg0Buffer != null) {
            long base = (arg1 != null) ? arg1.indexValue() : 10;
            if ((base < 0) || (base == 1) || (base > 36)) {
                throw PyValueError.raise("int() base must be >= 2 and <= 36, or 0");
            }
            return parseStringLike(new String(arg0Buffer), arg0.repr(), base);
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
    @Override public PyInt abs() { return (value >= 0) ? this : new PyInt(Math.negateExact(value)); }

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
    @Override public PyObject pow(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return pow(value, rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return pow(value, rhsBool.asInt());
        } else {
            return super.pow(rhs);
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
    @Override public boolean contains(PyObject rhs) { return defaultContains(rhs); }
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
    @Override public String format(String formatSpec) {
        return PyRuntime.pyfunc_int____format__(this, new PyString(formatSpec)).value;
    }
    @Override public int hashCode() { return Long.hashCode(value); }
    @Override public boolean hasIndex() { return true; }
    @Override public long indexValue() { return value; }
    @Override public long intValue() { return value; }
    @Override public String repr() { return PyRuntime.pyfunc_int____repr__(this).value; }
}
