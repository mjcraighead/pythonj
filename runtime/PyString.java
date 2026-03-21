// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.util.Locale;

final class PyStringType extends PyBuiltinClass {
    public static final PyStringType singleton = new PyStringType();
    private static final PyMethodDescriptor pydesc_capitalize = new PyMethodDescriptor(singleton, "capitalize", obj -> new PyString.PyStringMethodUnimplemented(obj, "capitalize"));
    private static final PyMethodDescriptor pydesc_casefold = new PyMethodDescriptor(singleton, "casefold", obj -> new PyString.PyStringMethodUnimplemented(obj, "casefold"));
    private static final PyMethodDescriptor pydesc_center = new PyMethodDescriptor(singleton, "center", obj -> new PyString.PyStringMethodUnimplemented(obj, "center"));
    private static final PyMethodDescriptor pydesc_count = new PyMethodDescriptor(singleton, "count", obj -> new PyString.PyStringMethodUnimplemented(obj, "count"));
    private static final PyMethodDescriptor pydesc_encode = new PyMethodDescriptor(singleton, "encode", obj -> new PyString.PyStringMethodUnimplemented(obj, "encode"));
    private static final PyMethodDescriptor pydesc_endswith = new PyMethodDescriptor(singleton, "endswith", obj -> new PyString.PyStringMethodUnimplemented(obj, "endswith"));
    private static final PyMethodDescriptor pydesc_expandtabs = new PyMethodDescriptor(singleton, "expandtabs", obj -> new PyString.PyStringMethodUnimplemented(obj, "expandtabs"));
    private static final PyMethodDescriptor pydesc_find = new PyMethodDescriptor(singleton, "find", PyString.PyStringMethod_find::new);
    private static final PyMethodDescriptor pydesc_format = new PyMethodDescriptor(singleton, "format", obj -> new PyString.PyStringMethodUnimplemented(obj, "format"));
    private static final PyMethodDescriptor pydesc_format_map = new PyMethodDescriptor(singleton, "format_map", obj -> new PyString.PyStringMethodUnimplemented(obj, "format_map"));
    private static final PyMethodDescriptor pydesc_index = new PyMethodDescriptor(singleton, "index", obj -> new PyString.PyStringMethodUnimplemented(obj, "index"));
    private static final PyMethodDescriptor pydesc_isalnum = new PyMethodDescriptor(singleton, "isalnum", obj -> new PyString.PyStringMethodUnimplemented(obj, "isalnum"));
    private static final PyMethodDescriptor pydesc_isalpha = new PyMethodDescriptor(singleton, "isalpha", obj -> new PyString.PyStringMethodUnimplemented(obj, "isalpha"));
    private static final PyMethodDescriptor pydesc_isascii = new PyMethodDescriptor(singleton, "isascii", obj -> new PyString.PyStringMethodUnimplemented(obj, "isascii"));
    private static final PyMethodDescriptor pydesc_isdecimal = new PyMethodDescriptor(singleton, "isdecimal", obj -> new PyString.PyStringMethodUnimplemented(obj, "isdecimal"));
    private static final PyMethodDescriptor pydesc_isdigit = new PyMethodDescriptor(singleton, "isdigit", obj -> new PyString.PyStringMethodUnimplemented(obj, "isdigit"));
    private static final PyMethodDescriptor pydesc_isidentifier = new PyMethodDescriptor(singleton, "isidentifier", obj -> new PyString.PyStringMethodUnimplemented(obj, "isidentifier"));
    private static final PyMethodDescriptor pydesc_islower = new PyMethodDescriptor(singleton, "islower", obj -> new PyString.PyStringMethodUnimplemented(obj, "islower"));
    private static final PyMethodDescriptor pydesc_isnumeric = new PyMethodDescriptor(singleton, "isnumeric", obj -> new PyString.PyStringMethodUnimplemented(obj, "isnumeric"));
    private static final PyMethodDescriptor pydesc_isprintable = new PyMethodDescriptor(singleton, "isprintable", obj -> new PyString.PyStringMethodUnimplemented(obj, "isprintable"));
    private static final PyMethodDescriptor pydesc_isspace = new PyMethodDescriptor(singleton, "isspace", obj -> new PyString.PyStringMethodUnimplemented(obj, "isspace"));
    private static final PyMethodDescriptor pydesc_istitle = new PyMethodDescriptor(singleton, "istitle", obj -> new PyString.PyStringMethodUnimplemented(obj, "istitle"));
    private static final PyMethodDescriptor pydesc_isupper = new PyMethodDescriptor(singleton, "isupper", obj -> new PyString.PyStringMethodUnimplemented(obj, "isupper"));
    private static final PyMethodDescriptor pydesc_join = new PyMethodDescriptor(singleton, "join", PyString.PyStringMethod_join::new);
    private static final PyMethodDescriptor pydesc_ljust = new PyMethodDescriptor(singleton, "ljust", obj -> new PyString.PyStringMethodUnimplemented(obj, "ljust"));
    private static final PyMethodDescriptor pydesc_lower = new PyMethodDescriptor(singleton, "lower", PyString.PyStringMethod_lower::new);
    private static final PyMethodDescriptor pydesc_lstrip = new PyMethodDescriptor(singleton, "lstrip", obj -> new PyString.PyStringMethodUnimplemented(obj, "lstrip"));
    private static final PyStaticMethod pydesc_maketrans = new PyStaticMethod(singleton, "maketrans", new PyStringType.PyStringStaticMethod_maketrans(singleton));
    private static final PyMethodDescriptor pydesc_partition = new PyMethodDescriptor(singleton, "partition", obj -> new PyString.PyStringMethodUnimplemented(obj, "partition"));
    private static final PyMethodDescriptor pydesc_removeprefix = new PyMethodDescriptor(singleton, "removeprefix", obj -> new PyString.PyStringMethodUnimplemented(obj, "removeprefix"));
    private static final PyMethodDescriptor pydesc_removesuffix = new PyMethodDescriptor(singleton, "removesuffix", obj -> new PyString.PyStringMethodUnimplemented(obj, "removesuffix"));
    private static final PyMethodDescriptor pydesc_replace = new PyMethodDescriptor(singleton, "replace", obj -> new PyString.PyStringMethodUnimplemented(obj, "replace"));
    private static final PyMethodDescriptor pydesc_rfind = new PyMethodDescriptor(singleton, "rfind", obj -> new PyString.PyStringMethodUnimplemented(obj, "rfind"));
    private static final PyMethodDescriptor pydesc_rindex = new PyMethodDescriptor(singleton, "rindex", obj -> new PyString.PyStringMethodUnimplemented(obj, "rindex"));
    private static final PyMethodDescriptor pydesc_rjust = new PyMethodDescriptor(singleton, "rjust", obj -> new PyString.PyStringMethodUnimplemented(obj, "rjust"));
    private static final PyMethodDescriptor pydesc_rpartition = new PyMethodDescriptor(singleton, "rpartition", obj -> new PyString.PyStringMethodUnimplemented(obj, "rpartition"));
    private static final PyMethodDescriptor pydesc_rsplit = new PyMethodDescriptor(singleton, "rsplit", obj -> new PyString.PyStringMethodUnimplemented(obj, "rsplit"));
    private static final PyMethodDescriptor pydesc_rstrip = new PyMethodDescriptor(singleton, "rstrip", obj -> new PyString.PyStringMethodUnimplemented(obj, "rstrip"));
    private static final PyMethodDescriptor pydesc_split = new PyMethodDescriptor(singleton, "split", PyString.PyStringMethod_split::new);
    private static final PyMethodDescriptor pydesc_splitlines = new PyMethodDescriptor(singleton, "splitlines", obj -> new PyString.PyStringMethodUnimplemented(obj, "splitlines"));
    private static final PyMethodDescriptor pydesc_startswith = new PyMethodDescriptor(singleton, "startswith", PyString.PyStringMethod_startswith::new);
    private static final PyMethodDescriptor pydesc_strip = new PyMethodDescriptor(singleton, "strip", obj -> new PyString.PyStringMethodUnimplemented(obj, "strip"));
    private static final PyMethodDescriptor pydesc_swapcase = new PyMethodDescriptor(singleton, "swapcase", obj -> new PyString.PyStringMethodUnimplemented(obj, "swapcase"));
    private static final PyMethodDescriptor pydesc_title = new PyMethodDescriptor(singleton, "title", obj -> new PyString.PyStringMethodUnimplemented(obj, "title"));
    private static final PyMethodDescriptor pydesc_translate = new PyMethodDescriptor(singleton, "translate", obj -> new PyString.PyStringMethodUnimplemented(obj, "translate"));
    private static final PyMethodDescriptor pydesc_upper = new PyMethodDescriptor(singleton, "upper", PyString.PyStringMethod_upper::new);
    private static final PyMethodDescriptor pydesc_zfill = new PyMethodDescriptor(singleton, "zfill", obj -> new PyString.PyStringMethodUnimplemented(obj, "zfill"));

    private PyStringType() { super("str", PyString.class); }
    @Override public PyDescriptor getDescriptor(String name) {
        switch (name) {
            case "capitalize": return pydesc_capitalize;
            case "casefold": return pydesc_casefold;
            case "center": return pydesc_center;
            case "count": return pydesc_count;
            case "encode": return pydesc_encode;
            case "endswith": return pydesc_endswith;
            case "expandtabs": return pydesc_expandtabs;
            case "find": return pydesc_find;
            case "format": return pydesc_format;
            case "format_map": return pydesc_format_map;
            case "index": return pydesc_index;
            case "isalnum": return pydesc_isalnum;
            case "isalpha": return pydesc_isalpha;
            case "isascii": return pydesc_isascii;
            case "isdecimal": return pydesc_isdecimal;
            case "isdigit": return pydesc_isdigit;
            case "isidentifier": return pydesc_isidentifier;
            case "islower": return pydesc_islower;
            case "isnumeric": return pydesc_isnumeric;
            case "isprintable": return pydesc_isprintable;
            case "isspace": return pydesc_isspace;
            case "istitle": return pydesc_istitle;
            case "isupper": return pydesc_isupper;
            case "join": return pydesc_join;
            case "ljust": return pydesc_ljust;
            case "lower": return pydesc_lower;
            case "lstrip": return pydesc_lstrip;
            case "maketrans": return pydesc_maketrans;
            case "partition": return pydesc_partition;
            case "removeprefix": return pydesc_removeprefix;
            case "removesuffix": return pydesc_removesuffix;
            case "replace": return pydesc_replace;
            case "rfind": return pydesc_rfind;
            case "rindex": return pydesc_rindex;
            case "rjust": return pydesc_rjust;
            case "rpartition": return pydesc_rpartition;
            case "rsplit": return pydesc_rsplit;
            case "rstrip": return pydesc_rstrip;
            case "split": return pydesc_split;
            case "splitlines": return pydesc_splitlines;
            case "startswith": return pydesc_startswith;
            case "strip": return pydesc_strip;
            case "swapcase": return pydesc_swapcase;
            case "title": return pydesc_title;
            case "translate": return pydesc_translate;
            case "upper": return pydesc_upper;
            case "zfill": return pydesc_zfill;
            default: return null;
        }
    }
    @Override public PyString call(PyObject[] args, PyDict kwargs) {
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

    static final class PyStringStaticMethod_maketrans extends PyBuiltinMethod<PyType> {
        PyStringStaticMethod_maketrans(PyType _self) { super(_self); }
        @Override public String methodName() { return "maketrans"; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("str.maketrans unimplemented");
        }
    }
}

public final class PyString extends PyObject {
    public static final PyString empty_singleton = new PyString("");

    static final class PyStringIter extends PyIter {
        private static final PyBuiltinClass type_singleton = new PyBuiltinClass("str_iterator", PyStringIter.class);

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
        @Override public PyBuiltinClass type() { return type_singleton; }
    };

    protected static final class PyStringMethodUnimplemented extends PyBuiltinMethod<PyString> {
        private final String name;
        PyStringMethodUnimplemented(PyObject _self, String _name) { super((PyString)_self); name = _name; }
        @Override public String methodName() { return name; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("str." + name + "() unimplemented");
        }
    }
    protected static final class PyStringMethod_find extends PyBuiltinMethod<PyString> {
        PyStringMethod_find(PyObject _self) { super((PyString)_self); }
        @Override public String methodName() { return "find"; }
        @Override public PyInt call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "str.find");
            Runtime.requireMinArgs(args, 1, "find");
            Runtime.requireMaxArgs(args, 3, "find");
            PyObject sub = args[0];
            PyObject start = (args.length >= 2) ? args[1] : PyNone.singleton;
            PyObject end = (args.length >= 3) ? args[2] : PyNone.singleton;
            return self.pymethod_find(sub, start, end);
        }
    }
    protected static final class PyStringMethod_join extends PyBuiltinMethod<PyString> {
        PyStringMethod_join(PyObject _self) { super((PyString)_self); }
        @Override public String methodName() { return "join"; }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "str.join");
            Runtime.requireExactArgsAlt(args, 1, "str.join");
            return self.pymethod_join(args[0]);
        }
    }
    protected static final class PyStringMethod_lower extends PyBuiltinMethod<PyString> {
        PyStringMethod_lower(PyObject _self) { super((PyString)_self); }
        @Override public String methodName() { return "lower"; }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "str.lower");
            Runtime.requireExactArgsAlt(args, 0, "str.lower");
            return self.pymethod_lower();
        }
    }
    protected static final class PyStringMethod_split extends PyBuiltinMethod<PyString> {
        PyStringMethod_split(PyObject _self) { super((PyString)_self); }
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
    protected static final class PyStringMethod_startswith extends PyBuiltinMethod<PyString> {
        PyStringMethod_startswith(PyObject _self) { super((PyString)_self); }
        @Override public String methodName() { return "startswith"; }
        @Override public PyBool call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "str.startswith");
            Runtime.requireMinArgs(args, 1, "startswith");
            Runtime.requireMaxArgs(args, 3, "startswith");
            PyObject prefix = args[0];
            PyObject start = (args.length >= 2) ? args[1] : PyNone.singleton;
            PyObject end = (args.length >= 3) ? args[2] : PyNone.singleton;
            return self.pymethod_startswith(prefix, start, end);
        }
    }
    protected static final class PyStringMethod_upper extends PyBuiltinMethod<PyString> {
        PyStringMethod_upper(PyObject _self) { super((PyString)_self); }
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
        } else {
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
    }
    @Override public void delItem(PyObject key) {
        throw PyTypeError.raise(PyString.reprOf(type().name()) + " object doesn't support item deletion");
    }

    @Override public final boolean hasIter() { return true; }
    @Override public PyStringIter iter() { return new PyStringIter(this); }
    @Override public PyBuiltinClass type() { return PyStringType.singleton; }

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
}
