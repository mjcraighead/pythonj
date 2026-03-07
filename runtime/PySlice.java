// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PySlice extends PyTruthyObject {
    public final PyObject start, end, step;

    PySlice(PyObject _start, PyObject _end, PyObject _step) {
        start = _start;
        end = _end;
        step = _step;
    }

    @Override public PyBuiltinClass type() { return Runtime.pyglobal_slice; }

    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PySlice rhsSlice) {
            return start.equals(rhsSlice.start) &&
                   end.equals(rhsSlice.end) &&
                   step.equals(rhsSlice.step);
        }
        return false;
    }
    @Override public String repr() {
        return String.format("slice(%s, %s, %s)", start.repr(), end.repr(), step.repr());
    }
}
