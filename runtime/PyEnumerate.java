// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

final class PyEnumerateType extends PyBuiltinType {
// BEGIN GENERATED CODE: PyEnumerateType
    public static final PyEnumerateType singleton = new PyEnumerateType();
    private static final PyString pyattr___doc__ = new PyString("Return an enumerate object.\n\n  iterable\n    an object supporting iteration\n\nThe enumerate object yields pairs containing a count (from start, which\ndefaults to zero) and a value yielded by the iterable argument.\n\nenumerate is useful for obtaining an indexed list:\n    (0, seq[0]), (1, seq[1]), (2, seq[2]), ...");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(1);
    static {
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyEnumerateType() { super("enumerate", PyEnumerate.class); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
// END GENERATED CODE: PyEnumerateType

    @Override public PyEnumerate call(PyObject[] args, PyDict kwargs) {
        // This is quirky, but is intended to match corner cases in CPython enumerate()
        long totalArgs = args.length;
        if (kwargs != null) {
            totalArgs += kwargs.items.size();
        }
        if ((totalArgs == 1) || (totalArgs == 2)) {
            PyObject iterable = (args.length >= 1) ? args[0] : null;
            PyObject start = (args.length >= 2) ? args[1] : null;
            if (kwargs != null) {
                for (var x: kwargs.items.entrySet()) {
                    PyString key = (PyString)x.getKey(); // PyString validated at call site
                    if ((key.value.equals("iterable")) && (iterable == null)) {
                        iterable = x.getValue();
                    } else if ((totalArgs == 2) && (key.value.equals("start")) && (start == null)) {
                        start = x.getValue();
                    } else {
                        throw PyTypeError.raiseFormat("%s is an invalid keyword argument for enumerate()", key.repr());
                    }
                }
            }
            if (iterable == null) {
                throw PyTypeError.raise("enumerate() missing required argument 'iterable'");
            }
            long startIndex = (start != null) ? start.indexValue() : 0;
            return new PyEnumerate(iterable.iter(), startIndex);
        } else if (args.length == 0) {
            throw PyTypeError.raise("enumerate() missing required argument 'iterable'");
        } else {
            throw PyTypeError.raiseFormat("enumerate() takes at most 2 arguments (%d given)", totalArgs);
        }
    }
}

public final class PyEnumerate extends PyIter {
    private final PyObject iter;
    private long i;

    PyEnumerate(PyObject _iter, long start) {
        iter = _iter;
        i = start;
    }

    @Override public PyTuple next() {
        var item = iter.next();
        if (item == null) {
            return null;
        }
        var ret = new PyTuple(new PyObject[] {new PyInt(i), item});
        i = Math.incrementExact(i);
        return ret;
    }

    @Override public boolean contains(PyObject rhs) { return defaultContains(rhs); }
    @Override public String repr() { return defaultRepr(); }
    @Override public PyBuiltinType type() { return PyEnumerateType.singleton; }
}
