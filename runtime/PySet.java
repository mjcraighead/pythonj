// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

final class PySetIter extends PyIter {
    private static final PyConcreteType type_singleton = new PyConcreteType("set_iterator", PySetIter.class, PyObjectType.singleton, null);

    private final Iterator<PyObject> it;

    PySetIter(Iterator<PyObject> _it) { it = _it; }

    @Override public PyObject next() { return it.hasNext() ? it.next() : null; }
    @Override public String repr() { return defaultRepr(); }
    @Override public PyConcreteType type() { return type_singleton; }
};

public final class PySet extends PyObject {
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

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireMinMaxPositional(args, kwargs, type.name(), 0, 1);
        return newObjPositional((args.length == 0) ? null : args[0]);
    }
    public static PySet newObjPositional(PyObject arg) {
        var ret = new PySet();
        if (arg == null) {
            return ret;
        }
        Runtime.addIterableToCollection(ret.items, arg);
        return ret;
    }

    static HashSet<PyObject> materializeIterable(PyObject obj) {
        var result = new HashSet<PyObject>();
        Runtime.addIterableToCollection(result, obj);
        return result;
    }
    static HashSet<PyObject> intersectionPreserveLeft(Iterable<PyObject> lhs, Set<PyObject> rhs) {
        var result = new HashSet<PyObject>();
        for (var x: lhs) {
            if (rhs.contains(x)) {
                result.add(x);
            }
        }
        return result;
    }
    static HashSet<PyObject> unionPreserveLeft(Iterable<PyObject> lhs, Iterable<PyObject> rhs) {
        var result = new HashSet<PyObject>();
        for (var x: lhs) {
            result.add(x);
        }
        for (var x: rhs) {
            result.add(x);
        }
        return result;
    }
    static HashSet<PyObject> differencePreserveLeft(Iterable<PyObject> lhs, Set<PyObject> rhs) {
        var result = new HashSet<PyObject>();
        for (var x: lhs) {
            if (!rhs.contains(x)) {
                result.add(x);
            }
        }
        return result;
    }
    static HashSet<PyObject> symmetricDifferencePreserveLeft(Iterable<PyObject> lhs, Set<PyObject> lhsSet, Iterable<PyObject> rhs, Set<PyObject> rhsSet) {
        var result = differencePreserveLeft(lhs, rhsSet);
        for (var x: rhs) {
            if (!lhsSet.contains(x)) {
                result.add(x);
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
        var rhsSet = rhs.asSetOrNull();
        if (rhsSet != null) {
            if (items.size() < rhsSet.size()) {
                return new PySet(intersectionPreserveLeft(items, rhsSet));
            } else {
                return new PySet(intersectionPreserveLeft(rhsSet, items));
            }
        } else {
            return super.and(rhs);
        }
    }
    @Override public PyObject andInPlace(PyObject rhs) {
        var rhsSet = rhs.asSetOrNull();
        if (rhsSet != null) {
            var lhs = new HashSet<PyObject>(items);
            items.clear();
            if (lhs.size() < rhsSet.size()) {
                items.addAll(intersectionPreserveLeft(lhs, rhsSet));
            } else {
                items.addAll(intersectionPreserveLeft(rhsSet, lhs));
            }
            return this;
        } else {
            return super.andInPlace(rhs);
        }
    }
    @Override public PyObject or(PyObject rhs) {
        if (rhs instanceof PySet rhsSet) {
            return new PySet(unionPreserveLeft(items, rhsSet.items));
        } else {
            return super.or(rhs);
        }
    }
    @Override public PyObject orInPlace(PyObject rhs) {
        if (rhs instanceof PySet rhsSet) {
            var lhs = new HashSet<PyObject>(items);
            items.clear();
            items.addAll(unionPreserveLeft(lhs, rhsSet.items));
            return this;
        } else {
            return super.orInPlace(rhs);
        }
    }
    @Override public PyObject sub(PyObject rhs) {
        if (rhs instanceof PySet rhsSet) {
            return new PySet(differencePreserveLeft(items, rhsSet.items));
        } else {
            return super.sub(rhs);
        }
    }
    @Override public PyObject subInPlace(PyObject rhs) {
        if (rhs instanceof PySet rhsSet) {
            var lhs = new HashSet<PyObject>(items);
            items.clear();
            items.addAll(differencePreserveLeft(lhs, rhsSet.items));
            return this;
        } else {
            return super.subInPlace(rhs);
        }
    }
    @Override public PyObject xor(PyObject rhs) {
        if (rhs instanceof PySet rhsSet) {
            return new PySet(symmetricDifferencePreserveLeft(items, items, rhsSet.items, rhsSet.items));
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
            var lhs = new HashSet<PyObject>(items);
            items.clear();
            items.addAll(symmetricDifferencePreserveLeft(lhs, lhs, rhsSet.items, rhsSet.items));
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
    @Override public PyConcreteType type() { return PySetType.singleton; }

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
    @Override public String repr() { return PyRuntime.pyfunc_set____repr__(this).value; }

    public PyNone pymethod_add(PyObject arg) {
        items.add(arg);
        return PyNone.singleton;
    }
    public PyNone pymethod_clear() {
        items.clear();
        return PyNone.singleton;
    }
    public PySet pymethod_copy() {
        return new PySet(new HashSet<>(items));
    }
    public PyNone pymethod_discard(PyObject arg) {
        items.remove(arg);
        return PyNone.singleton;
    }
    public PyObject pymethod_pop() {
        var it = items.iterator();
        if (!it.hasNext()) {
            throw PyKeyError.raise("pop from an empty set");
        }
        PyObject ret = it.next();
        it.remove();
        return ret;
    }
    public PyNone pymethod_remove(PyObject elem) {
        if (!items.remove(elem)) {
            throw new PyRaise(new PyKeyError(elem));
        }
        return PyNone.singleton;
    }
    public PyNone pymethod_update(PyObject[] args) {
        for (var arg: args) {
            Runtime.addIterableToCollection(items, arg);
        }
        return PyNone.singleton;
    }

    public PyObject pymethod_difference(PyObject[] others) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_difference_update(PyObject[] others) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_intersection(PyObject[] others) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_intersection_update(PyObject[] others) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isdisjoint(PyObject other) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_issubset(PyObject other) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_issuperset(PyObject other) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_symmetric_difference(PyObject other) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_symmetric_difference_update(PyObject other) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_union(PyObject[] others) { throw new UnsupportedOperationException(); }
}
