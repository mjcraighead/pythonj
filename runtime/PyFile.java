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

// BEGIN GENERATED CODE: PyTextIOWrapperType
final class PyTextIOWrapperType extends PyBuiltinType {
    public static final PyTextIOWrapperType singleton = new PyTextIOWrapperType();
    private static final PyMethodDescriptor pyattr_detach = new PyMethodDescriptor(singleton, "detach", obj -> new PyTextIOWrapper.PyTextIOWrapperMethodUnimplemented(obj, "detach"));
    private static final PyMethodDescriptor pyattr_reconfigure = new PyMethodDescriptor(singleton, "reconfigure", obj -> new PyTextIOWrapper.PyTextIOWrapperMethodUnimplemented(obj, "reconfigure"));
    private static final PyMethodDescriptor pyattr_write = new PyMethodDescriptor(singleton, "write", obj -> new PyTextIOWrapper.PyTextIOWrapperMethodUnimplemented(obj, "write"));
    private static final PyMethodDescriptor pyattr_read = new PyMethodDescriptor(singleton, "read", obj -> new PyTextIOWrapper.PyTextIOWrapperMethodUnimplemented(obj, "read"));
    private static final PyMethodDescriptor pyattr_readline = new PyMethodDescriptor(singleton, "readline", PyTextIOWrapper.PyTextIOWrapperMethod_readline::new);
    private static final PyMethodDescriptor pyattr_flush = new PyMethodDescriptor(singleton, "flush", obj -> new PyTextIOWrapper.PyTextIOWrapperMethodUnimplemented(obj, "flush"));
    private static final PyMethodDescriptor pyattr_close = new PyMethodDescriptor(singleton, "close", PyTextIOWrapper.PyTextIOWrapperMethod_close::new);
    private static final PyMethodDescriptor pyattr_fileno = new PyMethodDescriptor(singleton, "fileno", obj -> new PyTextIOWrapper.PyTextIOWrapperMethodUnimplemented(obj, "fileno"));
    private static final PyMethodDescriptor pyattr_seekable = new PyMethodDescriptor(singleton, "seekable", obj -> new PyTextIOWrapper.PyTextIOWrapperMethodUnimplemented(obj, "seekable"));
    private static final PyMethodDescriptor pyattr_readable = new PyMethodDescriptor(singleton, "readable", obj -> new PyTextIOWrapper.PyTextIOWrapperMethodUnimplemented(obj, "readable"));
    private static final PyMethodDescriptor pyattr_writable = new PyMethodDescriptor(singleton, "writable", obj -> new PyTextIOWrapper.PyTextIOWrapperMethodUnimplemented(obj, "writable"));
    private static final PyMethodDescriptor pyattr_isatty = new PyMethodDescriptor(singleton, "isatty", obj -> new PyTextIOWrapper.PyTextIOWrapperMethodUnimplemented(obj, "isatty"));
    private static final PyMethodDescriptor pyattr_seek = new PyMethodDescriptor(singleton, "seek", obj -> new PyTextIOWrapper.PyTextIOWrapperMethodUnimplemented(obj, "seek"));
    private static final PyMethodDescriptor pyattr_tell = new PyMethodDescriptor(singleton, "tell", obj -> new PyTextIOWrapper.PyTextIOWrapperMethodUnimplemented(obj, "tell"));
    private static final PyMethodDescriptor pyattr_truncate = new PyMethodDescriptor(singleton, "truncate", obj -> new PyTextIOWrapper.PyTextIOWrapperMethodUnimplemented(obj, "truncate"));
    private static final PyMemberDescriptor pyattr_encoding = new PyMemberDescriptor(singleton, "encoding", PyTextIOWrapper::pymember_encoding);
    private static final PyMemberDescriptor pyattr_buffer = new PyMemberDescriptor(singleton, "buffer", PyTextIOWrapper::pymember_buffer);
    private static final PyMemberDescriptor pyattr_line_buffering = new PyMemberDescriptor(singleton, "line_buffering", PyTextIOWrapper::pymember_line_buffering);
    private static final PyMemberDescriptor pyattr_write_through = new PyMemberDescriptor(singleton, "write_through", PyTextIOWrapper::pymember_write_through);
    private static final PyMemberDescriptor pyattr__finalizing = new PyMemberDescriptor(singleton, "_finalizing", PyTextIOWrapper::pymember__finalizing);
    private static final PyGetSetDescriptor pyattr_name = new PyGetSetDescriptor(singleton, "name", PyTextIOWrapper::pygetset_name);
    private static final PyGetSetDescriptor pyattr_closed = new PyGetSetDescriptor(singleton, "closed", PyTextIOWrapper::pygetset_closed);
    private static final PyGetSetDescriptor pyattr_newlines = new PyGetSetDescriptor(singleton, "newlines", PyTextIOWrapper::pygetset_newlines);
    private static final PyGetSetDescriptor pyattr_errors = new PyGetSetDescriptor(singleton, "errors", PyTextIOWrapper::pygetset_errors);
    private static final PyGetSetDescriptor pyattr__CHUNK_SIZE = new PyGetSetDescriptor(singleton, "_CHUNK_SIZE", PyTextIOWrapper::pygetset__CHUNK_SIZE);
    private static final PyString pyattr___doc__ = new PyString("Character and line based layer over a BufferedIOBase object, buffer.\n\nencoding gives the name of the encoding that the stream will be\ndecoded or encoded with. It defaults to locale.getencoding().\n\nerrors determines the strictness of encoding and decoding (see\nhelp(codecs.Codec) or the documentation for codecs.register) and\ndefaults to \"strict\".\n\nnewline controls how line endings are handled. It can be None, '',\n'\\n', '\\r', and '\\r\\n'.  It works as follows:\n\n* On input, if newline is None, universal newlines mode is\n  enabled. Lines in the input can end in '\\n', '\\r', or '\\r\\n', and\n  these are translated into '\\n' before being returned to the\n  caller. If it is '', universal newline mode is enabled, but line\n  endings are returned to the caller untranslated. If it has any of\n  the other legal values, input lines are only terminated by the given\n  string, and the line ending is returned to the caller untranslated.\n\n* On output, if newline is None, any '\\n' characters written are\n  translated to the system default line separator, os.linesep. If\n  newline is '' or '\\n', no translation takes place. If newline is any\n  of the other legal values, any '\\n' characters written are translated\n  to the given string.\n\nIf line_buffering is True, a call to flush is implied when a call to\nwrite contains a newline character.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(26);
    static {
        attrs.put(new PyString("detach"), pyattr_detach);
        attrs.put(new PyString("reconfigure"), pyattr_reconfigure);
        attrs.put(new PyString("write"), pyattr_write);
        attrs.put(new PyString("read"), pyattr_read);
        attrs.put(new PyString("readline"), pyattr_readline);
        attrs.put(new PyString("flush"), pyattr_flush);
        attrs.put(new PyString("close"), pyattr_close);
        attrs.put(new PyString("fileno"), pyattr_fileno);
        attrs.put(new PyString("seekable"), pyattr_seekable);
        attrs.put(new PyString("readable"), pyattr_readable);
        attrs.put(new PyString("writable"), pyattr_writable);
        attrs.put(new PyString("isatty"), pyattr_isatty);
        attrs.put(new PyString("seek"), pyattr_seek);
        attrs.put(new PyString("tell"), pyattr_tell);
        attrs.put(new PyString("truncate"), pyattr_truncate);
        attrs.put(new PyString("encoding"), pyattr_encoding);
        attrs.put(new PyString("buffer"), pyattr_buffer);
        attrs.put(new PyString("line_buffering"), pyattr_line_buffering);
        attrs.put(new PyString("write_through"), pyattr_write_through);
        attrs.put(new PyString("_finalizing"), pyattr__finalizing);
        attrs.put(new PyString("name"), pyattr_name);
        attrs.put(new PyString("closed"), pyattr_closed);
        attrs.put(new PyString("newlines"), pyattr_newlines);
        attrs.put(new PyString("errors"), pyattr_errors);
        attrs.put(new PyString("_CHUNK_SIZE"), pyattr__CHUNK_SIZE);
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyTextIOWrapperType() { super("_io.TextIOWrapper", PyTextIOWrapper.class); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "detach": return pyattr_detach;
            case "reconfigure": return pyattr_reconfigure;
            case "write": return pyattr_write;
            case "read": return pyattr_read;
            case "readline": return pyattr_readline;
            case "flush": return pyattr_flush;
            case "close": return pyattr_close;
            case "fileno": return pyattr_fileno;
            case "seekable": return pyattr_seekable;
            case "readable": return pyattr_readable;
            case "writable": return pyattr_writable;
            case "isatty": return pyattr_isatty;
            case "seek": return pyattr_seek;
            case "tell": return pyattr_tell;
            case "truncate": return pyattr_truncate;
            case "encoding": return pyattr_encoding;
            case "buffer": return pyattr_buffer;
            case "line_buffering": return pyattr_line_buffering;
            case "write_through": return pyattr_write_through;
            case "_finalizing": return pyattr__finalizing;
            case "name": return pyattr_name;
            case "closed": return pyattr_closed;
            case "newlines": return pyattr_newlines;
            case "errors": return pyattr_errors;
            case "_CHUNK_SIZE": return pyattr__CHUNK_SIZE;
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyTextIOWrapperType

final class PyTextIOWrapper extends PyIter {
// BEGIN GENERATED CODE: PyTextIOWrapper
    protected static final class PyTextIOWrapperMethodUnimplemented extends PyBuiltinMethod<PyTextIOWrapper> {
        private final String name;
        PyTextIOWrapperMethodUnimplemented(PyObject _self, String _name) { super((PyTextIOWrapper)_self); name = _name; }
        @Override public String methodName() { return name; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("_io.TextIOWrapper." + name + "() unimplemented");
        }
    }
// END GENERATED CODE: PyTextIOWrapper
    protected static final class PyTextIOWrapperMethod_close extends PyBuiltinMethod<PyTextIOWrapper> {
        PyTextIOWrapperMethod_close(PyObject _self) { super((PyTextIOWrapper)_self); }
        @Override public String methodName() { return "close"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "TextIOWrapper.close");
            Runtime.requireExactArgsAlt(args, 0, "TextIOWrapper.close");
            return self.pymethod_close();
        }
    }
    protected static final class PyTextIOWrapperMethod_readline extends PyBuiltinMethod<PyTextIOWrapper> {
        PyTextIOWrapperMethod_readline(PyObject _self) { super((PyTextIOWrapper)_self); }
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
    @Override public PyType type() { return PyTextIOWrapperType.singleton; }

    @Override public PyTextIOWrapper enter() { return this; }
    @Override public void exit() { pymethod_close(); }

    @Override public String repr() {
        // XXX hardcoded encoding matches CPython on Windows default
        return String.format("<%s name=%s mode='r' encoding=%s>", type().name(), name.repr(), PyString.reprOf("cp1252"));
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

    static PyObject pymember_encoding(PyObject obj) { throw new UnsupportedOperationException("TextIOWrapper.encoding unsupported"); }
    static PyObject pymember_buffer(PyObject obj) { throw new UnsupportedOperationException("TextIOWrapper.buffer unsupported"); }
    static PyObject pymember_line_buffering(PyObject obj) { throw new UnsupportedOperationException("TextIOWrapper.line_buffering unsupported"); }
    static PyObject pymember_write_through(PyObject obj) { throw new UnsupportedOperationException("TextIOWrapper.write_through unsupported"); }
    static PyObject pymember__finalizing(PyObject obj) { throw new UnsupportedOperationException("TextIOWrapper._finalizing unsupported"); }
    static PyObject pygetset_name(PyObject obj) { throw new UnsupportedOperationException("TextIOWrapper.name unsupported"); }
    static PyObject pygetset_closed(PyObject obj) { throw new UnsupportedOperationException("TextIOWrapper.closed unsupported"); }
    static PyObject pygetset_newlines(PyObject obj) { throw new UnsupportedOperationException("TextIOWrapper.newlines unsupported"); }
    static PyObject pygetset_errors(PyObject obj) { throw new UnsupportedOperationException("TextIOWrapper.errors unsupported"); }
    static PyObject pygetset__CHUNK_SIZE(PyObject obj) { throw new UnsupportedOperationException("TextIOWrapper._CHUNK_SIZE unsupported"); }
}

// BEGIN GENERATED CODE: PyBufferedReaderType
final class PyBufferedReaderType extends PyBuiltinType {
    public static final PyBufferedReaderType singleton = new PyBufferedReaderType();
    private static final PyMethodDescriptor pyattr_detach = new PyMethodDescriptor(singleton, "detach", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "detach"));
    private static final PyMethodDescriptor pyattr_flush = new PyMethodDescriptor(singleton, "flush", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "flush"));
    private static final PyMethodDescriptor pyattr_close = new PyMethodDescriptor(singleton, "close", PyBufferedReader.PyBufferedReaderMethod_close::new);
    private static final PyMethodDescriptor pyattr_seekable = new PyMethodDescriptor(singleton, "seekable", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "seekable"));
    private static final PyMethodDescriptor pyattr_readable = new PyMethodDescriptor(singleton, "readable", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "readable"));
    private static final PyMethodDescriptor pyattr_fileno = new PyMethodDescriptor(singleton, "fileno", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "fileno"));
    private static final PyMethodDescriptor pyattr_isatty = new PyMethodDescriptor(singleton, "isatty", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "isatty"));
    private static final PyMethodDescriptor pyattr__dealloc_warn = new PyMethodDescriptor(singleton, "_dealloc_warn", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "_dealloc_warn"));
    private static final PyMethodDescriptor pyattr_read = new PyMethodDescriptor(singleton, "read", PyBufferedReader.PyBufferedReaderMethod_read::new);
    private static final PyMethodDescriptor pyattr_peek = new PyMethodDescriptor(singleton, "peek", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "peek"));
    private static final PyMethodDescriptor pyattr_read1 = new PyMethodDescriptor(singleton, "read1", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "read1"));
    private static final PyMethodDescriptor pyattr_readinto = new PyMethodDescriptor(singleton, "readinto", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "readinto"));
    private static final PyMethodDescriptor pyattr_readinto1 = new PyMethodDescriptor(singleton, "readinto1", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "readinto1"));
    private static final PyMethodDescriptor pyattr_readline = new PyMethodDescriptor(singleton, "readline", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "readline"));
    private static final PyMethodDescriptor pyattr_seek = new PyMethodDescriptor(singleton, "seek", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "seek"));
    private static final PyMethodDescriptor pyattr_tell = new PyMethodDescriptor(singleton, "tell", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "tell"));
    private static final PyMethodDescriptor pyattr_truncate = new PyMethodDescriptor(singleton, "truncate", obj -> new PyBufferedReader.PyBufferedReaderMethodUnimplemented(obj, "truncate"));
    private static final PyMemberDescriptor pyattr_raw = new PyMemberDescriptor(singleton, "raw", PyBufferedReader::pymember_raw);
    private static final PyMemberDescriptor pyattr__finalizing = new PyMemberDescriptor(singleton, "_finalizing", PyBufferedReader::pymember__finalizing);
    private static final PyGetSetDescriptor pyattr_closed = new PyGetSetDescriptor(singleton, "closed", PyBufferedReader::pygetset_closed);
    private static final PyGetSetDescriptor pyattr_name = new PyGetSetDescriptor(singleton, "name", PyBufferedReader::pygetset_name);
    private static final PyGetSetDescriptor pyattr_mode = new PyGetSetDescriptor(singleton, "mode", PyBufferedReader::pygetset_mode);
    private static final PyString pyattr___doc__ = new PyString("Create a new buffered reader using the given readable raw IO object.");
    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>(23);
    static {
        attrs.put(new PyString("detach"), pyattr_detach);
        attrs.put(new PyString("flush"), pyattr_flush);
        attrs.put(new PyString("close"), pyattr_close);
        attrs.put(new PyString("seekable"), pyattr_seekable);
        attrs.put(new PyString("readable"), pyattr_readable);
        attrs.put(new PyString("fileno"), pyattr_fileno);
        attrs.put(new PyString("isatty"), pyattr_isatty);
        attrs.put(new PyString("_dealloc_warn"), pyattr__dealloc_warn);
        attrs.put(new PyString("read"), pyattr_read);
        attrs.put(new PyString("peek"), pyattr_peek);
        attrs.put(new PyString("read1"), pyattr_read1);
        attrs.put(new PyString("readinto"), pyattr_readinto);
        attrs.put(new PyString("readinto1"), pyattr_readinto1);
        attrs.put(new PyString("readline"), pyattr_readline);
        attrs.put(new PyString("seek"), pyattr_seek);
        attrs.put(new PyString("tell"), pyattr_tell);
        attrs.put(new PyString("truncate"), pyattr_truncate);
        attrs.put(new PyString("raw"), pyattr_raw);
        attrs.put(new PyString("_finalizing"), pyattr__finalizing);
        attrs.put(new PyString("closed"), pyattr_closed);
        attrs.put(new PyString("name"), pyattr_name);
        attrs.put(new PyString("mode"), pyattr_mode);
        attrs.put(new PyString("__doc__"), pyattr___doc__);
    }

    private PyBufferedReaderType() { super("_io.BufferedReader", PyBufferedReader.class); }
    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }
    @Override public PyObject lookupAttr(String name) {
        switch (name) {
            case "detach": return pyattr_detach;
            case "flush": return pyattr_flush;
            case "close": return pyattr_close;
            case "seekable": return pyattr_seekable;
            case "readable": return pyattr_readable;
            case "fileno": return pyattr_fileno;
            case "isatty": return pyattr_isatty;
            case "_dealloc_warn": return pyattr__dealloc_warn;
            case "read": return pyattr_read;
            case "peek": return pyattr_peek;
            case "read1": return pyattr_read1;
            case "readinto": return pyattr_readinto;
            case "readinto1": return pyattr_readinto1;
            case "readline": return pyattr_readline;
            case "seek": return pyattr_seek;
            case "tell": return pyattr_tell;
            case "truncate": return pyattr_truncate;
            case "raw": return pyattr_raw;
            case "_finalizing": return pyattr__finalizing;
            case "closed": return pyattr_closed;
            case "name": return pyattr_name;
            case "mode": return pyattr_mode;
            case "__doc__": return pyattr___doc__;
            default: return null;
        }
    }
}
// END GENERATED CODE: PyBufferedReaderType

final class PyBufferedReader extends PyIter {
// BEGIN GENERATED CODE: PyBufferedReader
    protected static final class PyBufferedReaderMethodUnimplemented extends PyBuiltinMethod<PyBufferedReader> {
        private final String name;
        PyBufferedReaderMethodUnimplemented(PyObject _self, String _name) { super((PyBufferedReader)_self); name = _name; }
        @Override public String methodName() { return name; }
        @Override public PyObject call(PyObject[] args, PyDict kwargs) {
            throw new UnsupportedOperationException("_io.BufferedReader." + name + "() unimplemented");
        }
    }
// END GENERATED CODE: PyBufferedReader
    protected static final class PyBufferedReaderMethod_close extends PyBuiltinMethod<PyBufferedReader> {
        PyBufferedReaderMethod_close(PyObject _self) { super((PyBufferedReader)_self); }
        @Override public String methodName() { return "close"; }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "BufferedReader.close");
            Runtime.requireExactArgsAlt(args, 0, "BufferedReader.close");
            return self.pymethod_close();
        }
    }
    protected static final class PyBufferedReaderMethod_read extends PyBuiltinMethod<PyBufferedReader> {
        PyBufferedReaderMethod_read(PyObject _self) { super((PyBufferedReader)_self); }
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
    public PyBytes pymethod_read() {
        try {
            return new PyBytes(reader.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static PyObject pymember_raw(PyObject obj) { throw new UnsupportedOperationException("BufferedReader.raw unsupported"); }
    static PyObject pymember__finalizing(PyObject obj) { throw new UnsupportedOperationException("BufferedReader._finalizing unsupported"); }
    static PyObject pygetset_closed(PyObject obj) { throw new UnsupportedOperationException("BufferedReader.closed unsupported"); }
    static PyObject pygetset_name(PyObject obj) { throw new UnsupportedOperationException("BufferedReader.name unsupported"); }
    static PyObject pygetset_mode(PyObject obj) { throw new UnsupportedOperationException("BufferedReader.mode unsupported"); }
}
