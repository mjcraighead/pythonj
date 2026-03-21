# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

from abc import ABC, abstractmethod
import argparse
import ast
import builtins
from contextlib import contextmanager
from dataclasses import dataclass
import difflib
from enum import Enum
import itertools
import json
import os
import shutil
import subprocess
import sys
import time
from types import ClassMethodDescriptorType, GetSetDescriptorType, MemberDescriptorType, MethodDescriptorType, NoneType
from typing import Iterator, Optional, TextIO
import _io

BUILTIN_FUNCTIONS = {
    'abs', 'all', 'any', 'ascii', 'chr', 'delattr', 'dir', 'format', 'getattr', 'hasattr', 'hash', 'hex',
    'isinstance', 'issubclass', 'iter', 'len', 'max', 'min', 'next', 'open', 'ord', 'print', 'repr',
    'setattr', 'sorted', 'sum',
}
BUILTIN_TYPES = {
    'bool': 'PyBool',
    'bytearray': 'PyByteArray',
    'bytes': 'PyBytes',
    'dict': 'PyDict',
    'enumerate': 'PyEnumerate',
    'int': 'PyInt',
    'list': 'PyList',
    'object': 'PyObject',
    'range': 'PyRange',
    'reversed': 'PyReversed',
    'set': 'PySet',
    'slice': 'PySlice',
    'str': 'PyString',
    'tuple': 'PyTuple',
    'type': 'PyType',
    'zip': 'PyZip',
}
EXCEPTION_TYPES = {
    'ArithmeticError', 'AssertionError', 'AttributeError', 'BaseException', 'Exception', 'IndexError',
    'KeyError', 'LookupError', 'StopIteration', 'TypeError', 'ValueError', 'ZeroDivisionError',
}

RUNTIME_JAVA_FILES = (
    'PyBool.java',
    'PyBuiltinFunctions.java',
    'PyByteArray.java',
    'PyBytes.java',
    'PyDict.java',
    'PyEnumerate.java',
    'PyExceptions.java',
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

class EmitContext:
    __slots__ = ('all_ints', 'all_strings', 'all_bytes')
    all_ints: set[int]
    all_strings: dict[str, int]
    all_bytes: dict[bytes, int]

    def __init__(self):
        self.all_ints = set()
        self.all_strings = {}
        self.all_bytes = {}

    def emit_java(self) -> Iterator[str]:
        for i in sorted(self.all_ints):
            value = JavaCreateObject('PyInt', [JavaIntLiteral(i, 'L')])
            yield f'private static final PyInt {int_name(i)} = {value.emit_java(self)};'
        for (k, v) in sorted(self.all_strings.items()):
            value = JavaCreateObject('PyString', [JavaStrLiteral(k)])
            yield f'private static final PyString str_singleton_{v} = {value.emit_java(self)};'
        for (k, v) in sorted(self.all_bytes.items()):
            value = JavaCreateObject('PyBytes', [JavaCreateArray('byte', [JavaIntLiteral(((x + 0x80) & 0xFF) - 0x80, '') for x in k])])
            yield f'private static final PyBytes bytes_singleton_{v} = {value.emit_java(self)};'

class JavaExpr(ABC):
    @abstractmethod
    def emit_java(self, ctx: EmitContext) -> str:
        raise NotImplementedError()

@dataclass(slots=True)
class JavaIntLiteral(JavaExpr):
    value: int
    suffix: str
    def emit_java(self, ctx: EmitContext) -> str:
        return f'{self.value}{self.suffix}'

@dataclass(slots=True)
class JavaStrLiteral(JavaExpr):
    s: str
    def emit_java(self, ctx: EmitContext) -> str:
        return java_string_literal(self.s)

@dataclass(slots=True)
class JavaIdentifier(JavaExpr):
    name: str
    def emit_java(self, ctx: EmitContext) -> str:
        return self.name

@dataclass(slots=True)
class JavaField(JavaExpr):
    obj: JavaExpr
    field: str
    def emit_java(self, ctx: EmitContext) -> str:
        return f'{self.obj.emit_java(ctx)}.{self.field}'

@dataclass(slots=True)
class JavaArrayAccess(JavaExpr):
    obj: JavaExpr
    index: JavaExpr
    def emit_java(self, ctx: EmitContext) -> str:
        return f'{self.obj.emit_java(ctx)}[{self.index.emit_java(ctx)}]'

@dataclass(slots=True)
class JavaUnaryOp(JavaExpr):
    op: str
    operand: JavaExpr
    def emit_java(self, ctx: EmitContext) -> str:
        return f'({self.op}{self.operand.emit_java(ctx)})'

@dataclass(slots=True)
class JavaBinaryOp(JavaExpr):
    op: str
    lhs: JavaExpr
    rhs: JavaExpr
    def emit_java(self, ctx: EmitContext) -> str:
        return f'({self.lhs.emit_java(ctx)} {self.op} {self.rhs.emit_java(ctx)})'

@dataclass(slots=True)
class JavaCondOp(JavaExpr):
    cond: JavaExpr
    true: JavaExpr
    false: JavaExpr
    def emit_java(self, ctx: EmitContext) -> str:
        return f'({self.cond.emit_java(ctx)} ? {self.true.emit_java(ctx)} : {self.false.emit_java(ctx)})'

@dataclass(slots=True)
class JavaCreateObject(JavaExpr):
    type: str
    args: list[JavaExpr]
    def emit_java(self, ctx: EmitContext) -> str:
        return f"new {self.type}({', '.join(arg.emit_java(ctx) for arg in self.args)})"

@dataclass(slots=True)
class JavaCreateArray(JavaExpr):
    type: str
    elts: list[JavaExpr]
    def emit_java(self, ctx: EmitContext) -> str:
        return f"new {self.type}[] {{{', '.join(x.emit_java(ctx) for x in self.elts)}}}"

@dataclass(slots=True)
class JavaMethodCall(JavaExpr):
    obj: JavaExpr
    method: str
    args: list[JavaExpr]
    def emit_java(self, ctx: EmitContext) -> str:
        return f"{self.obj.emit_java(ctx)}.{self.method}({', '.join(arg.emit_java(ctx) for arg in self.args)})"

@dataclass(slots=True)
class JavaAssignExpr(JavaExpr):
    lhs: JavaExpr
    rhs: JavaExpr
    def emit_java(self, ctx: EmitContext) -> str:
        return f'({self.lhs.emit_java(ctx)} = {self.rhs.emit_java(ctx)})'

@dataclass(slots=True)
class JavaPyConstant(JavaExpr):
    value: None | bool | int | str | bytes
    def emit_java(self, ctx: EmitContext) -> str:
        if self.value is None:
            return 'PyNone.singleton'
        elif self.value is False or self.value is True:
            return f'PyBool.{str(self.value).lower()}_singleton'
        elif isinstance(self.value, int):
            if -1 <= self.value <= 1:
                if self.value < 0:
                    return f'PyInt.singleton_neg{-self.value}'
                else:
                    return f'PyInt.singleton_{self.value}'
            ctx.all_ints.add(self.value)
            return int_name(self.value)
        elif isinstance(self.value, str):
            if not self.value:
                return 'PyString.empty_singleton'
            if self.value not in ctx.all_strings:
                ctx.all_strings[self.value] = len(ctx.all_strings)
            return f'str_singleton_{ctx.all_strings[self.value]}'
        else:
            assert isinstance(self.value, bytes), self.value
            if self.value not in ctx.all_bytes:
                ctx.all_bytes[self.value] = len(ctx.all_bytes)
            return f'bytes_singleton_{ctx.all_bytes[self.value]}'

class JavaStatement(ABC):
    def ends_control_flow(self) -> bool:
        return False

    @abstractmethod
    def emit_java(self, ctx: EmitContext) -> Iterator[str]:
        raise NotImplementedError()

@dataclass(slots=True)
class JavaVariableDecl(JavaStatement):
    type: str
    name: str
    value: Optional[JavaExpr]
    def emit_java(self, ctx: EmitContext) -> Iterator[str]:
        if self.value:
            yield f'{self.type} {self.name} = {self.value.emit_java(ctx)};'
        else:
            yield f'{self.type} {self.name};'

@dataclass(slots=True)
class JavaAssignStatement(JavaStatement):
    lhs: JavaExpr
    rhs: JavaExpr
    def emit_java(self, ctx: EmitContext) -> Iterator[str]:
        yield f'{self.lhs.emit_java(ctx)} = {self.rhs.emit_java(ctx)};'

@dataclass(slots=True)
class JavaExprStatement(JavaStatement):
    call: JavaCreateObject | JavaMethodCall # only limited types of expressions allowed by Java grammar
    def emit_java(self, ctx: EmitContext) -> Iterator[str]:
        yield f'{self.call.emit_java(ctx)};'

@dataclass(slots=True)
class JavaBreakStatement(JavaStatement):
    name: Optional[str]
    def ends_control_flow(self) -> bool:
        return True
    def emit_java(self, ctx: EmitContext) -> Iterator[str]:
        yield f'break {self.name};' if self.name else 'break;'

@dataclass(slots=True)
class JavaContinueStatement(JavaStatement):
    def ends_control_flow(self) -> bool:
        return True
    def emit_java(self, ctx: EmitContext) -> Iterator[str]:
        yield 'continue;'

@dataclass(slots=True)
class JavaReturnStatement(JavaStatement):
    expr: JavaExpr
    def ends_control_flow(self) -> bool:
        return True
    def emit_java(self, ctx: EmitContext) -> Iterator[str]:
        yield f'return {self.expr.emit_java(ctx)};'

@dataclass(slots=True)
class JavaThrowStatement(JavaStatement):
    expr: JavaExpr
    def ends_control_flow(self) -> bool:
        return True
    def emit_java(self, ctx: EmitContext) -> Iterator[str]:
        yield f'throw {self.expr.emit_java(ctx)};'

def block_simplify(block: list[JavaStatement]) -> list[JavaStatement]:
    ret = []
    for s in block:
        ret.append(s)
        if s.ends_control_flow():
            break
    return ret

def block_emit_java(block: list[JavaStatement], ctx: EmitContext) -> Iterator[str]:
    for s in block:
        yield from s.emit_java(ctx)

def block_ends_control_flow(block: list[JavaStatement]) -> bool:
    return bool(block) and block[-1].ends_control_flow()

@dataclass(slots=True)
class JavaIfStatement(JavaStatement):
    cond: JavaExpr
    body: list[JavaStatement]
    orelse: list[JavaStatement]

    def __post_init__(self):
        self.body = block_simplify(self.body)
        self.orelse = block_simplify(self.orelse)

    def ends_control_flow(self) -> bool:
        return block_ends_control_flow(self.body) and block_ends_control_flow(self.orelse)

    def emit_java(self, ctx: EmitContext) -> Iterator[str]:
        yield f'if ({self.cond.emit_java(ctx)}) {{'
        yield from block_emit_java(self.body, ctx)
        if self.orelse:
            yield '} else {'
            yield from block_emit_java(self.orelse, ctx)
        yield '}'

@dataclass(slots=True)
class JavaWhileStatement(JavaStatement):
    cond: JavaExpr
    body: list[JavaStatement]

    def __post_init__(self):
        self.body = block_simplify(self.body)

    def emit_java(self, ctx: EmitContext) -> Iterator[str]:
        yield f'while ({self.cond.emit_java(ctx)}) {{'
        yield from block_emit_java(self.body, ctx)
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

    def __post_init__(self):
        self.body = block_simplify(self.body)

    def emit_java(self, ctx: EmitContext) -> Iterator[str]:
        yield f'for ({self.init_type} {self.init_name} = {self.init_value.emit_java(ctx)}; {self.cond.emit_java(ctx)}; {self.incr_name} = {self.incr_value.emit_java(ctx)}) {{'
        yield from block_emit_java(self.body, ctx)
        yield '}'

@dataclass(slots=True)
class JavaTryStatement(JavaStatement):
    try_body: list[JavaStatement]
    exc_type: Optional[str]
    exc_name: Optional[str]
    catch_body: list[JavaStatement]
    finally_body: list[JavaStatement]

    def __post_init__(self):
        self.try_body = block_simplify(self.try_body)
        self.catch_body = block_simplify(self.catch_body)
        self.finally_body = block_simplify(self.finally_body)

    def ends_control_flow(self) -> bool:
        if block_ends_control_flow(self.finally_body):
            return True
        if self.exc_type is not None:
            return block_ends_control_flow(self.try_body) and block_ends_control_flow(self.catch_body)
        else:
            return block_ends_control_flow(self.try_body)

    def emit_java(self, ctx: EmitContext) -> Iterator[str]:
        yield 'try {'
        yield from block_emit_java(self.try_body, ctx)
        if self.exc_type is not None:
            yield f'}} catch ({self.exc_type} {self.exc_name}) {{'
            yield from block_emit_java(self.catch_body, ctx)
        else: # if no exception type, should not have an exception name or catch block either
            assert self.exc_name is None, self.exc_name
            assert not self.catch_body, self.catch_body
        if self.finally_body:
            yield '} finally {'
            yield from block_emit_java(self.finally_body, ctx)
        yield '}'

@dataclass(slots=True)
class JavaLabeledBlock(JavaStatement):
    name: str
    body: list[JavaStatement]

    def __post_init__(self):
        self.body = block_simplify(self.body)

    def emit_java(self, ctx: EmitContext) -> Iterator[str]:
        yield f'{self.name}: {{'
        yield from block_emit_java(self.body, ctx)
        yield '}'

def unary_op(op: str, operand: JavaExpr) -> JavaExpr:
    if op == '!' and isinstance(operand, JavaIdentifier):
        if operand.name == 'false':
            return JavaIdentifier('true')
        if operand.name == 'true':
            return JavaIdentifier('false')
    return JavaUnaryOp(op, operand)

def bool_value(expr: JavaExpr) -> JavaExpr:
    if (isinstance(expr, JavaMethodCall) and isinstance(expr.obj, JavaIdentifier) and
        expr.obj.name == 'PyBool' and expr.method == 'create' and len(expr.args) == 1):
        return expr.args[0] # return the raw boolean instead of box/unbox
    if isinstance(expr, JavaPyConstant):
        return JavaIdentifier('true') if expr.value else JavaIdentifier('false')
    return JavaMethodCall(expr, 'boolValue', [])

def chained_binary_op(op: str, exprs: list[JavaExpr]) -> JavaExpr:
    assert len(exprs) >= 1, exprs
    expr = exprs[0]
    for term in exprs[1:]:
        expr = JavaBinaryOp(op, expr, term)
    return expr

def if_statement(cond: JavaExpr, body: list[JavaStatement], orelse: list[JavaStatement]) -> Iterator[JavaStatement]:
    if isinstance(cond, JavaIdentifier) and cond.name == 'true':
        yield from body
    elif isinstance(cond, JavaIdentifier) and cond.name == 'false':
        yield from orelse
    else:
        yield JavaIfStatement(cond, body, orelse)

def while_statement(cond: JavaExpr, body: list[JavaStatement]) -> Iterator[JavaStatement]:
    if isinstance(cond, JavaIdentifier) and cond.name == 'false':
        pass
    else:
        yield JavaWhileStatement(cond, body)

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

class SymbolTableVisitor(ast.NodeVisitor):
    __slots__ = ('reads', 'writes', 'globals', 'nonlocals')
    reads: set[str]
    writes: set[str]
    globals: set[str]
    nonlocals: set[str]

    def __init__(self):
        self.reads = set()
        self.writes = set()
        self.globals = set()
        self.nonlocals = set()

    def visit_Name(self, node):
        if isinstance(node.ctx, (ast.Store, ast.Del)):
            self.writes.add(node.id)
        else:
            self.reads.add(node.id)

    def visit_ExceptHandler(self, node):
        if node.type is not None:
            self.visit(node.type)
        if node.name is not None:
            self.writes.add(node.name)
        for statement in node.body:
            self.visit(statement)

    def visit_Global(self, node) -> None:
        for name in node.names:
            self.globals.add(name)

    def visit_Nonlocal(self, node) -> None:
        for name in node.names:
            self.nonlocals.add(name)

    # Do not descend into nested scopes; only record function or class name as a write
    def visit_FunctionDef(self, node):
        self.writes.add(node.name)
    def visit_AsyncFunctionDef(self, node):
        self.writes.add(node.name)
    def visit_Lambda(self, node):
        pass
    def visit_ClassDef(self, node):
        self.writes.add(node.name)

    # For comprehensions, only descend into generators[0].iter; the rest is evaluated inside the lambda
    def _visit_comp(self, node):
        self.visit(node.generators[0].iter)
    def visit_ListComp(self, node):
        self._visit_comp(node)
    def visit_SetComp(self, node):
        self._visit_comp(node)
    def visit_DictComp(self, node):
        self._visit_comp(node)
    def visit_GeneratorExp(self, node):
        self._visit_comp(node)

class ScopeKind(Enum):
    MODULE = 1
    FUNCTION = 2

class Scope:
    __slots__ = ('parent', 'kind', 'locals', 'explicit_globals', 'n_temps', 'used_expr_discard')
    parent: Optional['Scope']
    kind: ScopeKind
    locals: set[str]
    explicit_globals: set[str]
    n_temps: int
    used_expr_discard: bool

    def __init__(self, parent: Optional['Scope'], kind: ScopeKind):
        self.parent = parent
        self.kind = kind
        self.locals = set()
        self.explicit_globals = set()
        self.n_temps = 0
        self.used_expr_discard = False

    def make_temp(self) -> str:
        """Assign and return a new temporary variable name."""
        temp_name = f'temp{self.n_temps}'
        self.n_temps += 1
        return temp_name

# XXX Need to design a systematic way to avoid "code too large" and "too many constants" errors.
# This is somewhat challenging, as even a single Python expression or statement can easily overflow
# the maximum code size limit, and it is somewhat unpredictable how much bytecode our translations
# will compile into.  Partial mitigations are likely to be easier than a total fix.
# XXX "invokedynamic" might help us a lot, but there is no way to access it from Java source
class PythonjVisitor(ast.NodeVisitor):
    __slots__ = ('path', 'n_errors', 'n_lambdas', 'scope', 'global_code', 'code',
                 'emit_ctx', 'break_name', 'functions', 'allow_intrinsics')
    path: str
    n_errors: int
    n_lambdas: int
    scope: Scope
    global_code: list[JavaStatement]
    code: list[JavaStatement]
    emit_ctx: EmitContext
    break_name: Optional[str]
    functions: dict[str, list[str]]
    allow_intrinsics: bool # enables special internal-only codegen features for builtins

    def __init__(self, path: str):
        self.path = path
        self.n_errors = 0
        self.n_lambdas = 0
        self.scope = Scope(None, ScopeKind.MODULE)
        self.global_code = [JavaVariableDecl('PyObject', 'expr_discard', None)] # XXX remove this variable if not needed
        self.code = self.global_code
        self.emit_ctx = EmitContext()
        self.break_name = None
        self.functions = {}
        self.allow_intrinsics = False

    # Note: if n_errors > 0, the generated Java code is not expected to be valid or executable.
    def error(self, lineno: Optional[int], msg: str) -> None:
        if lineno is None:
            print(f'ERROR: {self.path}: {msg}')
        else:
            print(f'ERROR: {self.path}:{lineno}: {msg}')
        self.n_errors += 1

    def ident_expr(self, name: str) -> JavaExpr:
        if self.allow_intrinsics and name == '__pythonj_null__':
            return JavaIdentifier('null')
        elif self.scope.kind == ScopeKind.FUNCTION and name in self.scope.locals:
            return JavaIdentifier(f'pylocal_{name}')
        elif name in BUILTIN_TYPES:
            return JavaField(JavaIdentifier(f'{BUILTIN_TYPES[name]}Type'), 'singleton')
        elif name in EXCEPTION_TYPES:
            return JavaField(JavaIdentifier(f'Py{name}Type'), 'singleton')
        elif name in BUILTIN_FUNCTIONS:
            return JavaField(JavaIdentifier(f'PyBuiltinFunction_{name}'), 'singleton')
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
            return JavaMethodCall(JavaIdentifier('PyBool'), 'create', [unary_op('!', bool_value(self.visit(node.operand)))])
        op = self.visit(node.op)
        operand = self.visit(node.operand)
        if isinstance(operand, JavaPyConstant) and isinstance(operand.value, int):
            match op:
                case 'pos': return JavaPyConstant(+operand.value)
                case 'neg': return JavaPyConstant(-operand.value)
        return JavaMethodCall(operand, op, [])

    def visit_Add(self, node): return 'add'
    def visit_BitAnd(self, node): return 'and'
    def visit_BitOr(self, node): return 'or'
    def visit_BitXor(self, node): return 'xor'
    def visit_Div(self, node): return 'trueDiv'
    def visit_FloorDiv(self, node): return 'floorDiv'
    def visit_LShift(self, node): return 'lshift'
    def visit_MatMult(self, node): return 'matmul'
    def visit_Mod(self, node): return 'mod'
    def visit_Mult(self, node): return 'mul'
    def visit_Pow(self, node): return 'pow'
    def visit_RShift(self, node): return 'rshift'
    def visit_Sub(self, node): return 'sub'
    def visit_BinOp(self, node) -> JavaExpr:
        op = self.visit(node.op)
        lhs = self.visit(node.left)
        rhs = self.visit(node.right)
        if (isinstance(lhs, JavaPyConstant) and isinstance(lhs.value, int) and
            isinstance(rhs, JavaPyConstant) and isinstance(rhs.value, int)):
            match op: # be careful to not raise an exception here or do anything platform-dependent
                case 'add':
                    return JavaPyConstant(lhs.value + rhs.value)
                case 'and':
                    return JavaPyConstant(lhs.value & rhs.value)
                case 'or':
                    return JavaPyConstant(lhs.value | rhs.value)
                case 'xor':
                    return JavaPyConstant(lhs.value ^ rhs.value)
                case 'lshift' if rhs.value >= 0:
                    return JavaPyConstant(lhs.value << rhs.value)
                case 'mul':
                    return JavaPyConstant(lhs.value * rhs.value)
                case 'rshift' if rhs.value >= 0:
                    return JavaPyConstant(lhs.value >> rhs.value)
                case 'sub':
                    return JavaPyConstant(lhs.value - rhs.value)
        return JavaMethodCall(lhs, op, [rhs])

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
                temp_name = self.scope.make_temp()
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
                term = unary_op('!', JavaMethodCall(lhs, 'in', [rhs]))
            elif isinstance(op, ast.Eq):
                term = JavaMethodCall(lhs, 'equals', [rhs])
            elif isinstance(op, ast.NotEq):
                term = unary_op('!', JavaMethodCall(lhs, 'equals', [rhs]))
            else:
                term = JavaMethodCall(lhs, self.visit(op), [rhs])
            exprs.append(term)
            if i < n_compares - 1:
                lhs = JavaIdentifier(temp_name)
        return JavaMethodCall(JavaIdentifier('PyBool'), 'create', [chained_binary_op('&&', exprs)])

    def emit_bool_op(self, op: ast.boolop, values: list[ast.expr]) -> JavaExpr:
        if len(values) == 1:
            return self.visit(values[0])
        temp_name = self.scope.make_temp()
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

    def visit_Constant(self, node) -> JavaExpr:
        if isinstance(node.value, (NoneType, bool, int, str, bytes)):
            return JavaPyConstant(node.value)
        else:
            self.error(node.lineno, f'literal {node.value!r} of type {type(node.value).__name__!r} is unsupported')
            return JavaIdentifier('__cannot_translate_constant__')

    def visit_JoinedStr(self, node) -> JavaExpr:
        if not node.values:
            return JavaPyConstant('')
        vals: list[JavaExpr] = []
        for val in node.values:
            if isinstance(val, ast.Constant):
                assert isinstance(val.value, str), val
                vals.append(JavaStrLiteral(val.value))
            else:
                assert isinstance(val, ast.FormattedValue), val
                # XXX Need to double check evaluation order here
                if val.format_spec is not None:
                    # Need to extract the String back out of the PyString
                    assert isinstance(val.format_spec, ast.JoinedStr), val.format_spec
                    expr = self.visit(val.format_spec)
                    if isinstance(expr, JavaPyConstant) and isinstance(expr.value, str):
                        format_spec = JavaStrLiteral(expr.value)
                    else:
                        format_spec = JavaField(expr, 'value')
                else:
                    format_spec = JavaStrLiteral("")
                expr = self.visit(val.value)
                if val.conversion == ord('s'):
                    expr = JavaCreateObject('PyString', [JavaMethodCall(expr, 'str', [])])
                elif val.conversion == ord('r'):
                    expr = JavaCreateObject('PyString', [JavaMethodCall(expr, 'repr', [])])
                elif val.conversion == ord('a'):
                    expr = JavaMethodCall(JavaField(JavaIdentifier('PyBuiltinFunction_ascii'), 'singleton'), 'call', [
                        JavaCreateArray('PyObject', [expr]), JavaIdentifier('null')
                    ])
                elif val.conversion != -1:
                    self.error(val.lineno, f'unsupported f string conversion type {val.conversion}')
                vals.append(JavaMethodCall(expr, 'format', [format_spec]))
        expr = chained_binary_op('+', vals)
        if isinstance(expr, JavaStrLiteral):
            return JavaPyConstant(expr.s)
        return JavaCreateObject('PyString', [expr])

    def emit_star_expanded(self, nodes: list, *, array_list_allowed: bool = False) -> JavaExpr:
        if any(isinstance(arg, ast.Starred) for arg in nodes):
            args = JavaCreateObject('java.util.ArrayList<PyObject>', [])
            for arg in nodes:
                if isinstance(arg, ast.Starred):
                    args = JavaMethodCall(JavaIdentifier('Runtime'), 'addStarToArrayList', [args, self.visit(arg.value)])
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
        kv_iter = itertools.chain.from_iterable(zip(node.keys, node.values))
        return JavaCreateObject('PyDict', [JavaIdentifier('null') if x is None else self.visit(x) for x in kv_iter])

    def visit_Call(self, node) -> JavaExpr:
        if self.allow_intrinsics and isinstance(node.func, ast.Name):
            if node.func.id == '__pythonj_next__':
                assert len(node.args) == 1 and not node.keywords, node.args
                return JavaMethodCall(self.visit(node.args[0]), 'next', [])

        func = self.visit(node.func)
        args = self.emit_star_expanded(node.args)
        if node.keywords:
            kv_list: list[JavaExpr] = []
            for kwarg in node.keywords:
                if kwarg.arg is None: # **kwargs
                    kv_list.append(JavaIdentifier('null'))
                    kv_list.append(self.visit(kwarg.value))
                else:
                    assert isinstance(kwarg.arg, str), kwarg.arg
                    kv_list.append(JavaPyConstant(kwarg.arg))
                    kv_list.append(self.visit(kwarg.value))
            kwargs = JavaMethodCall(JavaIdentifier('Runtime'), 'requireKwStrings', [JavaCreateObject('PyDict', kv_list)])
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
        lower = self.visit(node.lower) if node.lower else JavaPyConstant(None)
        upper = self.visit(node.upper) if node.upper else JavaPyConstant(None)
        step = self.visit(node.step) if node.step else JavaPyConstant(None)
        return JavaCreateObject('PySlice', [lower, upper, step])

    # XXX Change all statements to -> Iterator[JavaStatement] and yield statements?
    def visit_Pass(self, node) -> None:
        pass

    def visit_Global(self, node) -> None:
        pass # handled by SymbolTableVisitor

    def visit_Nonlocal(self, node) -> None:
        pass # handled by SymbolTableVisitor

    def emit_bind(self, target: ast.expr, value: JavaExpr) -> Iterator[JavaStatement]:
        if isinstance(target, ast.Name):
            yield JavaAssignStatement(self.visit(target), value)
        elif isinstance(target, ast.Attribute):
            temp_name = self.scope.make_temp()
            yield JavaVariableDecl('var', temp_name, value)
            yield JavaExprStatement(JavaMethodCall(self.visit(target.value), 'setAttr', [JavaStrLiteral(target.attr), JavaIdentifier(temp_name)]))
        elif isinstance(target, ast.Subscript):
            temp_name = self.scope.make_temp()
            yield JavaVariableDecl('var', temp_name, value)
            yield JavaExprStatement(JavaMethodCall(self.visit(target.value), 'setItem', [self.visit(target.slice), JavaIdentifier(temp_name)]))
        elif isinstance(target, (ast.Tuple, ast.List)):
            temp_name = self.scope.make_temp()
            yield JavaVariableDecl('var', temp_name, JavaMethodCall(value, 'iter', []))
            # XXX This is not atomic if an exception is thrown; a subset of LHS's will be left assigned
            for subtarget in target.elts:
                yield from self.emit_bind(subtarget, JavaMethodCall(JavaIdentifier('Runtime'), 'nextRequireNonNull', [JavaIdentifier(temp_name)]))
            yield JavaExprStatement(JavaMethodCall(JavaIdentifier('Runtime'), 'nextRequireNull', [JavaIdentifier(temp_name)]))
        else:
            self.error(target.lineno, f'binding to {type(target).__name__} is unsupported')

    def visit_Assign(self, node) -> None:
        if len(node.targets) != 1:
            self.error(node.lineno, 'chained assignment (a = b = c) is unsupported')
        target = node.targets[0]
        self.code.extend(self.emit_bind(target, self.visit(node.value)))

    def visit_AugAssign(self, node) -> None:
        op = f'{self.visit(node.op)}InPlace'

        if isinstance(node.target, ast.Name):
            code = JavaAssignStatement(self.visit(node.target), JavaMethodCall(self.visit(node.target), op, [self.visit(node.value)]))
        elif isinstance(node.target, ast.Attribute):
            temp_name = self.scope.make_temp()
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
            temp_name0 = self.scope.make_temp()
            temp_name1 = self.scope.make_temp()
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

    def visit_Assert(self, node) -> None:
        cond = unary_op('!', bool_value(self.visit(node.test)))
        msg = JavaStrLiteral(self.path + f':{node.lineno}: assertion failure')
        if node.msg:
            msg.s += ': '
            msg = JavaBinaryOp('+', msg, JavaMethodCall(self.visit(node.msg), 'repr', []))
        exception = JavaMethodCall(JavaIdentifier('PyAssertionError'), 'raise', [msg])
        self.code.extend(if_statement(cond, [JavaThrowStatement(exception)], []))

    def visit_Delete(self, node) -> None:
        for target in node.targets:
            if isinstance(target, ast.Attribute):
                code = JavaExprStatement(JavaMethodCall(self.visit(target.value), 'delAttr', [JavaStrLiteral(target.attr)]))
            elif isinstance(target, ast.Subscript):
                code = JavaExprStatement(JavaMethodCall(self.visit(target.value), 'delItem', [self.visit(target.slice)]))
            else:
                self.error(node.lineno, f"'del' of {type(target).__name__} is unsupported")
                continue
            self.code.append(code)

    def visit_Return(self, node) -> None:
        assert self.scope.kind == ScopeKind.FUNCTION, node
        value = self.visit(node.value) if node.value else JavaPyConstant(None)
        self.code.append(JavaReturnStatement(value))

    @contextmanager
    def new_block(self) -> Iterator[list[JavaStatement]]:
        saved = self.code
        self.code = []
        yield self.code
        self.code = saved

    @contextmanager
    def push_break_name(self, break_name: Optional[str]) -> Iterator[None]:
        saved = self.break_name
        self.break_name = break_name
        yield
        self.break_name = saved

    def visit_block(self, statements: list[ast.stmt]) -> list[JavaStatement]:
        with self.new_block() as body:
            for statement in statements:
                self.visit(statement)
        return body

    def visit_If(self, node) -> None:
        cond = bool_value(self.visit(node.test))
        body = self.visit_block(node.body)
        orelse = self.visit_block(node.orelse)
        self.code.extend(if_statement(cond, body, orelse))

    def visit_While(self, node) -> None:
        cond = bool_value(self.visit(node.test))

        block_name = self.scope.make_temp() if node.orelse else None
        with self.push_break_name(block_name):
            body = self.visit_block(node.body)

        loop = list(while_statement(cond, body))
        if node.orelse:
            orelse = self.visit_block(node.orelse)
            assert block_name is not None
            loop = [JavaLabeledBlock(block_name, [*loop, *orelse])]
        self.code.extend(loop)

    def visit_For(self, node) -> None:
        block_name = self.scope.make_temp() if node.orelse else None
        temp_name0 = self.scope.make_temp()
        temp_name1 = self.scope.make_temp()
        self.code.append(JavaVariableDecl('var', temp_name0, JavaMethodCall(self.visit(node.iter), 'iter', [])))

        with self.push_break_name(block_name):
            loop = JavaForStatement(
                'var', temp_name1, JavaMethodCall(JavaIdentifier(temp_name0), 'next', []),
                JavaBinaryOp('!=', JavaIdentifier(temp_name1), JavaIdentifier('null')),
                temp_name1, JavaMethodCall(JavaIdentifier(temp_name0), 'next', []),
                [
                    *self.emit_bind(node.target, JavaIdentifier(temp_name1)),
                    *self.visit_block(node.body),
                ]
            )
        if node.orelse:
            orelse = self.visit_block(node.orelse)
            assert block_name is not None
            loop = JavaLabeledBlock(block_name, [loop, *orelse])
        self.code.append(loop)

    def visit_With(self, node) -> None:
        # node.type_comment is ignored; we only plan to support "real" type annotations.
        if len(node.items) != 1:
            self.error(node.lineno, "multiple-item 'with' statements are unsupported")
        item = node.items[0]

        temp_name = self.scope.make_temp()
        self.code.append(JavaVariableDecl('var', temp_name, self.visit(item.context_expr)))
        if item.optional_vars is None:
            self.code.append(JavaExprStatement(JavaMethodCall(JavaIdentifier(temp_name), 'enter', [])))
        else:
            self.code.extend(self.emit_bind(item.optional_vars, JavaMethodCall(JavaIdentifier(temp_name), 'enter', [])))

        body = self.visit_block(node.body)

        self.code.append(JavaTryStatement(body, None, None, [], [
            JavaExprStatement(JavaMethodCall(JavaIdentifier(temp_name), 'exit', [])),
        ]))

    def visit_Try(self, node) -> None:
        if len(node.handlers) > 1:
            self.error(node.lineno, "at most 1 'except' clause is supported in 'try' statements")
        if node.orelse:
            self.error(node.lineno, "'else' clauses in 'try' statements are unsupported")
        for handler in node.handlers:
            if handler.type and not (isinstance(handler.type, ast.Name) and handler.type.id == 'BaseException'):
                self.error(node.lineno, "only catch-all 'except:' or 'except BaseException [as e]:' clauses in 'try' statements are supported")

        try_body = self.visit_block(node.body)

        exc_type = None
        exc_name = None
        catch_body = []
        if node.handlers:
            exc_type = 'PyRaise'
            exc_name = self.scope.make_temp()
            handler = node.handlers[0]
            with self.new_block() as catch_body:
                if handler.name is not None:
                    catch_body.append(JavaAssignStatement(self.ident_expr(handler.name), JavaField(JavaIdentifier(exc_name), 'exc')))
                for statement in handler.body:
                    self.visit(statement)

        finally_body = self.visit_block(node.finalbody)

        self.code.append(JavaTryStatement(try_body, exc_type, exc_name, catch_body, finally_body))

    def visit_Break(self, node) -> None:
        self.code.append(JavaBreakStatement(self.break_name))

    def visit_Continue(self, node) -> None:
        self.code.append(JavaContinueStatement())

    def visit_Expr(self, node) -> None:
        value = self.visit(node.value)
        if isinstance(value, (JavaMethodCall, JavaCreateObject)): # allowed by Java grammar as statements
            self.code.append(JavaExprStatement(value))
        else:
            # Avoid "not a statement" javac errors by assigning otherwise-unused values to a temp.
            # Cannot remove these statements because we rely on javac here to catch some portion of
            # Python usage errors.
            self.code.append(JavaAssignStatement(JavaIdentifier('expr_discard'), value))
            self.scope.used_expr_discard = True

    def check_args(self, lineno: int, args: ast.arguments) -> list[str]:
        if args.posonlyargs:
            self.error(lineno, 'position-only arguments are unsupported')
        if args.vararg:
            self.error(lineno, '*args are unsupported')
        if args.kwonlyargs:
            self.error(lineno, 'kw-only arguments are unsupported')
        if args.kw_defaults:
            self.error(lineno, 'kw-only argument defaults are unsupported')
        if args.kwarg:
            self.error(lineno, '**kwargs are unsupported')
        if args.defaults:
            self.error(lineno, 'argument defaults are unsupported')
        for arg in args.args:
            # arg.type_comment is ignored; we only plan to support "real" type annotations.
            if arg.annotation:
                self.error(lineno, 'argument type annotations are unsupported')
        return [arg.arg for arg in args.args]

    @contextmanager
    def new_function(self, lineno: int, symbol_table: SymbolTableVisitor, func_type: str) -> Iterator[None]:
        if symbol_table.nonlocals:
            self.error(lineno, "'nonlocal' is unsupported")

        free_vars = symbol_table.reads - symbol_table.writes
        free_vars -= symbol_table.globals # exclude any that match 'global' in current scope
        parent_scope = self.scope
        while parent_scope:
            free_vars -= parent_scope.explicit_globals # exclude any that match 'global' in parent scope
            if parent_scope.kind == ScopeKind.FUNCTION and (match_names := free_vars & parent_scope.locals):
                for name in match_names:
                    self.error(lineno, f'{func_type} capture of local {name!r} is unsupported')
            parent_scope = parent_scope.parent

        saved_scope = self.scope

        self.scope = Scope(self.scope, ScopeKind.FUNCTION)
        self.scope.locals = symbol_table.writes - symbol_table.globals
        self.scope.explicit_globals = symbol_table.globals.copy()

        yield

        self.scope = saved_scope

    def add_function(self, py_name: str, java_name: str, arg_names: list[str], body: list[JavaStatement],
                     invisible_args: bool = False) -> None:
        n_args = len(arg_names)
        func_code: list[JavaStatement] = [
            *if_statement(
                JavaBinaryOp('&&', JavaBinaryOp('!=', JavaIdentifier('kwargs'), JavaIdentifier('null')), bool_value(JavaIdentifier('kwargs'))),
                [JavaThrowStatement(JavaCreateObject('IllegalArgumentException', [JavaStrLiteral(f'{py_name}() does not accept kwargs')]))],
                [],
            ),
            *if_statement(
                JavaBinaryOp('!=', JavaField(JavaIdentifier('args'), 'length'), JavaIntLiteral(n_args, '')),
                [JavaThrowStatement(JavaMethodCall(JavaIdentifier('Runtime'), 'raiseUserExactArgs', [
                    JavaIdentifier('args'), JavaIntLiteral(n_args, ''), JavaStrLiteral(py_name),
                    *(JavaStrLiteral(arg) for arg in arg_names),
                ]))],
                [],
            ),
            *(JavaVariableDecl(
                'PyObject', arg if invisible_args else f'pylocal_{arg}', JavaArrayAccess(JavaIdentifier('args'), JavaIntLiteral(i, ''))
            ) for (i, arg) in enumerate(arg_names)),
            *((JavaVariableDecl('PyObject', 'expr_discard', None),) if self.scope.used_expr_discard else ()),
            # XXX It's tempting to assign Java null here, but this makes it far too easy for null to leak out into the runtime
            *(JavaVariableDecl('PyObject', f'pylocal_{name}', None) for name in sorted(self.scope.locals) if invisible_args or name not in arg_names),
            *body,
            JavaReturnStatement(JavaPyConstant(None)),
        ]

        assert java_name not in self.functions
        self.functions[java_name] = [
            f'private static final class {java_name} extends PyUserFunction {{',
            f'{java_name}() {{',
            f'super({java_string_literal(py_name)});',
            '}',
            '@Override public PyObject call(PyObject[] args, PyDict kwargs) {',
            *block_emit_java(block_simplify(func_code), self.emit_ctx),
            '}',
            '}',
        ]

    def visit_FunctionDef(self, node) -> None:
        # node.type_comment is ignored; we only plan to support "real" type annotations.
        if node.decorator_list:
            self.error(node.lineno, 'function decorators are unsupported')
        if node.returns:
            self.error(node.lineno, 'function return annotations are unsupported')
        if node.type_params:
            self.error(node.lineno, 'function type parameters are unsupported')

        arg_names = self.check_args(node.lineno, node.args)
        java_name = f'pyfunc_{node.name}'
        self.code.append(JavaAssignStatement(self.ident_expr(node.name), JavaCreateObject(java_name, [])))

        symbol_table = SymbolTableVisitor()
        symbol_table.writes.update(arg_names)
        for statement in node.body:
            symbol_table.visit(statement)

        with self.new_function(node.lineno, symbol_table, 'nested function'):
            body = self.visit_block(node.body)
            self.add_function(node.name, java_name, arg_names, body)

    def visit_Lambda(self, node) -> JavaExpr:
        arg_names = self.check_args(node.lineno, node.args)
        java_name = f'pylambda{self.n_lambdas}'
        self.n_lambdas += 1

        symbol_table = SymbolTableVisitor()
        symbol_table.writes.update(arg_names)
        symbol_table.visit(node.body)
        assert not symbol_table.globals, symbol_table.globals # should not be possible in a lambda
        assert not symbol_table.nonlocals, symbol_table.nonlocals # should not be possible in a lambda

        with self.new_function(node.lineno, symbol_table, 'lambda'):
            with self.new_block() as body:
                body.append(JavaReturnStatement(self.visit(node.body)))
            self.add_function('<lambda>', java_name, arg_names, body)

        return JavaCreateObject(java_name, [])

    def _lower_comp_generator(self, generator: ast.comprehension, iterable: JavaExpr, statements: list[JavaStatement]) -> list[JavaStatement]:
        for _if in reversed(generator.ifs):
            statements = list(if_statement(bool_value(self.visit(_if)), statements, []))

        temp_iter = self.scope.make_temp()
        temp_element = self.scope.make_temp()
        return [
            JavaVariableDecl('var', temp_iter, JavaMethodCall(iterable, 'iter', [])),
            JavaForStatement(
                'var', temp_element, JavaMethodCall(JavaIdentifier(temp_iter), 'next', []),
                JavaBinaryOp('!=', JavaIdentifier(temp_element), JavaIdentifier('null')),
                temp_element, JavaMethodCall(JavaIdentifier(temp_iter), 'next', []),
                [
                    *self.emit_bind(generator.target, JavaIdentifier(temp_element)),
                    *statements,
                ]
            )
        ]

    def _lower_comp(self, py_name: str, type_name: str, method_name: str, lineno: int,
                    generators: list[ast.comprehension], elts: list[ast.expr]) -> JavaExpr:
        arg_name = 'iterable'
        java_name = f'pylambda{self.n_lambdas}'
        self.n_lambdas += 1

        symbol_table = SymbolTableVisitor()
        for (i, generator) in enumerate(generators):
            if generator.is_async:
                self.error(lineno, 'asynchronous generators are unsupported')
            if i != 0: # generators[0].iter is evaluated outside the lambda
                symbol_table.visit(generator.iter)
            symbol_table.visit(generator.target)
            for _if in generator.ifs:
                symbol_table.visit(_if)
        for elt in elts:
            symbol_table.visit(elt)
        assert not symbol_table.globals, symbol_table.globals # should not be possible in a comprehension
        assert not symbol_table.nonlocals, symbol_table.nonlocals # should not be possible in a comprehension

        with self.new_function(lineno, symbol_table, 'comprehension'):
            with self.new_block() as body:
                temp_result = self.scope.make_temp()
                statements: list[JavaStatement] = [
                    JavaExprStatement(JavaMethodCall(JavaIdentifier(temp_result), method_name, [self.visit(elt) for elt in elts]))
                ]
                for (i, generator) in enumerate(reversed(generators)):
                    iterable = JavaIdentifier(arg_name) if i == len(generators)-1 else self.visit(generator.iter)
                    statements = self._lower_comp_generator(generator, iterable, statements)
                body += [
                    JavaVariableDecl('var', temp_result, JavaCreateObject(type_name, [])),
                    *statements,
                    JavaReturnStatement(JavaIdentifier(temp_result)),
                ]
            self.add_function(py_name, java_name, [arg_name], body, invisible_args=True)

        return JavaMethodCall(JavaCreateObject(java_name, []), 'call', [JavaCreateArray('PyObject', [self.visit(generators[0].iter)]), JavaIdentifier('null')])

    def visit_ListComp(self, node) -> JavaExpr:
        return self._lower_comp('<listcomp>', 'PyList', 'pymethod_append', node.lineno, node.generators, [node.elt])

    def visit_SetComp(self, node) -> JavaExpr:
        return self._lower_comp('<setcomp>', 'PySet', 'pymethod_add', node.lineno, node.generators, [node.elt])

    def visit_DictComp(self, node) -> JavaExpr:
        return self._lower_comp('<dictcomp>', 'PyDict', 'setItem', node.lineno, node.generators, [node.key, node.value])

    def visit_Module(self, node) -> None:
        symbol_table = SymbolTableVisitor()
        for statement in node.body:
            symbol_table.visit(statement)
        self.scope.locals.update(symbol_table.writes)

        for statement in node.body:
            self.visit(statement)

    def write_java(self, f: TextIO, py_name: str) -> None:
        writer = IndentedWriter(f, 0)
        writer.write(f'public final class {py_name} {{')
        for code in self.functions.values():
            for line in code:
                writer.write(line)
            writer.write('')

        # XXX Initializing all globals to None is weird, but we don't have a better option yet
        for name in sorted(self.scope.locals):
            writer.write(f'private static PyObject pyglobal_{name} = {JavaPyConstant(None).emit_java(self.emit_ctx)};')
        writer.write('')

        writer.write('public static void main(String[] args) {')
        for line in block_emit_java(block_simplify(self.global_code), self.emit_ctx):
            writer.write(line)
        writer.write('}')

        writer.write('')
        for line in self.emit_ctx.emit_java():
            writer.write(line)
        writer.write('}')
        assert writer.indent == 0, writer.indent

def gen_spec(path: str) -> None:
    spec = {}
    for name in ['bool', 'bytearray', 'bytes', 'dict', 'enumerate', 'int', 'list', 'object', 'range',
                 'reversed', 'set', 'slice', 'str', 'tuple', 'type', 'zip', '_io.BufferedReader',
                 '_io.TextIOWrapper', 'ArithmeticError', 'AssertionError', 'AttributeError', 'BaseException',
                 'Exception']:
        if name.startswith('_io.'):
            obj = getattr(_io, name.split('.', 1)[1])
        else:
            obj = getattr(builtins, name)
        attrs = {}
        for (k, v) in obj.__dict__.items():
            if k.startswith('__') and k not in {'__doc__'}:
                continue # only extract a subset of dunders
            if isinstance(v, str):
                attrs[k] = {'kind': 'string', 'value': v}
            elif isinstance(v, MemberDescriptorType):
                attrs[k] = {'kind': 'member'}
            elif isinstance(v, GetSetDescriptorType):
                attrs[k] = {'kind': 'getset'}
            elif isinstance(v, MethodDescriptorType):
                attrs[k] = {'kind': 'method'}
            elif isinstance(v, ClassMethodDescriptorType):
                attrs[k] = {'kind': 'classmethod'}
            elif isinstance(v, staticmethod):
                attrs[k] = {'kind': 'staticmethod'}
            else:
                assert False, (name, k, v, type(v))
        spec[name] = attrs

    with open(path, 'w') as f:
        json.dump(spec, f, indent=2)
        f.write('\n')

UNIMPLEMENTED_METHODS = {
    'bytearray': {
        'append', 'capitalize', 'center', 'clear', 'copy', 'count', 'decode', 'endswith', 'expandtabs',
        'extend', 'find', 'hex', 'index', 'insert', 'isalnum', 'isalpha', 'isascii', 'isdigit', 'islower',
        'isspace', 'istitle', 'isupper', 'join', 'ljust', 'lower', 'lstrip', 'partition', 'pop', 'remove',
        'replace', 'removeprefix', 'removesuffix', 'resize', 'reverse', 'rfind', 'rindex', 'rjust',
        'rpartition', 'rsplit', 'rstrip', 'split', 'splitlines', 'startswith', 'strip', 'swapcase', 'title',
        'translate', 'upper', 'zfill',
    },
    'bytes': {
        'capitalize', 'center', 'count', 'decode', 'endswith', 'expandtabs', 'find', 'hex', 'index',
        'isalnum', 'isalpha', 'isascii', 'isdigit', 'islower', 'isspace', 'istitle', 'isupper', 'join',
        'ljust', 'lower', 'lstrip', 'partition', 'removeprefix', 'removesuffix', 'replace', 'rfind',
        'rindex', 'rjust', 'rpartition', 'rsplit', 'rstrip', 'split', 'splitlines', 'startswith', 'strip',
        'swapcase', 'title', 'translate', 'upper', 'zfill',
    },
    'int': {
        'as_integer_ratio', 'bit_count', 'bit_length', 'conjugate', 'is_integer', 'to_bytes',
    },
    'set': {
        'copy', 'difference', 'difference_update', 'intersection', 'intersection_update', 'isdisjoint', 'issubset',
        'issuperset', 'pop', 'remove', 'symmetric_difference', 'symmetric_difference_update', 'union',
    },
    'str': {
        'capitalize', 'casefold', 'center', 'count', 'encode', 'endswith', 'expandtabs', 'format', 'format_map',
        'index', 'isalnum', 'isalpha', 'isascii', 'isdecimal', 'isdigit', 'isidentifier', 'islower', 'isnumeric',
        'isprintable', 'isspace', 'istitle', 'isupper', 'ljust', 'lstrip', 'partition', 'removeprefix',
        'removesuffix', 'replace', 'rfind', 'rindex', 'rjust', 'rpartition', 'rsplit', 'rstrip', 'splitlines',
        'strip', 'swapcase', 'title', 'translate', 'zfill',
    },
    'type': {'mro'},
    '_io.BufferedReader': {
        '_dealloc_warn', 'detach', 'fileno', 'flush', 'isatty', 'peek', 'read1', 'readable', 'readinto',
        'readinto1', 'readline', 'seek', 'seekable', 'tell', 'truncate',
    },
    '_io.TextIOWrapper': {
        'detach', 'fileno', 'flush', 'isatty', 'read', 'readable', 'reconfigure', 'seek', 'seekable', 'tell',
        'truncate', 'writable', 'write',
    },
    'BaseException': {'add_note', 'with_traceback'},
}
def gen_code(path) -> None:
    with open(path) as f:
        spec = json.load(f)

    for (name, attrs) in spec.items():
        gen_entire_class = name in {'ArithmeticError'} # XXX roll out to the rest
        if name == '_io.BufferedReader':
            java_name = 'PyBufferedReader'
            java_path = 'runtime/PyFile.java'
        elif name == '_io.TextIOWrapper':
            java_name = 'PyTextIOWrapper'
            java_path = 'runtime/PyFile.java'
        elif name in EXCEPTION_TYPES:
            java_name = f'Py{name}'
            java_path = 'runtime/PyExceptions.java'
        else:
            java_name = BUILTIN_TYPES[name]
            if name == 'type':
                java_path = f'runtime/Runtime.java'
            else:
                java_path = f'runtime/{java_name}.java'
        with open(java_path) as f:
            source_lines = f.readlines()

        begin_tag = f'// BEGIN GENERATED CODE: {java_name}Type\n'
        end_tag = f'// END GENERATED CODE: {java_name}Type\n'
        begin_index = source_lines.index(begin_tag) + 1
        end_index = source_lines.index(end_tag)

        gen_lines = []
        if gen_entire_class:
            gen_lines.append(f'final class {java_name}Type extends PyBuiltinType {{\n')
        gen_lines.append(f'    public static final {java_name}Type singleton = new {java_name}Type();\n')
        for (k, v) in attrs.items():
            if v['kind'] == 'string':
                gen_lines.append(f"    private static final PyString pyattr_{k} = new PyString({java_string_literal(v['value'])});\n")
            elif v['kind'] == 'member':
                gen_lines.append(f"    private static final PyMemberDescriptor pyattr_{k} = new PyMemberDescriptor(singleton, {java_string_literal(k)}, {java_name}::pymember_{k});\n")
            elif v['kind'] == 'getset':
                gen_lines.append(f"    private static final PyGetSetDescriptor pyattr_{k} = new PyGetSetDescriptor(singleton, {java_string_literal(k)}, {java_name}::pygetset_{k});\n")
            elif v['kind'] == 'method':
                if name in UNIMPLEMENTED_METHODS and k in UNIMPLEMENTED_METHODS[name]:
                    constructor = f'obj -> new {java_name}.{java_name}MethodUnimplemented(obj, {java_string_literal(k)})'
                else:
                    constructor = f'{java_name}.{java_name}Method_{k}::new'
                gen_lines.append(f"    private static final PyMethodDescriptor pyattr_{k} = new PyMethodDescriptor(singleton, {java_string_literal(k)}, {constructor});\n")
            elif v['kind'] == 'classmethod':
                constructor = f'{java_name}Type.{java_name}ClassMethod_{k}::new'
                gen_lines.append(f"    private static final PyClassMethodDescriptor pyattr_{k} = new PyClassMethodDescriptor(singleton, {java_string_literal(k)}, {constructor});\n")
            elif v['kind'] == 'staticmethod':
                constructor = f'new {java_name}Type.{java_name}StaticMethod_{k}(singleton)'
                gen_lines.append(f'    private static final PyStaticMethod pyattr_{k} = new PyStaticMethod(singleton, {java_string_literal(k)}, {constructor});\n')
            else:
                assert False, (name, k, v)
        gen_lines += [
            f'    private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>({len(attrs)});\n',
            '    static {\n',
            *(f'        attrs.put(new PyString("{k}"), pyattr_{k});\n' for k in attrs),
            '    }\n',
            '\n',
            f'    private {java_name}Type() {{ super({java_string_literal(name)}, {java_name}.class, {java_name}::newObj); }}\n'
            if gen_entire_class else
            f'    private {java_name}Type() {{ super({java_string_literal(name)}, {java_name}.class); }}\n',
            '    @Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }\n',
            '    @Override public PyObject lookupAttr(String name) {\n',
            '        switch (name) {\n',
            *(
                f'            case {java_string_literal(k)}: return pyattr_{k};\n'
                for (k, v) in attrs.items()
            ),
            '            default: return null;\n',
            '        }\n',
            '    }\n',
        ]
        if gen_entire_class:
            gen_lines.append('}\n')
        source_lines[begin_index:end_index] = gen_lines

        if name in UNIMPLEMENTED_METHODS:
            begin_tag = f'// BEGIN GENERATED CODE: {java_name}\n'
            end_tag = f'// END GENERATED CODE: {java_name}\n'
            begin_index = source_lines.index(begin_tag) + 1
            end_index = source_lines.index(end_tag)
            gen_lines = [
                f'    protected static final class {java_name}MethodUnimplemented extends PyBuiltinMethod<{java_name}> {{\n',
                '        private final String name;\n',
                f'        {java_name}MethodUnimplemented(PyObject _self, String _name) {{ super(({java_name})_self); name = _name; }}\n',
                '        @Override public String methodName() { return name; }\n',
                '        @Override public PyObject call(PyObject[] args, PyDict kwargs) {\n',
                f'            throw new UnsupportedOperationException("{name}." + name + "() unimplemented");\n',
                '        }\n',
                '    }\n',
            ]
            source_lines[begin_index:end_index] = gen_lines

        with open(java_path, 'w') as f:
            for line in source_lines:
                f.write(line)

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('py_names', nargs='*', help='names of tests to run')
    args = parser.parse_args()

    py_names = args.py_names
    if not py_names:
        py_names = sorted(x[:-3] for x in os.listdir('tests') if x.endswith('.py'))

    start = time.perf_counter()
    if os.path.exists('runtime/_out'):
        shutil.rmtree('runtime/_out')
    os.mkdir('runtime/_out')
    gen_spec('runtime/_out/spec.json')
    gen_code('runtime/_out/spec.json')
    codegen_time = time.perf_counter() - start
    print(f'{codegen_time=:.3f}')

    start = time.perf_counter()
    if os.path.exists('tests/_out'):
        shutil.rmtree('tests/_out')
    subprocess.check_call(['javac', '-d', '_out', *RUNTIME_JAVA_FILES], cwd='runtime')
    os.makedirs('tests/_out', exist_ok=True)
    subprocess.check_call(['jar', '--create', '--file', 'tests/_out/pythonj.jar', '--date=1980-01-01T00:00:02Z', '-C', 'runtime/_out', '.'])
    initial_javac_time = time.perf_counter() - start
    print(f'initial_javac_time={initial_javac_time:.3f}')

    start = time.perf_counter()
    for py_name in py_names:
        py_path = f'tests/{py_name}.py'
        with open(py_path, encoding='utf-8') as f:
            node = ast.parse(f.read())
        visitor = PythonjVisitor(py_path)
        visitor.visit(node)
        if visitor.n_errors:
            print(f'Translation failed: {visitor.n_errors} errors')
            exit(1)
        with open(f'tests/_out/{py_name}.java', 'w') as f:
            visitor.write_java(f, py_name)
    translate_time = time.perf_counter() - start
    print(f'translate_time={translate_time:5.3f}')

    start = time.perf_counter()
    subprocess.check_call(['javac', '-cp', 'pythonj.jar', *(f'{py_name}.java' for py_name in py_names)], cwd='tests/_out')
    javac_time = time.perf_counter() - start
    print(f'javac_time={javac_time:5.3f}')

    for py_name in py_names:
        start = time.perf_counter()
        sep = ';' if os.name == 'nt' else ':'
        j_output = subprocess.check_output(['java', '-cp', f'_out/pythonj.jar{sep}_out', py_name], cwd='tests')
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

if __name__ == '__main__':
    main()
