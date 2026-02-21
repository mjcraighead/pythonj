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

    private static final class PyStringMethod_join extends PyBuiltinMethod<PyString> {
        PyStringMethod_join(PyString _self) { super(_self); }
        @Override public String methodName() { return "join"; }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "str.join");
            if (args.length != 1) {
                throw new IllegalArgumentException("str.join() takes 1 argument");
            }
            return self.pymethod_join(args[0]);
        }
    }
    private static final class PyStringMethod_lower extends PyBuiltinMethod<PyString> {
        PyStringMethod_lower(PyString _self) { super(_self); }
        @Override public String methodName() { return "lower"; }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "str.lower");
            if (args.length != 0) {
                throw new IllegalArgumentException("str.lower() takes 0 arguments");
            }
            return self.pymethod_lower();
        }
    }
    private static final class PyStringMethod_split extends PyBuiltinMethod<PyString> {
        PyStringMethod_split(PyString _self) { super(_self); }
        @Override public String methodName() { return "split"; }
        @Override public PyList call(PyObject[] args, PyDict kwargs) {
            if (args.length != 1) {
                throw new IllegalArgumentException("str.split() takes 1 argument");
            }
            if ((kwargs != null) && kwargs.boolValue()) {
                throw new IllegalArgumentException("str.split() does not accept kwargs");
            }
            return self.pymethod_split(args[0]);
        }
    }
    private static final class PyStringMethod_upper extends PyBuiltinMethod<PyString> {
        PyStringMethod_upper(PyString _self) { super(_self); }
        @Override public String methodName() { return "upper"; }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "str.upper");
            if (args.length != 0) {
                throw new IllegalArgumentException("str.upper() takes 0 arguments");
            }
            return self.pymethod_upper();
        }
    }

    public final String value;

    PyString(String _value) { value = _value; }

    @Override public PyString add(PyObject rhs) {
        return new PyString(value + ((PyString)rhs).value);
    }
    @Override public PyString mul(PyObject rhs) {
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

    @Override public final boolean hasIter() { return true; }
    @Override public PyStringIter iter() { return new PyStringIter(this); }
    @Override public PyBuiltinClass type() { return Runtime.pyglobal_str; }

    @Override public boolean boolValue() { return !value.isEmpty(); }
    @Override public boolean equals(Object rhsArg) {
        if (rhsArg instanceof PyString rhs) {
            return value.equals(rhs.value);
        }
        return false;
    }
    @Override public PyObject getAttr(String key) {
        switch (key) {
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
    public PyList pymethod_split(PyObject arg) {
        // XXX Implement zero-args case
        // XXX Implement delimiters not of length 1
        if (arg.len() != 1) {
            throw new UnsupportedOperationException("multi-character split tokens unsupported");
        }
        char split = ((PyString)arg).value.charAt(0);
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
