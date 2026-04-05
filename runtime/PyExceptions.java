// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

class PyBaseException extends PyTruthyObject {
    protected final PyObject[] args;
    PyBaseException(PyObject[] _args) { args = _args; }
    @Override public PyConcreteType type() { return PyBaseExceptionType.singleton; }

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

    static PyObject pygetset_args(PyObject obj) { throw new UnsupportedOperationException(); }

    public PyObject pymethod_add_note(PyObject note) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_with_traceback(PyObject tb) { throw new UnsupportedOperationException(); }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyBaseException(args);
    }
}

class PyException extends PyBaseException {
    PyException(PyObject[] _args) { super(_args); }
    @Override public PyConcreteType type() { return PyExceptionType.singleton; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyException(args);
    }
}

final class PyAssertionError extends PyException {
    PyAssertionError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyAssertionErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyAssertionError(new PyString(msg)));
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyAssertionError(args);
    }
}

final class PyAttributeError extends PyException {
    PyAttributeError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyAttributeErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyAttributeError(new PyString(msg)));
    }
    static PyRaise raiseFormat(String fmt, Object... args) {
        return new PyRaise(new PyAttributeError(new PyString(String.format(fmt, args))));
    }

    static PyObject pymember_name(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pymember_obj(PyObject obj) { throw new UnsupportedOperationException(); }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        if ((kwargs != null) && kwargs.boolValue()) {
            long kwargsLen = kwargs.items.size();
            if (kwargsLen > 2) {
                throw Runtime.raiseAtMostKwArgs(type.name(), 2, args.length, kwargsLen);
            }
            for (var x: kwargs.items.entrySet()) {
                PyString kw = (PyString)x.getKey();
                if (!kw.value.equals("name") && !kw.value.equals("obj")) {
                    throw Runtime.raiseUnexpectedKwArg(type.name(), kw.value);
                }
            }
        }
        return new PyAttributeError(args);
    }
}

class PyArithmeticError extends PyException {
    PyArithmeticError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyArithmeticErrorType.singleton; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyArithmeticError(args);
    }
}
final class PyOverflowError extends PyArithmeticError {
    PyOverflowError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyOverflowErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyOverflowError(new PyString(msg)));
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyOverflowError(args);
    }
}
final class PyZeroDivisionError extends PyArithmeticError {
    PyZeroDivisionError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyZeroDivisionErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyZeroDivisionError(new PyString(msg)));
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyZeroDivisionError(args);
    }
}

class PyLookupError extends PyException {
    PyLookupError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyLookupErrorType.singleton; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyLookupError(args);
    }
}
final class PyIndexError extends PyLookupError {
    PyIndexError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyIndexErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyIndexError(new PyString(msg)));
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyIndexError(args);
    }
}
final class PyKeyError extends PyLookupError {
    PyKeyError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyKeyErrorType.singleton; }
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

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyKeyError(args);
    }
}

final class PyStopIteration extends PyException {
    PyStopIteration(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyStopIterationType.singleton; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyStopIteration(args);
    }

    static PyObject pymember_value(PyObject obj) { throw new UnsupportedOperationException(); }
}

final class PyRuntimeError extends PyException {
    PyRuntimeError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyRuntimeErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyRuntimeError(new PyString(msg)));
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyRuntimeError(args);
    }
}

final class PyTypeError extends PyException {
    PyTypeError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyTypeErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyTypeError(new PyString(msg)));
    }
    static PyRaise raiseFormat(String fmt, Object... args) {
        return new PyRaise(new PyTypeError(new PyString(String.format(fmt, args))));
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyTypeError(args);
    }
}

final class PyValueError extends PyException {
    PyValueError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyValueErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyValueError(new PyString(msg)));
    }
    static PyRaise raiseFormat(String fmt, Object... args) {
        return new PyRaise(new PyValueError(new PyString(String.format(fmt, args))));
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyValueError(args);
    }
}

final class PyZlibError extends PyException {
    PyZlibError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyZlibErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyZlibError(new PyString(msg)));
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyZlibError(args);
    }
}

final class PySystemExit extends PyBaseException {
    PySystemExit(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PySystemExitType.singleton; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PySystemExit(args);
    }

    static PyObject pymember_code(PyObject obj) {
        PyObject[] args = ((PySystemExit)obj).args;
        if (args.length == 0) {
            return PyNone.singleton;
        }
        if (args.length == 1) {
            return args[0];
        }
        return new PyTuple(args);
    }
}

final class PyRaise extends RuntimeException {
    final PyBaseException exc;
    PyRaise(PyBaseException _exc) { exc = _exc; }
    @Override public String toString() { return "PyRaise(" + exc.repr() + ")"; }
}
