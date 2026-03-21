// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

class PyBaseException extends PyTruthyObject {
    protected final PyObject[] args;
    PyBaseException(PyObject[] _args) { args = _args; }
    @Override public PyBuiltinClass type() { return Runtime.pyclass_BaseException.singleton; }

    @Override public boolean contains(PyObject rhs) { return defaultContains(rhs); }
    @Override public String str() {
        if (args.length == 0) {
            return "";
        }
        if (args.length == 1) {
            return args[0].str();
        }
        StringBuilder s = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            if (i != 0) {
                s.append(", ");
            }
            s.append(args[i].repr());
        }
        return s.append(")").toString();
    }
    @Override public String repr() {
        StringBuilder s = new StringBuilder(type().name()).append("(");
        for (int i = 0; i < args.length; i++) {
            if (i != 0) {
                s.append(", ");
            }
            s.append(args[i].repr());
        }
        return s.append(")").toString();
    }
}

class PyException extends PyBaseException {
    PyException(PyObject[] _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyclass_Exception.singleton; }
}

final class PyAssertionError extends PyException {
    PyAssertionError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyclass_AssertionError.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyAssertionError(new PyString(msg)));
    }
}

final class PyAttributeError extends PyException {
    PyAttributeError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyclass_AttributeError.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyAttributeError(new PyString(msg)));
    }
    static PyRaise raiseFormat(String fmt, Object... args) {
        return new PyRaise(new PyAttributeError(new PyString(String.format(fmt, args))));
    }
}

class PyArithmeticError extends PyException {
    PyArithmeticError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyclass_ArithmeticError.singleton; }
}
final class PyZeroDivisionError extends PyArithmeticError {
    PyZeroDivisionError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyclass_ZeroDivisionError.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyZeroDivisionError(new PyString(msg)));
    }
}

class PyLookupError extends PyException {
    PyLookupError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyclass_LookupError.singleton; }
}
final class PyIndexError extends PyLookupError {
    PyIndexError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyclass_IndexError.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyIndexError(new PyString(msg)));
    }
}
final class PyKeyError extends PyLookupError {
    PyKeyError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyclass_KeyError.singleton; }
    @Override public String str() { // special case for KeyError
        if (args.length == 1) {
            return args[0].repr();
        } else {
            return super.str();
        }
    }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyKeyError(new PyString(msg)));
    }
}

final class PyStopIteration extends PyException {
    PyStopIteration(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyclass_StopIteration.singleton; }
}

final class PyTypeError extends PyException {
    PyTypeError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyclass_TypeError.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyTypeError(new PyString(msg)));
    }
    static PyRaise raiseFormat(String fmt, Object... args) {
        return new PyRaise(new PyTypeError(new PyString(String.format(fmt, args))));
    }
}

final class PyValueError extends PyException {
    PyValueError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyclass_ValueError.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyValueError(new PyString(msg)));
    }
    static PyRaise raiseFormat(String fmt, Object... args) {
        return new PyRaise(new PyValueError(new PyString(String.format(fmt, args))));
    }
}

final class PyRaise extends RuntimeException {
    final PyBaseException exc;
    PyRaise(PyBaseException _exc) { exc = _exc; }
    @Override public String toString() { return "PyRaise(" + exc.repr() + ")"; }
}
