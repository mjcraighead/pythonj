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
    @Override public final PyIter iter() { return this; }
    @Override public String repr() { throw new UnsupportedOperationException("'repr' unimplemented"); }
}

abstract class PyType extends PyTruthyObject {
    public abstract String name();
}

class PyBuiltinClass extends PyType {
    protected final String typeName;
    protected PyBuiltinClass(String name) { typeName = name; }
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

abstract class PyUserFunction extends PyTruthyObject {
    private final String funcName;
    protected PyUserFunction(String name) { funcName = name; }
    @Override public PyBuiltinClass type() { return Runtime.pytype_function; }
    @Override public String repr() { return "<function " + funcName + ">"; }
}

public final class Runtime {
    abstract static class PyBuiltinFunction extends PyBuiltinFunctionOrMethod {
        private final String funcName;
        protected PyBuiltinFunction(String name) { funcName = name; }
        @Override public final String repr() { return "<built-in function " + funcName + ">"; }
    }
    public static final PyBuiltinClass pytype_builtin_function_or_method = new PyBuiltinClass("builtin_function_or_method");
    public static final PyBuiltinClass pytype_function = new PyBuiltinClass("function");
    public static final PyBuiltinClass pytype_io_TextIOWrapper = new PyBuiltinClass("_io.TextIOWrapper");

    static final class pyfunc_abs extends PyBuiltinFunction {
        pyfunc_abs() { super("abs"); }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("abs() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("abs() does not accept kwargs");
            }
            return args[0].abs();
        }
    }
    public static final pyfunc_abs pyglobal_abs = new pyfunc_abs();

    static final class pyfunc_all extends PyBuiltinFunction {
        pyfunc_all() { super("all"); }
        @Override public PyBool call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("all() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("all() does not accept kwargs");
            }
            var iter = args[0].iter();
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
            if (args.length != 1) {
                throw new IllegalArgumentException("any() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("any() does not accept kwargs");
            }
            var iter = args[0].iter();
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
        pyfunc_bool() { super("bool"); }
        @Override public PyBool call(PyObject[] args, PyDict kwargs) {
            if (args.length > 1) {
                throw new IllegalArgumentException("bool() takes 0 or 1 arguments");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("bool() does not accept kwargs");
            }
            if (args.length == 1) {
                return PyBool.create(args[0].boolValue());
            }
            return PyBool.false_singleton;
        }
    }
    public static final pyfunc_bool pyglobal_bool = new pyfunc_bool();

    static final class pyfunc_bytearray extends PyBuiltinClass {
        pyfunc_bytearray() { super("bytearray"); }
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
            if ((arg instanceof PyInt) || (arg instanceof PyBool)) {
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
        pyfunc_bytes() { super("bytes"); }
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
            if ((arg instanceof PyInt) || (arg instanceof PyBool)) {
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
            if (args.length != 1) {
                throw new IllegalArgumentException("chr() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("chr() does not accept kwargs");
            }
            long arg = args[0].indexValue();
            if ((arg < 0) || (arg > 65535)) {
                throw new IllegalArgumentException("chr() argument out of range");
            }
            return new PyString(String.valueOf((char)arg));
        }
    }
    public static final pyfunc_chr pyglobal_chr = new pyfunc_chr();

    static final class pyfunc_dict extends PyBuiltinClass {
        pyfunc_dict() { super("dict"); }
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
                    for (var item = iter.next(); item != null; item = iter.next()) {
                        var itemIter = item.iter();
                        PyObject key = Runtime.nextRequireNonNull(itemIter);
                        PyObject value = Runtime.nextRequireNonNull(itemIter);
                        Runtime.nextRequireNull(itemIter);
                        ret.setItem(key, value);
                    }
                }
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                for (var x: kwargs.items.entrySet()) {
                    ret.setItem(x.getKey(), x.getValue());
                }
            }
            return ret;
        }
    }
    public static final pyfunc_dict pyglobal_dict = new pyfunc_dict();

    static final class pyfunc_enumerate extends PyBuiltinClass {
        pyfunc_enumerate() { super("enumerate"); }
        @Override public PyEnumerate call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("enumerate() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("enumerate() does not accept kwargs");
            }
            return new PyEnumerate(args[0].iter());
        }
    }
    public static final pyfunc_enumerate pyglobal_enumerate = new pyfunc_enumerate();

    static final class pyfunc_hash extends PyBuiltinFunction {
        pyfunc_hash() { super("hash"); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("hash() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("hash() does not accept kwargs");
            }
            return new PyInt(args[0].hashCode());
        }
    }
    public static final pyfunc_hash pyglobal_hash = new pyfunc_hash();

    static final class pyfunc_hex extends PyBuiltinFunction {
        pyfunc_hex() { super("hex"); }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("hex() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("hex() does not accept kwargs");
            }
            long arg = args[0].indexValue();
            if (arg < 0) {
                return new PyString(String.format("-0x%x", -arg));
            } else {
                return new PyString(String.format("0x%x", arg));
            }
        }
    }
    public static final pyfunc_hex pyglobal_hex = new pyfunc_hex();

    static final class pyfunc_int extends PyBuiltinClass {
        pyfunc_int() { super("int"); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            if (args.length > 2) {
                throw new IllegalArgumentException("int() takes 0 to 2 arguments");
            }
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
                    if ((arg1 instanceof PyInt) || (arg1 instanceof PyBool)) {
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
        @Override public PyBool call(PyObject[] args, PyDict kwargs) {
            if (args.length != 2) {
                throw new IllegalArgumentException("isinstance() takes 2 arguments");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("isinstance() does not accept kwargs");
            }
            var obj = args[0];
            var type = args[1];
            if (type == pyglobal_bool) {
                return PyBool.create(obj instanceof PyBool);
            } else if (type == pyglobal_bytearray) {
                return PyBool.create(obj instanceof PyByteArray);
            } else if (type == pyglobal_bytes) {
                return PyBool.create(obj instanceof PyBytes);
            } else if (type == pyglobal_dict) {
                return PyBool.create(obj instanceof PyDict);
            } else if (type == pyglobal_enumerate) {
                return PyBool.create(obj instanceof PyEnumerate);
            } else if (type == pyglobal_int) {
                return PyBool.create(obj instanceof PyInt); // Note intentional compatibility break for "bool"
            } else if (type == pyglobal_list) {
                return PyBool.create(obj instanceof PyList);
            } else if (type == pyglobal_object) {
                return PyBool.true_singleton;
            } else if (type == pyglobal_range) {
                return PyBool.create(obj instanceof PyRange);
            } else if (type == pyglobal_reversed) {
                return PyBool.create(obj instanceof PyReversed);
            } else if (type == pyglobal_set) {
                return PyBool.create(obj instanceof PySet);
            } else if (type == pyglobal_slice) {
                return PyBool.create(obj instanceof PySlice);
            } else if (type == pyglobal_str) {
                return PyBool.create(obj instanceof PyString);
            } else if (type == pyglobal_tuple) {
                return PyBool.create(obj instanceof PyTuple);
            } else if (type == pyglobal_type) {
                return PyBool.create(obj instanceof PyType);
            } else if (type == pyglobal_zip) {
                return PyBool.create(obj instanceof PyZip);
            }
            throw new UnsupportedOperationException(String.format("isinstance() is unimplemented for type %s", type.repr()));
        }
    }
    public static final pyfunc_isinstance pyglobal_isinstance = new pyfunc_isinstance();

    static final class pyfunc_iter extends PyBuiltinFunction {
        pyfunc_iter() { super("iter"); }
        @Override public PyIter call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("iter() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("iter() does not accept kwargs");
            }
            return args[0].iter();
        }
    }
    public static final pyfunc_iter pyglobal_iter = new pyfunc_iter();

    static final class pyfunc_len extends PyBuiltinFunction {
        pyfunc_len() { super("len"); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("len() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("len() does not accept kwargs");
            }
            return new PyInt(args[0].len());
        }
    }
    public static final pyfunc_len pyglobal_len = new pyfunc_len();

    static final class pyfunc_list extends PyBuiltinClass {
        pyfunc_list() { super("list"); }
        @Override public PyList call(PyObject[] args, PyDict kwargs) {
            if (args.length > 1) {
                throw new IllegalArgumentException("list() takes 0 or 1 arguments");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("list() does not accept kwargs");
            }
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
            if (args.length < 1) {
                throw new IllegalArgumentException("max() takes at least 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("max() does not accept kwargs");
            }
            if (args.length == 1) {
                var iter = args[0].iter();
                PyObject ret = iter.next();
                if (ret == null) {
                    throw new IllegalArgumentException("max() expects non-empty iterable");
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
            if (args.length < 1) {
                throw new IllegalArgumentException("min() takes at least 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("min() does not accept kwargs");
            }
            if (args.length == 1) {
                var iter = args[0].iter();
                PyObject ret = iter.next();
                if (ret == null) {
                    throw new IllegalArgumentException("min() expects non-empty iterable");
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

    static final class pyfunc_object extends PyBuiltinClass {
        pyfunc_object() { super("object"); }
    }
    public static final pyfunc_object pyglobal_object = new pyfunc_object();

    static final class pyfunc_open extends PyBuiltinFunction {
        pyfunc_open() { super("open"); }
        @Override public PyFile call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("open() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("open() does not accept kwargs");
            }
            return new PyFile(((PyString)args[0]).value);
        }
    }
    public static final pyfunc_open pyglobal_open = new pyfunc_open();

    static final class pyfunc_ord extends PyBuiltinFunction {
        pyfunc_ord() { super("ord"); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("ord() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("ord() does not accept kwargs");
            }
            PyString arg = (PyString)args[0];
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
        pyfunc_range() { super("range"); }
        @Override public PyRange call(PyObject[] args, PyDict kwargs) {
            if ((args.length < 1) || (args.length > 3)) {
                throw new IllegalArgumentException("range() takes 1 to 3 arguments");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("range() does not accept kwargs");
            }
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
            if (args.length != 1) {
                throw new IllegalArgumentException("repr() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("repr() does not accept kwargs");
            }
            return new PyString(args[0].repr());
        }
    }
    public static final pyfunc_repr pyglobal_repr = new pyfunc_repr();

    static final class pyfunc_reversed extends PyBuiltinClass {
        pyfunc_reversed() { super("reversed"); }
        @Override public PyIter call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("reversed() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("reversed() does not accept kwargs");
            }
            return args[0].reversed();
        }
    }
    public static final pyfunc_reversed pyglobal_reversed = new pyfunc_reversed();

    static final class pyfunc_set extends PyBuiltinClass {
        pyfunc_set() { super("set"); }
        @Override public PySet call(PyObject[] args, PyDict kwargs) {
            if (args.length > 1) {
                throw new IllegalArgumentException("set() takes 0 or 1 arguments");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("set() does not accept kwargs");
            }
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
        pyfunc_slice() { super("slice"); }
        @Override public PySlice call(PyObject[] args, PyDict kwargs) {
            if ((args.length < 1) || (args.length > 3)) {
                throw new IllegalArgumentException("slice() takes 1 to 3 arguments");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("slice() does not accept kwargs");
            }
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
        pyfunc_str() { super("str"); }
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
                if ((item instanceof PyInt) || (item instanceof PyBool)) {
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
        pyfunc_tuple() { super("tuple"); }
        @Override public PyTuple call(PyObject[] args, PyDict kwargs) {
            if (args.length > 1) {
                throw new IllegalArgumentException("tuple() takes 0 or 1 arguments");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("tuple() does not accept kwargs");
            }
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
        pyfunc_type() { super("type"); }
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
        pyfunc_zip() { super("zip"); }
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

    // Helper functions used by the code generator
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
