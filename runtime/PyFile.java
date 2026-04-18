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

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        throw new UnsupportedOperationException(type.name() + ".__new__() unimplemented");
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
    @Override public PyType type() { return PyTextIOWrapperType.singleton; }

    @Override public PyTextIOWrapper enter() { return this; }
    @Override public void exit() { pymethod_close(); }

    @Override public String repr() {
        String encoding = Runtime.getDefaultTextEncoding();
        return String.format("<%s name=%s mode='r' encoding=%s>", type().name(), name.repr(), PyString.reprOf(encoding));
    }

    public PyNone pymethod_close() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return PyNone.singleton;
    }
    public PyString pymethod_readline(PyObject size) {
        if (!size.equals(PyInt.singleton_neg1)) {
            throw new UnsupportedOperationException("'size' argument to TextIOWrapper.readline() is not supported");
        }
        PyString ret = next();
        return (ret != null) ? ret : PyString.empty_singleton;
    }
    public PyString pymethod_read(PyObject size) {
        if (!size.equals(PyInt.singleton_neg1)) {
            throw new UnsupportedOperationException("'size' argument to TextIOWrapper.read() is not supported");
        }
        try {
            StringBuilder s = new StringBuilder();
            for (;;) {
                int c = reader.read();
                if (c == -1) {
                    break;
                }
                s.append((char)c);
            }
            if (s.isEmpty()) {
                return PyString.empty_singleton;
            }
            return new PyString(s.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public PyObject pymethod_detach() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_fileno() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_flush() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isatty() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_readable() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_reconfigure(PyObject encoding, PyObject errors, PyObject newline, PyObject line_buffering, PyObject write_through) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_seek(PyObject cookie, PyObject whence) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_seekable() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_tell() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_truncate(PyObject pos) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_writable() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_write(PyObject text) { throw new UnsupportedOperationException(); }

    static PyObject pyget_encoding(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pyget_buffer(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pyget_line_buffering(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pyget_write_through(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pyget__finalizing(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pyget_name(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pyget_closed(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pyget_newlines(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pyget_errors(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pyget__CHUNK_SIZE(PyObject obj) { throw new UnsupportedOperationException(); }
}

final class PyBufferedReader extends PyIter {
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

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        throw new UnsupportedOperationException(type.name() + ".__new__() unimplemented");
    }

    @Override public PyBytes next() { throw new UnsupportedOperationException("iterating over binary files not supported"); }
    @Override public PyType type() { return PyBufferedReaderType.singleton; }

    @Override public PyBufferedReader enter() { return this; }
    @Override public void exit() { pymethod_close(); }

    @Override public String repr() { return String.format("<%s name=%s>", type().name(), name.repr()); }

    public PyNone pymethod_close() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return PyNone.singleton;
    }
    public PyBytes pymethod_read(PyObject size) {
        if (!size.equals(PyInt.singleton_neg1)) {
            throw new UnsupportedOperationException("'size' argument to BufferedReader.read() is not supported");
        }
        try {
            return new PyBytes(reader.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public PyObject pymethod__dealloc_warn(PyObject source) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_detach() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_fileno() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_flush() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_isatty() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_peek(PyObject size) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_read1(PyObject size) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_readable() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_readinto(PyObject buffer) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_readinto1(PyObject buffer) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_readline(PyObject size) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_seek(PyObject target, PyObject whence) { throw new UnsupportedOperationException(); }
    public PyObject pymethod_seekable() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_tell() { throw new UnsupportedOperationException(); }
    public PyObject pymethod_truncate(PyObject pos) { throw new UnsupportedOperationException(); }

    static PyObject pyget_raw(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pyget__finalizing(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pyget_closed(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pyget_name(PyObject obj) { throw new UnsupportedOperationException(); }
    static PyObject pyget_mode(PyObject obj) { throw new UnsupportedOperationException(); }
}
