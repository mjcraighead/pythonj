// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;

public final class PyList extends PyObject {
    private static final PyBuiltinClass iter_class_singleton = new PyBuiltinClass("list_iterator");
    static final class PyListIter extends PyIter {
        private final Iterator<PyObject> it;

        PyListIter(Iterator<PyObject> _it) { it = _it; }

        @Override public PyObject next() { return it.hasNext() ? it.next() : null; }
        @Override public PyBuiltinClass type() { return iter_class_singleton; }
    };

    private static class PyListMethod extends PyTruthyObject {
        protected final PyList self;
        PyListMethod(PyList _self) { self = _self; }
    }
    private static final class PyListMethod_append extends PyListMethod {
        PyListMethod_append(PyList _self) { super(_self); }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new RuntimeException("list.append() takes 1 argument");
            }
            return self.pymethod_append(args[0]);
        }
    }
    private static final class PyListMethod_count extends PyListMethod {
        PyListMethod_count(PyList _self) { super(_self); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new RuntimeException("list.count() takes 1 argument");
            }
            return self.pymethod_count(args[0]);
        }
    }
    private static final class PyListMethod_extend extends PyListMethod {
        PyListMethod_extend(PyList _self) { super(_self); }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new RuntimeException("list.extend() takes 1 argument");
            }
            return self.pymethod_extend(args[0]);
        }
    }

    public final ArrayList<PyObject> items;

    PyList() {
        items = new ArrayList<>();
    }
    PyList(PyObject[] args) {
        items = new ArrayList<>(Arrays.asList(args));
    }
    PyList(ArrayList<PyObject> _items) {
        items = _items; // WARNING: takes ownership of _items from caller, does not copy
    }

    @Override public PyList add(PyObject rhs_arg) {
        if (!(rhs_arg instanceof PyList)) {
            throw new RuntimeException("list add error");
        }
        var ret = new PyList();
        ret.items.addAll(items);
        ret.items.addAll(((PyList)rhs_arg).items);
        return ret;
    }
    @Override public PyList mul(PyObject rhs) {
        var ret = new PyList();
        long count = rhs.indexValue();
        if (count <= 0) {
            return ret;
        }
        for (long i = 0; i < count; i++) {
            ret.items.addAll(items);
        }
        return ret;
    }

    @Override public PyObject getItem(PyObject key) {
        int index = (int)key.indexValue();
        if (index < 0) {
            index += items.size();
        }
        return items.get(index);
    }
    @Override public void setItem(PyObject key, PyObject value) {
        int index = (int)key.indexValue();
        if (index < 0) {
            index += items.size();
        }
        items.set(index, value);
    }

    @Override public PyListIter iter() { return new PyListIter(items.iterator()); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_list; }

    @Override public boolean boolValue() { return !items.isEmpty(); }
    @Override public boolean contains(PyObject rhs) {
        for (var x: items) {
            if (x.equals(rhs)) {
                return true;
            }
        }
        return false;
    }
    @Override public boolean equals(Object rhs) {
        if (!(rhs instanceof PyList)) {
            return false;
        }
        return items.equals(((PyList)rhs).items);
    }
    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "append": return new PyListMethod_append(this);
            case "count": return new PyListMethod_count(this);
            case "extend": return new PyListMethod_extend(this);
            default: return super.getAttr(key);
        }
    }
    @Override public long len() { return items.size(); }
    @Override public String repr() {
        var s = new StringBuilder("[");
        boolean first = true;
        for (var x: items) {
            if (!first) {
                s.append(", ");
            }
            first = false;
            s.append(x.repr());
        }
        return s + "]";
    }

    public PyNone pymethod_append(PyObject arg) {
        items.add(arg);
        return PyNone.singleton;
    }
    public PyInt pymethod_count(PyObject arg) {
        long n = 0;
        for (var x: items) {
            if (x.equals(arg)) {
                n++;
            }
        }
        return new PyInt(n);
    }
    public PyNone pymethod_extend(PyObject arg) {
        var iter = arg.iter();
        for (var item = iter.next(); item != null; item = iter.next()) {
            items.add(item);
        }
        return PyNone.singleton;
    }
}
