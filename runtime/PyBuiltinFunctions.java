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
    static PyList pyfunc_dir(PyObject object) {
        if (object == null) {
            throw new UnsupportedOperationException("dir() with no arguments is not implemented");
        }
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
    static PyString pyfunc_format(PyObject value, PyObject format_spec_obj) {
        if (format_spec_obj instanceof PyString format_spec_str) {
            return new PyString(value.format(format_spec_str.value));
        } else {
            throw PyTypeError.raiseFormat("format() argument 2 must be str, not %s", format_spec_obj.type().name());
        }
    }
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
        } else if (type instanceof PyConcreteType type_class) {
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
        } else if (obj instanceof PyConcreteType obj_class &&
                    type instanceof PyConcreteType type_class) {
            return type_class.instanceClass.isAssignableFrom(obj_class.instanceClass);
        } else {
            throw new UnsupportedOperationException(String.format("issubclass() is unimplemented for types %s and %s", obj.repr(), type.repr()));
        }
    }
    static PyBool pyfunc_issubclass(PyObject obj, PyObject type) { return PyBool.create(isSubclassImpl(obj, type)); }
    static PyIter pyfunc_iter(PyObject obj, PyObject sentinel) {
        if (sentinel != null) {
            throw new UnsupportedOperationException("iter() with callable+sentinel is not implemented");
        }
        return obj.iter();
    }
    static PyObject minMaxImpl(PyObject[] args, PyDict kwargs, String name, boolean isMax) {
        int argsLength = args.length;
        Runtime.requireMinArgs(args, 1, name);
        PyObject defaultObj = null;
        PyObject keyFunc = PyNone.singleton;
        if ((kwargs != null) && kwargs.boolValue()) {
            long kwargsLen = kwargs.len();
            if (kwargsLen > 2) {
                throw Runtime.raiseAtMostKwArgs(name, 2, 0, kwargsLen);
            }
            for (var x: kwargs.items.entrySet()) {
                PyString kw = (PyString)x.getKey(); // PyString validated at call site
                if (kw.value.equals("default")) {
                    defaultObj = x.getValue();
                } else if (kw.value.equals("key")) {
                    keyFunc = x.getValue();
                } else {
                    throw Runtime.raiseUnexpectedKwArg(name, kw.value);
                }
            }
        }
        if (argsLength == 1) {
            var iter = args[0].iter();
            PyObject ret = iter.next();
            if (ret == null) {
                if (defaultObj != null) {
                    return defaultObj;
                }
                throw PyValueError.raise(name + "() iterable argument is empty");
            }
            if (keyFunc == PyNone.singleton) {
                for (var item = iter.next(); item != null; item = iter.next()) {
                    if (isMax ? item.gt(ret) : item.lt(ret)) {
                        ret = item;
                    }
                }
            } else {
                PyObject retKey = keyFunc.call(new PyObject[] {ret}, null);
                for (var item = iter.next(); item != null; item = iter.next()) {
                    PyObject itemKey = keyFunc.call(new PyObject[] {item}, null);
                    if (isMax ? itemKey.gt(retKey) : itemKey.lt(retKey)) {
                        ret = item;
                        retKey = itemKey;
                    }
                }
            }
            return ret;
        } else {
            if (defaultObj != null) {
                throw PyTypeError.raise("Cannot specify a default for " + name + "() with multiple positional arguments");
            }
            PyObject ret = args[0];
            if (keyFunc == PyNone.singleton) {
                for (int i = 1; i < argsLength; i++) {
                    PyObject item = args[i];
                    if (isMax ? item.gt(ret) : item.lt(ret)) {
                        ret = item;
                    }
                }
            } else {
                PyObject retKey = keyFunc.call(new PyObject[] {ret}, null);
                for (int i = 1; i < argsLength; i++) {
                    PyObject item = args[i];
                    PyObject itemKey = keyFunc.call(new PyObject[] {item}, null);
                    if (isMax ? itemKey.gt(retKey) : itemKey.lt(retKey)) {
                        ret = item;
                        retKey = itemKey;
                    }
                }
            }
            return ret;
        }
    }
    static PyObject pyfunc_max(PyObject[] args, PyDict kwargs) {
        return minMaxImpl(args, kwargs, "max", true);
    }
    static PyObject pyfunc_min(PyObject[] args, PyDict kwargs) {
        return minMaxImpl(args, kwargs, "min", false);
    }
    static PyObject pyfunc_open(PyObject file, PyObject mode, PyObject buffering, PyObject encoding,
                                PyObject errors, PyObject newline, PyObject closefd, PyObject opener) {
        if (!(file instanceof PyString fileStr)) {
            throw PyTypeError.raise("open() argument 'file' must be str, not " + file.type().name());
        }
        if (!(mode instanceof PyString modeStr)) {
            throw PyTypeError.raise("open() argument 'mode' must be str, not " + mode.type().name());
        }
        if ((buffering.indexValue() != -1) || (encoding != PyNone.singleton) || (errors != PyNone.singleton) ||
            (newline != PyNone.singleton) || !closefd.boolValue() || (opener != PyNone.singleton)) {
            throw new UnsupportedOperationException("open() arguments beyond file/mode are not supported");
        }
        if (modeStr.value.equals("r")) {
            return new PyTextIOWrapper(fileStr);
        } else if (modeStr.value.equals("rb")) {
            return new PyBufferedReader(fileStr);
        } else {
            throw new UnsupportedOperationException("open() only supports mode='r' and mode='rb'");
        }
    }
    static PyInt pyfunc_ord(PyObject arg_obj) {
        PyString arg = (PyString)arg_obj;
        if (arg.len() != 1) {
            throw new IllegalArgumentException("argument to ord() must be string of length 1");
        }
        return new PyInt(arg.value.charAt(0));
    }
    static PyNone pyfunc_print(PyObject[] args, PyDict kwargs) {
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
    static PyList pyfunc_sorted(PyObject iterable, PyObject key, PyObject reverse) {
        var ret = new PyList();
        Runtime.addIterableToCollection(ret.items, iterable);
        ret.pymethod_sort(key, reverse);
        return ret;
    }
    static PyInt pyfunc_sum(PyObject iterable, PyObject start) {
        var iter = iterable.iter();
        long sum = start.indexValue();
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
