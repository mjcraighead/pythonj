# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

from abc import ABC, abstractmethod
import argparse
import ast
from contextlib import contextmanager
from dataclasses import dataclass
import difflib
import glob
import itertools
import os
import subprocess
import sys
import time
from typing import Iterator, Optional, TextIO

builtins = {
    'abs', 'all', 'any', 'bool', 'bytearray', 'bytes', 'chr', 'dict', 'enumerate', 'hash', 'hex', 'int', 'isinstance', 'iter', 'len', 'list',
    'max', 'min', 'open', 'ord', 'print', 'range', 'repr', 'reversed', 'set', 'slice', 'sorted', 'str', 'sum', 'tuple', 'type', 'zip',
}

RUNTIME_JAVA_FILES = (
    'PyBool.java',
    'PyByteArray.java',
    'PyBytes.java',
    'PyDict.java',
    'PyEnumerate.java',
    'PyFile.java',
    'PyInt.java',
    'PyList.java',
    'PyNone.java',
    'PyObject.java',
    'PyRange.java',
    'PyReversed.java',
    'PySet.java',
    'PySlice.java',
    'PyString.java',
    'PyTuple.java',
    'PyZip.java',
    'Runtime.java',
)

def int_name(i: int) -> str:
    """Return the Java variable name to use for the PyInt singleton with a given value."""
    return f'int_singleton_neg{-i}' if i < 0 else f'int_singleton_{i}'

CHAR_ESCAPE = {
    '"': r'\"',
    '\\': r'\\',
    '\n': r'\n',
    '\r': r'\r',
    '\t': r'\t',
    '\b': r'\b',
    '\f': r'\f',
}
def java_string_literal(s: str) -> str:
    """Escape a Python string into a Java string literal with all special characters escaped."""
    out = ['"']
    for c in s:
        if c in CHAR_ESCAPE:
            out.append(CHAR_ESCAPE[c])
        else:
            o = ord(c)
            if 0xD800 <= o <= 0xDFFF:
                raise ValueError(f'cannot encode string containing surrogate code points: {s!r}')
            assert o <= 0xFFFF, o # XXX implement surrogate pairs for astral chars
            if 0x20 <= o <= 0x7E: # safe ASCII
                out.append(c)
            else:
                out.append(f'\\u{o:04x}')
    out.append('"')
    return ''.join(out)

class JavaExpr(ABC):
    @abstractmethod
    def emit_java(self) -> str:
        raise NotImplementedError()

@dataclass(slots=True)
class JavaIntLiteral(JavaExpr):
    value: int
    suffix: str
    def emit_java(self) -> str:
        return f'{self.value}{self.suffix}'

@dataclass(slots=True)
class JavaStrLiteral(JavaExpr):
    s: str
    def emit_java(self) -> str:
        return java_string_literal(self.s)

@dataclass(slots=True)
class JavaIdentifier(JavaExpr):
    name: str
    def emit_java(self) -> str:
        return self.name

@dataclass(slots=True)
class JavaField(JavaExpr):
    obj: JavaExpr
    field: str
    def emit_java(self) -> str:
        return f'{self.obj.emit_java()}.{self.field}'

@dataclass(slots=True)
class JavaArrayAccess(JavaExpr):
    obj: JavaExpr
    index: JavaExpr
    def emit_java(self) -> str:
        return f'{self.obj.emit_java()}[{self.index.emit_java()}]'

@dataclass(slots=True)
class JavaUnaryOp(JavaExpr):
    op: str
    operand: JavaExpr
    def emit_java(self) -> str:
        return f'({self.op}{self.operand.emit_java()})'

@dataclass(slots=True)
class JavaBinaryOp(JavaExpr):
    op: str
    lhs: JavaExpr
    rhs: JavaExpr
    def emit_java(self) -> str:
        return f'({self.lhs.emit_java()} {self.op} {self.rhs.emit_java()})'

@dataclass(slots=True)
class JavaCondOp(JavaExpr):
    cond: JavaExpr
    true: JavaExpr
    false: JavaExpr
    def emit_java(self) -> str:
        return f'({self.cond.emit_java()} ? {self.true.emit_java()} : {self.false.emit_java()})'

@dataclass(slots=True)
class JavaCreateObject(JavaExpr):
    type: str
    args: list[JavaExpr]
    def emit_java(self) -> str:
        return f"new {self.type}({', '.join(arg.emit_java() for arg in self.args)})"

@dataclass(slots=True)
class JavaCreateArray(JavaExpr):
    type: str
    elts: list[JavaExpr]
    def emit_java(self) -> str:
        return f"new {self.type}[] {{{', '.join(x.emit_java() for x in self.elts)}}}"

@dataclass(slots=True)
class JavaMethodCall(JavaExpr):
    obj: JavaExpr
    method: str
    args: list[JavaExpr]
    def emit_java(self) -> str:
        return f"{self.obj.emit_java()}.{self.method}({', '.join(arg.emit_java() for arg in self.args)})"

@dataclass(slots=True)
class JavaAssignExpr(JavaExpr):
    lhs: JavaExpr
    rhs: JavaExpr
    def emit_java(self) -> str:
        return f'({self.lhs.emit_java()} = {self.rhs.emit_java()})'

class JavaStatement(ABC):
    @abstractmethod
    def emit_java(self) -> Iterator[str]:
        raise NotImplementedError()

@dataclass(slots=True)
class JavaVariableDecl(JavaStatement):
    type: str
    name: str
    value: Optional[JavaExpr]
    def emit_java(self) -> Iterator[str]:
        if self.value:
            yield f'{self.type} {self.name} = {self.value.emit_java()};'
        else:
            yield f'{self.type} {self.name};'

@dataclass(slots=True)
class JavaAssignStatement(JavaStatement):
    lhs: JavaExpr
    rhs: JavaExpr
    def emit_java(self) -> Iterator[str]:
        yield f'{self.lhs.emit_java()} = {self.rhs.emit_java()};'

@dataclass(slots=True)
class JavaExprStatement(JavaStatement):
    call: JavaCreateObject | JavaMethodCall # only limited types of expressions allowed by Java grammar
    def emit_java(self) -> Iterator[str]:
        yield f'{self.call.emit_java()};'

@dataclass(slots=True)
class JavaBreakStatement(JavaStatement):
    def emit_java(self) -> Iterator[str]:
        yield 'break;'

@dataclass(slots=True)
class JavaContinueStatement(JavaStatement):
    def emit_java(self) -> Iterator[str]:
        yield 'continue;'

@dataclass(slots=True)
class JavaReturnStatement(JavaStatement):
    expr: JavaExpr
    def emit_java(self) -> Iterator[str]:
        yield f'return {self.expr.emit_java()};'

@dataclass(slots=True)
class JavaThrowStatement(JavaStatement):
    expr: JavaExpr
    def emit_java(self) -> Iterator[str]:
        yield f'throw {self.expr.emit_java()};'

@dataclass(slots=True)
class JavaIfStatement(JavaStatement):
    cond: JavaExpr
    body: list[JavaStatement]
    orelse: list[JavaStatement]
    def emit_java(self) -> Iterator[str]:
        yield f'if ({self.cond.emit_java()}) {{'
        for s in self.body:
            yield from s.emit_java()
        if self.orelse:
            yield '} else {'
            for s in self.orelse:
                yield from s.emit_java()
        yield '}'

@dataclass(slots=True)
class JavaWhileStatement(JavaStatement):
    cond: JavaExpr
    body: list[JavaStatement]
    def emit_java(self) -> Iterator[str]:
        yield f'while ({self.cond.emit_java()}) {{'
        for s in self.body:
            yield from s.emit_java()
        yield '}'

# simplified; init/incr are weird because of semicolons/parens if we try to map them to statement or expr
@dataclass(slots=True)
class JavaForStatement(JavaStatement):
    init_type: str
    init_name: str
    init_value: JavaExpr
    cond: JavaExpr
    incr_name: str
    incr_value: JavaExpr
    body: list[JavaStatement]
    def emit_java(self) -> Iterator[str]:
        yield f'for ({self.init_type} {self.init_name} = {self.init_value.emit_java()}; {self.cond.emit_java()}; {self.incr_name} = {self.incr_value.emit_java()}) {{'
        for s in self.body:
            yield from s.emit_java()
        yield '}'

def py_none() -> JavaExpr:
    return JavaField(JavaIdentifier('PyNone'), 'singleton')

def bool_value(expr: JavaExpr) -> JavaExpr:
    return JavaMethodCall(expr, 'boolValue', [])

def chained_binary_op(op: str, exprs: list[JavaExpr]) -> JavaExpr:
    assert len(exprs) >= 1, exprs
    expr = exprs[0]
    for term in exprs[1:]:
        expr = JavaBinaryOp(op, expr, term)
    return expr

@dataclass(slots=True)
class IndentedWriter:
    f: TextIO
    indent: int
    def write(self, line: str) -> None:
        if not line: # write a blank line
            self.f.write('\n')
            return
        if line.startswith('}'):
            self.indent -= 1
        self.f.write('    ' * self.indent)
        self.f.write(line)
        self.f.write('\n')
        if line.endswith('{'):
            self.indent += 1

# XXX Need to design a systematic way to avoid "code too large" and "too many constants" errors.
# This is somewhat challenging, as even a single Python expression or statement can easily overflow
# the maximum code size limit, and it is somewhat unpredictable how much bytecode our translations
# will compile into.  Partial mitigations are likely to be easier than a total fix.
# XXX "invokedynamic" might help us a lot, but there is no way to access it from Java source
class PythonjVisitor(ast.NodeVisitor):
    __slots__ = ('path', 'n_errors', 'n_temps', 'global_names', 'global_code', 'names', 'code',
                 'all_ints', 'all_strings', 'all_bytes', 'in_function', 'has_returns', 'functions')
    path: str
    n_errors: int
    n_temps: int
    global_names: set[str]
    global_code: list[JavaStatement]
    names: set[str]
    code: list[JavaStatement]
    all_ints: set[int]
    all_strings: dict[str, int]
    all_bytes: dict[bytes, int]
    in_function: bool
    has_returns: bool
    used_expr_discard: bool
    functions: dict[str, list[str]]

    def __init__(self, path: str):
        self.path = path
        self.n_errors = 0
        self.n_temps = 0
        self.global_names = set()
        self.global_code = [JavaVariableDecl('PyObject', 'expr_discard', None)] # XXX remove this variable if not needed
        self.names = self.global_names
        self.code = self.global_code
        self.all_ints = set()
        self.all_strings = {}
        self.all_bytes = {}
        self.in_function = False
        self.has_returns = False
        self.used_expr_discard = False
        self.functions = {}

    # Note: if n_errors > 0, the generated Java code is not expected to be valid or executable.
    def error(self, lineno: Optional[int], msg: str) -> None:
        if lineno is None:
            print(f'ERROR: {self.path}: {msg}')
        else:
            print(f'ERROR: {self.path}:{lineno}: {msg}')
        self.n_errors += 1

    def make_temp(self) -> str:
        """Assign and return a new temporary variable name."""
        temp_name = f'temp{self.n_temps}'
        self.n_temps += 1
        return temp_name

    def ident_expr(self, name: str) -> JavaExpr:
        if name in builtins:
            return JavaField(JavaIdentifier('Runtime'), f'pyglobal_{name}')
        elif self.in_function and name not in self.global_names:
            return JavaIdentifier(f'pylocal_{name}')
        else:
            return JavaIdentifier(f'pyglobal_{name}')

    def generic_visit(self, node):
        """Print an error for all unknown constructs in translation."""
        self.error(getattr(node, 'lineno', None), f'unsupported Python construct: {type(node).__name__}')
        if isinstance(node, ast.expr):
            return JavaIdentifier('__cannot_translate_expr__') # return placeholder JavaExpr IR node

    def visit_Invert(self, node): return 'invert'
    def visit_UAdd(self, node): return 'pos'
    def visit_USub(self, node): return 'neg'
    def visit_UnaryOp(self, node) -> JavaExpr:
        if isinstance(node.op, ast.Not):
            return JavaMethodCall(JavaIdentifier('PyBool'), 'create', [JavaUnaryOp('!', bool_value(self.visit(node.operand)))])
        return JavaMethodCall(self.visit(node.operand), self.visit(node.op), [])

    def visit_Add(self, node): return 'add'
    def visit_BitAnd(self, node): return 'and'
    def visit_BitOr(self, node): return 'or'
    def visit_BitXor(self, node): return 'xor'
    def visit_Div(self, node): return 'truediv'
    def visit_FloorDiv(self, node): return 'floordiv'
    def visit_LShift(self, node): return 'lshift'
    def visit_MatMult(self, node): return 'matmul'
    def visit_Mod(self, node): return 'mod'
    def visit_Mult(self, node): return 'mul'
    def visit_Pow(self, node): return 'pow'
    def visit_RShift(self, node): return 'rshift'
    def visit_Sub(self, node): return 'sub'
    def visit_BinOp(self, node) -> JavaExpr:
        return JavaMethodCall(self.visit(node.left), self.visit(node.op), [self.visit(node.right)])

    def visit_Lt(self, node): return 'lt'
    def visit_LtE(self, node): return 'le'
    def visit_Gt(self, node): return 'gt'
    def visit_GtE(self, node): return 'ge'
    def visit_Compare(self, node) -> JavaExpr:
        n_compares = len(node.comparators)
        assert n_compares == len(node.ops), node # should have consistent number of these
        assert n_compares >= 1, node # should always have at least one

        lhs = self.visit(node.left)
        exprs: list[JavaExpr] = []
        for (i, (op, comparator)) in enumerate(zip(node.ops, node.comparators)):
            rhs = self.visit(comparator)
            if i < n_compares - 1:
                temp_name = self.make_temp()
                self.code.append(JavaVariableDecl('PyObject', temp_name, None))
                rhs = JavaAssignExpr(JavaIdentifier(temp_name), rhs)
            else:
                temp_name = '__unused__'
            if isinstance(op, ast.Is):
                term = JavaBinaryOp('==', lhs, rhs)
            elif isinstance(op, ast.IsNot):
                term = JavaBinaryOp('!=', lhs, rhs)
            elif isinstance(op, ast.In):
                term = JavaMethodCall(lhs, 'in', [rhs])
            elif isinstance(op, ast.NotIn):
                term = JavaUnaryOp('!', JavaMethodCall(lhs, 'in', [rhs]))
            elif isinstance(op, ast.Eq):
                term = JavaMethodCall(lhs, 'equals', [rhs])
            elif isinstance(op, ast.NotEq):
                term = JavaUnaryOp('!', JavaMethodCall(lhs, 'equals', [rhs]))
            else:
                term = JavaMethodCall(lhs, self.visit(op), [rhs])
            exprs.append(term)
            if i < n_compares - 1:
                lhs = JavaIdentifier(temp_name)
        return JavaMethodCall(JavaIdentifier('PyBool'), 'create', [chained_binary_op('&&', exprs)])

    def emit_bool_op(self, op: ast.boolop, values: list[ast.expr]) -> JavaExpr:
        if len(values) == 1:
            return self.visit(values[0])
        temp_name = self.make_temp()
        self.code.append(JavaVariableDecl('PyObject', temp_name, None))
        lhs = self.visit(values[0])
        rhs = self.emit_bool_op(op, values[1:])
        if isinstance(op, ast.And):
            return JavaCondOp(bool_value(JavaAssignExpr(JavaIdentifier(temp_name), lhs)), rhs, JavaIdentifier(temp_name))
        else:
            assert isinstance(op, ast.Or), op
            return JavaCondOp(bool_value(JavaAssignExpr(JavaIdentifier(temp_name), lhs)), JavaIdentifier(temp_name), rhs)

    def visit_BoolOp(self, node) -> JavaExpr:
        assert len(node.values) >= 2, node
        return self.emit_bool_op(node.op, node.values)

    def visit_IfExp(self, node) -> JavaExpr:
        return JavaCondOp(bool_value(self.visit(node.test)), self.visit(node.body), self.visit(node.orelse))

    # XXX Note that we never actually get negative integers here in the AST
    def visit_Constant(self, node) -> JavaExpr:
        if node.value is None:
            return py_none()
        elif node.value is False or node.value is True:
            return JavaField(JavaIdentifier('PyBool'), f'{str(node.value).lower()}_singleton')
        elif isinstance(node.value, int):
            self.all_ints.add(node.value)
            return JavaIdentifier(int_name(node.value))
        elif isinstance(node.value, str):
            if node.value not in self.all_strings:
                self.all_strings[node.value] = len(self.all_strings)
            return JavaIdentifier(f'str_singleton_{self.all_strings[node.value]}')
        elif isinstance(node.value, bytes):
            if node.value not in self.all_bytes:
                self.all_bytes[node.value] = len(self.all_bytes)
            return JavaIdentifier(f'bytes_singleton_{self.all_bytes[node.value]}')
        else:
            self.error(node.lineno, f'literal {node.value!r} of type {type(node.value).__name__!r} is unsupported')
            return JavaIdentifier('__cannot_translate_constant__')

    def visit_JoinedStr(self, node) -> JavaExpr:
        if not node.values:
            return JavaField(JavaIdentifier('PyString'), 'empty_singleton')
        vals: list[JavaExpr] = []
        for val in node.values:
            if isinstance(val, ast.Constant):
                assert isinstance(val.value, str), val
                vals.append(JavaStrLiteral(val.value))
            else:
                assert isinstance(val, ast.FormattedValue), val
                if val.conversion != -1:
                    self.error(val.lineno, f'unsupported f string conversion type {val.conversion}')
                if val.format_spec is not None:
                    # Need to extract the String back out of the PyString
                    assert isinstance(val.format_spec, ast.JoinedStr), val.format_spec
                    format_spec = JavaField(self.visit(val.format_spec), 'value')
                else:
                    format_spec = JavaStrLiteral("")
                vals.append(JavaMethodCall(self.visit(val.value), 'format', [format_spec]))
        return JavaCreateObject('PyString', [chained_binary_op('+', vals)])

    def emit_star_expanded(self, nodes: list, *, array_list_allowed: bool = False) -> JavaExpr:
        if any(isinstance(arg, ast.Starred) for arg in nodes):
            args = JavaCreateObject('java.util.ArrayList<PyObject>', [])
            for arg in nodes:
                if isinstance(arg, ast.Starred):
                    args = JavaMethodCall(JavaIdentifier('Runtime'), 'addPyIterToArrayList', [args, JavaMethodCall(self.visit(arg.value), 'iter', [])])
                else:
                    args = JavaMethodCall(JavaIdentifier('Runtime'), 'addPyObjectToArrayList', [args, self.visit(arg)])
            if not array_list_allowed:
                args = JavaMethodCall(JavaIdentifier('Runtime'), 'arrayListToArray', [args])
            return args
        else:
            return JavaCreateArray('PyObject', [self.visit(x) for x in nodes])

    def visit_List(self, node) -> JavaExpr:
        args = self.emit_star_expanded(node.elts, array_list_allowed=True)
        return JavaCreateObject('PyList', [args])

    def visit_Tuple(self, node) -> JavaExpr:
        args = self.emit_star_expanded(node.elts, array_list_allowed=True)
        return JavaCreateObject('PyTuple', [args])

    def visit_Set(self, node) -> JavaExpr:
        args = self.emit_star_expanded(node.elts, array_list_allowed=True)
        return JavaCreateObject('PySet', [args])

    def visit_Dict(self, node) -> JavaExpr:
        assert len(node.keys) == len(node.values), node
        if any(k is None for k in node.keys):
            self.error(node.lineno, 'dictionary packing is unsupported')
        kv_iter = itertools.chain.from_iterable(zip(node.keys, node.values))
        return JavaCreateObject('PyDict', [JavaIdentifier('__dict_unpack_unsupported__') if x is None else self.visit(x) for x in kv_iter])

    def visit_Call(self, node) -> JavaExpr:
        func = self.visit(node.func)
        args = self.emit_star_expanded(node.args)
        if node.keywords:
            kv_list: list[JavaExpr] = []
            for kwarg in node.keywords:
                if kwarg.arg is None:
                    self.error(node.lineno, 'dictionary packing is unsupported')
                    self.visit(kwarg.value) # recurse to find more errors
                else:
                    assert isinstance(kwarg.arg, str), kwarg.arg
                    # XXX These should be stored in all_strings
                    kv_list.append(JavaCreateObject('PyString', [JavaStrLiteral(kwarg.arg)]))
                    kv_list.append(self.visit(kwarg.value))
            kwargs = JavaCreateObject('PyDict', kv_list)
        else:
            kwargs = JavaIdentifier('null')
        return JavaMethodCall(func, 'call', [args, kwargs])

    def visit_Name(self, node) -> JavaExpr:
        return self.ident_expr(node.id)

    def visit_Subscript(self, node) -> JavaExpr:
        return JavaMethodCall(self.visit(node.value), 'getItem', [self.visit(node.slice)])

    def visit_Attribute(self, node) -> JavaExpr:
        return JavaMethodCall(self.visit(node.value), 'getAttr', [JavaStrLiteral(node.attr)])

    def visit_Slice(self, node) -> JavaExpr:
        lower = self.visit(node.lower) if node.lower else py_none()
        upper = self.visit(node.upper) if node.upper else py_none()
        step = self.visit(node.step) if node.step else py_none()
        return JavaCreateObject('PySlice', [lower, upper, step])

    # XXX Change all statements to -> Iterator[JavaStatement] and yield statements?
    def visit_Pass(self, node):
        pass

    def visit_Assign(self, node):
        if len(node.targets) != 1:
            self.generic_visit(node) # error
            self.visit(node.value) # recurse to find more errors
            return
        target = node.targets[0] 

        if isinstance(target, ast.Name):
            self.names.add(target.id)
            code = JavaAssignStatement(self.visit(target), self.visit(node.value))
        elif isinstance(target, ast.Attribute):
            code = JavaExprStatement(JavaMethodCall(self.visit(target.value), 'setAttr', [JavaStrLiteral(target.attr), self.visit(node.value)]))
        elif isinstance(target, ast.Subscript):
            code = JavaExprStatement(JavaMethodCall(self.visit(target.value), 'setItem', [self.visit(target.slice), self.visit(node.value)]))
        else:
            self.generic_visit(node) # error
            self.visit(node.value) # recurse to find more errors
            return
        self.code.append(code)

    def visit_AugAssign(self, node):
        op = f'{self.visit(node.op)}InPlace'

        if isinstance(node.target, ast.Name):
            code = JavaAssignStatement(self.visit(node.target), JavaMethodCall(self.visit(node.target), op, [self.visit(node.value)]))
        elif isinstance(node.target, ast.Attribute):
            temp_name = self.make_temp()
            self.code.append(JavaVariableDecl('var', temp_name, self.visit(node.target.value)))
            code = JavaExprStatement(JavaMethodCall(JavaIdentifier(temp_name), 'setAttr', [
                JavaStrLiteral(node.target.attr),
                JavaMethodCall(
                    JavaMethodCall(JavaIdentifier(temp_name), 'getAttr', [JavaStrLiteral(node.target.attr)]),
                    op,
                    [self.visit(node.value)]
                )
            ]))
        elif isinstance(node.target, ast.Subscript):
            temp_name0 = self.make_temp()
            temp_name1 = self.make_temp()
            self.code.append(JavaVariableDecl('var', temp_name0, self.visit(node.target.value)))
            self.code.append(JavaVariableDecl('var', temp_name1, self.visit(node.target.slice)))
            code = JavaExprStatement(JavaMethodCall(JavaIdentifier(temp_name0), 'setItem', [
                JavaIdentifier(temp_name1),
                JavaMethodCall(
                    JavaMethodCall(JavaIdentifier(temp_name0), 'getItem', [JavaIdentifier(temp_name1)]),
                    op,
                    [self.visit(node.value)]
                )
            ]))
        else:
            self.error(node.lineno, f'augmented assignment to {type(node.target).__name__} is unsupported')
            self.visit(node.value) # recurse to find more errors
            return
        self.code.append(code)

    def visit_Assert(self, node):
        cond = JavaUnaryOp('!', bool_value(self.visit(node.test)))
        msg = JavaStrLiteral(self.path + f':{node.lineno}: assertion failure')
        if node.msg:
            msg.s += ': '
            msg = JavaBinaryOp('+', msg, JavaMethodCall(self.visit(node.msg), 'repr', []))
        exception = JavaCreateObject('AssertionError', [msg])
        self.code.append(JavaIfStatement(cond, [JavaThrowStatement(exception)], []))

    def visit_Delete(self, node):
        for target in node.targets:
            if isinstance(target, ast.Attribute):
                code = JavaExprStatement(JavaMethodCall(self.visit(target.value), 'delAttr', [JavaStrLiteral(target.attr)]))
            elif isinstance(target, ast.Subscript):
                code = JavaExprStatement(JavaMethodCall(self.visit(target.value), 'delItem', [self.visit(target.slice)]))
            else:
                self.error(node.lineno, f"'del' of {type(target).__name__} is unsupported")
                continue
            self.code.append(code)

    def visit_Return(self, node):
        assert self.in_function, node
        self.has_returns = True
        value = self.visit(node.value) if node.value else py_none()
        self.code.append(JavaReturnStatement(value))

    @contextmanager
    def new_block(self):
        saved_code = self.code
        self.code = []
        yield self.code
        self.code = saved_code

    def visit_If(self, node):
        cond = bool_value(self.visit(node.test))
        with self.new_block() as body:
            for statement in node.body:
                self.visit(statement)
        with self.new_block() as orelse:
            for statement in node.orelse:
                self.visit(statement)
        self.code.append(JavaIfStatement(cond, body, orelse))

    def visit_While(self, node):
        cond = bool_value(self.visit(node.test))
        with self.new_block() as body:
            for statement in node.body:
                self.visit(statement)
        self.code.append(JavaWhileStatement(cond, body))

        if node.orelse:
            self.error(node.lineno, "'else:' clauses on 'while' statements are unsupported")
            for statement in node.orelse:
                self.visit(statement) # recursively visit to find all errors

    def visit_For(self, node):
        if not isinstance(node.target, ast.Name):
            self.error(node.lineno, f"'for' statement target is unsupported: {node.target}")
            return
        target = node.target.id
        self.names.add(target)

        temp_name0 = self.make_temp()
        temp_name1 = self.make_temp()
        self.code.append(JavaVariableDecl('var', temp_name0, JavaMethodCall(self.visit(node.iter), 'iter', [])))

        with self.new_block() as body:
            for statement in node.body:
                self.visit(statement)

        self.code.append(JavaForStatement(
            'var', temp_name1, JavaMethodCall(JavaIdentifier(temp_name0), 'next', []),
            JavaBinaryOp('!=', JavaIdentifier(temp_name1), JavaIdentifier('null')),
            temp_name1, JavaMethodCall(JavaIdentifier(temp_name0), 'next', []),
            [
                JavaAssignStatement(self.ident_expr(target), JavaIdentifier(temp_name1)),
                *body,
            ]
        ))

        if node.orelse:
            self.error(node.lineno, "'else:' clauses on 'for' statements are unsupported")
            for statement in node.orelse:
                self.visit(statement) # recursively visit to find all errors

    def visit_Break(self, node):
        self.code.append(JavaBreakStatement())

    def visit_Continue(self, node):
        self.code.append(JavaContinueStatement())

    def visit_Expr(self, node):
        value = self.visit(node.value)
        if isinstance(value, (JavaMethodCall, JavaCreateObject)): # allowed by Java grammar as statements
            self.code.append(JavaExprStatement(value))
        else:
            # Avoid "not a statement" javac errors by assigning otherwise-unused values to a temp.
            # Cannot remove these statements because we rely on javac here to catch some portion of
            # Python usage errors.
            self.code.append(JavaAssignStatement(JavaIdentifier('expr_discard'), value))
            self.used_expr_discard = True

    def visit_FunctionDef(self, node):
        if self.in_function:
            self.error(node.lineno, 'nested function definitions are unsupported')
            return

        # node.type_comment is ignored; we only plan to support "real" type annotations.
        if node.decorator_list:
            self.error(node.lineno, 'function decorators are unsupported')
        if node.returns:
            self.error(node.lineno, 'function return annotations are unsupported')
        if node.type_params:
            self.error(node.lineno, 'function type parameters are unsupported')

        if node.args.posonlyargs:
            self.error(node.lineno, 'position-only arguments are unsupported')
        if node.args.vararg:
            self.error(node.lineno, '*args are unsupported')
        if node.args.kwonlyargs:
            self.error(node.lineno, 'kw-only arguments are unsupported')
        if node.args.kw_defaults:
            self.error(node.lineno, 'kw-only argument defaults are unsupported')
        if node.args.kwarg:
            self.error(node.lineno, '**kwargs are unsupported')
        if node.args.defaults:
            self.error(node.lineno, 'argument defaults are unsupported')
        for arg in node.args.args:
            # arg.type_comment is ignored; we only plan to support "real" type annotations.
            if arg.annotation:
                self.error(node.lineno, 'argument type annotations are unsupported')

        n_args = len(node.args.args)
        self.global_names.add(node.name)
        expr = JavaCreateObject(f'pyfunc_{node.name}', [])
        self.global_code.append(JavaAssignStatement(JavaIdentifier(f'pyglobal_{node.name}'), expr))

        self.in_function = True
        self.has_returns = False
        self.used_expr_discard = False
        self.names = set()

        # Number temps in each function starting from temp0 for improved clarity/stability of generated code
        saved_temps = self.n_temps
        self.n_temps = 0
        with self.new_block() as body:
            for statement in node.body:
                self.visit(statement)
        self.n_temps = saved_temps

        func_code: list[JavaStatement] = [
            JavaIfStatement(
                JavaBinaryOp('!=', JavaField(JavaIdentifier('args'), 'length'), JavaIntLiteral(n_args, '')),
                [JavaThrowStatement(JavaCreateObject('RuntimeException', [JavaStrLiteral(f'{node.name}() takes {n_args} arguments')]))],
                [],
            ),
            JavaIfStatement(
                JavaBinaryOp('!=', JavaIdentifier('kwargs'), JavaIdentifier('null')),
                [JavaThrowStatement(JavaCreateObject('RuntimeException', [JavaStrLiteral(f'{node.name}() does not accept kwargs')]))],
                [],
            ),
            *(JavaVariableDecl(
                'PyObject', f'pylocal_{arg.arg}', JavaArrayAccess(JavaIdentifier('args'), JavaIntLiteral(i, ''))
            ) for (i, arg) in enumerate(node.args.args)),
            *((JavaVariableDecl('PyObject', 'expr_discard', None),) if self.used_expr_discard else ()),
            # XXX It's tempting to assign Java null here, but this makes it far too easy for null to leak out into the runtime
            *(JavaVariableDecl('PyObject', f'pylocal_{name}', None) for name in sorted(self.names)),
            *body,
        ]
        # XXX need this in some, but not all, cases with has_returns (avoid "unreachable statement" javac errors)
        if not self.has_returns:
            func_code.append(JavaReturnStatement(py_none()))

        self.functions[node.name] = [
            f'private static final class pyfunc_{node.name} extends PyTruthyObject {{',
            '@Override public PyObject call(PyObject[] args, PyDict kwargs) {',
            *itertools.chain.from_iterable(s.emit_java() for s in func_code),
            '}',
            '}',
        ]

        self.in_function = False
        self.names = self.global_names

    def visit_Module(self, node):
        for statement in node.body:
            self.visit(statement)

    def write_java(self, f: TextIO, py_name: str):
        writer = IndentedWriter(f, 0)
        writer.write(f'public final class {py_name} {{')
        for i in sorted(self.all_ints):
            value = JavaCreateObject('PyInt', [JavaIntLiteral(i, 'L')])
            writer.write(f'private static final PyInt {int_name(i)} = {value.emit_java()};')
        for (k, v) in sorted(self.all_strings.items()):
            value = JavaCreateObject('PyString', [JavaStrLiteral(k)])
            writer.write(f'private static final PyString str_singleton_{v} = {value.emit_java()};')
        for (k, v) in sorted(self.all_bytes.items()):
            value = JavaCreateObject('PyBytes', [JavaCreateArray('byte', [JavaIntLiteral(((x + 0x80) & 0xFF) - 0x80, '') for x in k])])
            writer.write(f'private static final PyBytes bytes_singleton_{v} = {value.emit_java()};')
        writer.write('')

        for (name, code) in sorted(self.functions.items()):
            for line in code:
                writer.write(line)
            writer.write('')

        # XXX Initializing all globals to None is weird, but we don't have a better option yet
        for name in sorted(self.global_names):
            writer.write(f'private static PyObject pyglobal_{name} = PyNone.singleton;')
        writer.write('')

        writer.write('public static void main(String[] args) {')
        for s in self.global_code:
            for line in s.emit_java():
                writer.write(line)
        writer.write('}')
        writer.write('}')
        assert writer.indent == 0, writer.indent

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('py_names', nargs='*', help='names of tests to run')
    args = parser.parse_args()

    py_names = args.py_names
    if not py_names:
        py_names = sorted(x[:-3] for x in os.listdir('tests') if x.endswith('.py'))

    start = time.perf_counter()
    for path in glob.glob('runtime/*.class'):
        os.unlink(path)
    subprocess.check_call(['javac', *RUNTIME_JAVA_FILES], cwd='runtime')
    subprocess.check_call(['jar', 'cf', '../tests/pythonj.jar', '*.class'], cwd='runtime')
    for path in glob.glob('runtime/*.class'):
        os.unlink(path)
    initial_javac_time = time.perf_counter() - start
    print(f'initial_javac_time={initial_javac_time:.3f}')

    start = time.perf_counter()
    for py_name in py_names:
        py_path = f'tests/{py_name}.py'
        with open(py_path) as f:
            node = ast.parse(f.read())
        visitor = PythonjVisitor(py_path)
        visitor.visit(node)
        if visitor.n_errors:
            print(f'Translation failed: {visitor.n_errors} errors')
            exit(1)
        with open(f'tests/{py_name}.java', 'w') as f:
            visitor.write_java(f, py_name)
    translate_time = time.perf_counter() - start
    print(f'translate_time={translate_time:5.3f}')

    start = time.perf_counter()
    subprocess.check_call(['javac', '-cp', 'pythonj.jar', *(f'{py_name}.java' for py_name in py_names)], cwd='tests')
    javac_time = time.perf_counter() - start
    print(f'javac_time={javac_time:5.3f}')

    for py_name in py_names:
        start = time.perf_counter()
        sep = ';' if os.name == 'nt' else ':'
        j_output = subprocess.check_output(['java', '-cp', f'pythonj.jar{sep}.', py_name], cwd='tests')
        jexec_time = time.perf_counter() - start

        start = time.perf_counter()
        c_output = subprocess.check_output([sys.executable, f'{py_name}.py'], cwd='tests')
        pyexec_time = time.perf_counter() - start

        if j_output == c_output:
            print(f'{py_name:>15}: jexec_time={jexec_time:5.3f} pyexec_time={pyexec_time:5.3f} ({pyexec_time / jexec_time:5.2f}x)')
        else:
            print()
            print(f'ERROR: output mismatched on test {py_name!r}:')
            j_output = j_output.decode(errors='surrogateescape').splitlines()
            c_output = c_output.decode(errors='surrogateescape').splitlines()
            for line in difflib.unified_diff(j_output, c_output, 'pythonj output', 'cpython output', lineterm=''):
                print(line)
            exit(1)

    for path in glob.glob('tests/*.class'):
        os.unlink(path)

if __name__ == '__main__':
    main()
