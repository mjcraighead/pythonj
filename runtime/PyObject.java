// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.NoSuchElementException;

public abstract class PyObject implements Comparable<PyObject> {
    // These all take and/or return boxed PyObjects
    public PyObject invert() { throw unimplementedMethod("invert"); }
    public PyObject pos() { throw unimplementedMethod("pos"); }
    public PyObject neg() { throw unimplementedMethod("neg"); }
    public PyObject abs() { throw unimplementedMethod("abs"); }

    public PyObject add(PyObject rhs) { throw unimplementedMethod("add"); }
    public PyObject and(PyObject rhs) { throw unimplementedMethod("and"); }
    public PyObject floordiv(PyObject rhs) { throw unimplementedMethod("floordiv"); }
    public PyObject lshift(PyObject rhs) { throw unimplementedMethod("lshift"); }
    public PyObject matmul(PyObject rhs) { throw unimplementedMethod("matmul"); }
    public PyObject mod(PyObject rhs) { throw unimplementedMethod("mod"); }
    public PyObject mul(PyObject rhs) { throw unimplementedMethod("mul"); }
    public PyObject or(PyObject rhs) { throw unimplementedMethod("or"); }
    public PyObject pow(PyObject rhs) { throw unimplementedMethod("pow"); }
    public PyObject rshift(PyObject rhs) { throw unimplementedMethod("rshift"); }
    public PyObject sub(PyObject rhs) { throw unimplementedMethod("sub"); }
    public PyObject truediv(PyObject rhs) { throw unimplementedMethod("truediv"); }
    public PyObject xor(PyObject rhs) { throw unimplementedMethod("xor"); }

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

    public boolean ge(PyObject rhs) { throw unimplementedMethod("ge"); }
    public boolean gt(PyObject rhs) { throw unimplementedMethod("gt"); }
    public boolean le(PyObject rhs) { throw unimplementedMethod("le"); }
    public boolean lt(PyObject rhs) { throw unimplementedMethod("lt"); }

    public PyObject getItem(PyObject key) { throw unimplementedMethod("getItem"); }
    public void setItem(PyObject key, PyObject value) { throw unimplementedMethod("setItem"); }
    public void delItem(PyObject key) { throw unimplementedMethod("delItem"); }

    public PyObject getAttr(String key) {
        switch (key) {
            case "__class__": return type();
            default: throw new NoSuchElementException("object does not have attribute " + PyString.reprOf(key));
        }
    }
    public void setAttr(String key, PyObject value) { throw unimplementedMethod("setAttr"); }
    public void delAttr(String key) { throw unimplementedMethod("delAttr"); }

    public PyObject call(PyObject args[], PyDict kwargs) { throw unimplementedMethod("call"); }

    public boolean hasIter() { return false; }
    public PyIter iter() {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object is not iterable");
    }
    public PyIter reversed() { throw unimplementedMethod("reversed"); }
    public PyObject next() {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object is not an iterator");
    }
    public abstract PyType type();

    public PyObject enter() { throw unimplementedMethod("enter"); }
    public void exit() { throw unimplementedMethod("exit"); }

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
    public boolean contains(PyObject rhs) { throw unimplementedMethod("contains"); }
    @Override public boolean equals(Object rhs) { throw unimplementedMethod("equals"); }
    public double floatValue() { throw unimplementedMethod("floatValue"); }
    public String format(String formatSpec) {
        if (!formatSpec.isEmpty()) {
            throw new UnsupportedOperationException(String.format("formatSpec=%s unimplemented on %s", PyString.reprOf(formatSpec), PyString.reprOf(type().name())));
        }
        return str();
    }
    @Override public int hashCode() { throw unimplementedMethod("hashCode"); }
    public boolean hasIndex() { return false; }
    public long indexValue() { throw unimplementedMethod("indexValue"); }
    public long intValue() { throw unimplementedMethod("intValue"); }
    public long len() { throw unimplementedMethod("len"); }
    public abstract String repr();
    public String str() { return repr(); }

    // Wrapper that reverses the order of evaluation vs. "contains" to assist translator
    public final boolean in(PyObject rhs) { return rhs.contains(this); }

    // Helpers used by derived classes
    protected UnsupportedOperationException unimplementedMethod(String method) {
        return new UnsupportedOperationException(String.format("%s unimplemented on %s", PyString.reprOf(method), PyString.reprOf(type().name())));
    }
    protected UnsupportedOperationException unimplementedBinOp(String op, PyObject rhs) {
        return new UnsupportedOperationException(String.format("%s %s %s is not implemented", type().name(), op, rhs.type().name()));
    }
    protected PyRaise raiseBinOp(String op, PyObject rhs) {
        return PyTypeError.raiseFormat("unsupported operand type(s) for %s: %s and %s", op, PyString.reprOf(type().name()), PyString.reprOf(rhs.type().name()));
    }
    protected String defaultRepr() { return "<" + type().name() + " object>"; }
}
