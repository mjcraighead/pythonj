// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

// BEGIN GENERATED CODE: PyArithmeticErrorType
final class PyArithmeticErrorType extends PyBuiltinType {
    public static final PyArithmeticErrorType singleton = new PyArithmeticErrorType();
    private static final PyString pyattr___doc__ = new PyString("Base class for arithmetic errors.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyArithmeticErrorType() { super("ArithmeticError", PyArithmeticError.class, PyArithmeticError::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyArithmeticErrorType

final class PyAssertionErrorType extends PyBuiltinType {
// BEGIN GENERATED CODE: PyAssertionErrorType
    public static final PyAssertionErrorType singleton = new PyAssertionErrorType();
    private static final PyString pyattr___doc__ = new PyString("Assertion failed.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyAssertionErrorType() { super("AssertionError", PyAssertionError.class); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
// END GENERATED CODE: PyAssertionErrorType

    @Override public PyAssertionError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyAssertionError(args);
    }
}

final class PyAttributeErrorType extends PyBuiltinType {
// BEGIN GENERATED CODE: PyAttributeErrorType
    public static final PyAttributeErrorType singleton = new PyAttributeErrorType();
    private static final PyMemberDescriptor pyattr_name = new PyMemberDescriptor(singleton, "name", PyAttributeError::pymember_name);
    private static final PyMemberDescriptor pyattr_obj = new PyMemberDescriptor(singleton, "obj", PyAttributeError::pymember_obj);
    private static final PyString pyattr___doc__ = new PyString("Attribute not found.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(3);
    static {
        attrs.put(new PyString("name"), pyattr_name);
        attrs.put(new PyString("obj"), pyattr_obj);
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyAttributeErrorType() { super("AttributeError", PyAttributeError.class); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "name": return pyattr_name;
            case "obj": return pyattr_obj;
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
// END GENERATED CODE: PyAttributeErrorType

    @Override public PyAttributeError call(PyObject[] args, PyDict kwargs) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("AttributeError() does not accept kwargs");
        }
        return new PyAttributeError(args);
    }
}

final class PyBaseExceptionType extends PyBuiltinType {
// BEGIN GENERATED CODE: PyBaseExceptionType
    public static final PyBaseExceptionType singleton = new PyBaseExceptionType();
    private static final PyMethodDescriptor pyattr_with_traceback = new PyMethodDescriptor(singleton, "with_traceback", obj -> new PyBaseException.PyBaseExceptionMethodUnimplemented(obj, "with_traceback"));
    private static final PyMethodDescriptor pyattr_add_note = new PyMethodDescriptor(singleton, "add_note", obj -> new PyBaseException.PyBaseExceptionMethodUnimplemented(obj, "add_note"));
    private static final PyGetSetDescriptor pyattr_args = new PyGetSetDescriptor(singleton, "args", PyBaseException::pygetset_args);
    private static final PyString pyattr___doc__ = new PyString("Common base class for all exceptions");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(4);
    static {
        attrs.put(new PyString("with_traceback"), pyattr_with_traceback);
        attrs.put(new PyString("add_note"), pyattr_add_note);
        attrs.put(new PyString("args"), pyattr_args);
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyBaseExceptionType() { super("BaseException", PyBaseException.class); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "with_traceback": return pyattr_with_traceback;
            case "add_note": return pyattr_add_note;
            case "args": return pyattr_args;
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
// END GENERATED CODE: PyBaseExceptionType

    @Override public PyBaseException call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyBaseException(args);
    }
}

final class PyExceptionType extends PyBuiltinType {
// BEGIN GENERATED CODE: PyExceptionType
    public static final PyExceptionType singleton = new PyExceptionType();
    private static final PyString pyattr___doc__ = new PyString("Common base class for all non-exit exceptions.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyExceptionType() { super("Exception", PyException.class); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
// END GENERATED CODE: PyExceptionType

    @Override public PyException call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyException(args);
    }
}

final class PyIndexErrorType extends PyBuiltinType {
    public static final PyIndexErrorType singleton = new PyIndexErrorType();
    private PyIndexErrorType() { super("IndexError", PyIndexError.class); }
    @Override public PyIndexError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyIndexError(args);
    }
}

final class PyKeyErrorType extends PyBuiltinType {
    public static final PyKeyErrorType singleton = new PyKeyErrorType();
    private PyKeyErrorType() { super("KeyError", PyKeyError.class); }
    @Override public PyKeyError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyKeyError(args);
    }
}

final class PyLookupErrorType extends PyBuiltinType {
    public static final PyLookupErrorType singleton = new PyLookupErrorType();
    private PyLookupErrorType() { super("LookupError", PyLookupError.class); }
    @Override public PyLookupError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyLookupError(args);
    }
}

final class PyStopIterationType extends PyBuiltinType {
    public static final PyStopIterationType singleton = new PyStopIterationType();
    private PyStopIterationType() { super("StopIteration", PyStopIteration.class); }
    @Override public PyStopIteration call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyStopIteration(args);
    }
}

final class PyTypeErrorType extends PyBuiltinType {
    public static final PyTypeErrorType singleton = new PyTypeErrorType();
    private PyTypeErrorType() { super("TypeError", PyTypeError.class); }
    @Override public PyTypeError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyTypeError(args);
    }
}

final class PyValueErrorType extends PyBuiltinType {
    public static final PyValueErrorType singleton = new PyValueErrorType();
    private PyValueErrorType() { super("ValueError", PyValueError.class); }
    @Override public PyValueError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyValueError(args);
    }
}

final class PyZeroDivisionErrorType extends PyBuiltinType {
    public static final PyZeroDivisionErrorType singleton = new PyZeroDivisionErrorType();
    private PyZeroDivisionErrorType() { super("ZeroDivisionError", PyZeroDivisionError.class); }
    @Override public PyZeroDivisionError call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        return new PyZeroDivisionError(args);
    }
}

class PyBaseException extends PyTruthyObject {
// BEGIN GENERATED CODE: PyBaseException
    protected static final class PyBaseExceptionMethodUnimplemented extends PyBuiltinMethod<PyBaseException> {
        private final String name;
        PyBaseExceptionMethodUnimplemented(PyObject _self, String _name) { super((PyBaseException)_self); name = _name; }
        @Override public String methodName() { return name; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("BaseException." + name + "() unimplemented");
        }
    }
// END GENERATED CODE: PyBaseException

    protected final PyObject[] args;
    PyBaseException(PyObject[] _args) { args = _args; }
    @Override public PyBuiltinType type() { return PyBaseExceptionType.singleton; }

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

    static PyObject pygetset_args(PyObject obj) { throw new UnsupportedOperationException("BaseException.args unsupported"); }
}

class PyException extends PyBaseException {
    PyException(PyObject[] _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyExceptionType.singleton; }
}

final class PyAssertionError extends PyException {
    PyAssertionError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyAssertionErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyAssertionError(new PyString(msg)));
    }
}

final class PyAttributeError extends PyException {
    PyAttributeError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyAttributeErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyAttributeError(new PyString(msg)));
    }
    static PyRaise raiseFormat(String fmt, Object... args) {
        return new PyRaise(new PyAttributeError(new PyString(String.format(fmt, args))));
    }

    static PyObject pymember_name(PyObject obj) { throw new UnsupportedOperationException("AttributeError.name unsupported"); }
    static PyObject pymember_obj(PyObject obj) { throw new UnsupportedOperationException("AttributeError.obj unsupported"); }
}

class PyArithmeticError extends PyException {
    PyArithmeticError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyArithmeticErrorType.singleton; }

    static public PyArithmeticError newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyArithmeticError(args);
    }
}
final class PyZeroDivisionError extends PyArithmeticError {
    PyZeroDivisionError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyZeroDivisionErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyZeroDivisionError(new PyString(msg)));
    }
}

class PyLookupError extends PyException {
    PyLookupError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyLookupErrorType.singleton; }
}
final class PyIndexError extends PyLookupError {
    PyIndexError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyIndexErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyIndexError(new PyString(msg)));
    }
}
final class PyKeyError extends PyLookupError {
    PyKeyError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyKeyErrorType.singleton; }
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
    @Override public PyBuiltinType type() { return PyStopIterationType.singleton; }
}

final class PyTypeError extends PyException {
    PyTypeError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyTypeErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyTypeError(new PyString(msg)));
    }
    static PyRaise raiseFormat(String fmt, Object... args) {
        return new PyRaise(new PyTypeError(new PyString(String.format(fmt, args))));
    }
}

final class PyValueError extends PyException {
    PyValueError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyValueErrorType.singleton; }

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
