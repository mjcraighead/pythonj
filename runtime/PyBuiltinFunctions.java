// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.ArrayList;
import java.util.Collections;

abstract class PyBuiltinFunction extends PyBuiltinFunctionOrMethod {
    protected final String funcName;
    protected PyBuiltinFunction(String name) { funcName = name; }
    @Override public final String repr() { return "<built-in function " + funcName + ">"; }
}

final class PyBuiltinFunctionsImpl {
    static PyObject pyfunc_abs(PyObject arg) { return arg.abs(); }
    static PyBool pyfunc_all(PyObject arg) {
        var iter = arg.iter();
        for (var item = iter.next(); item != null; item = iter.next()) {
            if (!item.boolValue()) {
                return PyBool.false_singleton;
            }
        }
        return PyBool.true_singleton;
    }
    static PyBool pyfunc_any(PyObject arg) {
        var iter = arg.iter();
        for (var item = iter.next(); item != null; item = iter.next()) {
            if (item.boolValue()) {
                return PyBool.true_singleton;
            }
        }
        return PyBool.false_singleton;
    }
    static PyString pyfunc_ascii(PyObject arg) {
        String r = arg.repr();
        var s = new StringBuilder();
        for (int i = 0; i < r.length(); i++) {
            char c = r.charAt(i);
            if (c < 0x80) {
                s.append(c);
            } else if (c <= 0xFF) {
                s.append("\\x");
                s.append("0123456789abcdef".charAt(c >> 4));
                s.append("0123456789abcdef".charAt(c & 15));
            } else {
                s.append("\\u");
                s.append(String.format("%04x", (int)c));
            }
        }
        return new PyString(s.toString());
    }
    static PyString pyfunc_chr(PyObject arg) {
        long index = arg.indexValue();
        if ((index < 0) || (index > 65535)) {
            throw new IllegalArgumentException("chr() argument out of range");
        }
        return new PyString(String.valueOf((char)index));
    }
    static PyNone pyfunc_delattr(PyObject obj, PyObject name_obj) {
        if (name_obj instanceof PyString name) {
            obj.delAttr(name.value);
            return PyNone.singleton;
        } else {
            throw PyTypeError.raiseFormat("attribute name must be string, not %s", PyString.reprOf(name_obj.type().name()));
        }
    }
    static PyString pyfunc_format(PyObject value, PyObject format_spec_obj) {
        if (format_spec_obj instanceof PyString format_spec_str) {
            return new PyString(value.format(format_spec_str.value));
        } else {
            throw PyTypeError.raiseFormat("format() argument 2 must be str, not %s", format_spec_obj.type().name());
        }
    }
    static PyObject pyfunc_getattr(PyObject obj, PyObject name_obj, PyObject default_obj) {
        if (name_obj instanceof PyString name) {
            try {
                return obj.getAttr(name.value);
            } catch (PyRaise r) {
                if ((default_obj != null) && (r.exc instanceof PyAttributeError)) {
                    return default_obj;
                }
                throw r;
            }
        } else {
            throw PyTypeError.raiseFormat("attribute name must be string, not %s", PyString.reprOf(name_obj.type().name()));
        }
    }
    static PyBool pyfunc_hasattr(PyObject obj, PyObject name_obj) {
        if (name_obj instanceof PyString name) {
            try {
                obj.getAttr(name.value);
            } catch (PyRaise r) {
                if (r.exc instanceof PyAttributeError) {
                    return PyBool.false_singleton;
                }
                throw r;
            }
            return PyBool.true_singleton;
        } else {
            throw PyTypeError.raiseFormat("attribute name must be string, not %s", PyString.reprOf(name_obj.type().name()));
        }
    }
    static PyInt pyfunc_hash(PyObject arg) { return new PyInt(arg.hashCode()); }
    static PyString pyfunc_hex(PyObject arg) {
        long index = arg.indexValue();
        if (index < 0) {
            return new PyString(String.format("-0x%x", Math.negateExact(index)));
        } else {
            return new PyString(String.format("0x%x", index));
        }
    }
    private static boolean isInstanceImpl(PyObject obj, PyObject type) {
        if (type instanceof PyTuple type_tuple) {
            for (var x: type_tuple.items) {
                if (isInstanceImpl(obj, x)) {
                    return true;
                }
            }
            return false;
        } else if (type instanceof PyBuiltinType type_class) {
            return type_class.instanceClass.isInstance(obj);
        } else if (type instanceof PyType) {
            throw new UnsupportedOperationException("isinstance() is unimplemented for type " + type.repr());
        } else {
            throw PyTypeError.raise("isinstance() arg 2 must be a type, a tuple of types, or a union");
        }
    }
    static PyBool pyfunc_isinstance(PyObject obj, PyObject type) { return PyBool.create(isInstanceImpl(obj, type)); }
    private static boolean isSubclassImpl(PyObject obj, PyObject type) {
        if (type instanceof PyTuple type_tuple) {
            for (var x: type_tuple.items) {
                if (isSubclassImpl(obj, x)) {
                    return true;
                }
            }
            return false;
        } else if (obj instanceof PyBuiltinType obj_class &&
                    type instanceof PyBuiltinType type_class) {
            return type_class.instanceClass.isAssignableFrom(obj_class.instanceClass);
        } else {
            throw new UnsupportedOperationException(String.format("issubclass() is unimplemented for types %s and %s", obj.repr(), type.repr()));
        }
    }
    static PyBool pyfunc_issubclass(PyObject obj, PyObject type) { return PyBool.create(isSubclassImpl(obj, type)); }
    static PyInt pyfunc_len(PyObject arg) { return new PyInt(arg.len()); }
    static PyInt pyfunc_ord(PyObject arg_obj) {
        PyString arg = (PyString)arg_obj;
        if (arg.len() != 1) {
            throw new IllegalArgumentException("argument to ord() must be string of length 1");
        }
        return new PyInt(arg.value.charAt(0));
    }
    static PyString pyfunc_repr(PyObject arg) { return new PyString(arg.repr()); }
    static PyNone pyfunc_setattr(PyObject obj, PyObject name_obj, PyObject value) {
        if (name_obj instanceof PyString name) {
            obj.setAttr(name.value, value);
            return PyNone.singleton;
        } else {
            throw PyTypeError.raiseFormat("attribute name must be string, not %s", PyString.reprOf(name_obj.type().name()));
        }
    }
}

final class PyBuiltinFunction_dir extends PyBuiltinFunction {
    public static final PyBuiltinFunction_dir singleton = new PyBuiltinFunction_dir();

    private PyBuiltinFunction_dir() { super("dir"); }
    @Override public PyList call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, funcName);
        Runtime.requireMaxArgs(args, 1, funcName);
        if (args.length == 0) {
            throw new UnsupportedOperationException("dir() with no arguments is not implemented");
        }
        PyObject object = args[0];
        PyType attrsType;
        if (object instanceof PyType objectType) {
            attrsType = objectType;
        } else {
            attrsType = object.type();
        }
        var attrs = attrsType.getAttributes();
        if (attrs == null) {
            throw new UnsupportedOperationException(attrsType.name() + ".__dict__ is not implemented");
        }
        ArrayList<PyObject> list = new ArrayList<>(attrs.keySet());
        Collections.sort(list);
        return new PyList(list);
    }
}

final class PyBuiltinFunction_iter extends PyBuiltinFunction {
    public static final PyBuiltinFunction_iter singleton = new PyBuiltinFunction_iter();

    private PyBuiltinFunction_iter() { super("iter"); }
    @Override public PyIter call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, funcName);
        if (args.length != 1) {
            throw new IllegalArgumentException("iter() takes 1 argument");
        }
        return args[0].iter();
    }
}

final class PyBuiltinFunction_max extends PyBuiltinFunction {
    public static final PyBuiltinFunction_max singleton = new PyBuiltinFunction_max();

    private PyBuiltinFunction_max() { super("max"); }
    @Override public PyObject call(PyObject[] args, PyDict kwargs) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("max() does not accept kwargs");
        }
        Runtime.requireMinArgs(args, 1, funcName);
        if (args.length == 1) {
            var iter = args[0].iter();
            PyObject ret = iter.next();
            if (ret == null) {
                throw PyValueError.raise("max() iterable argument is empty");
            }
            for (var item = iter.next(); item != null; item = iter.next()) {
                if (item.gt(ret)) {
                    ret = item;
                }
            }
            return ret;
        } else {
            PyObject ret = args[0];
            for (int i = 1; i < args.length; i++) {
                PyObject item = args[i];
                if (item.gt(ret)) {
                    ret = item;
                }
            }
            return ret;
        }
    }
}

final class PyBuiltinFunction_min extends PyBuiltinFunction {
    public static final PyBuiltinFunction_min singleton = new PyBuiltinFunction_min();

    private PyBuiltinFunction_min() { super("min"); }
    @Override public PyObject call(PyObject[] args, PyDict kwargs) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("min() does not accept kwargs");
        }
        Runtime.requireMinArgs(args, 1, funcName);
        if (args.length == 1) {
            var iter = args[0].iter();
            PyObject ret = iter.next();
            if (ret == null) {
                throw PyValueError.raise("min() iterable argument is empty");
            }
            for (var item = iter.next(); item != null; item = iter.next()) {
                if (item.lt(ret)) {
                    ret = item;
                }
            }
            return ret;
        } else {
            PyObject ret = args[0];
            for (int i = 1; i < args.length; i++) {
                PyObject item = args[i];
                if (item.lt(ret)) {
                    ret = item;
                }
            }
            return ret;
        }
    }
}

final class PyBuiltinFunction_next extends PyBuiltinFunction {
    public static final PyBuiltinFunction_next singleton = new PyBuiltinFunction_next();

    private PyBuiltinFunction_next() { super("next"); }
    @Override public PyObject call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, funcName);
        Runtime.requireMinArgs(args, 1, funcName);
        Runtime.requireMaxArgs(args, 2, funcName);
        PyObject ret = args[0].next();
        if (ret == null) {
            if (args.length == 2) {
                return args[1];
            }
            throw new PyRaise(new PyStopIteration());
        }
        return ret;
    }
}

final class PyBuiltinFunction_open extends PyBuiltinFunction {
    public static final PyBuiltinFunction_open singleton = new PyBuiltinFunction_open();

    private PyBuiltinFunction_open() { super("open"); }
    @Override public PyObject call(PyObject[] args, PyDict kwargs) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("open() does not accept kwargs");
        }
        if (args.length == 1) {
            return new PyTextIOWrapper((PyString)args[0]);
        } else if (args.length == 2) {
            if (args[1] instanceof PyString arg1_str) {
                if (!arg1_str.value.equals("rb")) {
                    throw new IllegalArgumentException("open() second argument must be 'rb'");
                }
                return new PyBufferedReader((PyString)args[0]);
            } else {
                throw new IllegalArgumentException("open() second argument must be a string");
            }
        } else {
            throw new IllegalArgumentException("open() takes 1 or 2 arguments");
        }
    }
}

final class PyBuiltinFunction_print extends PyBuiltinFunction {
    public static final PyBuiltinFunction_print singleton = new PyBuiltinFunction_print();

    private PyBuiltinFunction_print() { super("print"); }
    @Override public PyNone call(PyObject[] args, PyDict kwargs) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("print() does not accept kwargs");
        }
        boolean first = true;
        for (var arg: args) {
            if (!first) {
                System.out.print(" ");
            }
            first = false;
            System.out.print(arg.str());
        }
        System.out.println();
        return PyNone.singleton;
    }
}

final class PyBuiltinFunction_sorted extends PyBuiltinFunction {
    public static final PyBuiltinFunction_sorted singleton = new PyBuiltinFunction_sorted();

    private PyBuiltinFunction_sorted() { super("sorted"); }
    @Override public PyList call(PyObject[] args, PyDict kwargs) {
        if (args.length != 1) {
            throw new IllegalArgumentException("sorted() takes 1 argument");
        }
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("sorted() does not accept kwargs");
        }
        var ret = new PyList();
        Runtime.addIterableToCollection(ret.items, args[0]);
        Collections.sort(ret.items);
        return ret;
    }
}

final class PyBuiltinFunction_sum extends PyBuiltinFunction {
    public static final PyBuiltinFunction_sum singleton = new PyBuiltinFunction_sum();

    private PyBuiltinFunction_sum() { super("sum"); }
    @Override public PyInt call(PyObject[] args, PyDict kwargs) {
        if (args.length != 1) {
            throw new IllegalArgumentException("sum() takes 1 argument");
        }
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("sum() does not accept kwargs");
        }
        var iter = args[0].iter();
        long sum = 0;
        for (var item = iter.next(); item != null; item = iter.next()) {
            if (item.hasIndex()) {
                sum = Math.addExact(sum, item.indexValue());
            } else {
                throw new IllegalArgumentException("item must be an int or bool");
            }
        }
        return new PyInt(sum);
    }
}
