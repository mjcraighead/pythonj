// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class PySet extends PyObject {
    static final class PySetIter extends PyIter {
        private static final PyBuiltinType type_singleton = new PyBuiltinType("set_iterator", PySetIter.class);

        private final Iterator<PyObject> it;

        PySetIter(Iterator<PyObject> _it) { it = _it; }

        @Override public PyObject next() { return it.hasNext() ? it.next() : null; }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyBuiltinType type() { return type_singleton; }
    };

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

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        Runtime.requireMaxArgs(args, 1, type.name());
        var ret = new PySet();
        if (args.length == 0) {
            return ret;
        }
        Runtime.addIterableToCollection(ret.items, args[0]);
        return ret;
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

    @Override public final boolean hasIter() { return true; }
    @Override public PySetIter iter() { return new PySetIter(items.iterator()); }
    @Override public PyBuiltinType type() { return PySetType.singleton; }

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

    public PyObject pymethod_copy() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_difference(PyObject[] others) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_difference_update(PyObject[] others) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_intersection(PyObject[] others) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_intersection_update(PyObject[] others) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isdisjoint(PyObject other) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_issubset(PyObject other) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_issuperset(PyObject other) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_pop() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_remove(PyObject elem) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_symmetric_difference(PyObject other) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_symmetric_difference_update(PyObject other) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_union(PyObject[] others) { throw new UnsupportedOperationException(); }
}
