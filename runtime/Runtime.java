// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;

abstract class PyTruthyObject extends PyObject {
    @Override public final boolean boolValue() { return true; }
}

abstract class PyIter extends PyTruthyObject {
    @Override public final boolean hasIter() { return true; }
    @Override public final PyIter iter() { return this; }
}

abstract class PyType extends PyTruthyObject {
    public abstract String name();
}

class PyBuiltinClass extends PyType {
    protected final String typeName;
    protected final Class<? extends PyObject> instanceClass;

    protected PyBuiltinClass(String name, Class<? extends PyObject> _instanceClass) {
        typeName = name;
        instanceClass = _instanceClass;
    }
    @Override public final PyObject getAttr(String key) {
        switch (key) {
            case "__name__": return new PyString(typeName);
            default: return super.getAttr(key);
        }
    }
    @Override public final String repr() { return "<class '" + typeName + "'>"; }
    @Override public final Runtime.pyfunc_type type() { return Runtime.pyglobal_type; }
    @Override public String name() { return typeName; }
}

abstract class PyBuiltinFunctionOrMethod extends PyTruthyObject {
    @Override public final PyBuiltinClass type() { return Runtime.pytype_builtin_function_or_method; }
}

abstract class PyBuiltinMethod<T extends PyObject> extends PyBuiltinFunctionOrMethod {
    protected final T self;
    PyBuiltinMethod(T _self) { self = _self; }
    @Override public String repr() {
        return "<built-in method " + methodName() + " of " + self.type().name() + " object>";
    }
    public abstract String methodName();
}

abstract class PyUserFunction extends PyTruthyObject {
    private final String funcName;
    protected PyUserFunction(String name) { funcName = name; }
    @Override public PyBuiltinClass type() { return Runtime.pytype_function; }
    @Override public String repr() { return "<function " + funcName + ">"; }
}

public final class Runtime {
    abstract static class PyBuiltinFunction extends PyBuiltinFunctionOrMethod {
        protected final String funcName;
        protected PyBuiltinFunction(String name) { funcName = name; }
        @Override public final String repr() { return "<built-in function " + funcName + ">"; }
        protected PyObject exactlyOneArg(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, funcName);
            if (args.length != 1) {
                throw PyTypeError.raiseFormat("%s() takes exactly one argument (%d given)", funcName, args.length);
            }
            return args[0];
        }
    }

    public static final PyBuiltinClass pytype_builtin_function_or_method = new PyBuiltinClass("builtin_function_or_method", PyBuiltinFunctionOrMethod.class);
    public static final PyBuiltinClass pytype_function = new PyBuiltinClass("function", PyUserFunction.class);
    public static final PyBuiltinClass pytype_io_BufferedReader = new PyBuiltinClass("_io.BufferedReader", PyBufferedReader.class);
    public static final PyBuiltinClass pytype_io_TextIOWrapper = new PyBuiltinClass("_io.TextIOWrapper", PyTextIOWrapper.class);

    static final class pyfunc_abs extends PyBuiltinFunction {
        pyfunc_abs() { super("abs"); }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            return arg.abs();
        }
    }
    public static final pyfunc_abs pyglobal_abs = new pyfunc_abs();

    static final class pyfunc_all extends PyBuiltinFunction {
        pyfunc_all() { super("all"); }
        @Override public PyBool call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            var iter = arg.iter();
            for (var item = iter.next(); item != null; item = iter.next()) {
                if (!item.boolValue()) {
                    return PyBool.false_singleton;
                }
            }
            return PyBool.true_singleton;
        }
    }
    public static final pyfunc_all pyglobal_all = new pyfunc_all();

    static final class pyfunc_any extends PyBuiltinFunction {
        pyfunc_any() { super("any"); }
        @Override public PyBool call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            var iter = arg.iter();
            for (var item = iter.next(); item != null; item = iter.next()) {
                if (item.boolValue()) {
                    return PyBool.true_singleton;
                }
            }
            return PyBool.false_singleton;
        }
    }
    public static final pyfunc_any pyglobal_any = new pyfunc_any();

    static final class pyfunc_bool extends PyBuiltinClass {
        pyfunc_bool() { super("bool", PyBool.class); }
        @Override public PyBool call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireMaxArgs(args, 1, typeName);
            if (args.length == 1) {
                return PyBool.create(args[0].boolValue());
            }
            return PyBool.false_singleton;
        }
    }
    public static final pyfunc_bool pyglobal_bool = new pyfunc_bool();

    static final class pyfunc_bytearray extends PyBuiltinClass {
        pyfunc_bytearray() { super("bytearray", PyByteArray.class); }
        @Override public PyByteArray call(PyObject[] args, PyDict kwargs) {
            if (args.length > 1) {
                throw new IllegalArgumentException("bytearray() takes 0 or 1 arguments");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("bytearray() does not accept kwargs");
            }
            if (args.length == 0) {
                return new PyByteArray(new byte[0]);
            }
            PyObject arg = args[0];
            if (arg.hasIndex()) {
                return new PyByteArray(new byte[Math.toIntExact(arg.indexValue())]);
            }
            var b = new ByteArrayOutputStream();
            var iter = arg.iter();
            for (var item = iter.next(); item != null; item = iter.next()) {
                long i = item.indexValue();
                if ((i < 0) || (i >= 256)) {
                    throw new IllegalArgumentException("invalid byte value");
                }
                b.write((int)i);
            }
            return new PyByteArray(b.toByteArray());
        }
    }
    public static final pyfunc_bytearray pyglobal_bytearray = new pyfunc_bytearray();

    static final class pyfunc_bytes extends PyBuiltinClass {
        pyfunc_bytes() { super("bytes", PyBytes.class); }
        @Override public PyBytes call(PyObject[] args, PyDict kwargs) {
            if (args.length > 1) {
                throw new IllegalArgumentException("bytes() takes 0 or 1 arguments");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("bytes() does not accept kwargs");
            }
            if (args.length == 0) {
                return new PyBytes(new byte[0]);
            }
            PyObject arg = args[0];
            if (arg.hasIndex()) {
                return new PyBytes(new byte[Math.toIntExact(arg.indexValue())]);
            }
            var b = new ByteArrayOutputStream();
            var iter = arg.iter();
            for (var item = iter.next(); item != null; item = iter.next()) {
                long i = item.indexValue();
                if ((i < 0) || (i >= 256)) {
                    throw new IllegalArgumentException("invalid byte value");
                }
                b.write((int)i);
            }
            return new PyBytes(b.toByteArray());
        }
    }
    public static final pyfunc_bytes pyglobal_bytes = new pyfunc_bytes();

    static final class pyfunc_chr extends PyBuiltinFunction {
        pyfunc_chr() { super("chr"); }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            long index = arg.indexValue();
            if ((index < 0) || (index > 65535)) {
                throw new IllegalArgumentException("chr() argument out of range");
            }
            return new PyString(String.valueOf((char)index));
        }
    }
    public static final pyfunc_chr pyglobal_chr = new pyfunc_chr();

    static final class pyfunc_dict extends PyBuiltinClass {
        pyfunc_dict() { super("dict", PyDict.class); }
        @Override public PyDict call(PyObject[] args, PyDict kwargs) {
            if (args.length > 1) {
                throw new IllegalArgumentException("dict() takes 0 or 1 arguments");
            }
            var ret = new PyDict();
            if (args.length != 0) {
                PyObject arg = args[0];
                if (arg instanceof PyDict dict) { // XXX support arbitrary mappings here
                    for (var x: dict.items.entrySet()) {
                        ret.setItem(x.getKey(), x.getValue());
                    }
                } else {
                    var iter = arg.iter();
                    long index = 0;
                    for (var item = iter.next(); item != null; item = iter.next(), index++) {
                        if (!item.hasIter()) {
                            throw PyTypeError.raise("object is not iterable");
                        }
                        var itemIter = item.iter();
                        PyObject key = itemIter.next();
                        if (key == null) {
                            throw PyValueError.raiseFormat("dictionary update sequence element #%d has length 0; 2 is required", index);
                        }
                        PyObject value = itemIter.next();
                        if (value == null) {
                            throw PyValueError.raiseFormat("dictionary update sequence element #%d has length 1; 2 is required", index);
                        }
                        Runtime.nextRequireNull(itemIter);
                        ret.setItem(key, value);
                    }
                }
            }
            if (kwargs != null) {
                for (var x: kwargs.items.entrySet()) {
                    ret.setItem(x.getKey(), x.getValue());
                }
            }
            return ret;
        }
    }
    public static final pyfunc_dict pyglobal_dict = new pyfunc_dict();

    static final class pyfunc_enumerate extends PyBuiltinClass {
        pyfunc_enumerate() { super("enumerate", PyEnumerate.class); }
        @Override public PyEnumerate call(PyObject[] args, PyDict kwargs) {
            // This is quirky, but is intended to match corner cases in CPython enumerate()
            long totalArgs = args.length;
            if (kwargs != null) {
                totalArgs += kwargs.items.size();
            }
            if ((totalArgs == 1) || (totalArgs == 2)) {
                PyObject iterable = (args.length >= 1) ? args[0] : null;
                PyObject start = (args.length >= 2) ? args[1] : null;
                if (kwargs != null) {
                    for (var x: kwargs.items.entrySet()) {
                        PyString key = (PyString)x.getKey(); // PyString validated at call site
                        if ((key.value.equals("iterable")) && (iterable == null)) {
                            iterable = x.getValue();
                        } else if ((totalArgs == 2) && (key.value.equals("start")) && (start == null)) {
                            start = x.getValue();
                        } else {
                            throw PyTypeError.raiseFormat("%s is an invalid keyword argument for enumerate()", key.repr());
                        }
                    }
                }
                if (iterable == null) {
                    throw PyTypeError.raise("enumerate() missing required argument 'iterable'");
                }
                long startIndex = (start != null) ? start.indexValue() : 0;
                return new PyEnumerate(iterable.iter(), startIndex);
            } else if (args.length == 0) {
                throw PyTypeError.raise("enumerate() missing required argument 'iterable'");
            } else {
                throw PyTypeError.raiseFormat("enumerate() takes at most 2 arguments (%d given)", totalArgs);
            }
        }
    }
    public static final pyfunc_enumerate pyglobal_enumerate = new pyfunc_enumerate();

    static final class pyfunc_format extends PyBuiltinFunction {
        pyfunc_format() { super("format"); }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, funcName);
            requireMinArgs(args, 1, funcName);
            requireMaxArgs(args, 2, funcName);
            String formatSpec = "";
            if (args.length == 2) {
                if (args[1] instanceof PyString arg1_str) {
                    formatSpec = arg1_str.value;
                } else {
                    throw PyTypeError.raiseFormat("format() argument 2 must be str, not %s", args[1].type().name());
                }
            }
            return new PyString(args[0].format(formatSpec));
        }
    }
    public static final pyfunc_format pyglobal_format = new pyfunc_format();

    static final class pyfunc_getattr extends PyBuiltinFunction {
        pyfunc_getattr() { super("getattr"); }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, funcName);
            requireMinArgs(args, 2, funcName);
            requireMaxArgs(args, 3, funcName);
            // Currently we never use args[2] (default) because this will throw a Java exception
            if (args[1] instanceof PyString name) {
                return args[0].getAttr(name.value);
            } else {
                throw PyTypeError.raiseFormat("attribute name must be string, not %s", PyString.reprOf(args[1].type().name()));
            }
        }
    }
    public static final pyfunc_getattr pyglobal_getattr = new pyfunc_getattr();

    static final class pyfunc_hash extends PyBuiltinFunction {
        pyfunc_hash() { super("hash"); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            return new PyInt(arg.hashCode());
        }
    }
    public static final pyfunc_hash pyglobal_hash = new pyfunc_hash();

    static final class pyfunc_hex extends PyBuiltinFunction {
        pyfunc_hex() { super("hex"); }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            long index = arg.indexValue();
            if (index < 0) {
                return new PyString(String.format("-0x%x", Math.negateExact(index)));
            } else {
                return new PyString(String.format("0x%x", index));
            }
        }
    }
    public static final pyfunc_hex pyglobal_hex = new pyfunc_hex();

    static final class pyfunc_int extends PyBuiltinClass {
        pyfunc_int() { super("int", PyInt.class); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            requireMaxArgs(args, 2, typeName);
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("int() does not accept kwargs");
            }
            if (args.length == 0) {
                return PyInt.singleton_0;
            }
            PyObject arg0 = args[0];
            // XXX should always call intValue when length is 1
            if (arg0 instanceof PyInt arg0_int) {
                if (args.length > 1) {
                    throw new IllegalArgumentException("int() cannot accept a base when passed an int");
                }
                return arg0_int;
            }
            if (arg0 instanceof PyBool) {
                if (args.length > 1) {
                    throw new IllegalArgumentException("int() cannot accept a base when passed a bool");
                }
                return new PyInt(arg0.intValue());
            }
            if (arg0 instanceof PyString arg0_str) {
                long base = 10;
                if (args.length > 1) {
                    PyObject arg1 = args[1];
                    if (arg1.hasIndex()) {
                        base = arg1.indexValue();
                    } else {
                        throw new IllegalArgumentException("base must be an int");
                    }
                    if ((base < 0) || (base == 1) || (base > 36)) {
                        throw new IllegalArgumentException("base must be 0 or 2-36");
                    }
                    if (base == 0) {
                        throw new UnsupportedOperationException("base 0 unsupported at present");
                    }
                }
                String s = arg0_str.value;
                int i = 0;
                while (s.charAt(i) == ' ') {
                    i++;
                }
                long sign = 1;
                if (s.charAt(i) == '-') {
                    i++;
                    sign = -1;
                } else if (s.charAt(i) == '+') {
                    i++;
                }
                long value = 0;
                while (i < s.length()) {
                    long digit;
                    char c = s.charAt(i++);
                    if ((c >= '0') && (c <= '9')) {
                        digit = c - '0';
                    } else if ((c >= 'a') && (c <= 'z')) {
                        digit = c - 'a' + 10;
                    } else if ((c >= 'A') && (c <= 'Z')) {
                        digit = c - 'A' + 10;
                    } else {
                        throw new IllegalArgumentException("unexpected digit");
                    }
                    if (digit >= base) {
                        throw new IllegalArgumentException("digit not valid in base");
                    }
                    value = value*base + digit;
                }
                return new PyInt(sign*value);
            }
            throw new UnsupportedOperationException("don't know how to handle argument to int()");
        }
    }
    public static final pyfunc_int pyglobal_int = new pyfunc_int();

    static final class pyfunc_isinstance extends PyBuiltinFunction {
        pyfunc_isinstance() { super("isinstance"); }
        private static boolean isInstanceImpl(PyObject obj, PyObject type) {
            if (type instanceof PyTuple type_tuple) {
                for (var x: type_tuple.items) {
                    if (isInstanceImpl(obj, x)) {
                        return true;
                    }
                }
                return false;
            } else if (type instanceof PyBuiltinClass type_class) {
                return type_class.instanceClass.isInstance(obj);
            } else if (type instanceof PyType) {
                throw new UnsupportedOperationException(String.format("isinstance() is unimplemented for type %s", type.repr()));
            } else {
                throw PyTypeError.raise("isinstance() arg 2 must be a type, a tuple of types, or a union");
            }
        }
        @Override public PyBool call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, funcName);
            requireExactArgs(args, 2, funcName);
            return PyBool.create(isInstanceImpl(args[0], args[1]));
        }
    }
    public static final pyfunc_isinstance pyglobal_isinstance = new pyfunc_isinstance();

    static final class pyfunc_issubclass extends PyBuiltinFunction {
        pyfunc_issubclass() { super("issubclass"); }
        private static boolean isSubclassImpl(PyObject obj, PyObject type) {
            if (type instanceof PyTuple type_tuple) {
                for (var x: type_tuple.items) {
                    if (isSubclassImpl(obj, x)) {
                        return true;
                    }
                }
                return false;
            } else if (obj instanceof PyBuiltinClass obj_class &&
                       type instanceof PyBuiltinClass type_class) {
                return type_class.instanceClass.isAssignableFrom(obj_class.instanceClass);
            } else {
                throw new UnsupportedOperationException(String.format("issubclass() is unimplemented for types %s and %s", obj.repr(), type.repr()));
            }
        }
        @Override public PyBool call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, funcName);
            requireExactArgs(args, 2, funcName);
            return PyBool.create(isSubclassImpl(args[0], args[1]));
        }
    }
    public static final pyfunc_issubclass pyglobal_issubclass = new pyfunc_issubclass();

    static final class pyfunc_iter extends PyBuiltinFunction {
        pyfunc_iter() { super("iter"); }
        @Override public PyIter call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, funcName);
            if (args.length != 1) {
                throw new IllegalArgumentException("iter() takes 1 argument");
            }
            return args[0].iter();
        }
    }
    public static final pyfunc_iter pyglobal_iter = new pyfunc_iter();

    static final class pyfunc_len extends PyBuiltinFunction {
        pyfunc_len() { super("len"); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            return new PyInt(arg.len());
        }
    }
    public static final pyfunc_len pyglobal_len = new pyfunc_len();

    static final class pyfunc_list extends PyBuiltinClass {
        pyfunc_list() { super("list", PyList.class); }
        @Override public PyList call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireMaxArgs(args, 1, typeName);
            var ret = new PyList();
            if (args.length == 0) {
                return ret;
            }
            var iter = args[0].iter();
            for (var item = iter.next(); item != null; item = iter.next()) {
                ret.items.add(item);
            }
            return ret;
        }
    }
    public static final pyfunc_list pyglobal_list = new pyfunc_list();

    static final class pyfunc_max extends PyBuiltinFunction {
        pyfunc_max() { super("max"); }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("max() does not accept kwargs");
            }
            requireMinArgs(args, 1, funcName);
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
    public static final pyfunc_max pyglobal_max = new pyfunc_max();

    static final class pyfunc_min extends PyBuiltinFunction {
        pyfunc_min() { super("min"); }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("min() does not accept kwargs");
            }
            requireMinArgs(args, 1, funcName);
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
    public static final pyfunc_min pyglobal_min = new pyfunc_min();

    static final class pyfunc_next extends PyBuiltinFunction {
        pyfunc_next() { super("next"); }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, funcName);
            requireMinArgs(args, 1, funcName);
            requireMaxArgs(args, 2, funcName);
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
    public static final pyfunc_next pyglobal_next = new pyfunc_next();

    static final class pyfunc_object extends PyBuiltinClass {
        pyfunc_object() { super("object", PyObject.class); }
    }
    public static final pyfunc_object pyglobal_object = new pyfunc_object();

    static final class pyfunc_open extends PyBuiltinFunction {
        pyfunc_open() { super("open"); }
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
    public static final pyfunc_open pyglobal_open = new pyfunc_open();

    static final class pyfunc_ord extends PyBuiltinFunction {
        pyfunc_ord() { super("ord"); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            PyString arg = (PyString)exactlyOneArg(args, kwargs);
            if (arg.len() != 1) {
                throw new IllegalArgumentException("argument to ord() must be string of length 1");
            }
            return new PyInt(arg.value.charAt(0));
        }
    }
    public static final pyfunc_ord pyglobal_ord = new pyfunc_ord();

    static final class pyfunc_print extends PyBuiltinFunction {
        pyfunc_print() { super("print"); }
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
    public static final pyfunc_print pyglobal_print = new pyfunc_print();

    static final class pyfunc_range extends PyBuiltinClass {
        pyfunc_range() { super("range", PyRange.class); }
        @Override public PyRange call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireMinArgs(args, 1, typeName);
            requireMaxArgs(args, 3, typeName);
            long start = 0, end, step = 1;
            if (args.length == 1) {
                end = args[0].indexValue();
            } else {
                start = args[0].indexValue();
                end = args[1].indexValue();
                if (args.length == 3) {
                    step = args[2].indexValue();
                }
            }
            return new PyRange(start, end, step);
        }
    }
    public static final pyfunc_range pyglobal_range = new pyfunc_range();

    static final class pyfunc_repr extends PyBuiltinFunction {
        pyfunc_repr() { super("repr"); }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            return new PyString(arg.repr());
        }
    }
    public static final pyfunc_repr pyglobal_repr = new pyfunc_repr();

    static final class pyfunc_reversed extends PyBuiltinClass {
        pyfunc_reversed() { super("reversed", PyReversed.class); }
        @Override public PyIter call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireExactArgs(args, 1, typeName);
            return args[0].reversed();
        }
    }
    public static final pyfunc_reversed pyglobal_reversed = new pyfunc_reversed();

    static final class pyfunc_set extends PyBuiltinClass {
        pyfunc_set() { super("set", PySet.class); }
        @Override public PySet call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireMaxArgs(args, 1, typeName);
            var ret = new PySet();
            if (args.length == 0) {
                return ret;
            }
            var iter = args[0].iter();
            for (var item = iter.next(); item != null; item = iter.next()) {
                ret.items.add(item);
            }
            return ret;
        }
    }
    public static final pyfunc_set pyglobal_set = new pyfunc_set();

    static final class pyfunc_slice extends PyBuiltinClass {
        pyfunc_slice() { super("slice", PySlice.class); }
        @Override public PySlice call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireMinArgs(args, 1, typeName);
            requireMaxArgs(args, 3, typeName);
            PyObject start = PyNone.singleton;
            PyObject end;
            PyObject step = PyNone.singleton;
            if (args.length == 1) {
                end = args[0];
            } else {
                start = args[0];
                end = args[1];
                if (args.length == 3) {
                    step = args[2];
                }
            }
            return new PySlice(start, end, step);
        }
    }
    public static final pyfunc_slice pyglobal_slice = new pyfunc_slice();

    static final class pyfunc_sorted extends PyBuiltinFunction {
        pyfunc_sorted() { super("sorted"); }
        @Override public PyList call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("sorted() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("sorted() does not accept kwargs");
            }
            var ret = new PyList();
            var iter = args[0].iter();
            for (var item = iter.next(); item != null; item = iter.next()) {
                ret.items.add(item);
            }
            Collections.sort(ret.items);
            return ret;
        }
    }
    public static final pyfunc_sorted pyglobal_sorted = new pyfunc_sorted();

    static final class pyfunc_str extends PyBuiltinClass {
        pyfunc_str() { super("str", PyString.class); }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            if (args.length > 1) {
                throw new IllegalArgumentException("str() takes 0 or 1 arguments");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("str() does not accept kwargs");
            }
            if (args.length == 1) {
                return new PyString(args[0].str());
            }
            return PyString.empty_singleton;
        }
    }
    public static final pyfunc_str pyglobal_str = new pyfunc_str();

    static final class pyfunc_sum extends PyBuiltinFunction {
        pyfunc_sum() { super("sum"); }
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
    public static final pyfunc_sum pyglobal_sum = new pyfunc_sum();

    static final class pyfunc_tuple extends PyBuiltinClass {
        pyfunc_tuple() { super("tuple", PyTuple.class); }
        @Override public PyTuple call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireMaxArgs(args, 1, typeName);
            if (args.length == 0) {
                return new PyTuple();
            }
            var iter = args[0].iter();
            var list = new ArrayList<PyObject>();
            addPyIterToArrayList(list, iter);
            return new PyTuple(list);
        }
    }
    public static final pyfunc_tuple pyglobal_tuple = new pyfunc_tuple();

    static final class pyfunc_type extends PyBuiltinClass {
        pyfunc_type() { super("type", PyType.class); }
        @Override public PyType call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("type() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("type() does not accept kwargs");
            }
            return args[0].type();
        }
    }
    public static final pyfunc_type pyglobal_type = new pyfunc_type();

    static final class pyfunc_zip extends PyBuiltinClass {
        pyfunc_zip() { super("zip", PyZip.class); }
        @Override public PyZip call(PyObject[] args, PyDict kwargs) {
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("zip() does not accept kwargs");
            }
            PyIter iters[] = new PyIter[args.length];
            for (int i = 0; i < args.length; i++) {
                iters[i] = args[i].iter();
            }
            return new PyZip(iters);
        }
    }
    public static final pyfunc_zip pyglobal_zip = new pyfunc_zip();

    static final class pyfunc_ArithmeticError extends PyBuiltinClass {
        pyfunc_ArithmeticError() { super("ArithmeticError", PyArithmeticError.class); }
        @Override public PyArithmeticError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyArithmeticError(args);
        }
    }
    public static final pyfunc_ArithmeticError pyglobal_ArithmeticError = new pyfunc_ArithmeticError();

    static final class pyfunc_AssertionError extends PyBuiltinClass {
        pyfunc_AssertionError() { super("AssertionError", PyAssertionError.class); }
        @Override public PyAssertionError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyAssertionError(args);
        }
    }
    public static final pyfunc_AssertionError pyglobal_AssertionError = new pyfunc_AssertionError();

    static final class pyfunc_IndexError extends PyBuiltinClass {
        pyfunc_IndexError() { super("IndexError", PyIndexError.class); }
        @Override public PyIndexError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyIndexError(args);
        }
    }
    public static final pyfunc_IndexError pyglobal_IndexError = new pyfunc_IndexError();

    static final class pyfunc_KeyError extends PyBuiltinClass {
        pyfunc_KeyError() { super("KeyError", PyKeyError.class); }
        @Override public PyKeyError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyKeyError(args);
        }
    }
    public static final pyfunc_KeyError pyglobal_KeyError = new pyfunc_KeyError();

    static final class pyfunc_LookupError extends PyBuiltinClass {
        pyfunc_LookupError() { super("LookupError", PyLookupError.class); }
        @Override public PyLookupError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyLookupError(args);
        }
    }
    public static final pyfunc_LookupError pyglobal_LookupError = new pyfunc_LookupError();

    static final class pyfunc_StopIteration extends PyBuiltinClass {
        pyfunc_StopIteration() { super("StopIteration", PyStopIteration.class); }
        @Override public PyStopIteration call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyStopIteration(args);
        }
    }
    public static final pyfunc_StopIteration pyglobal_StopIteration = new pyfunc_StopIteration();

    static final class pyfunc_TypeError extends PyBuiltinClass {
        pyfunc_TypeError() { super("TypeError", PyTypeError.class); }
        @Override public PyTypeError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyTypeError(args);
        }
    }
    public static final pyfunc_TypeError pyglobal_TypeError = new pyfunc_TypeError();

    static final class pyfunc_ValueError extends PyBuiltinClass {
        pyfunc_ValueError() { super("ValueError", PyValueError.class); }
        @Override public PyValueError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyValueError(args);
        }
    }
    public static final pyfunc_ValueError pyglobal_ValueError = new pyfunc_ValueError();

    static final class pyfunc_ZeroDivisionError extends PyBuiltinClass {
        pyfunc_ZeroDivisionError() { super("ZeroDivisionError", PyZeroDivisionError.class); }
        @Override public PyZeroDivisionError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyZeroDivisionError(args);
        }
    }
    public static final pyfunc_ZeroDivisionError pyglobal_ZeroDivisionError = new pyfunc_ZeroDivisionError();

    // Helper functions used by the builtins and code generator
    public static void requireNoKwArgs(PyDict kwargs, String name) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw PyTypeError.raiseFormat("%s() takes no keyword arguments", name);
        }
    }
    public static void requireExactArgs(PyObject[] args, int n, String name) {
        if (args.length != n) {
            throw PyTypeError.raiseFormat("%s expected %d argument%s, got %d", name, n, (n == 1) ? "" : "s", args.length);
        }
    }
    public static void requireExactArgsAlt(PyObject[] args, int n, String name) {
        if (args.length != n) {
            if (n == 0) {
                throw PyTypeError.raiseFormat("%s() takes no arguments (%d given)", name, args.length);
            } else if (n == 1) {
                throw PyTypeError.raiseFormat("%s() takes exactly one argument (%d given)", name, args.length);
            } else { // XXX Figure out what to do in this case
                throw new IllegalArgumentException(String.format("%s expected %d argument%s, got %d", name, n, (n == 1) ? "" : "s", args.length));
            }
        }
    }
    public static PyRaise raiseUserExactArgs(PyObject[] args, int n, String name, String... argNames) {
        if (args.length > n) {
            return PyTypeError.raiseFormat("%s() takes %d positional argument%s but %d %s given",
                name, n, (n == 1) ? "" : "s", args.length, (args.length == 1) ? "was" : "were");
        } else {
            int missing = n - args.length;
            StringBuilder s = new StringBuilder(String.format("%s() missing %d required positional argument%s:", name, missing, (missing == 1) ? "" : "s"));
            for (int i = args.length; i < n; i++) {
                s.append(" '").append(argNames[i]).append("'");
                if ((missing >= 3) && (i != n - 1)) {
                    s.append(",");
                }
                if (i == n - 2) {
                    s.append(" and");
                }
            }
            return PyTypeError.raise(s.toString());
        }
    }
    public static void requireMinArgs(PyObject[] args, int min, String name) {
        if (args.length < min) {
            throw PyTypeError.raiseFormat("%s expected at least %d argument%s, got %d", name, min, (min == 1) ? "" : "s", args.length);
        }
    }
    public static void requireMaxArgs(PyObject[] args, int max, String name) {
        if (args.length > max) {
            throw PyTypeError.raiseFormat("%s expected at most %d argument%s, got %d", name, max, (max == 1) ? "" : "s", args.length);
        }
    }
    public static PyDict requireKwStrings(PyDict dict) {
        for (var x: dict.items.keySet()) {
            if (!(x instanceof PyString)) {
                throw PyTypeError.raise("keywords must be strings");
            }
        }
        return dict;
    }
    public static ArrayList<PyObject> addPyObjectToArrayList(ArrayList<PyObject> list, PyObject obj) {
        list.add(obj);
        return list;
    }
    public static ArrayList<PyObject> addPyIterToArrayList(ArrayList<PyObject> list, PyObject iter) {
        for (var item = iter.next(); item != null; item = iter.next()) {
            list.add(item);
        }
        return list;
    }
    public static ArrayList<PyObject> addStarToArrayList(ArrayList<PyObject> list, PyObject iterable) {
        if (iterable.hasIter()) {
            return addPyIterToArrayList(list, iterable.iter());
        } else {
            throw PyTypeError.raiseFormat("Value after * must be an iterable, not %s", iterable.type().name());
        }
    }
    public static PyObject[] arrayListToArray(ArrayList<PyObject> list) {
        var array = new PyObject[list.size()];
        list.toArray(array);
        return array;
    }
    public static PyObject nextRequireNonNull(PyIter iter) {
        PyObject obj = iter.next();
        if (obj == null) {
            throw new IllegalStateException("not enough values to unpack");
        }
        return obj;
    }
    public static void nextRequireNull(PyIter iter) {
        PyObject obj = iter.next();
        if (obj != null) {
            throw new IllegalStateException("too many values to unpack");
        }
    }
}
