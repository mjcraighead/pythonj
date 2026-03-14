// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PySlice extends PyTruthyObject {
    public final PyObject start, stop, step;

    PySlice(PyObject _start, PyObject _stop, PyObject _step) {
        start = _start;
        stop = _stop;
        step = _step;
    }

    @Override public PyBuiltinClass type() { return Runtime.pyglobal_slice; }

    @Override public boolean ge(PyObject rhs) { throw unimplementedMethod("ge"); }
    @Override public boolean gt(PyObject rhs) { throw unimplementedMethod("gt"); }
    @Override public boolean le(PyObject rhs) { throw unimplementedMethod("le"); }
    @Override public boolean lt(PyObject rhs) { throw unimplementedMethod("lt"); }

    @Override public boolean contains(PyObject rhs) { return defaultContains(rhs); }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PySlice rhsSlice) {
            return start.equals(rhsSlice.start) &&
                   stop.equals(rhsSlice.stop) &&
                   step.equals(rhsSlice.step);
        }
        return false;
    }
    @Override public String repr() {
        return String.format("slice(%s, %s, %s)", start.repr(), stop.repr(), step.repr());
    }

    public record Indices(int start, int step, int length) {}
    private static int asIndex(PyObject obj) {
        if (!obj.hasIndex()) {
            throw PyTypeError.raise("slice indices must be integers or None or have an __index__ method");
        }
        return Math.toIntExact(obj.indexValue());
    }
    private static int clamp(int v, int lo, int hi) {
        return (v < lo) ? lo : (v > hi) ? hi : v;
    }
    public final Indices computeIndices(int length) {
        int step = 1;
        if (this.step != PyNone.singleton) {
            step = asIndex(this.step);
            if (step == 0) {
                throw PyValueError.raise("slice step cannot be zero");
            }
        }

        int start, stop;
        if (this.start == PyNone.singleton) {
            start = (step < 0) ? (length - 1) : 0;
        } else {
            start = asIndex(this.start);
        }

        boolean stopIsNone = this.stop == PyNone.singleton;
        if (stopIsNone) {
            stop = (step < 0) ? -1 : length;
        } else {
            stop = asIndex(this.stop);
        }

        if (start < 0) {
            start += length;
        }

        int slicelen;
        if (step < 0) {
            start = clamp(start, -1, length - 1);

            if (!stopIsNone && (stop < 0)) {
                stop += length;
            }
            stop = clamp(stop, -1, length - 1);

            slicelen = (stop < start) ? ((start - stop - 1) / (-step) + 1) : 0;
        } else {
            start = clamp(start, 0, length);

            if (stop < 0) {
                stop += length;
            }
            stop = clamp(stop, 0, length);

            slicelen = (start < stop) ? ((stop - start - 1) / step + 1) : 0;
        }
        return new Indices(start, step, slicelen);
    }
}
