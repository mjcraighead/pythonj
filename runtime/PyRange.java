// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

final class PyRangeType extends PyBuiltinType {
    public static final PyRangeType singleton = new PyRangeType();
// BEGIN GENERATED CODE: PyRangeType
    private static final PyMethodDescriptor pydesc_count = new PyMethodDescriptor(singleton, "count", PyRange.PyRangeMethod_count::new);
    private static final PyMethodDescriptor pydesc_index = new PyMethodDescriptor(singleton, "index", PyRange.PyRangeMethod_index::new);
    private static final PyMemberDescriptor pydesc_start = new PyMemberDescriptor(singleton, "start", PyRange::pymember_start);
    private static final PyMemberDescriptor pydesc_stop = new PyMemberDescriptor(singleton, "stop", PyRange::pymember_stop);
    private static final PyMemberDescriptor pydesc_step = new PyMemberDescriptor(singleton, "step", PyRange::pymember_step);
    private static final PyString pydesc___doc__ = new PyString("range(stop) -> range object\nrange(start, stop[, step]) -> range object\n\nReturn an object that produces a sequence of integers from start (inclusive)\nto stop (exclusive) by step.  range(i, j) produces i, i+1, i+2, ..., j-1.\nstart defaults to 0, and stop is omitted!  range(4) produces 0, 1, 2, 3.\nThese are exactly the valid indices for a list of 4 elements.\nWhen step is given, it specifies the increment (or decrement).");
    private static final PyAttr attrs[] = new PyAttr[] {
        new PyAttr("count", pydesc_count),
        new PyAttr("index", pydesc_index),
        new PyAttr("start", pydesc_start),
        new PyAttr("stop", pydesc_stop),
        new PyAttr("step", pydesc_step),
        new PyAttr("__doc__", pydesc___doc__)
    };
    @Override public PyAttr[] getAttributes() { return attrs; }
    @Override public PyDescriptor getDescriptor(String name) {
        switch (name) {
            case "count": return pydesc_count;
            case "index": return pydesc_index;
            case "start": return pydesc_start;
            case "stop": return pydesc_stop;
            case "step": return pydesc_step;
            default: return null;
        }
    }
// END GENERATED CODE: PyRangeType

    private PyRangeType() { super("range", PyRange.class); }
    @Override public PyRange call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        Runtime.requireMinArgs(args, 1, typeName);
        Runtime.requireMaxArgs(args, 3, typeName);
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
}

public final class PyRange extends PyObject {
    static final class PyRangeIter extends PyIter {
        private static final PyBuiltinType type_singleton = new PyBuiltinType("range_iterator", PyRangeIter.class);

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
        @Override public PyBuiltinType type() { return type_singleton; }
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
    @Override public PyBuiltinType type() { return PyRangeType.singleton; }

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
}
