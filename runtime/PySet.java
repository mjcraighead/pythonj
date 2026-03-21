// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

// BEGIN GENERATED CODE: PySetType
final class PySetType extends PyBuiltinType {
    public static final PySetType singleton = new PySetType();
    private static final PyMethodDescriptor pyattr_add = new PyMethodDescriptor(singleton, "add", PySet.PySetMethod_add::new);
    private static final PyMethodDescriptor pyattr_clear = new PyMethodDescriptor(singleton, "clear", PySet.PySetMethod_clear::new);
    private static final PyMethodDescriptor pyattr_copy = new PyMethodDescriptor(singleton, "copy", obj -> new PySet.PySetMethodUnimplemented(obj, "copy"));
    private static final PyMethodDescriptor pyattr_discard = new PyMethodDescriptor(singleton, "discard", PySet.PySetMethod_discard::new);
    private static final PyMethodDescriptor pyattr_difference = new PyMethodDescriptor(singleton, "difference", obj -> new PySet.PySetMethodUnimplemented(obj, "difference"));
    private static final PyMethodDescriptor pyattr_difference_update = new PyMethodDescriptor(singleton, "difference_update", obj -> new PySet.PySetMethodUnimplemented(obj, "difference_update"));
    private static final PyMethodDescriptor pyattr_intersection = new PyMethodDescriptor(singleton, "intersection", obj -> new PySet.PySetMethodUnimplemented(obj, "intersection"));
    private static final PyMethodDescriptor pyattr_intersection_update = new PyMethodDescriptor(singleton, "intersection_update", obj -> new PySet.PySetMethodUnimplemented(obj, "intersection_update"));
    private static final PyMethodDescriptor pyattr_isdisjoint = new PyMethodDescriptor(singleton, "isdisjoint", obj -> new PySet.PySetMethodUnimplemented(obj, "isdisjoint"));
    private static final PyMethodDescriptor pyattr_issubset = new PyMethodDescriptor(singleton, "issubset", obj -> new PySet.PySetMethodUnimplemented(obj, "issubset"));
    private static final PyMethodDescriptor pyattr_issuperset = new PyMethodDescriptor(singleton, "issuperset", obj -> new PySet.PySetMethodUnimplemented(obj, "issuperset"));
    private static final PyMethodDescriptor pyattr_pop = new PyMethodDescriptor(singleton, "pop", obj -> new PySet.PySetMethodUnimplemented(obj, "pop"));
    private static final PyMethodDescriptor pyattr_remove = new PyMethodDescriptor(singleton, "remove", obj -> new PySet.PySetMethodUnimplemented(obj, "remove"));
    private static final PyMethodDescriptor pyattr_symmetric_difference = new PyMethodDescriptor(singleton, "symmetric_difference", obj -> new PySet.PySetMethodUnimplemented(obj, "symmetric_difference"));
    private static final PyMethodDescriptor pyattr_symmetric_difference_update = new PyMethodDescriptor(singleton, "symmetric_difference_update", obj -> new PySet.PySetMethodUnimplemented(obj, "symmetric_difference_update"));
    private static final PyMethodDescriptor pyattr_union = new PyMethodDescriptor(singleton, "union", obj -> new PySet.PySetMethodUnimplemented(obj, "union"));
    private static final PyMethodDescriptor pyattr_update = new PyMethodDescriptor(singleton, "update", PySet.PySetMethod_update::new);
    private static final PyString pyattr___doc__ = new PyString("Build an unordered collection of unique elements.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(18);
    static {
        attrs.put(new PyString("add"), pyattr_add);
        attrs.put(new PyString("clear"), pyattr_clear);
        attrs.put(new PyString("copy"), pyattr_copy);
        attrs.put(new PyString("discard"), pyattr_discard);
        attrs.put(new PyString("difference"), pyattr_difference);
        attrs.put(new PyString("difference_update"), pyattr_difference_update);
        attrs.put(new PyString("intersection"), pyattr_intersection);
        attrs.put(new PyString("intersection_update"), pyattr_intersection_update);
        attrs.put(new PyString("isdisjoint"), pyattr_isdisjoint);
        attrs.put(new PyString("issubset"), pyattr_issubset);
        attrs.put(new PyString("issuperset"), pyattr_issuperset);
        attrs.put(new PyString("pop"), pyattr_pop);
        attrs.put(new PyString("remove"), pyattr_remove);
        attrs.put(new PyString("symmetric_difference"), pyattr_symmetric_difference);
        attrs.put(new PyString("symmetric_difference_update"), pyattr_symmetric_difference_update);
        attrs.put(new PyString("union"), pyattr_union);
        attrs.put(new PyString("update"), pyattr_update);
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PySetType() { super("set", PySet.class, PySet::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "add": return pyattr_add;
            case "clear": return pyattr_clear;
            case "copy": return pyattr_copy;
            case "discard": return pyattr_discard;
            case "difference": return pyattr_difference;
            case "difference_update": return pyattr_difference_update;
            case "intersection": return pyattr_intersection;
            case "intersection_update": return pyattr_intersection_update;
            case "isdisjoint": return pyattr_isdisjoint;
            case "issubset": return pyattr_issubset;
            case "issuperset": return pyattr_issuperset;
            case "pop": return pyattr_pop;
            case "remove": return pyattr_remove;
            case "symmetric_difference": return pyattr_symmetric_difference;
            case "symmetric_difference_update": return pyattr_symmetric_difference_update;
            case "union": return pyattr_union;
            case "update": return pyattr_update;
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PySetType

public final class PySet extends PyObject {
    static final class PySetIter extends PyIter {
        private static final PyBuiltinType type_singleton = new PyBuiltinType("set_iterator", PySetIter.class);

        private final Iterator<PyObject> it;

        PySetIter(Iterator<PyObject> _it) { it = _it; }

        @Override public PyObject next() { return it.hasNext() ? it.next() : null; }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyBuiltinType type() { return type_singleton; }
    };

// BEGIN GENERATED CODE: PySet
    protected static final class PySetMethodUnimplemented extends PyBuiltinMethod<PySet> {
        private final String name;
        PySetMethodUnimplemented(PyObject _self, String _name) { super((PySet)_self); name = _name; }
        @Override public String methodName() { return name; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("set." + name + "() unimplemented");
        }
    }
// END GENERATED CODE: PySet
    protected static final class PySetMethod_add extends PyBuiltinMethod<PySet> {
        PySetMethod_add(PyObject _self) { super((PySet)_self); }
        @Override public String methodName() { return "add"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "set.add");
            Runtime.requireExactArgsAlt(args, 1, "set.add");
            return self.pymethod_add(args[0]);
        }
    }
    protected static final class PySetMethod_clear extends PyBuiltinMethod<PySet> {
        PySetMethod_clear(PyObject _self) { super((PySet)_self); }
        @Override public String methodName() { return "clear"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "set.clear");
            Runtime.requireExactArgsAlt(args, 0, "set.clear");
            return self.pymethod_clear();
        }
    }
    protected static final class PySetMethod_discard extends PyBuiltinMethod<PySet> {
        PySetMethod_discard(PyObject _self) { super((PySet)_self); }
        @Override public String methodName() { return "discard"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "set.discard");
            Runtime.requireExactArgsAlt(args, 1, "set.discard");
            return self.pymethod_discard(args[0]);
        }
    }
    protected static final class PySetMethod_update extends PyBuiltinMethod<PySet> {
        PySetMethod_update(PyObject _self) { super((PySet)_self); }
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
}
