// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;

public final class PyDict extends PyObject {
    private static final PyBuiltinClass iter_class_singleton = new PyBuiltinClass("dict_keyiterator");
    static final class PyDictIter extends PyIter {
        private final Iterator<PyObject> it;

        PyDictIter(Iterator<PyObject> _it) { it = _it; }

        @Override public PyObject next() { return it.hasNext() ? it.next() : null; }
        @Override public PyBuiltinClass type() { return iter_class_singleton; }
    };

    private static final PyBuiltinClass itemiter_class_singleton = new PyBuiltinClass("dict_itemiterator");
    static final class PyDictItemIter extends PyIter {
        private final Iterator<Map.Entry<PyObject, PyObject>> it;

        PyDictItemIter(Iterator<Map.Entry<PyObject, PyObject>> _it) { it = _it; }

        @Override public PyObject next() {
            if (!it.hasNext()) {
                return null;
            }
            Map.Entry<PyObject, PyObject> entry = it.next();
            return new PyTuple(new PyObject[] {entry.getKey(), entry.getValue()});
        }
        @Override public PyBuiltinClass type() { return itemiter_class_singleton; }
    };

    private static final PyBuiltinClass valueiter_class_singleton = new PyBuiltinClass("dict_valueiterator");
    static final class PyDictValueIter extends PyIter {
        private final Iterator<Map.Entry<PyObject, PyObject>> it;

        PyDictValueIter(Iterator<Map.Entry<PyObject, PyObject>> _it) { it = _it; }

        @Override public PyObject next() { return it.hasNext() ? it.next().getValue() : null; }
        @Override public PyBuiltinClass type() { return valueiter_class_singleton; }
    };

    private static final PyBuiltinClass items_class_singleton = new PyBuiltinClass("dict_items");
    private static final class PyDictItems extends PyObject {
        private final LinkedHashMap<PyObject, PyObject> items;

        PyDictItems(LinkedHashMap<PyObject, PyObject> _items) { items = _items; }

        @Override public boolean boolValue() { return !items.isEmpty(); }
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
        @Override public PyBuiltinClass type() { return items_class_singleton; }
    };

    private static final PyBuiltinClass keys_class_singleton = new PyBuiltinClass("dict_keys");
    private static final class PyDictKeys extends PyObject {
        private final LinkedHashMap<PyObject, PyObject> items;

        PyDictKeys(LinkedHashMap<PyObject, PyObject> _items) { items = _items; }

        @Override public boolean boolValue() { return !items.isEmpty(); }
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
        @Override public PyBuiltinClass type() { return keys_class_singleton; }
    };

    private static final PyBuiltinClass values_class_singleton = new PyBuiltinClass("dict_values");
    private static final class PyDictValues extends PyObject {
        private final LinkedHashMap<PyObject, PyObject> items;

        PyDictValues(LinkedHashMap<PyObject, PyObject> _items) { items = _items; }

        @Override public boolean boolValue() { return !items.isEmpty(); }
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
        @Override public PyBuiltinClass type() { return values_class_singleton; }
    };

    private static class PyDictMethod extends PyTruthyObject {
        protected final PyDict self;
        PyDictMethod(PyDict _self) { self = _self; }
    }
    private static final class PyDictMethod_get extends PyDictMethod {
        PyDictMethod_get(PyDict _self) { super(_self); }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            if (args.length != 2) {
                throw new RuntimeException("dict.get() takes 2 arguments");
            }
            return self.pymethod_get(args[0], args[1]);
        }
    }
    private static final class PyDictMethod_items extends PyDictMethod {
        PyDictMethod_items(PyDict _self) { super(_self); }
        @Override public PyDictItems call(PyObject[] args, PyDict kwargs) {
            if (args.length != 0) {
                throw new RuntimeException("dict.items() takes no arguments");
            }
            return self.pymethod_items();
        }
    }
    private static final class PyDictMethod_keys extends PyDictMethod {
        PyDictMethod_keys(PyDict _self) { super(_self); }
        @Override public PyDictKeys call(PyObject[] args, PyDict kwargs) {
            if (args.length != 0) {
                throw new RuntimeException("dict.keys() takes no arguments");
            }
            return self.pymethod_keys();
        }
    }
    private static final class PyDictMethod_values extends PyDictMethod {
        PyDictMethod_values(PyDict _self) { super(_self); }
        @Override public PyDictValues call(PyObject[] args, PyDict kwargs) {
            if (args.length != 0) {
                throw new RuntimeException("dict.values() takes no arguments");
            }
            return self.pymethod_values();
        }
    }

    public final LinkedHashMap<PyObject, PyObject> items;

    PyDict(PyObject... args) {
        items = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            PyObject k = args[i];
            PyObject v = args[i+1];
            if (k == null) { // used to encode dictionary unpacking -- XXX support arbitrary mappings here
                for (var x: ((PyDict)v).items.entrySet()) {
                    items.put(x.getKey(), x.getValue());
                }
            } else {
                items.put(k, v);
            }
        }
    }

    @Override public PyDictIter iter() { return new PyDictIter(items.keySet().iterator()); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_dict; }

    @Override public boolean boolValue() { return !items.isEmpty(); }
    @Override public boolean contains(PyObject rhs) { return items.containsKey(rhs); }
    @Override public boolean equals(Object rhs) {
        if (!(rhs instanceof PyDict)) {
            return false;
        }
        return items.equals(((PyDict)rhs).items);
    }
    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "get": return new PyDictMethod_get(this);
            case "keys": return new PyDictMethod_keys(this);
            case "items": return new PyDictMethod_items(this);
            case "values": return new PyDictMethod_values(this);
            default: return super.getAttr(key);
        }
    }
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

    @Override public PyObject getItem(PyObject key) {
        PyObject value = items.get(key);
        if (value == null) {
            throw new RuntimeException("cannot find key in dict");
        }
        return value;
    }
    @Override public void setItem(PyObject key, PyObject value) { items.put(key, value); }
    @Override public void delItem(PyObject key) {
        PyObject value = items.remove(key);
        if (value == null) {
            throw new RuntimeException("cannot find key in dict");
        }
    }

    public PyObject pymethod_get(PyObject arg0, PyObject arg1) {
        PyObject value = items.get(arg0);
        if (value == null) {
            return arg1;
        }
        return value;
    }
    public PyDictItems pymethod_items() { return new PyDictItems(items); }
    public PyDictKeys pymethod_keys() { return new PyDictKeys(items); }
    public PyDictValues pymethod_values() { return new PyDictValues(items); }
}
