// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

public final class PyReversed extends PyIter {
    private final PyObject obj;
    private long i = 0;

    PyReversed(PyObject _obj) {
        obj = _obj;
        i = _obj.len() - 1;
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        PyString pyName = new PyString(type.name());
        PyRuntime.pyfunc_require_exact_positional(new PyInt(args.length), kwargs, pyName, pyName, PyInt.singleton_1, PyBool.true_singleton);
        return newObjPositional(args[0]);
    }
    public static PyObject newObjPositional(PyObject arg) {
        PyType argType = arg.type();
        PyObject reversedFunc = argType.lookupAttr("__reversed__");
        if ((reversedFunc != null) && (reversedFunc != PyNone.singleton)) {
            return reversedFunc.call(new PyObject[]{arg}, null);
        }
        if (argType.lookupAttr("__getitem__") == null) {
            throw PyTypeError.raise(PyString.reprOf(argType.name()) + " object is not reversible");
        }
        return new PyReversed(arg);
    }

    @Override public PyObject next() {
        if (i < 0) {
            return null;
        }
        long cur = i--;
        return obj.getItem(new PyInt(cur));
    }
    @Override public String repr() { return defaultRepr(); }
    @Override public PyConcreteType type() { return PyReversedType.singleton; }

    public PyObject pymethod___length_hint__() { throw new UnsupportedOperationException(); }
}
