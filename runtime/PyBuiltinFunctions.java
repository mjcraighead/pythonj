// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

abstract class PyBuiltinFunction extends PyBuiltinFunctionOrMethod {
    protected final String funcName;
    protected PyBuiltinFunction(String name) { funcName = name; }
    @Override public final String repr() { return "<built-in function " + funcName + ">"; }
}

final class PyBuiltinFunctionsImpl {
    static PyString pyfunc_json_encode_basestring_ascii(PyObject arg) {
        if (!(arg instanceof PyString argStr)) {
            throw PyTypeError.raise("first argument must be a string, not " + arg.type().name());
        }
        String s = argStr.value;
        var out = new StringBuilder();
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if ((c >= 0x20) && (c <= 0x7E)) {
                        out.append(c);
                    } else {
                        out.append("\\u");
                        out.append(String.format("%04x", (int)c));
                    }
                    break;
            }
        }
        out.append('"');
        return new PyString(out.toString());
    }
    private static PyRaise jsonScanstringError(String msg, String s, int pos) {
        int line = 1;
        int col = 1;
        for (int i = 0; i < pos; i++) {
            if (s.charAt(i) == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }
        return PyValueError.raiseFormat("%s: line %d column %d (char %d)", msg, line, col, pos);
    }
    static PyTuple pyfunc_json_scanstring(PyObject sObj, PyObject endObj) {
        if (!(sObj instanceof PyString sStr)) {
            throw PyTypeError.raise("first argument must be a string, not " + sObj.type().name());
        }
        String s = sStr.value;
        int start = (int)endObj.indexValue();
        int i = start;
        var out = new StringBuilder();
        while (true) {
            if (i >= s.length()) {
                throw jsonScanstringError("Unterminated string starting at", s, start - 1);
            }
            char c = s.charAt(i);
            if (c == '"') {
                return new PyTuple(new PyObject[] {new PyString(out.toString()), new PyInt(i + 1)});
            }
            if (c == '\\') {
                i++;
                if (i >= s.length()) {
                    throw jsonScanstringError("Unterminated string starting at", s, start - 1);
                }
                char esc = s.charAt(i);
                switch (esc) {
                    case '"': out.append('"'); break;
                    case '\\': out.append('\\'); break;
                    case '/': out.append('/'); break;
                    case 'b': out.append('\b'); break;
                    case 'f': out.append('\f'); break;
                    case 'n': out.append('\n'); break;
                    case 'r': out.append('\r'); break;
                    case 't': out.append('\t'); break;
                    case 'u': {
                        if (i + 4 >= s.length()) {
                            throw jsonScanstringError("Invalid \\uXXXX escape", s, i);
                        }
                        int code = 0;
                        for (int j = 1; j <= 4; j++) {
                            int digit = Character.digit(s.charAt(i + j), 16);
                            if (digit < 0) {
                                throw jsonScanstringError("Invalid \\uXXXX escape", s, i);
                            }
                            code = (code << 4) | digit;
                        }
                        out.append((char)code);
                        i += 4;
                        break;
                    }
                    default:
                        throw jsonScanstringError("Invalid \\escape", s, i - 1);
                }
                i++;
                continue;
            }
            out.append(c);
            i++;
        }
    }
    private static double requireMathReal(PyObject arg) {
        if ((arg instanceof PyFloat) || (arg instanceof PyInt) || (arg instanceof PyBool)) {
            return arg.floatValue();
        }
        throw PyTypeError.raise("must be real number, not " + arg.type().name());
    }
    static PyFloat pyfunc_math_copysign(PyObject x, PyObject y) {
        return new PyFloat(Math.copySign(requireMathReal(x), requireMathReal(y)));
    }
    static PyBool pyfunc_math_isfinite(PyObject arg) {
        return PyBool.create(Double.isFinite(requireMathReal(arg)));
    }
    static PyBool pyfunc_math_isinf(PyObject arg) {
        return PyBool.create(Double.isInfinite(requireMathReal(arg)));
    }
    static PyBool pyfunc_math_isnan(PyObject arg) {
        return PyBool.create(Double.isNaN(requireMathReal(arg)));
    }
    static PyBool pyfunc_operator_contains(PyObject a, PyObject b) {
        return PyBool.create(a.contains(b));
    }
    static PyNone pyfunc_operator_delitem(PyObject a, PyObject b) {
        a.delItem(b);
        return PyNone.singleton;
    }
    static PyObject pyfunc_operator_getitem(PyObject a, PyObject b) {
        return a.getItem(b);
    }
    static PyInt pyfunc_operator_index(PyObject a) {
        return new PyInt(a.indexValue());
    }
    static PyNone pyfunc_operator_setitem(PyObject a, PyObject b, PyObject c) {
        a.setItem(b, c);
        return PyNone.singleton;
    }
    static PyBytes pyfunc_zlib_compress(PyObject arg, PyObject level, PyObject wbits) {
        byte[] in = Runtime.requireBytesLikeBuffer(arg);
        if ((level.indexValue() != -1) || (wbits.indexValue() != 15)) {
            throw new UnsupportedOperationException("zlib.compress() arguments beyond data are unsupported");
        }
        Deflater deflater = new Deflater();
        deflater.setInput(in);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, in.length + 32));
        byte[] buf = new byte[1024];
        try {
            while (!deflater.finished()) {
                int n = deflater.deflate(buf);
                if (n > 0) {
                    out.write(buf, 0, n);
                }
            }
        } finally {
            deflater.end();
        }
        return new PyBytes(out.toByteArray());
    }
    static PyBytes pyfunc_zlib_decompress(PyObject arg, PyObject wbits, PyObject bufsize) {
        byte[] in = Runtime.requireBytesLikeBuffer(arg);
        if ((wbits.indexValue() != 15) || (bufsize.indexValue() != 16384)) {
            throw new UnsupportedOperationException("zlib.decompress() arguments beyond data are unsupported");
        }
        Inflater inflater = new Inflater();
        inflater.setInput(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, in.length * 2));
        byte[] buf = new byte[1024];
        try {
            while (!inflater.finished()) {
                int n = inflater.inflate(buf);
                if (n > 0) {
                    out.write(buf, 0, n);
                } else if (inflater.needsInput() || inflater.needsDictionary()) {
                    throw PyZlibError.raise("Error -5 while decompressing data: incomplete or truncated stream");
                } else {
                    throw PyZlibError.raise("Error -3 while decompressing data: invalid stored block lengths");
                }
            }
        } catch (DataFormatException e) {
            throw PyZlibError.raise("Error -3 while decompressing data: " + e.getMessage());
        } finally {
            inflater.end();
        }
        return new PyBytes(out.toByteArray());
    }
    static PyString pyfunc_ascii(PyObject arg) {
        String r = arg.repr();
        var s = new StringBuilder();
        for (int i = 0; i < r.length(); i++) {
            char c = r.charAt(i);
            if (c < 0x80) {
                s.append(c);
            } else if (c <= 0xFF) {
                s.append("\\x");
                s.append("0123456789abcdef".charAt(c >> 4));
                s.append("0123456789abcdef".charAt(c & 15));
            } else {
                s.append("\\u");
                s.append(String.format("%04x", (int)c));
            }
        }
        return new PyString(s.toString());
    }
    static PyString pyfunc_chr(PyObject arg) {
        long index = arg.indexValue();
        if ((index < 0) || (index > 65535)) {
            throw new IllegalArgumentException("chr() argument out of range");
        }
        return new PyString(String.valueOf((char)index));
    }
    static PyList pyfunc_dir(PyObject object) {
        if (object == null) {
            throw new UnsupportedOperationException("dir() with no arguments is not implemented");
        }
        PyType attrsType;
        if (object instanceof PyType objectType) {
            attrsType = objectType;
        } else {
            attrsType = object.type();
        }
        var attrs = attrsType.getAttributes();
        if (attrs == null) {
            throw new UnsupportedOperationException(attrsType.name() + ".__dict__ is not implemented");
        }
        ArrayList<PyObject> list = new ArrayList<>(attrs.keySet());
        Collections.sort(list);
        return new PyList(list);
    }
    static PyString pyfunc_hex(PyObject arg) {
        long index = arg.indexValue();
        if (index < 0) {
            return new PyString(String.format("-0x%x", Math.negateExact(index)));
        } else {
            return new PyString(String.format("0x%x", index));
        }
    }
    static PyIter pyfunc_iter(PyObject obj, PyObject sentinel) {
        if (sentinel != null) {
            throw new UnsupportedOperationException("iter() with callable+sentinel is not implemented");
        }
        return obj.iter();
    }
    static PyObject minMaxImpl(PyObject[] args, PyDict kwargs, String name, boolean isMax) {
        int argsLength = args.length;
        if (argsLength < 1) {
            throw Runtime.raiseMinArgs(args, 1, name);
        }
        PyObject defaultObj = null;
        PyObject keyFunc = PyNone.singleton;
        if ((kwargs != null) && kwargs.boolValue()) {
            long kwargsLen = kwargs.len();
            if (kwargsLen > 2) {
                throw Runtime.raiseAtMostKwArgs(name, 2, 0, kwargsLen);
            }
            for (var x: kwargs.items.entrySet()) {
                PyString kw = (PyString)x.getKey(); // PyString validated at call site
                if (kw.value.equals("default")) {
                    defaultObj = x.getValue();
                } else if (kw.value.equals("key")) {
                    keyFunc = x.getValue();
                } else {
                    throw Runtime.raiseUnexpectedKwArg(name, kw.value);
                }
            }
        }
        if (argsLength == 1) {
            return isMax
                ? PyRuntime.pyfunc_max_iterable(args[0], defaultObj, keyFunc)
                : PyRuntime.pyfunc_min_iterable(args[0], defaultObj, keyFunc);
        } else {
            if (defaultObj != null) {
                throw PyTypeError.raise("Cannot specify a default for " + name + "() with multiple positional arguments");
            }
            PyObject ret = args[0];
            if (keyFunc == PyNone.singleton) {
                for (int i = 1; i < argsLength; i++) {
                    PyObject item = args[i];
                    if (isMax ? item.gt(ret) : item.lt(ret)) {
                        ret = item;
                    }
                }
            } else {
                PyObject retKey = keyFunc.call(new PyObject[] {ret}, null);
                for (int i = 1; i < argsLength; i++) {
                    PyObject item = args[i];
                    PyObject itemKey = keyFunc.call(new PyObject[] {item}, null);
                    if (isMax ? itemKey.gt(retKey) : itemKey.lt(retKey)) {
                        ret = item;
                        retKey = itemKey;
                    }
                }
            }
            return ret;
        }
    }
    static PyObject pyfunc_max(PyObject[] args, PyDict kwargs) {
        return minMaxImpl(args, kwargs, "max", true);
    }
    static PyObject pyfunc_min(PyObject[] args, PyDict kwargs) {
        return minMaxImpl(args, kwargs, "min", false);
    }
    static PyObject pyfunc_open(PyObject file, PyObject mode, PyObject buffering, PyObject encoding,
                                PyObject errors, PyObject newline, PyObject closefd, PyObject opener) {
        if (!(file instanceof PyString fileStr)) {
            throw PyTypeError.raise("open() argument 'file' must be str, not " + file.type().name());
        }
        if (!(mode instanceof PyString modeStr)) {
            throw PyTypeError.raise("open() argument 'mode' must be str, not " + mode.type().name());
        }
        if ((buffering.indexValue() != -1) || (encoding != PyNone.singleton) || (errors != PyNone.singleton) ||
            (newline != PyNone.singleton) || !closefd.boolValue() || (opener != PyNone.singleton)) {
            throw new UnsupportedOperationException("open() arguments beyond file/mode are not supported");
        }
        if (modeStr.value.equals("r")) {
            return new PyTextIOWrapper(fileStr);
        } else if (modeStr.value.equals("rb")) {
            return new PyBufferedReader(fileStr);
        } else {
            throw new UnsupportedOperationException("open() only supports mode='r' and mode='rb'");
        }
    }
    static PyInt pyfunc_ord(PyObject arg_obj) {
        PyString arg = (PyString)arg_obj;
        if (arg.len() != 1) {
            throw new IllegalArgumentException("argument to ord() must be string of length 1");
        }
        return new PyInt(arg.value.charAt(0));
    }
    static PyNone pyfunc_print(PyObject[] args, PyObject sep, PyObject end, PyObject file, PyObject flush) {
        if (file != PyNone.singleton) {
            throw new UnsupportedOperationException("print() file= is not supported");
        }
        if (flush.boolValue()) {
            throw new UnsupportedOperationException("print() flush= is not supported");
        }
        String sepStr = " ";
        if (sep != PyNone.singleton) {
            if (!(sep instanceof PyString sepString)) {
                throw PyTypeError.raise("sep must be None or a string, not " + sep.type().name());
            }
            sepStr = sepString.value;
        }
        String endStr = "\n";
        if (end != PyNone.singleton) {
            if (!(end instanceof PyString endString)) {
                throw PyTypeError.raise("end must be None or a string, not " + end.type().name());
            }
            endStr = endString.value;
        }
        boolean first = true;
        for (var arg: args) {
            if (!first) {
                System.out.print(sepStr);
            }
            first = false;
            System.out.print(arg.str());
        }
        if (endStr.equals("\n")) { // XXX hack for Windows mismatches
            System.out.println();
        } else {
            System.out.print(endStr);
        }
        return PyNone.singleton;
    }
    static PyList pyfunc_sorted(PyObject iterable, PyObject key, PyObject reverse) {
        var ret = new PyList();
        Runtime.addIterableToCollection(ret.items, iterable);
        ret.pymethod_sort(key, reverse);
        return ret;
    }
}
