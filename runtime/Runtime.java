// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

@FunctionalInterface
interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}

abstract class PyTruthyObject extends PyObject {
    @Override public final boolean boolValue() { return true; }
}

abstract class PyIter extends PyTruthyObject {
    @Override public final boolean hasIter() { return true; }
    @Override public final PyIter iter() { return this; }
}

abstract class PyType extends PyTruthyObject {
    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        if (args.length != 1) {
            throw new IllegalArgumentException("type() takes 1 argument");
        }
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("type() does not accept kwargs");
        }
        return args[0].type();
    }

    @Override public PyObject or(PyObject rhs) {
        if ((rhs instanceof PyType) || (rhs instanceof PyNone)) {
            throw new UnsupportedOperationException("type unions are unsupported");
        } else {
            return super.or(rhs);
        }
    }

    public Map<PyObject, PyObject> getAttributes() { return null; }
    public PyObject lookupAttr(String name) { return null; }

    @Override public boolean contains(PyObject rhs) { return defaultContains(rhs); }

    public abstract String name();

    static PyObject pygetset___doc__(PyObject obj) {
        throw new UnsupportedOperationException("type.__doc__ unimplemented");
    }
}

class PyBuiltinType extends PyType {
    protected final String typeName;
    protected final Class<? extends PyObject> instanceClass;
    protected final TriFunction<PyBuiltinType, PyObject[], PyDict, PyObject> newObj;

    protected PyBuiltinType(String name, Class<? extends PyObject> _instanceClass) {
        typeName = name;
        instanceClass = _instanceClass;
        newObj = null;
    }
    protected PyBuiltinType(String name, Class<? extends PyObject> _instanceClass,
                            TriFunction<PyBuiltinType, PyObject[], PyDict, PyObject> _newObj) {
        typeName = name;
        instanceClass = _instanceClass;
        newObj = _newObj;
    }
    @Override public final PyObject getAttr(String key) {
        var desc = lookupAttr(key);
        if (desc != null) {
            return desc.get(null);
        }
        switch (key) {
            case "__dict__": {
                var attrs = getAttributes();
                if (attrs == null) {
                    throw new UnsupportedOperationException(name() + ".__dict__ is not implemented");
                }
                PyDict ret = new PyDict(); // XXX This should return a mappingproxy singleton, not a PyDict snapshot
                ret.items.putAll(attrs);
                return ret;
            }
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

    @Override public PyObject call(PyObject[] args, PyDict kwargs) {
        if (newObj != null) {
            return newObj.apply(this, args, kwargs);
        }
        return super.call(args, kwargs);
    }

    @Override public final String repr() { return "<class '" + typeName + "'>"; }
    @Override public final PyTypeType type() { return PyTypeType.singleton; }
    @Override public final String name() { return typeName; }
}

abstract class PyGettableDescriptor extends PyTruthyObject {
    protected final PyType owner;
    protected final String name;
    protected final Function<PyObject, PyObject> getter;
    protected final String doc;

    protected PyGettableDescriptor(PyType _owner, String _name, Function<PyObject, PyObject> _getter, String _doc) {
        owner = _owner;
        name = _name;
        getter = _getter;
        doc = _doc;
    }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        throw PyTypeError.raise("cannot create " + PyString.reprOf(type.name()) + " instances");
    }

    @Override public final PyObject get(PyObject instance) {
        if (instance == null) {
            return this;
        } else {
            return getter.apply(instance);
        }
    }

    static PyObject pygetset___doc__(PyObject obj) {
        String doc = ((PyGettableDescriptor)obj).doc;
        return (doc != null) ? new PyString(doc) : PyNone.singleton;
    }
}

final class PyMemberDescriptor extends PyGettableDescriptor {
    protected PyMemberDescriptor(PyType _owner, String _name, Function<PyObject, PyObject> _getter, String _doc) {
        super(_owner, _name, _getter, _doc);
    }

    @Override public final boolean isDataDescriptor() { return true; }
    @Override public final void set(PyObject instance, PyObject value) {
        throw PyAttributeError.raise("readonly attribute");
    }
    @Override public final void delete(PyObject instance) {
        throw PyAttributeError.raise("readonly attribute");
    }

    @Override public final String repr() { return "<member " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects>"; }
    @Override public final PyBuiltinType type() { return PyMemberDescriptorType.singleton; }
}

final class PyGetSetDescriptor extends PyGettableDescriptor {
    protected PyGetSetDescriptor(PyType _owner, String _name, Function<PyObject, PyObject> _getter, String _doc) {
        super(_owner, _name, _getter, _doc);
    }

    @Override public final boolean isDataDescriptor() { return true; }
    @Override public final void set(PyObject instance, PyObject value) {
        throw PyAttributeError.raise("attribute " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects is not writable");
    }
    @Override public final void delete(PyObject instance) {
        throw PyAttributeError.raise("attribute " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects is not writable");
    }

    @Override public final String repr() { return "<attribute " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects>"; }
    @Override public final PyBuiltinType type() { return PyGetSetDescriptorType.singleton; }
}

final class PyMethodDescriptor extends PyGettableDescriptor {
    protected PyMethodDescriptor(PyType _owner, String _name, Function<PyObject, PyObject> _getter, String _doc) {
        super(_owner, _name, _getter, _doc);
    }

    @Override public final String repr() { return "<method " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects>"; }
    @Override public final PyBuiltinType type() { return PyMethodDescriptorType.singleton; }
}

final class PyClassMethodDescriptor extends PyTruthyObject {
    private final PyType owner;
    private final String name;
    private final Function<PyType, PyObject> getter;
    private final String doc;

    protected PyClassMethodDescriptor(PyType _owner, String _name, Function<PyType, PyObject> _getter, String _doc) {
        owner = _owner;
        name = _name;
        getter = _getter;
        doc = _doc;
    }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        throw PyTypeError.raise("cannot create " + PyString.reprOf(type.name()) + " instances");
    }

    @Override public final PyObject get(PyObject instance) {
        return getter.apply(owner);
    }

    @Override public final String repr() { return "<method " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects>"; }
    @Override public final PyBuiltinType type() { return PyClassMethodDescriptorType.singleton; }

    static PyObject pygetset___doc__(PyObject obj) {
        String doc = ((PyClassMethodDescriptor)obj).doc;
        return (doc != null) ? new PyString(doc) : PyNone.singleton;
    }
}

final class PyStaticMethod extends PyTruthyObject {
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

    @Override public final String repr() { return "<staticmethod(" + func.repr() + ")>"; }
    @Override public final PyBuiltinType type() { return PyStaticMethodType.singleton; }
}

abstract class PyBuiltinFunctionOrMethod extends PyTruthyObject {
    @Override public final PyBuiltinType type() { return PyBuiltinFunctionOrMethodType.singleton; }

    static PyObject pygetset___doc__(PyObject obj) {
        throw new UnsupportedOperationException("builtin_function_or_method.__doc__ unimplemented");
    }
}

abstract class PyBuiltinMethod<T extends PyObject> extends PyBuiltinFunctionOrMethod {
    protected final T self;
    PyBuiltinMethod(T _self) { self = _self; }
    @Override public String repr() {
        return "<built-in method " + methodName() + " of " + self.type().name() + " object>";
    }
    public abstract String methodName();
}

abstract class PyFunction extends PyTruthyObject {
    private final String funcName;
    protected PyFunction(String name) { funcName = name; }
    @Override public PyBuiltinType type() { return PyFunctionType.singleton; }
    @Override public String repr() { return "<function " + funcName + ">"; }

    static PyObject pymember___doc__(PyObject obj) {
        throw new UnsupportedOperationException("function.__doc__ unimplemented");
    }
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
