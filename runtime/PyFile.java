// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;

final class PyTextIOWrapper extends PyIter {
    private static final class PyTextIOWrapperMethod_close extends PyBuiltinMethod<PyTextIOWrapper> {
        PyTextIOWrapperMethod_close(PyTextIOWrapper _self) { super(_self); }
        @Override public String methodName() { return "close"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "TextIOWrapper.close");
            Runtime.requireExactArgsAlt(args, 0, "TextIOWrapper.close");
            return self.pymethod_close();
        }
    }
    private static final class PyTextIOWrapperMethod_readline extends PyBuiltinMethod<PyTextIOWrapper> {
        PyTextIOWrapperMethod_readline(PyTextIOWrapper _self) { super(_self); }
        @Override public String methodName() { return "readline"; }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "TextIOWrapper.readline");
            Runtime.requireMaxArgs(args, 1, "readline");
            if (args.length != 0) {
                throw new UnsupportedOperationException("'size' argument to TextIOWrapper.readline() is not supported");
            }
            return self.pymethod_readline();
        }
    }

    private final PyObject name;
    public final BufferedReader reader;

    PyTextIOWrapper(PyString path) {
        name = path;
        try {
            reader = new BufferedReader(new FileReader(path.value));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override public PyString next() {
        try {
            StringBuilder s = new StringBuilder();
            for (;;) {
                int c = reader.read();
                if (c == -1) { // EOF
                    if (s.length() == 0) {
                        return null;
                    }
                    break; // last line, no newline
                }
                if (c == '\n') {
                    int len = s.length();
                    if ((len != 0) && (s.charAt(len - 1) == '\r')) {
                        s.deleteCharAt(len - 1);
                    }
                    s.append((char)c);
                    break; // full line
                }
                s.append((char)c);
            }
            return new PyString(s.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    @Override public PyType type() { return Runtime.pytype_io_TextIOWrapper; }

    @Override public PyTextIOWrapper enter() { return this; }
    @Override public void exit() { pymethod_close(); }

    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "close": return new PyTextIOWrapperMethod_close(this);
            case "readline": return new PyTextIOWrapperMethod_readline(this);
            default: return super.getAttr(key);
        }
    }
    @Override public String repr() {
        // XXX hardcoded encoding matches CPython on Windows default
        return String.format("<%s name=%s mode='r' encoding='%s'>", type().name(), name.repr(), "cp1252");
    }

    public PyNone pymethod_close() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return PyNone.singleton;
    }
    public PyString pymethod_readline() {
        PyString ret = next();
        return (ret != null) ? ret : PyString.empty_singleton;
    }
}

final class PyBufferedReader extends PyIter {
    private static final class PyBufferedReaderMethod_close extends PyBuiltinMethod<PyBufferedReader> {
        PyBufferedReaderMethod_close(PyBufferedReader _self) { super(_self); }
        @Override public String methodName() { return "close"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "BufferedReader.close");
            Runtime.requireExactArgsAlt(args, 0, "BufferedReader.close");
            return self.pymethod_close();
        }
    }
    private static final class PyBufferedReaderMethod_read extends PyBuiltinMethod<PyBufferedReader> {
        PyBufferedReaderMethod_read(PyBufferedReader _self) { super(_self); }
        @Override public String methodName() { return "read"; }
        @Override public PyBytes call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "BufferedReader.read");
            Runtime.requireMaxArgs(args, 1, "read");
            if (args.length != 0) {
                throw new UnsupportedOperationException("'size' argument to BufferedReader.read() is not supported");
            }
            return self.pymethod_read();
        }
    }

    private final PyObject name;
    public final BufferedInputStream reader;

    PyBufferedReader(PyString path) {
        name = path;
        try {
            reader = new BufferedInputStream(new FileInputStream(path.value));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override public PyBytes next() { throw new UnsupportedOperationException("iterating over binary files not supported"); }
    @Override public PyType type() { return Runtime.pytype_io_BufferedReader; }

    @Override public PyBufferedReader enter() { return this; }
    @Override public void exit() { pymethod_close(); }

    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "close": return new PyBufferedReaderMethod_close(this);
            case "read": return new PyBufferedReaderMethod_read(this);
            default: return super.getAttr(key);
        }
    }
    @Override public String repr() { return String.format("<%s name=%s>", type().name(), name.repr()); }

    public PyNone pymethod_close() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return PyNone.singleton;
    }
    public PyBytes pymethod_read() {
        try {
            return new PyBytes(reader.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
