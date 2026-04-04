# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Iterator, Optional, TextIO

JAVA_FORBIDDEN_IDENTIFIERS = {
    '_', 'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const',
    'continue', 'default', 'do', 'double', 'else', 'enum', 'exports', 'extends', 'false', 'final', 'finally',
    'float', 'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'module',
    'native', 'new', 'null', 'open', 'opens', 'package', 'permits', 'private', 'protected', 'provides',
    'public', 'record', 'requires', 'return', 'sealed', 'short', 'static', 'strictfp', 'super', 'switch',
    'synchronized', 'this', 'throw', 'throws', 'to', 'transient', 'transitive', 'true', 'try', 'uses', 'var',
    'void', 'volatile', 'while', 'with', 'yield',
}

def _int_name(i: int) -> str:
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
def _java_string_literal(s: str) -> str:
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
            return self.qualify(_int_name(value))
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
            value = CreateObject('PyInt', [IntLiteral(i, 'L')])
            yield f'{field_prefix} PyInt {_int_name(i)} = {value.emit_java(self)};'
        for (k, v) in sorted(self.all_strings.items()):
            value = CreateObject('PyString', [StrLiteral(k)])
            yield f'{field_prefix} PyString str_singleton_{v} = {value.emit_java(self)};'
        for (k, v) in sorted(self.all_floats.items()):
            yield f'{field_prefix} PyFloat float_singleton_{v} = new PyFloat({k!r});'
        for (k, v) in sorted(self.all_tuples.items(), key=lambda x: x[1]):
            value = CreateObject('PyTuple', [CreateArray('PyObject', [PyConstant(x) for x in k])])
            yield f'{field_prefix} PyTuple tuple_singleton_{v} = {value.emit_java(self)};'
        for (k, v) in sorted(self.all_bytes.items()):
            value = CreateObject('PyBytes', [CreateArray('byte', [IntLiteral(((x + 0x80) & 0xFF) - 0x80, '') for x in k])])
            yield f'{field_prefix} PyBytes bytes_singleton_{v} = {value.emit_java(self)};'
        if self.holder_name is not None:
            yield '}'

class Expr(ABC):
    @abstractmethod
    def emit_java(self, pool: ConstantPool) -> str:
        raise NotImplementedError()

@dataclass(slots=True)
class IntLiteral(Expr):
    value: int
    suffix: str = ''
    def emit_java(self, pool: ConstantPool) -> str:
        return f'{self.value}{self.suffix}'

@dataclass(slots=True)
class StrLiteral(Expr):
    s: str
    def emit_java(self, pool: ConstantPool) -> str:
        return _java_string_literal(self.s)

@dataclass(slots=True)
class Identifier(Expr):
    name: str
    def __post_init__(self):
        assert self.name not in JAVA_FORBIDDEN_IDENTIFIERS, self.name
    def emit_java(self, pool: ConstantPool) -> str:
        return self.name

@dataclass(slots=True)
class Null(Expr):
    def emit_java(self, pool: ConstantPool) -> str:
        return 'null'

@dataclass(slots=True)
class This(Expr):
    def emit_java(self, pool: ConstantPool) -> str:
        return 'this'

@dataclass(slots=True)
class Bool(Expr):
    value: bool
    def emit_java(self, pool: ConstantPool) -> str:
        return 'true' if self.value else 'false'

@dataclass(slots=True)
class Field(Expr):
    obj: Expr
    field: str
    def emit_java(self, pool: ConstantPool) -> str:
        return f'{self.obj.emit_java(pool)}.{self.field}'

@dataclass(slots=True)
class ArrayAccess(Expr):
    obj: Expr
    index: Expr
    def emit_java(self, pool: ConstantPool) -> str:
        return f'{self.obj.emit_java(pool)}[{self.index.emit_java(pool)}]'

@dataclass(slots=True)
class CastExpr(Expr):
    type: str
    expr: Expr
    def emit_java(self, pool: ConstantPool) -> str:
        return f'(({self.type}){self.expr.emit_java(pool)})'

@dataclass(slots=True)
class UnaryOp(Expr):
    op: str
    operand: Expr
    def emit_java(self, pool: ConstantPool) -> str:
        return f'({self.op}{self.operand.emit_java(pool)})'

@dataclass(slots=True)
class BinaryOp(Expr):
    op: str
    lhs: Expr
    rhs: Expr
    def emit_java(self, pool: ConstantPool) -> str:
        return f'({self.lhs.emit_java(pool)} {self.op} {self.rhs.emit_java(pool)})'

@dataclass(slots=True)
class CondOp(Expr):
    cond: Expr
    true: Expr
    false: Expr
    def emit_java(self, pool: ConstantPool) -> str:
        return f'({self.cond.emit_java(pool)} ? {self.true.emit_java(pool)} : {self.false.emit_java(pool)})'

@dataclass(slots=True)
class CreateObject(Expr):
    type: str
    args: list[Expr]
    def emit_java(self, pool: ConstantPool) -> str:
        return f"new {self.type}({', '.join(arg.emit_java(pool) for arg in self.args)})"

@dataclass(slots=True)
class CreateArray(Expr):
    type: str
    elts: list[Expr]
    def emit_java(self, pool: ConstantPool) -> str:
        return f"new {self.type}[] {{{', '.join(x.emit_java(pool) for x in self.elts)}}}"

@dataclass(slots=True)
class MethodCall(Expr):
    obj: Expr
    method: str
    args: list[Expr]
    def emit_java(self, pool: ConstantPool) -> str:
        return f"{self.obj.emit_java(pool)}.{self.method}({', '.join(arg.emit_java(pool) for arg in self.args)})"

@dataclass(slots=True)
class AssignExpr(Expr):
    lhs: Expr
    rhs: Expr
    def emit_java(self, pool: ConstantPool) -> str:
        return f'({self.lhs.emit_java(pool)} = {self.rhs.emit_java(pool)})'

@dataclass(slots=True)
class PyConstant(Expr):
    value: object
    def emit_java(self, pool: ConstantPool) -> str:
        return pool.emit_constant(self.value)

class Statement(ABC):
    def ends_control_flow(self) -> bool:
        return False

    @abstractmethod
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        raise NotImplementedError()

@dataclass(slots=True)
class VariableDecl(Statement):
    type: str
    name: str
    value: Optional[Expr]
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        if self.value:
            yield f'{self.type} {self.name} = {self.value.emit_java(pool)};'
        else:
            yield f'{self.type} {self.name};'

@dataclass(slots=True)
class AssignStatement(Statement):
    lhs: Expr
    rhs: Expr
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'{self.lhs.emit_java(pool)} = {self.rhs.emit_java(pool)};'

@dataclass(slots=True)
class ExprStatement(Statement):
    call: CreateObject | MethodCall # only limited types of expressions allowed by Java grammar
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'{self.call.emit_java(pool)};'

@dataclass(slots=True)
class BreakStatement(Statement):
    name: Optional[str]
    def ends_control_flow(self) -> bool:
        return True
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'break {self.name};' if self.name else 'break;'

@dataclass(slots=True)
class ContinueStatement(Statement):
    def ends_control_flow(self) -> bool:
        return True
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield 'continue;'

@dataclass(slots=True)
class ReturnStatement(Statement):
    expr: Expr
    def ends_control_flow(self) -> bool:
        return True
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'return {self.expr.emit_java(pool)};'

@dataclass(slots=True)
class ThrowStatement(Statement):
    expr: Expr
    def ends_control_flow(self) -> bool:
        return True
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'throw {self.expr.emit_java(pool)};'

def block_simplify(block: list[Statement]) -> list[Statement]:
    ret = []
    for s in block:
        ret.append(s)
        if s.ends_control_flow():
            break
    return ret

def block_emit_java(block: list[Statement], pool: ConstantPool) -> Iterator[str]:
    for s in block:
        yield from s.emit_java(pool)

def block_ends_control_flow(block: list[Statement]) -> bool:
    return bool(block) and block[-1].ends_control_flow()

@dataclass(slots=True)
class IfStatement(Statement):
    cond: Expr
    body: list[Statement]
    orelse: list[Statement]

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
class WhileStatement(Statement):
    cond: Expr
    body: list[Statement]

    def __post_init__(self):
        self.body = block_simplify(self.body)

    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'while ({self.cond.emit_java(pool)}) {{'
        yield from block_emit_java(self.body, pool)
        yield '}'

# simplified; init/incr are weird because of semicolons/parens if we try to map them to statement or expr
@dataclass(slots=True)
class ForStatement(Statement):
    init_type: str
    init_name: str
    init_value: Expr
    cond: Expr
    incr_name: str
    incr_value: Expr
    body: list[Statement]

    def __post_init__(self):
        self.body = block_simplify(self.body)

    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'for ({self.init_type} {self.init_name} = {self.init_value.emit_java(pool)}; {self.cond.emit_java(pool)}; {self.incr_name} = {self.incr_value.emit_java(pool)}) {{'
        yield from block_emit_java(self.body, pool)
        yield '}'

@dataclass(slots=True)
class ForEachStatement(Statement):
    var_type: str
    var_name: str
    iterable: Expr
    body: list[Statement]

    def __post_init__(self):
        self.body = block_simplify(self.body)

    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'for ({self.var_type} {self.var_name}: {self.iterable.emit_java(pool)}) {{'
        yield from block_emit_java(self.body, pool)
        yield '}'

@dataclass(slots=True)
class TryStatement(Statement):
    try_body: list[Statement]
    exc_type: Optional[str]
    exc_name: Optional[str]
    catch_body: list[Statement]
    finally_body: list[Statement]

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
class LabeledBlock(Statement):
    name: str
    body: list[Statement]

    def __post_init__(self):
        self.body = block_simplify(self.body)

    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'{self.name}: {{'
        yield from block_emit_java(self.body, pool)
        yield '}'

def unary_op(op: str, operand: Expr) -> Expr:
    if op == '!' and isinstance(operand, Bool):
        return Bool(not operand.value)
    return UnaryOp(op, operand)

def bool_value(expr: Expr) -> Expr:
    if (isinstance(expr, MethodCall) and isinstance(expr.obj, Identifier) and
        expr.obj.name == 'PyBool' and expr.method == 'create' and len(expr.args) == 1):
        return expr.args[0] # return the raw boolean instead of box/unbox
    if isinstance(expr, PyConstant):
        return Bool(bool(expr.value))
    return MethodCall(expr, 'boolValue', [])

def chained_binary_op(op: str, exprs: list[Expr]) -> Expr:
    assert len(exprs) >= 1, exprs
    expr = exprs[0]
    for term in exprs[1:]:
        expr = BinaryOp(op, expr, term)
    return expr

def if_statement(cond: Expr, body: list[Statement], orelse: list[Statement]) -> Iterator[Statement]:
    if isinstance(cond, Bool) and cond.value:
        yield from body
    elif isinstance(cond, Bool) and not cond.value:
        yield from orelse
    else:
        yield IfStatement(cond, body, orelse)

def if_chain(conditions_and_bodies: list[tuple[Expr, list[Statement]]],
             else_body: list[Statement]) -> IfStatement:
    assert conditions_and_bodies, conditions_and_bodies
    orelse: list[Statement] = else_body
    for (cond, body) in reversed(conditions_and_bodies[1:]):
        orelse = [IfStatement(cond, body, orelse)]
    return IfStatement(conditions_and_bodies[0][0], conditions_and_bodies[0][1], orelse)

def while_statement(cond: Expr, body: list[Statement]) -> Iterator[Statement]:
    if isinstance(cond, Bool) and not cond.value:
        pass
    else:
        yield WhileStatement(cond, body)

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

def emit_java_statements(writer: IndentedWriter, statements: list[Statement], pool: ConstantPool) -> None:
    for line in block_emit_java(statements, pool):
        writer.write(line)

def emit_java_statement(writer: IndentedWriter, statement: Statement, pool: ConstantPool) -> None:
    emit_java_statements(writer, [statement], pool)
