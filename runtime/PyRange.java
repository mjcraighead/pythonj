// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyRange extends PyObject {
    private static final PyBuiltinClass iter_class_singleton = new PyBuiltinClass("range_iterator");
    static final class PyRangeIter extends PyIter {
        private long start;
        private final long end, step;

        PyRangeIter(PyRange r) {
            start = r.start;
            end = r.end;
            step = r.step;
        }

        @Override public PyInt next() {
            if (step > 0) {
                if (start >= end) {
                    return null;
                }
            } else {
                if (start <= end) {
                    return null;
                }
            }
            var ret = new PyInt(start);
            start += step;
            return ret;
        }
        @Override public PyBuiltinClass type() { return iter_class_singleton; }
    };

    public final long start, end, step;

    PyRange(long _start, long _end, long _step) {
        if (_step == 0) {
            throw new RuntimeException("range() step cannot be zero");
        }
        start = _start;
        end = _end;
        step = _step;
    }

    @Override public PyRangeIter iter() { return new PyRangeIter(this); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_range; }

    @Override public boolean boolValue() {
        return len() != 0;
    }
    @Override public boolean equals(Object rhs_arg) {
        if (rhs_arg instanceof PyRange rhs) {
            // XXX CPython does not compare end, it compares start/step/length, and also special cases empty ranges
            // This could be fixed, but creates potentially expensive hashing obligations as well
            return (start == rhs.start) &&
                   (end == rhs.end) &&
                   (step == rhs.step);
        }
        return false;
    }
    @Override public long len() {
        if (step > 0) {
            if (start >= end) {
                return 0;
            }
            return (end - start + step - 1) / step;
        } else {
            if (start <= end) {
                return 0;
            }
            return (start - end - step - 1) / (-step);
        }
    }
    @Override public String repr() {
        if (step == 1) {
            return String.format("range(%d, %d)", start, end);
        } else {
            return String.format("range(%d, %d, %d)", start, end, step);
        }
    }
}
