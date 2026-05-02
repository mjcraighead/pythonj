// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

class PyBaseException extends PyTruthyObject {
    protected final PyTuple args;
    PyBaseException(PyObject[] _args) { args = new PyTuple(_args); }
    @Override public PyConcreteType type() { return PyBaseExceptionType.singleton; }

    static PyObject pyget___dict__(PyObject obj) {
        throw new UnsupportedOperationException("BaseException.__dict__ unimplemented");
    }

    @Override public String str() {
        if (args.items.length == 0) {
            return "";
        }
        if (args.items.length == 1) {
            return args.items[0].str();
        }
        StringBuilder s = new StringBuilder("(");
        for (int i = 0; i < args.items.length; i++) {
            if (i != 0) {
                s.append(", ");
            }
            s.append(args.items[i].repr());
        }
        return s.append(")").toString();
    }
    @Override public String repr() {
        StringBuilder s = new StringBuilder(type().name()).append("(");
        for (int i = 0; i < args.items.length; i++) {
            if (i != 0) {
                s.append(", ");
            }
            s.append(args.items[i].repr());
        }
        return s.append(")").toString();
    }

    static PyObject pyget_args(PyObject obj) { return ((PyBaseException)obj).args; }
    static PyObject pyget___suppress_context__(PyObject obj) { throw new UnsupportedOperationException("BaseException.__suppress_context__ unimplemented"); }
    static PyObject pyget___traceback__(PyObject obj) { throw new UnsupportedOperationException("BaseException.__traceback__ unimplemented"); }
    static PyObject pyget___context__(PyObject obj) { throw new UnsupportedOperationException("BaseException.__context__ unimplemented"); }
    static PyObject pyget___cause__(PyObject obj) { throw new UnsupportedOperationException("BaseException.__cause__ unimplemented"); }

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

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyAssertionError(args);
    }
}

final class PyAttributeError extends PyException {
    private final PyObject name;
    private final PyObject obj;

    PyAttributeError(PyObject... _args) {
        this(_args, PyNone.singleton, PyNone.singleton);
    }
    private PyAttributeError(PyObject[] _args, PyObject _name, PyObject _obj) {
        super(_args);
        name = _name;
        obj = _obj;
    }
    @Override public PyConcreteType type() { return PyAttributeErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyAttributeError(new PyString(msg)));
    }

    static PyObject pyget_name(PyObject obj) { return ((PyAttributeError)obj).name; }
    static PyObject pyget_obj(PyObject obj) { return ((PyAttributeError)obj).obj; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        PyObject name = PyNone.singleton;
        PyObject obj = PyNone.singleton;
        if ((kwargs != null) && kwargs.boolValue()) {
            long kwargsLen = kwargs.items.size();
            if (kwargsLen > 2) {
                throw Runtime.raiseAtMostKwArgs(type.name(), 2, args.length, kwargsLen);
            }
            for (var x: kwargs.items.entrySet()) {
                PyString kw = (PyString)x.getKey();
                switch (kw.value) {
                    case "name" -> name = x.getValue();
                    case "obj" -> obj = x.getValue();
                    default -> throw Runtime.raiseUnexpectedKwArg(type.name(), kw.value);
                }
            }
        }
        return new PyAttributeError(args, name, obj);
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
        if (args.items.length == 1) {
            return args.items[0].repr();
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

class PyNameError extends PyException {
    protected final PyObject name;

    PyNameError(PyObject... _args) {
        this(_args, PyNone.singleton);
    }
    protected PyNameError(PyObject[] _args, PyObject _name) {
        super(_args);
        name = _name;
    }
    @Override public PyConcreteType type() { return PyNameErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyNameError(new PyString(msg)));
    }

    static PyObject pyget_name(PyObject obj) { return ((PyNameError)obj).name; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        return newObj(type, args, kwargs, false);
    }
    protected static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs, boolean isUnboundLocalError) {
        String bindName = isUnboundLocalError ? "NameError" : type.name();
        PyObject name = PyNone.singleton;
        if ((kwargs != null) && kwargs.boolValue()) {
            if (kwargs.items.size() > 1) {
                throw Runtime.raiseAtMostKwArgs(bindName, 1, args.length, kwargs.items.size());
            }
            for (var x: kwargs.items.entrySet()) {
                PyString kw = (PyString)x.getKey();
                if (!kw.value.equals("name")) {
                    throw Runtime.raiseUnexpectedKwArg(bindName, kw.value);
                }
                name = x.getValue();
            }
        }
        return isUnboundLocalError ? new PyUnboundLocalError(args, name) : new PyNameError(args, name);
    }
}
final class PyUnboundLocalError extends PyNameError {
    PyUnboundLocalError(PyObject... _args) { super(_args); }
    PyUnboundLocalError(PyObject[] _args, PyObject _name) { super(_args, _name); }
    @Override public PyConcreteType type() { return PyUnboundLocalErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyUnboundLocalError(new PyString(msg)));
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        return PyNameError.newObj(type, args, kwargs, true);
    }
}

final class PyStopIteration extends PyException {
    PyStopIteration(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyStopIterationType.singleton; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyStopIteration(args);
    }

    static PyObject pyget_value(PyObject obj) {
        PyTuple args = ((PyStopIteration)obj).args;
        return (args.items.length == 0) ? PyNone.singleton : args.items[0];
    }
}

class PyRuntimeError extends PyException {
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

final class PyNotImplementedError extends PyRuntimeError {
    PyNotImplementedError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyNotImplementedErrorType.singleton; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyNotImplementedError(args);
    }
}

final class PyTypeError extends PyException {
    PyTypeError(PyObject... _args) { super(_args); }
    @Override public PyConcreteType type() { return PyTypeErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyTypeError(new PyString(msg)));
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

    static PyObject pyget_code(PyObject obj) {
        PyTuple args = ((PySystemExit)obj).args;
        if (args.items.length == 0) {
            return PyNone.singleton;
        }
        if (args.items.length == 1) {
            return args.items[0];
        }
        return args;
    }
}

final class PyRaise extends RuntimeException {
    final PyBaseException exc;
    PyRaise(PyBaseException _exc) { exc = _exc; }
    @Override public String toString() { return "PyRaise(" + exc.repr() + ")"; }
}
