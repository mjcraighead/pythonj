// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class PyDictType extends PyBuiltinType {
// BEGIN GENERATED CODE: PyDictType
    public static final PyDictType singleton = new PyDictType();
    private static final PyMethodDescriptor pyattr_get = new PyMethodDescriptor(singleton, "get", PyDict.PyDictMethod_get::new);
    private static final PyMethodDescriptor pyattr_setdefault = new PyMethodDescriptor(singleton, "setdefault", PyDict.PyDictMethod_setdefault::new);
    private static final PyMethodDescriptor pyattr_pop = new PyMethodDescriptor(singleton, "pop", PyDict.PyDictMethod_pop::new);
    private static final PyMethodDescriptor pyattr_popitem = new PyMethodDescriptor(singleton, "popitem", PyDict.PyDictMethod_popitem::new);
    private static final PyMethodDescriptor pyattr_keys = new PyMethodDescriptor(singleton, "keys", PyDict.PyDictMethod_keys::new);
    private static final PyMethodDescriptor pyattr_items = new PyMethodDescriptor(singleton, "items", PyDict.PyDictMethod_items::new);
    private static final PyMethodDescriptor pyattr_values = new PyMethodDescriptor(singleton, "values", PyDict.PyDictMethod_values::new);
    private static final PyMethodDescriptor pyattr_update = new PyMethodDescriptor(singleton, "update", PyDict.PyDictMethod_update::new);
    private static final PyClassMethodDescriptor pyattr_fromkeys = new PyClassMethodDescriptor(singleton, "fromkeys", PyDictType.PyDictClassMethod_fromkeys::new);
    private static final PyMethodDescriptor pyattr_clear = new PyMethodDescriptor(singleton, "clear", PyDict.PyDictMethod_clear::new);
    private static final PyMethodDescriptor pyattr_copy = new PyMethodDescriptor(singleton, "copy", PyDict.PyDictMethod_copy::new);
    private static final PyString pyattr___doc__ = new PyString("dict() -> new empty dictionary\ndict(mapping) -> new dictionary initialized from a mapping object's\n    (key, value) pairs\ndict(iterable) -> new dictionary initialized as if via:\n    d = {}\n    for k, v in iterable:\n        d[k] = v\ndict(**kwargs) -> new dictionary initialized with the name=value pairs\n    in the keyword argument list.  For example:  dict(one=1, two=2)");
    private static final PyAttr attrs[] = new PyAttr[] {
        new PyAttr("get", pyattr_get),
        new PyAttr("setdefault", pyattr_setdefault),
        new PyAttr("pop", pyattr_pop),
        new PyAttr("popitem", pyattr_popitem),
        new PyAttr("keys", pyattr_keys),
        new PyAttr("items", pyattr_items),
        new PyAttr("values", pyattr_values),
        new PyAttr("update", pyattr_update),
        new PyAttr("fromkeys", pyattr_fromkeys),
        new PyAttr("clear", pyattr_clear),
        new PyAttr("copy", pyattr_copy),
        new PyAttr("__doc__", pyattr___doc__)
    };
    @Override public PyAttr[] getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "get": return pyattr_get;
            case "setdefault": return pyattr_setdefault;
            case "pop": return pyattr_pop;
            case "popitem": return pyattr_popitem;
            case "keys": return pyattr_keys;
            case "items": return pyattr_items;
            case "values": return pyattr_values;
            case "update": return pyattr_update;
            case "fromkeys": return pyattr_fromkeys;
            case "clear": return pyattr_clear;
            case "copy": return pyattr_copy;
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
// END GENERATED CODE: PyDictType

    private PyDictType() { super("dict", PyDict.class); }
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

public final class PyDict extends PyObject {
    static final class PyDictIter extends PyIter {
        private static final PyBuiltinType type_singleton = new PyBuiltinType("dict_keyiterator", PyDictIter.class);

        private final Iterator<PyObject> it;

        PyDictIter(Iterator<PyObject> _it) { it = _it; }

        @Override public PyObject next() { return it.hasNext() ? it.next() : null; }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyBuiltinType type() { return type_singleton; }
    };

    static final class PyDictItemIter extends PyIter {
        private static final PyBuiltinType type_singleton = new PyBuiltinType("dict_itemiterator", PyDictItemIter.class);

        private final Iterator<Map.Entry<PyObject, PyObject>> it;

        PyDictItemIter(Iterator<Map.Entry<PyObject, PyObject>> _it) { it = _it; }

        @Override public PyObject next() {
            if (!it.hasNext()) {
                return null;
            }
            Map.Entry<PyObject, PyObject> entry = it.next();
            return new PyTuple(new PyObject[] {entry.getKey(), entry.getValue()});
        }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyBuiltinType type() { return type_singleton; }
    };

    static final class PyDictValueIter extends PyIter {
        private static final PyBuiltinType type_singleton = new PyBuiltinType("dict_valueiterator", PyDictValueIter.class);

        private final Iterator<Map.Entry<PyObject, PyObject>> it;

        PyDictValueIter(Iterator<Map.Entry<PyObject, PyObject>> _it) { it = _it; }

        @Override public PyObject next() { return it.hasNext() ? it.next().getValue() : null; }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyBuiltinType type() { return type_singleton; }
    };

    static final class PyDictItems extends PyObject {
        private static final PyBuiltinType type_singleton = new PyBuiltinType("dict_items", PyDictItems.class);

        private final LinkedHashMap<PyObject, PyObject> items;

        PyDictItems(LinkedHashMap<PyObject, PyObject> _items) { items = _items; }

        private HashSet<PyObject> materializeSet() {
            var ret = new HashSet<PyObject>();
            for (var x: items.entrySet()) {
                ret.add(new PyTuple(new PyObject[] {x.getKey(), x.getValue()}));
            }
            return ret;
        }

        @Override public PyObject and(PyObject rhs) {
            var itemsSet = materializeSet();
            var result = new HashSet<PyObject>();
            var iter = rhs.iter();
            for (var item = iter.next(); item != null; item = iter.next()) {
                if (itemsSet.contains(item)) {
                    result.add(item);
                }
            }
            return new PySet(result);
        }
        @Override public PyObject or(PyObject rhs) {
            var itemsSet = materializeSet();
            Runtime.addIterableToCollection(itemsSet, rhs);
            return new PySet(itemsSet);
        }
        @Override public PyObject sub(PyObject rhs) {
            var itemsSet = materializeSet();
            var iter = rhs.iter();
            for (var item = iter.next(); item != null; item = iter.next()) {
                itemsSet.remove(item);
            }
            return new PySet(itemsSet);
        }
        @Override public PyObject xor(PyObject rhs) {
            var itemsSet = materializeSet();
            var rhsSet = new HashSet<PyObject>();
            Runtime.addIterableToCollection(rhsSet, rhs);
            return new PySet(PySet.xor(itemsSet, rhsSet));
        }
        @Override public PyObject rand(PyObject rhs) { return and(rhs); }
        @Override public PyObject ror(PyObject rhs) { return or(rhs); }
        @Override public PyObject rsub(PyObject rhs) {
            var itemsSet = materializeSet();
            var rhsSet = new HashSet<PyObject>();
            Runtime.addIterableToCollection(rhsSet, rhs);
            rhsSet.removeAll(itemsSet);
            return new PySet(rhsSet);
        }
        @Override public PyObject rxor(PyObject rhs) { return xor(rhs); }

        @Override public boolean ge(PyObject rhs) {
            var rhsSet = rhs.asSetOrNull();
            if (rhsSet != null) {
                return PySet.ge(materializeSet(), rhsSet);
            } else {
                return super.ge(rhs);
            }
        }
        @Override public boolean gt(PyObject rhs) {
            var rhsSet = rhs.asSetOrNull();
            if (rhsSet != null) {
                return PySet.gt(materializeSet(), rhsSet);
            } else {
                return super.gt(rhs);
            }
        }
        @Override public boolean le(PyObject rhs) {
            var rhsSet = rhs.asSetOrNull();
            if (rhsSet != null) {
                return PySet.le(materializeSet(), rhsSet);
            } else {
                return super.le(rhs);
            }
        }
        @Override public boolean lt(PyObject rhs) {
            var rhsSet = rhs.asSetOrNull();
            if (rhsSet != null) {
                return PySet.lt(materializeSet(), rhsSet);
            } else {
                return super.lt(rhs);
            }
        }

        @Override public Set<PyObject> asSetOrNull() { return materializeSet(); }
        @Override public boolean boolValue() { return !items.isEmpty(); }
        @Override public final boolean hasIter() { return true; }
        @Override public PyDictItemIter iter() { return new PyDictItemIter(items.entrySet().iterator()); }
        @Override public long len() { return items.size(); }
        @Override public String repr() {
            var s = new StringBuilder("dict_items([");
            boolean first = true;
            for (var x: items.entrySet()) {
                if (!first) {
                    s.append(", ");
                }
                first = false;
                s.append("(");
                s.append(x.getKey().repr());
                s.append(", ");
                s.append(x.getValue().repr());
                s.append(")");
            }
            return s + "])";
        }
        @Override public PyBuiltinType type() { return type_singleton; }
    };

    static final class PyDictKeys extends PyObject {
        private static final PyBuiltinType type_singleton = new PyBuiltinType("dict_keys", PyDictKeys.class);

        private final LinkedHashMap<PyObject, PyObject> items;

        PyDictKeys(LinkedHashMap<PyObject, PyObject> _items) { items = _items; }

        @Override public PySet and(PyObject rhs) {
            var result = new HashSet<PyObject>();
            var iter = rhs.iter();
            for (var item = iter.next(); item != null; item = iter.next()) {
                if (items.containsKey(item)) {
                    result.add(item);
                }
            }
            return new PySet(result);
        }
        @Override public PyObject or(PyObject rhs) {
            var result = new HashSet<PyObject>(items.keySet());
            Runtime.addIterableToCollection(result, rhs);
            return new PySet(result);
        }
        @Override public PyObject sub(PyObject rhs) {
            var result = new HashSet<PyObject>(items.keySet());
            var iter = rhs.iter();
            for (var item = iter.next(); item != null; item = iter.next()) {
                result.remove(item);
            }
            return new PySet(result);
        }
        @Override public PyObject xor(PyObject rhs) {
            var rhsSet = new HashSet<PyObject>();
            Runtime.addIterableToCollection(rhsSet, rhs);
            return new PySet(PySet.xor(items.keySet(), rhsSet));
        }
        @Override public PyObject rand(PyObject rhs) { return and(rhs); }
        @Override public PyObject ror(PyObject rhs) { return or(rhs); }
        @Override public PyObject rsub(PyObject rhs) {
            var result = new HashSet<PyObject>();
            Runtime.addIterableToCollection(result, rhs);
            result.removeAll(items.keySet());
            return new PySet(result);
        }
        @Override public PyObject rxor(PyObject rhs) { return xor(rhs); }

        @Override public boolean ge(PyObject rhs) {
            var rhsSet = rhs.asSetOrNull();
            if (rhsSet != null) {
                return PySet.ge(items.keySet(), rhsSet);
            } else {
                return super.ge(rhs);
            }
        }
        @Override public boolean gt(PyObject rhs) {
            var rhsSet = rhs.asSetOrNull();
            if (rhsSet != null) {
                return PySet.gt(items.keySet(), rhsSet);
            } else {
                return super.gt(rhs);
            }
        }
        @Override public boolean le(PyObject rhs) {
            var rhsSet = rhs.asSetOrNull();
            if (rhsSet != null) {
                return PySet.le(items.keySet(), rhsSet);
            } else {
                return super.le(rhs);
            }
        }
        @Override public boolean lt(PyObject rhs) {
            var rhsSet = rhs.asSetOrNull();
            if (rhsSet != null) {
                return PySet.lt(items.keySet(), rhsSet);
            } else {
                return super.lt(rhs);
            }
        }

        @Override public Set<PyObject> asSetOrNull() { return items.keySet(); }
        @Override public boolean boolValue() { return !items.isEmpty(); }
        @Override public final boolean hasIter() { return true; }
        @Override public PyDictIter iter() { return new PyDictIter(items.keySet().iterator()); }
        @Override public long len() { return items.size(); }
        @Override public String repr() {
            var s = new StringBuilder("dict_keys([");
            boolean first = true;
            for (var x: items.keySet()) {
                if (!first) {
                    s.append(", ");
                }
                first = false;
                s.append(x.repr());
            }
            return s + "])";
        }
        @Override public PyBuiltinType type() { return type_singleton; }
    };

    private static final class PyDictValues extends PyObject {
        private static final PyBuiltinType type_singleton = new PyBuiltinType("dict_values", PyDictValues.class);

        private final LinkedHashMap<PyObject, PyObject> items;

        PyDictValues(LinkedHashMap<PyObject, PyObject> _items) { items = _items; }

        @Override public boolean boolValue() { return !items.isEmpty(); }
        @Override public final boolean hasIter() { return true; }
        @Override public PyDictValueIter iter() { return new PyDictValueIter(items.entrySet().iterator()); }
        @Override public long len() { return items.size(); }
        @Override public String repr() {
            var s = new StringBuilder("dict_values([");
            boolean first = true;
            for (var x: items.entrySet()) {
                if (!first) {
                    s.append(", ");
                }
                first = false;
                s.append(x.getValue().repr());
            }
            return s + "])";
        }
        @Override public PyBuiltinType type() { return type_singleton; }
    };

    protected static final class PyDictMethod_clear extends PyBuiltinMethod<PyDict> {
        PyDictMethod_clear(PyObject _self) { super((PyDict)_self); }
        @Override public String methodName() { return "clear"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "dict.clear");
            Runtime.requireExactArgsAlt(args, 0, "dict.clear");
            return self.pymethod_clear();
        }
    }
    protected static final class PyDictMethod_copy extends PyBuiltinMethod<PyDict> {
        PyDictMethod_copy(PyObject _self) { super((PyDict)_self); }
        @Override public String methodName() { return "copy"; }
        @Override public PyDict call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "dict.copy");
            Runtime.requireExactArgsAlt(args, 0, "dict.copy");
            return self.pymethod_copy();
        }
    }
    protected static final class PyDictMethod_get extends PyBuiltinMethod<PyDict> {
        PyDictMethod_get(PyObject _self) { super((PyDict)_self); }
        @Override public String methodName() { return "get"; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "dict.get");
            Runtime.requireMinArgs(args, 1, "get");
            Runtime.requireMaxArgs(args, 2, "get");
            return self.pymethod_get(args[0], (args.length == 2) ? args[1] : PyNone.singleton);
        }
    }
    protected static final class PyDictMethod_items extends PyBuiltinMethod<PyDict> {
        PyDictMethod_items(PyObject _self) { super((PyDict)_self); }
        @Override public String methodName() { return "items"; }
        @Override public PyDictItems call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "dict.items");
            Runtime.requireExactArgsAlt(args, 0, "dict.items");
            return self.pymethod_items();
        }
    }
    protected static final class PyDictMethod_keys extends PyBuiltinMethod<PyDict> {
        PyDictMethod_keys(PyObject _self) { super((PyDict)_self); }
        @Override public String methodName() { return "keys"; }
        @Override public PyDictKeys call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "dict.keys");
            Runtime.requireExactArgsAlt(args, 0, "dict.keys");
            return self.pymethod_keys();
        }
    }
    protected static final class PyDictMethod_pop extends PyBuiltinMethod<PyDict> {
        PyDictMethod_pop(PyObject _self) { super((PyDict)_self); }
        @Override public String methodName() { return "pop"; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "dict.pop");
            Runtime.requireMinArgs(args, 1, "pop");
            Runtime.requireMaxArgs(args, 2, "pop");
            return self.pymethod_pop(args[0], (args.length == 2) ? args[1] : null);
        }
    }
    protected static final class PyDictMethod_popitem extends PyBuiltinMethod<PyDict> {
        PyDictMethod_popitem(PyObject _self) { super((PyDict)_self); }
        @Override public String methodName() { return "popitem"; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "dict.popitem");
            Runtime.requireExactArgsAlt(args, 0, "dict.popitem");
            return self.pymethod_popitem();
        }
    }
    protected static final class PyDictMethod_setdefault extends PyBuiltinMethod<PyDict> {
        PyDictMethod_setdefault(PyObject _self) { super((PyDict)_self); }
        @Override public String methodName() { return "setdefault"; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "dict.setdefault");
            Runtime.requireMinArgs(args, 1, "setdefault");
            Runtime.requireMaxArgs(args, 2, "setdefault");
            return self.pymethod_setdefault(args[0], (args.length == 2) ? args[1] : PyNone.singleton);
        }
    }
    protected static final class PyDictMethod_update extends PyBuiltinMethod<PyDict> {
        PyDictMethod_update(PyObject _self) { super((PyDict)_self); }
        @Override public String methodName() { return "update"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireMaxArgs(args, 1, "update");
            self.pymethod_update(args, kwargs);
            return PyNone.singleton;
        }
    }
    protected static final class PyDictMethod_values extends PyBuiltinMethod<PyDict> {
        PyDictMethod_values(PyObject _self) { super((PyDict)_self); }
        @Override public String methodName() { return "values"; }
        @Override public PyDictValues call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "dict.values");
            Runtime.requireExactArgsAlt(args, 0, "dict.values");
            return self.pymethod_values();
        }
    }

    public final LinkedHashMap<PyObject, PyObject> items;

    PyDict(PyObject... args) {
        items = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            PyObject k = args[i];
            PyObject v = args[i+1];
            if (k == null) { // used to encode dictionary unpacking
                if (v instanceof PyDict dict) {
                    items.putAll(dict.items);
                } else {
                    throw new UnsupportedOperationException("dictionary unpacking only implemented for dict");
                }
            } else {
                items.put(k, v);
            }
        }
    }

    @Override public PyObject or(PyObject rhs) {
        if (rhs instanceof PyDict rhsDict) {
            var ret = new PyDict();
            ret.items.putAll(items);
            ret.items.putAll(rhsDict.items);
            return ret;
        } else {
            return super.or(rhs);
        }
    }
    @Override public PyObject orInPlace(PyObject rhs) {
        if (rhs instanceof PyDict rhsDict) {
            items.putAll(rhsDict.items);
            return this;
        } else {
            return super.orInPlace(rhs);
        }
    }

    @Override public PyObject getItem(PyObject key) {
        PyObject value = items.get(key);
        if (value == null) {
            throw new PyRaise(new PyKeyError(key));
        }
        return value;
    }
    @Override public void setItem(PyObject key, PyObject value) { items.put(key, value); }
    @Override public void delItem(PyObject key) {
        PyObject value = items.remove(key);
        if (value == null) {
            throw new PyRaise(new PyKeyError(key));
        }
    }

    @Override public final boolean hasIter() { return true; }
    @Override public PyDictIter iter() { return new PyDictIter(items.keySet().iterator()); }
    @Override public PyBuiltinType type() { return PyDictType.singleton; }

    @Override public boolean boolValue() { return !items.isEmpty(); }
    @Override public boolean contains(PyObject rhs) { return items.containsKey(rhs); }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PyDict rhsDict) {
            return items.equals(rhsDict.items);
        }
        return false;
    }
    @Override public int hashCode() { throw raiseUnhashable(); }
    @Override public long len() { return items.size(); }
    @Override public String repr() {
        var s = new StringBuilder("{");
        boolean first = true;
        for (var x: items.entrySet()) {
            if (!first) {
                s.append(", ");
            }
            first = false;
            s.append(x.getKey().repr());
            s.append(": ");
            s.append(x.getValue().repr());
        }
        return s + "}";
    }

    public PyNone pymethod_clear() {
        items.clear();
        return PyNone.singleton;
    }
    public PyDict pymethod_copy() {
        var ret = new PyDict();
        ret.items.putAll(items);
        return ret;
    }
    public PyObject pymethod_get(PyObject key, PyObject defaultValue) {
        return items.getOrDefault(key, defaultValue);
    }
    public PyDictItems pymethod_items() { return new PyDictItems(items); }
    public PyDictKeys pymethod_keys() { return new PyDictKeys(items); }
    public PyObject pymethod_pop(PyObject key, PyObject defaultValue) {
        PyObject value = items.remove(key);
        if (value != null) {
            return value;
        }
        if (defaultValue != null) {
            return defaultValue;
        }
        throw new PyRaise(new PyKeyError(key));
    }
    public PyObject pymethod_popitem() {
        Map.Entry<PyObject, PyObject> last = null;
        for (var e: items.entrySet()) {
            last = e;
        }
        if (last == null) {
            throw PyKeyError.raise("popitem(): dictionary is empty");
        }
        var key = last.getKey();
        var value = last.getValue();
        items.remove(key);
        return new PyTuple(new PyObject[] {key, value});
    }
    public PyObject pymethod_setdefault(PyObject key, PyObject defaultValue) {
        PyObject value = items.get(key);
        if (value != null) {
            return value;
        }
        items.put(key, defaultValue);
        return defaultValue;
    }
    public void pymethod_update(PyObject[] args, PyDict kwargs) {
        if (args.length == 1) {
            var arg = args[0];
            if (arg instanceof PyDict dict) { // XXX support arbitrary mappings here
                items.putAll(dict.items);
            } else {
                var iter = arg.iter();
                long index = 0;
                for (var item = iter.next(); item != null; item = iter.next(), index++) {
                    if (!item.hasIter()) {
                        throw PyTypeError.raise("object is not iterable");
                    }
                    var itemIter = item.iter();
                    var key = itemIter.next();
                    if (key == null) {
                        throw PyValueError.raiseFormat("dictionary update sequence element #%d has length 0; 2 is required", index);
                    }
                    var value = itemIter.next();
                    if (value == null) {
                        throw PyValueError.raiseFormat("dictionary update sequence element #%d has length 1; 2 is required", index);
                    }
                    var nextItem = itemIter.next();
                    if (nextItem != null) {
                        long length = 3;
                        while (itemIter.next() != null) {
                            length++;
                        }
                        throw PyValueError.raiseFormat("dictionary update sequence element #%d has length %d; 2 is required", index, length);
                    }
                    items.put(key, value);
                }
            }
        }
        if (kwargs != null) {
            items.putAll(kwargs.items);
        }
    }
    public PyDictValues pymethod_values() { return new PyDictValues(items); }
}
