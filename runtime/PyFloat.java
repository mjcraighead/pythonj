// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.nio.charset.StandardCharsets;

public final class PyFloat extends PyObject {
    public final double value;

    PyFloat(double _value) { value = _value; }

    private static long[] finiteIntegerRatio(double value) {
        long bits = Double.doubleToRawLongBits(value);
        boolean negative = bits < 0;
        int exponentBits = (int)((bits >>> 52) & 0x7FF);
        long numerator = bits & ((1L << 52) - 1);
        int exponent;
        if (exponentBits == 0) {
            exponent = -1022 - 52;
        } else {
            numerator |= (1L << 52);
            exponent = exponentBits - 1023 - 52;
        }
        while (((numerator & 1) == 0) && (exponent < 0)) {
            numerator >>= 1;
            exponent++;
        }
        long denominator = 1;
        if (exponent > 0) {
            numerator = PyInt.lshift(numerator, exponent);
        } else if (exponent < 0) {
            denominator = PyInt.lshift(1, -exponent);
        }
        if (negative) {
            numerator = Math.negateExact(numerator);
        }
        return new long[]{numerator, denominator};
    }
    private static PyFloat parseString(String s, String repr) {
        String sl = s.toLowerCase();
        if (sl.equals("inf") || sl.equals("+inf") || sl.equals("infinity") || sl.equals("+infinity")) {
            return new PyFloat(Double.POSITIVE_INFINITY);
        } else if (sl.equals("-inf") || sl.equals("-infinity")) {
            return new PyFloat(Double.NEGATIVE_INFINITY);
        } else if (sl.equals("nan") || sl.equals("+nan") || sl.equals("-nan")) {
            return new PyFloat(Double.NaN);
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
    private static String strOf(double value) {
        if (Double.isNaN(value)) {
            return "nan";
        } else if (value == Double.POSITIVE_INFINITY) {
            return "inf";
        } else if (value == Double.NEGATIVE_INFINITY) {
            return "-inf";
        } else {
            return Double.toString(value);
        }
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        Runtime.requireMaxArgs(args, 1, type.name());
        if (args.length == 0) {
            return new PyFloat(0.0);
        }
        PyObject arg = args[0];
        if (arg instanceof PyFloat argFloat) {
            return argFloat;
        } else if (arg.hasIndex()) {
            return new PyFloat(arg.indexValue());
        } else if (arg instanceof PyString argStr) {
            return parseString(argStr.value, PyString.reprOf(argStr.value));
        } else if (arg instanceof PyBytes argBytes) {
            String s = new String(argBytes.value, StandardCharsets.ISO_8859_1);
            return parseString(s, arg.repr());
        } else if (arg instanceof PyByteArray argByteArray) {
            String s = new String(argByteArray.value, StandardCharsets.ISO_8859_1);
            return parseString(s, arg.repr());
        } else {
            throw PyTypeError.raise("float() argument must be a string or a real number, not " + PyString.reprOf(arg.type().name()));
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
        if (Double.isNaN(value)) {
            throw new UnsupportedOperationException();
        } else if (value == Double.POSITIVE_INFINITY) {
            return 314159;
        } else if (value == Double.NEGATIVE_INFINITY) {
            return -314159;
        }
        long[] ratio = finiteIntegerRatio(value);
        return Runtime.hashRational(ratio[0], ratio[1]);
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
    @Override public String repr() { return strOf(value); }
    @Override public String str() { return strOf(value); }
    @Override public PyConcreteType type() { return PyFloatType.singleton; }

    public PyObject pymethod_as_integer_ratio() {
        if (Double.isNaN(value)) {
            throw PyValueError.raise("cannot convert NaN to integer ratio");
        } else if (Double.isInfinite(value)) {
            throw PyOverflowError.raise("cannot convert Infinity to integer ratio");
        }
        long[] ratio = finiteIntegerRatio(value);
        return new PyTuple(new PyObject[]{new PyInt(ratio[0]), new PyInt(ratio[1])});
    }
    public PyFloat pymethod_conjugate() {
        return this;
    }
    public static PyObject pymethod_from_number(PyType self, PyObject number) {
        if ((number instanceof PyFloat) || (number instanceof PyInt) || (number instanceof PyBool)) {
            return new PyFloat(number.floatValue());
        }
        throw new UnsupportedOperationException();
    }
    public static PyObject pymethod_fromhex(PyType self, PyObject s) {
        throw new UnsupportedOperationException();
    }
    public PyObject pymethod_hex() {
        throw new UnsupportedOperationException();
    }
    public PyBool pymethod_is_integer() {
        return PyBool.create(Double.isFinite(value) && (value == Math.rint(value)));
    }

    static PyObject pygetset_real(PyObject obj) { return obj; }
    static PyObject pygetset_imag(PyObject obj) { return new PyFloat(0.0); }
}
