// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

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
            if (args.length != 1) {
                throw new IllegalArgumentException("set.update() takes 1 argument");
            }
            return self.pymethod_update(args[0]);
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

    @Override public final boolean hasIter() { return true; }
    @Override public PySetIter iter() { return new PySetIter(items.iterator()); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_set; }

    @Override public boolean boolValue() { return !items.isEmpty(); }
    @Override public boolean contains(PyObject rhs) { return items.contains(rhs); }
    @Override public boolean equals(Object rhsArg) {
        if (rhsArg instanceof PySet rhs) {
            return items.equals(rhs.items);
        }
        return false;
    }
    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "add": return new PySetMethod_add(this);
            case "clear": return new PySetMethod_clear(this);
            case "discard": return new PySetMethod_discard(this);
            case "update": return new PySetMethod_update(this);
            default: return super.getAttr(key);
        }
    }
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
    public PyNone pymethod_update(PyObject arg) {
        var iter = arg.iter();
        for (var item = iter.next(); item != null; item = iter.next()) {
            items.add(item);
        }
        return PyNone.singleton;
    }
}
