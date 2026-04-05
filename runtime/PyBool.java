// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

// Note intentional compatibility break: this is not a subclass of PyInt, so:
// isinstance(False, int) -> False
// issubclass(bool, int) -> False
public final class PyBool extends PyObject {
    public static final PyBool false_singleton = new PyBool(false);
    public static final PyBool true_singleton = new PyBool(true);

    public final boolean value;

    private PyBool(boolean _value) { value = _value; }
    public static PyBool create(boolean value) {
        return value ? true_singleton : false_singleton;
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        if (args.length > 1) {
            throw Runtime.raiseMaxArgs(args, 1, type.name());
        }
        return (args.length == 1) ? newObjPositional(args[0]) : newObjPositional(null);
    }
    public static PyObject newObjPositional(PyObject arg) {
        return (arg != null) ? PyBool.create(arg.boolValue()) : PyBool.false_singleton;
    }

    protected int asInt() { return value ? 1 : 0; }

    @Override public PyInt invert() { throw unimplementedMethod("invert"); }
    @Override public PyInt pos() { return new PyInt(asInt()); }
    @Override public PyInt neg() { return new PyInt(value ? -1 : 0); }
    @Override public PyInt abs() { return new PyInt(asInt()); }

    @Override public PyObject add(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(Math.addExact(asInt(), rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(asInt() + rhsBool.asInt());
        } else {
            return super.add(rhs);
        }
    }
    @Override public PyObject and(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(asInt() & rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return create(value & rhsBool.value);
        } else {
            return super.and(rhs);
        }
    }
    @Override public PyObject floorDiv(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(PyInt.floorDiv(asInt(), rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(PyInt.floorDiv(asInt(), rhsBool.asInt()));
        } else {
            return super.floorDiv(rhs);
        }
    }
    @Override public PyObject lshift(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(PyInt.lshift(asInt(), rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(PyInt.lshift(asInt(), rhsBool.asInt()));
        } else {
            return super.lshift(rhs);
        }
    }
    @Override public PyObject mod(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(PyInt.mod(asInt(), rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(PyInt.mod(asInt(), rhsBool.asInt()));
        } else {
            return super.mod(rhs);
        }
    }
    @Override public PyObject mul(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(asInt() * rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(asInt() * rhsBool.asInt());
        } else {
            return super.mul(rhs);
        }
    }
    @Override public PyObject or(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(asInt() | rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return create(value | rhsBool.value);
        } else {
            return super.or(rhs);
        }
    }
    @Override public PyObject pow(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(PyInt.pow(asInt(), rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(PyInt.pow(asInt(), rhsBool.asInt()));
        } else {
            return super.pow(rhs);
        }
    }
    @Override public PyObject rshift(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(PyInt.rshift(asInt(), rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(PyInt.rshift(asInt(), rhsBool.asInt()));
        } else {
            return super.rshift(rhs);
        }
    }
    @Override public PyObject sub(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(Math.subtractExact(asInt(), rhsInt.value));
        } else if (rhs instanceof PyBool rhsBool) {
            return new PyInt(asInt() - rhsBool.asInt());
        } else {
            return super.sub(rhs);
        }
    }
    @Override public PyObject trueDiv(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            throw unimplementedMethod("trueDiv");
        } else {
            return super.trueDiv(rhs);
        }
    }
    @Override public PyObject xor(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return new PyInt(asInt() ^ rhsInt.value);
        } else if (rhs instanceof PyBool rhsBool) {
            return create(value ^ rhsBool.value);
        } else {
            return super.xor(rhs);
        }
    }

    @Override public boolean ge(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return asInt() >= rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return asInt() >= rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return asInt() >= rhsFloat.value;
        } else {
            return super.ge(rhs);
        }
    }
    @Override public boolean gt(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return asInt() > rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return asInt() > rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return asInt() > rhsFloat.value;
        } else {
            return super.gt(rhs);
        }
    }
    @Override public boolean le(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return asInt() <= rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return asInt() <= rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return asInt() <= rhsFloat.value;
        } else {
            return super.le(rhs);
        }
    }
    @Override public boolean lt(PyObject rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return asInt() < rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return asInt() < rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return asInt() < rhsFloat.value;
        } else {
            return super.lt(rhs);
        }
    }

    @Override public PyBoolType type() { return PyBoolType.singleton; }

    @Override public boolean boolValue() { return value; }
    @Override public boolean contains(PyObject rhs) { return defaultContains(rhs); }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PyInt rhsInt) {
            return asInt() == rhsInt.value;
        } else if (rhs instanceof PyBool rhsBool) {
            return asInt() == rhsBool.asInt();
        } else if (rhs instanceof PyFloat rhsFloat) {
            return floatValue() == rhsFloat.value;
        } else {
            return false;
        }
    }
    @Override public double floatValue() { return asInt(); }
    @Override public String format(String formatSpec) {
        return ((PyString)PyRuntimePythonImpl.pyfunc_pyj_bool_format(this, new PyString(formatSpec))).value;
    }
    @Override public int hashCode() { return Runtime.hashRational(asInt(), 1); }
    @Override public boolean hasIndex() { return true; }
    @Override public long indexValue() { return asInt(); }
    @Override public long intValue() { return asInt(); }
    @Override public String repr() { return value ? "True" : "False"; }
}
