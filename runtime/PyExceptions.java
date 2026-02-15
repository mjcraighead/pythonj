// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

abstract class PyBaseException extends PyTruthyObject {
    protected final PyObject[] args;
    PyBaseException(PyObject[] _args) { args = _args; }

    @Override public String repr() {
        String typeName = type().name();
        StringBuilder s = new StringBuilder();
        s.append(typeName).append("(");
        for (int i = 0; i < args.length; i++) {
            if (i != 0) {
                s.append(", ");
            }
            s.append(args[i].repr());
        }
        s.append(")");
        return s.toString();
    }
}

abstract class PyException extends PyBaseException {
    PyException(PyObject[] _args) { super(_args); }
}

final class PyAssertionError extends PyException {
    PyAssertionError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_AssertionError; }
}

class PyLookupError extends PyException {
    PyLookupError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_LookupError; }
}
final class PyIndexError extends PyLookupError {
    PyIndexError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_IndexError; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyIndexError(new PyString(msg)));
    }
}
final class PyKeyError extends PyLookupError {
    PyKeyError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_KeyError; }
}

final class PyStopIteration extends PyException {
    PyStopIteration(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_StopIteration; }
}

final class PyTypeError extends PyException {
    PyTypeError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_TypeError; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyTypeError(new PyString(msg)));
    }
    static PyRaise raiseFormat(String fmt, Object... args) {
        return new PyRaise(new PyTypeError(new PyString(String.format(fmt, args))));
    }
}

final class PyValueError extends PyException {
    PyValueError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_ValueError; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyValueError(new PyString(msg)));
    }
}

final class PyRaise extends RuntimeException {
    final PyBaseException exc;
    PyRaise(PyBaseException _exc) { exc = _exc; }
    @Override public String toString() { return "PyRaise(" + exc.repr() + ")"; }
}
