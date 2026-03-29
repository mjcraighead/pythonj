// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyFloat extends PyObject {
    public final double value;

    PyFloat(double _value) { value = _value; }

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

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        Runtime.requireMaxArgs(args, 1, type.name());
        if (args.length == 0) {
            return new PyFloat(0.0);
        }
        PyObject arg = args[0];
        if (arg instanceof PyFloat argFloat) {
            return argFloat;
        }
        if (arg.hasIndex()) {
            return new PyFloat(arg.indexValue());
        }
        if (arg instanceof PyString argStr) {
            try {
                return new PyFloat(Double.parseDouble(argStr.value));
            } catch (NumberFormatException e) {
                throw PyValueError.raise("could not convert string to float: " + PyString.reprOf(argStr.value));
            }
        }
        throw new UnsupportedOperationException("don't know how to handle argument to float()");
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
    @Override public int hashCode() { throw new UnsupportedOperationException(); }
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
    @Override public String repr() { return Double.toString(value); }
    @Override public String str() { return Double.toString(value); }
    @Override public PyBuiltinType type() { return PyFloatType.singleton; }

    public static PyObject pymethod_from_number(PyType self, PyObject number) {
        throw new UnsupportedOperationException();
    }
    public static PyObject pymethod_fromhex(PyType self, PyObject s) {
        throw new UnsupportedOperationException();
    }
    static PyObject pygetset_real(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pygetset_imag(PyObject obj) { throw new UnsupportedOperationException(); }
}
