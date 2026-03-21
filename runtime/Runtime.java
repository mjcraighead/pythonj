// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

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
    @Override public final PyTypeType type() { return PyTypeType.singleton; }
    @Override public final String name() { return typeName; }
}

final class PyTypeType extends PyBuiltinClass {
    public static final PyTypeType singleton = new PyTypeType();

    private PyTypeType() { super("type", PyType.class); }
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
        public static final pyfunc_abs singleton = new pyfunc_abs();

        private pyfunc_abs() { super("abs"); }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            return arg.abs();
        }
    }

    static final class pyfunc_all extends PyBuiltinFunction {
        public static final pyfunc_all singleton = new pyfunc_all();

        private pyfunc_all() { super("all"); }
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

    static final class pyfunc_any extends PyBuiltinFunction {
        public static final pyfunc_any singleton = new pyfunc_any();

        private pyfunc_any() { super("any"); }
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

    static final class pyfunc_ascii extends PyBuiltinFunction {
        public static final pyfunc_ascii singleton = new pyfunc_ascii();

        private pyfunc_ascii() { super("ascii"); }
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

    static final class pyfunc_chr extends PyBuiltinFunction {
        public static final pyfunc_chr singleton = new pyfunc_chr();

        private pyfunc_chr() { super("chr"); }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            long index = arg.indexValue();
            if ((index < 0) || (index > 65535)) {
                throw new IllegalArgumentException("chr() argument out of range");
            }
            return new PyString(String.valueOf((char)index));
        }
    }

    static final class pyfunc_delattr extends PyBuiltinFunction {
        public static final pyfunc_delattr singleton = new pyfunc_delattr();

        private pyfunc_delattr() { super("delattr"); }
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

    static final class pyfunc_format extends PyBuiltinFunction {
        public static final pyfunc_format singleton = new pyfunc_format();

        private pyfunc_format() { super("format"); }
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

    static final class pyfunc_getattr extends PyBuiltinFunction {
        public static final pyfunc_getattr singleton = new pyfunc_getattr();

        private pyfunc_getattr() { super("getattr"); }
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

    static final class pyfunc_hasattr extends PyBuiltinFunction {
        public static final pyfunc_hasattr singleton = new pyfunc_hasattr();

        private pyfunc_hasattr() { super("hasattr"); }
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

    static final class pyfunc_hash extends PyBuiltinFunction {
        public static final pyfunc_hash singleton = new pyfunc_hash();

        private pyfunc_hash() { super("hash"); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            return new PyInt(arg.hashCode());
        }
    }

    static final class pyfunc_hex extends PyBuiltinFunction {
        public static final pyfunc_hex singleton = new pyfunc_hex();

        private pyfunc_hex() { super("hex"); }
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

    static final class pyfunc_isinstance extends PyBuiltinFunction {
        public static final pyfunc_isinstance singleton = new pyfunc_isinstance();

        private pyfunc_isinstance() { super("isinstance"); }
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

    static final class pyfunc_issubclass extends PyBuiltinFunction {
        public static final pyfunc_issubclass singleton = new pyfunc_issubclass();

        private pyfunc_issubclass() { super("issubclass"); }
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

    static final class pyfunc_iter extends PyBuiltinFunction {
        public static final pyfunc_iter singleton = new pyfunc_iter();

        private pyfunc_iter() { super("iter"); }
        @Override public PyIter call(PyObject[] args, PyDict kwargs) {
            requireNoKwArgs(kwargs, funcName);
            if (args.length != 1) {
                throw new IllegalArgumentException("iter() takes 1 argument");
            }
            return args[0].iter();
        }
    }

    static final class pyfunc_len extends PyBuiltinFunction {
        public static final pyfunc_len singleton = new pyfunc_len();

        private pyfunc_len() { super("len"); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            return new PyInt(arg.len());
        }
    }

    static final class pyfunc_max extends PyBuiltinFunction {
        public static final pyfunc_max singleton = new pyfunc_max();

        private pyfunc_max() { super("max"); }
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

    static final class pyfunc_min extends PyBuiltinFunction {
        public static final pyfunc_min singleton = new pyfunc_min();

        private pyfunc_min() { super("min"); }
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

    static final class pyfunc_next extends PyBuiltinFunction {
        public static final pyfunc_next singleton = new pyfunc_next();

        private pyfunc_next() { super("next"); }
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

    static final class pyfunc_open extends PyBuiltinFunction {
        public static final pyfunc_open singleton = new pyfunc_open();

        private pyfunc_open() { super("open"); }
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

    static final class pyfunc_ord extends PyBuiltinFunction {
        public static final pyfunc_ord singleton = new pyfunc_ord();

        private pyfunc_ord() { super("ord"); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            PyString arg = (PyString)exactlyOneArg(args, kwargs);
            if (arg.len() != 1) {
                throw new IllegalArgumentException("argument to ord() must be string of length 1");
            }
            return new PyInt(arg.value.charAt(0));
        }
    }

    static final class pyfunc_print extends PyBuiltinFunction {
        public static final pyfunc_print singleton = new pyfunc_print();

        private pyfunc_print() { super("print"); }
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

    static final class pyfunc_repr extends PyBuiltinFunction {
        public static final pyfunc_repr singleton = new pyfunc_repr();

        private pyfunc_repr() { super("repr"); }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            var arg = exactlyOneArg(args, kwargs);
            return new PyString(arg.repr());
        }
    }

    static final class pyfunc_setattr extends PyBuiltinFunction {
        public static final pyfunc_setattr singleton = new pyfunc_setattr();

        private pyfunc_setattr() { super("setattr"); }
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

    static final class pyfunc_sorted extends PyBuiltinFunction {
        public static final pyfunc_sorted singleton = new pyfunc_sorted();

        private pyfunc_sorted() { super("sorted"); }
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

    static final class pyfunc_sum extends PyBuiltinFunction {
        public static final pyfunc_sum singleton = new pyfunc_sum();

        private pyfunc_sum() { super("sum"); }
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
