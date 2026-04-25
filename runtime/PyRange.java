// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

final class PyRangeIter extends PyIter {
    private static final PyConcreteType type_singleton = new PyConcreteType("range_iterator", "range_iterator", "builtins", PyRangeIter.class, PyObjectType.singleton, null);

    private long start;
    private final long stop, step;

    PyRangeIter(PyRange r) {
        start = r.start;
        stop = r.stop;
        step = r.step;
    }

    @Override public PyInt next() {
        if (step > 0) {
            if (start >= stop) {
                return null;
            }
        } else {
            if (start <= stop) {
                return null;
            }
        }
        var ret = new PyInt(start);
        start = Math.addExact(start, step);
        return ret;
    }
    @Override public String repr() { return defaultRepr(); }
    @Override public PyConcreteType type() { return type_singleton; }
};

public final class PyRange extends PyObject {
    protected final long start, stop, step;

    PyRange(long _start, long _stop, long _step) {
        if (_step == 0) {
            throw PyValueError.raise("range() arg 3 must not be zero");
        }
        start = _start;
        stop = _stop;
        step = _step;
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireMinMaxPositional(args, kwargs, type.name(), 1, 3);
        PyObject arg0 = args[0];
        PyObject arg1 = (args.length >= 2) ? args[1] : null;
        PyObject arg2 = (args.length >= 3) ? args[2] : null;
        return newObjPositional(arg0, arg1, arg2);
    }
    public static PyRange newObjPositional(PyObject arg0, PyObject arg1, PyObject arg2) {
        long start = 0;
        long stop;
        long step = 1;
        if (arg1 == null) {
            stop = arg0.indexValue();
        } else {
            start = arg0.indexValue();
            stop = arg1.indexValue();
            if (arg2 != null) {
                step = arg2.indexValue();
            }
        }
        return new PyRange(start, stop, step);
    }

    @Override public PyInt getItem(PyObject key) { throw unimplementedMethod("getItem"); }
    @Override public void delItem(PyObject key) {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object doesn't support item deletion");
    }
    @Override public final boolean hasIter() { return true; }
    @Override public PyRangeIter iter() { return new PyRangeIter(this); }
    @Override public PyConcreteType type() { return PyRangeType.singleton; }

    @Override public boolean boolValue() {
        return len() != 0;
    }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PyRange rhsRange) {
            // XXX CPython does not compare stop, it compares start/step/length, and also special cases empty ranges
            // This could be fixed, but creates potentially expensive hashing obligations as well
            return (start == rhsRange.start) &&
                   (stop == rhsRange.stop) &&
                   (step == rhsRange.step);
        }
        return false;
    }
    @Override public long len() {
        // XXX Math needs some overflow checks/handling
        if (step > 0) {
            if (start >= stop) {
                return 0;
            }
            return (stop - start + step - 1) / step;
        } else {
            if (start <= stop) {
                return 0;
            }
            return (start - stop - step - 1) / (-step);
        }
    }
    @Override public String repr() { return slotBasedRepr(); }

    public PyObject pymethod___reversed__() { throw new UnsupportedOperationException(); }
}
