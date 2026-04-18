// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.nio.charset.Charset;
import java.util.Locale;

final class PyStringIter extends PyIter {
    private static final PyConcreteType type_singleton = new PyConcreteType("str_iterator", PyStringIter.class, PyObjectType.singleton, null);

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
    @Override public PyConcreteType type() { return type_singleton; }
};

public final class PyString extends PyObject {
    public static final PyString empty_singleton = new PyString("");
    private static final PyTuple constructor_positional_names = new PyTuple(new PyObject[] {new PyString("object"), new PyString("encoding"), new PyString("errors")});

    public final String value;

    PyString(String _value) { value = _value; }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        var boundArgs = Runtime.bindMinMaxPositionalOrKeyword(args, kwargs, type.name(), constructor_positional_names, 0, PyTuple.empty_singleton, 0, 3, 3, false, false);
        return newObjPositional(boundArgs.get(0), boundArgs.get(1), boundArgs.get(2));
    }
    public static PyString newObjPositional(PyObject object, PyObject encoding, PyObject errors) {
        if (object == null) {
            return PyString.empty_singleton;
        }
        if ((encoding == null) && (errors == null)) {
            return new PyString(object.str());
        }
        if (errors != null) {
            Runtime.requireErrorsArg(errors, "str");
        }
        if (object instanceof PyString) {
            throw PyTypeError.raise("decoding str is not supported");
        }
        if ((encoding == null) && (errors != null)) {
            throw PyTypeError.raise("decoding to str: need a bytes-like object, " + object.type().name() + " found");
        }
        byte[] objectBuffer = Runtime.getBytesLikeBuffer(object);
        if (objectBuffer == null) {
            throw PyTypeError.raise("decoding to str: need a bytes-like object, " + object.type().name() + " found");
        }
        String encodingStr = Runtime.requireEncodingArg(encoding, "str");
        Charset charset = Runtime.lookupCharset(encodingStr);
        return new PyString(new String(objectBuffer, charset));
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
        return PyRuntime.pyfunc_pyj_percent_format(this, rhs);
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
    @Override public PyConcreteType type() { return PyStringType.singleton; }

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
    @Override public String format(String formatSpec) {
        return PyRuntime.pyfunc_pyj_str_format(this, new PyString(formatSpec)).value;
    }
    @Override public String str() { return value; }
    public static String reprOf(String value) {
        boolean useDoubleQuotes = (value.indexOf('\'') >= 0) && (value.indexOf('"') < 0);
        var s = new StringBuilder(value.length() + 2).append(useDoubleQuotes ? "\"" : "'");
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
            } else if ((c == '\'') && !useDoubleQuotes) {
                s.append("\\'");
            } else {
                s.append(c);
            }
        }
        s.append(useDoubleQuotes ? '"' : '\'');
        return s.toString();
    }
    @Override public String repr() { return reprOf(value); }

    public PyInt pymethod_count(PyObject sub, PyObject start, PyObject end) {
        if (!(sub instanceof PyString subStr)) {
            throw PyTypeError.raise("must be str, not " + sub.type().name());
        }
        int n = value.length();
        int startIndex = Runtime.asSearchIndexAllowNone(start, 0, n);
        int endIndex = Math.min(Runtime.asSearchIndexAllowNone(end, n, n), n);
        if ((startIndex > endIndex) || (startIndex > n)) {
            return PyInt.singleton_0;
        }
        String needle = subStr.value;
        if (needle.isEmpty()) {
            return new PyInt(endIndex - startIndex + 1);
        }
        int count = 0;
        int i = startIndex;
        while (true) {
            i = value.indexOf(needle, i);
            if ((i < 0) || (i + needle.length() > endIndex)) {
                return new PyInt(count);
            }
            count++;
            i += needle.length();
        }
    }
    public PyInt pymethod_find(PyObject sub, PyObject start, PyObject end) {
        if (!(sub instanceof PyString subStr)) {
            throw PyTypeError.raise("find() argument 1 must be str, not " + sub.type().name());
        }
        int n = value.length();
        int startIndex = Runtime.asSearchIndexAllowNone(start, 0, n);
        int endIndex = Math.min(Runtime.asSearchIndexAllowNone(end, n, n), n);
        if (subStr.value.isEmpty()) {
            if ((startIndex > endIndex) || (startIndex > n)) {
                return PyInt.singleton_neg1;
            }
            return new PyInt(startIndex);
        }
        if (startIndex > n) {
            return PyInt.singleton_neg1;
        }
        int index = value.indexOf(subStr.value, startIndex);
        if ((index < 0) || (index + subStr.value.length() > endIndex)) {
            return PyInt.singleton_neg1;
        }
        return new PyInt(index);
    }
    public PyInt pymethod_index(PyObject sub, PyObject start, PyObject end) {
        PyInt ret = pymethod_find(sub, start, end);
        if (ret.value < 0) {
            throw PyValueError.raise("substring not found");
        }
        return ret;
    }
    public PyString pymethod_lower() { return new PyString(value.toLowerCase(Locale.ROOT)); }
    public PyList pymethod_split(PyObject sep, PyObject maxsplit) {
        long m = maxsplit.indexValue();
        if (sep == PyNone.singleton) {
            return splitWhitespace(m);
        }
        if (!(sep instanceof PyString sepStr)) {
            throw PyTypeError.raise("must be str or None, not " + sep.type().name());
        }
        if (sepStr.value.isEmpty()) {
            throw PyValueError.raise("empty separator");
        }
        return splitOnString(sepStr.value, m);
    }
    private static boolean splitWhitespaceChar(char c) {
        return (c == '\u0085') || Character.isWhitespace(c) || Character.isSpaceChar(c);
    }
    private PyList splitWhitespace(long maxsplit) {
        var ret = new PyList();
        int i = 0;
        int n = value.length();
        while ((i < n) && splitWhitespaceChar(value.charAt(i))) {
            i++;
        }
        if (i == n) {
            return ret;
        }
        if (maxsplit == 0) {
            ret.items.add(new PyString(value.substring(i)));
            return ret;
        }
        long remaining = maxsplit;
        while (i < n) {
            if (remaining == 0) {
                ret.items.add(new PyString(value.substring(i)));
                return ret;
            }
            int start = i;
            while ((i < n) && !splitWhitespaceChar(value.charAt(i))) {
                i++;
            }
            ret.items.add(new PyString(value.substring(start, i)));
            if (remaining > 0) {
                remaining--;
            }
            while ((i < n) && splitWhitespaceChar(value.charAt(i))) {
                i++;
            }
        }
        return ret;
    }
    private PyList splitOnString(String sep, long maxsplit) {
        var ret = new PyList();
        int start = 0;
        long remaining = maxsplit;
        while (true) {
            if (remaining == 0) {
                ret.items.add(new PyString(value.substring(start)));
                return ret;
            }
            int i = value.indexOf(sep, start);
            if (i == -1) {
                ret.items.add(new PyString(value.substring(start)));
                return ret;
            }
            ret.items.add(new PyString(value.substring(start, i)));
            start = i + sep.length();
            if (remaining > 0) {
                remaining--;
            }
        }
    }
    public PyBool pymethod_startswith(PyObject prefix, PyObject start, PyObject end) {
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
    public PyBool pymethod_endswith(PyObject suffix, PyObject start, PyObject end) {
        int startIndex = Runtime.asSearchIndexAllowNone(start, 0, value.length());
        Runtime.unsupportedSearchIndexAllowNone(end, "str.endswith() does not yet support 'end' argument");
        if (suffix instanceof PyString suffixStr) {
            int offset = value.length() - suffixStr.value.length();
            return PyBool.create((offset >= startIndex) && value.startsWith(suffixStr.value, offset));
        } else if (suffix instanceof PyTuple suffixTuple) {
            for (int i = 0; i < suffixTuple.items.length; i++) {
                var item = suffixTuple.items[i];
                if (item instanceof PyString itemStr) {
                    int offset = value.length() - itemStr.value.length();
                    if ((offset >= startIndex) && value.startsWith(itemStr.value, offset)) {
                        return PyBool.true_singleton;
                    }
                } else {
                    throw PyTypeError.raise("tuple for endswith must only contain str, not " + item.type().name());
                }
            }
            return PyBool.false_singleton;
        } else {
            throw PyTypeError.raise("endswith first arg must be str or a tuple of str, not " + suffix.type().name());
        }
    }
    public PyString pymethod_upper() { return new PyString(value.toUpperCase(Locale.ROOT)); }
    public PyBool pymethod_isdecimal() {
        if (value.isEmpty()) {
            return PyBool.false_singleton;
        }
        for (int i = 0; i < value.length(); ) {
            int cp = value.codePointAt(i);
            if (Character.getType(cp) != Character.DECIMAL_DIGIT_NUMBER) {
                return PyBool.false_singleton;
            }
            i += Character.charCount(cp);
        }
        return PyBool.true_singleton;
    }
    public PyBool pymethod_isdigit() {
        if (value.isEmpty()) {
            return PyBool.false_singleton;
        }
        for (int i = 0; i < value.length(); ) {
            int cp = value.codePointAt(i);
            boolean isDigit = Character.isDigit(cp);
            if (!isDigit && (Character.getType(cp) == Character.OTHER_NUMBER)) {
                int numericValue = Character.getNumericValue(cp);
                isDigit = (numericValue >= 0) && (numericValue <= 9);
            }
            if (!isDigit) {
                return PyBool.false_singleton;
            }
            i += Character.charCount(cp);
        }
        return PyBool.true_singleton;
    }

    public PyObject pymethod_format(PyObject[] args, PyDict kwargs) { throw new UnsupportedOperationException(); }
    public static PyObject pymethod_maketrans(PyObject x, PyObject y, PyObject z) { throw new UnsupportedOperationException(); }
}
