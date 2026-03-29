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
import inspect
import itertools
import json
import os
import shutil
import subprocess
import sys
import time
import types
from typing import Iterator, Optional, TextIO, cast
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
    'float': 'PyFloat',
    'int': 'PyInt',
    'list': 'PyList',
    'object': 'PyObject',
    'range': 'PyRange',
    'reversed': 'PyReversed',
    'set': 'PySet',
    'slice': 'PySlice',
    'staticmethod': 'PyStaticMethod',
    'str': 'PyString',
    'tuple': 'PyTuple',
    'type': 'PyType',
    'zip': 'PyZip',
}
EXCEPTION_TYPES = {
    'ArithmeticError', 'AssertionError', 'AttributeError', 'BaseException', 'Exception', 'IndexError',
    'KeyError', 'LookupError', 'OverflowError', 'StopIteration', 'TypeError', 'ValueError', 'ZeroDivisionError',
}

# Keep this explicit and narrow; widen it only for builtin types we have actually
# exercised and are comfortable lowering directly to JVM instanceof checks.
ISINSTANCE_SINGLE_FASTPATH_BUILTIN_TYPES = {'bytearray', 'bytes', 'str', 'tuple'}

RUNTIME_JAVA_FILES = (
    'PyBool.java',
    'PyBuiltinFunctions.java',
    'PyByteArray.java',
    'PyBytes.java',
    'PyDict.java',
    'PyEnumerate.java',
    'PyExceptions.java',
    'PyFile.java',
    'PyFloat.java',
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

class ConstantPool:
    __slots__ = ('all_ints', 'all_strings', 'all_floats', 'all_tuples', 'all_bytes', 'holder_name')
    all_ints: set[int]
    all_strings: dict[str, int]
    all_floats: dict[float, int]
    all_tuples: dict[tuple[object, ...], int]
    all_bytes: dict[bytes, int]
    holder_name: Optional[str]

    def __init__(self, holder_name: Optional[str] = None):
        self.all_ints = set()
        self.all_strings = {}
        self.all_floats = {}
        self.all_tuples = {}
        self.all_bytes = {}
        self.holder_name = holder_name

    def qualify(self, name: str) -> str:
        return name if self.holder_name is None else f'{self.holder_name}.{name}'

    def emit_constant(self, value: object) -> str:
        if value is None:
            return 'PyNone.singleton'
        elif value is False or value is True:
            return f'PyBool.{str(value).lower()}_singleton'
        elif isinstance(value, int):
            if -1 <= value <= 1:
                if value < 0:
                    return f'PyInt.singleton_neg{-value}'
                else:
                    return f'PyInt.singleton_{value}'
            self.all_ints.add(value)
            return self.qualify(int_name(value))
        elif isinstance(value, str):
            if not value:
                return 'PyString.empty_singleton'
            if value not in self.all_strings:
                self.all_strings[value] = len(self.all_strings)
            return self.qualify(f'str_singleton_{self.all_strings[value]}')
        elif isinstance(value, float):
            if value not in self.all_floats:
                self.all_floats[value] = len(self.all_floats)
            return self.qualify(f'float_singleton_{self.all_floats[value]}')
        elif isinstance(value, tuple):
            if not value:
                return 'PyTuple.empty_singleton'
            if value not in self.all_tuples:
                for x in value:
                    self.emit_constant(x)
                self.all_tuples[value] = len(self.all_tuples)
            return self.qualify(f'tuple_singleton_{self.all_tuples[value]}')
        else:
            assert isinstance(value, bytes), value
            if not value:
                return 'PyBytes.empty_singleton'
            if value not in self.all_bytes:
                self.all_bytes[value] = len(self.all_bytes)
            return self.qualify(f'bytes_singleton_{self.all_bytes[value]}')

    def emit_pool(self) -> Iterator[str]:
        if self.holder_name is not None:
            yield f'final class {self.holder_name} {{'
        field_prefix = 'private static final' if self.holder_name is None else 'static final'
        for i in sorted(self.all_ints):
            value = JavaCreateObject('PyInt', [JavaIntLiteral(i, 'L')])
            yield f'{field_prefix} PyInt {int_name(i)} = {value.emit_java(self)};'
        for (k, v) in sorted(self.all_strings.items()):
            value = JavaCreateObject('PyString', [JavaStrLiteral(k)])
            yield f'{field_prefix} PyString str_singleton_{v} = {value.emit_java(self)};'
        for (k, v) in sorted(self.all_floats.items()):
            yield f'{field_prefix} PyFloat float_singleton_{v} = new PyFloat({k!r});'
        for (k, v) in sorted(self.all_tuples.items(), key=lambda x: x[1]):
            value = JavaCreateObject('PyTuple', [JavaCreateArray('PyObject', [JavaPyConstant(x) for x in k])])
            yield f'{field_prefix} PyTuple tuple_singleton_{v} = {value.emit_java(self)};'
        for (k, v) in sorted(self.all_bytes.items()):
            value = JavaCreateObject('PyBytes', [JavaCreateArray('byte', [JavaIntLiteral(((x + 0x80) & 0xFF) - 0x80, '') for x in k])])
            yield f'{field_prefix} PyBytes bytes_singleton_{v} = {value.emit_java(self)};'
        if self.holder_name is not None:
            yield '}'

class JavaExpr(ABC):
    @abstractmethod
    def emit_java(self, pool: ConstantPool) -> str:
        raise NotImplementedError()

@dataclass(slots=True)
class JavaIntLiteral(JavaExpr):
    value: int
    suffix: str
    def emit_java(self, pool: ConstantPool) -> str:
        return f'{self.value}{self.suffix}'

@dataclass(slots=True)
class JavaStrLiteral(JavaExpr):
    s: str
    def emit_java(self, pool: ConstantPool) -> str:
        return java_string_literal(self.s)

@dataclass(slots=True)
class JavaIdentifier(JavaExpr):
    name: str
    def emit_java(self, pool: ConstantPool) -> str:
        return self.name

@dataclass(slots=True)
class JavaField(JavaExpr):
    obj: JavaExpr
    field: str
    def emit_java(self, pool: ConstantPool) -> str:
        return f'{self.obj.emit_java(pool)}.{self.field}'

@dataclass(slots=True)
class JavaArrayAccess(JavaExpr):
    obj: JavaExpr
    index: JavaExpr
    def emit_java(self, pool: ConstantPool) -> str:
        return f'{self.obj.emit_java(pool)}[{self.index.emit_java(pool)}]'

@dataclass(slots=True)
class JavaUnaryOp(JavaExpr):
    op: str
    operand: JavaExpr
    def emit_java(self, pool: ConstantPool) -> str:
        return f'({self.op}{self.operand.emit_java(pool)})'

@dataclass(slots=True)
class JavaBinaryOp(JavaExpr):
    op: str
    lhs: JavaExpr
    rhs: JavaExpr
    def emit_java(self, pool: ConstantPool) -> str:
        return f'({self.lhs.emit_java(pool)} {self.op} {self.rhs.emit_java(pool)})'

@dataclass(slots=True)
class JavaCondOp(JavaExpr):
    cond: JavaExpr
    true: JavaExpr
    false: JavaExpr
    def emit_java(self, pool: ConstantPool) -> str:
        return f'({self.cond.emit_java(pool)} ? {self.true.emit_java(pool)} : {self.false.emit_java(pool)})'

@dataclass(slots=True)
class JavaCreateObject(JavaExpr):
    type: str
    args: list[JavaExpr]
    def emit_java(self, pool: ConstantPool) -> str:
        return f"new {self.type}({', '.join(arg.emit_java(pool) for arg in self.args)})"

@dataclass(slots=True)
class JavaCreateArray(JavaExpr):
    type: str
    elts: list[JavaExpr]
    def emit_java(self, pool: ConstantPool) -> str:
        return f"new {self.type}[] {{{', '.join(x.emit_java(pool) for x in self.elts)}}}"

@dataclass(slots=True)
class JavaMethodCall(JavaExpr):
    obj: JavaExpr
    method: str
    args: list[JavaExpr]
    def emit_java(self, pool: ConstantPool) -> str:
        return f"{self.obj.emit_java(pool)}.{self.method}({', '.join(arg.emit_java(pool) for arg in self.args)})"

@dataclass(slots=True)
class JavaAssignExpr(JavaExpr):
    lhs: JavaExpr
    rhs: JavaExpr
    def emit_java(self, pool: ConstantPool) -> str:
        return f'({self.lhs.emit_java(pool)} = {self.rhs.emit_java(pool)})'

@dataclass(slots=True)
class JavaPyConstant(JavaExpr):
    value: object
    def emit_java(self, pool: ConstantPool) -> str:
        return pool.emit_constant(self.value)

class JavaStatement(ABC):
    def ends_control_flow(self) -> bool:
        return False

    @abstractmethod
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        raise NotImplementedError()

@dataclass(slots=True)
class JavaVariableDecl(JavaStatement):
    type: str
    name: str
    value: Optional[JavaExpr]
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        if self.value:
            yield f'{self.type} {self.name} = {self.value.emit_java(pool)};'
        else:
            yield f'{self.type} {self.name};'

@dataclass(slots=True)
class JavaAssignStatement(JavaStatement):
    lhs: JavaExpr
    rhs: JavaExpr
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'{self.lhs.emit_java(pool)} = {self.rhs.emit_java(pool)};'

@dataclass(slots=True)
class JavaExprStatement(JavaStatement):
    call: JavaCreateObject | JavaMethodCall # only limited types of expressions allowed by Java grammar
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'{self.call.emit_java(pool)};'

@dataclass(slots=True)
class JavaBreakStatement(JavaStatement):
    name: Optional[str]
    def ends_control_flow(self) -> bool:
        return True
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'break {self.name};' if self.name else 'break;'

@dataclass(slots=True)
class JavaContinueStatement(JavaStatement):
    def ends_control_flow(self) -> bool:
        return True
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield 'continue;'

@dataclass(slots=True)
class JavaReturnStatement(JavaStatement):
    expr: JavaExpr
    def ends_control_flow(self) -> bool:
        return True
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'return {self.expr.emit_java(pool)};'

@dataclass(slots=True)
class JavaThrowStatement(JavaStatement):
    expr: JavaExpr
    def ends_control_flow(self) -> bool:
        return True
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'throw {self.expr.emit_java(pool)};'

def block_simplify(block: list[JavaStatement]) -> list[JavaStatement]:
    ret = []
    for s in block:
        ret.append(s)
        if s.ends_control_flow():
            break
    return ret

def block_emit_java(block: list[JavaStatement], pool: ConstantPool) -> Iterator[str]:
    for s in block:
        yield from s.emit_java(pool)

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

    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'if ({self.cond.emit_java(pool)}) {{'
        yield from block_emit_java(self.body, pool)
        if self.orelse:
            yield '} else {'
            yield from block_emit_java(self.orelse, pool)
        yield '}'

@dataclass(slots=True)
class JavaWhileStatement(JavaStatement):
    cond: JavaExpr
    body: list[JavaStatement]

    def __post_init__(self):
        self.body = block_simplify(self.body)

    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'while ({self.cond.emit_java(pool)}) {{'
        yield from block_emit_java(self.body, pool)
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

    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'for ({self.init_type} {self.init_name} = {self.init_value.emit_java(pool)}; {self.cond.emit_java(pool)}; {self.incr_name} = {self.incr_value.emit_java(pool)}) {{'
        yield from block_emit_java(self.body, pool)
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

    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield 'try {'
        yield from block_emit_java(self.try_body, pool)
        if self.exc_type is not None:
            yield f'}} catch ({self.exc_type} {self.exc_name}) {{'
            yield from block_emit_java(self.catch_body, pool)
        else: # if no exception type, should not have an exception name or catch block either
            assert self.exc_name is None, self.exc_name
            assert not self.catch_body, self.catch_body
        if self.finally_body:
            yield '} finally {'
            yield from block_emit_java(self.finally_body, pool)
        yield '}'

@dataclass(slots=True)
class JavaLabeledBlock(JavaStatement):
    name: str
    body: list[JavaStatement]

    def __post_init__(self):
        self.body = block_simplify(self.body)

    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'{self.name}: {{'
        yield from block_emit_java(self.body, pool)
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

class ScopeKind(Enum):
    MODULE = 1
    FUNCTION = 2

@dataclass
class ScopeInfo:
    kind: ScopeKind
    locals: set[str]
    explicit_globals: set[str]
    nonlocals: set[str]
    cell_vars: set[str]
    needs_from_outer: set[str]

class ScopeAnalyzer(ast.NodeVisitor):
    __slots__ = ('scope_infos',)
    scope_infos: dict[ast.AST, ScopeInfo]

    def __init__(self):
        self.scope_infos = {}

    def _walk_local(self, node: ast.AST, reads: set[str], writes: set[str], explicit_globals: set[str],
                    nonlocals: set[str], children: list[ast.AST]) -> None:
        if isinstance(node, ast.Name):
            if isinstance(node.ctx, (ast.Store, ast.Del)):
                writes.add(node.id)
            else:
                reads.add(node.id)
            return
        if isinstance(node, ast.ExceptHandler):
            if node.type is not None:
                self._walk_local(node.type, reads, writes, explicit_globals, nonlocals, children)
            if node.name is not None:
                writes.add(node.name)
            for statement in node.body:
                self._walk_local(statement, reads, writes, explicit_globals, nonlocals, children)
            return
        if isinstance(node, ast.Global):
            explicit_globals.update(node.names)
            return
        if isinstance(node, ast.Nonlocal):
            nonlocals.update(node.names)
            return
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            writes.add(node.name)
            children.append(node)
            return
        if isinstance(node, ast.Lambda):
            children.append(node)
            return
        if isinstance(node, ast.ClassDef):
            writes.add(node.name)
            return
        if isinstance(node, (ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp)):
            self._walk_local(node.generators[0].iter, reads, writes, explicit_globals, nonlocals, children)
            children.append(node)
            return
        for child in ast.iter_child_nodes(node):
            self._walk_local(child, reads, writes, explicit_globals, nonlocals, children)

    def _param_names(self, args: ast.arguments) -> set[str]:
        out = {arg.arg for arg in args.posonlyargs}
        out.update(arg.arg for arg in args.args)
        out.update(arg.arg for arg in args.kwonlyargs)
        if args.vararg is not None:
            out.add(args.vararg.arg)
        if args.kwarg is not None:
            out.add(args.kwarg.arg)
        return out

    def _analyze_scope_node(self, node: ast.AST) -> ScopeInfo:
        if node in self.scope_infos:
            return self.scope_infos[node]

        reads: set[str] = set()
        writes: set[str] = set()
        explicit_globals: set[str] = set()
        nonlocals: set[str] = set()
        children: list[ast.AST] = []

        if isinstance(node, ast.Module):
            for statement in node.body:
                self._walk_local(statement, reads, writes, explicit_globals, nonlocals, children)
        elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            writes.update(self._param_names(node.args))
            for statement in node.body:
                self._walk_local(statement, reads, writes, explicit_globals, nonlocals, children)
        elif isinstance(node, ast.Lambda):
            writes.update(self._param_names(node.args))
            self._walk_local(node.body, reads, writes, explicit_globals, nonlocals, children)
        else:
            assert isinstance(node, (ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp)), node
            generators = node.generators
            elts: list[ast.expr] = [node.elt] if not isinstance(node, ast.DictComp) else [node.key, node.value]
            for (i, generator) in enumerate(generators):
                if i != 0:
                    self._walk_local(generator.iter, reads, writes, explicit_globals, nonlocals, children)
                self._walk_local(generator.target, reads, writes, explicit_globals, nonlocals, children)
                for _if in generator.ifs:
                    self._walk_local(_if, reads, writes, explicit_globals, nonlocals, children)
            for elt in elts:
                self._walk_local(elt, reads, writes, explicit_globals, nonlocals, children)

        locals = writes - explicit_globals - nonlocals
        needs_from_outer = (reads | nonlocals) - locals - explicit_globals
        cell_vars = set()
        for child in children:
            child_info = self._analyze_scope_node(child)
            for name in child_info.needs_from_outer:
                if name in locals:
                    cell_vars.add(name)
                elif name not in explicit_globals:
                    needs_from_outer.add(name)
        kind = ScopeKind.MODULE if isinstance(node, ast.Module) else ScopeKind.FUNCTION
        scope_info = ScopeInfo(kind, locals, explicit_globals, nonlocals, cell_vars, needs_from_outer)
        self.scope_infos[node] = scope_info
        return scope_info

    def visit_Module(self, node) -> None:
        self._analyze_scope_node(node)

class Scope:
    __slots__ = ('parent', 'info', 'qualname', 'free_vars', 'locals_are_fields', 'n_temps', 'used_expr_discard')
    parent: Optional['Scope']
    info: ScopeInfo
    qualname: Optional[str]
    free_vars: set[str]
    locals_are_fields: bool
    n_temps: int
    used_expr_discard: bool

    def __init__(self, parent: Optional['Scope'], info: ScopeInfo, qualname: Optional[str] = None):
        self.parent = parent
        self.info = info
        self.qualname = qualname
        self.free_vars = set()
        self.locals_are_fields = False
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
    __slots__ = ('path', 'n_errors', 'n_functions', 'n_lambdas', 'scope', 'global_code', 'code',
                 'scope_infos', 'pool', 'break_name', 'functions', 'allow_intrinsics',
                 'python_helper_names', 'python_helper_class')
    path: str
    n_errors: int
    n_functions: int
    n_lambdas: int
    scope: Scope
    global_code: list[JavaStatement]
    code: list[JavaStatement]
    scope_infos: dict[ast.AST, ScopeInfo]
    pool: ConstantPool
    break_name: Optional[str]
    functions: dict[str, list[str]]
    allow_intrinsics: bool # enables special internal-only codegen features for builtins
    python_helper_names: set[str]
    python_helper_class: Optional[str]

    def __init__(self, path: str, scope_infos: dict[ast.AST, ScopeInfo], module_info: ScopeInfo):
        self.path = path
        self.n_errors = 0
        self.n_functions = 0
        self.n_lambdas = 0
        self.scope = Scope(None, module_info)
        self.global_code = [JavaVariableDecl('PyObject', 'expr_discard', None)] # XXX remove this variable if not needed
        self.code = self.global_code
        self.scope_infos = scope_infos
        self.pool = ConstantPool()
        self.break_name = None
        self.functions = {}
        self.allow_intrinsics = False
        self.python_helper_names = set()
        self.python_helper_class = None

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
        elif self.scope.info.kind == ScopeKind.FUNCTION and (name in self.scope.info.cell_vars or name in self.scope.free_vars):
            return JavaField(JavaIdentifier(f'pycell_{name}'), 'obj')
        elif self.scope.info.kind == ScopeKind.FUNCTION and name in self.scope.info.locals:
            if self.scope.locals_are_fields:
                return JavaField(JavaIdentifier('this'), f'pylocal_{name}')
            return JavaIdentifier(f'pylocal_{name}')
        elif name in BUILTIN_TYPES:
            return JavaField(JavaIdentifier(f'{BUILTIN_TYPES[name]}Type'), 'singleton')
        elif name in EXCEPTION_TYPES:
            return JavaField(JavaIdentifier(f'Py{name}Type'), 'singleton')
        elif name in BUILTIN_FUNCTIONS:
            return JavaField(JavaIdentifier(f'PyBuiltinFunction_{name}'), 'singleton')
        else:
            return JavaIdentifier(f'pyglobal_{name}')

    def resolve_free_vars(self, lineno: int, scope_info: ScopeInfo, func_type: str) -> set[str]:
        free_vars = set()
        for name in scope_info.needs_from_outer:
            parent_scope = self.scope
            found = False
            while parent_scope:
                if name in parent_scope.info.explicit_globals:
                    break
                if parent_scope.info.kind == ScopeKind.FUNCTION and name in (parent_scope.info.locals | parent_scope.info.cell_vars | parent_scope.free_vars):
                    free_vars.add(name)
                    found = True
                    break
                parent_scope = parent_scope.parent
            if name in scope_info.nonlocals and not found:
                self.error(lineno, f"no binding for nonlocal {name!r} found")
        return free_vars

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
        if isinstance(node.value, (types.NoneType, bool, int, float, str, bytes)):
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
                    expr = JavaMethodCall(JavaIdentifier('PyBuiltinFunctionsImpl'), 'pyfunc_ascii', [expr])
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
            match node.func.id:
                case '__pythonj_abs__':
                    assert len(node.args) == 1 and not node.keywords, node.args
                    return JavaMethodCall(self.visit(node.args[0]), 'abs', [])
                case '__pythonj_delattr__':
                    assert len(node.args) == 2 and not node.keywords, node.args
                    return JavaMethodCall(JavaIdentifier('Runtime'), 'pythonjDelAttr', [self.visit(node.args[0]), self.visit(node.args[1])])
                case '__pythonj_dict_get__':
                    assert len(node.args) == 2 and not node.keywords, node.args
                    return JavaMethodCall(JavaIdentifier('Runtime'), 'pythonjDictGet', [self.visit(node.args[0]), self.visit(node.args[1])])
                case '__pythonj_format__':
                    assert len(node.args) == 2 and not node.keywords, node.args
                    return JavaMethodCall(JavaIdentifier('Runtime'), 'pythonjFormat', [self.visit(node.args[0]), self.visit(node.args[1])])
                case '__pythonj_getattr__':
                    assert len(node.args) == 2 and not node.keywords, node.args
                    return JavaMethodCall(JavaIdentifier('Runtime'), 'pythonjGetAttr', [self.visit(node.args[0]), self.visit(node.args[1])])
                case '__pythonj_hash__':
                    assert len(node.args) == 1 and not node.keywords, node.args
                    return JavaMethodCall(JavaIdentifier('Runtime'), 'pythonjHash', [self.visit(node.args[0])])
                case '__pythonj_isinstance_single__':
                    assert len(node.args) == 2 and not node.keywords, node.args
                    if isinstance(node.args[1], ast.Name) and node.args[1].id in ISINSTANCE_SINGLE_FASTPATH_BUILTIN_TYPES:
                        builtin_type = node.args[1].id
                        assert builtin_type in BUILTIN_TYPES, builtin_type
                        return JavaMethodCall(JavaIdentifier('PyBool'), 'create', [
                            JavaBinaryOp('instanceof', self.visit(node.args[0]), JavaIdentifier(BUILTIN_TYPES[builtin_type]))
                        ])
                    return JavaMethodCall(JavaIdentifier('Runtime'), 'pythonjIsInstanceSingle', [self.visit(node.args[0]), self.visit(node.args[1])])
                case '__pythonj_issubclass_single__':
                    assert len(node.args) == 2 and not node.keywords, node.args
                    return JavaMethodCall(JavaIdentifier('Runtime'), 'pythonjIsSubclassSingle', [self.visit(node.args[0]), self.visit(node.args[1])])
                case '__pythonj_iter__':
                    assert len(node.args) == 1 and not node.keywords, node.args
                    return JavaMethodCall(self.visit(node.args[0]), 'iter', [])
                case '__pythonj_len__':
                    assert len(node.args) == 1 and not node.keywords, node.args
                    return JavaMethodCall(JavaIdentifier('Runtime'), 'pythonjLen', [self.visit(node.args[0])])
                case '__pythonj_next__':
                    assert len(node.args) == 1 and not node.keywords, node.args
                    return JavaMethodCall(self.visit(node.args[0]), 'next', [])
                case '__pythonj_repr__':
                    assert len(node.args) == 1 and not node.keywords, node.args
                    return JavaMethodCall(JavaIdentifier('Runtime'), 'pythonjRepr', [self.visit(node.args[0])])
                case '__pythonj_setattr__':
                    assert len(node.args) == 3 and not node.keywords, node.args
                    return JavaMethodCall(JavaIdentifier('Runtime'), 'pythonjSetAttr', [self.visit(node.args[0]), self.visit(node.args[1]), self.visit(node.args[2])])
        if isinstance(node.func, ast.Name) and node.func.id in self.python_helper_names:
            if node.keywords or any(isinstance(arg, ast.Starred) for arg in node.args):
                self.error(node.lineno, 'same-file helper calls only support plain positional args')
            assert self.python_helper_class is not None, node.func.id
            return JavaMethodCall(JavaIdentifier(self.python_helper_class), f'pyfunc_{node.func.id}', [self.visit(arg) for arg in node.args])

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
        assert self.scope.info.kind == ScopeKind.FUNCTION, node
        value = self.visit(node.value) if node.value else JavaPyConstant(None)
        self.code.append(JavaReturnStatement(value))

    def visit_Raise(self, node) -> None:
        if node.exc is None:
            self.error(node.lineno, "bare 'raise' is unsupported")
            return
        if node.cause is not None:
            self.error(node.lineno, "'raise ... from ...' is unsupported")
            self.visit(node.exc)
            self.visit(node.cause)
            return
        self.code.append(JavaThrowStatement(JavaMethodCall(JavaIdentifier('Runtime'), 'raiseExpr', [self.visit(node.exc)])))

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
            if handler.type is not None:
                if not isinstance(handler.type, ast.Name):
                    self.error(node.lineno, "only 'except SomeException [as e]:' with a named built-in exception is supported")
                elif handler.type.id not in EXCEPTION_TYPES:
                    self.error(node.lineno, "only built-in exception names are supported in 'except' clauses")

        try_body = self.visit_block(node.body)

        exc_type = None
        exc_name = None
        catch_body = []
        if node.handlers:
            exc_type = 'PyRaise'
            exc_name = self.scope.make_temp()
            handler = node.handlers[0]
            with self.new_block() as catch_body:
                if handler.type is not None:
                    caught_exc = JavaField(JavaIdentifier(exc_name), 'exc')
                    expected_exc = JavaIdentifier(f'Py{cast(ast.Name, handler.type).id}')
                    catch_body.extend(if_statement(
                        unary_op('!', JavaBinaryOp('instanceof', caught_exc, expected_exc)),
                        [JavaThrowStatement(JavaIdentifier(exc_name))],
                        [],
                    ))
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

    def check_args(self, lineno: int, args: ast.arguments) -> tuple[list[str], list[object]]:
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
        defaults: list[object] = []
        for default in args.defaults:
            if isinstance(default, ast.Constant) and is_constant_default(default.value):
                defaults.append(default.value)
            elif isinstance(default, ast.Tuple):
                try:
                    value = ast.literal_eval(default)
                except (TypeError, ValueError):
                    self.error(lineno, 'only constant argument defaults are supported')
                else:
                    if is_constant_default(value):
                        defaults.append(value)
                    else:
                        self.error(lineno, 'only constant argument defaults are supported')
            else:
                self.error(lineno, 'only constant argument defaults are supported')
        for arg in args.args:
            # arg.type_comment is ignored; we only plan to support "real" type annotations.
            if arg.annotation:
                self.error(lineno, 'argument type annotations are unsupported')
        return ([arg.arg for arg in args.args], defaults)

    @contextmanager
    def new_function(self, scope_info: ScopeInfo, free_vars: set[str], qualname: str) -> Iterator[None]:
        saved_scope = self.scope
        self.scope = Scope(self.scope, scope_info, qualname)
        self.scope.free_vars = free_vars.copy()
        try:
            yield
        finally:
            self.scope = saved_scope

    def qualname(self, name: str) -> str:
        return name if self.scope.qualname is None else f'{self.scope.qualname}.<locals>.{name}'

    def add_function(self, py_name: str, java_name: str, arg_names: list[str], arg_defaults: list[object], body: list[JavaStatement],
                     invisible_args: bool = False) -> None:
        n_args = len(arg_names)
        n_required = n_args - len(arg_defaults)
        bind_arg_names = [f'pyarg_{arg}' for arg in arg_names]
        free_var_names = sorted(self.scope.free_vars)
        py_name_java = java_string_literal(py_name)
        arg_names_java = ', '.join(java_string_literal(arg) for arg in arg_names)
        required_arg_names_java = ', '.join(java_string_literal(arg) for arg in arg_names[:n_required])
        func_code = [
            f'private static final class {java_name} extends PyFunction {{',
            *(f'private final PyCell pycell_{name};' for name in free_var_names),
            (f'{java_name}({", ".join(f"PyCell pycell_{name}" for name in free_var_names)}) {{' if free_var_names else f'{java_name}() {{'),
            f'super({py_name_java});',
            *(f'this.pycell_{name} = pycell_{name};' for name in free_var_names),
            '}',
            '@Override public PyObject call(PyObject[] args, PyDict kwargs) {',
            'int argsLength = args.length;',
            *(f'PyObject {name} = (argsLength >= {i + 1}) ? args[{i}] : null;' for (i, name) in enumerate(bind_arg_names)),
            'if ((kwargs == null) || !kwargs.boolValue()) {',
        ]
        if n_required == n_args:
            func_code.extend([
                f'if (argsLength != {n_args}) {{',
                f'throw Runtime.raiseUserExactArgs(args, {n_args}, {py_name_java}{", " if arg_names_java else ""}{arg_names_java});',
                '}',
            ])
        else:
            if n_required != 0:
                func_code.extend([
                    f'if (argsLength < {n_required}) {{',
                    f'throw Runtime.raiseUserMissingArgs(argsLength, {py_name_java}, {required_arg_names_java});',
                    '}',
                ])
            func_code.extend([
                f'if (argsLength > {n_args}) {{',
                f'throw Runtime.raiseUserFromToArgs(args, {n_required}, {n_args}, {py_name_java});',
                '}',
            ])
        func_code.append('} else {')
        if arg_names:
            func_code.extend([
                'for (var x: kwargs.items.entrySet()) {',
                'String kwName = ((PyString)x.getKey()).value;',
                'PyObject kwValue = x.getValue();',
            ])
            for (i, arg_name) in enumerate(arg_names):
                prefix = '' if i == 0 else 'else '
                func_code.extend([
                    f'{prefix}if (kwName.equals({java_string_literal(arg_name)})) {{',
                    f'if ({bind_arg_names[i]} != null) {{',
                    f'throw Runtime.raiseMultipleValues({py_name_java}, {java_string_literal(arg_name)});',
                    '}',
                    f'{bind_arg_names[i]} = kwValue;',
                    '}',
                ])
            func_code.extend([
                'else {',
                f'throw Runtime.raiseUnexpectedKwArg({py_name_java}, kwName);',
                '}',
                '}',
            ])
        else:
            func_code.extend([
                'String kwName = ((PyString)kwargs.items.keySet().iterator().next()).value;',
                f'throw Runtime.raiseUnexpectedKwArg({py_name_java}, kwName);',
            ])
        if n_args != 0:
            func_code.extend([
                f'if (argsLength > {n_args}) {{',
                f'throw Runtime.raiseUserFromToArgs(args, {n_required}, {n_args}, {py_name_java});',
                '}',
            ])
            if n_required != 0:
                func_code.append(f'if ({ " || ".join(f"{bind_arg_names[i]} == null" for i in range(n_required)) }) {{')
                func_code.append(f'throw Runtime.raiseUserMissingKwArgs({py_name_java}, new PyObject[] {{{", ".join(bind_arg_names[:n_required])}}}, {required_arg_names_java});')
                func_code.append('}')
        func_code.append('}')
        for (i, name) in enumerate(bind_arg_names[n_required:]):
            func_code.append(f'if ({name} == null) {{ {name} = {emit_default_expr(arg_defaults[i], self.pool)}; }}')
        if self.scope.used_expr_discard:
            func_code.append('PyObject expr_discard;')
        for (arg_name, bind_arg_name) in zip(arg_names, bind_arg_names):
            if arg_name in self.scope.info.cell_vars:
                func_code.append(f'PyCell pycell_{arg_name} = new PyCell({bind_arg_name});')
            else:
                local_arg_name = arg_name if invisible_args else f'pylocal_{arg_name}'
                func_code.append(f'PyObject {local_arg_name} = {bind_arg_name};')
        for name in sorted(self.scope.info.cell_vars - set(arg_names)):
            func_code.append(f'PyCell pycell_{name} = new PyCell({JavaPyConstant(None).emit_java(self.pool)});')
        for name in sorted(self.scope.info.locals - self.scope.info.cell_vars - set(arg_names)):
            if invisible_args or name not in arg_names:
                func_code.append(f'PyObject pylocal_{name};')
        func_code.extend(block_emit_java(block_simplify([*body, JavaReturnStatement(JavaPyConstant(None))]), self.pool))
        func_code.extend([
            '}',
            '}',
        ])
        assert java_name not in self.functions
        self.functions[java_name] = func_code

    def visit_FunctionDef(self, node) -> None:
        # node.type_comment is ignored; we only plan to support "real" type annotations.
        if node.decorator_list:
            self.error(node.lineno, 'function decorators are unsupported')
        if node.returns:
            self.error(node.lineno, 'function return annotations are unsupported')
        if node.type_params:
            self.error(node.lineno, 'function type parameters are unsupported')

        (arg_names, arg_defaults) = self.check_args(node.lineno, node.args)
        qualname = self.qualname(node.name)
        java_name = f'pyfunc_{node.name}_{self.n_functions}'
        self.n_functions += 1
        scope_info = self.scope_infos[node]
        free_vars = self.resolve_free_vars(node.lineno, scope_info, 'nested function')
        self.code.append(JavaAssignStatement(self.ident_expr(node.name), JavaCreateObject(java_name, [JavaIdentifier(f'pycell_{name}') for name in sorted(free_vars)])))

        with self.new_function(scope_info, free_vars, qualname):
            body = self.visit_block(node.body)
            self.add_function(qualname, java_name, arg_names, arg_defaults, body)

    def visit_ClassDef(self, node) -> None:
        if self.scope.info.kind != ScopeKind.MODULE:
            self.error(node.lineno, "only module-level class definitions are supported")
        if node.decorator_list:
            self.error(node.lineno, 'class decorators are unsupported')
        if node.bases:
            self.error(node.lineno, 'class inheritance is unsupported')
        if node.keywords:
            self.error(node.lineno, 'class keywords are unsupported')

        slots = None
        if len(node.body) == 1 and isinstance(node.body[0], ast.Assign):
            assign = node.body[0]
            if len(assign.targets) != 1 or not isinstance(assign.targets[0], ast.Name) or assign.targets[0].id != '__slots__':
                self.error(node.lineno, "only 'class X: pass' and 'class X: __slots__ = (...)' are supported")
            if not isinstance(assign.value, (ast.Tuple, ast.List)):
                self.error(node.lineno, "__slots__ must be a tuple or list of strings")
            slots = []
            for elt in assign.value.elts:
                if not isinstance(elt, ast.Constant) or not isinstance(elt.value, str):
                    self.error(node.lineno, "__slots__ must be a tuple or list of strings")
                if elt.value in slots:
                    self.error(node.lineno, 'duplicate name in __slots__')
                slots.append(elt.value)
        elif len(node.body) != 1 or not isinstance(node.body[0], ast.Pass):
            self.error(node.lineno, "only 'class X: pass' and 'class X: __slots__ = (...)' are supported")

        java_name = f'pyclass_{node.name}'
        type_name = f'{java_name}Type'
        type_class_name = f'{type_name}Class'
        class_code = []
        if slots is None:
            class_code.extend([
                f'private static final class {java_name} extends PyBagObject {{',
                f'{java_name}() {{ super({type_class_name}.singleton); }}',
                f'public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {{',
                f'Runtime.requireNoKwArgs(kwargs, type.name());',
                f'if (args.length != 0) {{',
                f'throw PyTypeError.raise(type.name() + "() takes no arguments");',
                f'}}',
                f'return new {java_name}();',
                '}',
                '}',
            ])
        else:
            class_code.extend([
                f'private static final class {java_name} extends PySlottedObject {{',
                *[f'private PyObject pyslot_{name} = null;' for name in slots],
                f'{java_name}() {{ super({type_class_name}.singleton); }}',
                '@Override public PyObject getAttr(String key) {',
                'switch (key) {',
            ])
            for name in slots:
                class_code.extend([
                    f'case {java_string_literal(name)}:',
                    f'if (pyslot_{name} == null) {{',
                    'throw raiseMissingAttr(key);',
                    '}',
                    f'return pyslot_{name};',
                ])
            class_code.extend([
                'case "__dict__": throw raiseMissingAttr(key);',
                'default: return super.getAttr(key);',
                '}',
                '}',
                '@Override public void setAttr(String key, PyObject value) {',
                'switch (key) {',
            ])
            for name in slots:
                class_code.extend([
                    f'case {java_string_literal(name)}:',
                    f'pyslot_{name} = value;',
                    'return;',
                ])
            class_code.extend([
                'case "__class__": throw Runtime.raiseNamedReadOnlyAttr(type(), key);',
                'default: super.setAttr(key, value);',
                '}',
                '}',
                '@Override public void delAttr(String key) {',
                'switch (key) {',
            ])
            for name in slots:
                class_code.extend([
                    f'case {java_string_literal(name)}:',
                    f'if (pyslot_{name} == null) {{',
                    'throw raiseMissingAttr(key);',
                    '}',
                    f'pyslot_{name} = null;',
                    'return;',
                ])
            class_code.extend([
                'case "__class__": throw Runtime.raiseNamedReadOnlyAttr(type(), key);',
                'default: super.delAttr(key);',
                '}',
                '}',
                f'public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {{',
                f'Runtime.requireNoKwArgs(kwargs, type.name());',
                f'if (args.length != 0) {{',
                f'throw PyTypeError.raise(type.name() + "() takes no arguments");',
                f'}}',
                f'return new {java_name}();',
                '}',
                '}',
            ])
        class_code.extend([
            f'private static final class {type_class_name} extends PyConcreteType {{',
            f'private static final {type_class_name} singleton = new {type_class_name}();',
            f'private {type_class_name}() {{ super({java_string_literal(node.name)}, {java_name}.class, {java_name}::newObj); }}',
            '}',
        ])
        assert java_name not in self.functions
        self.functions[java_name] = class_code
        self.code.append(JavaAssignStatement(self.ident_expr(node.name), JavaIdentifier(f'{type_class_name}.singleton')))

    def visit_Lambda(self, node) -> JavaExpr:
        (arg_names, arg_defaults) = self.check_args(node.lineno, node.args)
        qualname = self.qualname('<lambda>')
        java_name = f'pylambda{self.n_lambdas}'
        self.n_lambdas += 1

        scope_info = self.scope_infos[node]
        assert not scope_info.explicit_globals, scope_info.explicit_globals # should not be possible in a lambda
        assert not scope_info.nonlocals, scope_info.nonlocals # should not be possible in a lambda
        free_vars = self.resolve_free_vars(node.lineno, scope_info, 'lambda')

        with self.new_function(scope_info, free_vars, qualname):
            with self.new_block() as body:
                body.append(JavaReturnStatement(self.visit(node.body)))
            self.add_function(qualname, java_name, arg_names, arg_defaults, body)

        return JavaCreateObject(java_name, [JavaIdentifier(f'pycell_{name}') for name in sorted(free_vars)])

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

    def _lower_comp(self, node: ast.expr, py_name: str, type_name: str, method_name: str, lineno: int,
                    generators: list[ast.comprehension], elts: list[ast.expr]) -> JavaExpr:
        arg_name = 'iterable'
        java_name = f'pylambda{self.n_lambdas}'
        self.n_lambdas += 1

        qualname = self.qualname(py_name)
        scope_info = self.scope_infos[node]
        assert not scope_info.explicit_globals, scope_info.explicit_globals # should not be possible in a comprehension
        assert not scope_info.nonlocals, scope_info.nonlocals # should not be possible in a comprehension
        free_vars = self.resolve_free_vars(lineno, scope_info, 'comprehension')
        with self.new_function(scope_info, free_vars, qualname):
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
            self.add_function(qualname, java_name, [arg_name], [], body, invisible_args=True)

        return JavaMethodCall(JavaCreateObject(java_name, [JavaIdentifier(f'pycell_{name}') for name in sorted(free_vars)]), 'call', [JavaCreateArray('PyObject', [self.visit(generators[0].iter)]), JavaIdentifier('null')])

    def _lower_genexpr(self, node: ast.GeneratorExp) -> JavaExpr:
        if len(node.generators) != 1:
            self.error(node.lineno, 'generator expressions with multiple for clauses are unsupported')
            return JavaMethodCall(JavaCreateObject('PyTuple', []), 'iter', [])
        generator = node.generators[0]
        if generator.is_async:
            self.error(node.lineno, 'async generator expressions are unsupported')
            return JavaMethodCall(JavaCreateObject('PyTuple', []), 'iter', [])

        qualname = self.qualname('<genexpr>')
        java_name = f'pylambda{self.n_lambdas}'
        self.n_lambdas += 1

        scope_info = self.scope_infos[node]
        assert not scope_info.explicit_globals, scope_info.explicit_globals
        assert not scope_info.nonlocals, scope_info.nonlocals
        free_vars = self.resolve_free_vars(node.lineno, scope_info, 'generator expression')

        with self.new_function(scope_info, free_vars, qualname):
            self.scope.locals_are_fields = True
            with self.new_block() as next_body:
                temp_item = self.scope.make_temp()
                temp_item_expr = JavaIdentifier(temp_item)
                next_body.append(JavaVariableDecl('PyObject', temp_item, None))
                body: list[JavaStatement] = [JavaReturnStatement(self.visit(node.elt))]
                for _if in reversed(generator.ifs):
                    body = list(if_statement(bool_value(self.visit(_if)), body, [JavaContinueStatement()]))
                body = [
                    JavaAssignStatement(temp_item_expr, JavaMethodCall(JavaIdentifier('pyiter_iterable'), 'next', [])),
                    *if_statement(JavaBinaryOp('==', temp_item_expr, JavaIdentifier('null')), [JavaReturnStatement(JavaIdentifier('null'))], []),
                    *self.emit_bind(generator.target, temp_item_expr),
                    *body,
                ]
                next_body.append(JavaWhileStatement(JavaIdentifier('true'), body))

            free_var_names = sorted(self.scope.free_vars)
            func_code = [
                f'private static final class {java_name} extends PyIter {{',
                f'private static final PyConcreteType type_singleton = new PyConcreteType("generator", {java_name}.class);',
                *(f'private final PyCell pycell_{name};' for name in free_var_names),
                'private final PyIter pyiter_iterable;',
                *(f'private final PyCell pycell_{name} = new PyCell({JavaPyConstant(None).emit_java(self.pool)});' for name in sorted(self.scope.info.cell_vars)),
                *(f'private PyObject pylocal_{name} = {JavaPyConstant(None).emit_java(self.pool)};' for name in sorted(self.scope.info.locals - self.scope.info.cell_vars)),
                f'{java_name}({", ".join([*(f"PyCell pycell_{name}" for name in free_var_names), "PyObject iterable"])}) {{' if free_var_names else f'{java_name}(PyObject iterable) {{',
                *(f'this.pycell_{name} = pycell_{name};' for name in free_var_names),
                'this.pyiter_iterable = iterable.iter();',
                '}',
                '@Override public PyObject next() {',
                *block_emit_java(block_simplify(next_body), self.pool),
                '}',
                f'@Override public String repr() {{ return "<generator object {qualname}>"; }}',
                '@Override public PyConcreteType type() { return type_singleton; }',
                '}',
            ]
            assert java_name not in self.functions
            self.functions[java_name] = func_code

        return JavaCreateObject(java_name, [*(JavaIdentifier(f'pycell_{name}') for name in sorted(free_vars)), self.visit(generator.iter)])

    def visit_ListComp(self, node) -> JavaExpr:
        return self._lower_comp(node, '<listcomp>', 'PyList', 'pymethod_append', node.lineno, node.generators, [node.elt])

    def visit_SetComp(self, node) -> JavaExpr:
        return self._lower_comp(node, '<setcomp>', 'PySet', 'pymethod_add', node.lineno, node.generators, [node.elt])

    def visit_DictComp(self, node) -> JavaExpr:
        return self._lower_comp(node, '<dictcomp>', 'PyDict', 'setItem', node.lineno, node.generators, [node.key, node.value])

    def visit_GeneratorExp(self, node) -> JavaExpr:
        return self._lower_genexpr(node)

    def visit_Module(self, node) -> None:
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
        for name in sorted(self.scope.info.locals):
            writer.write(f'private static PyObject pyglobal_{name} = {JavaPyConstant(None).emit_java(self.pool)};')
        writer.write('')

        writer.write('public static void main(String[] args) {')
        for line in block_emit_java(block_simplify(self.global_code), self.pool):
            writer.write(line)
        writer.write('}')

        writer.write('')
        for line in self.pool.emit_pool():
            writer.write(line)
        writer.write('}')
        assert writer.indent == 0, writer.indent

def get_runtime_obj(name: str) -> object:
    if name.startswith('_io.'):
        return getattr(_io, name.split('.', 1)[1])
    elif name.startswith('types.'):
        return getattr(types, name.split('.', 1)[1])
    else:
        return getattr(builtins, name)

def gen_spec(spec_path: str) -> None:
    spec = {}
    for name in [*BUILTIN_TYPES, *sorted(EXCEPTION_TYPES),
                 'types.BuiltinFunctionType', 'types.ClassMethodDescriptorType',
                 'types.FunctionType', 'types.GetSetDescriptorType', 'types.MemberDescriptorType',
                 'types.MethodDescriptorType', 'types.NoneType', '_io.BufferedReader', '_io.TextIOWrapper']:
        obj = get_runtime_obj(name)
        attrs = {}
        for (k, v) in obj.__dict__.items():
            if k.startswith('__') and k not in {'__doc__'}:
                continue # only extract a subset of dunders
            v_type = type(v)
            if v_type is str:
                attrs[k] = {'kind': 'string', 'value': v}
            elif v_type is types.MemberDescriptorType:
                attrs[k] = {'kind': 'member', 'doc': v.__doc__}
            elif v_type is types.GetSetDescriptorType:
                attrs[k] = {'kind': 'getset', 'doc': v.__doc__}
            elif v_type is types.MethodDescriptorType:
                attrs[k] = {'kind': 'method', 'doc': v.__doc__}
            elif v_type is types.ClassMethodDescriptorType:
                attrs[k] = {'kind': 'classmethod', 'doc': v.__doc__}
            elif v_type is staticmethod:
                attrs[k] = {'kind': 'staticmethod'}
            else:
                assert False, (name, k, v, v_type)
        spec[name] = attrs

    with open(spec_path, 'w') as f:
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
        'center', 'count', 'decode', 'endswith', 'expandtabs', 'find', 'hex', 'index',
        'ljust', 'lstrip', 'partition', 'removeprefix', 'removesuffix', 'replace', 'rfind',
        'rindex', 'rjust', 'rpartition', 'rsplit', 'rstrip', 'split', 'splitlines', 'startswith', 'strip',
        'translate', 'zfill',
    },
    'str': {
        'center', 'count', 'encode', 'expandtabs', 'format', 'format_map', 'index', 'ljust',
        'lstrip', 'partition', 'replace', 'rfind', 'rindex', 'rjust',
        'rpartition', 'rsplit', 'rstrip', 'splitlines', 'strip', 'translate', 'zfill',
    },
    'type': {'mro'},
    '_io.BufferedReader': {
        '_dealloc_warn', 'detach', 'fileno', 'flush', 'isatty', 'peek', 'read1', 'readable', 'readinto',
        'readinto1', 'readline', 'seek', 'seekable', 'tell', 'truncate',
    },
    '_io.TextIOWrapper': {
        'detach', 'fileno', 'flush', 'isatty', 'readable', 'reconfigure', 'seek', 'seekable', 'tell',
        'truncate', 'writable', 'write',
    },
    'BaseException': {'add_note', 'with_traceback'},
}

NULL = object()
RAW_ARGS_KWARGS_BUILTINS = {'max', 'min', 'print'}

PYTHON_IMPLS = {
    '_runtime': {
        '_pyj_float_apply_width',
        '_pyj_float_apply_zero_fill',
        '_pyj_float_is_zero_result',
        '_pyj_float_sign_prefix',
        '_pyj_float_split_sign',
        'max_iterable',
        'min_iterable',
        'pyj_float_finish_text',
        'pyj_float_parse_spec',
        'pyj_float_special_text',
    },
    'builtins': {'abs', 'all', 'any', 'delattr', 'format', 'getattr', 'hash', 'hasattr', 'isinstance', 'issubclass', 'len', 'next', 'repr', 'setattr', 'sum'},
    'dict': {'setdefault'},
    'float': {'conjugate'},
    'int': {'conjugate', 'is_integer'},
    'str': {'removeprefix', 'removesuffix'},
}

def make_param(name: str, default: object = inspect.Parameter.empty) -> inspect.Parameter:
    return inspect.Parameter(name, inspect.Parameter.POSITIONAL_ONLY, default=default)

def is_constant_default(default: object) -> bool:
    if isinstance(default, (types.NoneType, bool, int, float, str, bytes)):
        return True
    if isinstance(default, tuple):
        return all(is_constant_default(x) for x in default)
    return False

SYNTHETIC_PARAMS = {
    'builtins': {
        'dir': [make_param('object', NULL)],
        'getattr': [make_param('object'), make_param('name'), make_param('default', NULL)],
        'iter': [make_param('iterable'), make_param('sentinel', NULL)],
        'next': [make_param('iterator'), make_param('default', NULL)],
    },
    'dict': {
        'pop': [make_param('key'), make_param('defaultValue', NULL)],
    },
    'list': {
        'index': [make_param('value'), make_param('start', NULL), make_param('stop', NULL)],
    },
    'str': {
        'endswith': [make_param('suffix'), make_param('start', None), make_param('end', None)],
        'find': [make_param('sub'), make_param('start', None), make_param('end', None)],
        'maketrans': [make_param('x'), make_param('y', NULL), make_param('z', NULL)],
        'startswith': [make_param('prefix'), make_param('start', None), make_param('end', None)],
    },
    '_io.TextIOWrapper': {
        'read': [make_param('size', -1)],
    },
    'tuple': {
        'index': [make_param('value'), make_param('start', NULL), make_param('stop', NULL)],
    },
}

def get_signature_params(target: object, implicit_name: Optional[str]) -> Optional[list[inspect.Parameter]]:
    try:
        params = list(inspect.signature(target).parameters.values())
    except (TypeError, ValueError):
        return None
    if implicit_name is not None:
        if not params or params[0].name != implicit_name or params[0].kind is not inspect.Parameter.POSITIONAL_ONLY:
            return None
        params = params[1:]
    return params

def get_method_params(name: str, method_name: str) -> Optional[list[inspect.Parameter]]:
    synthetic = SYNTHETIC_PARAMS.get(name, {}).get(method_name)
    if synthetic is not None:
        return synthetic
    obj = get_runtime_obj(name)
    desc = obj.__dict__.get(method_name)
    if desc is None:
        return None
    desc_type = type(desc)
    if desc_type is types.MethodDescriptorType:
        params = get_signature_params(desc, 'self')
    elif desc_type is types.ClassMethodDescriptorType:
        params = get_signature_params(desc, 'type')
    elif desc_type is staticmethod:
        params = get_signature_params(getattr(obj, method_name), None)
    else:
        params = None
    return params

def get_builtin_function_params(name: str) -> Optional[list[inspect.Parameter]]:
    desc = builtins.__dict__.get(name)
    if type(desc) is not types.BuiltinFunctionType:
        return None
    params = get_signature_params(desc, None)
    if params is not None:
        return params
    return SYNTHETIC_PARAMS['builtins'].get(name)

@dataclass(slots=True)
class SignatureShape:
    params: list[inspect.Parameter]
    posonly_params: list[inspect.Parameter]
    poskw_params: list[inspect.Parameter]
    kwonly_params: list[inspect.Parameter]
    vararg_param: Optional[inspect.Parameter]
    max_total: int
    max_positional: int
    missing_style: Optional[str]

def analyze_params(params: list[inspect.Parameter]) -> SignatureShape:
    posonly_params = [param for param in params if param.kind is inspect.Parameter.POSITIONAL_ONLY]
    poskw_params = [param for param in params if param.kind is inspect.Parameter.POSITIONAL_OR_KEYWORD]
    kwonly_params = [param for param in params if param.kind is inspect.Parameter.KEYWORD_ONLY]
    vararg_params = [param for param in params if param.kind is inspect.Parameter.VAR_POSITIONAL]
    vararg_param = vararg_params[0] if vararg_params else None
    max_total = len(params)
    max_positional = len(posonly_params) + len(poskw_params)
    if poskw_params and poskw_params[0].default is inspect.Parameter.empty:
        missing_style = 'required_arg'
    elif posonly_params and posonly_params[0].default is inspect.Parameter.empty:
        if poskw_params:
            missing_style = 'min_positional'
        else:
            missing_style = 'exact_args'
    else:
        missing_style = None
    return SignatureShape(params, posonly_params, poskw_params, kwonly_params, vararg_param, max_total, max_positional, missing_style)

def emit_python_function_static(visitor: PythonjVisitor, func_name: str, arg_names: list[str], body: list[JavaStatement]) -> list[str]:
    func_code = [
        f'static PyObject pyfunc_{func_name}({", ".join(f"PyObject pyarg_{arg}" for arg in arg_names)}) {{',
    ]
    if visitor.scope.used_expr_discard:
        func_code.append('PyObject expr_discard;')
    for arg_name in arg_names:
        if arg_name in visitor.scope.info.cell_vars:
            func_code.append(f'PyCell pycell_{arg_name} = new PyCell(pyarg_{arg_name});')
        else:
            func_code.append(f'PyObject pylocal_{arg_name} = pyarg_{arg_name};')
    for name in sorted(visitor.scope.info.cell_vars - set(arg_names)):
        func_code.append(f'PyCell pycell_{name} = new PyCell({JavaPyConstant(None).emit_java(visitor.pool)});')
    for name in sorted(visitor.scope.info.locals - visitor.scope.info.cell_vars - set(arg_names)):
        func_code.append(f'PyObject pylocal_{name};')
    func_code.extend(block_emit_java(block_simplify([*body, JavaReturnStatement(JavaPyConstant(None))]), visitor.pool))
    func_code.append('}')
    return func_code

def translate_python_impl(path: str, func_name: str, display_name: str, role: str,
                          pool: ConstantPool) -> tuple[list[list[str]], list[str]]:
    with open(path, encoding='utf-8') as f:
        node = ast.parse(f.read(), path)
    funcs = {x.name: x for x in node.body if isinstance(x, ast.FunctionDef)}
    func = funcs[func_name]
    analyzer = ScopeAnalyzer()
    analyzer.visit(node)
    visitor = PythonjVisitor(display_name, analyzer.scope_infos, analyzer.scope_infos[node])
    visitor.pool = pool
    visitor.allow_intrinsics = True
    if role == 'runtime helper':
        visitor.python_helper_names = set(funcs)
        visitor.python_helper_class = 'PyRuntimePythonImpl'

    assert func.name == func_name, (func.name, func_name)
    (arg_names, arg_defaults) = visitor.check_args(func.lineno, func.args)
    assert not arg_defaults, (func_name, arg_defaults)
    scope_info = visitor.scope_infos[func]
    free_vars = visitor.resolve_free_vars(func.lineno, scope_info, role)
    assert not free_vars, (func_name, free_vars)
    with visitor.new_function(scope_info, free_vars, func.name):
        body = visitor.visit_block(func.body)
        helper_method = emit_python_function_static(visitor, func_name, arg_names, body)
    assert visitor.n_errors == 0, (func_name, visitor.n_errors)
    return (list(visitor.functions.values()), helper_method)

def translate_python_builtin_impl(func_name: str, pool: ConstantPool) -> tuple[list[list[str]], list[str]]:
    return translate_python_impl('pythonj_builtins.py', func_name, f'<builtin {func_name}>', 'builtin function', pool)

def translate_python_method_impl(type_name: str, method_name: str, pool: ConstantPool) -> tuple[list[list[str]], list[str]]:
    func_name = f'{type_name}__{method_name}'
    return translate_python_impl('pythonj_methods.py', func_name, f'<method {type_name}.{method_name}>', 'builtin method', pool)

def translate_python_runtime_impl(func_name: str, pool: ConstantPool) -> tuple[list[list[str]], list[str]]:
    return translate_python_impl('pythonj_runtime.py', func_name, f'<runtime {func_name}>', 'runtime helper', pool)

def infer_default_expr(default: object) -> Optional[str]:
    if default is None:
        return 'PyNone.singleton'
    elif default is NULL:
        return 'null'
    elif default is False or default is True:
        return f'PyBool.{str(default).lower()}_singleton'
    elif type(default) is int:
        if default == -1:
            return 'PyInt.singleton_neg1'
        elif default == 0:
            return 'PyInt.singleton_0'
        elif default == 1:
            return 'PyInt.singleton_1'
        else:
            return None
    elif default == '':
        return 'PyString.empty_singleton'
    else:
        return None

def emit_default_expr(default: object, pool: ConstantPool) -> str:
    inferred = infer_default_expr(default)
    if inferred is not None:
        return inferred
    else:
        return JavaPyConstant(default).emit_java(pool)

def emit_arg_binding(writer: IndentedWriter, shape: SignatureShape, positional_name: str,
                     kw_name: str, kw_overflow_args_length: str,
                     noarg_name: str, pool: ConstantPool) -> list[str]:
    if not shape.posonly_params and not shape.poskw_params and not shape.kwonly_params and shape.vararg_param is not None:
        writer.write('if ((kwargs != null) && kwargs.boolValue()) {')
        writer.write(f'throw Runtime.raiseNoKwArgs("{noarg_name}");')
        writer.write('}')
        return ['args']
    writer.write('int argsLength = args.length;')
    if shape.params and not shape.poskw_params and not shape.kwonly_params and shape.vararg_param is None:
        writer.write('if ((kwargs != null) && kwargs.boolValue()) {')
        writer.write(f'throw Runtime.raiseNoKwArgs("{noarg_name}");')
        writer.write('}')
        min_args = sum(param.default is inspect.Parameter.empty for param in shape.posonly_params)
        max_args = len(shape.posonly_params)
        bind_args = [f'args[{i}]' for i in range(min_args)]
        if min_args == max_args:
            writer.write(f'if (argsLength != {max_args}) {{')
            if max_args == 0:
                writer.write(f'throw Runtime.raiseNoArgs(args, "{noarg_name}");')
            elif max_args == 1:
                writer.write(f'throw Runtime.raiseOneArg(args, "{noarg_name}");')
            else:
                writer.write(f'throw Runtime.raiseExactArgs(args, {max_args}, "{kw_name}");')
            writer.write('}')
        else:
            if min_args > 0:
                writer.write(f'if (argsLength < {min_args}) {{')
                writer.write(f'throw Runtime.raiseMinArgs(args, {min_args}, "{kw_name}");')
                writer.write('}')
            writer.write(f'if (argsLength > {max_args}) {{')
            writer.write(f'throw Runtime.raiseMaxArgs(args, {max_args}, "{kw_name}");')
            writer.write('}')
            for i in range(min_args, max_args):
                writer.write(
                    f'PyObject arg{i} = (argsLength >= {i+1}) ? args[{i}] : '
                    f'{emit_default_expr(shape.posonly_params[i].default, pool)};'
                )
                bind_args.append(f'arg{i}')
        return bind_args
    if not shape.params:
        writer.write('if ((kwargs != null) && kwargs.boolValue()) {')
        writer.write(f'throw Runtime.raiseNoKwArgs("{noarg_name}");')
        writer.write('}')
        writer.write('if (argsLength != 0) {')
        writer.write(f'throw Runtime.raiseNoArgs(args, "{noarg_name}");')
        writer.write('}')
        return []
    posonly_params = shape.posonly_params
    poskw_params = shape.poskw_params
    kwonly_params = shape.kwonly_params
    max_total = shape.max_total
    max_positional = shape.max_positional
    missing_style = shape.missing_style
    if missing_style == 'exact_args':
        writer.write(f'if (argsLength != 1) {{ throw Runtime.raiseExactArgs(args, 1, "{positional_name}"); }}')
    elif missing_style == 'min_positional':
        writer.write('if (argsLength == 0) {')
        writer.write(f'throw PyTypeError.raise("{positional_name}() takes at least 1 positional argument (0 given)");')
        writer.write('}')
    for (i, param) in enumerate(posonly_params + poskw_params):
        writer.write(f'PyObject {param.name} = (argsLength >= {i+1}) ? args[{i}] : null;')
    for param in kwonly_params:
        writer.write(f'PyObject {param.name} = null;')
    if kwonly_params and not poskw_params:
        if missing_style != 'exact_args' and (max_positional != 0):
            assert False, (positional_name, shape.params, missing_style)
        writer.write('if ((kwargs != null) && kwargs.boolValue()) {')
        writer.write('long kwargsLen = kwargs.len();')
        writer.write(f'if (kwargsLen > {max_total}) {{')
        writer.write(f'throw Runtime.raiseAtMostKwArgs("{kw_name}", {max_total}, {kw_overflow_args_length}, kwargsLen);')
        writer.write('}')
        writer.write('String unknownKw = null;')
        writer.write('for (var x: kwargs.items.entrySet()) {')
        writer.write('PyString kw = (PyString)x.getKey(); // PyString validated at call site')
        for (i, param) in enumerate(kwonly_params):
            prefix = 'if' if i == 0 else 'else if'
            writer.write(f'{prefix} (kw.value.equals("{param.name}")) {{')
            writer.write(f'{param.name} = x.getValue();')
            writer.write('}')
        writer.write('else if (unknownKw == null) {')
        writer.write('unknownKw = kw.value;')
        writer.write('}')
        writer.write('}')
        writer.write('if (unknownKw != null) {')
        writer.write(f'throw Runtime.raiseUnexpectedKwArg("{kw_name}", unknownKw);')
        writer.write('}')
        writer.write('}')
        if max_positional == 0:
            writer.write(f'if (argsLength > {max_total}) {{')
            writer.write(f'throw Runtime.raiseAtMostArgs("{positional_name}", {max_total}, argsLength);')
            writer.write('} else if (argsLength != 0) {')
            writer.write(f'throw PyTypeError.raise("{positional_name}() takes no positional arguments");')
            writer.write('}')
    else:
        writer.write('if ((kwargs != null) && kwargs.boolValue()) {')
        writer.write('long kwargsLen = kwargs.len();')
        writer.write(f'if (argsLength + kwargsLen > {max_total}) {{')
        writer.write(f'throw Runtime.raiseAtMostKwArgs("{kw_name}", {max_total}, argsLength, kwargsLen);')
        writer.write('}')
        writer.write('String unknownKw = null;')
        writer.write('for (var x: kwargs.items.entrySet()) {')
        writer.write('PyString kw = (PyString)x.getKey(); // PyString validated at call site')
        kw_index = len(posonly_params)
        for (i, param) in enumerate(poskw_params):
            prefix = 'if' if i == 0 else 'else if'
            writer.write(f'{prefix} (kw.value.equals("{param.name}")) {{')
            writer.write(f'if ({param.name} != null) {{')
            writer.write(f'throw Runtime.raiseArgGivenByNameAndPosition("{kw_name}", "{param.name}", {kw_index + i + 1});')
            writer.write('}')
            writer.write(f'{param.name} = x.getValue();')
            writer.write('}')
        for (i, param) in enumerate(kwonly_params):
            prefix = 'if' if (not poskw_params and i == 0) else 'else if'
            writer.write(f'{prefix} (kw.value.equals("{param.name}")) {{')
            writer.write(f'{param.name} = x.getValue();')
            writer.write('}')
        writer.write('else if (unknownKw == null) {')
        writer.write('unknownKw = kw.value;')
        writer.write('}')
        writer.write('}')
        for (i, param) in enumerate(posonly_params + poskw_params):
            if param.default is inspect.Parameter.empty:
                if missing_style == 'required_arg':
                    writer.write(f'if ({param.name} == null) {{')
                    writer.write(f'throw Runtime.raiseMissingRequiredArg("{positional_name}", "{param.name}", {i + 1});')
                    writer.write('}')
                elif param.kind is inspect.Parameter.POSITIONAL_ONLY:
                    assert missing_style == 'min_positional', (positional_name, param)
        writer.write('if (unknownKw != null) {')
        writer.write(f'throw Runtime.raiseUnexpectedKwArg("{kw_name}", unknownKw);')
        writer.write('}')
        writer.write('}')
        if max_positional < max_total:
            writer.write(f'if (argsLength > {max_total}) {{')
            writer.write(f'throw Runtime.raiseAtMostArgs("{positional_name}", {max_total}, argsLength);')
            writer.write(f'}} else if (argsLength > {max_positional}) {{')
            writer.write(f'throw Runtime.raiseAtMostPosArgs("{positional_name}", {max_positional}, argsLength);')
            writer.write('}')
        else:
            writer.write(f'if (argsLength > {max_total}) {{')
            writer.write(f'throw Runtime.raiseAtMostArgs("{positional_name}", {max_total}, argsLength);')
            writer.write('}')
        if missing_style == 'required_arg':
            for (i, param) in enumerate(posonly_params + poskw_params):
                if param.default is inspect.Parameter.empty:
                    writer.write(f'if ({param.name} == null) {{')
                    writer.write(f'throw Runtime.raiseMissingRequiredArg("{positional_name}", "{param.name}", {i + 1});')
                    writer.write('}')
    bind_args = []
    for param in shape.params:
        if param.default is not inspect.Parameter.empty:
            writer.write(f'if ({param.name} == null) {{')
            writer.write(f'{param.name} = {emit_default_expr(param.default, pool)};')
            writer.write('}')
        bind_args.append(param.name)
    return bind_args

def get_java_name(name: str) -> str:
    if name.startswith('_io.'):
        return f"Py{name.split('.', 1)[1]}" # _io.Foo -> PyFoo + PyFooType
    elif name == 'types.BuiltinFunctionType':
        return 'PyBuiltinFunctionOrMethod' # weird shared type
    elif name.startswith('types.') and name.endswith('Type'):
        return f"Py{name[:-4].split('.', 1)[1]}" # types.FooType -> PyFoo + PyFooType
    elif name in EXCEPTION_TYPES:
        return f'Py{name}'
    else:
        return BUILTIN_TYPES[name]

def gen_code(spec_path: str, java_path: str) -> None:
    with open(spec_path) as f:
        spec = json.load(f)

    pool = ConstantPool('PyGeneratedConstants')
    with open(java_path, 'w') as f:
        writer = IndentedWriter(f, 0)
        python_runtime_helper_classes: list[list[str]] = []
        python_runtime_helper_methods: list[list[str]] = []
        python_builtin_helper_classes: list[list[str]] = []
        python_builtin_helper_methods: list[list[str]] = []
        python_method_helper_classes: list[list[str]] = []
        python_method_helper_methods: list[list[str]] = []
        for (name, attrs) in spec.items():
            java_name = get_java_name(name)
            match name:
                case 'types.BuiltinFunctionType': py_name = 'builtin_function_or_method'
                case 'types.ClassMethodDescriptorType': py_name = 'classmethod_descriptor'
                case 'types.FunctionType': py_name = 'function'
                case 'types.GetSetDescriptorType': py_name = 'getset_descriptor'
                case 'types.MemberDescriptorType': py_name = 'member_descriptor'
                case 'types.MethodDescriptorType': py_name = 'method_descriptor'
                case 'types.NoneType': py_name = 'NoneType'
                case _: py_name = name

            writer.write(f'final class {java_name}Type extends PyConcreteType {{')
            writer.write(f'public static final {java_name}Type singleton = new {java_name}Type();')
            for (k, v) in attrs.items():
                if v['kind'] == 'string':
                    writer.write(f"private static final PyString pyattr_{k} = new PyString({java_string_literal(v['value'])});")
                elif v['kind'] == 'member':
                    doc = 'null' if v['doc'] is None else java_string_literal(v['doc'])
                    writer.write(f"private static final PyMemberDescriptor pyattr_{k} = new PyMemberDescriptor(singleton, {java_string_literal(k)}, {java_name}::pymember_{k}, {doc});")
                elif v['kind'] == 'getset':
                    doc = 'null' if v['doc'] is None else java_string_literal(v['doc'])
                    writer.write(f"private static final PyGetSetDescriptor pyattr_{k} = new PyGetSetDescriptor(singleton, {java_string_literal(k)}, {java_name}::pygetset_{k}, {doc});")
                elif v['kind'] == 'method':
                    doc = 'null' if v['doc'] is None else java_string_literal(v['doc'])
                    if name in UNIMPLEMENTED_METHODS and k in UNIMPLEMENTED_METHODS[name]:
                        constructor = f'obj -> new {java_name}MethodUnimplemented(obj, {java_string_literal(k)})'
                    else:
                        constructor = f'{java_name}Method_{k}::new'
                    writer.write(f"private static final PyMethodDescriptor pyattr_{k} = new PyMethodDescriptor(singleton, {java_string_literal(k)}, {constructor}, {doc});")
                elif v['kind'] == 'classmethod':
                    doc = 'null' if v['doc'] is None else java_string_literal(v['doc'])
                    constructor = f'{java_name}ClassMethod_{k}::new'
                    writer.write(f"private static final PyClassMethodDescriptor pyattr_{k} = new PyClassMethodDescriptor(singleton, {java_string_literal(k)}, {constructor}, {doc});")
                elif v['kind'] == 'staticmethod':
                    constructor = f'new {java_name}StaticMethod_{k}(singleton)'
                    writer.write(f'private static final PyStaticMethod pyattr_{k} = new PyStaticMethod(singleton, {java_string_literal(k)}, {constructor});')
                else:
                    assert False, (name, k, v)
            writer.write(f'private static final java.util.LinkedHashMap<PyObject, PyObject> attrs = new java.util.LinkedHashMap<>({len(attrs)});')
            writer.write('static {')
            for k in attrs:
                writer.write(f'attrs.put(new PyString("{k}"), pyattr_{k});')
            writer.write('}')
            writer.write('')
            writer.write(f'private {java_name}Type() {{ super({java_string_literal(py_name)}, {java_name}.class, {java_name}::newObj); }}')
            writer.write('@Override public java.util.Map<PyObject, PyObject> getAttributes() { return attrs; }')
            writer.write('@Override public PyObject lookupAttr(String name) {')
            writer.write('switch (name) {')
            for (k, v) in attrs.items():
                writer.write(f'case {java_string_literal(k)}: return pyattr_{k};')
            writer.write('default: return null;')
            writer.write('}')
            writer.write('}')
            writer.write('}')
            writer.write('')

            if name in UNIMPLEMENTED_METHODS:
                writer.write(f'final class {java_name}MethodUnimplemented extends PyBuiltinMethod<{java_name}> {{')
                writer.write('private final String name;')
                writer.write(f'{java_name}MethodUnimplemented(PyObject _self, String _name) {{ super(({java_name})_self); name = _name; }}')
                writer.write('@Override public String methodName() { return name; }')
                writer.write('@Override public PyObject call(PyObject[] args, PyDict kwargs) {')
                writer.write(f'throw new UnsupportedOperationException("{name}." + name + "() unimplemented");')
                writer.write('}')
                writer.write('}')
                writer.write('')

            gen_methods = {}
            for (method_name, v) in attrs.items():
                if v['kind'] not in {'method', 'classmethod', 'staticmethod'}:
                    continue
                if name in UNIMPLEMENTED_METHODS and method_name in UNIMPLEMENTED_METHODS[name]:
                    continue
                params = get_method_params(name, method_name)
                if params is None:
                    gen_methods[method_name] = None
                else:
                    gen_methods[method_name] = analyze_params(params)

            if gen_methods:
                if '.' in name:
                    py_name = name.rsplit('.')[1]
                else:
                    py_name = name
                for (method_name, kwarg_params) in gen_methods.items():
                    kind = attrs[method_name]['kind']
                    if kind == 'method':
                        method_class_name = f'{java_name}Method_{method_name}'
                        self_type = java_name
                        ctor_arg = 'PyObject _self'
                        super_arg = f'({java_name})_self'
                        method_target = 'self'
                    elif kind == 'classmethod':
                        method_class_name = f'{java_name}ClassMethod_{method_name}'
                        self_type = 'PyType'
                        ctor_arg = 'PyType _self'
                        super_arg = '_self'
                        method_target = java_name
                    elif kind == 'staticmethod':
                        method_class_name = f'{java_name}StaticMethod_{method_name}'
                        self_type = 'PyType'
                        ctor_arg = 'PyType _self'
                        super_arg = '_self'
                        method_target = java_name
                    else:
                        assert False, (name, method_name, kind)
                    method_impl_target = None
                    if kind == 'method' and method_name in PYTHON_IMPLS.get(name, set()):
                        helper_name = f'{name.replace(".", "_")}__{method_name}'
                        method_impl_target = f'PyBuiltinMethodsPythonImpl.pyfunc_{helper_name}'
                        (helper_classes, helper_method) = translate_python_method_impl(name, method_name, pool)
                        python_method_helper_classes.extend(helper_classes)
                        python_method_helper_methods.append(helper_method)
                    writer.write(f'final class {method_class_name} extends PyBuiltinMethod<{self_type}> {{')
                    writer.write(f'{method_class_name}({ctor_arg}) {{ super({super_arg}); }}')
                    writer.write(f'@Override public String methodName() {{ return "{method_name}"; }}')
                    writer.write('@Override public PyObject call(PyObject[] args, PyDict kwargs) {')
                    if kwarg_params is None:
                        bind_args = ['args', 'kwargs']
                    else:
                        bind_args = emit_arg_binding(
                            writer, kwarg_params, method_name,
                            method_name,
                            'argsLength',
                            f'{py_name}.{method_name}',
                            pool,
                        )
                    if kind == 'classmethod':
                        bind_args = ['self'] + bind_args
                    if method_impl_target is not None:
                        writer.write(f"return {method_impl_target}(self{', ' if bind_args else ''}{', '.join(bind_args)});")
                    else:
                        writer.write(f"return {method_target}.pymethod_{method_name}({', '.join(bind_args)});")
                    writer.write('}')
                    writer.write('}')
                writer.write('')

        if PYTHON_IMPLS['_runtime']:
            for func_name in sorted(PYTHON_IMPLS['_runtime']):
                (helper_classes, helper_method) = translate_python_runtime_impl(func_name, pool)
                python_runtime_helper_classes.extend(helper_classes)
                python_runtime_helper_methods.append(helper_method)
            for code in python_runtime_helper_classes:
                for line in code:
                    writer.write(line)
                writer.write('')
            writer.write('final class PyRuntimePythonImpl {')
            for method in python_runtime_helper_methods:
                for line in method:
                    writer.write(line)
                writer.write('')
            writer.write('}')
            writer.write('')

        if any(PYTHON_IMPLS.get(name, set()) for name in ('dict', 'float', 'int', 'str')):
            for code in python_method_helper_classes:
                for line in code:
                    writer.write(line)
                writer.write('')
            writer.write('final class PyBuiltinMethodsPythonImpl {')
            for method in python_method_helper_methods:
                for line in method:
                    writer.write(line)
                writer.write('')
            writer.write('}')
            writer.write('')

        if PYTHON_IMPLS['builtins']:
            for func_name in sorted(PYTHON_IMPLS['builtins']):
                (helper_classes, helper_method) = translate_python_builtin_impl(func_name, pool)
                python_builtin_helper_classes.extend(helper_classes)
                python_builtin_helper_methods.append(helper_method)
            for code in python_builtin_helper_classes:
                for line in code:
                    writer.write(line)
                writer.write('')
            writer.write('final class PyBuiltinFunctionsPythonImpl {')
            for method in python_builtin_helper_methods:
                for line in method:
                    writer.write(line)
                writer.write('')
            writer.write('}')
            writer.write('')

        for func_name in sorted(BUILTIN_FUNCTIONS):
            params = get_builtin_function_params(func_name)
            if params is None or func_name in RAW_ARGS_KWARGS_BUILTINS:
                kwarg_params = None
            else:
                kwarg_params = analyze_params(params)
            writer.write(f'final class PyBuiltinFunction_{func_name} extends PyBuiltinFunction {{')
            writer.write(f'public static final PyBuiltinFunction_{func_name} singleton = new PyBuiltinFunction_{func_name}();')
            writer.write('')
            writer.write(f'private PyBuiltinFunction_{func_name}() {{ super("{func_name}"); }}')
            writer.write('@Override public PyObject call(PyObject[] args, PyDict kwargs) {')
            if kwarg_params is None:
                bind_args = ['args', 'kwargs']
            else:
                kw_name = 'sort' if func_name == 'sorted' else func_name
                kw_overflow_args_length = '0' if func_name == 'sorted' else 'argsLength'
                bind_args = emit_arg_binding(
                    writer, kwarg_params, func_name,
                    kw_name,
                    kw_overflow_args_length,
                    func_name,
                    pool,
                )
            if func_name in PYTHON_IMPLS['builtins']:
                writer.write(f'return PyBuiltinFunctionsPythonImpl.pyfunc_{func_name}({", ".join(bind_args)});')
            else:
                writer.write(f'return PyBuiltinFunctionsImpl.pyfunc_{func_name}({", ".join(bind_args)});')
            writer.write('}')
            writer.write('}')
            writer.write('')

        for line in pool.emit_pool():
            writer.write(line)

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
    gen_code('runtime/_out/spec.json', 'runtime/_out/PyGenerated.java')
    codegen_time = time.perf_counter() - start
    print(f'{codegen_time=:.3f}')

    start = time.perf_counter()
    if os.path.exists('tests/_out'):
        shutil.rmtree('tests/_out')
    subprocess.check_call(['javac', '-d', '_out', *RUNTIME_JAVA_FILES, '_out/PyGenerated.java'], cwd='runtime')
    os.makedirs('tests/_out', exist_ok=True)
    subprocess.check_call(['jar', '--create', '--file', 'tests/_out/pythonj.jar', '--date=1980-01-01T00:00:02Z', '-C', 'runtime/_out', '.'])
    initial_javac_time = time.perf_counter() - start
    print(f'initial_javac_time={initial_javac_time:.3f}')

    start = time.perf_counter()
    for py_name in py_names:
        py_path = f'tests/{py_name}.py'
        with open(py_path, encoding='utf-8') as f:
            node = ast.parse(f.read())
        analyzer = ScopeAnalyzer()
        analyzer.visit(node)
        visitor = PythonjVisitor(py_path, analyzer.scope_infos, analyzer.scope_infos[node])
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
