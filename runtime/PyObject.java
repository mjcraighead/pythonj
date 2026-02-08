// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.NoSuchElementException;

public abstract class PyObject implements Comparable<PyObject> {
    // These all take and/or return boxed PyObjects
    public PyObject invert() { throw new UnsupportedOperationException("'invert' unimplemented"); }
    public PyObject pos() { throw new UnsupportedOperationException("'pos' unimplemented"); }
    public PyObject neg() { throw new UnsupportedOperationException("'neg' unimplemented"); }
    public PyObject abs() { throw new UnsupportedOperationException("'abs' unimplemented"); }

    public PyObject add(PyObject rhs) { throw new UnsupportedOperationException("'add' unimplemented"); }
    public PyObject and(PyObject rhs) { throw new UnsupportedOperationException("'and' unimplemented"); }
    public PyObject floordiv(PyObject rhs) { throw new UnsupportedOperationException("'floordiv' unimplemented"); }
    public PyObject lshift(PyObject rhs) { throw new UnsupportedOperationException("'lshift' unimplemented"); }
    public PyObject matmul(PyObject rhs) { throw new UnsupportedOperationException("'matmul' unimplemented"); }
    public PyObject mod(PyObject rhs) { throw new UnsupportedOperationException("'mod' unimplemented"); }
    public PyObject mul(PyObject rhs) { throw new UnsupportedOperationException("'mul' unimplemented"); }
    public PyObject or(PyObject rhs) { throw new UnsupportedOperationException("'or' unimplemented"); }
    public PyObject pow(PyObject rhs) { throw new UnsupportedOperationException("'pow' unimplemented"); }
    public PyObject rshift(PyObject rhs) { throw new UnsupportedOperationException("'rshift' unimplemented"); }
    public PyObject sub(PyObject rhs) { throw new UnsupportedOperationException("'sub' unimplemented"); }
    public PyObject truediv(PyObject rhs) { throw new UnsupportedOperationException("'truediv' unimplemented"); }
    public PyObject xor(PyObject rhs) { throw new UnsupportedOperationException("'xor' unimplemented"); }

    // By default, in-place ops map to regular ops
    public PyObject addInPlace(PyObject rhs) { return add(rhs); }
    public PyObject andInPlace(PyObject rhs) { return and(rhs); }
    public PyObject floordivInPlace(PyObject rhs) { return floordiv(rhs); }
    public PyObject lshiftInPlace(PyObject rhs) { return lshift(rhs); }
    public PyObject matmulInPlace(PyObject rhs) { return matmul(rhs); }
    public PyObject modInPlace(PyObject rhs) { return mod(rhs); }
    public PyObject mulInPlace(PyObject rhs) { return mul(rhs); }
    public PyObject orInPlace(PyObject rhs) { return or(rhs); }
    public PyObject powInPlace(PyObject rhs) { return pow(rhs); }
    public PyObject rshiftInPlace(PyObject rhs) { return rshift(rhs); }
    public PyObject subInPlace(PyObject rhs) { return sub(rhs); }
    public PyObject truedivInPlace(PyObject rhs) { return truediv(rhs); }
    public PyObject xorInPlace(PyObject rhs) { return xor(rhs); }

    public boolean ge(PyObject rhs) { throw new UnsupportedOperationException("'ge' unimplemented"); }
    public boolean gt(PyObject rhs) { throw new UnsupportedOperationException("'gt' unimplemented"); }
    public boolean le(PyObject rhs) { throw new UnsupportedOperationException("'le' unimplemented"); }
    public boolean lt(PyObject rhs) { throw new UnsupportedOperationException("'lt' unimplemented"); }

    public PyObject getItem(PyObject key) { throw new UnsupportedOperationException("'getItem' unimplemented"); }
    public void setItem(PyObject key, PyObject value) { throw new UnsupportedOperationException("'setItem' unimplemented"); }
    public void delItem(PyObject key) { throw new UnsupportedOperationException("'delItem' unimplemented"); }

    public PyObject getAttr(String key) {
        switch (key) {
            case "__class__": return type();
            default: throw new NoSuchElementException(String.format("object does not have attribute '%s'", key));
        }
    }
    public void setAttr(String key, PyObject value) { throw new UnsupportedOperationException("'setAttr' unimplemented"); }
    public void delAttr(String key) { throw new UnsupportedOperationException("'delAttr' unimplemented"); }

    public PyObject call(PyObject args[], PyDict kwargs) { throw new UnsupportedOperationException("'call' unimplemented"); }

    public PyIter iter() { throw new UnsupportedOperationException("'iter' unimplemented"); }
    public PyIter reversed() { throw new UnsupportedOperationException("'reversed' unimplemented"); }
    public PyObject next() { throw new UnsupportedOperationException("'next' unimplemented"); }
    public PyType type() { throw new UnsupportedOperationException("'type' unimplemented"); }

    public PyObject enter() { throw new UnsupportedOperationException("'enter' unimplemented"); }
    public void exit() { throw new UnsupportedOperationException("'exit' unimplemented"); }

    // These take and/or return unboxed values
    // Note: any subclass that overrides equals() must also override hashCode(), unless it is intentionally 
    // unhashable.  If a.equals(b) is true, then a.hashCode() == b.hashCode() must also be true.
    public abstract boolean boolValue();
    @Override public final int compareTo(PyObject rhs) {
        if (lt(rhs)) {
            return -1;
        }
        if (equals(rhs)) {
            return 0;
        }
        return 1;
    }
    public boolean contains(PyObject rhs) { throw new UnsupportedOperationException("'contains' unimplemented"); }
    @Override public boolean equals(Object rhs) { throw new UnsupportedOperationException("'equals' unimplemented"); }
    public double floatValue() { throw new UnsupportedOperationException("'floatValue' unimplemented"); }
    public String format(String formatSpec) { throw new UnsupportedOperationException("'format' unimplemented"); }
    @Override public int hashCode() { throw new UnsupportedOperationException("'hashCode' unimplemented"); }
    public long indexValue() { throw new UnsupportedOperationException("'indexValue' unimplemented"); }
    public long intValue() { throw new UnsupportedOperationException("'intValue' unimplemented"); }
    public long len() { throw new UnsupportedOperationException("'len' unimplemented"); }
    public abstract String repr();
    public String str() { return repr(); }

    // Wrapper that reverses the order of evaluation vs. "contains" to assist translator
    public final boolean in(PyObject rhs) { return rhs.contains(this); }
}
