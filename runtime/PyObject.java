// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Set;

public abstract class PyObject implements Comparable<PyObject> {
    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        throw new UnsupportedOperationException(type.name() + ".__new__() unimplemented");
    }

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

    public PyDict getInstanceDict() { return null; }

    public PyObject getAttr(String key) {
        var desc = type().lookupAttr(key);
        if ((desc != null) && desc.isDataDescriptor()) {
            return desc.get(this, type());
        }
        PyDict instanceDict = getInstanceDict();
        if (instanceDict != null) {
            PyObject value = instanceDict.items.get(new PyString(key));
            if (value != null) {
                return value;
            }
        }
        if (desc != null) {
            return desc.get(this, type());
        }
        if (key.startsWith("__")) {
            throw new UnsupportedOperationException(type().name() + "." + key + " is not implemented");
        }
        throw raiseMissingAttr(key);
    }
    public final void setAttr(String key, PyObject value) {
        type().lookupAttr("__setattr__").get(this, type()).call(new PyObject[]{new PyString(key), value}, null);
    }
    public final void delAttr(String key) {
        type().lookupAttr("__delattr__").get(this, type()).call(new PyObject[]{new PyString(key)}, null);
    }
    public PyObject get(PyObject obj, PyType owner) { return this; }
    public boolean isDataDescriptor() { return false; }
    public void set(PyObject obj, PyType owner, PyObject value) { throw unimplementedMethod("set"); }
    public void delete(PyObject obj, PyType owner) { throw unimplementedMethod("delete"); }

    static PyObject pyget___class__(PyObject obj) {
        return obj.type();
    }

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
    public boolean contains(PyObject rhs) {
        PyObject containsDesc = type().lookupAttr("__contains__");
        if (containsDesc != null) {
            return containsDesc.get(this, type()).call(new PyObject[]{rhs}, null).boolValue();
        }
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
    @Override public boolean equals(Object rhs) { throw unimplementedMethod("equals"); }
    public double floatValue() { throw unimplementedMethod("floatValue"); }
    public final String format(String formatSpec) {
        PyObject ret = type().getAttr("__format__").call(new PyObject[]{this, new PyString(formatSpec)}, null);
        if (!(ret instanceof PyString retStr)) {
            throw PyTypeError.raise("__format__ must return a str, not " + ret.type().name());
        }
        return retStr.value;
    }
    @Override public int hashCode() { throw unimplementedMethod("hashCode"); }
    public boolean hasIndex() { return false; }
    public long indexValue() {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object cannot be interpreted as an integer");
    }
    public long intValue() { throw unimplementedMethod("intValue"); }
    public long len() {
        throw PyTypeError.raise("object of type " + PyString.reprOf(type().name()) + " has no len()");
    }
    public abstract String repr();
    public String str() { return repr(); }

    public PyObject pymethod___bytes__() { throw new UnsupportedOperationException(type().name() + ".__bytes__() unimplemented"); }
    public PyObject pymethod___ceil__() { throw new UnsupportedOperationException(type().name() + ".__ceil__() unimplemented"); }
    public PyObject pymethod___dir__() { throw new UnsupportedOperationException(type().name() + ".__dir__() unimplemented"); }
    public PyObject pymethod___floor__() { throw new UnsupportedOperationException(type().name() + ".__floor__() unimplemented"); }
    public PyObject pymethod___getitem__(PyObject key) { throw new UnsupportedOperationException(type().name() + ".__getitem__() unimplemented"); }
    public PyObject pymethod___getnewargs__() { throw new UnsupportedOperationException(type().name() + ".__getnewargs__() unimplemented"); }
    public PyObject pymethod___getstate__() { throw new UnsupportedOperationException(type().name() + ".__getstate__() unimplemented"); }
    public PyObject pymethod___length_hint__() { throw new UnsupportedOperationException(type().name() + ".__length_hint__() unimplemented"); }
    public PyObject pymethod___reduce__() { throw new UnsupportedOperationException(type().name() + ".__reduce__() unimplemented"); }
    public PyObject pymethod___reduce_ex__(PyObject proto) { throw new UnsupportedOperationException(type().name() + ".__reduce_ex__() unimplemented"); }
    public PyObject pymethod___reversed__() { throw new UnsupportedOperationException(type().name() + ".__reversed__() unimplemented"); }
    public PyObject pymethod___round__(PyObject ndigits) { throw new UnsupportedOperationException(type().name() + ".__round__() unimplemented"); }
    public PyObject pymethod___setstate__(PyObject state) { throw new UnsupportedOperationException(type().name() + ".__setstate__() unimplemented"); }
    public PyObject pymethod___subclasses__() { throw new UnsupportedOperationException(type().name() + ".__subclasses__() unimplemented"); }
    public PyObject pymethod___trunc__() { throw new UnsupportedOperationException(type().name() + ".__trunc__() unimplemented"); }

    // Wrapper that reverses the order of evaluation vs. "contains" to assist translator
    public final boolean in(PyObject rhs) { return rhs.contains(this); }

    // Helpers used by derived classes
    protected final UnsupportedOperationException unimplementedMethod(String method) {
        return new UnsupportedOperationException(PyString.reprOf(method) + " unimplemented on " + PyString.reprOf(type().name()));
    }
    protected final PyRaise raiseUnaryOp(String op) {
        return PyTypeError.raise("bad operand type for " + op + ": " + PyString.reprOf(type().name()));
    }
    protected final PyRaise raiseBinOp(String op, PyObject rhs) {
        return PyTypeError.raise("unsupported operand type(s) for " + op + ": " + PyString.reprOf(type().name()) + " and " + PyString.reprOf(rhs.type().name()));
    }
    protected final PyRaise raiseCompareOp(String op, PyObject rhs) {
        return PyTypeError.raise("'" + op + "' not supported between instances of " + PyString.reprOf(type().name()) + " and " + PyString.reprOf(rhs.type().name()));
    }
    protected final PyRaise raiseUnhashable() {
        return PyTypeError.raise("unhashable type: " + PyString.reprOf(type().name()));
    }
    protected final PyRaise raiseMissingAttr(String key) {
        return PyAttributeError.raise(PyString.reprOf(type().name()) + " object has no attribute " + PyString.reprOf(key));
    }
    protected final int defaultHashCode() {
        return System.identityHashCode(this);
    }
    protected final String defaultRepr() { return "<" + type().name() + " object>"; }
    protected final String slotBasedRepr() {
        PyObject ret = type().getAttr("__repr__").call(new PyObject[]{this}, null);
        if (!(ret instanceof PyString retStr)) {
            throw PyTypeError.raise("__repr__ must return a str, not " + ret.type().name());
        }
        return retStr.value;
    }
}
