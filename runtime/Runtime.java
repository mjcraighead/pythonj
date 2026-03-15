// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

abstract class PyTruthyObject extends PyObject {
    @Override public final boolean boolValue() { return true; }
}

abstract class PyIter extends PyTruthyObject {
    @Override public final boolean hasIter() { return true; }
    @Override public final PyIter iter() { return this; }
}

abstract class PyType extends PyTruthyObject {
    @Override public PyObject or(PyObject rhs) {
        if ((rhs instanceof PyType) || (rhs instanceof PyNone)) {
            throw new UnsupportedOperationException("type unions are unsupported");
        } else {
            return super.or(rhs);
        }
    }

    public PyDescriptor getDescriptor(String name) { return null; }

    @Override public boolean contains(PyObject rhs) { return defaultContains(rhs); }

    public abstract String name();
}

class PyBuiltinClass extends PyType {
    protected final String typeName;
    protected final Class<? extends PyObject> instanceClass;

    protected PyBuiltinClass(String name, Class<? extends PyObject> _instanceClass) {
        typeName = name;
        instanceClass = _instanceClass;
    }
    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "__name__": return new PyString(typeName);
            default: return super.getAttr(key);
        }
    }
    @Override public final void setAttr(String key, PyObject value) {
        throw PyTypeError.raiseFormat("cannot set %s attribute of immutable type %s", PyString.reprOf(key), PyString.reprOf(typeName));
    }
    @Override public final void delAttr(String key) {
        throw PyTypeError.raiseFormat("cannot set %s attribute of immutable type %s", PyString.reprOf(key), PyString.reprOf(typeName));
    }

    @Override public final String repr() { return "<class '" + typeName + "'>"; }
    @Override public final Runtime.pyclass_type type() { return Runtime.pyglobal_type; }
    @Override public String name() { return typeName; }
}

abstract class PyDescriptor extends PyTruthyObject {
    abstract public PyObject bind();
}

class PyMethodDescriptor extends PyDescriptor {
    protected final PyType owner;
    protected final String name;

    protected PyMethodDescriptor(PyType _owner, String _name) {
        owner = _owner;
        name = _name;
    }

    @Override public final PyObject bind() {
        return this;
    }

    @Override public final String repr() { return "<method " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects>"; }
    @Override public final PyBuiltinClass type() { return Runtime.pytype_method_descriptor; }
}

abstract class PyClassMethodDescriptor extends PyDescriptor {
    protected final PyType owner;
    protected final String name;

    protected PyClassMethodDescriptor(PyType _owner, String _name) {
        owner = _owner;
        name = _name;
    }

    @Override public final String repr() { return "<method " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects>"; }
    @Override public final PyBuiltinClass type() { return Runtime.pytype_classmethod_descriptor; }
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
    public static final PyBuiltinClass pytype_classmethod_descriptor = new PyBuiltinClass("classmethod_descriptor", PyClassMethodDescriptor.class);
    public static final PyBuiltinClass pytype_method_descriptor = new PyBuiltinClass("method_descriptor", PyMethodDescriptor.class);

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

    static final class pyfunc_ascii extends PyBuiltinFunction {
        pyfunc_ascii() { super("ascii"); }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
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
    }
    public static final pyfunc_ascii pyglobal_ascii = new pyfunc_ascii();

    static final class pyclass_bool extends PyBuiltinClass {
        pyclass_bool() { super("bool", PyBool.class); }
        @Override public PyBool call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireMaxArgs(args, 1, typeName);
            if (args.length == 1) {
                return PyBool.create(args[0].boolValue());
            }
            return PyBool.false_singleton;
        }
    }
    public static final pyclass_bool pyglobal_bool = new pyclass_bool();

    static final class pyclass_bytearray extends PyBuiltinClass {
        pyclass_bytearray() { super("bytearray", PyByteArray.class); }
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
    public static final pyclass_bytearray pyglobal_bytearray = new pyclass_bytearray();

    static final class pyclass_bytes extends PyBuiltinClass {
        pyclass_bytes() { super("bytes", PyBytes.class); }
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
    public static final pyclass_bytes pyglobal_bytes = new pyclass_bytes();

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

    static final class pyfunc_delattr extends PyBuiltinFunction {
        pyfunc_delattr() { super("delattr"); }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, funcName);
            requireExactArgs(args, 2, funcName);
            if (args[1] instanceof PyString name) {
                args[0].delAttr(name.value);
                return PyNone.singleton;
            } else {
                throw PyTypeError.raiseFormat("attribute name must be string, not %s", PyString.reprOf(args[1].type().name()));
            }
        }
    }
    public static final pyfunc_delattr pyglobal_delattr = new pyfunc_delattr();

    static final class pyclass_dict extends PyBuiltinClass {
        pyclass_dict() { super("dict", PyDict.class); }

        @Override public PyDescriptor getDescriptor(String name) {
            switch (name) {
                case "clear": return pydesc_dict_clear;
                case "copy": return pydesc_dict_copy;
                case "fromkeys": return pydesc_dict_fromkeys;
                case "get": return pydesc_dict_get;
                case "items": return pydesc_dict_items;
                case "keys": return pydesc_dict_keys;
                case "pop": return pydesc_dict_pop;
                case "popitem": return pydesc_dict_popitem;
                case "setdefault": return pydesc_dict_setdefault;
                case "update": return pydesc_dict_update;
                case "values": return pydesc_dict_values;
                default: return null;
            }
        }
        @Override public PyObject getAttr(String key) {
            var desc = getDescriptor(key);
            if (desc != null) {
                return desc.bind();
            }
            return super.getAttr(key);
        }

        @Override public PyDict call(PyObject[] args, PyDict kwargs) {
            if (args.length > 1) {
                throw new IllegalArgumentException("dict() takes 0 or 1 arguments");
            }
            var ret = new PyDict();
            ret.pymethod_update(args, kwargs);
            return ret;
        }

        static final class PyDictClassMethod_fromkeys extends PyBuiltinMethod<PyType> {
            PyDictClassMethod_fromkeys(PyType _self) { super(_self); }
            @Override public String methodName() { return "fromkeys"; }
            @Override public PyDict call(PyObject[] args, PyDict kwargs) {
                Runtime.requireNoKwArgs(kwargs, "dict.fromkeys");
                Runtime.requireMinArgs(args, 1, "fromkeys");
                Runtime.requireMaxArgs(args, 2, "fromkeys");
                PyObject iterable = args[0];
                PyObject value = (args.length == 2) ? args[1] : PyNone.singleton;
                var ret = new PyDict();
                var iter = iterable.iter();
                for (var key = iter.next(); key != null; key = iter.next()) {
                    ret.items.put(key, value);
                }
                return ret;
            }
        }
    }
    public static final pyclass_dict pyglobal_dict = new pyclass_dict();
    private static final PyMethodDescriptor pydesc_dict_copy = new PyMethodDescriptor(pyglobal_dict, "copy");
    private static final PyMethodDescriptor pydesc_dict_clear = new PyMethodDescriptor(pyglobal_dict, "clear");
    private static final PyClassMethodDescriptor pydesc_dict_fromkeys = new PyClassMethodDescriptor(pyglobal_dict, "fromkeys") {
        @Override public final PyObject bind() {
            return new pyclass_dict.PyDictClassMethod_fromkeys(owner);
        }
    };
    private static final PyMethodDescriptor pydesc_dict_get = new PyMethodDescriptor(pyglobal_dict, "get");
    private static final PyMethodDescriptor pydesc_dict_keys = new PyMethodDescriptor(pyglobal_dict, "keys");
    private static final PyMethodDescriptor pydesc_dict_items = new PyMethodDescriptor(pyglobal_dict, "items");
    private static final PyMethodDescriptor pydesc_dict_pop = new PyMethodDescriptor(pyglobal_dict, "pop");
    private static final PyMethodDescriptor pydesc_dict_popitem = new PyMethodDescriptor(pyglobal_dict, "popitem");
    private static final PyMethodDescriptor pydesc_dict_setdefault = new PyMethodDescriptor(pyglobal_dict, "setdefault");
    private static final PyMethodDescriptor pydesc_dict_update = new PyMethodDescriptor(pyglobal_dict, "update");
    private static final PyMethodDescriptor pydesc_dict_values = new PyMethodDescriptor(pyglobal_dict, "values");

    static final class pyclass_enumerate extends PyBuiltinClass {
        pyclass_enumerate() { super("enumerate", PyEnumerate.class); }
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
    public static final pyclass_enumerate pyglobal_enumerate = new pyclass_enumerate();

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
            if (args[1] instanceof PyString name) {
                try {
                    return args[0].getAttr(name.value);
                } catch (PyRaise r) {
                    if ((args.length == 3) && (r.exc instanceof PyAttributeError)) {
                        return args[2];
                    }
                    throw r;
                }
            } else {
                throw PyTypeError.raiseFormat("attribute name must be string, not %s", PyString.reprOf(args[1].type().name()));
            }
        }
    }
    public static final pyfunc_getattr pyglobal_getattr = new pyfunc_getattr();

    static final class pyfunc_hasattr extends PyBuiltinFunction {
        pyfunc_hasattr() { super("hasattr"); }
        @Override public PyBool call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, funcName);
            requireExactArgs(args, 2, funcName);
            if (args[1] instanceof PyString name) {
                try {
                    args[0].getAttr(name.value);
                } catch (PyRaise r) {
                    if (r.exc instanceof PyAttributeError) {
                        return PyBool.false_singleton;
                    }
                    throw r;
                }
                return PyBool.true_singleton;
            } else {
                throw PyTypeError.raiseFormat("attribute name must be string, not %s", PyString.reprOf(args[1].type().name()));
            }
        }
    }
    public static final pyfunc_hasattr pyglobal_hasattr = new pyfunc_hasattr();

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

    static final class pyclass_int extends PyBuiltinClass {
        pyclass_int() { super("int", PyInt.class); }
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
    public static final pyclass_int pyglobal_int = new pyclass_int();

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
                throw new UnsupportedOperationException("isinstance() is unimplemented for type " + type.repr());
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

    static final class pyclass_list extends PyBuiltinClass {
        pyclass_list() { super("list", PyList.class); }
        @Override public PyMethodDescriptor getDescriptor(String name) {
            switch (name) {
                case "append": return pydesc_list_append;
                case "clear": return pydesc_list_clear;
                case "copy": return pydesc_list_copy;
                case "count": return pydesc_list_count;
                case "extend": return pydesc_list_extend;
                case "index": return pydesc_list_index;
                case "insert": return pydesc_list_insert;
                case "pop": return pydesc_list_pop;
                case "remove": return pydesc_list_remove;
                case "reverse": return pydesc_list_reverse;
                case "sort": return pydesc_list_sort;
                default: return null;
            }
        }
        @Override public PyObject getAttr(String key) {
            var desc = getDescriptor(key);
            if (desc != null) {
                return desc.bind();
            }
            return super.getAttr(key);
        }
        @Override public PyList call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireMaxArgs(args, 1, typeName);
            var ret = new PyList();
            if (args.length == 0) {
                return ret;
            }
            addIterableToCollection(ret.items, args[0]);
            return ret;
        }
    }
    public static final pyclass_list pyglobal_list = new pyclass_list();
    private static final PyMethodDescriptor pydesc_list_append = new PyMethodDescriptor(pyglobal_list, "append");
    private static final PyMethodDescriptor pydesc_list_clear = new PyMethodDescriptor(pyglobal_list, "clear");
    private static final PyMethodDescriptor pydesc_list_copy = new PyMethodDescriptor(pyglobal_list, "copy");
    private static final PyMethodDescriptor pydesc_list_count = new PyMethodDescriptor(pyglobal_list, "count");
    private static final PyMethodDescriptor pydesc_list_extend = new PyMethodDescriptor(pyglobal_list, "extend");
    private static final PyMethodDescriptor pydesc_list_index = new PyMethodDescriptor(pyglobal_list, "index");
    private static final PyMethodDescriptor pydesc_list_insert = new PyMethodDescriptor(pyglobal_list, "insert");
    private static final PyMethodDescriptor pydesc_list_pop = new PyMethodDescriptor(pyglobal_list, "pop");
    private static final PyMethodDescriptor pydesc_list_remove = new PyMethodDescriptor(pyglobal_list, "remove");
    private static final PyMethodDescriptor pydesc_list_reverse = new PyMethodDescriptor(pyglobal_list, "reverse");
    private static final PyMethodDescriptor pydesc_list_sort = new PyMethodDescriptor(pyglobal_list, "sort");

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

    static final class pyclass_object extends PyBuiltinClass {
        pyclass_object() { super("object", PyObject.class); }
    }
    public static final pyclass_object pyglobal_object = new pyclass_object();

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

    static final class pyclass_range extends PyBuiltinClass {
        pyclass_range() { super("range", PyRange.class); }
        @Override public PyMethodDescriptor getDescriptor(String name) {
            switch (name) {
                case "count": return pydesc_range_count;
                case "index": return pydesc_range_index;
                default: return null;
            }
        }
        @Override public PyObject getAttr(String key) {
            var desc = getDescriptor(key);
            if (desc != null) {
                return desc.bind();
            }
            return super.getAttr(key);
        }
        @Override public PyRange call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireMinArgs(args, 1, typeName);
            requireMaxArgs(args, 3, typeName);
            long start = 0, stop, step = 1;
            if (args.length == 1) {
                stop = args[0].indexValue();
            } else {
                start = args[0].indexValue();
                stop = args[1].indexValue();
                if (args.length == 3) {
                    step = args[2].indexValue();
                }
            }
            return new PyRange(start, stop, step);
        }
    }
    public static final pyclass_range pyglobal_range = new pyclass_range();
    private static final PyMethodDescriptor pydesc_range_count = new PyMethodDescriptor(pyglobal_range, "count");
    private static final PyMethodDescriptor pydesc_range_index = new PyMethodDescriptor(pyglobal_range, "index");

    static final class pyfunc_repr extends PyBuiltinFunction {
        pyfunc_repr() { super("repr"); }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            return new PyString(arg.repr());
        }
    }
    public static final pyfunc_repr pyglobal_repr = new pyfunc_repr();

    static final class pyclass_reversed extends PyBuiltinClass {
        pyclass_reversed() { super("reversed", PyReversed.class); }
        @Override public PyIter call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireExactArgs(args, 1, typeName);
            return args[0].reversed();
        }
    }
    public static final pyclass_reversed pyglobal_reversed = new pyclass_reversed();

    static final class pyclass_set extends PyBuiltinClass {
        pyclass_set() { super("set", PySet.class); }
        @Override public PyMethodDescriptor getDescriptor(String name) {
            switch (name) {
                case "add": return pydesc_set_add;
                case "clear": return pydesc_set_clear;
                case "discard": return pydesc_set_discard;
                case "update": return pydesc_set_update;
                default: return null;
            }
        }
        @Override public PyObject getAttr(String key) {
            var desc = getDescriptor(key);
            if (desc != null) {
                return desc.bind();
            }
            return super.getAttr(key);
        }
        @Override public PySet call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireMaxArgs(args, 1, typeName);
            var ret = new PySet();
            if (args.length == 0) {
                return ret;
            }
            addIterableToCollection(ret.items, args[0]);
            return ret;
        }
    }
    public static final pyclass_set pyglobal_set = new pyclass_set();
    private static final PyMethodDescriptor pydesc_set_add = new PyMethodDescriptor(pyglobal_set, "add");
    private static final PyMethodDescriptor pydesc_set_clear = new PyMethodDescriptor(pyglobal_set, "clear");
    private static final PyMethodDescriptor pydesc_set_discard = new PyMethodDescriptor(pyglobal_set, "discard");
    private static final PyMethodDescriptor pydesc_set_update = new PyMethodDescriptor(pyglobal_set, "update");

    static final class pyfunc_setattr extends PyBuiltinFunction {
        pyfunc_setattr() { super("setattr"); }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, funcName);
            requireExactArgs(args, 3, funcName);
            if (args[1] instanceof PyString name) {
                args[0].setAttr(name.value, args[2]);
                return PyNone.singleton;
            } else {
                throw PyTypeError.raiseFormat("attribute name must be string, not %s", PyString.reprOf(args[1].type().name()));
            }
        }
    }
    public static final pyfunc_setattr pyglobal_setattr = new pyfunc_setattr();

    static final class pyclass_slice extends PyBuiltinClass {
        pyclass_slice() { super("slice", PySlice.class); }
        @Override public PySlice call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireMinArgs(args, 1, typeName);
            requireMaxArgs(args, 3, typeName);
            PyObject start = PyNone.singleton;
            PyObject stop;
            PyObject step = PyNone.singleton;
            if (args.length == 1) {
                stop = args[0];
            } else {
                start = args[0];
                stop = args[1];
                if (args.length == 3) {
                    step = args[2];
                }
            }
            return new PySlice(start, stop, step);
        }
    }
    public static final pyclass_slice pyglobal_slice = new pyclass_slice();

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
            addIterableToCollection(ret.items, args[0]);
            Collections.sort(ret.items);
            return ret;
        }
    }
    public static final pyfunc_sorted pyglobal_sorted = new pyfunc_sorted();

    static final class pyclass_str extends PyBuiltinClass {
        pyclass_str() { super("str", PyString.class); }
        @Override public PyMethodDescriptor getDescriptor(String name) {
            switch (name) {
                case "capitalize": return pydesc_str_capitalize;
                case "casefold": return pydesc_str_casefold;
                case "center": return pydesc_str_center;
                case "count": return pydesc_str_count;
                case "encode": return pydesc_str_encode;
                case "endswith": return pydesc_str_endswith;
                case "expandtabs": return pydesc_str_expandtabs;
                case "find": return pydesc_str_find;
                case "format": return pydesc_str_format;
                case "format_map": return pydesc_str_format_map;
                case "index": return pydesc_str_index;
                case "isalnum": return pydesc_str_isalnum;
                case "isalpha": return pydesc_str_isalpha;
                case "isascii": return pydesc_str_isascii;
                case "isdecimal": return pydesc_str_isdecimal;
                case "isdigit": return pydesc_str_isdigit;
                case "isidentifier": return pydesc_str_isidentifier;
                case "islower": return pydesc_str_islower;
                case "isnumeric": return pydesc_str_isnumeric;
                case "isprintable": return pydesc_str_isprintable;
                case "isspace": return pydesc_str_isspace;
                case "istitle": return pydesc_str_istitle;
                case "isupper": return pydesc_str_isupper;
                case "join": return pydesc_str_join;
                case "ljust": return pydesc_str_ljust;
                case "lower": return pydesc_str_lower;
                case "lstrip": return pydesc_str_lstrip;
                // XXX maketrans (static method)
                case "partition": return pydesc_str_partition;
                case "removeprefix": return pydesc_str_removeprefix;
                case "removesuffix": return pydesc_str_removesuffix;
                case "replace": return pydesc_str_replace;
                case "rfind": return pydesc_str_rfind;
                case "rindex": return pydesc_str_rindex;
                case "rjust": return pydesc_str_rjust;
                case "rpartition": return pydesc_str_rpartition;
                case "rsplit": return pydesc_str_rsplit;
                case "rstrip": return pydesc_str_rstrip;
                case "split": return pydesc_str_split;
                case "splitlines": return pydesc_str_splitlines;
                case "startswith": return pydesc_str_startswith;
                case "strip": return pydesc_str_strip;
                case "swapcase": return pydesc_str_swapcase;
                case "title": return pydesc_str_title;
                case "translate": return pydesc_str_translate;
                case "upper": return pydesc_str_upper;
                case "zfill": return pydesc_str_zfill;
                default: return null;
            }
        }
        @Override public PyObject getAttr(String key) {
            var desc = getDescriptor(key);
            if (desc != null) {
                return desc.bind();
            }
            return super.getAttr(key);
        }
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
    public static final pyclass_str pyglobal_str = new pyclass_str();
    private static final PyMethodDescriptor pydesc_str_capitalize = new PyMethodDescriptor(pyglobal_str, "capitalize");
    private static final PyMethodDescriptor pydesc_str_casefold = new PyMethodDescriptor(pyglobal_str, "casefold");
    private static final PyMethodDescriptor pydesc_str_center = new PyMethodDescriptor(pyglobal_str, "center");
    private static final PyMethodDescriptor pydesc_str_count = new PyMethodDescriptor(pyglobal_str, "count");
    private static final PyMethodDescriptor pydesc_str_encode = new PyMethodDescriptor(pyglobal_str, "encode");
    private static final PyMethodDescriptor pydesc_str_endswith = new PyMethodDescriptor(pyglobal_str, "endswith");
    private static final PyMethodDescriptor pydesc_str_expandtabs = new PyMethodDescriptor(pyglobal_str, "expandtabs");
    private static final PyMethodDescriptor pydesc_str_find = new PyMethodDescriptor(pyglobal_str, "find");
    private static final PyMethodDescriptor pydesc_str_format = new PyMethodDescriptor(pyglobal_str, "format");
    private static final PyMethodDescriptor pydesc_str_format_map = new PyMethodDescriptor(pyglobal_str, "format_map");
    private static final PyMethodDescriptor pydesc_str_index = new PyMethodDescriptor(pyglobal_str, "index");
    private static final PyMethodDescriptor pydesc_str_isalnum = new PyMethodDescriptor(pyglobal_str, "isalnum");
    private static final PyMethodDescriptor pydesc_str_isalpha = new PyMethodDescriptor(pyglobal_str, "isalpha");
    private static final PyMethodDescriptor pydesc_str_isascii = new PyMethodDescriptor(pyglobal_str, "isascii");
    private static final PyMethodDescriptor pydesc_str_isdecimal = new PyMethodDescriptor(pyglobal_str, "isdecimal");
    private static final PyMethodDescriptor pydesc_str_isdigit = new PyMethodDescriptor(pyglobal_str, "isdigit");
    private static final PyMethodDescriptor pydesc_str_isidentifier = new PyMethodDescriptor(pyglobal_str, "isidentifier");
    private static final PyMethodDescriptor pydesc_str_islower = new PyMethodDescriptor(pyglobal_str, "islower");
    private static final PyMethodDescriptor pydesc_str_isnumeric = new PyMethodDescriptor(pyglobal_str, "isnumeric");
    private static final PyMethodDescriptor pydesc_str_isprintable = new PyMethodDescriptor(pyglobal_str, "isprintable");
    private static final PyMethodDescriptor pydesc_str_isspace = new PyMethodDescriptor(pyglobal_str, "isspace");
    private static final PyMethodDescriptor pydesc_str_istitle = new PyMethodDescriptor(pyglobal_str, "istitle");
    private static final PyMethodDescriptor pydesc_str_isupper = new PyMethodDescriptor(pyglobal_str, "isupper");
    private static final PyMethodDescriptor pydesc_str_join = new PyMethodDescriptor(pyglobal_str, "join");
    private static final PyMethodDescriptor pydesc_str_ljust = new PyMethodDescriptor(pyglobal_str, "ljust");
    private static final PyMethodDescriptor pydesc_str_lower = new PyMethodDescriptor(pyglobal_str, "lower");
    private static final PyMethodDescriptor pydesc_str_lstrip = new PyMethodDescriptor(pyglobal_str, "lstrip");
    // XXX maketrans (static method)
    private static final PyMethodDescriptor pydesc_str_partition = new PyMethodDescriptor(pyglobal_str, "partition");
    private static final PyMethodDescriptor pydesc_str_removeprefix = new PyMethodDescriptor(pyglobal_str, "removeprefix");
    private static final PyMethodDescriptor pydesc_str_removesuffix = new PyMethodDescriptor(pyglobal_str, "removesuffix");
    private static final PyMethodDescriptor pydesc_str_replace = new PyMethodDescriptor(pyglobal_str, "replace");
    private static final PyMethodDescriptor pydesc_str_rfind = new PyMethodDescriptor(pyglobal_str, "rfind");
    private static final PyMethodDescriptor pydesc_str_rindex = new PyMethodDescriptor(pyglobal_str, "rindex");
    private static final PyMethodDescriptor pydesc_str_rjust = new PyMethodDescriptor(pyglobal_str, "rjust");
    private static final PyMethodDescriptor pydesc_str_rpartition = new PyMethodDescriptor(pyglobal_str, "rpartition");
    private static final PyMethodDescriptor pydesc_str_rsplit = new PyMethodDescriptor(pyglobal_str, "rsplit");
    private static final PyMethodDescriptor pydesc_str_rstrip = new PyMethodDescriptor(pyglobal_str, "rstrip");
    private static final PyMethodDescriptor pydesc_str_split = new PyMethodDescriptor(pyglobal_str, "split");
    private static final PyMethodDescriptor pydesc_str_splitlines = new PyMethodDescriptor(pyglobal_str, "splitlines");
    private static final PyMethodDescriptor pydesc_str_startswith = new PyMethodDescriptor(pyglobal_str, "startswith");
    private static final PyMethodDescriptor pydesc_str_strip = new PyMethodDescriptor(pyglobal_str, "strip");
    private static final PyMethodDescriptor pydesc_str_swapcase = new PyMethodDescriptor(pyglobal_str, "swapcase");
    private static final PyMethodDescriptor pydesc_str_title = new PyMethodDescriptor(pyglobal_str, "title");
    private static final PyMethodDescriptor pydesc_str_translate = new PyMethodDescriptor(pyglobal_str, "translate");
    private static final PyMethodDescriptor pydesc_str_upper = new PyMethodDescriptor(pyglobal_str, "upper");
    private static final PyMethodDescriptor pydesc_str_zfill = new PyMethodDescriptor(pyglobal_str, "zfill");

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

    static final class pyclass_tuple extends PyBuiltinClass {
        pyclass_tuple() { super("tuple", PyTuple.class); }
        @Override public PyMethodDescriptor getDescriptor(String name) {
            switch (name) {
                case "count": return pydesc_tuple_count;
                case "index": return pydesc_tuple_index;
                default: return null;
            }
        }
        @Override public PyObject getAttr(String key) {
            var desc = getDescriptor(key);
            if (desc != null) {
                return desc.bind();
            }
            return super.getAttr(key);
        }
        @Override public PyTuple call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireMaxArgs(args, 1, typeName);
            if (args.length == 0) {
                return new PyTuple();
            }
            var list = new ArrayList<PyObject>();
            addIterableToCollection(list, args[0]);
            return new PyTuple(list);
        }
    }
    public static final pyclass_tuple pyglobal_tuple = new pyclass_tuple();
    private static final PyMethodDescriptor pydesc_tuple_count = new PyMethodDescriptor(pyglobal_tuple, "count");
    private static final PyMethodDescriptor pydesc_tuple_index = new PyMethodDescriptor(pyglobal_tuple, "index");

    static final class pyclass_type extends PyBuiltinClass {
        pyclass_type() { super("type", PyType.class); }
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
    public static final pyclass_type pyglobal_type = new pyclass_type();

    static final class pyclass_zip extends PyBuiltinClass {
        pyclass_zip() { super("zip", PyZip.class); }
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
    public static final pyclass_zip pyglobal_zip = new pyclass_zip();

    static final class pyclass_ArithmeticError extends PyBuiltinClass {
        pyclass_ArithmeticError() { super("ArithmeticError", PyArithmeticError.class); }
        @Override public PyArithmeticError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyArithmeticError(args);
        }
    }
    public static final pyclass_ArithmeticError pyglobal_ArithmeticError = new pyclass_ArithmeticError();

    static final class pyclass_AssertionError extends PyBuiltinClass {
        pyclass_AssertionError() { super("AssertionError", PyAssertionError.class); }
        @Override public PyAssertionError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyAssertionError(args);
        }
    }
    public static final pyclass_AssertionError pyglobal_AssertionError = new pyclass_AssertionError();

    static final class pyclass_AttributeError extends PyBuiltinClass {
        pyclass_AttributeError() { super("AttributeError", PyAttributeError.class); }
        @Override public PyAttributeError call(PyObject[] args, PyDict kwargs) {
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("AttributeError() does not accept kwargs");
            }
            return new PyAttributeError(args);
        }
    }
    public static final pyclass_AttributeError pyglobal_AttributeError = new pyclass_AttributeError();

    static final class pyclass_BaseException extends PyBuiltinClass {
        pyclass_BaseException() { super("BaseException", PyBaseException.class); }
        @Override public PyBaseException call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyBaseException(args);
        }
    }
    public static final pyclass_BaseException pyglobal_BaseException = new pyclass_BaseException();

    static final class pyclass_Exception extends PyBuiltinClass {
        pyclass_Exception() { super("Exception", PyException.class); }
        @Override public PyException call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyException(args);
        }
    }
    public static final pyclass_Exception pyglobal_Exception = new pyclass_Exception();

    static final class pyclass_IndexError extends PyBuiltinClass {
        pyclass_IndexError() { super("IndexError", PyIndexError.class); }
        @Override public PyIndexError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyIndexError(args);
        }
    }
    public static final pyclass_IndexError pyglobal_IndexError = new pyclass_IndexError();

    static final class pyclass_KeyError extends PyBuiltinClass {
        pyclass_KeyError() { super("KeyError", PyKeyError.class); }
        @Override public PyKeyError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyKeyError(args);
        }
    }
    public static final pyclass_KeyError pyglobal_KeyError = new pyclass_KeyError();

    static final class pyclass_LookupError extends PyBuiltinClass {
        pyclass_LookupError() { super("LookupError", PyLookupError.class); }
        @Override public PyLookupError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyLookupError(args);
        }
    }
    public static final pyclass_LookupError pyglobal_LookupError = new pyclass_LookupError();

    static final class pyclass_StopIteration extends PyBuiltinClass {
        pyclass_StopIteration() { super("StopIteration", PyStopIteration.class); }
        @Override public PyStopIteration call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyStopIteration(args);
        }
    }
    public static final pyclass_StopIteration pyglobal_StopIteration = new pyclass_StopIteration();

    static final class pyclass_TypeError extends PyBuiltinClass {
        pyclass_TypeError() { super("TypeError", PyTypeError.class); }
        @Override public PyTypeError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyTypeError(args);
        }
    }
    public static final pyclass_TypeError pyglobal_TypeError = new pyclass_TypeError();

    static final class pyclass_ValueError extends PyBuiltinClass {
        pyclass_ValueError() { super("ValueError", PyValueError.class); }
        @Override public PyValueError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyValueError(args);
        }
    }
    public static final pyclass_ValueError pyglobal_ValueError = new pyclass_ValueError();

    static final class pyclass_ZeroDivisionError extends PyBuiltinClass {
        pyclass_ZeroDivisionError() { super("ZeroDivisionError", PyZeroDivisionError.class); }
        @Override public PyZeroDivisionError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyZeroDivisionError(args);
        }
    }
    public static final pyclass_ZeroDivisionError pyglobal_ZeroDivisionError = new pyclass_ZeroDivisionError();

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
    public static PyRaise raiseNamedReadOnlyAttr(PyObject obj, String key) {
        return PyAttributeError.raise(PyString.reprOf(obj.type().name()) + " object attribute " + PyString.reprOf(key) + " is read-only");
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
    public static void addIterableToCollection(Collection<PyObject> list, PyObject iterable) {
        var iter = iterable.iter();
        for (var item = iter.next(); item != null; item = iter.next()) {
            list.add(item);
        }
    }
    public static ArrayList<PyObject> addStarToArrayList(ArrayList<PyObject> list, PyObject iterable) {
        if (iterable.hasIter()) {
            addIterableToCollection(list, iterable);
            return list;
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
    public static int asSliceIndexAllowNull(PyObject obj, int defaultIndex, int n) {
        if (obj == null) {
            return defaultIndex;
        }
        if (!obj.hasIndex()) {
            throw PyTypeError.raise("slice indices must be integers or have an __index__ method");
        }
        int i = Math.toIntExact(obj.indexValue());
        if (i < 0) {
            i += n;
            i = Math.max(i, 0);
        } else {
            i = Math.min(i, n);
        }
        return i;
    }
    public static int asSearchIndexAllowNone(PyObject obj, int defaultIndex, int n) {
        if (obj == PyNone.singleton) {
            return defaultIndex;
        }
        if (!obj.hasIndex()) {
            throw PyTypeError.raise("slice indices must be integers or None or have an __index__ method");
        }
        int i = Math.toIntExact(obj.indexValue());
        if (i < 0) {
            i += n;
            i = Math.max(i, 0);
        }
        return i;
    }
    public static void unsupportedSearchIndexAllowNone(PyObject obj, String msg) {
        if (obj == PyNone.singleton) {
            return;
        }
        if (!obj.hasIndex()) {
            throw PyTypeError.raise("slice indices must be integers or None or have an __index__ method");
        }
        throw new UnsupportedOperationException(msg);
    }
}
