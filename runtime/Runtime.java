// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

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
    @Override public final PyObject getAttr(String key) {
        var desc = getDescriptor(key);
        if (desc != null) {
            return desc.get(null);
        }
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
    @Override public final Runtime.pyclass_type type() { return Runtime.pyclass_type.singleton; }
    @Override public final String name() { return typeName; }
}

abstract class PyDescriptor extends PyTruthyObject {
    abstract public PyObject get(PyObject instance);
    abstract public void set(PyObject instance, PyObject value);
    abstract public void delete(PyObject instance);
}

abstract class PyGettableDescriptor extends PyDescriptor {
    protected final PyType owner;
    protected final String name;
    protected final Function<PyObject, PyObject> getter;

    protected PyGettableDescriptor(PyType _owner, String _name, Function<PyObject, PyObject> _getter) {
        owner = _owner;
        name = _name;
        getter = _getter;
    }

    @Override public final PyObject get(PyObject instance) {
        if (instance == null) {
            return this;
        } else {
            return getter.apply(instance);
        }
    }
}

final class PyMemberDescriptor extends PyGettableDescriptor {
    private static final PyBuiltinClass type_singleton = new PyBuiltinClass("member_descriptor", PyMemberDescriptor.class);

    protected PyMemberDescriptor(PyType _owner, String _name, Function<PyObject, PyObject> _getter) {
        super(_owner, _name, _getter);
    }
    @Override public final void set(PyObject instance, PyObject value) {
        throw PyAttributeError.raise("readonly attribute");
    }
    @Override public final void delete(PyObject instance) {
        throw PyAttributeError.raise("readonly attribute");
    }

    @Override public final String repr() { return "<member " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects>"; }
    @Override public final PyBuiltinClass type() { return type_singleton; }
}

final class PyGetSetDescriptor extends PyGettableDescriptor {
    private static final PyBuiltinClass type_singleton = new PyBuiltinClass("getset_descriptor", PyGetSetDescriptor.class);

    protected PyGetSetDescriptor(PyType _owner, String _name, Function<PyObject, PyObject> _getter) {
        super(_owner, _name, _getter);
    }
    @Override public final void set(PyObject instance, PyObject value) {
        throw PyAttributeError.raise("attribute " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects is not writable");
    }
    @Override public final void delete(PyObject instance) {
        throw PyAttributeError.raise("attribute " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects is not writable");
    }

    @Override public final String repr() { return "<attribute " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects>"; }
    @Override public final PyBuiltinClass type() { return type_singleton; }
}

final class PyMethodDescriptor extends PyGettableDescriptor {
    private static final PyBuiltinClass type_singleton = new PyBuiltinClass("method_descriptor", PyMethodDescriptor.class);

    protected PyMethodDescriptor(PyType _owner, String _name, Function<PyObject, PyObject> _getter) {
        super(_owner, _name, _getter);
    }
    @Override public final void set(PyObject instance, PyObject value) {
        throw Runtime.raiseNamedReadOnlyAttr(owner, name);
    }
    @Override public final void delete(PyObject instance) {
        throw Runtime.raiseNamedReadOnlyAttr(owner, name);
    }

    @Override public final String repr() { return "<method " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects>"; }
    @Override public final PyBuiltinClass type() { return type_singleton; }
}

final class PyClassMethodDescriptor extends PyDescriptor {
    private static final PyBuiltinClass type_singleton = new PyBuiltinClass("classmethod_descriptor", PyClassMethodDescriptor.class);

    protected final PyType owner;
    protected final String name;
    protected final Function<PyType, PyObject> getter;

    protected PyClassMethodDescriptor(PyType _owner, String _name, Function<PyType, PyObject> _getter) {
        owner = _owner;
        name = _name;
        getter = _getter;
    }
    @Override public final PyObject get(PyObject instance) {
        return getter.apply(owner);
    }
    @Override public final void set(PyObject instance, PyObject value) {
        throw Runtime.raiseNamedReadOnlyAttr(owner, name);
    }
    @Override public final void delete(PyObject instance) {
        throw Runtime.raiseNamedReadOnlyAttr(owner, name);
    }

    @Override public final String repr() { return "<method " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects>"; }
    @Override public final PyBuiltinClass type() { return type_singleton; }
}

final class PyStaticMethod extends PyDescriptor {
    private static final PyBuiltinClass type_singleton = new PyBuiltinClass("staticmethod", PyStaticMethod.class);

    protected final PyType owner;
    protected final String name;
    protected final PyObject func;

    protected PyStaticMethod(PyType _owner, String _name, PyObject _func) {
        owner = _owner;
        name = _name;
        func = _func;
    }

    @Override public final PyObject get(PyObject instance) {
        return func;
    }
    @Override public final void set(PyObject instance, PyObject value) {
        throw Runtime.raiseNamedReadOnlyAttr(owner, name);
    }
    @Override public final void delete(PyObject instance) {
        throw Runtime.raiseNamedReadOnlyAttr(owner, name);
    }

    @Override public final String repr() { return "<staticmethod(" + func.repr() + ")>"; }
    @Override public final PyBuiltinClass type() { return type_singleton; }
}

abstract class PyBuiltinFunctionOrMethod extends PyTruthyObject {
    private static final PyBuiltinClass type_singleton = new PyBuiltinClass("builtin_function_or_method", PyBuiltinFunctionOrMethod.class);
    @Override public final PyBuiltinClass type() { return type_singleton; }
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
    private static final PyBuiltinClass type_singleton = new PyBuiltinClass("function", PyUserFunction.class);
    private final String funcName;
    protected PyUserFunction(String name) { funcName = name; }
    @Override public PyBuiltinClass type() { return type_singleton; }
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
        public static final pyclass_bool singleton = new pyclass_bool();

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

    static final class pyclass_bytearray extends PyBuiltinClass {
        public static final pyclass_bytearray singleton = new pyclass_bytearray();

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

    static final class pyclass_bytes extends PyBuiltinClass {
        public static final pyclass_bytes singleton = new pyclass_bytes();
        private static final PyMethodDescriptor pydesc_capitalize = new PyMethodDescriptor(singleton, "capitalize", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "capitalize"));
        private static final PyMethodDescriptor pydesc_center = new PyMethodDescriptor(singleton, "center", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "center"));
        private static final PyMethodDescriptor pydesc_count = new PyMethodDescriptor(singleton, "count", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "count"));
        private static final PyMethodDescriptor pydesc_decode = new PyMethodDescriptor(singleton, "decode", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "decode"));
        private static final PyMethodDescriptor pydesc_endswith = new PyMethodDescriptor(singleton, "endswith", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "endswith"));
        private static final PyMethodDescriptor pydesc_expandtabs = new PyMethodDescriptor(singleton, "expandtabs", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "expandtabs"));
        private static final PyMethodDescriptor pydesc_find = new PyMethodDescriptor(singleton, "find", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "find"));
        private static final PyClassMethodDescriptor pydesc_fromhex = new PyClassMethodDescriptor(singleton, "fromhex", pyclass_bytes.PyBytesClassMethod_fromhex::new);
        private static final PyMethodDescriptor pydesc_hex = new PyMethodDescriptor(singleton, "hex", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "hex"));
        private static final PyMethodDescriptor pydesc_index = new PyMethodDescriptor(singleton, "index", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "index"));
        private static final PyMethodDescriptor pydesc_isalnum = new PyMethodDescriptor(singleton, "isalnum", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "isalnum"));
        private static final PyMethodDescriptor pydesc_isalpha = new PyMethodDescriptor(singleton, "isalpha", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "isalpha"));
        private static final PyMethodDescriptor pydesc_isascii = new PyMethodDescriptor(singleton, "isascii", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "isascii"));
        private static final PyMethodDescriptor pydesc_isdigit = new PyMethodDescriptor(singleton, "isdigit", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "isdigit"));
        private static final PyMethodDescriptor pydesc_islower = new PyMethodDescriptor(singleton, "islower", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "islower"));
        private static final PyMethodDescriptor pydesc_isspace = new PyMethodDescriptor(singleton, "isspace", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "isspace"));
        private static final PyMethodDescriptor pydesc_istitle = new PyMethodDescriptor(singleton, "istitle", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "istitle"));
        private static final PyMethodDescriptor pydesc_isupper = new PyMethodDescriptor(singleton, "isupper", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "isupper"));
        private static final PyMethodDescriptor pydesc_join = new PyMethodDescriptor(singleton, "join", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "join"));
        private static final PyMethodDescriptor pydesc_ljust = new PyMethodDescriptor(singleton, "ljust", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "ljust"));
        private static final PyMethodDescriptor pydesc_lower = new PyMethodDescriptor(singleton, "lower", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "lower"));
        private static final PyMethodDescriptor pydesc_lstrip = new PyMethodDescriptor(singleton, "lstrip", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "lstrip"));
        private static final PyStaticMethod pydesc_maketrans = new PyStaticMethod(singleton, "maketrans", new pyclass_bytes.PyBytesStaticMethod_maketrans(singleton));
        private static final PyMethodDescriptor pydesc_partition = new PyMethodDescriptor(singleton, "partition", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "partition"));
        private static final PyMethodDescriptor pydesc_removeprefix = new PyMethodDescriptor(singleton, "removeprefix", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "removeprefix"));
        private static final PyMethodDescriptor pydesc_removesuffix = new PyMethodDescriptor(singleton, "removesuffix", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "removesuffix"));
        private static final PyMethodDescriptor pydesc_replace = new PyMethodDescriptor(singleton, "replace", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "replace"));
        private static final PyMethodDescriptor pydesc_rfind = new PyMethodDescriptor(singleton, "rfind", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "rfind"));
        private static final PyMethodDescriptor pydesc_rindex = new PyMethodDescriptor(singleton, "rindex", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "rindex"));
        private static final PyMethodDescriptor pydesc_rjust = new PyMethodDescriptor(singleton, "rjust", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "rjust"));
        private static final PyMethodDescriptor pydesc_rpartition = new PyMethodDescriptor(singleton, "rpartition", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "rpartition"));
        private static final PyMethodDescriptor pydesc_rsplit = new PyMethodDescriptor(singleton, "rsplit", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "rsplit"));
        private static final PyMethodDescriptor pydesc_rstrip = new PyMethodDescriptor(singleton, "rstrip", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "rstrip"));
        private static final PyMethodDescriptor pydesc_split = new PyMethodDescriptor(singleton, "split", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "split"));
        private static final PyMethodDescriptor pydesc_splitlines = new PyMethodDescriptor(singleton, "splitlines", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "splitlines"));
        private static final PyMethodDescriptor pydesc_startswith = new PyMethodDescriptor(singleton, "startswith", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "startswith"));
        private static final PyMethodDescriptor pydesc_strip = new PyMethodDescriptor(singleton, "strip", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "strip"));
        private static final PyMethodDescriptor pydesc_swapcase = new PyMethodDescriptor(singleton, "swapcase", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "swapcase"));
        private static final PyMethodDescriptor pydesc_title = new PyMethodDescriptor(singleton, "title", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "title"));
        private static final PyMethodDescriptor pydesc_translate = new PyMethodDescriptor(singleton, "translate", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "translate"));
        private static final PyMethodDescriptor pydesc_upper = new PyMethodDescriptor(singleton, "upper", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "upper"));
        private static final PyMethodDescriptor pydesc_zfill = new PyMethodDescriptor(singleton, "zfill", obj -> new PyBytes.PyBytesMethodUnimplemented(obj, "zfill"));

        pyclass_bytes() { super("bytes", PyBytes.class); }
        @Override public PyDescriptor getDescriptor(String name) {
            switch (name) {
                case "capitalize": return pydesc_capitalize;
                case "center": return pydesc_center;
                case "count": return pydesc_count;
                case "decode": return pydesc_decode;
                case "endswith": return pydesc_endswith;
                case "expandtabs": return pydesc_expandtabs;
                case "find": return pydesc_find;
                case "fromhex": return pydesc_fromhex;
                case "hex": return pydesc_hex;
                case "index": return pydesc_index;
                case "isalnum": return pydesc_isalnum;
                case "isalpha": return pydesc_isalpha;
                case "isascii": return pydesc_isascii;
                case "isdigit": return pydesc_isdigit;
                case "islower": return pydesc_islower;
                case "isspace": return pydesc_isspace;
                case "istitle": return pydesc_istitle;
                case "isupper": return pydesc_isupper;
                case "join": return pydesc_join;
                case "ljust": return pydesc_ljust;
                case "lower": return pydesc_lower;
                case "lstrip": return pydesc_lstrip;
                case "maketrans": return pydesc_maketrans;
                case "partition": return pydesc_partition;
                case "removeprefix": return pydesc_removeprefix;
                case "removesuffix": return pydesc_removesuffix;
                case "replace": return pydesc_replace;
                case "rfind": return pydesc_rfind;
                case "rindex": return pydesc_rindex;
                case "rjust": return pydesc_rjust;
                case "rpartition": return pydesc_rpartition;
                case "rsplit": return pydesc_rsplit;
                case "rstrip": return pydesc_rstrip;
                case "split": return pydesc_split;
                case "splitlines": return pydesc_splitlines;
                case "startswith": return pydesc_startswith;
                case "strip": return pydesc_strip;
                case "swapcase": return pydesc_swapcase;
                case "title": return pydesc_title;
                case "translate": return pydesc_translate;
                case "upper": return pydesc_upper;
                case "zfill": return pydesc_zfill;
                default: return null;
            }
        }
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

        protected static final class PyBytesClassMethod_fromhex extends PyBuiltinMethod<PyType> {
            PyBytesClassMethod_fromhex(PyType self) { super(self); }
            @Override public String methodName() { return "fromhex"; }
            @Override public PyObject call(PyObject[] args, PyDict kwargs) {
                throw new UnsupportedOperationException("bytes.fromhex() unimplemented");
            }
        }
        protected static final class PyBytesStaticMethod_maketrans extends PyBuiltinMethod<PyType> {
            PyBytesStaticMethod_maketrans(PyType self) { super(self); }
            @Override public String methodName() { return "maketrans"; }
            @Override public PyObject call(PyObject[] args, PyDict kwargs) {
                throw new UnsupportedOperationException("bytes.maketrans() unimplemented");
            }
        }
    }

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
        public static final pyclass_dict singleton = new pyclass_dict();
        private static final PyMethodDescriptor pydesc_clear = new PyMethodDescriptor(singleton, "clear", PyDict.PyDictMethod_clear::new);
        private static final PyMethodDescriptor pydesc_copy = new PyMethodDescriptor(singleton, "copy", PyDict.PyDictMethod_copy::new);
        private static final PyClassMethodDescriptor pydesc_fromkeys = new PyClassMethodDescriptor(singleton, "fromkeys", pyclass_dict.PyDictClassMethod_fromkeys::new);
        private static final PyMethodDescriptor pydesc_get = new PyMethodDescriptor(singleton, "get", PyDict.PyDictMethod_get::new);
        private static final PyMethodDescriptor pydesc_keys = new PyMethodDescriptor(singleton, "keys", PyDict.PyDictMethod_keys::new);
        private static final PyMethodDescriptor pydesc_items = new PyMethodDescriptor(singleton, "items", PyDict.PyDictMethod_items::new);
        private static final PyMethodDescriptor pydesc_pop = new PyMethodDescriptor(singleton, "pop", PyDict.PyDictMethod_pop::new);
        private static final PyMethodDescriptor pydesc_popitem = new PyMethodDescriptor(singleton, "popitem", PyDict.PyDictMethod_popitem::new);
        private static final PyMethodDescriptor pydesc_setdefault = new PyMethodDescriptor(singleton, "setdefault", PyDict.PyDictMethod_setdefault::new);
        private static final PyMethodDescriptor pydesc_update = new PyMethodDescriptor(singleton, "update", PyDict.PyDictMethod_update::new);
        private static final PyMethodDescriptor pydesc_values = new PyMethodDescriptor(singleton, "values", PyDict.PyDictMethod_values::new);

        pyclass_dict() { super("dict", PyDict.class); }
        @Override public PyDescriptor getDescriptor(String name) {
            switch (name) {
                case "clear": return pydesc_clear;
                case "copy": return pydesc_copy;
                case "fromkeys": return pydesc_fromkeys;
                case "get": return pydesc_get;
                case "items": return pydesc_items;
                case "keys": return pydesc_keys;
                case "pop": return pydesc_pop;
                case "popitem": return pydesc_popitem;
                case "setdefault": return pydesc_setdefault;
                case "update": return pydesc_update;
                case "values": return pydesc_values;
                default: return null;
            }
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

    static final class pyclass_enumerate extends PyBuiltinClass {
        public static final pyclass_enumerate singleton = new pyclass_enumerate();

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
        public static final pyclass_int singleton = new pyclass_int();
        private static final PyMethodDescriptor pydesc_as_integer_ratio = new PyMethodDescriptor(singleton, "as_integer_ratio", obj -> new PyInt.PyIntMethodUnimplemented(obj, "as_integer_ratio"));
        private static final PyMethodDescriptor pydesc_bit_count = new PyMethodDescriptor(singleton, "bit_count", obj -> new PyInt.PyIntMethodUnimplemented(obj, "bit_count"));
        private static final PyMethodDescriptor pydesc_bit_length = new PyMethodDescriptor(singleton, "bit_length", obj -> new PyInt.PyIntMethodUnimplemented(obj, "bit_length"));
        private static final PyMethodDescriptor pydesc_conjugate = new PyMethodDescriptor(singleton, "conjugate", obj -> new PyInt.PyIntMethodUnimplemented(obj, "conjugate"));
        private static final PyGetSetDescriptor pydesc_denominator = new PyGetSetDescriptor(singleton, "denominator", obj -> PyInt.singleton_1);
        private static final PyClassMethodDescriptor pydesc_from_bytes = new PyClassMethodDescriptor(singleton, "from_bytes", pyclass_int.PyIntClassMethod_from_bytes::new);
        private static final PyGetSetDescriptor pydesc_imag = new PyGetSetDescriptor(singleton, "imag", obj -> PyInt.singleton_0);
        private static final PyMethodDescriptor pydesc_is_integer = new PyMethodDescriptor(singleton, "is_integer", obj -> new PyInt.PyIntMethodUnimplemented(obj, "is_integer"));
        private static final PyGetSetDescriptor pydesc_numerator = new PyGetSetDescriptor(singleton, "numerator", obj -> obj);
        private static final PyGetSetDescriptor pydesc_real = new PyGetSetDescriptor(singleton, "real", obj -> obj);
        private static final PyMethodDescriptor pydesc_to_bytes = new PyMethodDescriptor(singleton, "to_bytes", obj -> new PyInt.PyIntMethodUnimplemented(obj, "to_bytes"));

        pyclass_int() { super("int", PyInt.class); }
        @Override public PyDescriptor getDescriptor(String name) {
            switch (name) {
                case "as_integer_ratio": return pydesc_as_integer_ratio;
                case "bit_count": return pydesc_bit_count;
                case "bit_length": return pydesc_bit_length;
                case "conjugate": return pydesc_conjugate;
                case "denominator": return pydesc_denominator;
                case "from_bytes": return pydesc_from_bytes;
                case "imag": return pydesc_imag;
                case "is_integer": return pydesc_is_integer;
                case "numerator": return pydesc_numerator;
                case "real": return pydesc_real;
                case "to_bytes": return pydesc_to_bytes;
                default: return null;
            }
        }
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
        protected static final class PyIntClassMethod_from_bytes extends PyBuiltinMethod<PyType> {
            PyIntClassMethod_from_bytes(PyType self) { super(self); }
            @Override public String methodName() { return "from_bytes"; }
            @Override public PyObject call(PyObject[] args, PyDict kwargs) {
                throw new UnsupportedOperationException("int.from_bytes() unimplemented");
            }
        }
    }

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
        public static final pyclass_list singleton = new pyclass_list();
        private static final PyMethodDescriptor pydesc_append = new PyMethodDescriptor(singleton, "append", PyList.PyListMethod_append::new);
        private static final PyMethodDescriptor pydesc_clear = new PyMethodDescriptor(singleton, "clear", PyList.PyListMethod_clear::new);
        private static final PyMethodDescriptor pydesc_copy = new PyMethodDescriptor(singleton, "copy", PyList.PyListMethod_copy::new);
        private static final PyMethodDescriptor pydesc_count = new PyMethodDescriptor(singleton, "count", PyList.PyListMethod_count::new);
        private static final PyMethodDescriptor pydesc_extend = new PyMethodDescriptor(singleton, "extend", PyList.PyListMethod_extend::new);
        private static final PyMethodDescriptor pydesc_index = new PyMethodDescriptor(singleton, "index", PyList.PyListMethod_index::new);
        private static final PyMethodDescriptor pydesc_insert = new PyMethodDescriptor(singleton, "insert", PyList.PyListMethod_insert::new);
        private static final PyMethodDescriptor pydesc_pop = new PyMethodDescriptor(singleton, "pop", PyList.PyListMethod_pop::new);
        private static final PyMethodDescriptor pydesc_remove = new PyMethodDescriptor(singleton, "remove", PyList.PyListMethod_remove::new);
        private static final PyMethodDescriptor pydesc_reverse = new PyMethodDescriptor(singleton, "reverse", PyList.PyListMethod_reverse::new);
        private static final PyMethodDescriptor pydesc_sort = new PyMethodDescriptor(singleton, "sort", PyList.PyListMethod_sort::new);

        pyclass_list() { super("list", PyList.class); }
        @Override public PyDescriptor getDescriptor(String name) {
            switch (name) {
                case "append": return pydesc_append;
                case "clear": return pydesc_clear;
                case "copy": return pydesc_copy;
                case "count": return pydesc_count;
                case "extend": return pydesc_extend;
                case "index": return pydesc_index;
                case "insert": return pydesc_insert;
                case "pop": return pydesc_pop;
                case "remove": return pydesc_remove;
                case "reverse": return pydesc_reverse;
                case "sort": return pydesc_sort;
                default: return null;
            }
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
        public static final pyclass_object singleton = new pyclass_object();
        pyclass_object() { super("object", PyObject.class); }
    }

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
        public static final pyclass_range singleton = new pyclass_range();
        private static final PyMethodDescriptor pydesc_count = new PyMethodDescriptor(singleton, "count", PyRange.PyRangeMethod_count::new);
        private static final PyMethodDescriptor pydesc_index = new PyMethodDescriptor(singleton, "index", PyRange.PyRangeMethod_index::new);
        private static final PyMemberDescriptor pydesc_start = new PyMemberDescriptor(singleton, "start", obj -> new PyInt(((PyRange)obj).start));
        private static final PyMemberDescriptor pydesc_step = new PyMemberDescriptor(singleton, "step", obj -> new PyInt(((PyRange)obj).step));
        private static final PyMemberDescriptor pydesc_stop = new PyMemberDescriptor(singleton, "stop", obj -> new PyInt(((PyRange)obj).stop));

        pyclass_range() { super("range", PyRange.class); }
        @Override public PyDescriptor getDescriptor(String name) {
            switch (name) {
                case "count": return pydesc_count;
                case "index": return pydesc_index;
                case "start": return pydesc_start;
                case "step": return pydesc_step;
                case "stop": return pydesc_stop;
                default: return null;
            }
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

    static final class pyfunc_repr extends PyBuiltinFunction {
        pyfunc_repr() { super("repr"); }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            return new PyString(arg.repr());
        }
    }
    public static final pyfunc_repr pyglobal_repr = new pyfunc_repr();

    static final class pyclass_reversed extends PyBuiltinClass {
        public static final pyclass_reversed singleton = new pyclass_reversed();

        pyclass_reversed() { super("reversed", PyReversed.class); }
        @Override public PyIter call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            requireExactArgs(args, 1, typeName);
            return args[0].reversed();
        }
    }

    static final class pyclass_set extends PyBuiltinClass {
        public static final pyclass_set singleton = new pyclass_set();
        private static final PyMethodDescriptor pydesc_add = new PyMethodDescriptor(singleton, "add", PySet.PySetMethod_add::new);
        private static final PyMethodDescriptor pydesc_clear = new PyMethodDescriptor(singleton, "clear", PySet.PySetMethod_clear::new);
        private static final PyMethodDescriptor pydesc_copy = new PyMethodDescriptor(singleton, "copy", obj -> new PySet.PySetMethodUnimplemented(obj, "copy"));
        private static final PyMethodDescriptor pydesc_difference = new PyMethodDescriptor(singleton, "difference", obj -> new PySet.PySetMethodUnimplemented(obj, "difference"));
        private static final PyMethodDescriptor pydesc_difference_update = new PyMethodDescriptor(singleton, "difference_update", obj -> new PySet.PySetMethodUnimplemented(obj, "difference_update"));
        private static final PyMethodDescriptor pydesc_discard = new PyMethodDescriptor(singleton, "discard", PySet.PySetMethod_discard::new);
        private static final PyMethodDescriptor pydesc_intersection = new PyMethodDescriptor(singleton, "intersection", obj -> new PySet.PySetMethodUnimplemented(obj, "intersection"));
        private static final PyMethodDescriptor pydesc_intersection_update = new PyMethodDescriptor(singleton, "intersection_update", obj -> new PySet.PySetMethodUnimplemented(obj, "intersection_update"));
        private static final PyMethodDescriptor pydesc_isdisjoint = new PyMethodDescriptor(singleton, "isdisjoint", obj -> new PySet.PySetMethodUnimplemented(obj, "isdisjoint"));
        private static final PyMethodDescriptor pydesc_issubset = new PyMethodDescriptor(singleton, "issubset", obj -> new PySet.PySetMethodUnimplemented(obj, "issubset"));
        private static final PyMethodDescriptor pydesc_issuperset = new PyMethodDescriptor(singleton, "issuperset", obj -> new PySet.PySetMethodUnimplemented(obj, "issuperset"));
        private static final PyMethodDescriptor pydesc_pop = new PyMethodDescriptor(singleton, "pop", obj -> new PySet.PySetMethodUnimplemented(obj, "pop"));
        private static final PyMethodDescriptor pydesc_remove = new PyMethodDescriptor(singleton, "remove", obj -> new PySet.PySetMethodUnimplemented(obj, "remove"));
        private static final PyMethodDescriptor pydesc_symmetric_difference = new PyMethodDescriptor(singleton, "symmetric_difference", obj -> new PySet.PySetMethodUnimplemented(obj, "symmetric_difference"));
        private static final PyMethodDescriptor pydesc_symmetric_difference_update = new PyMethodDescriptor(singleton, "symmetric_difference_update", obj -> new PySet.PySetMethodUnimplemented(obj, "symmetric_difference_update"));
        private static final PyMethodDescriptor pydesc_union = new PyMethodDescriptor(singleton, "union", obj -> new PySet.PySetMethodUnimplemented(obj, "union"));
        private static final PyMethodDescriptor pydesc_update = new PyMethodDescriptor(singleton, "update", PySet.PySetMethod_update::new);

        pyclass_set() { super("set", PySet.class); }
        @Override public PyDescriptor getDescriptor(String name) {
            switch (name) {
                case "add": return pydesc_add;
                case "clear": return pydesc_clear;
                case "copy": return pydesc_copy;
                case "difference": return pydesc_difference;
                case "difference_update": return pydesc_difference_update;
                case "discard": return pydesc_discard;
                case "intersection": return pydesc_intersection;
                case "intersection_update": return pydesc_intersection_update;
                case "isdisjoint": return pydesc_isdisjoint;
                case "issubset": return pydesc_issubset;
                case "issuperset": return pydesc_issuperset;
                case "pop": return pydesc_pop;
                case "remove": return pydesc_remove;
                case "symmetric_difference": return pydesc_symmetric_difference;
                case "symmetric_difference_update": return pydesc_symmetric_difference_update;
                case "union": return pydesc_union;
                case "update": return pydesc_update;
                default: return null;
            }
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
        public static final pyclass_slice singleton = new pyclass_slice();
        private static final PyMethodDescriptor pydesc_indices = new PyMethodDescriptor(singleton, "indices", PySlice.PySliceMethod_indices::new);
        private static final PyMemberDescriptor pydesc_start = new PyMemberDescriptor(singleton, "start", obj -> ((PySlice)obj).start);
        private static final PyMemberDescriptor pydesc_step = new PyMemberDescriptor(singleton, "step", obj -> ((PySlice)obj).step);
        private static final PyMemberDescriptor pydesc_stop = new PyMemberDescriptor(singleton, "stop", obj -> ((PySlice)obj).stop);

        pyclass_slice() { super("slice", PySlice.class); }
        @Override public PyDescriptor getDescriptor(String name) {
            switch (name) {
                case "indices": return pydesc_indices;
                case "start": return pydesc_start;
                case "step": return pydesc_step;
                case "stop": return pydesc_stop;
                default: return null;
            }
        }
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
        public static final pyclass_str singleton = new pyclass_str();
        private static final PyMethodDescriptor pydesc_capitalize = new PyMethodDescriptor(singleton, "capitalize", obj -> new PyString.PyStringMethodUnimplemented(obj, "capitalize"));
        private static final PyMethodDescriptor pydesc_casefold = new PyMethodDescriptor(singleton, "casefold", obj -> new PyString.PyStringMethodUnimplemented(obj, "casefold"));
        private static final PyMethodDescriptor pydesc_center = new PyMethodDescriptor(singleton, "center", obj -> new PyString.PyStringMethodUnimplemented(obj, "center"));
        private static final PyMethodDescriptor pydesc_count = new PyMethodDescriptor(singleton, "count", obj -> new PyString.PyStringMethodUnimplemented(obj, "count"));
        private static final PyMethodDescriptor pydesc_encode = new PyMethodDescriptor(singleton, "encode", obj -> new PyString.PyStringMethodUnimplemented(obj, "encode"));
        private static final PyMethodDescriptor pydesc_endswith = new PyMethodDescriptor(singleton, "endswith", obj -> new PyString.PyStringMethodUnimplemented(obj, "endswith"));
        private static final PyMethodDescriptor pydesc_expandtabs = new PyMethodDescriptor(singleton, "expandtabs", obj -> new PyString.PyStringMethodUnimplemented(obj, "expandtabs"));
        private static final PyMethodDescriptor pydesc_find = new PyMethodDescriptor(singleton, "find", PyString.PyStringMethod_find::new);
        private static final PyMethodDescriptor pydesc_format = new PyMethodDescriptor(singleton, "format", obj -> new PyString.PyStringMethodUnimplemented(obj, "format"));
        private static final PyMethodDescriptor pydesc_format_map = new PyMethodDescriptor(singleton, "format_map", obj -> new PyString.PyStringMethodUnimplemented(obj, "format_map"));
        private static final PyMethodDescriptor pydesc_index = new PyMethodDescriptor(singleton, "index", obj -> new PyString.PyStringMethodUnimplemented(obj, "index"));
        private static final PyMethodDescriptor pydesc_isalnum = new PyMethodDescriptor(singleton, "isalnum", obj -> new PyString.PyStringMethodUnimplemented(obj, "isalnum"));
        private static final PyMethodDescriptor pydesc_isalpha = new PyMethodDescriptor(singleton, "isalpha", obj -> new PyString.PyStringMethodUnimplemented(obj, "isalpha"));
        private static final PyMethodDescriptor pydesc_isascii = new PyMethodDescriptor(singleton, "isascii", obj -> new PyString.PyStringMethodUnimplemented(obj, "isascii"));
        private static final PyMethodDescriptor pydesc_isdecimal = new PyMethodDescriptor(singleton, "isdecimal", obj -> new PyString.PyStringMethodUnimplemented(obj, "isdecimal"));
        private static final PyMethodDescriptor pydesc_isdigit = new PyMethodDescriptor(singleton, "isdigit", obj -> new PyString.PyStringMethodUnimplemented(obj, "isdigit"));
        private static final PyMethodDescriptor pydesc_isidentifier = new PyMethodDescriptor(singleton, "isidentifier", obj -> new PyString.PyStringMethodUnimplemented(obj, "isidentifier"));
        private static final PyMethodDescriptor pydesc_islower = new PyMethodDescriptor(singleton, "islower", obj -> new PyString.PyStringMethodUnimplemented(obj, "islower"));
        private static final PyMethodDescriptor pydesc_isnumeric = new PyMethodDescriptor(singleton, "isnumeric", obj -> new PyString.PyStringMethodUnimplemented(obj, "isnumeric"));
        private static final PyMethodDescriptor pydesc_isprintable = new PyMethodDescriptor(singleton, "isprintable", obj -> new PyString.PyStringMethodUnimplemented(obj, "isprintable"));
        private static final PyMethodDescriptor pydesc_isspace = new PyMethodDescriptor(singleton, "isspace", obj -> new PyString.PyStringMethodUnimplemented(obj, "isspace"));
        private static final PyMethodDescriptor pydesc_istitle = new PyMethodDescriptor(singleton, "istitle", obj -> new PyString.PyStringMethodUnimplemented(obj, "istitle"));
        private static final PyMethodDescriptor pydesc_isupper = new PyMethodDescriptor(singleton, "isupper", obj -> new PyString.PyStringMethodUnimplemented(obj, "isupper"));
        private static final PyMethodDescriptor pydesc_join = new PyMethodDescriptor(singleton, "join", PyString.PyStringMethod_join::new);
        private static final PyMethodDescriptor pydesc_ljust = new PyMethodDescriptor(singleton, "ljust", obj -> new PyString.PyStringMethodUnimplemented(obj, "ljust"));
        private static final PyMethodDescriptor pydesc_lower = new PyMethodDescriptor(singleton, "lower", PyString.PyStringMethod_lower::new);
        private static final PyMethodDescriptor pydesc_lstrip = new PyMethodDescriptor(singleton, "lstrip", obj -> new PyString.PyStringMethodUnimplemented(obj, "lstrip"));
        private static final PyStaticMethod pydesc_maketrans = new PyStaticMethod(singleton, "maketrans", new pyclass_str.PyStringStaticMethod_maketrans(singleton));
        private static final PyMethodDescriptor pydesc_partition = new PyMethodDescriptor(singleton, "partition", obj -> new PyString.PyStringMethodUnimplemented(obj, "partition"));
        private static final PyMethodDescriptor pydesc_removeprefix = new PyMethodDescriptor(singleton, "removeprefix", obj -> new PyString.PyStringMethodUnimplemented(obj, "removeprefix"));
        private static final PyMethodDescriptor pydesc_removesuffix = new PyMethodDescriptor(singleton, "removesuffix", obj -> new PyString.PyStringMethodUnimplemented(obj, "removesuffix"));
        private static final PyMethodDescriptor pydesc_replace = new PyMethodDescriptor(singleton, "replace", obj -> new PyString.PyStringMethodUnimplemented(obj, "replace"));
        private static final PyMethodDescriptor pydesc_rfind = new PyMethodDescriptor(singleton, "rfind", obj -> new PyString.PyStringMethodUnimplemented(obj, "rfind"));
        private static final PyMethodDescriptor pydesc_rindex = new PyMethodDescriptor(singleton, "rindex", obj -> new PyString.PyStringMethodUnimplemented(obj, "rindex"));
        private static final PyMethodDescriptor pydesc_rjust = new PyMethodDescriptor(singleton, "rjust", obj -> new PyString.PyStringMethodUnimplemented(obj, "rjust"));
        private static final PyMethodDescriptor pydesc_rpartition = new PyMethodDescriptor(singleton, "rpartition", obj -> new PyString.PyStringMethodUnimplemented(obj, "rpartition"));
        private static final PyMethodDescriptor pydesc_rsplit = new PyMethodDescriptor(singleton, "rsplit", obj -> new PyString.PyStringMethodUnimplemented(obj, "rsplit"));
        private static final PyMethodDescriptor pydesc_rstrip = new PyMethodDescriptor(singleton, "rstrip", obj -> new PyString.PyStringMethodUnimplemented(obj, "rstrip"));
        private static final PyMethodDescriptor pydesc_split = new PyMethodDescriptor(singleton, "split", PyString.PyStringMethod_split::new);
        private static final PyMethodDescriptor pydesc_splitlines = new PyMethodDescriptor(singleton, "splitlines", obj -> new PyString.PyStringMethodUnimplemented(obj, "splitlines"));
        private static final PyMethodDescriptor pydesc_startswith = new PyMethodDescriptor(singleton, "startswith", PyString.PyStringMethod_startswith::new);
        private static final PyMethodDescriptor pydesc_strip = new PyMethodDescriptor(singleton, "strip", obj -> new PyString.PyStringMethodUnimplemented(obj, "strip"));
        private static final PyMethodDescriptor pydesc_swapcase = new PyMethodDescriptor(singleton, "swapcase", obj -> new PyString.PyStringMethodUnimplemented(obj, "swapcase"));
        private static final PyMethodDescriptor pydesc_title = new PyMethodDescriptor(singleton, "title", obj -> new PyString.PyStringMethodUnimplemented(obj, "title"));
        private static final PyMethodDescriptor pydesc_translate = new PyMethodDescriptor(singleton, "translate", obj -> new PyString.PyStringMethodUnimplemented(obj, "translate"));
        private static final PyMethodDescriptor pydesc_upper = new PyMethodDescriptor(singleton, "upper", PyString.PyStringMethod_upper::new);
        private static final PyMethodDescriptor pydesc_zfill = new PyMethodDescriptor(singleton, "zfill", obj -> new PyString.PyStringMethodUnimplemented(obj, "zfill"));

        pyclass_str() { super("str", PyString.class); }
        @Override public PyDescriptor getDescriptor(String name) {
            switch (name) {
                case "capitalize": return pydesc_capitalize;
                case "casefold": return pydesc_casefold;
                case "center": return pydesc_center;
                case "count": return pydesc_count;
                case "encode": return pydesc_encode;
                case "endswith": return pydesc_endswith;
                case "expandtabs": return pydesc_expandtabs;
                case "find": return pydesc_find;
                case "format": return pydesc_format;
                case "format_map": return pydesc_format_map;
                case "index": return pydesc_index;
                case "isalnum": return pydesc_isalnum;
                case "isalpha": return pydesc_isalpha;
                case "isascii": return pydesc_isascii;
                case "isdecimal": return pydesc_isdecimal;
                case "isdigit": return pydesc_isdigit;
                case "isidentifier": return pydesc_isidentifier;
                case "islower": return pydesc_islower;
                case "isnumeric": return pydesc_isnumeric;
                case "isprintable": return pydesc_isprintable;
                case "isspace": return pydesc_isspace;
                case "istitle": return pydesc_istitle;
                case "isupper": return pydesc_isupper;
                case "join": return pydesc_join;
                case "ljust": return pydesc_ljust;
                case "lower": return pydesc_lower;
                case "lstrip": return pydesc_lstrip;
                case "maketrans": return pydesc_maketrans;
                case "partition": return pydesc_partition;
                case "removeprefix": return pydesc_removeprefix;
                case "removesuffix": return pydesc_removesuffix;
                case "replace": return pydesc_replace;
                case "rfind": return pydesc_rfind;
                case "rindex": return pydesc_rindex;
                case "rjust": return pydesc_rjust;
                case "rpartition": return pydesc_rpartition;
                case "rsplit": return pydesc_rsplit;
                case "rstrip": return pydesc_rstrip;
                case "split": return pydesc_split;
                case "splitlines": return pydesc_splitlines;
                case "startswith": return pydesc_startswith;
                case "strip": return pydesc_strip;
                case "swapcase": return pydesc_swapcase;
                case "title": return pydesc_title;
                case "translate": return pydesc_translate;
                case "upper": return pydesc_upper;
                case "zfill": return pydesc_zfill;
                default: return null;
            }
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

        static final class PyStringStaticMethod_maketrans extends PyBuiltinMethod<PyType> {
            PyStringStaticMethod_maketrans(PyType _self) { super(_self); }
            @Override public String methodName() { return "maketrans"; }
            @Override public PyObject call(PyObject[] args, PyDict kwargs) {
                throw new UnsupportedOperationException("str.maketrans unimplemented");
            }
        }
    }

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
        public static final pyclass_tuple singleton = new pyclass_tuple();
        private static final PyMethodDescriptor pydesc_count = new PyMethodDescriptor(singleton, "count", PyTuple.PyTupleMethod_count::new);
        private static final PyMethodDescriptor pydesc_index = new PyMethodDescriptor(singleton, "index", PyTuple.PyTupleMethod_index::new);

        pyclass_tuple() { super("tuple", PyTuple.class); }
        @Override public PyDescriptor getDescriptor(String name) {
            switch (name) {
                case "count": return pydesc_count;
                case "index": return pydesc_index;
                default: return null;
            }
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

    static final class pyclass_type extends PyBuiltinClass {
        public static final pyclass_type singleton = new pyclass_type();

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

    static final class pyclass_zip extends PyBuiltinClass {
        public static final pyclass_zip singleton = new pyclass_zip();

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

    static final class pyclass_ArithmeticError extends PyBuiltinClass {
        public static final pyclass_ArithmeticError singleton = new pyclass_ArithmeticError();
        pyclass_ArithmeticError() { super("ArithmeticError", PyArithmeticError.class); }
        @Override public PyArithmeticError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyArithmeticError(args);
        }
    }

    static final class pyclass_AssertionError extends PyBuiltinClass {
        public static final pyclass_AssertionError singleton = new pyclass_AssertionError();
        pyclass_AssertionError() { super("AssertionError", PyAssertionError.class); }
        @Override public PyAssertionError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyAssertionError(args);
        }
    }

    static final class pyclass_AttributeError extends PyBuiltinClass {
        public static final pyclass_AttributeError singleton = new pyclass_AttributeError();
        pyclass_AttributeError() { super("AttributeError", PyAttributeError.class); }
        @Override public PyAttributeError call(PyObject[] args, PyDict kwargs) {
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("AttributeError() does not accept kwargs");
            }
            return new PyAttributeError(args);
        }
    }

    static final class pyclass_BaseException extends PyBuiltinClass {
        public static final pyclass_BaseException singleton = new pyclass_BaseException();
        pyclass_BaseException() { super("BaseException", PyBaseException.class); }
        @Override public PyBaseException call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyBaseException(args);
        }
    }

    static final class pyclass_Exception extends PyBuiltinClass {
        public static final pyclass_Exception singleton = new pyclass_Exception();
        pyclass_Exception() { super("Exception", PyException.class); }
        @Override public PyException call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyException(args);
        }
    }

    static final class pyclass_IndexError extends PyBuiltinClass {
        public static final pyclass_IndexError singleton = new pyclass_IndexError();
        pyclass_IndexError() { super("IndexError", PyIndexError.class); }
        @Override public PyIndexError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyIndexError(args);
        }
    }

    static final class pyclass_KeyError extends PyBuiltinClass {
        public static final pyclass_KeyError singleton = new pyclass_KeyError();
        pyclass_KeyError() { super("KeyError", PyKeyError.class); }
        @Override public PyKeyError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyKeyError(args);
        }
    }

    static final class pyclass_LookupError extends PyBuiltinClass {
        public static final pyclass_LookupError singleton = new pyclass_LookupError();
        pyclass_LookupError() { super("LookupError", PyLookupError.class); }
        @Override public PyLookupError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyLookupError(args);
        }
    }

    static final class pyclass_StopIteration extends PyBuiltinClass {
        public static final pyclass_StopIteration singleton = new pyclass_StopIteration();
        pyclass_StopIteration() { super("StopIteration", PyStopIteration.class); }
        @Override public PyStopIteration call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyStopIteration(args);
        }
    }

    static final class pyclass_TypeError extends PyBuiltinClass {
        public static final pyclass_TypeError singleton = new pyclass_TypeError();
        pyclass_TypeError() { super("TypeError", PyTypeError.class); }
        @Override public PyTypeError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyTypeError(args);
        }
    }

    static final class pyclass_ValueError extends PyBuiltinClass {
        public static final pyclass_ValueError singleton = new pyclass_ValueError();
        pyclass_ValueError() { super("ValueError", PyValueError.class); }
        @Override public PyValueError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyValueError(args);
        }
    }

    static final class pyclass_ZeroDivisionError extends PyBuiltinClass {
        public static final pyclass_ZeroDivisionError singleton = new pyclass_ZeroDivisionError();
        pyclass_ZeroDivisionError() { super("ZeroDivisionError", PyZeroDivisionError.class); }
        @Override public PyZeroDivisionError call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, typeName);
            return new PyZeroDivisionError(args);
        }
    }

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
    public static PyRaise raiseNamedReadOnlyAttr(PyType owner, String key) {
        return PyAttributeError.raise(PyString.reprOf(owner.name()) + " object attribute " + PyString.reprOf(key) + " is read-only");
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
