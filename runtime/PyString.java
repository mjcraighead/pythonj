// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Locale;

public final class PyString extends PyObject {
    public static final PyString empty_singleton = new PyString("");

    static final class PyStringIter extends PyIter {
        private static final PyBuiltinType type_singleton = new PyBuiltinType("str_iterator", PyStringIter.class);

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
        @Override public PyBuiltinType type() { return type_singleton; }
    };

    public final String value;

    PyString(String _value) { value = _value; }

    public static PyObject newObj(PyBuiltinType type, PyObject[] args, PyDict kwargs) {
        if (args.length > 1) {
            throw new IllegalArgumentException("str() takes 0 or 1 arguments");
        }
        if ((kwargs != null) && kwargs.boolValue()) {
            throw new IllegalArgumentException("str() does not accept kwargs");
        }
        if (args.length == 1) {
            return new PyString(args[0].str());
        }
        return PyString.empty_singleton;
    }

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
        if (key instanceof PySlice slice) {
            PySlice.Indices indices = slice.computeIndices(value.length());
            int index = indices.start();
            int step = indices.step();
            int n = indices.length();
            if (step == 1) {
                return new PyString(value.substring(index, index + n));
            } else {
                var s = new StringBuilder(n);
                for (int i = 0; i < n; i++) {
                    s.append(value.charAt(index));
                    index += step;
                }
                return new PyString(s.toString());
            }
        } else if (key.hasIndex()) {
            int index = Math.toIntExact(key.indexValue());
            int length = value.length();
            if ((index < -length) || (index >= length)) {
                throw PyIndexError.raise("string index out of range");
            }
            if (index < 0) {
                index += length;
            }
            return new PyString(String.valueOf(value.charAt(index)));
        } else {
            throw PyTypeError.raise("string indices must be integers, not " + PyString.reprOf(key.type().name()));
        }
    }
    @Override public void delItem(PyObject key) {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object doesn't support item deletion");
    }

    @Override public final boolean hasIter() { return true; }
    @Override public PyStringIter iter() { return new PyStringIter(this); }
    @Override public PyBuiltinType type() { return PyStringType.singleton; }

    @Override public boolean boolValue() { return !value.isEmpty(); }
    @Override public boolean contains(PyObject rhs) {
        if (rhs instanceof PyString rhsStr) {
            return value.contains(rhsStr.value);
        } else {
            throw PyTypeError.raise("'in <string>' requires string as left operand, not " + rhs.type().name());
        }
    }
    @Override public boolean equals(Object rhs) {
        if (rhs instanceof PyString rhsStr) {
            return value.equals(rhsStr.value);
        }
        return false;
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

    public PyInt pymethod_find(PyObject sub, PyObject start, PyObject end) {
        if (!(sub instanceof PyString subStr)) {
            throw PyTypeError.raise("find() argument 1 must be str, not " + sub.type().name());
        }
        Runtime.unsupportedSearchIndexAllowNone(start, "str.find() does not yet support 'start' argument");
        Runtime.unsupportedSearchIndexAllowNone(end, "str.find() does not yet support 'end' argument");
        return new PyInt(value.indexOf(subStr.value));
    }
    public PyString pymethod_join(PyObject arg) {
        // XXX consider special casing value.isEmpty() for performance
        var s = new StringBuilder();
        if (!arg.hasIter()) {
            throw PyTypeError.raise("can only join an iterable");
        }
        var iter = arg.iter();
        long index = 0;
        for (var item = iter.next(); item != null; item = iter.next(), index++) {
            if (index != 0) {
                s.append(value);
            }
            if (item instanceof PyString itemStr) {
                s.append(itemStr.value);
            } else {
                throw PyTypeError.raiseFormat("sequence item %d: expected str instance, %s found", index, item.type().name());
            }
        }
        return new PyString(s.toString());
    }
    public PyString pymethod_lower() { return new PyString(value.toLowerCase(Locale.ROOT)); }
    public PyList splitImpl(PyObject sep, PyObject maxsplit) {
        if (sep == PyNone.singleton) {
            throw new UnsupportedOperationException("sep=None unsupported");
        }
        long m = maxsplit.indexValue();
        if (!(sep instanceof PyString sepStr)) {
            throw PyTypeError.raise("must be str or None, not " + sep.type().name());
        }
        if (sepStr.len() != 1) {
            throw new UnsupportedOperationException("multi-character split tokens unsupported");
        }
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
    public PyList pymethod_split(PyObject[] args, PyDict kwargs) {
        PyObject sep = null;
        PyObject maxsplit = null;
        int argsLength = args.length;
        if ((kwargs != null) && kwargs.boolValue()) {
            if ((argsLength == 0) && (kwargs.len() > 2)) {
                throw Runtime.raiseAtMostKwArgs("split", 2, kwargs.len());
            }
            if (argsLength + kwargs.len() > 2) {
                throw Runtime.raiseAtMostArgs("split", 2, argsLength + kwargs.len());
            }
        } else if (argsLength > 2) {
            throw Runtime.raiseAtMostArgs("split", 2, argsLength);
        }
        if (argsLength >= 1) {
            sep = args[0];
        }
        if (argsLength >= 2) {
            maxsplit = args[1];
        }
        if ((kwargs != null) && kwargs.boolValue()) {
            for (var x: kwargs.items.entrySet()) {
                PyString key = (PyString)x.getKey(); // PyString validated at call site
                if (key.value.equals("sep")) {
                    if (sep != null) {
                        throw PyTypeError.raise("argument for split() given by name ('sep') and position (1)");
                    }
                    sep = x.getValue();
                } else if (key.value.equals("maxsplit")) {
                    maxsplit = x.getValue();
                } else {
                    throw Runtime.raiseUnexpectedKwArg("split", key.value);
                }
            }
        }
        if (sep == null) {
            sep = PyNone.singleton;
        }
        if (maxsplit == null) {
            maxsplit = PyInt.singleton_neg1;
        }
        return splitImpl(sep, maxsplit);
    }
    public PyBool pymethod_startswith(PyObject prefix, PyObject start, PyObject end) {
        int length = value.length();
        int startIndex = Runtime.asSearchIndexAllowNone(start, 0, value.length());
        Runtime.unsupportedSearchIndexAllowNone(end, "str.startswith() does not yet support 'end' argument");
        if (prefix instanceof PyString prefixStr) {
            return PyBool.create(value.startsWith(prefixStr.value, startIndex));
        } else if (prefix instanceof PyTuple prefixTuple) {
            for (int i = 0; i < prefixTuple.items.length; i++) {
                var item = prefixTuple.items[i];
                if (item instanceof PyString itemStr) {
                    if (value.startsWith(itemStr.value, startIndex)) {
                        return PyBool.true_singleton;
                    }
                } else {
                    throw PyTypeError.raise("tuple for startswith must only contain str, not " + item.type().name());
                }
            }
            return PyBool.false_singleton;
        } else {
            throw PyTypeError.raise("startswith first arg must be str or a tuple of str, not " + prefix.type().name());
        }
    }
    public PyString pymethod_upper() { return new PyString(value.toUpperCase(Locale.ROOT)); }

    public PyObject pymethod_capitalize() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_casefold() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isalnum() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isalpha() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isascii() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isdecimal() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isdigit() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isidentifier() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_islower() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isnumeric() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isprintable() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isspace() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_istitle() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isupper() { throw new UnsupportedOperationException(); }
    public static PyObject pymethod_maketrans(PyObject x, PyObject y, PyObject z) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_swapcase() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_title() { throw new UnsupportedOperationException(); }
}
