// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyRange extends PyObject {
    private static final PyBuiltinClass iter_class_singleton = new PyBuiltinClass("range_iterator", PyRangeIter.class);
    static final class PyRangeIter extends PyIter {
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
            start += step;
            return ret;
        }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyBuiltinClass type() { return iter_class_singleton; }
    };

    protected static final class PyRangeMethod_count extends PyBuiltinMethod<PyRange> {
        PyRangeMethod_count(PyObject _self) { super((PyRange)_self); }
        @Override public String methodName() { return "count"; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("range.count() unimplemented");
        }
    }
    protected static final class PyRangeMethod_index extends PyBuiltinMethod<PyRange> {
        PyRangeMethod_index(PyObject _self) { super((PyRange)_self); }
        @Override public String methodName() { return "index"; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("range.index() unimplemented");
        }
    }

    protected final long start, stop, step;

    PyRange(long _start, long _stop, long _step) {
        if (_step == 0) {
            throw new IllegalArgumentException("range() step cannot be zero");
        }
        start = _start;
        stop = _stop;
        step = _step;
    }

    @Override public PyInt getItem(PyObject key) { throw unimplementedMethod("getItem"); }
    @Override public void delItem(PyObject key) {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object doesn't support item deletion");
    }

    @Override public final boolean hasIter() { return true; }
    @Override public PyRangeIter iter() { return new PyRangeIter(this); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_range; }

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
    @Override public String repr() {
        if (step == 1) {
            return String.format("range(%d, %d)", start, stop);
        } else {
            return String.format("range(%d, %d, %d)", start, stop, step);
        }
    }
}
