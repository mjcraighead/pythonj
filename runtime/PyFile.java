// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

// XXX This is not even close to supporting fully correct Python IO semantics.  It's
// just intended to be enough that we can do basic things like "for line in f:".
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class PyFile extends PyIter {
    private static class PyFileMethod extends PyBuiltinFunctionOrMethod {
        protected final PyFile self;
        PyFileMethod(PyFile _self) { self = _self; }
        @Override public String repr() { throw new UnsupportedOperationException("'repr' unimplemented"); }
    }
    private static final class PyFileMethod_close extends PyFileMethod {
        PyFileMethod_close(PyFile _self) { super(_self); }
        @Override public PyNone call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "TextIOWrapper.close");
            Runtime.requireExactArgsAlt(args, 0, "TextIOWrapper.close");
            return self.pymethod_close();
        }
    }
    private static final class PyFileMethod_readline extends PyFileMethod {
        PyFileMethod_readline(PyFile _self) { super(_self); }
        @Override public PyString call(PyObject[] args, PyDict kwargs) {
            Runtime.requireNoKwArgs(kwargs, "TextIOWrapper.readline");
            Runtime.requireMaxArgs(args, 1, "readline");
            if (args.length != 0) {
                throw new UnsupportedOperationException("'size' argument to TextIOWrapper.readline() is not supported");
            }
            return self.pymethod_readline();
        }
    }

    public final BufferedReader reader;

    PyFile(String path) {
        try {
            reader = new BufferedReader(new FileReader(path));
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

    @Override public PyFile enter() { return this; }
    @Override public void exit() { pymethod_close(); }

    @Override public PyObject getAttr(String key) {
        switch (key) {
            case "close": return new PyFileMethod_close(this);
            case "readline": return new PyFileMethod_readline(this);
            default: return super.getAttr(key);
        }
    }
    @Override public String repr() { throw new UnsupportedOperationException("'repr' unimplemented"); }

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
