// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Set;

final class PyObjectType extends PyBuiltinType {
// BEGIN GENERATED CODE: PyObjectType
    public static final PyObjectType singleton = new PyObjectType();
    private static final PyString pyattr___doc__ = new PyString("The base class of the class hierarchy.\n\nWhen called, it accepts no arguments and returns a new featureless\ninstance that has no instance attributes and cannot be given any.\n");
    private static final PyAttr attrs[] = new PyAttr[] {
        new PyAttr("__doc__", pyattr___doc__)
    };
    @Override public PyAttr[] getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
// END GENERATED CODE: PyObjectType

    private PyObjectType() { super("object", PyObject.class); }
}

public abstract class PyObject implements Comparable<PyObject> {
    // These all take and/or return boxed PyObjects
    public PyObject invert() { throw raiseUnaryOp("unary ~"); }
    public PyObject pos() { throw raiseUnaryOp("unary +"); }
    public PyObject neg() { throw raiseUnaryOp("unary -"); }
    public PyObject abs() { throw raiseUnaryOp("abs()"); }

    public PyObject add(PyObject rhs) { return rhs.radd(this); }
    public PyObject and(PyObject rhs) { return rhs.rand(this); }
    public PyObject floorDiv(PyObject rhs) { return rhs.rfloorDiv(this); }
    public PyObject lshift(PyObject rhs) { return rhs.rlshift(this); }
    public PyObject matmul(PyObject rhs) { return rhs.rmatmul(this); }
    public PyObject mod(PyObject rhs) { return rhs.rmod(this); }
    public PyObject mul(PyObject rhs) { return rhs.rmul(this); }
    public PyObject or(PyObject rhs) { return rhs.ror(this); }
    public PyObject pow(PyObject rhs) { return rhs.rpow(this); }
    public PyObject rshift(PyObject rhs) { return rhs.rrshift(this); }
    public PyObject sub(PyObject rhs) { return rhs.rsub(this); }
    public PyObject trueDiv(PyObject rhs) { return rhs.rtrueDiv(this); }
    public PyObject xor(PyObject rhs) { return rhs.rxor(this); }

    // Reverse ops
    public PyObject radd(PyObject rhs) { throw rhs.raiseBinOp("+", this); }
    public PyObject rand(PyObject rhs) { throw rhs.raiseBinOp("&", this); }
    public PyObject rfloorDiv(PyObject rhs) { throw rhs.raiseBinOp("//", this); }
    public PyObject rlshift(PyObject rhs) { throw rhs.raiseBinOp("<<", this); }
    public PyObject rmatmul(PyObject rhs) { throw rhs.raiseBinOp("@", this); }
    public PyObject rmod(PyObject rhs) { throw rhs.raiseBinOp("%", this); }
    public PyObject rmul(PyObject rhs) { throw rhs.raiseBinOp("*", this); }
    public PyObject ror(PyObject rhs) { throw rhs.raiseBinOp("|", this); }
    public PyObject rpow(PyObject rhs) { throw rhs.raiseBinOp("** or pow()", this); }
    public PyObject rrshift(PyObject rhs) { throw rhs.raiseBinOp(">>", this); }
    public PyObject rsub(PyObject rhs) { throw rhs.raiseBinOp("-", this); }
    public PyObject rtrueDiv(PyObject rhs) { throw rhs.raiseBinOp("/", this); }
    public PyObject rxor(PyObject rhs) { throw rhs.raiseBinOp("^", this); }

    // By default, in-place ops map to regular ops
    public PyObject addInPlace(PyObject rhs) { return add(rhs); }
    public PyObject andInPlace(PyObject rhs) { return and(rhs); }
    public PyObject floorDivInPlace(PyObject rhs) { return floorDiv(rhs); }
    public PyObject lshiftInPlace(PyObject rhs) { return lshift(rhs); }
    public PyObject matmulInPlace(PyObject rhs) { return matmul(rhs); }
    public PyObject modInPlace(PyObject rhs) { return mod(rhs); }
    public PyObject mulInPlace(PyObject rhs) { return mul(rhs); }
    public PyObject orInPlace(PyObject rhs) { return or(rhs); }
    public PyObject powInPlace(PyObject rhs) { return pow(rhs); }
    public PyObject rshiftInPlace(PyObject rhs) { return rshift(rhs); }
    public PyObject subInPlace(PyObject rhs) { return sub(rhs); }
    public PyObject trueDivInPlace(PyObject rhs) { return trueDiv(rhs); }
    public PyObject xorInPlace(PyObject rhs) { return xor(rhs); }

    public boolean ge(PyObject rhs) { throw raiseCompareOp(">=", rhs); }
    public boolean gt(PyObject rhs) { throw raiseCompareOp(">", rhs); }
    public boolean le(PyObject rhs) { throw raiseCompareOp("<=", rhs); }
    public boolean lt(PyObject rhs) { throw raiseCompareOp("<", rhs); }

    public PyObject getItem(PyObject key) {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object is not subscriptable");
    }
    public void setItem(PyObject key, PyObject value) {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object does not support item assignment");
    }
    public void delItem(PyObject key) {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object does not support item deletion");
    }

    public PyObject getAttr(String key) {
        var desc = type().lookupAttr(key);
        if (desc != null) {
            return desc.get(this);
        }
        if (!key.startsWith("__")) { // we assume all of these are handled by descriptors or derived class
            throw raiseMissingAttr(key);
        }
        switch (key) {
            case "__class__": return type();
            default: throw new UnsupportedOperationException(PyString.reprOf(key) + " attribute is not handled");
        }
    }
    public void setAttr(String key, PyObject value) {
        var desc = type().lookupAttr(key);
        if (desc != null) {
            if (desc.isDataDescriptor()) {
                desc.set(this, value);
            } else {
                throw Runtime.raiseNamedReadOnlyAttr(type(), key);
            }
            return;
        }
        throw PyAttributeError.raiseFormat("%s object has no attribute %s and no __dict__ for setting new attributes", PyString.reprOf(type().name()), PyString.reprOf(key));
    }
    public void delAttr(String key) {
        var desc = type().lookupAttr(key);
        if (desc != null) {
            if (desc.isDataDescriptor()) {
                desc.delete(this);
            } else {
                throw Runtime.raiseNamedReadOnlyAttr(type(), key);
            }
            return;
        }
        throw PyAttributeError.raiseFormat("%s object has no attribute %s and no __dict__ for setting new attributes", PyString.reprOf(type().name()), PyString.reprOf(key));
    }
    public PyObject get(PyObject instance) { return this; }
    public boolean isDataDescriptor() { return false; }
    public void set(PyObject instance, PyObject value) { throw unimplementedMethod("set"); }
    public void delete(PyObject instance) { throw unimplementedMethod("delete"); }

    public PyObject call(PyObject args[], PyDict kwargs) {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object is not callable");
    }

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
    public Set<PyObject> asSetOrNull() { return null; }
    public abstract boolean boolValue();
    @Override public final int compareTo(PyObject rhs) {
        if (equals(rhs)) {
            return 0;
        }
        return lt(rhs) ? -1 : 1;
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
    public long len() {
        throw PyTypeError.raiseFormat("object of type %s has no len()", PyString.reprOf(type().name()));
    }
    public abstract String repr();
    public String str() { return repr(); }

    // Wrapper that reverses the order of evaluation vs. "contains" to assist translator
    public final boolean in(PyObject rhs) { return rhs.contains(this); }

    // Helpers used by derived classes
    protected final UnsupportedOperationException unimplementedMethod(String method) {
        return new UnsupportedOperationException(String.format("%s unimplemented on %s", PyString.reprOf(method), PyString.reprOf(type().name())));
    }
    protected final UnsupportedOperationException unimplementedAttr(String key) {
        return new UnsupportedOperationException(String.format("%s attribute unimplemented on %s", PyString.reprOf(key), PyString.reprOf(type().name())));
    }
    protected final PyRaise raiseUnaryOp(String op) {
        return PyTypeError.raiseFormat("bad operand type for %s: %s", op, PyString.reprOf(type().name()));
    }
    protected final PyRaise raiseBinOp(String op, PyObject rhs) {
        return PyTypeError.raiseFormat("unsupported operand type(s) for %s: %s and %s", op, PyString.reprOf(type().name()), PyString.reprOf(rhs.type().name()));
    }
    protected final PyRaise raiseCompareOp(String op, PyObject rhs) {
        return PyTypeError.raiseFormat("'%s' not supported between instances of %s and %s", op, PyString.reprOf(type().name()), PyString.reprOf(rhs.type().name()));
    }
    protected final PyRaise raiseUnhashable() {
        return PyTypeError.raise("unhashable type: " + PyString.reprOf(type().name()));
    }
    protected final PyRaise raiseMissingAttr(String key) {
        return PyAttributeError.raiseFormat("%s object has no attribute %s", PyString.reprOf(type().name()), PyString.reprOf(key));
    }
    protected final boolean defaultContains(PyObject rhs) {
        if (!hasIter()) {
            throw PyTypeError.raise("argument of type " + PyString.reprOf(type().name()) + " is not a container or iterable");
        }
        var iter = iter();
        for (var item = iter.next(); item != null; item = iter.next()) {
            if (item.equals(rhs)) {
                return true;
            }
        }
        return false;
    }
    protected final String defaultRepr() { return "<" + type().name() + " object>"; }
}
