// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public final class PyList extends PyObject {
    private static final PyBuiltinClass iter_class_singleton = new PyBuiltinClass("list_iterator", PyListIter.class);
    static final class PyListIter extends PyIter {
        private final Iterator<PyObject> it;

        PyListIter(Iterator<PyObject> _it) { it = _it; }

        @Override public PyObject next() { return it.hasNext() ? it.next() : null; }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyBuiltinClass type() { return iter_class_singleton; }
    };

    private static final class PyListMethod_append extends PyBuiltinMethod<PyList> {
        PyListMethod_append(PyList _self) { super(_self); }
        @Override public String methodName() { return "append"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.append");
            Runtime.requireExactArgsAlt(args, 1, "list.append");
            return self.pymethod_append(args[0]);
        }
    }
    private static final class PyListMethod_clear extends PyBuiltinMethod<PyList> {
        PyListMethod_clear(PyList _self) { super(_self); }
        @Override public String methodName() { return "clear"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.clear");
            Runtime.requireExactArgsAlt(args, 0, "list.clear");
            return self.pymethod_clear();
        }
    }
    private static final class PyListMethod_copy extends PyBuiltinMethod<PyList> {
        PyListMethod_copy(PyList _self) { super(_self); }
        @Override public String methodName() { return "copy"; }
        @Override public PyList call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.copy");
            Runtime.requireExactArgsAlt(args, 0, "list.copy");
            return self.pymethod_copy();
        }
    }
    private static final class PyListMethod_count extends PyBuiltinMethod<PyList> {
        PyListMethod_count(PyList _self) { super(_self); }
        @Override public String methodName() { return "count"; }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.count");
            Runtime.requireExactArgsAlt(args, 1, "list.count");
            return self.pymethod_count(args[0]);
        }
    }
    private static final class PyListMethod_extend extends PyBuiltinMethod<PyList> {
        PyListMethod_extend(PyList _self) { super(_self); }
        @Override public String methodName() { return "extend"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.extend");
            Runtime.requireExactArgsAlt(args, 1, "list.extend");
            return self.pymethod_extend(args[0]);
        }
    }
    private static final class PyListMethod_index extends PyBuiltinMethod<PyList> {
        PyListMethod_index(PyList _self) { super(_self); }
        @Override public String methodName() { return "index"; }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.index");
            Runtime.requireMinArgs(args, 1, "index");
            Runtime.requireMaxArgs(args, 3, "index");
            if (args.length != 1) {
                throw new IllegalArgumentException("list.index() takes 1 argument");
            }
            return self.pymethod_index(args[0]);
        }
    }
    private static final class PyListMethod_insert extends PyBuiltinMethod<PyList> {
        PyListMethod_insert(PyList _self) { super(_self); }
        @Override public String methodName() { return "insert"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.insert");
            Runtime.requireExactArgs(args, 2, "insert");
            return self.pymethod_insert(args[0], args[1]);
        }
    }
    private static final class PyListMethod_pop extends PyBuiltinMethod<PyList> {
        PyListMethod_pop(PyList _self) { super(_self); }
        @Override public String methodName() { return "pop"; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.pop");
            Runtime.requireMinArgs(args, 0, "pop");
            Runtime.requireMaxArgs(args, 1, "pop");
            PyObject index = (args.length >= 1) ? args[0] : PyInt.singleton_neg1;
            return self.pymethod_pop(index);
        }
    }
    private static final class PyListMethod_remove extends PyBuiltinMethod<PyList> {
        PyListMethod_remove(PyList _self) { super(_self); }
        @Override public String methodName() { return "remove"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.remove");
            Runtime.requireExactArgsAlt(args, 1, "list.remove");
            return self.pymethod_remove(args[0]);
        }
    }
    private static final class PyListMethod_reverse extends PyBuiltinMethod<PyList> {
        PyListMethod_reverse(PyList _self) { super(_self); }
        @Override public String methodName() { return "reverse"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "list.reverse");
            Runtime.requireExactArgsAlt(args, 0, "list.reverse");
            return self.pymethod_reverse();
        }
    }
    private static final class PyListMethod_sort extends PyBuiltinMethod<PyList> {
        PyListMethod_sort(PyList _self) { super(_self); }
        @Override public String methodName() { return "sort"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            if ((kwargs != null) && kwargs.boolValue()) { // XXX Handle more cases correctly here
                if (kwargs.len() > 2) {
                    throw PyTypeError.raiseFormat("sort() takes at most 2 keyword arguments (%d given)", kwargs.len());
                }
                for (var x: kwargs.items.entrySet()) {
                    PyString key = (PyString)x.getKey(); // PyString validated at call site
                    if (!key.value.equals("key") && !key.value.equals("reverse")) {
                        throw PyTypeError.raise("sort() got an unexpected keyword argument " + key.repr());
                    }
                }
                throw new IllegalArgumentException("list.sort() does not accept kwargs");
            }
            if (args.length > 2) {
                throw PyTypeError.raiseFormat("sort() takes at most 2 arguments (%d given)", args.length);
            } else if (args.length != 0) {
                throw PyTypeError.raise("sort() takes no positional arguments");
            }
            return self.pymethod_sort();
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

    static int compare(ArrayList<PyObject> lhs, ArrayList<PyObject> rhs) {
        int n = Math.min(lhs.size(), rhs.size());
        for (int i = 0; i < n; i++) {
            int c = lhs.get(i).compareTo(rhs.get(i));
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(lhs.size(), rhs.size());
    }

    @Override public PyList add(PyObject rhs) {
        if (rhs instanceof PyList rhsList) {
            var list = new ArrayList<PyObject>(Math.addExact(items.size(), rhsList.items.size()));
            list.addAll(items);
            list.addAll(rhsList.items);
            return new PyList(list);
        } else {
            throw PyTypeError.raise("can only concatenate list (not \"" + rhs.type().name() + "\") to list");
        }
    }
    @Override public PyList addInPlace(PyObject rhs) {
        pymethod_extend(rhs);
        return this;
    }
    @Override public PyList mul(PyObject rhs) {
        if (!rhs.hasIndex()) {
            throw PyTypeError.raise("can't multiply sequence by non-int of type " + PyString.reprOf(rhs.type().name()));
        }
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
    @Override public PyList rmul(PyObject rhs) { return mul(rhs); }

    @Override public boolean ge(PyObject rhs) {
        if (rhs instanceof PyList rhsList) {
            return compare(items, rhsList.items) >= 0;
        } else {
            return super.ge(rhs);
        }
    }
    @Override public boolean gt(PyObject rhs) {
        if (rhs instanceof PyList rhsList) {
            return compare(items, rhsList.items) > 0;
        } else {
            return super.gt(rhs);
        }
    }
    @Override public boolean le(PyObject rhs) {
        if (rhs instanceof PyList rhsList) {
            return compare(items, rhsList.items) <= 0;
        } else {
            return super.le(rhs);
        }
    }
    @Override public boolean lt(PyObject rhs) {
        if (rhs instanceof PyList rhsList) {
            return compare(items, rhsList.items) < 0;
        } else {
            return super.lt(rhs);
        }
    }

    @Override public PyObject getItem(PyObject key) {
        if (key instanceof PySlice slice) {
            PySlice.Indices indices = slice.computeIndices(items.size());
            int index = indices.start();
            int step = indices.step();
            int n = indices.length();
            if (step == 1) {
                return new PyList(new ArrayList<>(items.subList(index, index + n)));
            } else {
                var result = new ArrayList<PyObject>(n);
                for (int i = 0; i < n; i++) {
                    result.add(items.get(index));
                    index += step;
                }
                return new PyList(result);
            }
        } else {
            int index = Math.toIntExact(key.indexValue());
            if (index < 0) {
                index += items.size();
            }
            try {
                return items.get(index);
            } catch (IndexOutOfBoundsException e) {
                throw PyIndexError.raise("list index out of range");
            }
        }
    }
    @Override public void setItem(PyObject key, PyObject value) {
        if (key instanceof PySlice) {
            throw unimplementedMethod("slice assignment");
        } else {
            int index = Math.toIntExact(key.indexValue());
            if (index < 0) {
                index += items.size();
            }
            try {
                items.set(index, value);
            } catch (IndexOutOfBoundsException e) {
                throw PyIndexError.raise("list assignment index out of range");
            }
        }
    }
    @Override public void delItem(PyObject key) {
        throw unimplementedMethod("delItem");
    }

    // NOTE: CPython returns a specialized list_reverseiterator.
    // pythonj intentionally uses the generic reversed iterator, at least for now.
    @Override public final boolean hasIter() { return true; }
    @Override public PyListIter iter() { return new PyListIter(items.iterator()); }
    @Override public PyReversed reversed() { return new PyReversed(this); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_list; }

    @Override public boolean boolValue() { return !items.isEmpty(); }
    @Override public boolean contains(PyObject rhs) { return items.contains(rhs); }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PyList rhsList) {
            return items.equals(rhsList.items);
        }
        return false;
    }
    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "append": return new PyListMethod_append(this);
            case "clear": return new PyListMethod_clear(this);
            case "copy": return new PyListMethod_copy(this);
            case "count": return new PyListMethod_count(this);
            case "extend": return new PyListMethod_extend(this);
            case "index": return new PyListMethod_index(this);
            case "insert": return new PyListMethod_insert(this);
            case "pop": return new PyListMethod_pop(this);
            case "remove": return new PyListMethod_remove(this);
            case "reverse": return new PyListMethod_reverse(this);
            case "sort": return new PyListMethod_sort(this);
            default:
                if (key.startsWith("__")) {
                    return super.getAttr(key);
                } else {
                    throw raiseMissingAttr(key);
                }
        }
    }
    @Override public int hashCode() { throw raiseUnhashable(); }
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
    public PyList pymethod_copy() {
        return new PyList(new ArrayList<>(items));
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
        if (arg instanceof PyList argList) {
            items.addAll(argList.items); // safe for x.extend(x)
        } else {
            Runtime.addIterableToCollection(items, arg);
        }
        return PyNone.singleton;
    }
    public PyInt pymethod_index(PyObject arg) {
        int index = items.indexOf(arg);
        if (index == -1) {
            throw PyValueError.raise("list.index(x): x not in list");
        }
        return new PyInt(index);
    }
    public PyNone pymethod_insert(PyObject indexObj, PyObject value) {
        int index = Math.toIntExact(indexObj.indexValue());
        if (index < 0) {
            index += items.size();
        }
        index = Math.max(index, 0);
        index = Math.min(index, items.size());
        items.add(index, value);
        return PyNone.singleton;
    }
    public PyObject pymethod_pop(PyObject indexObj) {
        if (items.isEmpty()) {
            throw PyIndexError.raise("pop from empty list");
        }
        int index = Math.toIntExact(indexObj.indexValue());
        if (index < 0) {
            index += items.size();
        }
        try {
            return items.remove(index);
        } catch (IndexOutOfBoundsException e) {
            throw PyIndexError.raise("pop index out of range");
        }
    }
    public PyNone pymethod_remove(PyObject value) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equals(value)) {
                items.remove(i);
                return PyNone.singleton;
            }
        }
        throw PyValueError.raise("list.remove(x): x not in list");
    }
    public PyNone pymethod_reverse() {
        Collections.reverse(items);
        return PyNone.singleton;
    }
    public PyNone pymethod_sort() {
        Collections.sort(items);
        return PyNone.singleton;
    }
}
