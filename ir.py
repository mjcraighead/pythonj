# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

from abc import ABC, abstractmethod
from dataclasses import dataclass
import math
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
JAVA_TYPE_UNKNOWN = 'unknown'
STATIC_METHOD_RETURN_TYPES = {
    ('PyBool', 'create'): 'PyBool',
    ('PyBuiltinFunctionsImpl', 'pyfunc_ascii'): 'PyString',
    ('PyInt', 'add'): 'PyInt',
    ('PyInt', 'and'): 'PyInt',
    ('PyInt', 'floorDiv'): 'PyInt',
    ('PyInt', 'lshift'): 'PyInt',
    ('PyInt', 'mod'): 'PyInt',
    ('PyInt', 'mul'): 'PyInt',
    ('PyInt', 'or'): 'PyInt',
    ('PyInt', 'pow'): 'PyInt',
    ('PyInt', 'rshift'): 'PyInt',
    ('PyInt', 'sub'): 'PyInt',
    ('PyInt', 'trueDiv'): 'PyFloat',
    ('PyInt', 'xor'): 'PyInt',
    ('Runtime', 'pythonjAbs'): 'PyObject',
    ('Runtime', 'pythonjBytesBuilder'): 'PyBytesBuilder',
    ('Runtime', 'pythonjBytesBuilderAppend'): 'PyNone',
    ('Runtime', 'pythonjBytesBuilderAppendByte'): 'PyNone',
    ('Runtime', 'pythonjBytesBuilderFinish'): 'PyBytes',
    ('Runtime', 'pythonjDelAttr'): 'PyNone',
    ('Runtime', 'pythonjDictGet'): 'PyObject',
    ('Runtime', 'pythonjFormat'): 'PyString',
    ('Runtime', 'pythonjHash'): 'PyInt',
    ('Runtime', 'pythonjGetAttr'): 'PyObject',
    ('Runtime', 'pythonjIsInstance'): 'PyBool',
    ('Runtime', 'pythonjIsSubclass'): 'PyBool',
    ('Runtime', 'pythonjIter'): 'PyIter',
    ('Runtime', 'pythonjLen'): 'PyInt',
    ('Runtime', 'pythonjNext'): 'PyObject',
    ('Runtime', 'pythonjRepr'): 'PyString',
    ('Runtime', 'pythonjSetAttr'): 'PyNone',
    ('Runtime', 'pythonjStrBuilder'): 'PyStringBuilder',
    ('Runtime', 'pythonjStrBuilderAppend'): 'PyNone',
    ('Runtime', 'pythonjStrBuilderFinish'): 'PyString',
    ('Runtime', 'pythonjZipNew'): 'PyObject',
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
            if 0x20 <= o <= 0x7E: # safe ASCII
                out.append(c)
            elif o <= 0xFFFF:
                out.append(f'\\u{o:04x}')
            else:
                o -= 0x10000
                high = 0xD800 | (o >> 10)
                low = 0xDC00 | (o & 0x3FF)
                out.append(f'\\u{high:04x}\\u{low:04x}')
    out.append('"')
    return ''.join(out)

class ConstantPool:
    __slots__ = ('all_ints', 'all_strings', 'all_floats', 'all_tuples', 'all_bytes', 'owner_name')
    all_ints: set[int]
    all_strings: dict[str, int]
    all_floats: dict[str, tuple[float, int]]
    all_tuples: dict[tuple[object, ...], int]
    all_bytes: dict[bytes, int]
    owner_name: Optional[str]

    def __init__(self, owner_name: Optional[str] = None):
        self.all_ints = set()
        self.all_strings = {}
        self.all_floats = {}
        self.all_tuples = {}
        self.all_bytes = {}
        self.owner_name = owner_name

    def qualify_name(self, name: str) -> str:
        return name if self.owner_name is None else f'{self.owner_name}.{name}'

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
            return self.qualify_name(_int_name(value))
        elif isinstance(value, str):
            if not value:
                return 'PyString.empty_singleton'
            if value not in self.all_strings:
                self.all_strings[value] = len(self.all_strings)
            return self.qualify_name(f'str_singleton_{self.all_strings[value]}')
        elif isinstance(value, float):
            if math.isnan(value):
                return 'PyFloat.nan_singleton'
            key = value.hex()
            if key not in self.all_floats:
                self.all_floats[key] = (value, len(self.all_floats))
            return self.qualify_name(f'float_singleton_{self.all_floats[key][1]}')
        elif isinstance(value, tuple):
            if not value:
                return 'PyTuple.empty_singleton'
            if value not in self.all_tuples:
                for x in value:
                    self.emit_constant(x)
                self.all_tuples[value] = len(self.all_tuples)
            return self.qualify_name(f'tuple_singleton_{self.all_tuples[value]}')
        else:
            assert isinstance(value, bytes), value
            if not value:
                return 'PyBytes.empty_singleton'
            if value not in self.all_bytes:
                self.all_bytes[value] = len(self.all_bytes)
            return self.qualify_name(f'bytes_singleton_{self.all_bytes[value]}')

    def build_field_decls(self) -> list[Decl]:
        decls: list[Decl] = []
        field_prefix = 'private static final' if self.owner_name is None else 'static final'
        for i in sorted(self.all_ints):
            value = CreateObject('PyInt', [IntLiteral(i, 'L')])
            decls.append(FieldDecl(field_prefix, 'PyInt', _int_name(i), value))
        for (k, v) in sorted(self.all_strings.items()):
            value = CreateObject('PyString', [StrLiteral(k)])
            decls.append(FieldDecl(field_prefix, 'PyString', f'str_singleton_{v}', value))
        for (_, (k, v)) in sorted(self.all_floats.items(), key=lambda item: item[1][1]):
            decls.append(FieldDecl(field_prefix, 'PyFloat', f'float_singleton_{v}', CreateObject('PyFloat', [FloatLiteral(k)])))
        for (k, v) in sorted(self.all_tuples.items(), key=lambda x: x[1]):
            value = CreateObject('PyTuple', [CreateArray('PyObject', [PyConstant(x) for x in k])])
            decls.append(FieldDecl(field_prefix, 'PyTuple', f'tuple_singleton_{v}', value))
        for (k, v) in sorted(self.all_bytes.items()):
            value = CreateObject('PyBytes', [CreateArray('byte', [IntLiteral(((x + 0x80) & 0xFF) - 0x80, '') for x in k])])
            decls.append(FieldDecl(field_prefix, 'PyBytes', f'bytes_singleton_{v}', value))
        return decls

def with_pooled_fields(class_decl: ClassDecl, pool: ConstantPool) -> ClassDecl:
    for _ in class_decl.emit_java(pool):
        pass
    return ClassDecl(class_decl.modifiers, class_decl.name, class_decl.extends, [*pool.build_field_decls(), *class_decl.decls])

class Expr(ABC):
    def java_type(self) -> str:
        return JAVA_TYPE_UNKNOWN

    @abstractmethod
    def emit_java(self, pool: ConstantPool) -> str:
        raise NotImplementedError()

@dataclass(slots=True)
class IntLiteral(Expr):
    value: int
    suffix: str = ''
    def java_type(self) -> str:
        return 'long' if self.suffix == 'L' else 'int'
    def emit_java(self, pool: ConstantPool) -> str:
        return f'{self.value}{self.suffix}'

@dataclass(slots=True)
class FloatLiteral(Expr):
    value: float
    def java_type(self) -> str:
        return 'double'
    def emit_java(self, pool: ConstantPool) -> str:
        return f'{self.value!r}'

@dataclass(slots=True)
class StrLiteral(Expr):
    s: str
    def java_type(self) -> str:
        return 'String'
    def emit_java(self, pool: ConstantPool) -> str:
        return _java_string_literal(self.s)

@dataclass(slots=True)
class Identifier(Expr):
    name: str
    java_type_hint: str = JAVA_TYPE_UNKNOWN
    def __post_init__(self):
        assert self.name not in JAVA_FORBIDDEN_IDENTIFIERS, self.name
    def java_type(self) -> str:
        return self.java_type_hint
    def emit_java(self, pool: ConstantPool) -> str:
        return self.name

@dataclass(slots=True)
class PyBuiltinFunction(Expr):
    name: str
    java_name: Optional[str] = None
    def java_type(self) -> str:
        return self.java_name if self.java_name is not None else f'PyBuiltinFunction_{self.name}'
    def emit_java(self, pool: ConstantPool) -> str:
        java_name = self.java_name if self.java_name is not None else f'PyBuiltinFunction_{self.name}'
        return f'{java_name}.singleton'

@dataclass(slots=True)
class PyBuiltinType(Expr):
    name: str
    java_name: str
    def java_type(self) -> str:
        return f'{self.java_name}Type'
    def emit_java(self, pool: ConstantPool) -> str:
        return f'{self.java_name}Type.singleton'

@dataclass(slots=True)
class PyBuiltinModule(Expr):
    name: str
    java_name: str
    def java_type(self) -> str:
        return self.java_name
    def emit_java(self, pool: ConstantPool) -> str:
        return f'{self.java_name}.singleton'

@dataclass(slots=True)
class Null(Expr):
    def java_type(self) -> str:
        return 'null'
    def emit_java(self, pool: ConstantPool) -> str:
        return 'null'

@dataclass(slots=True)
class This(Expr):
    def emit_java(self, pool: ConstantPool) -> str:
        return 'this'

@dataclass(slots=True)
class Super(Expr):
    def emit_java(self, pool: ConstantPool) -> str:
        return 'super'

@dataclass(slots=True)
class Bool(Expr):
    value: bool
    def java_type(self) -> str:
        return 'boolean'
    def emit_java(self, pool: ConstantPool) -> str:
        return 'true' if self.value else 'false'

@dataclass(slots=True)
class Field(Expr):
    obj: Expr
    field: str
    field_type: str = JAVA_TYPE_UNKNOWN
    def java_type(self) -> str:
        return self.field_type
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
    def java_type(self) -> str:
        return self.type
    def emit_java(self, pool: ConstantPool) -> str:
        if self.expr.java_type() == self.type:
            return self.expr.emit_java(pool)
        return f'(({self.type}){self.expr.emit_java(pool)})'

@dataclass(slots=True)
class UnaryOp(Expr):
    op: str
    operand: Expr
    def java_type(self) -> str:
        if self.op == '!':
            return 'boolean'
        return JAVA_TYPE_UNKNOWN
    def emit_java(self, pool: ConstantPool) -> str:
        return f'({self.op}{self.operand.emit_java(pool)})'

@dataclass(slots=True)
class BinaryOp(Expr):
    op: str
    lhs: Expr
    rhs: Expr
    def java_type(self) -> str:
        if self.op in {'==', '!=', '<', '<=', '>', '>=', '&&', '||', 'instanceof'}:
            return 'boolean'
        return JAVA_TYPE_UNKNOWN
    def emit_java(self, pool: ConstantPool) -> str:
        return f'({self.lhs.emit_java(pool)} {self.op} {self.rhs.emit_java(pool)})'

@dataclass(slots=True)
class CondOp(Expr):
    cond: Expr
    true: Expr
    false: Expr
    def java_type(self) -> str:
        true_type = self.true.java_type()
        false_type = self.false.java_type()
        if true_type == false_type:
            return true_type
        return JAVA_TYPE_UNKNOWN
    def emit_java(self, pool: ConstantPool) -> str:
        return f'({self.cond.emit_java(pool)} ? {self.true.emit_java(pool)} : {self.false.emit_java(pool)})'

@dataclass(slots=True)
class CreateObject(Expr):
    type: str
    args: list[Expr]
    def java_type(self) -> str:
        return self.type
    def emit_java(self, pool: ConstantPool) -> str:
        return f"new {self.type}({', '.join(arg.emit_java(pool) for arg in self.args)})"

@dataclass(slots=True)
class CreateArray(Expr):
    type: str
    elts: list[Expr]
    def java_type(self) -> str:
        return f'{self.type}[]'
    def emit_java(self, pool: ConstantPool) -> str:
        return f"new {self.type}[] {{{', '.join(x.emit_java(pool) for x in self.elts)}}}"

@dataclass(slots=True)
class MethodCall(Expr):
    obj: Expr
    method: str
    args: list[Expr]
    return_type: str = JAVA_TYPE_UNKNOWN
    def java_type(self) -> str:
        return self.return_type
    def emit_java(self, pool: ConstantPool) -> str:
        return f"{self.obj.emit_java(pool)}.{self.method}({', '.join(arg.emit_java(pool) for arg in self.args)})"

@dataclass(slots=True)
class StaticMethodCall(Expr):
    class_name: str
    method: str
    args: list[Expr]
    return_type: str = JAVA_TYPE_UNKNOWN
    def java_type(self) -> str:
        return self.return_type
    def emit_java(self, pool: ConstantPool) -> str:
        return f"{self.class_name}.{self.method}({', '.join(arg.emit_java(pool) for arg in self.args)})"

@dataclass(slots=True)
class MethodRef(Expr):
    obj: str
    method: str
    def emit_java(self, pool: ConstantPool) -> str:
        return f'{self.obj}::{self.method}'

@dataclass(slots=True)
class AssignExpr(Expr):
    lhs: Expr
    rhs: Expr
    def java_type(self) -> str:
        lhs_type = self.lhs.java_type()
        if lhs_type != JAVA_TYPE_UNKNOWN:
            return lhs_type
        return self.rhs.java_type()
    def emit_java(self, pool: ConstantPool) -> str:
        return f'({self.lhs.emit_java(pool)} = {self.rhs.emit_java(pool)})'

@dataclass(slots=True)
class PyConstant(Expr):
    value: object
    def java_type(self) -> str:
        if self.value is None:
            return 'PyNone'
        elif self.value is False or self.value is True:
            return 'PyBool'
        elif isinstance(self.value, int):
            return 'PyInt'
        elif isinstance(self.value, str):
            return 'PyString'
        elif isinstance(self.value, float):
            return 'PyFloat'
        elif isinstance(self.value, tuple):
            return 'PyTuple'
        else:
            assert isinstance(self.value, bytes), self.value
            return 'PyBytes'
    def emit_java(self, pool: ConstantPool) -> str:
        return pool.emit_constant(self.value)

class Statement(ABC):
    def ends_control_flow(self) -> bool:
        return False

    @abstractmethod
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        raise NotImplementedError()

@dataclass(slots=True)
class LocalDecl(Statement):
    type: str
    name: str
    value: Optional[Expr]
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        if self.value is not None:
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
    call: CreateObject | MethodCall | StaticMethodCall # only limited types of expressions allowed by Java grammar
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'{self.call.emit_java(pool)};'

@dataclass(slots=True)
class SuperConstructorCall(Statement):
    args: list[Expr]
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f"super({', '.join(arg.emit_java(pool) for arg in self.args)});"

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
    expr: Optional[Expr] = None
    def ends_control_flow(self) -> bool:
        return True
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        if self.expr is None:
            yield 'return;'
        else:
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
        node = self
        prefix = ''
        while True:
            yield f'{prefix}if ({node.cond.emit_java(pool)}) {{'
            yield from block_emit_java(node.body, pool)
            if node.orelse:
                if len(node.orelse) == 1 and isinstance(node.orelse[0], IfStatement):
                    node = node.orelse[0]
                    prefix = '} else '
                    continue
                yield '} else {'
                yield from block_emit_java(node.orelse, pool)
            break
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

@dataclass(slots=True)
class SwitchCase:
    expr: Expr
    value: Expr

@dataclass(slots=True)
class SwitchStatement(Statement):
    expr: Expr
    cases: list[SwitchCase]
    default: Expr

    def __post_init__(self):
        assert self.cases, self.cases

    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'switch ({self.expr.emit_java(pool)}) {{'
        for case in self.cases:
            yield f'case {case.expr.emit_java(pool)}: return {case.value.emit_java(pool)};'
        yield f'default: return {self.default.emit_java(pool)};'
        yield '}'

@dataclass(slots=True)
class SwitchVoidStatement(Statement):
    expr: Expr
    cases: list[SwitchCase]
    default: Expr

    def __post_init__(self):
        assert self.cases, self.cases

    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'switch ({self.expr.emit_java(pool)}) {{'
        for case in self.cases:
            yield f'case {case.expr.emit_java(pool)}:'
            yield f'{case.value.emit_java(pool)};'
            yield 'return;'
        yield 'default:'
        yield f'{self.default.emit_java(pool)};'
        yield 'return;'
        yield '}'

class Decl(ABC):
    @abstractmethod
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        raise NotImplementedError()

@dataclass(slots=True)
class FieldDecl(Decl):
    modifiers: str
    type: str
    name: str
    value: Optional[Expr]
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        if self.value is not None:
            yield f'{self.modifiers} {self.type} {self.name} = {self.value.emit_java(pool)};'
        else:
            yield f'{self.modifiers} {self.type} {self.name};'

@dataclass(slots=True)
class MethodDecl(Decl):
    modifiers: str
    return_type: str
    name: str
    args: list[str]
    body: list[Statement]
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f"{self.modifiers} {self.return_type} {self.name}({', '.join(self.args)}) {{"
        yield from block_emit_java(block_simplify(self.body), pool)
        yield '}'

@dataclass(slots=True)
class ConstructorDecl(Decl):
    modifiers: str
    name: str
    args: list[str]
    body: list[Statement]
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        if self.modifiers:
            yield f"{self.modifiers} {self.name}({', '.join(self.args)}) {{"
        else:
            yield f"{self.name}({', '.join(self.args)}) {{"
        yield from block_emit_java(block_simplify(self.body), pool)
        yield '}'

@dataclass(slots=True)
class StaticBlock(Decl):
    body: list[Statement]
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield 'static {'
        yield from block_emit_java(block_simplify(self.body), pool)
        yield '}'

@dataclass(slots=True)
class ClassDecl(Decl):
    modifiers: str
    name: str
    extends: Optional[str]
    decls: list[Decl]
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        extends = f' extends {self.extends}' if self.extends else ''
        yield f'{self.modifiers} class {self.name}{extends} {{'
        for decl in self.decls:
            yield from decl.emit_java(pool)
        yield '}'

def unary_op(op: str, operand: Expr) -> Expr:
    if op == '!' and isinstance(operand, Bool):
        return Bool(not operand.value)
    return UnaryOp(op, operand)

def bool_value(expr: Expr) -> Expr:
    if isinstance(expr, StaticMethodCall) and expr.class_name == 'PyBool' and expr.method == 'create':
        return expr.args[0] # return the raw boolean instead of box/unbox
    if isinstance(expr, PyConstant):
        return Bool(bool(expr.value))
    return MethodCall(expr, 'boolValue', [], 'boolean')

def unbox_int(expr: Expr) -> Expr:
    if isinstance(expr, PyConstant) and isinstance(expr.value, int) and not isinstance(expr.value, bool):
        return IntLiteral(expr.value, 'L')
    if isinstance(expr, CreateObject) and expr.type == 'PyInt':
        return expr.args[0]
    if expr.java_type() == 'PyInt':
        return Field(expr, 'value', 'long')
    return Field(CastExpr('PyInt', expr), 'value', 'long')

def unbox_str(expr: Expr) -> Expr:
    if isinstance(expr, PyConstant) and isinstance(expr.value, str):
        return StrLiteral(expr.value)
    if isinstance(expr, CreateObject) and expr.type == 'PyString':
        return expr.args[0]
    if expr.java_type() == 'PyString':
        return Field(expr, 'value', 'String')
    return Field(CastExpr('PyString', expr), 'value', 'String')

def static_method_call(class_name: str, method: str, args: list[Expr]) -> Expr:
    if class_name == 'PyBool' and method == 'create':
        (arg,) = args
        if isinstance(arg, Bool):
            return PyConstant(arg.value)
    return StaticMethodCall(class_name, method, args, STATIC_METHOD_RETURN_TYPES.get((class_name, method), JAVA_TYPE_UNKNOWN))

def chained_binary_op(op: str, exprs: list[Expr]) -> Expr:
    assert len(exprs) >= 1, exprs
    expr = exprs[0]
    for term in exprs[1:]:
        expr = BinaryOp(op, expr, term)
    return expr

def method_call_statement(obj: Expr, method: str, args: list[Expr]) -> ExprStatement:
    return ExprStatement(MethodCall(obj, method, args))

def static_method_call_statement(class_name: str, method: str, args: list[Expr]) -> ExprStatement:
    return ExprStatement(static_method_call(class_name, method, args))

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

def write_decls(f: TextIO, decls: list[Decl], pool: ConstantPool) -> None:
    for decl in decls:
        for _ in decl.emit_java(pool):
            pass
    indent = 0
    for decl in decls:
        for line in decl.emit_java(pool):
            if line.startswith('}'):
                indent -= 1
            f.write('    ' * indent)
            f.write(line)
            f.write('\n')
            if line.endswith('{'):
                indent += 1
    assert indent == 0, indent
