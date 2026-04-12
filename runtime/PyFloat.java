// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.nio.charset.StandardCharsets;

public final class PyFloat extends PyObject {
    public static final PyFloat nan_singleton = new PyFloat(Double.NaN);

    public final double value;

    PyFloat(double _value) { value = _value; }

    public static double addUnboxed(double lhs, double rhs) { return lhs + rhs; }
    public static double mulUnboxed(double lhs, double rhs) { return lhs * rhs; }
    public static double subUnboxed(double lhs, double rhs) { return lhs - rhs; }

    private static PyFloat parseString(String s, String repr) {
        String sl = s.toLowerCase();
        if (sl.equals("inf") || sl.equals("+inf") || sl.equals("infinity") || sl.equals("+infinity")) {
            return new PyFloat(Double.POSITIVE_INFINITY);
        } else if (sl.equals("-inf") || sl.equals("-infinity")) {
            return new PyFloat(Double.NEGATIVE_INFINITY);
        } else if (sl.equals("nan") || sl.equals("+nan") || sl.equals("-nan")) {
            return nan_singleton;
        }
        try {
            return new PyFloat(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            throw PyValueError.raise("could not convert string to float: " + repr);
        }
    }
    private static double floatLikeValue(PyObject obj) {
        if ((obj instanceof PyFloat) || (obj instanceof PyInt) || (obj instanceof PyBool)) {
            return obj.floatValue();
        }
        throw new UnsupportedOperationException("not a float-like object");
    }
    private static PyFloat add(double lhs, double rhs) { return new PyFloat(lhs + rhs); }
    private static PyFloat sub(double lhs, double rhs) { return new PyFloat(lhs - rhs); }
    private static PyFloat mul(double lhs, double rhs) { return new PyFloat(lhs * rhs); }
    private static PyFloat trueDiv(double lhs, double rhs) {
        if (rhs == 0.0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        return new PyFloat(lhs / rhs);
    }
    private static PyFloat floorDiv(double lhs, double rhs) {
        if (rhs == 0.0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        return new PyFloat(Math.floor(lhs / rhs));
    }
    private static PyFloat mod(double lhs, double rhs) {
        if (rhs == 0.0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        double ret = lhs - (Math.floor(lhs / rhs) * rhs);
        if (ret == 0.0) {
            ret = Math.copySign(0.0, rhs);
        }
        return new PyFloat(ret);
    }
    private static PyFloat pow(double lhs, double rhs) {
        if ((lhs == 0.0) && (rhs < 0.0)) {
            throw PyZeroDivisionError.raise("zero to a negative power");
        }
        double ret = Math.pow(lhs, rhs);
        if (Double.isNaN(ret)) {
            throw new UnsupportedOperationException("float pow is unimplemented for this input");
        }
        return new PyFloat(ret);
    }
    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireMinMaxPositional(args, kwargs, type.name(), 0, 1);
        return newObjPositional((args.length == 0) ? null : args[0]);
    }
    public static PyFloat newObjPositional(PyObject arg) {
        if (arg == null) {
            return new PyFloat(0.0);
        }
        if (arg instanceof PyFloat argFloat) {
            return argFloat;
        } else if (arg.hasIndex()) {
            return new PyFloat(arg.indexValue());
        } else if (arg instanceof PyString argStr) {
            return parseString(argStr.value, PyString.reprOf(argStr.value));
        } else {
            byte[] argBuffer = Runtime.getBytesLikeBuffer(arg);
            if (argBuffer == null) {
                throw PyTypeError.raise("float() argument must be a string or a real number, not " + PyString.reprOf(arg.type().name()));
            }
            String s = new String(argBuffer, StandardCharsets.ISO_8859_1);
            return parseString(s, arg.repr());
        }
    }

    @Override public PyFloat pos() { return this; }
    @Override public PyFloat neg() { return new PyFloat(-value); }
    @Override public PyFloat abs() { return new PyFloat(Math.abs(value)); }
    @Override public PyObject add(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return add(value, floatLikeValue(rhs));
        } else {
            return super.add(rhs);
        }
    }
    @Override public PyObject floorDiv(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return floorDiv(value, floatLikeValue(rhs));
        } else {
            return super.floorDiv(rhs);
        }
    }
    @Override public PyObject mod(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return mod(value, floatLikeValue(rhs));
        } else {
            return super.mod(rhs);
        }
    }
    @Override public PyObject mul(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return mul(value, floatLikeValue(rhs));
        } else {
            return super.mul(rhs);
        }
    }
    @Override public PyObject pow(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return pow(value, floatLikeValue(rhs));
        } else {
            return super.pow(rhs);
        }
    }
    @Override public PyObject radd(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return add(floatLikeValue(rhs), value);
        } else {
            return super.radd(rhs);
        }
    }
    @Override public PyObject rfloorDiv(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return floorDiv(floatLikeValue(rhs), value);
        } else {
            return super.rfloorDiv(rhs);
        }
    }
    @Override public PyObject rmod(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return mod(floatLikeValue(rhs), value);
        } else {
            return super.rmod(rhs);
        }
    }
    @Override public PyObject rmul(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return mul(floatLikeValue(rhs), value);
        } else {
            return super.rmul(rhs);
        }
    }
    @Override public PyObject rpow(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return pow(floatLikeValue(rhs), value);
        } else {
            return super.rpow(rhs);
        }
    }
    @Override public PyObject rsub(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return sub(floatLikeValue(rhs), value);
        } else {
            return super.rsub(rhs);
        }
    }
    @Override public PyObject rtrueDiv(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return trueDiv(floatLikeValue(rhs), value);
        } else {
            return super.rtrueDiv(rhs);
        }
    }
    @Override public PyObject sub(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return sub(value, floatLikeValue(rhs));
        } else {
            return super.sub(rhs);
        }
    }
    @Override public PyObject trueDiv(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return trueDiv(value, floatLikeValue(rhs));
        } else {
            return super.trueDiv(rhs);
        }
    }
    @Override public final boolean boolValue() { return value != 0.0; }
    @Override public boolean equals(Object rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return value == floatLikeValue((PyObject)rhs);
        }
        return false;
    }
    @Override public double floatValue() { return value; }
    @Override public int hashCode() {
        if (Double.isFinite(value) && (value >= Long.MIN_VALUE) && (value <= Long.MAX_VALUE)) {
            long longValue = (long)value;
            if (value == (double)longValue) {
                return Long.hashCode(longValue);
            }
        }
        return Long.hashCode(Double.doubleToLongBits(value));
    }
    @Override public boolean ge(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return value >= floatLikeValue(rhs);
        } else {
            return super.ge(rhs);
        }
    }
    @Override public boolean gt(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return value > floatLikeValue(rhs);
        } else {
            return super.gt(rhs);
        }
    }
    @Override public boolean le(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return value <= floatLikeValue(rhs);
        } else {
            return super.le(rhs);
        }
    }
    @Override public boolean lt(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return value < floatLikeValue(rhs);
        } else {
            return super.lt(rhs);
        }
    }
    @Override public String repr() { return PyRuntime.pyfunc_float____repr__(this).value; }
    @Override public String str() { return PyRuntime.pyfunc_pyj_float_str(this).value; }
    @Override public PyConcreteType type() { return PyFloatType.singleton; }
    @Override public String format(String formatSpec) {
        return PyRuntime.pyfunc_float____format__(this, new PyString(formatSpec)).value;
    }
}
