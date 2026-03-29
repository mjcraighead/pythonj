// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Arrays;
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
    @Override public int hashCode() { return defaultHashCode(); }
    @Override public final PyIter iter() { return this; }
}

final class PyCell {
    PyObject obj;

    PyCell(PyObject _obj) {
        obj = _obj;
    }
}

abstract class PyBagObject extends PyTruthyObject {
    private final PyType bagType;
    private final PyDict attrs = new PyDict();

    protected PyBagObject(PyType _bagType) {
        bagType = _bagType;
    }

    @Override public PyObject getAttr(String key) {
        PyObject value = attrs.items.get(new PyString(key));
        if (value != null) {
            return value;
        }
        if (key.equals("__dict__")) {
            return attrs;
        }
        return super.getAttr(key);
    }
    @Override public void setAttr(String key, PyObject value) {
        if (key.equals("__class__") || key.equals("__dict__")) {
            throw Runtime.raiseNamedReadOnlyAttr(type(), key);
        }
        attrs.items.put(new PyString(key), value);
    }
    @Override public void delAttr(String key) {
        if (key.equals("__class__") || key.equals("__dict__")) {
            throw Runtime.raiseNamedReadOnlyAttr(type(), key);
        }
        if (attrs.items.remove(new PyString(key)) == null) {
            throw raiseMissingAttr(key);
        }
    }

    @Override public boolean equals(Object rhs) { return this == rhs; }
    @Override public int hashCode() { return defaultHashCode(); }
    @Override public String repr() { return defaultRepr(); }
    @Override public PyType type() { return bagType; }
}

abstract class PySlottedObject extends PyTruthyObject {
    private final PyType slotType;

    protected PySlottedObject(PyType _slotType) {
        slotType = _slotType;
    }

    @Override public boolean equals(Object rhs) { return this == rhs; }
    @Override public int hashCode() { return defaultHashCode(); }
    @Override public String repr() { return defaultRepr(); }
    @Override public PyType type() { return slotType; }
}

abstract class PyType extends PyTruthyObject {
    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
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
    @Override public int hashCode() { return defaultHashCode(); }

    public abstract String name();

    static PyObject pygetset___doc__(PyObject obj) {
        throw new UnsupportedOperationException("type.__doc__ unimplemented");
    }
}

class PyConcreteType extends PyType {
    protected final String typeName;
    protected final Class<? extends PyObject> instanceClass;
    protected final TriFunction<PyConcreteType, PyObject[], PyDict, PyObject> newObj;

    protected PyConcreteType(String name, Class<? extends PyObject> _instanceClass) {
        typeName = name;
        instanceClass = _instanceClass;
        newObj = null;
    }
    protected PyConcreteType(String name, Class<? extends PyObject> _instanceClass,
                            TriFunction<PyConcreteType, PyObject[], PyDict, PyObject> _newObj) {
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

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
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
    @Override public final PyConcreteType type() { return PyMemberDescriptorType.singleton; }
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
    @Override public final PyConcreteType type() { return PyGetSetDescriptorType.singleton; }
}

final class PyMethodDescriptor extends PyGettableDescriptor {
    protected PyMethodDescriptor(PyType _owner, String _name, Function<PyObject, PyObject> _getter, String _doc) {
        super(_owner, _name, _getter, _doc);
    }

    @Override public final String repr() { return "<method " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects>"; }
    @Override public final PyConcreteType type() { return PyMethodDescriptorType.singleton; }
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

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        throw PyTypeError.raise("cannot create " + PyString.reprOf(type.name()) + " instances");
    }

    @Override public final PyObject get(PyObject instance) {
        return getter.apply(owner);
    }

    @Override public final String repr() { return "<method " + PyString.reprOf(name) + " of " + PyString.reprOf(owner.name()) + " objects>"; }
    @Override public final PyConcreteType type() { return PyClassMethodDescriptorType.singleton; }

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
    @Override public final PyConcreteType type() { return PyStaticMethodType.singleton; }
}

abstract class PyBuiltinFunctionOrMethod extends PyTruthyObject {
    @Override public int hashCode() { return defaultHashCode(); }
    @Override public final PyConcreteType type() { return PyBuiltinFunctionOrMethodType.singleton; }

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
    @Override public int hashCode() { return defaultHashCode(); }
    @Override public PyConcreteType type() { return PyFunctionType.singleton; }
    @Override public String repr() { return "<function " + funcName + ">"; }

    static PyObject pymember___doc__(PyObject obj) {
        throw new UnsupportedOperationException("function.__doc__ unimplemented");
    }
}

// Helper functions used by the builtins and code generator
public final class Runtime {
    public static int hashRational(long numerator, long denominator) {
        if (denominator <= 0) {
            throw new IllegalArgumentException("denominator must be positive");
        }
        return 31 * Long.hashCode(numerator) + Long.hashCode(denominator);
    }
    public static PyObject pythonjGetAttr(PyObject obj, PyObject nameObj) {
        return obj.getAttr(((PyString)nameObj).value);
    }
    public static PyNone pythonjSetAttr(PyObject obj, PyObject nameObj, PyObject value) {
        obj.setAttr(((PyString)nameObj).value, value);
        return PyNone.singleton;
    }
    public static PyNone pythonjDelAttr(PyObject obj, PyObject nameObj) {
        obj.delAttr(((PyString)nameObj).value);
        return PyNone.singleton;
    }
    public static PyString pythonjFormat(PyObject obj, PyObject formatSpecObj) {
        return new PyString(obj.format(((PyString)formatSpecObj).value));
    }
    public static PyInt pythonjLen(PyObject obj) {
        return new PyInt(obj.len());
    }
    public static PyInt pythonjHash(PyObject obj) {
        return new PyInt(obj.hashCode());
    }
    public static PyBool pythonjIsInstanceSingle(PyObject obj, PyObject type) {
        if (type instanceof PyConcreteType typeClass) {
            return PyBool.create(typeClass.instanceClass.isInstance(obj));
        } else if (type instanceof PyType) {
            throw new UnsupportedOperationException("isinstance() is unimplemented for type " + type.repr());
        } else {
            throw PyTypeError.raise("isinstance() arg 2 must be a type, a tuple of types, or a union");
        }
    }
    public static PyBool pythonjIsSubclassSingle(PyObject obj, PyObject type) {
        if (obj instanceof PyConcreteType objClass &&
            type instanceof PyConcreteType typeClass) {
            return PyBool.create(typeClass.instanceClass.isAssignableFrom(objClass.instanceClass));
        } else {
            throw new UnsupportedOperationException(String.format("issubclass() is unimplemented for types %s and %s", obj.repr(), type.repr()));
        }
    }
    public static PyString pythonjRepr(PyObject obj) {
        return new PyString(obj.repr());
    }
    public static PyRaise raiseNoKwArgs(String name) {
        return PyTypeError.raise(name + "() takes no keyword arguments");
    }
    public static PyRaise raiseExactArgs(PyObject[] args, int n, String name) {
        return PyTypeError.raiseFormat("%s expected %d argument%s, got %d", name, n, (n == 1) ? "" : "s", args.length);
    }
    public static PyRaise raiseNoArgs(PyObject[] args, String name) {
        return PyTypeError.raiseFormat("%s() takes no arguments (%d given)", name, args.length);
    }
    public static PyRaise raiseOneArg(PyObject[] args, String name) {
        return PyTypeError.raiseFormat("%s() takes exactly one argument (%d given)", name, args.length);
    }
    public static void requireNoKwArgs(PyDict kwargs, String name) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw raiseNoKwArgs(name);
        }
    }
    public static void requireExactArgs(PyObject[] args, int n, String name) {
        if (args.length != n) {
            throw raiseExactArgs(args, n, name);
        }
    }
    private static PyRaise raiseUserMissingArgsImpl(String name, Collection<String> missingArgNames) {
        int missing = missingArgNames.size();
        StringBuilder s = new StringBuilder(String.format("%s() missing %d required positional argument%s:", name, missing, (missing == 1) ? "" : "s"));
        int i = 0;
        for (var argName: missingArgNames) {
            i++;
            s.append(" '").append(argName).append("'");
            if ((missing >= 3) && (i != missing)) {
                s.append(",");
            }
            if (i == missing - 1) {
                s.append(" and");
            }
        }
        return PyTypeError.raise(s.toString());
    }
    public static PyRaise raiseUserExactArgs(PyObject[] args, int n, String name, String... argNames) {
        if (args.length > n) {
            return PyTypeError.raiseFormat("%s() takes %d positional argument%s but %d %s given",
                name, n, (n == 1) ? "" : "s", args.length, (args.length == 1) ? "was" : "were");
        } else {
            return raiseUserMissingArgsImpl(name, Arrays.asList(argNames).subList(args.length, n));
        }
    }
    public static PyRaise raiseUserMissingArgs(int nBound, String name, String... argNames) {
        return raiseUserMissingArgsImpl(name, Arrays.asList(argNames).subList(nBound, argNames.length));
    }
    public static PyRaise raiseUserMissingKwArgs(String name, PyObject[] boundArgs, String... argNames) {
        var missingArgNames = new ArrayList<String>();
        for (int i = 0; i < argNames.length; i++) {
            if (boundArgs[i] == null) {
                missingArgNames.add(argNames[i]);
            }
        }
        return raiseUserMissingArgsImpl(name, missingArgNames);
    }
    public static PyRaise raiseUserFromToArgs(PyObject[] args, int min, int max, String name) {
        return PyTypeError.raiseFormat("%s() takes from %d to %d positional arguments but %d %s given",
            name, min, max, args.length, (args.length == 1) ? "was" : "were");
    }
    public static PyRaise raiseAtMostArgs(String name, long max, long given) {
        return PyTypeError.raiseFormat("%s() takes at most %d arguments (%d given)", name, max, given);
    }
    public static PyRaise raiseAtMostPosArgs(String name, long max, long given) {
        return PyTypeError.raiseFormat("%s() takes at most %d positional arguments (%d given)", name, max, given);
    }
    public static PyRaise raiseAtMostKwArgs(String name, long max, long argsLength, long kwargsLen) {
        if (argsLength == 0) {
            return PyTypeError.raiseFormat("%s() takes at most %d keyword arguments (%d given)", name, max, kwargsLen);
        } else {
            return raiseAtMostArgs(name, max, argsLength + kwargsLen);
        }
    }
    public static PyRaise raiseMissingRequiredArg(String name, String argName, int position) {
        return PyTypeError.raiseFormat("%s() missing required argument %s (pos %d)", name, PyString.reprOf(argName), position);
    }
    public static PyRaise raiseArgGivenByNameAndPosition(String name, String argName, int position) {
        return PyTypeError.raiseFormat("argument for %s() given by name (%s) and position (%d)", name, PyString.reprOf(argName), position);
    }
    public static PyRaise raiseUnexpectedKwArg(String name, String kwName) {
        return PyTypeError.raiseFormat("%s() got an unexpected keyword argument %s", name, PyString.reprOf(kwName));
    }
    public static PyRaise raiseMultipleValues(String name, String argName) {
        return PyTypeError.raiseFormat("%s() got multiple values for argument %s", name, PyString.reprOf(argName));
    }
    public static PyRaise raiseExpr(PyObject exc) {
        if (exc instanceof PyBaseException baseExc) {
            return new PyRaise(baseExc);
        } else {
            throw PyTypeError.raise("exceptions must derive from BaseException");
        }
    }
    public static PyRaise raiseNamedReadOnlyAttr(PyType owner, String key) {
        return PyAttributeError.raise(PyString.reprOf(owner.name()) + " object attribute " + PyString.reprOf(key) + " is read-only");
    }
    public static PyRaise raiseMinArgs(PyObject[] args, int min, String name) {
        return PyTypeError.raiseFormat("%s expected at least %d argument%s, got %d", name, min, (min == 1) ? "" : "s", args.length);
    }
    public static PyRaise raiseMaxArgs(PyObject[] args, int max, String name) {
        return PyTypeError.raiseFormat("%s expected at most %d argument%s, got %d", name, max, (max == 1) ? "" : "s", args.length);
    }
    public static void requireMinArgs(PyObject[] args, int min, String name) {
        if (args.length < min) {
            throw raiseMinArgs(args, min, name);
        }
    }
    public static void requireMaxArgs(PyObject[] args, int max, String name) {
        if (args.length > max) {
            throw raiseMaxArgs(args, max, name);
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
    public static String getDefaultTextEncoding() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win") ? "cp1252" : "UTF-8"; // XXX extremely basic, good enough for now
    }
}
