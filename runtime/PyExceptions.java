// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

abstract class PyBaseException extends PyTruthyObject {
    protected final PyObject[] args;
    PyBaseException(PyObject[] _args) { args = _args; }
}

abstract class PyException extends PyBaseException {
    PyException(PyObject[] _args) { super(_args); }
}

final class PyRaise extends RuntimeException {
    final PyBaseException exc;
    PyRaise(PyBaseException _exc) { exc = _exc; }
    @Override public String toString() { return "PyRaise(" + exc.repr() + ")"; }
}
