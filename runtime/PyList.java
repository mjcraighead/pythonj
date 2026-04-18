// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

final class PyListIter extends PyIter {
    private static final PyConcreteType type_singleton = new PyConcreteType("list_iterator", PyListIter.class, PyObjectType.singleton);

    private final Iterator<PyObject> it;

    PyListIter(Iterator<PyObject> _it) { it = _it; }

    @Override public PyObject next() { return it.hasNext() ? it.next() : null; }
    @Override public String repr() { return defaultRepr(); }
    @Override public PyConcreteType type() { return type_singleton; }
};

public final class PyList extends PyObject {
    record SortKeyedItem(PyObject item, PyObject key) implements Comparable<SortKeyedItem> {
        @Override public int compareTo(SortKeyedItem rhs) {
            return key.compareTo(rhs.key);
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

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireMinMaxPositional(args, kwargs, type.name(), 0, 1);
        return newObjPositional((args.length == 0) ? null : args[0]);
    }
    public static PyList newObjPositional(PyObject arg) {
        var ret = new PyList();
        if (arg == null) {
            return ret;
        }
        Runtime.addIterableToCollection(ret.items, arg);
        return ret;
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
        if (!rhs.hasIndex()) {
            throw PyTypeError.raise("can't multiply sequence by non-int of type " + PyString.reprOf(rhs.type().name()));
        }
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
        } else if (key.hasIndex()) {
            int index = Math.toIntExact(key.indexValue());
            if (index < 0) {
                index += items.size();
            }
            try {
                return items.get(index);
            } catch (IndexOutOfBoundsException e) {
                throw PyIndexError.raise("list index out of range");
            }
        } else {
            throw PyTypeError.raise("list indices must be integers or slices, not " + key.type().name());
        }
    }
    @Override public void setItem(PyObject key, PyObject value) {
        if (key instanceof PySlice) {
            throw unimplementedMethod("slice assignment");
        } else if (key.hasIndex()) {
            int index = Math.toIntExact(key.indexValue());
            if (index < 0) {
                index += items.size();
            }
            try {
                items.set(index, value);
            } catch (IndexOutOfBoundsException e) {
                throw PyIndexError.raise("list assignment index out of range");
            }
        } else {
            throw PyTypeError.raise("list indices must be integers or slices, not " + key.type().name());
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
    @Override public PyConcreteType type() { return PyListType.singleton; }

    @Override public boolean boolValue() { return !items.isEmpty(); }
    @Override public boolean contains(PyObject rhs) { return items.contains(rhs); }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PyList rhsList) {
            return items.equals(rhsList.items);
        }
        return false;
    }
    @Override public int hashCode() { throw raiseUnhashable(); }
    @Override public long len() { return items.size(); }
    @Override public String repr() { return PyRuntime.pyfunc_list____repr__(this).value; }

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
    public PyInt pymethod_index(PyObject value, PyObject start, PyObject stop) {
        int n = items.size();
        int startIndex = Runtime.asSliceIndexAllowNull(start, 0, n);
        int stopIndex = Runtime.asSliceIndexAllowNull(stop, n, n);
        try {
            for (int i = startIndex; i < stopIndex; i++) {
                if (items.get(i).equals(value)) {
                    return new PyInt(i);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // Concurrent mutation can invalidate the originally computed bounds.
            throw PyRuntimeError.raise("list changed size during index search");
        }
        throw PyValueError.raise("list.index(x): x not in list");
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
        int index = Math.toIntExact(indexObj.indexValue());
        if (items.isEmpty()) {
            throw PyIndexError.raise("pop from empty list");
        }
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
    public PyNone pymethod_sort(PyObject key, PyObject reverse) {
        boolean reverseBool = reverse.boolValue();
        if (key == PyNone.singleton) {
            Collections.sort(items);
        } else {
            var keyedItems = new ArrayList<SortKeyedItem>(items.size());
            for (var item: items) {
                keyedItems.add(new SortKeyedItem(item, key.call(new PyObject[] {item}, null)));
            }
            Collections.sort(keyedItems);
            items.clear();
            for (var keyedItem: keyedItems) {
                items.add(keyedItem.item);
            }
        }
        if (reverseBool) {
            Collections.reverse(items);
        }
        return PyNone.singleton;
    }
}
