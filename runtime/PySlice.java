// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

final class PySliceType extends PyBuiltinType {
// BEGIN GENERATED CODE: PySliceType
    public static final PySliceType singleton = new PySliceType();
    private static final PyMethodDescriptor pydesc_indices = new PyMethodDescriptor(singleton, "indices", PySlice.PySliceMethod_indices::new);
    private static final PyMemberDescriptor pydesc_start = new PyMemberDescriptor(singleton, "start", PySlice::pymember_start);
    private static final PyMemberDescriptor pydesc_stop = new PyMemberDescriptor(singleton, "stop", PySlice::pymember_stop);
    private static final PyMemberDescriptor pydesc_step = new PyMemberDescriptor(singleton, "step", PySlice::pymember_step);
    private static final PyString pydesc___doc__ = new PyString("slice(stop)\nslice(start, stop[, step])\n\nCreate a slice object.  This is used for extended slicing (e.g. a[0:10:2]).");
    private static final PyAttr attrs[] = new PyAttr[] {
        new PyAttr("indices", pydesc_indices),
        new PyAttr("start", pydesc_start),
        new PyAttr("stop", pydesc_stop),
        new PyAttr("step", pydesc_step),
        new PyAttr("__doc__", pydesc___doc__)
    };
    @Override public PyAttr[] getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "indices": return pydesc_indices;
            case "start": return pydesc_start;
            case "stop": return pydesc_stop;
            case "step": return pydesc_step;
            case "__doc__": return pydesc___doc__;
            default: return null;
        }
    }
// END GENERATED CODE: PySliceType

    private PySliceType() { super("slice", PySlice.class); }
    @Override public PySlice call(PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, typeName);
        Runtime.requireMinArgs(args, 1, typeName);
        Runtime.requireMaxArgs(args, 3, typeName);
        PyObject start = PyNone.singleton;
        PyObject stop;
        PyObject step = PyNone.singleton;
        if (args.length == 1) {
            stop = args[0];
        } else {
            start = args[0];
            stop = args[1];
            if (args.length == 3) {
                step = args[2];
            }
        }
        return new PySlice(start, stop, step);
    }
}

public final class PySlice extends PyTruthyObject {
    protected static final class PySliceMethod_indices extends PyBuiltinMethod<PySlice> {
        PySliceMethod_indices(PyObject _self) { super((PySlice)_self); }
        @Override public String methodName() { return "indices"; }
        @Override public PyTuple call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "slice.indices");
            Runtime.requireExactArgsAlt(args, 1, "slice.indices");
            return self.pymethod_indices(args[0]);
        }
    }

    public final PyObject start, stop, step;

    PySlice(PyObject _start, PyObject _stop, PyObject _step) {
        start = _start;
        stop = _stop;
        step = _step;
    }

    @Override public PyBuiltinType type() { return PySliceType.singleton; }

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

    public record Indices(int start, int stop, int step, int length) {}
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
            if (start < 0) {
                start += length;
            }
        }

        boolean stopIsNone = this.stop == PyNone.singleton;
        if (stopIsNone) {
            stop = (step < 0) ? -1 : length;
        } else {
            stop = asIndex(this.stop);
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
        return new Indices(start, stop, step, slicelen);
    }

    PyTuple pymethod_indices(PyObject lengthArg) {
        if (!lengthArg.hasIndex()) {
            throw PyTypeError.raise(PyString.reprOf(lengthArg.type().name()) + " object cannot be interpreted as an integer");
        }
        int length = Math.toIntExact(lengthArg.indexValue());
        if (length < 0) {
            throw PyValueError.raise("length should not be negative");
        }
        Indices indices = computeIndices(length);
        return new PyTuple(new PyObject[] {
            new PyInt(indices.start()),
            new PyInt(indices.stop()),
            new PyInt(indices.step())
        });
    }

    static PyObject pymember_start(PyObject obj) { return ((PySlice)obj).start; }
    static PyObject pymember_step(PyObject obj) { return ((PySlice)obj).step; }
    static PyObject pymember_stop(PyObject obj) { return ((PySlice)obj).stop; }
}
