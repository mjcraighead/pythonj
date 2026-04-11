// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

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

final class PyStringBuilder extends PyTruthyObject {
    final StringBuilder value;

    PyStringBuilder() {
        value = new StringBuilder();
    }
    PyStringBuilder(long capacity) {
        value = new StringBuilder(Math.toIntExact(capacity));
    }

    @Override public int hashCode() { return defaultHashCode(); }
    @Override public String repr() { throw new UnsupportedOperationException("PyStringBuilder is internal-only"); }
    @Override public PyType type() { throw new UnsupportedOperationException("PyStringBuilder is internal-only"); }
}

final class PyBytesBuilder extends PyTruthyObject {
    final ByteArrayOutputStream value;

    PyBytesBuilder() {
        value = new ByteArrayOutputStream();
    }
    PyBytesBuilder(long capacity) {
        value = new ByteArrayOutputStream(Math.toIntExact(capacity));
    }

    @Override public int hashCode() { return defaultHashCode(); }
    @Override public String repr() { throw new UnsupportedOperationException("PyBytesBuilder is internal-only"); }
    @Override public PyType type() { throw new UnsupportedOperationException("PyBytesBuilder is internal-only"); }
}

final class PySysImplementation extends PyTruthyObject {
    public static final PySysImplementation singleton = new PySysImplementation();

    private PySysImplementation() {}

    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "name": return new PyString("pythonj");
            default: return super.getAttr(key);
        }
    }
    @Override public int hashCode() { return defaultHashCode(); }
    @Override public String repr() { return defaultRepr(); }
    @Override public PyType type() { return PyObjectType.singleton; }
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

abstract class PyModule extends PyTruthyObject {
    private final String moduleName;

    protected PyModule(String _moduleName) {
        moduleName = _moduleName;
    }

    @Override public PyObject getAttr(String key) {
        if (key.equals("__name__")) {
            return new PyString(moduleName);
        }
        return super.getAttr(key);
    }
    @Override public boolean equals(Object rhs) { return this == rhs; }
    @Override public int hashCode() { return defaultHashCode(); }
    @Override public String repr() { return "<module '" + moduleName + "' (built-in)>"; }
    @Override public PyType type() { return PyModuleType.singleton; }
}

final class PyJsonModule extends PyModule {
    public static final PyJsonModule singleton = new PyJsonModule();

    private PyJsonModule() { super("_json"); }

    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "encode_basestring_ascii": return PyJsonFunction_encode_basestring_ascii.singleton;
            case "scanstring": return PyJsonFunction_scanstring.singleton;
            default: return super.getAttr(key);
        }
    }
}

final class PyIoModule extends PyModule {
    public static final PyIoModule singleton = new PyIoModule();

    private PyIoModule() { super("_io"); }

    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "BufferedReader": return PyBufferedReaderType.singleton;
            case "TextIOWrapper": return PyTextIOWrapperType.singleton;
            default: return super.getAttr(key);
        }
    }
}

final class PyMathModule extends PyModule {
    public static final PyMathModule singleton = new PyMathModule();

    private PyMathModule() { super("math"); }

    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "copysign": return PyMathFunction_copysign.singleton;
            case "isfinite": return PyMathFunction_isfinite.singleton;
            case "isinf": return PyMathFunction_isinf.singleton;
            case "isnan": return PyMathFunction_isnan.singleton;
            default: return super.getAttr(key);
        }
    }
}

final class PySysModule extends PyModule {
    public static final PySysModule singleton = new PySysModule();

    private PySysModule() { super("sys"); }

    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "argv": return Runtime.sysArgv;
            case "exit": return PySysFunction_exit.singleton;
            case "implementation": return PySysImplementation.singleton;
            default: return super.getAttr(key);
        }
    }
}

final class PyOperatorModule extends PyModule {
    public static final PyOperatorModule singleton = new PyOperatorModule();

    private PyOperatorModule() { super("_operator"); }

    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "contains": return PyOperatorFunction_contains.singleton;
            case "delitem": return PyOperatorFunction_delitem.singleton;
            case "getitem": return PyOperatorFunction_getitem.singleton;
            case "index": return PyOperatorFunction_index.singleton;
            case "setitem": return PyOperatorFunction_setitem.singleton;
            default: return super.getAttr(key);
        }
    }
}

final class PyTypesModule extends PyModule {
    public static final PyTypesModule singleton = new PyTypesModule();

    private PyTypesModule() { super("_types"); }

    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "BuiltinFunctionType": return PyBuiltinFunctionOrMethodType.singleton;
            case "ClassMethodDescriptorType": return PyClassMethodDescriptorType.singleton;
            case "FunctionType": return PyFunctionType.singleton;
            case "GetSetDescriptorType": return PyGetSetDescriptorType.singleton;
            case "MappingProxyType": return PyMappingProxyType.singleton;
            case "MemberDescriptorType": return PyMemberDescriptorType.singleton;
            case "MethodDescriptorType": return PyMethodDescriptorType.singleton;
            case "NoneType": return PyNoneType.singleton;
            default: return super.getAttr(key);
        }
    }
}

final class PyZlibModule extends PyModule {
    public static final PyZlibModule singleton = new PyZlibModule();

    private PyZlibModule() { super("zlib"); }

    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "compress": return PyZlibFunction_compress.singleton;
            case "decompress": return PyZlibFunction_decompress.singleton;
            case "error": return PyZlibErrorType.singleton;
            default: return super.getAttr(key);
        }
    }
}

abstract class PyType extends PyTruthyObject {
    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        if (args.length == 1) {
            return newObjPositional(args[0], null, null);
        }
        if (args.length == 3) {
            return newObjPositional(args[0], args[1], args[2]);
        }
        throw PyTypeError.raise("type() takes 1 or 3 arguments");
    }
    public static PyType newObjPositional(PyObject arg0, PyObject arg1, PyObject arg2) {
        if ((arg1 == null) && (arg2 == null)) {
            return arg0.type();
        }
        if ((arg1 != null) && (arg2 != null)) {
            throw new UnsupportedOperationException("type(name, bases, dict) is unsupported");
        }
        throw PyTypeError.raise("type() takes 1 or 3 arguments");
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

    protected PyConcreteType(String name, Class<? extends PyObject> _instanceClass) {
        typeName = name;
        instanceClass = _instanceClass;
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
                return new PyMappingProxy(attrs);
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
    @Override public final String repr() { return "<class '" + typeName + "'>"; }
    @Override public final PyTypeType type() { return PyTypeType.singleton; }
    @Override public final String name() { return typeName; }
}

final class PyModuleType extends PyConcreteType {
    public static final PyModuleType singleton = new PyModuleType();

    private PyModuleType() { super("module", PyModule.class); }
}

final class PyZlibErrorType extends PyConcreteType {
    public static final PyZlibErrorType singleton = new PyZlibErrorType();

    private PyZlibErrorType() { super("error", PyZlibError.class); }

    @Override public PyObject call(PyObject[] args, PyDict kwargs) {
        return PyZlibError.newObj(this, args, kwargs);
    }
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

    @Override public final PyObject call(PyObject[] args, PyDict kwargs) {
        if (args.length == 0) {
            throw PyTypeError.raise("unbound method " + owner.name() + "." + name + "() needs an argument");
        }
        PyObject self = args[0];
        if (!Runtime.pythonjIsInstance(self, owner).boolValue()) {
            throw PyTypeError.raise(
                "descriptor " + PyString.reprOf(name) + " for " + PyString.reprOf(owner.name()) +
                " objects doesn't apply to a " + PyString.reprOf(self.type().name()) + " object"
            );
        }
        PyObject[] rest = Arrays.copyOfRange(args, 1, args.length);
        return getter.apply(self).call(rest, kwargs);
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
    public static PyList sysArgv = new PyList(new PyObject[] {new PyString("")});

    public static void setArgv(String argv0, String[] args) {
        PyObject[] argv = new PyObject[args.length + 1];
        argv[0] = new PyString(argv0);
        for (int i = 0; i < args.length; i++) {
            argv[i + 1] = new PyString(args[i]);
        }
        sysArgv = new PyList(argv);
    }

    public static PyBool pythonjIsInstance(PyObject obj, PyObject type) {
        if (type instanceof PyConcreteType typeClass) {
            return PyBool.create(typeClass.instanceClass.isInstance(obj));
        } else if (type instanceof PyType) {
            throw new UnsupportedOperationException("isinstance() is unimplemented for type " + type.repr());
        } else {
            throw PyTypeError.raise("isinstance() arg 2 must be a type, a tuple of types, or a union");
        }
    }
    public static PyBool pythonjIsSubclass(PyObject obj, PyObject type) {
        if (obj instanceof PyConcreteType objClass &&
            type instanceof PyConcreteType typeClass) {
            return PyBool.create(typeClass.instanceClass.isAssignableFrom(objClass.instanceClass));
        } else {
            throw new UnsupportedOperationException(String.format("issubclass() is unimplemented for types %s and %s", obj.repr(), type.repr()));
        }
    }
    public static PyRaise raiseExactArgs(PyObject[] args, int n, String name) {
        return PyTypeError.raiseFormat("%s expected %d argument%s, got %d", name, n, (n == 1) ? "" : "s", args.length);
    }
    public static void requireNoKwArgs(PyDict kwargs, String name) {
        if ((kwargs != null) && kwargs.boolValue()) {
            throw PyTypeError.raise(name + "() takes no keyword arguments");
        }
    }
    public static PyRaise raiseAtMostArgs(String name, long max, long given) {
        return PyTypeError.raiseFormat("%s() takes at most %d arguments (%d given)", name, max, given);
    }
    public static PyRaise raiseAtMostKwArgs(String name, long max, long argsLength, long kwargsLen) {
        if (argsLength == 0) {
            return PyTypeError.raiseFormat("%s() takes at most %d keyword argument%s (%d given)", name, max, (max == 1) ? "" : "s", kwargsLen);
        } else {
            return raiseAtMostArgs(name, max, argsLength + kwargsLen);
        }
    }
    public static PyRaise raiseUnexpectedKwArg(String name, String kwName) {
        return PyTypeError.raiseFormat("%s() got an unexpected keyword argument %s", name, PyString.reprOf(kwName));
    }
    public static PyRaise raiseExpr(PyObject exc) {
        if (exc instanceof PyBaseException baseExc) {
            return new PyRaise(baseExc);
        } else if ((exc instanceof PyConcreteType excType) && PyBaseException.class.isAssignableFrom(excType.instanceClass)) {
            return new PyRaise((PyBaseException)excType.call(new PyObject[0], null));
        } else {
            throw PyTypeError.raise("exceptions must derive from BaseException");
        }
    }
    public static void handleTopLevelPyRaise(PyRaise exc) {
        PyBaseException baseExc = exc.exc;
        if (baseExc instanceof PySystemExit) {
            PyObject code = PySystemExit.pymember_code(baseExc);
            if (code == PyNone.singleton) {
                System.exit(0);
            } else if (code.hasIndex()) {
                System.exit((int)code.indexValue());
            } else {
                System.err.println(code.str());
                System.exit(1);
            }
        }
        throw exc;
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
    public static void requireMinMaxPositional(PyObject[] args, PyDict kwargs, String name, int minArgs, int maxArgs) {
        PyString pyName = new PyString(name);
        PyRuntime.pyfunc_require_min_max_positional(new PyInt(args.length), kwargs, pyName, pyName, new PyInt(minArgs), new PyInt(maxArgs));
    }
    public static ArrayList<PyObject> bindMinMaxPositionalOrKeyword(PyObject[] args, PyDict kwargs, PyString kwName, PyString positionalName, PyTuple positionalNames, PyInt posonlyCount, PyTuple kwonlyNames, PyInt minArgs, PyInt maxPositional, PyInt maxTotal, PyBool minPositionalStyle, PyBool exactArgsStyle) {
        return PyRuntime.pyfunc_bind_min_max_positional_or_keyword(new PyTuple(args), kwargs, kwName, positionalName, positionalNames, posonlyCount, kwonlyNames, minArgs, maxPositional, maxTotal, minPositionalStyle, exactArgsStyle).items;
    }
    public static ArrayList<PyObject> bindMinMaxPositionalOrKeyword(PyObject[] args, PyDict kwargs, String name, PyTuple positionalNames, int posonlyCount, PyTuple kwonlyNames, int minArgs, int maxPositional, int maxTotal, boolean minPositionalStyle, boolean exactArgsStyle) {
        PyString pyName = new PyString(name);
        return bindMinMaxPositionalOrKeyword(args, kwargs, pyName, pyName, positionalNames, new PyInt(posonlyCount), kwonlyNames, new PyInt(minArgs), new PyInt(maxPositional), new PyInt(maxTotal), PyBool.create(minPositionalStyle), PyBool.create(exactArgsStyle));
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
        long raw = obj.indexValue();
        int i;
        if (raw < 0) {
            long adjusted = Math.max(raw + n, 0);
            i = Math.toIntExact(adjusted);
        } else {
            i = Math.toIntExact(Math.min(raw, n));
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
        long raw = obj.indexValue();
        int i;
        if (raw < 0) {
            long adjusted = Math.max(raw + n, 0);
            i = Math.toIntExact(adjusted);
        } else {
            i = Math.toIntExact(raw);
        }
        return i;
    }
    public static int asBoundedSearchIndexAllowNull(PyObject obj, int defaultIndex, int n) {
        if (obj == null) {
            return defaultIndex;
        }
        if (!obj.hasIndex()) {
            throw PyTypeError.raise("slice indices must be integers or have an __index__ method");
        }
        long raw = obj.indexValue();
        if (raw < 0) {
            return Math.toIntExact(Math.max(raw + n, 0));
        }
        return Math.toIntExact(Math.min(raw, n));
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
    public static byte[] getBytesLikeBuffer(PyObject arg) {
        if (arg instanceof PyBytes argBytes) {
            return argBytes.value;
        } else if (arg instanceof PyByteArray argByteArray) {
            return argByteArray.value;
        } else {
            return null;
        }
    }
    public static byte[] requireBytesLikeBuffer(PyObject arg) {
        byte[] buffer = getBytesLikeBuffer(arg);
        if (buffer == null) {
            throw PyTypeError.raise("a bytes-like object is required, not " + PyString.reprOf(arg.type().name()));
        }
        return buffer;
    }
    public static String requireEncodingArg(PyObject encodingObj, String name) {
        if (!(encodingObj instanceof PyString encodingStr)) {
            String typeName = (encodingObj == PyNone.singleton) ? "None" : encodingObj.type().name();
            throw PyTypeError.raiseFormat("%s() argument 'encoding' must be str, not %s", name, typeName);
        }
        return encodingStr.value;
    }
    public static void requireErrorsArg(PyObject errorsObj, String name) {
        if (!(errorsObj instanceof PyString)) {
            String typeName = (errorsObj == PyNone.singleton) ? "None" : errorsObj.type().name();
            throw PyTypeError.raiseFormat("%s() argument 'errors' must be str, not %s", name, typeName);
        }
    }
    public static Charset lookupCharset(String encoding) {
        try {
            return Charset.forName(encoding);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw PyValueError.raise("unknown encoding: " + encoding);
        }
    }
    public static String getDefaultTextEncoding() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win") ? "cp1252" : "UTF-8"; // XXX extremely basic, good enough for now
    }
}
