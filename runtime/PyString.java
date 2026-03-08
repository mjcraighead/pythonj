// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Locale;

public final class PyString extends PyObject {
    public static final PyString empty_singleton = new PyString("");

    private static final PyBuiltinClass iter_class_singleton = new PyBuiltinClass("str_iterator", PyStringIter.class);
    static final class PyStringIter extends PyIter {
        private final String s;
        private int index = 0;

        PyStringIter(PyString _s) { s = _s.value; }

        @Override public PyString next() {
            if (index >= s.length()) {
                return null;
            }
            var ret = new PyString(String.valueOf(s.charAt(index)));
            index++;
            return ret;
        }
        @Override public String repr() { return defaultRepr(); }
        @Override public PyBuiltinClass type() { return iter_class_singleton; }
    };

    private static final class PyStringMethod_find extends PyBuiltinMethod<PyString> {
        PyStringMethod_find(PyString _self) { super(_self); }
        @Override public String methodName() { return "find"; }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "str.find");
            Runtime.requireMinArgs(args, 1, "find");
            Runtime.requireMaxArgs(args, 3, "find");
            if (args.length != 1) {
                throw new UnsupportedOperationException("str.find() currently only supports 1 argument");
            }
            if (args[0] instanceof PyString arg0Str) {
                return self.pymethod_find(arg0Str);
            } else {
                throw new UnsupportedOperationException("str.find() only supports a str as argument");
            }
        }
    }
    private static final class PyStringMethod_join extends PyBuiltinMethod<PyString> {
        PyStringMethod_join(PyString _self) { super(_self); }
        @Override public String methodName() { return "join"; }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "str.join");
            Runtime.requireExactArgsAlt(args, 1, "str.join");
            return self.pymethod_join(args[0]);
        }
    }
    private static final class PyStringMethod_lower extends PyBuiltinMethod<PyString> {
        PyStringMethod_lower(PyString _self) { super(_self); }
        @Override public String methodName() { return "lower"; }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "str.lower");
            Runtime.requireExactArgsAlt(args, 0, "str.lower");
            return self.pymethod_lower();
        }
    }
    private static final class PyStringMethod_split extends PyBuiltinMethod<PyString> {
        PyStringMethod_split(PyString _self) { super(_self); }
        @Override public String methodName() { return "split"; }
        @Override public PyList call(PyObject[] args, PyDict kwargs) {
            if ((kwargs != null) && kwargs.boolValue()) { // XXX Handle more cases correctly here
                if (kwargs.len() > 2) {
                    throw PyTypeError.raiseFormat("split() takes at most 2 keyword arguments (%d given)", kwargs.len());
                }
                for (var x: kwargs.items.entrySet()) {
                    PyString key = (PyString)x.getKey(); // PyString validated at call site
                    if (!key.value.equals("sep") && !key.value.equals("maxsplit")) {
                        throw PyTypeError.raise("split() got an unexpected keyword argument " + key.repr());
                    }
                }
                throw new IllegalArgumentException("str.split() does not accept kwargs");
            }
            if (args.length > 2) {
                throw PyTypeError.raiseFormat("split() takes at most 2 arguments (%d given)", args.length);
            }
            PyObject sep = (args.length >= 1) ? args[0] : PyNone.singleton;
            PyObject maxsplit = (args.length >= 2) ? args[1] : PyInt.singleton_neg1;
            return self.pymethod_split(sep, maxsplit);
        }
    }
    private static final class PyStringMethod_upper extends PyBuiltinMethod<PyString> {
        PyStringMethod_upper(PyString _self) { super(_self); }
        @Override public String methodName() { return "upper"; }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "str.upper");
            Runtime.requireExactArgsAlt(args, 0, "str.upper");
            return self.pymethod_upper();
        }
    }

    public final String value;

    PyString(String _value) { value = _value; }

    @Override public PyString add(PyObject rhs) {
        if (rhs instanceof PyString rhsStr) {
            return new PyString(value + rhsStr.value);
        } else {
            throw PyTypeError.raise("can only concatenate str (not \"" + rhs.type().name() + "\") to str");
        }
    }
    @Override public PyString mul(PyObject rhs) {
        if (!rhs.hasIndex()) {
            throw PyTypeError.raise("can't multiply sequence by non-int of type " + PyString.reprOf(rhs.type().name()));
        }
        var s = new StringBuilder();
        long count = rhs.indexValue();
        for (long i = 0; i < count; i++) {
            s.append(value);
        }
        return new PyString(s.toString());
    }
    @Override public PyString mod(PyObject rhs) {
        var s = new StringBuilder();
        PyObject[] rhsArray;
        if (rhs instanceof PyTuple rhsTuple) {
            rhsArray = rhsTuple.items;
        } else {
            rhsArray = new PyObject[] {rhs};
        }
        int argIndex = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '%') {
                c = value.charAt(++i);
                boolean hasLeadingZero = false;
                while (c == '0') {
                    hasLeadingZero = true;
                    c = value.charAt(++i);
                }
                int width = 0;
                if ((c >= '1') && (c <= '9')) {
                    // XXX We only allow 1 width character right now
                    width = c - '0';
                    c = value.charAt(++i);
                }
                PyObject arg = rhsArray[argIndex++];
                if (c == 's') {
                    if (hasLeadingZero || (width != 0)) {
                        throw new UnsupportedOperationException("width for %s is unimplemented");
                    }
                    s.append(arg.str());
                } else if (c == 'r') {
                    if (hasLeadingZero || (width != 0)) {
                        throw new UnsupportedOperationException("width for %r is unimplemented");
                    }
                    s.append(arg.repr());
                } else if ((c == 'd') || (c == 'x') || (c == 'X')) {
                    // XXX Negative hex values are being printed wrong
                    String fmt = "%" + (hasLeadingZero ? "0" : "") + ((width != 0) ? width : "") + c;
                    s.append(String.format(fmt, ((PyInt)arg).value));
                } else {
                    throw new UnsupportedOperationException("don't know how to implement format specifier");
                }
            } else {
                s.append(c);
            }
        }
        return new PyString(s.toString());
    }
    @Override public PyString rmul(PyObject rhs) { return mul(rhs); }

    // Note some divergences vs. CPython when comparing surrogate pairs vs. private-use BMP chars
    @Override public boolean ge(PyObject rhs) {
        if (rhs instanceof PyString rhsStr) {
            return value.compareTo(rhsStr.value) >= 0;
        } else {
            return super.ge(rhs);
        }
    }
    @Override public boolean gt(PyObject rhs) {
        if (rhs instanceof PyString rhsStr) {
            return value.compareTo(rhsStr.value) > 0;
        } else {
            return super.gt(rhs);
        }
    }
    @Override public boolean le(PyObject rhs) {
        if (rhs instanceof PyString rhsStr) {
            return value.compareTo(rhsStr.value) <= 0;
        } else {
            return super.le(rhs);
        }
    }
    @Override public boolean lt(PyObject rhs) {
        if (rhs instanceof PyString rhsStr) {
            return value.compareTo(rhsStr.value) < 0;
        } else {
            return super.lt(rhs);
        }
    }

    @Override public PyString getItem(PyObject key) {
        int index = Math.toIntExact(key.indexValue());
        int length = value.length();
        if ((index < -length) || (index >= length)) {
            throw PyIndexError.raise("string index out of range");
        }
        if (index < 0) {
            index += length;
        }
        return new PyString(String.valueOf(value.charAt(index)));
    }
    @Override public void delItem(PyObject key) {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object doesn't support item deletion");
    }

    @Override public final boolean hasIter() { return true; }
    @Override public PyStringIter iter() { return new PyStringIter(this); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_str; }

    @Override public boolean boolValue() { return !value.isEmpty(); }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PyString rhsStr) {
            return value.equals(rhsStr.value);
        }
        return false;
    }
    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "find": return new PyStringMethod_find(this);
            case "join": return new PyStringMethod_join(this);
            case "lower": return new PyStringMethod_lower(this);
            case "split": return new PyStringMethod_split(this);
            case "upper": return new PyStringMethod_upper(this);
            default: return super.getAttr(key);
        }
    }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public long len() { return value.length(); }
    @Override public String str() { return value; }
    public static String reprOf(String value) {
        boolean use_double_quotes = (value.indexOf('\'') >= 0) && (value.indexOf('"') < 0);
        var s = new StringBuilder(use_double_quotes ? "\"" : "'");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n') {
                s.append("\\n");
            } else if (c == '\r') {
                s.append("\\r");
            } else if (c == '\t') {
                s.append("\\t");
            } else if ((c < 0x20) || ((c >= 0x7F) && (c <= 0xA0)) || (c == 0xAD)) { // XXX This logic is incomplete
                s.append("\\x");
                s.append("0123456789abcdef".charAt(c >> 4));
                s.append("0123456789abcdef".charAt(c & 15));
            } else if (c == '\\') {
                s.append("\\\\");
            } else if ((c == '\'') && !use_double_quotes) {
                s.append("\\'");
            } else {
                s.append(c);
            }
        }
        s.append(use_double_quotes ? '"' : '\'');
        return s.toString();
    }
    @Override public String repr() { return reprOf(value); }

    public PyInt pymethod_find(PyString arg) {
        return new PyInt(value.indexOf(arg.value));
    }
    public PyString pymethod_join(PyObject arg) {
        // XXX consider special casing value.isEmpty() for performance
        var s = new StringBuilder();
        var iter = arg.iter();
        boolean first = true;
        for (var item = iter.next(); item != null; item = iter.next()) {
            if (first) {
                first = false;
            } else {
                s.append(value);
            }
            s.append(((PyString)item).value);
        }
        return new PyString(s.toString());
    }
    public PyString pymethod_lower() { return new PyString(value.toLowerCase(Locale.ROOT)); }
    public PyList pymethod_split(PyObject sep, PyObject maxsplit) {
        if (sep == PyNone.singleton) {
            throw new UnsupportedOperationException("sep=None unsupported");
        }
        if (!maxsplit.hasIndex()) {
            throw PyTypeError.raise(PyString.reprOf(maxsplit.type().name()) + " object cannot be interpreted as an integer");
        }
        if (!(sep instanceof PyString sepStr)) {
            throw PyTypeError.raise("must be str or None, not " + sep.type().name());
        }
        if (sepStr.len() != 1) {
            throw new UnsupportedOperationException("multi-character split tokens unsupported");
        }
        long m = maxsplit.indexValue();
        if (m != -1) {
            throw new UnsupportedOperationException("maxsplit=-1 is the only value supported");
        }
        char split = sepStr.value.charAt(0);
        var ret = new PyList();
        var s = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == split) {
                ret.items.add(new PyString(s.toString()));
                s = new StringBuilder();
            } else {
                s.append(c);
            }
        }
        ret.items.add(new PyString(s.toString()));
        return ret;
    }
    public PyString pymethod_upper() { return new PyString(value.toUpperCase(Locale.ROOT)); }
}
