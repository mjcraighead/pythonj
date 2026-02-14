// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class PyList extends PyObject {
    private static final PyBuiltinClass iter_class_singleton = new PyBuiltinClass("list_iterator", PyListIter.class);
    static final class PyListIter extends PyIter {
        private final Iterator<PyObject> it;

        PyListIter(Iterator<PyObject> _it) { it = _it; }

        @Override public PyObject next() { return it.hasNext() ? it.next() : null; }
        @Override public PyBuiltinClass type() { return iter_class_singleton; }
    };

    private static class PyListMethod extends PyBuiltinFunctionOrMethod {
        protected final PyList self;
        PyListMethod(PyList _self) { self = _self; }
        @Override public String repr() { throw new UnsupportedOperationException("'repr' unimplemented"); }
    }
    private static final class PyListMethod_append extends PyListMethod {
        PyListMethod_append(PyList _self) { super(_self); }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.append");
            if (args.length != 1) {
                throw new IllegalArgumentException("list.append() takes 1 argument");
            }
            return self.pymethod_append(args[0]);
        }
    }
    private static final class PyListMethod_clear extends PyListMethod {
        PyListMethod_clear(PyList _self) { super(_self); }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.clear");
            if (args.length != 0) {
                throw new IllegalArgumentException("list.clear() takes 0 arguments");
            }
            return self.pymethod_clear();
        }
    }
    private static final class PyListMethod_count extends PyListMethod {
        PyListMethod_count(PyList _self) { super(_self); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.count");
            if (args.length != 1) {
                throw new IllegalArgumentException("list.count() takes 1 argument");
            }
            return self.pymethod_count(args[0]);
        }
    }
    private static final class PyListMethod_extend extends PyListMethod {
        PyListMethod_extend(PyList _self) { super(_self); }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.extend");
            if (args.length != 1) {
                throw new IllegalArgumentException("list.extend() takes 1 argument");
            }
            return self.pymethod_extend(args[0]);
        }
    }
    private static final class PyListMethod_index extends PyListMethod {
        PyListMethod_index(PyList _self) { super(_self); }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.index");
            if (args.length != 1) {
                throw new IllegalArgumentException("list.index() takes 1 argument");
            }
            return self.pymethod_index(args[0]);
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
        if (rhs_arg instanceof PyList rhs) {
            var ret = new PyList();
            ret.items.addAll(items);
            ret.items.addAll(rhs.items);
            return ret;
        }
        throw new UnsupportedOperationException("unsupported operand type for list addition");
    }
    @Override public PyList addInPlace(PyObject rhs_arg) {
        if (rhs_arg instanceof PyList rhs) {
            items.addAll(rhs.items);
            return this;
        }
        throw new UnsupportedOperationException("unsupported operand type for list addition");
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
    @Override public PyList mulInPlace(PyObject rhs) {
        long count = rhs.indexValue();
        if (count <= 0) {
            items.clear();
            return this;
        }
        var original = new ArrayList<>(items);
        for (long i = 1; i < count; i++) {
            items.addAll(original);
        }
        return this;
    }

    @Override public PyObject getItem(PyObject key) {
        int index = Math.toIntExact(key.indexValue());
        if (index < 0) {
            index += items.size();
        }
        return items.get(index);
    }
    @Override public void setItem(PyObject key, PyObject value) {
        int index = Math.toIntExact(key.indexValue());
        if (index < 0) {
            index += items.size();
        }
        items.set(index, value);
    }

    // NOTE: CPython returns a specialized list_reverseiterator.
    // pythonj intentionally uses the generic reversed iterator, at least for now.
    @Override public PyListIter iter() { return new PyListIter(items.iterator()); }
    @Override public PyReversed reversed() { return new PyReversed(this); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_list; }

    @Override public boolean boolValue() { return !items.isEmpty(); }
    @Override public boolean contains(PyObject rhs) { return items.contains(rhs); }
    @Override public boolean equals(Object rhs_arg) {
        if (rhs_arg instanceof PyList rhs) {
            return items.equals(rhs.items);
        }
        return false;
    }
    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "append": return new PyListMethod_append(this);
            case "clear": return new PyListMethod_clear(this);
            case "count": return new PyListMethod_count(this);
            case "extend": return new PyListMethod_extend(this);
            case "index": return new PyListMethod_index(this);
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
    public PyNone pymethod_clear() {
        items.clear();
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
    public PyInt pymethod_index(PyObject arg) {
        int index = items.indexOf(arg);
        if (index == -1) {
            throw new NoSuchElementException("cannot find item in list");
        }
        return new PyInt(index);
    }
}
