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

// BEGIN GENERATED CODE: PyAssertionErrorType
final class PyAssertionErrorType extends PyBuiltinType {
    public static final PyAssertionErrorType singleton = new PyAssertionErrorType();
    private static final PyString pyattr___doc__ = new PyString("Assertion failed.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyAssertionErrorType() { super("AssertionError", PyAssertionError.class, PyAssertionError::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyAssertionErrorType

// BEGIN GENERATED CODE: PyAttributeErrorType
final class PyAttributeErrorType extends PyBuiltinType {
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

    private PyAttributeErrorType() { super("AttributeError", PyAttributeError.class, PyAttributeError::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "name": return pyattr_name;
            case "obj": return pyattr_obj;
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyAttributeErrorType

// BEGIN GENERATED CODE: PyBaseExceptionType
final class PyBaseExceptionType extends PyBuiltinType {
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

    private PyBaseExceptionType() { super("BaseException", PyBaseException.class, PyBaseException::newObj); }
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
}
// END GENERATED CODE: PyBaseExceptionType

// BEGIN GENERATED CODE: PyExceptionType
final class PyExceptionType extends PyBuiltinType {
    public static final PyExceptionType singleton = new PyExceptionType();
    private static final PyString pyattr___doc__ = new PyString("Common base class for all non-exit exceptions.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyExceptionType() { super("Exception", PyException.class, PyException::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyExceptionType

// BEGIN GENERATED CODE: PyIndexErrorType
final class PyIndexErrorType extends PyBuiltinType {
    public static final PyIndexErrorType singleton = new PyIndexErrorType();
    private static final PyString pyattr___doc__ = new PyString("Sequence index out of range.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyIndexErrorType() { super("IndexError", PyIndexError.class, PyIndexError::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyIndexErrorType

// BEGIN GENERATED CODE: PyKeyErrorType
final class PyKeyErrorType extends PyBuiltinType {
    public static final PyKeyErrorType singleton = new PyKeyErrorType();
    private static final PyString pyattr___doc__ = new PyString("Mapping key not found.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyKeyErrorType() { super("KeyError", PyKeyError.class, PyKeyError::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyKeyErrorType

// BEGIN GENERATED CODE: PyLookupErrorType
final class PyLookupErrorType extends PyBuiltinType {
    public static final PyLookupErrorType singleton = new PyLookupErrorType();
    private static final PyString pyattr___doc__ = new PyString("Base class for lookup errors.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyLookupErrorType() { super("LookupError", PyLookupError.class, PyLookupError::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyLookupErrorType

// BEGIN GENERATED CODE: PyStopIterationType
final class PyStopIterationType extends PyBuiltinType {
    public static final PyStopIterationType singleton = new PyStopIterationType();
    private static final PyMemberDescriptor pyattr_value = new PyMemberDescriptor(singleton, "value", PyStopIteration::pymember_value);
    private static final PyString pyattr___doc__ = new PyString("Signal the end from iterator.__next__().");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(2);
    static {
        attrs.put(new PyString("value"), pyattr_value);
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyStopIterationType() { super("StopIteration", PyStopIteration.class, PyStopIteration::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "value": return pyattr_value;
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyStopIterationType

// BEGIN GENERATED CODE: PyTypeErrorType
final class PyTypeErrorType extends PyBuiltinType {
    public static final PyTypeErrorType singleton = new PyTypeErrorType();
    private static final PyString pyattr___doc__ = new PyString("Inappropriate argument type.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyTypeErrorType() { super("TypeError", PyTypeError.class, PyTypeError::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyTypeErrorType

// BEGIN GENERATED CODE: PyValueErrorType
final class PyValueErrorType extends PyBuiltinType {
    public static final PyValueErrorType singleton = new PyValueErrorType();
    private static final PyString pyattr___doc__ = new PyString("Inappropriate argument value (of correct type).");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyValueErrorType() { super("ValueError", PyValueError.class, PyValueError::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyValueErrorType

// BEGIN GENERATED CODE: PyZeroDivisionErrorType
final class PyZeroDivisionErrorType extends PyBuiltinType {
    public static final PyZeroDivisionErrorType singleton = new PyZeroDivisionErrorType();
    private static final PyString pyattr___doc__ = new PyString("Second argument to a division or modulo operation was zero.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyZeroDivisionErrorType() { super("ZeroDivisionError", PyZeroDivisionError.class, PyZeroDivisionError::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyZeroDivisionErrorType

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

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyBaseException(args);
    }
}

class PyException extends PyBaseException {
    PyException(PyObject[] _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyExceptionType.singleton; }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyException(args);
    }
}

final class PyAssertionError extends PyException {
    PyAssertionError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyAssertionErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyAssertionError(new PyString(msg)));
    }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyAssertionError(args);
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

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("AttributeError() does not accept kwargs");
        }
        return new PyAttributeError(args);
    }
}

class PyArithmeticError extends PyException {
    PyArithmeticError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyArithmeticErrorType.singleton; }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
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

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyZeroDivisionError(args);
    }
}

class PyLookupError extends PyException {
    PyLookupError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyLookupErrorType.singleton; }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyLookupError(args);
    }
}
final class PyIndexError extends PyLookupError {
    PyIndexError(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyIndexErrorType.singleton; }

    static PyRaise raise(String msg) {
        return new PyRaise(new PyIndexError(new PyString(msg)));
    }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyIndexError(args);
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

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyKeyError(args);
    }
}

final class PyStopIteration extends PyException {
    PyStopIteration(PyObject... _args) { super(_args); }
    @Override public PyBuiltinType type() { return PyStopIterationType.singleton; }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyStopIteration(args);
    }

    static PyObject pymember_value(PyObject obj) { throw new UnsupportedOperationException("StopIteration.value unsupported"); }
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

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyTypeError(args);
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

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        return new PyValueError(args);
    }
}

final class PyRaise extends RuntimeException {
    final PyBaseException exc;
    PyRaise(PyBaseException _exc) { exc = _exc; }
    @Override public String toString() { return "PyRaise(" + exc.repr() + ")"; }
}
