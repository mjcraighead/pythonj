// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyFloat extends PyObject {
    public final double value;

    PyFloat(double _value) { value = _value; }

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

    @Override public final boolean boolValue() { return value != 0.0; }
    @Override public boolean equals(Object rhs) {
        return (rhs instanceof PyFloat rhsFloat) && (Double.compare(value, rhsFloat.value) == 0);
    }
    @Override public double floatValue() { return value; }
    @Override public int hashCode() { throw new UnsupportedOperationException(); }
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
