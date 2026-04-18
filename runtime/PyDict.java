// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class PyDict extends PyObject {
    static final class PyDictIter extends PyIter {
        private static final PyConcreteType type_singleton = new PyConcreteType("dict_keyiterator", PyDictIter.class, PyObjectType.singleton, null);

        private final Iterator<PyObject> it;

        PyDictIter(Iterator<PyObject> _it) { it = _it; }

        @Override public PyObject next() { return it.hasNext() ? it.next() : null; }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyConcreteType type() { return type_singleton; }
    };

    static final class PyDictItemIter extends PyIter {
        private static final PyConcreteType type_singleton = new PyConcreteType("dict_itemiterator", PyDictItemIter.class, PyObjectType.singleton, null);

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
        @Override public PyConcreteType type() { return type_singleton; }
    };

    static final class PyDictValueIter extends PyIter {
        private static final PyConcreteType type_singleton = new PyConcreteType("dict_valueiterator", PyDictValueIter.class, PyObjectType.singleton, null);

        private final Iterator<Map.Entry<PyObject, PyObject>> it;

        PyDictValueIter(Iterator<Map.Entry<PyObject, PyObject>> _it) { it = _it; }

        @Override public PyObject next() { return it.hasNext() ? it.next().getValue() : null; }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyConcreteType type() { return type_singleton; }
    };

    static final class PyDictItems extends PyObject {
        private static final PyConcreteType type_singleton = new PyConcreteType("dict_items", PyDictItems.class, PyObjectType.singleton, null);

        private final Map<PyObject, PyObject> items;

        PyDictItems(Map<PyObject, PyObject> _items) { items = _items; }

        private HashSet<PyObject> materializeSet() {
            var ret = new HashSet<PyObject>();
            for (var x: items.entrySet()) {
                ret.add(new PyTuple(new PyObject[] {x.getKey(), x.getValue()}));
            }
            return ret;
        }

        @Override public PyObject and(PyObject rhs) {
            var itemsSet = materializeSet();
            var rhsSet = rhs.asSetOrNull();
            if (rhsSet != null) {
                return new PySet(PySet.intersectionPreserveLeft(itemsSet, rhsSet));
            } else {
                return new PySet(PySet.intersectionPreserveLeft(PySet.materializeIterable(rhs), itemsSet));
            }
        }
        @Override public PyObject or(PyObject rhs) {
            var itemsSet = materializeSet();
            return new PySet(PySet.unionPreserveLeft(itemsSet, PySet.materializeIterable(rhs)));
        }
        @Override public PyObject sub(PyObject rhs) {
            var itemsSet = materializeSet();
            return new PySet(PySet.differencePreserveLeft(itemsSet, PySet.materializeIterable(rhs)));
        }
        @Override public PyObject xor(PyObject rhs) {
            var itemsSet = materializeSet();
            var rhsSet = PySet.materializeIterable(rhs);
            return new PySet(PySet.symmetricDifferencePreserveLeft(itemsSet, itemsSet, rhsSet, rhsSet));
        }
        @Override public PyObject rand(PyObject rhs) {
            var itemsSet = materializeSet();
            var rhsSet = rhs.asSetOrNull();
            if (rhsSet != null) {
                return new PySet(PySet.intersectionPreserveLeft(itemsSet, rhsSet));
            } else {
                return new PySet(PySet.intersectionPreserveLeft(PySet.materializeIterable(rhs), itemsSet));
            }
        }
        @Override public PyObject ror(PyObject rhs) {
            var itemsSet = materializeSet();
            var rhsSet = PySet.materializeIterable(rhs);
            return new PySet(PySet.unionPreserveLeft(rhsSet, itemsSet));
        }
        @Override public PyObject rsub(PyObject rhs) {
            var itemsSet = materializeSet();
            var rhsSet = PySet.materializeIterable(rhs);
            return new PySet(PySet.differencePreserveLeft(rhsSet, itemsSet));
        }
        @Override public PyObject rxor(PyObject rhs) {
            var itemsSet = materializeSet();
            var rhsSet = PySet.materializeIterable(rhs);
            return new PySet(PySet.symmetricDifferencePreserveLeft(rhsSet, rhsSet, itemsSet, itemsSet));
        }

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
        @Override public PyConcreteType type() { return type_singleton; }
    };

    static final class PyDictKeys extends PyObject {
        private static final PyConcreteType type_singleton = new PyConcreteType("dict_keys", PyDictKeys.class, PyObjectType.singleton, null);

        private final Map<PyObject, PyObject> items;

        PyDictKeys(Map<PyObject, PyObject> _items) { items = _items; }

        @Override public PySet and(PyObject rhs) {
            var rhsSet = rhs.asSetOrNull();
            if (rhsSet != null) {
                return new PySet(PySet.intersectionPreserveLeft(items.keySet(), rhsSet));
            } else {
                return new PySet(PySet.intersectionPreserveLeft(PySet.materializeIterable(rhs), items.keySet()));
            }
        }
        @Override public PyObject or(PyObject rhs) {
            return new PySet(PySet.unionPreserveLeft(items.keySet(), PySet.materializeIterable(rhs)));
        }
        @Override public PyObject sub(PyObject rhs) {
            return new PySet(PySet.differencePreserveLeft(items.keySet(), PySet.materializeIterable(rhs)));
        }
        @Override public PyObject xor(PyObject rhs) {
            var rhsSet = PySet.materializeIterable(rhs);
            return new PySet(PySet.symmetricDifferencePreserveLeft(items.keySet(), items.keySet(), rhsSet, rhsSet));
        }
        @Override public PyObject rand(PyObject rhs) {
            var rhsSet = rhs.asSetOrNull();
            if (rhsSet != null) {
                return new PySet(PySet.intersectionPreserveLeft(items.keySet(), rhsSet));
            } else {
                return new PySet(PySet.intersectionPreserveLeft(PySet.materializeIterable(rhs), items.keySet()));
            }
        }
        @Override public PyObject ror(PyObject rhs) {
            var rhsSet = PySet.materializeIterable(rhs);
            return new PySet(PySet.unionPreserveLeft(rhsSet, items.keySet()));
        }
        @Override public PyObject rsub(PyObject rhs) {
            var rhsSet = PySet.materializeIterable(rhs);
            return new PySet(PySet.differencePreserveLeft(rhsSet, items.keySet()));
        }
        @Override public PyObject rxor(PyObject rhs) {
            var rhsSet = PySet.materializeIterable(rhs);
            return new PySet(PySet.symmetricDifferencePreserveLeft(rhsSet, rhsSet, items.keySet(), items.keySet()));
        }

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
        @Override public PyConcreteType type() { return type_singleton; }
    };

    static final class PyDictValues extends PyObject {
        private static final PyConcreteType type_singleton = new PyConcreteType("dict_values", PyDictValues.class, PyObjectType.singleton, null);

        private final Map<PyObject, PyObject> items;

        PyDictValues(Map<PyObject, PyObject> _items) { items = _items; }

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
        @Override public PyConcreteType type() { return type_singleton; }
    };

    public final LinkedHashMap<PyObject, PyObject> items;

    private static boolean hasKeysMethod(PyObject obj) {
        try {
            obj.getAttr("keys");
            return true;
        } catch (PyRaise e) {
            if (e.exc instanceof PyAttributeError) {
                return false;
            }
            throw e;
        }
    }

    private void updateFromMapping(PyObject mapping) {
        var keys = mapping.getAttr("keys").call(new PyObject[] {}, null);
        var iter = keys.iter();
        for (var key = iter.next(); key != null; key = iter.next()) {
            items.put(key, mapping.getItem(key));
        }
    }

    PyDict(PyObject... args) {
        items = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            PyObject k = args[i];
            PyObject v = args[i+1];
            if (k == null) { // used to encode dictionary unpacking
                if (v instanceof PyDict dict) {
                    items.putAll(dict.items);
                } else if (hasKeysMethod(v)) {
                    updateFromMapping(v);
                } else {
                    throw new UnsupportedOperationException("dictionary unpacking only implemented for dict");
                }
            } else {
                items.put(k, v);
            }
        }
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        if (args.length > 1) {
            throw Runtime.raiseMaxArgs(args, 1, type.name());
        }
        var ret = new PyDict();
        ret.updateImpl(args, kwargs);
        return ret;
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
    @Override public PyConcreteType type() { return PyDictType.singleton; }

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
    @Override public String repr() { return PyRuntime.pyfunc_dict____repr__(this).value; }

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
    private void updateImpl(PyObject[] args, PyDict kwargs) {
        if (args.length == 1) {
            var arg = args[0];
            if (arg instanceof PyDict dict) {
                items.putAll(dict.items);
            } else if (hasKeysMethod(arg)) {
                updateFromMapping(arg);
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
                        throw PyValueError.raise("dictionary update sequence element #" + index + " has length 0; 2 is required");
                    }
                    var value = itemIter.next();
                    if (value == null) {
                        throw PyValueError.raise("dictionary update sequence element #" + index + " has length 1; 2 is required");
                    }
                    var nextItem = itemIter.next();
                    if (nextItem != null) {
                        long length = 3;
                        while (itemIter.next() != null) {
                            length++;
                        }
                        throw PyValueError.raise("dictionary update sequence element #" + index + " has length " + length + "; 2 is required");
                    }
                    items.put(key, value);
                }
            }
        }
        if (kwargs != null) {
            items.putAll(kwargs.items);
        }
    }
    public PyNone pymethod_update(PyObject[] args, PyDict kwargs) {
        if (args.length > 1) {
            throw Runtime.raiseMaxArgs(args, 1, "update");
        }
        updateImpl(args, kwargs);
        return PyNone.singleton;
    }
    public PyDictValues pymethod_values() { return new PyDictValues(items); }
}
