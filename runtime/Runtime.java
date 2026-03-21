// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.ArrayList;
import java.util.Collection;
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

// Helper functions used by the builtins and code generator
public final class Runtime {
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
