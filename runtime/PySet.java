// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class PySet extends PyObject {
    private static final PyBuiltinClass iter_class_singleton = new PyBuiltinClass("set_iterator", PySetIter.class);
    static final class PySetIter extends PyIter {
        private final Iterator<PyObject> it;

        PySetIter(Iterator<PyObject> _it) { it = _it; }

        @Override public PyObject next() { return it.hasNext() ? it.next() : null; }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyBuiltinClass type() { return iter_class_singleton; }
    };

    private static final class PySetMethod_add extends PyBuiltinMethod<PySet> {
        PySetMethod_add(PySet _self) { super(_self); }
        @Override public String methodName() { return "add"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "set.add");
            Runtime.requireExactArgsAlt(args, 1, "set.add");
            return self.pymethod_add(args[0]);
        }
    }
    private static final class PySetMethod_clear extends PyBuiltinMethod<PySet> {
        PySetMethod_clear(PySet _self) { super(_self); }
        @Override public String methodName() { return "clear"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "set.clear");
            Runtime.requireExactArgsAlt(args, 0, "set.clear");
            return self.pymethod_clear();
        }
    }
    private static final class PySetMethod_discard extends PyBuiltinMethod<PySet> {
        PySetMethod_discard(PySet _self) { super(_self); }
        @Override public String methodName() { return "discard"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "set.discard");
            Runtime.requireExactArgsAlt(args, 1, "set.discard");
            return self.pymethod_discard(args[0]);
        }
    }
    private static final class PySetMethod_update extends PyBuiltinMethod<PySet> {
        PySetMethod_update(PySet _self) { super(_self); }
        @Override public String methodName() { return "update"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "set.update");
            return self.pymethod_update(args);
        }
    }

    public final HashSet<PyObject> items;

    PySet() {
        items = new HashSet<>();
    }
    PySet(PyObject[] args) {
        items = new HashSet<>();
        for (var x: args) {
            items.add(x);
        }
    }
    PySet(ArrayList<PyObject> list) {
        items = new HashSet<>(list);
    }
    PySet(HashSet<PyObject> _items) {
        items = _items; // WARNING: takes ownership of _items from caller, does not copy
    }

    static HashSet<PyObject> xor(Set<PyObject> lhs, Set<PyObject> rhs) {
        var result = new HashSet<PyObject>(lhs);
        for (var x: rhs) {
            if (!result.add(x)) {
                result.remove(x);
            }
        }
        return result;
    }
    static boolean ge(Set<PyObject> lhs, Set<PyObject> rhs) {
        return lhs.containsAll(rhs);
    }
    // XXX This and lt() has some fragility with mutable sets and non-mathematical sets with size() check
    static boolean gt(Set<PyObject> lhs, Set<PyObject> rhs) {
        return (lhs.size() > rhs.size()) && lhs.containsAll(rhs);
    }
    static boolean le(Set<PyObject> lhs, Set<PyObject> rhs) {
        return rhs.containsAll(lhs);
    }
    static boolean lt(Set<PyObject> lhs, Set<PyObject> rhs) {
        return (rhs.size() > lhs.size()) && rhs.containsAll(lhs);
    }

    @Override public PyObject and(PyObject rhs) {
        if (rhs instanceof PySet rhsSet) {
            var result = new HashSet<PyObject>(items);
            result.retainAll(rhsSet.items);
            return new PySet(result);
        } else {
            return super.and(rhs);
        }
    }
    @Override public PyObject andInPlace(PyObject rhs) {
        if (rhs instanceof PySet rhsSet) {
            items.retainAll(rhsSet.items);
            return this;
        } else {
            return super.andInPlace(rhs);
        }
    }
    @Override public PyObject or(PyObject rhs) {
        if (rhs instanceof PySet rhsSet) {
            var result = new HashSet<PyObject>(items);
            result.addAll(rhsSet.items);
            return new PySet(result);
        } else {
            return super.or(rhs);
        }
    }
    @Override public PyObject orInPlace(PyObject rhs) {
        if (rhs instanceof PySet rhsSet) {
            items.addAll(rhsSet.items);
            return this;
        } else {
            return super.orInPlace(rhs);
        }
    }
    @Override public PyObject sub(PyObject rhs) {
        if (rhs instanceof PySet rhsSet) {
            var result = new HashSet<PyObject>(items);
            result.removeAll(rhsSet.items);
            return new PySet(result);
        } else {
            return super.sub(rhs);
        }
    }
    @Override public PyObject subInPlace(PyObject rhs) {
        if (rhs instanceof PySet rhsSet) {
            items.removeAll(rhsSet.items);
            return this;
        } else {
            return super.subInPlace(rhs);
        }
    }
    @Override public PyObject xor(PyObject rhs) {
        if (rhs instanceof PySet rhsSet) {
            return new PySet(xor(items, rhsSet.items));
        } else {
            return super.xor(rhs);
        }
    }
    @Override public PyObject xorInPlace(PyObject rhs) {
        if (rhs instanceof PySet rhsSet) {
            if (rhsSet == this) {
                items.clear();
                return this;
            }
            for (var x: rhsSet.items) {
                if (!items.add(x)) {
                    items.remove(x);
                }
            }
            return this;
        } else {
            return super.xorInPlace(rhs);
        }
    }

    @Override public boolean ge(PyObject rhs) {
        var rhsSet = rhs.asSetOrNull();
        if (rhsSet != null) {
            return ge(items, rhsSet);
        } else {
            return super.ge(rhs);
        }
    }
    @Override public boolean gt(PyObject rhs) {
        var rhsSet = rhs.asSetOrNull();
        if (rhsSet != null) {
            return gt(items, rhsSet);
        } else {
            return super.gt(rhs);
        }
    }
    @Override public boolean le(PyObject rhs) {
        var rhsSet = rhs.asSetOrNull();
        if (rhsSet != null) {
            return le(items, rhsSet);
        } else {
            return super.le(rhs);
        }
    }
    @Override public boolean lt(PyObject rhs) {
        var rhsSet = rhs.asSetOrNull();
        if (rhsSet != null) {
            return lt(items, rhsSet);
        } else {
            return super.lt(rhs);
        }
    }

    @Override public void delItem(PyObject key) {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object doesn't support item deletion");
    }

    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "add": return new PySetMethod_add(this);
            case "clear": return new PySetMethod_clear(this);
            case "copy": throw unimplementedAttr(key);
            case "difference": throw unimplementedAttr(key);
            case "difference_update": throw unimplementedAttr(key);
            case "discard": return new PySetMethod_discard(this);
            case "intersection": throw unimplementedAttr(key);
            case "intersection_update": throw unimplementedAttr(key);
            case "isdisjoint": throw unimplementedAttr(key);
            case "issubset": throw unimplementedAttr(key);
            case "issuperset": throw unimplementedAttr(key);
            case "pop": throw unimplementedAttr(key);
            case "remove": throw unimplementedAttr(key);
            case "symmetric_difference": throw unimplementedAttr(key);
            case "symmetric_difference_update": throw unimplementedAttr(key);
            case "union": throw unimplementedAttr(key);
            case "update": return new PySetMethod_update(this);
            default:
                if (key.startsWith("__")) {
                    return super.getAttr(key);
                } else {
                    throw raiseMissingAttr(key);
                }
        }
    }
    @Override public void setAttr(String key, PyObject value) {
        switch (key) {
            case "add": throw Runtime.raiseNamedReadOnlyAttr(this, key);
            case "clear": throw Runtime.raiseNamedReadOnlyAttr(this, key);
            case "discard": throw Runtime.raiseNamedReadOnlyAttr(this, key);
            case "update": throw Runtime.raiseNamedReadOnlyAttr(this, key);
            default: super.setAttr(key, value); break;
        }
    }
    @Override public void delAttr(String key) {
        switch (key) {
            case "add": throw Runtime.raiseNamedReadOnlyAttr(this, key);
            case "clear": throw Runtime.raiseNamedReadOnlyAttr(this, key);
            case "discard": throw Runtime.raiseNamedReadOnlyAttr(this, key);
            case "update": throw Runtime.raiseNamedReadOnlyAttr(this, key);
            default: super.delAttr(key); break;
        }
    }

    @Override public final boolean hasIter() { return true; }
    @Override public PySetIter iter() { return new PySetIter(items.iterator()); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_set; }

    @Override public Set<PyObject> asSetOrNull() { return items; }
    @Override public boolean boolValue() { return !items.isEmpty(); }
    @Override public boolean contains(PyObject rhs) { return items.contains(rhs); }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PySet rhsSet) {
            return items.equals(rhsSet.items);
        }
        return false;
    }
    @Override public int hashCode() { throw raiseUnhashable(); }
    @Override public long len() { return items.size(); }
    @Override public String repr() {
        if (items.isEmpty()) {
            return "set()";
        }
        var s = new StringBuilder("{");
        boolean first = true;
        for (var x: items) {
            if (!first) {
                s.append(", ");
            }
            first = false;
            s.append(x.repr());
        }
        return s + "}";
    }

    public PyNone pymethod_add(PyObject arg) {
        items.add(arg);
        return PyNone.singleton;
    }
    public PyNone pymethod_clear() {
        items.clear();
        return PyNone.singleton;
    }
    public PyNone pymethod_discard(PyObject arg) {
        items.remove(arg);
        return PyNone.singleton;
    }
    public PyNone pymethod_update(PyObject[] args) {
        for (var arg: args) {
            Runtime.addIterableToCollection(items, arg);
        }
        return PyNone.singleton;
    }
}
