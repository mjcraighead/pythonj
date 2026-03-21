// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

final class PyArithmeticErrorType extends PyBuiltinClass {
    public static final PyArithmeticErrorType singleton = new PyArithmeticErrorType();
    private PyArithmeticErrorType() { super("ArithmeticError", PyArithmeticError.class); }
    @Override public PyArithmeticError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyArithmeticError(args);
    }
}

final class PyAssertionErrorType extends PyBuiltinClass {
    public static final PyAssertionErrorType singleton = new PyAssertionErrorType();
    private PyAssertionErrorType() { super("AssertionError", PyAssertionError.class); }
    @Override public PyAssertionError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyAssertionError(args);
    }
}

final class PyAttributeErrorType extends PyBuiltinClass {
    public static final PyAttributeErrorType singleton = new PyAttributeErrorType();
    private PyAttributeErrorType() { super("AttributeError", PyAttributeError.class); }
    @Override public PyAttributeError call(PyObject[] args, PyDict kwargs) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("AttributeError() does not accept kwargs");
        }
        return new PyAttributeError(args);
    }
}

final class PyBaseExceptionType extends PyBuiltinClass {
    public static final PyBaseExceptionType singleton = new PyBaseExceptionType();
    private PyBaseExceptionType() { super("BaseException", PyBaseException.class); }
    @Override public PyBaseException call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyBaseException(args);
    }
}

final class PyExceptionType extends PyBuiltinClass {
    public static final PyExceptionType singleton = new PyExceptionType();
    private PyExceptionType() { super("Exception", PyException.class); }
    @Override public PyException call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyException(args);
    }
}

final class PyIndexErrorType extends PyBuiltinClass {
    public static final PyIndexErrorType singleton = new PyIndexErrorType();
    private PyIndexErrorType() { super("IndexError", PyIndexError.class); }
    @Override public PyIndexError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyIndexError(args);
    }
}

final class PyKeyErrorType extends PyBuiltinClass {
    public static final PyKeyErrorType singleton = new PyKeyErrorType();
    private PyKeyErrorType() { super("KeyError", PyKeyError.class); }
    @Override public PyKeyError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyKeyError(args);
    }
}

final class PyLookupErrorType extends PyBuiltinClass {
    public static final PyLookupErrorType singleton = new PyLookupErrorType();
    private PyLookupErrorType() { super("LookupError", PyLookupError.class); }
    @Override public PyLookupError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyLookupError(args);
    }
}

final class PyStopIterationType extends PyBuiltinClass {
    public static final PyStopIterationType singleton = new PyStopIterationType();
    private PyStopIterationType() { super("StopIteration", PyStopIteration.class); }
    @Override public PyStopIteration call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyStopIteration(args);
    }
}

final class PyTypeErrorType extends PyBuiltinClass {
    public static final PyTypeErrorType singleton = new PyTypeErrorType();
    private PyTypeErrorType() { super("TypeError", PyTypeError.class); }
    @Override public PyTypeError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyTypeError(args);
    }
}

final class PyValueErrorType extends PyBuiltinClass {
    public static final PyValueErrorType singleton = new PyValueErrorType();
    private PyValueErrorType() { super("ValueError", PyValueError.class); }
    @Override public PyValueError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyValueError(args);
    }
}

final class PyZeroDivisionErrorType extends PyBuiltinClass {
    public static final PyZeroDivisionErrorType singleton = new PyZeroDivisionErrorType();
    private PyZeroDivisionErrorType() { super("ZeroDivisionError", PyZeroDivisionError.class); }
    @Override public PyZeroDivisionError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyZeroDivisionError(args);
    }
}

class PyBaseException extends PyTruthyObject {
    protected final PyObject[] args;
    PyBaseException(PyObject[] _args) { args = _args; }
    @Override public PyBuiltinClass type() { return PyBaseExceptionType.singleton; }

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
    @Override public PyBuiltinClass type() { return PyExceptionType.singleton; }
}

final class PyAssertionError extends PyException {
    PyAssertionError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return PyAssertionErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyAssertionError(new PyString(msg)));
    }
}

final class PyAttributeError extends PyException {
    PyAttributeError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return PyAttributeErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyAttributeError(new PyString(msg)));
    }
    static PyRaise raiseFormat(String fmt, Object... args) {
        return new PyRaise(new PyAttributeError(new PyString(String.format(fmt, args))));
    }
}

class PyArithmeticError extends PyException {
    PyArithmeticError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return PyArithmeticErrorType.singleton; }
}
final class PyZeroDivisionError extends PyArithmeticError {
    PyZeroDivisionError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return PyZeroDivisionErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyZeroDivisionError(new PyString(msg)));
    }
}

class PyLookupError extends PyException {
    PyLookupError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return PyLookupErrorType.singleton; }
}
final class PyIndexError extends PyLookupError {
    PyIndexError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return PyIndexErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyIndexError(new PyString(msg)));
    }
}
final class PyKeyError extends PyLookupError {
    PyKeyError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return PyKeyErrorType.singleton; }
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
    @Override public PyBuiltinClass type() { return PyStopIterationType.singleton; }
}

final class PyTypeError extends PyException {
    PyTypeError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return PyTypeErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyTypeError(new PyString(msg)));
    }
    static PyRaise raiseFormat(String fmt, Object... args) {
        return new PyRaise(new PyTypeError(new PyString(String.format(fmt, args))));
    }
}

final class PyValueError extends PyException {
    PyValueError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return PyValueErrorType.singleton; }

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
