// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

// BEGIN GENERATED CODE: PyRangeType
final class PyRangeType extends PyBuiltinType {
    public static final PyRangeType singleton = new PyRangeType();
    private static final PyMethodDescriptor pyattr_count = new PyMethodDescriptor(singleton, "count", PyRange.PyRangeMethod_count::new);
    private static final PyMethodDescriptor pyattr_index = new PyMethodDescriptor(singleton, "index", PyRange.PyRangeMethod_index::new);
    private static final PyMemberDescriptor pyattr_start = new PyMemberDescriptor(singleton, "start", PyRange::pymember_start);
    private static final PyMemberDescriptor pyattr_stop = new PyMemberDescriptor(singleton, "stop", PyRange::pymember_stop);
    private static final PyMemberDescriptor pyattr_step = new PyMemberDescriptor(singleton, "step", PyRange::pymember_step);
    private static final PyString pyattr___doc__ = new PyString("range(stop) -> range object\nrange(start, stop[, step]) -> range object\n\nReturn an object that produces a sequence of integers from start (inclusive)\nto stop (exclusive) by step.  range(i, j) produces i, i+1, i+2, ..., j-1.\nstart defaults to 0, and stop is omitted!  range(4) produces 0, 1, 2, 3.\nThese are exactly the valid indices for a list of 4 elements.\nWhen step is given, it specifies the increment (or decrement).");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(6);
    static {
        attrs.put(new PyString("count"), pyattr_count);
        attrs.put(new PyString("index"), pyattr_index);
        attrs.put(new PyString("start"), pyattr_start);
        attrs.put(new PyString("stop"), pyattr_stop);
        attrs.put(new PyString("step"), pyattr_step);
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyRangeType() { super("range", PyRange.class, PyRange::newObj); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "count": return pyattr_count;
            case "index": return pyattr_index;
            case "start": return pyattr_start;
            case "stop": return pyattr_stop;
            case "step": return pyattr_step;
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyRangeType

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

    static public PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
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
