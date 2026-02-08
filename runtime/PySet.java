// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public final class PySet extends PyObject {
    private static final PyBuiltinClass iter_class_singleton = new PyBuiltinClass("set_iterator");
    static final class PySetIter extends PyIter {
        private final Iterator<PyObject> it;

        PySetIter(Iterator<PyObject> _it) { it = _it; }

        @Override public PyObject next() { return it.hasNext() ? it.next() : null; }
        @Override public PyBuiltinClass type() { return iter_class_singleton; }
    };

    private static class PySetMethod extends PyBuiltinFunctionOrMethod {
        protected final PySet self;
        PySetMethod(PySet _self) { self = _self; }
        @Override public String repr() { throw new UnsupportedOperationException("'repr' unimplemented"); }
    }
    private static final class PySetMethod_add extends PySetMethod {
        PySetMethod_add(PySet _self) { super(_self); }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("set.add() takes 1 argument");
            }
            if (kwargs != null) {
                throw new IllegalArgumentException("set.add() does not accept kwargs");
            }
            return self.pymethod_add(args[0]);
        }
    }
    private static final class PySetMethod_discard extends PySetMethod {
        PySetMethod_discard(PySet _self) { super(_self); }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("set.discard() takes 1 argument");
            }
            if (kwargs != null) {
                throw new IllegalArgumentException("set.discard() does not accept kwargs");
            }
            return self.pymethod_discard(args[0]);
        }
    }
    private static final class PySetMethod_update extends PySetMethod {
        PySetMethod_update(PySet _self) { super(_self); }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("set.update() takes 1 argument");
            }
            if (kwargs != null) {
                throw new IllegalArgumentException("set.update() does not accept kwargs");
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

    @Override public PySetIter iter() { return new PySetIter(items.iterator()); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_set; }

    @Override public boolean boolValue() { return !items.isEmpty(); }
    @Override public boolean contains(PyObject rhs) { return items.contains(rhs); }
    @Override public boolean equals(Object rhs_arg) {
        if (rhs_arg instanceof PySet rhs) {
            return items.equals(rhs.items);
        }
        return false;
    }
    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "add": return new PySetMethod_add(this);
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
