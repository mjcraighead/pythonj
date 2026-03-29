// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyRange extends PyObject {
    static final class PyRangeIter extends PyIter {
        private static final PyConcreteType type_singleton = new PyConcreteType("range_iterator", PyRangeIter.class);

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
        @Override public PyConcreteType type() { return type_singleton; }
    };

    protected final long start, stop, step;

    PyRange(long _start, long _stop, long _step) {
        if (_step == 0) {
            throw new IllegalArgumentException("range() step cannot be zero");
        }
        start = _start;
        stop = _stop;
        step = _step;
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        Runtime.requireMinArgs(args, 1, type.name());
        Runtime.requireMaxArgs(args, 3, type.name());
        long start = 0, stop, step = 1;
        if (args.length == 1) {
            stop = args[0].indexValue();
        } else {
            start = args[0].indexValue();
            stop = args[1].indexValue();
            if (args.length == 3) {
                step = args[2].indexValue();
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
    @Override public String repr() {
        if (step == 1) {
            return String.format("range(%d, %d)", start, stop);
        } else {
            return String.format("range(%d, %d, %d)", start, stop, step);
        }
    }

    static PyObject pymember_start(PyObject obj) { return new PyInt(((PyRange)obj).start); }
    static PyObject pymember_step(PyObject obj) { return new PyInt(((PyRange)obj).step); }
    static PyObject pymember_stop(PyObject obj) { return new PyInt(((PyRange)obj).stop); }

    public PyObject pymethod_count(PyObject arg) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_index(PyObject arg) { throw new UnsupportedOperationException(); }
}
