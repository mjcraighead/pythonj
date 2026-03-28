// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyEnumerate extends PyIter {
    private final PyObject iter;
    private long i;

    PyEnumerate(PyObject _iter, long start) {
        iter = _iter;
        i = start;
    }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
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
                    PyString kw = (PyString)x.getKey(); // PyString validated at call site
                    if ((kw.value.equals("iterable")) && (iterable == null)) {
                        iterable = x.getValue();
                    } else if ((totalArgs == 2) && (kw.value.equals("start")) && (start == null)) {
                        start = x.getValue();
                    } else {
                        throw PyTypeError.raiseFormat("%s is an invalid keyword argument for enumerate()", kw.repr());
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
            throw Runtime.raiseAtMostArgs("enumerate", 2, totalArgs);
        }
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
