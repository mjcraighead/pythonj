// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Map;

public final class PyMappingProxy extends PyObject {
    private final Map<PyObject, PyObject> items;

    PyMappingProxy(Map<PyObject, PyObject> _items) {
        items = _items;
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        if (args.length != 1) {
            throw PyTypeError.raise("mappingproxy() takes 1 argument");
        }
        PyObject arg = args[0];
        if (arg instanceof PyDict dict) {
            return new PyMappingProxy(dict.items);
        } else if (arg instanceof PyMappingProxy proxy) {
            return new PyMappingProxy(proxy.items);
        } else {
            throw PyTypeError.raise("mappingproxy() argument must be a mapping");
        }
    }

    @Override public PyObject getItem(PyObject key) {
        PyObject ret = items.get(key);
        if (ret == null) {
            throw new PyRaise(new PyKeyError(key));
        }
        return ret;
    }
    @Override public void delItem(PyObject key) {
        throw PyTypeError.raise("'mappingproxy' object doesn't support item deletion");
    }

    @Override public boolean contains(PyObject rhs) {
        return items.containsKey(rhs);
    }
    @Override public boolean boolValue() { return !items.isEmpty(); }
    @Override public boolean hasIter() { return true; }
    @Override public PyIter iter() { return new PyDict.PyDictIter(items.keySet().iterator()); }
    @Override public long len() { return items.size(); }
    @Override public int hashCode() { throw raiseUnhashable(); }
    @Override public boolean equals(Object rhs) { return this == rhs; }
    @Override public String repr() {
        var s = new StringBuilder("mappingproxy({");
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
        return s.append("})").toString();
    }
    @Override public PyConcreteType type() { return PyMappingProxyType.singleton; }

    public PyDict pymethod_copy() {
        PyDict ret = new PyDict();
        ret.items.putAll(items);
        return ret;
    }
    public PyObject pymethod_get(PyObject key, PyObject defaultValue) {
        PyObject ret = items.get(key);
        return (ret != null) ? ret : defaultValue;
    }
    public PyDict.PyDictItems pymethod_items() { return new PyDict.PyDictItems(items); }
    public PyDict.PyDictKeys pymethod_keys() { return new PyDict.PyDictKeys(items); }
    public PyDict.PyDictValues pymethod_values() { return new PyDict.PyDictValues(items); }
}
