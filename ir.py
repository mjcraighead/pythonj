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
    ('Long', 'toString'): 'String',
    ('PyBool', 'create'): 'PyBool',
    ('PyBuiltinFunctionsImpl', 'pyfunc_ascii'): 'PyString',
    ('PyBytesBuilder', 'newUnboxed'): 'java.io.ByteArrayOutputStream',
    ('PyFloat', 'addUnboxed'): 'double',
    ('PyFloat', 'mulUnboxed'): 'double',
    ('PyFloat', 'subUnboxed'): 'double',
    ('PyInt', 'addUnboxed'): 'long',
    ('PyInt', 'andUnboxed'): 'long',
    ('PyInt', 'floorDivUnboxed'): 'long',
    ('PyInt', 'lshiftUnboxed'): 'long',
    ('PyInt', 'modUnboxed'): 'long',
    ('PyInt', 'mulUnboxed'): 'long',
    ('PyInt', 'orUnboxed'): 'long',
    ('PyInt', 'pow'): 'PyObject',
    ('PyInt', 'rshiftUnboxed'): 'long',
    ('PyInt', 'subUnboxed'): 'long',
    ('PyInt', 'trueDivUnboxed'): 'double',
    ('PyInt', 'xorUnboxed'): 'long',
    ('PyString', 'reprOf'): 'String',
    ('PyStringBuilder', 'newUnboxed'): 'StringBuilder',
    ('PyType', 'newObjPositional'): 'PyType',
    ('PyZip', 'newObjPositional'): 'PyZip',
    ('Runtime', 'pythonjIsInstance'): 'PyBool',
    ('Runtime', 'pythonjIsSubclass'): 'PyBool',
    ('Runtime', 'pythonjUnsupported'): 'void',
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

    def visit_children(self, visitor: IRVisitor) -> None:
        pass

    def transform_children(self, transformer: IRTransformer) -> Expr:
        return self

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
    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.obj)
    def transform_children(self, transformer: IRTransformer) -> Expr:
        self.obj = transformer.transform_expr(self.obj)
        return self
    def emit_java(self, pool: ConstantPool) -> str:
        return f'{self.obj.emit_java(pool)}.{self.field}'

@dataclass(slots=True)
class ArrayAccess(Expr):
    obj: Expr
    index: Expr
    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.obj)
        visitor.visit_expr(self.index)
    def transform_children(self, transformer: IRTransformer) -> Expr:
        self.obj = transformer.transform_expr(self.obj)
        self.index = transformer.transform_expr(self.index)
        return self
    def emit_java(self, pool: ConstantPool) -> str:
        return f'{self.obj.emit_java(pool)}[{self.index.emit_java(pool)}]'

@dataclass(slots=True)
class CastExpr(Expr):
    type: str
    expr: Expr
    def java_type(self) -> str:
        return self.type
    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.expr)
    def transform_children(self, transformer: IRTransformer) -> Expr:
        self.expr = transformer.transform_expr(self.expr)
        return self
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
    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.operand)
    def transform_children(self, transformer: IRTransformer) -> Expr:
        self.operand = transformer.transform_expr(self.operand)
        return self
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
    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.lhs)
        visitor.visit_expr(self.rhs)
    def transform_children(self, transformer: IRTransformer) -> Expr:
        self.lhs = transformer.transform_expr(self.lhs)
        self.rhs = transformer.transform_expr(self.rhs)
        return self
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
    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.cond)
        visitor.visit_expr(self.true)
        visitor.visit_expr(self.false)
    def transform_children(self, transformer: IRTransformer) -> Expr:
        self.cond = transformer.transform_expr(self.cond)
        self.true = transformer.transform_expr(self.true)
        self.false = transformer.transform_expr(self.false)
        return self
    def emit_java(self, pool: ConstantPool) -> str:
        return f'({self.cond.emit_java(pool)} ? {self.true.emit_java(pool)} : {self.false.emit_java(pool)})'

@dataclass(slots=True)
class CreateObject(Expr):
    type: str
    args: list[Expr]
    def java_type(self) -> str:
        return self.type
    def visit_children(self, visitor: IRVisitor) -> None:
        for arg in self.args:
            visitor.visit_expr(arg)
    def transform_children(self, transformer: IRTransformer) -> Expr:
        self.args = [transformer.transform_expr(arg) for arg in self.args]
        return self
    def emit_java(self, pool: ConstantPool) -> str:
        return f"new {self.type}({', '.join(arg.emit_java(pool) for arg in self.args)})"

@dataclass(slots=True)
class CreateArray(Expr):
    type: str
    elts: list[Expr]
    def java_type(self) -> str:
        return f'{self.type}[]'
    def visit_children(self, visitor: IRVisitor) -> None:
        for elt in self.elts:
            visitor.visit_expr(elt)
    def transform_children(self, transformer: IRTransformer) -> Expr:
        self.elts = [transformer.transform_expr(elt) for elt in self.elts]
        return self
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
    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.obj)
        for arg in self.args:
            visitor.visit_expr(arg)
    def transform_children(self, transformer: IRTransformer) -> Expr:
        self.obj = transformer.transform_expr(self.obj)
        self.args = [transformer.transform_expr(arg) for arg in self.args]
        return self
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
    def visit_children(self, visitor: IRVisitor) -> None:
        for arg in self.args:
            visitor.visit_expr(arg)
    def transform_children(self, transformer: IRTransformer) -> Expr:
        self.args = [transformer.transform_expr(arg) for arg in self.args]
        return self
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
    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.lhs)
        visitor.visit_expr(self.rhs)
    def transform_children(self, transformer: IRTransformer) -> Expr:
        self.lhs = transformer.transform_expr(self.lhs)
        self.rhs = transformer.transform_expr(self.rhs)
        return self
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

    def visit_children(self, visitor: IRVisitor) -> None:
        pass

    def transform_children(self, transformer: IRTransformer) -> Statement:
        return self

    @abstractmethod
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        raise NotImplementedError()

@dataclass(slots=True)
class LocalDecl(Statement):
    type: str
    name: str
    value: Optional[Expr]
    def visit_children(self, visitor: IRVisitor) -> None:
        if self.value is not None:
            visitor.visit_expr(self.value)
    def transform_children(self, transformer: IRTransformer) -> Statement:
        if self.value is not None:
            self.value = transformer.transform_expr(self.value)
        return self
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        if self.value is not None:
            yield f'{self.type} {self.name} = {self.value.emit_java(pool)};'
        else:
            yield f'{self.type} {self.name};'

@dataclass(slots=True)
class AssignStatement(Statement):
    lhs: Expr
    rhs: Expr
    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.lhs)
        visitor.visit_expr(self.rhs)
    def transform_children(self, transformer: IRTransformer) -> Statement:
        self.lhs = transformer.transform_expr(self.lhs)
        self.rhs = transformer.transform_expr(self.rhs)
        return self
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'{self.lhs.emit_java(pool)} = {self.rhs.emit_java(pool)};'

@dataclass(slots=True)
class ExprStatement(Statement):
    call: CreateObject | MethodCall | StaticMethodCall # only limited types of expressions allowed by Java grammar
    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.call)
    def transform_children(self, transformer: IRTransformer) -> Statement:
        self.call = transformer.transform_expr(self.call)
        return self
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        yield f'{self.call.emit_java(pool)};'

@dataclass(slots=True)
class SuperConstructorCall(Statement):
    args: list[Expr]
    def visit_children(self, visitor: IRVisitor) -> None:
        for arg in self.args:
            visitor.visit_expr(arg)
    def transform_children(self, transformer: IRTransformer) -> Statement:
        self.args = [transformer.transform_expr(arg) for arg in self.args]
        return self
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
    def visit_children(self, visitor: IRVisitor) -> None:
        if self.expr is not None:
            visitor.visit_expr(self.expr)
    def transform_children(self, transformer: IRTransformer) -> Statement:
        if self.expr is not None:
            self.expr = transformer.transform_expr(self.expr)
        return self
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
    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.expr)
    def transform_children(self, transformer: IRTransformer) -> Statement:
        self.expr = transformer.transform_expr(self.expr)
        return self
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

    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.cond)
        for s in self.body:
            visitor.visit_stmt(s)
        for s in self.orelse:
            visitor.visit_stmt(s)

    def transform_children(self, transformer: IRTransformer) -> Statement:
        self.cond = transformer.transform_expr(self.cond)
        self.body = [transformer.transform_stmt(s) for s in self.body]
        self.orelse = [transformer.transform_stmt(s) for s in self.orelse]
        return self

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

    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.cond)
        for s in self.body:
            visitor.visit_stmt(s)

    def transform_children(self, transformer: IRTransformer) -> Statement:
        self.cond = transformer.transform_expr(self.cond)
        self.body = [transformer.transform_stmt(s) for s in self.body]
        return self

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

    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.init_value)
        visitor.visit_expr(self.cond)
        visitor.visit_expr(self.incr_value)
        for s in self.body:
            visitor.visit_stmt(s)

    def transform_children(self, transformer: IRTransformer) -> Statement:
        self.init_value = transformer.transform_expr(self.init_value)
        self.cond = transformer.transform_expr(self.cond)
        self.incr_value = transformer.transform_expr(self.incr_value)
        self.body = [transformer.transform_stmt(s) for s in self.body]
        return self

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

    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.iterable)
        for s in self.body:
            visitor.visit_stmt(s)

    def transform_children(self, transformer: IRTransformer) -> Statement:
        self.iterable = transformer.transform_expr(self.iterable)
        self.body = [transformer.transform_stmt(s) for s in self.body]
        return self

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

    def visit_children(self, visitor: IRVisitor) -> None:
        for s in self.try_body:
            visitor.visit_stmt(s)
        for s in self.catch_body:
            visitor.visit_stmt(s)
        for s in self.finally_body:
            visitor.visit_stmt(s)

    def transform_children(self, transformer: IRTransformer) -> Statement:
        self.try_body = [transformer.transform_stmt(s) for s in self.try_body]
        self.catch_body = [transformer.transform_stmt(s) for s in self.catch_body]
        self.finally_body = [transformer.transform_stmt(s) for s in self.finally_body]
        return self

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

    def visit_children(self, visitor: IRVisitor) -> None:
        for s in self.body:
            visitor.visit_stmt(s)

    def transform_children(self, transformer: IRTransformer) -> Statement:
        self.body = [transformer.transform_stmt(s) for s in self.body]
        return self

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

    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.expr)
        for case in self.cases:
            visitor.visit_expr(case.expr)
            visitor.visit_expr(case.value)
        visitor.visit_expr(self.default)

    def transform_children(self, transformer: IRTransformer) -> Statement:
        self.expr = transformer.transform_expr(self.expr)
        self.cases = [SwitchCase(transformer.transform_expr(case.expr), transformer.transform_expr(case.value)) for case in self.cases]
        self.default = transformer.transform_expr(self.default)
        return self

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

    def visit_children(self, visitor: IRVisitor) -> None:
        visitor.visit_expr(self.expr)
        for case in self.cases:
            visitor.visit_expr(case.expr)
            visitor.visit_expr(case.value)
        visitor.visit_expr(self.default)

    def transform_children(self, transformer: IRTransformer) -> Statement:
        self.expr = transformer.transform_expr(self.expr)
        self.cases = [SwitchCase(transformer.transform_expr(case.expr), transformer.transform_expr(case.value)) for case in self.cases]
        self.default = transformer.transform_expr(self.default)
        return self

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
    def visit_children(self, visitor: IRVisitor) -> None:
        pass

    def transform_children(self, transformer: IRTransformer) -> Decl:
        return self

    @abstractmethod
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        raise NotImplementedError()

@dataclass(slots=True)
class FieldDecl(Decl):
    modifiers: str
    type: str
    name: str
    value: Optional[Expr]
    def visit_children(self, visitor: IRVisitor) -> None:
        if self.value is not None:
            visitor.visit_expr(self.value)
    def transform_children(self, transformer: IRTransformer) -> Decl:
        if self.value is not None:
            self.value = transformer.transform_expr(self.value)
        return self
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
    def visit_children(self, visitor: IRVisitor) -> None:
        for stmt in self.body:
            visitor.visit_stmt(stmt)
    def transform_children(self, transformer: IRTransformer) -> Decl:
        self.body = [transformer.transform_stmt(stmt) for stmt in self.body]
        return self
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
    def visit_children(self, visitor: IRVisitor) -> None:
        for stmt in self.body:
            visitor.visit_stmt(stmt)
    def transform_children(self, transformer: IRTransformer) -> Decl:
        self.body = [transformer.transform_stmt(stmt) for stmt in self.body]
        return self
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
    def visit_children(self, visitor: IRVisitor) -> None:
        for stmt in self.body:
            visitor.visit_stmt(stmt)
    def transform_children(self, transformer: IRTransformer) -> Decl:
        self.body = [transformer.transform_stmt(stmt) for stmt in self.body]
        return self
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
    def visit_children(self, visitor: IRVisitor) -> None:
        for decl in self.decls:
            visitor.visit_decl(decl)
    def transform_children(self, transformer: IRTransformer) -> Decl:
        self.decls = [transformer.transform_decl(decl) for decl in self.decls]
        return self
    def emit_java(self, pool: ConstantPool) -> Iterator[str]:
        extends = f' extends {self.extends}' if self.extends else ''
        yield f'{self.modifiers} class {self.name}{extends} {{'
        for decl in self.decls:
            yield from decl.emit_java(pool)
        yield '}'

class IRVisitor:
    def visit_expr(self, expr: Expr) -> None:
        expr.visit_children(self)

    def visit_stmt(self, stmt: Statement) -> None:
        stmt.visit_children(self)

    def visit_decl(self, decl: Decl) -> None:
        decl.visit_children(self)

class IRTransformer:
    def transform_expr(self, expr: Expr) -> Expr:
        return expr.transform_children(self)

    def transform_stmt(self, stmt: Statement) -> Statement:
        return stmt.transform_children(self)

    def transform_decl(self, decl: Decl) -> Decl:
        return decl.transform_children(self)

@dataclass(frozen=True, slots=True)
class LocalCarrierSpec:
    boxed_type: str
    raw_type: str
    field: str = 'value'
    raw_factory_class: Optional[str] = None
    raw_factory_method: str = 'newUnboxed'

    def matches_constant(self, value: object) -> bool:
        match self.boxed_type:
            case 'PyInt':
                return isinstance(value, int) and not isinstance(value, bool)
            case 'PyBool':
                return value is False or value is True
            case 'PyFloat':
                return isinstance(value, float)
            case 'PyString':
                return isinstance(value, str)
        return False

    def raw_constant_expr(self, value: object) -> Expr:
        match self.boxed_type:
            case 'PyInt':
                return IntLiteral(value, 'L')
            case 'PyBool':
                return Bool(value)
            case 'PyFloat':
                if math.isnan(value):
                    return Identifier('Double.NaN', 'double')
                if math.isinf(value):
                    return Identifier('Double.POSITIVE_INFINITY' if value > 0 else 'Double.NEGATIVE_INFINITY', 'double')
                return FloatLiteral(value)
            case 'PyString':
                return StrLiteral(value)
        assert False, self.boxed_type

    def default_raw_expr(self) -> Expr:
        match self.raw_type:
            case 'long':
                return IntLiteral(0, 'L')
            case 'boolean':
                return Bool(False)
            case 'double':
                return FloatLiteral(0.0)
            case 'String' | 'byte[]' | 'PyObject[]' | 'java.io.ByteArrayOutputStream' | 'StringBuilder' | 'ArrayList<PyObject>' | 'HashSet<PyObject>' | 'LinkedHashMap<PyObject, PyObject>':
                return Null()
        assert False, self.raw_type

ALL_LOCAL_CARRIER_SPECS = {
    spec.boxed_type: spec
    for spec in [
        LocalCarrierSpec('PyInt', 'long'),
        LocalCarrierSpec('PyBool', 'boolean'),
        LocalCarrierSpec('PyFloat', 'double'),
        LocalCarrierSpec('PyString', 'String'),
        LocalCarrierSpec('PyBytes', 'byte[]'),
        LocalCarrierSpec('PyTuple', 'PyObject[]', 'items'),
        LocalCarrierSpec('PyBytesBuilder', 'java.io.ByteArrayOutputStream', raw_factory_class='PyBytesBuilder'),
        LocalCarrierSpec('PyStringBuilder', 'StringBuilder', raw_factory_class='PyStringBuilder'),
        LocalCarrierSpec('PyByteArray', 'byte[]'),
        LocalCarrierSpec('PyList', 'ArrayList<PyObject>', 'items'),
        LocalCarrierSpec('PySet', 'HashSet<PyObject>', 'items'),
        LocalCarrierSpec('PyDict', 'LinkedHashMap<PyObject, PyObject>', 'items'),
    ]
}

# Final submit set. During development this can be widened to exercise more carrier types.
ENABLED_LOCAL_CARRIER_TYPES = {
    'PyInt',
    'PyBool',
    'PyFloat',
    'PyString',
    'PyBytes',
    'PyTuple',
    'PyBytesBuilder',
    'PyStringBuilder',
}

LOCAL_CARRIER_SPECS = {
    boxed_type: ALL_LOCAL_CARRIER_SPECS[boxed_type]
    for boxed_type in ENABLED_LOCAL_CARRIER_TYPES
}

def _local_carrier_assignment_expr(expr: Expr, spec: LocalCarrierSpec) -> bool:
    if isinstance(expr, CastExpr) and expr.type == 'PyObject':
        expr = expr.expr
    return (
        isinstance(expr, Null) or
        (isinstance(expr, PyConstant) and spec.matches_constant(expr.value)) or
        expr.java_type() == spec.boxed_type
    )

def _matched_local_carrier_field(expr: Expr, specs_by_name: dict[str, LocalCarrierSpec]) -> Optional[tuple[str, LocalCarrierSpec]]:
    if not isinstance(expr, Field):
        return None
    obj = expr.obj
    if isinstance(obj, Identifier) and obj.name in specs_by_name:
        spec = specs_by_name[obj.name]
        if expr.field == spec.field and expr.field_type == spec.raw_type:
            return (obj.name, spec)
    if isinstance(obj, CastExpr) and isinstance(obj.expr, Identifier) and obj.expr.name in specs_by_name:
        spec = specs_by_name[obj.expr.name]
        if obj.type == spec.boxed_type and expr.field == spec.field and expr.field_type == spec.raw_type:
            return (obj.expr.name, spec)
    return None

class LocalCarrierUsageValidator(IRVisitor):
    def __init__(self, specs_by_name: dict[str, LocalCarrierSpec]):
        self.specs_by_name = specs_by_name
        self.valid = set(specs_by_name)

    def invalidate(self, name: str) -> None:
        self.valid.discard(name)

    def visit_lhs(self, expr: Expr) -> None:
        if not self.valid:
            return
        match expr:
            case Identifier(name, _):
                if name in self.valid:
                    self.invalidate(name)
            case Field(obj, _, _):
                self.visit_expr(obj)
            case ArrayAccess(obj, index):
                self.visit_expr(obj)
                self.visit_expr(index)
            case _:
                self.visit_expr(expr)

    def check_assignment_expr(self, expr: Expr, name: str, spec: LocalCarrierSpec) -> None:
        if not _local_carrier_assignment_expr(expr, spec):
            self.invalidate(name)
            return
        self.visit_expr(expr)

    def visit_expr(self, expr: Expr) -> None:
        if not self.valid:
            return
        if _matched_local_carrier_field(expr, self.specs_by_name) is not None:
            return
        if isinstance(expr, Identifier) and expr.name in self.valid:
            self.invalidate(expr.name)
            return
        if isinstance(expr, AssignExpr):
            if isinstance(expr.lhs, Identifier) and expr.lhs.name in self.valid:
                name = expr.lhs.name
                self.check_assignment_expr(expr.rhs, name, self.specs_by_name[name])
            else:
                self.visit_lhs(expr.lhs)
                self.visit_expr(expr.rhs)
            return
        super().visit_expr(expr)

    def visit_stmt(self, stmt: Statement) -> None:
        if not self.valid:
            return
        match stmt:
            case LocalDecl(_, name, value):
                if name in self.valid:
                    if value is not None:
                        self.check_assignment_expr(value, name, self.specs_by_name[name])
                    return
            case AssignStatement(lhs, rhs):
                if isinstance(lhs, Identifier) and lhs.name in self.valid:
                    self.check_assignment_expr(rhs, lhs.name, self.specs_by_name[lhs.name])
                    return
                self.visit_lhs(lhs)
                self.visit_expr(rhs)
                return
            case ForStatement(_, init_name, init_value, _, incr_name, incr_value, body):
                if init_name in self.valid:
                    self.invalidate(init_name)
                if incr_name in self.valid:
                    self.invalidate(incr_name)
                self.visit_expr(init_value)
                self.visit_expr(stmt.cond)
                self.visit_expr(incr_value)
                for child in body:
                    self.visit_stmt(child)
                return
            case ForEachStatement(_, var_name, iterable, body):
                if var_name in self.valid:
                    self.invalidate(var_name)
                self.visit_expr(iterable)
                for child in body:
                    self.visit_stmt(child)
                return
            case TryStatement(try_body, _, exc_name, catch_body, finally_body):
                if exc_name is not None and exc_name in self.valid:
                    self.invalidate(exc_name)
                for child in try_body:
                    self.visit_stmt(child)
                for child in catch_body:
                    self.visit_stmt(child)
                for child in finally_body:
                    self.visit_stmt(child)
                return
        super().visit_stmt(stmt)

class LocalCarrierRewriter(IRTransformer):
    def __init__(self, specs_by_name: dict[str, LocalCarrierSpec]):
        self.specs_by_name = specs_by_name

    def rewrite_assignment_expr(self, expr: Expr, spec: LocalCarrierSpec) -> Expr:
        expr = self.transform_expr(expr)
        if isinstance(expr, CastExpr) and expr.type == 'PyObject':
            expr = expr.expr
        if isinstance(expr, Null):
            return spec.default_raw_expr()
        if isinstance(expr, PyConstant) and spec.matches_constant(expr.value):
            return spec.raw_constant_expr(expr.value)
        return unbox_local_carrier(expr, spec)

    def transform_expr(self, expr: Expr) -> Expr:
        matched_field = _matched_local_carrier_field(expr, self.specs_by_name)
        if matched_field is not None:
            (name, spec) = matched_field
            return Identifier(name, spec.raw_type)
        expr = super().transform_expr(expr)
        if isinstance(expr, CastExpr) and expr.type in ALL_LOCAL_CARRIER_SPECS:
            spec = ALL_LOCAL_CARRIER_SPECS[expr.type]
            if expr.expr.java_type() == spec.raw_type:
                return expr.expr
        return expr

    def transform_stmt(self, stmt: Statement) -> Statement:
        match stmt:
            case LocalDecl(_, name, value):
                if name in self.specs_by_name:
                    spec = self.specs_by_name[name]
                    stmt.type = spec.raw_type
                    stmt.value = spec.default_raw_expr() if value is None else self.rewrite_assignment_expr(value, spec)
                    return stmt
            case AssignStatement(lhs, rhs):
                if isinstance(lhs, Identifier) and lhs.name in self.specs_by_name:
                    spec = self.specs_by_name[lhs.name]
                    stmt.lhs = Identifier(lhs.name, spec.raw_type)
                    stmt.rhs = self.rewrite_assignment_expr(rhs, spec)
                    return stmt
        return super().transform_stmt(stmt)

def _valid_local_carrier_locals_in_block(body: list[Statement], specs_by_name: dict[str, LocalCarrierSpec]) -> dict[str, LocalCarrierSpec]:
    validator = LocalCarrierUsageValidator(specs_by_name)
    for stmt in body:
        validator.visit_stmt(stmt)
        if not validator.valid:
            break
    return {name: specs_by_name[name] for name in validator.valid}

class LocalCarrierAssignmentTypeCollector(IRVisitor):
    def __init__(self, names: set[str]):
        self.names = names
        self.assigned_types: dict[str, set[str]] = {name: set() for name in names}

    def _record(self, name: str, expr: Expr) -> None:
        if isinstance(expr, CastExpr) and expr.type == 'PyObject':
            expr = expr.expr
        if isinstance(expr, Null):
            return
        expr_type = expr.java_type()
        if expr_type in ALL_LOCAL_CARRIER_SPECS:
            self.assigned_types[name].add(expr_type)

    def visit_expr(self, expr: Expr) -> None:
        if isinstance(expr, AssignExpr) and isinstance(expr.lhs, Identifier) and expr.lhs.name in self.names:
            self._record(expr.lhs.name, expr.rhs)
        super().visit_expr(expr)

    def visit_stmt(self, stmt: Statement) -> None:
        match stmt:
            case LocalDecl(_, name, value):
                if name in self.names and value is not None:
                    self._record(name, value)
            case AssignStatement(lhs, rhs):
                if isinstance(lhs, Identifier) and lhs.name in self.names:
                    self._record(lhs.name, rhs)
        super().visit_stmt(stmt)

def _collect_local_carrier_candidates(body: list[Statement]) -> dict[str, LocalCarrierSpec]:
    candidates: dict[str, LocalCarrierSpec] = {}
    pyobject_names: set[str] = set()
    for stmt in body:
        if not isinstance(stmt, LocalDecl):
            continue
        if stmt.type in LOCAL_CARRIER_SPECS:
            candidates[stmt.name] = LOCAL_CARRIER_SPECS[stmt.type]
            continue
        if stmt.type == 'PyObject':
            pyobject_names.add(stmt.name)
    if pyobject_names:
        collector = LocalCarrierAssignmentTypeCollector(pyobject_names)
        for stmt in body:
            collector.visit_stmt(stmt)
        for name in pyobject_names:
            assigned_types = collector.assigned_types[name]
            if len(assigned_types) == 1:
                boxed_type = next(iter(assigned_types))
                if boxed_type in LOCAL_CARRIER_SPECS:
                    candidates[name] = LOCAL_CARRIER_SPECS[boxed_type]
    return candidates

def lower_local_carrier_locals_in_stmt(stmt: Statement) -> Statement:
    match stmt:
        case IfStatement(_, body, orelse):
            stmt.body = lower_local_carrier_locals_in_block(body)
            stmt.orelse = lower_local_carrier_locals_in_block(orelse)
            return stmt
        case WhileStatement(_, body):
            stmt.body = lower_local_carrier_locals_in_block(body)
            return stmt
        case ForStatement(_, _, _, _, _, _, body):
            stmt.body = lower_local_carrier_locals_in_block(body)
            return stmt
        case ForEachStatement(_, _, _, body):
            stmt.body = lower_local_carrier_locals_in_block(body)
            return stmt
        case TryStatement(try_body, _, _, catch_body, finally_body):
            stmt.try_body = lower_local_carrier_locals_in_block(try_body)
            stmt.catch_body = lower_local_carrier_locals_in_block(catch_body)
            stmt.finally_body = lower_local_carrier_locals_in_block(finally_body)
            return stmt
        case LabeledBlock(_, body):
            stmt.body = lower_local_carrier_locals_in_block(body)
            return stmt
        case _:
            return stmt

def lower_local_carrier_locals_in_method(method: MethodDecl) -> MethodDecl:
    method.body = lower_local_carrier_locals_in_block(method.body)
    return method

def lower_local_carrier_locals_in_block(body: list[Statement]) -> list[Statement]:
    body = [lower_local_carrier_locals_in_stmt(stmt) for stmt in body]
    candidates = _collect_local_carrier_candidates(body)
    if not candidates:
        return body
    valid = _valid_local_carrier_locals_in_block(body, candidates)
    if not valid:
        return body
    rewriter = LocalCarrierRewriter(valid)
    return [rewriter.transform_stmt(stmt) for stmt in body]

def lower_local_carrier_locals_in_decl(decl: Decl) -> Decl:
    match decl:
        case MethodDecl():
            return lower_local_carrier_locals_in_method(decl)
        case ConstructorDecl(_, _, _, body):
            decl.body = lower_local_carrier_locals_in_block(body)
            return decl
        case StaticBlock(body):
            decl.body = lower_local_carrier_locals_in_block(body)
            return decl
        case ClassDecl(_, _, _, decls):
            decl.decls = [lower_local_carrier_locals_in_decl(d) for d in decls]
            return decl
        case _:
            return decl

def unary_op(op: str, operand: Expr) -> Expr:
    if op == '!' and isinstance(operand, Bool):
        return Bool(not operand.value)
    return UnaryOp(op, operand)

def bool_value(expr: Expr) -> Expr:
    if isinstance(expr, StaticMethodCall) and expr.class_name == 'PyBool' and expr.method == 'create':
        return expr.args[0] # return the raw boolean instead of box/unbox
    if isinstance(expr, PyConstant):
        return Bool(bool(expr.value))
    if expr.java_type() == 'PyBool':
        return Field(expr, 'value', 'boolean')
    return MethodCall(expr, 'boolValue', [], 'boolean')

def _unbox_create_object(expr: CreateObject, spec: LocalCarrierSpec) -> Expr:
    if spec.raw_factory_class is not None:
        return static_method_call(spec.raw_factory_class, spec.raw_factory_method, expr.args)
    if not expr.args:
        return CreateObject(spec.raw_type, [])
    arg = expr.args[0]
    return arg if arg.java_type() == spec.raw_type else CastExpr(spec.raw_type, arg)

def unbox_local_carrier(expr: Expr, spec: LocalCarrierSpec) -> Expr:
    if isinstance(expr, PyConstant) and spec.matches_constant(expr.value):
        return spec.raw_constant_expr(expr.value)
    if isinstance(expr, CondOp):
        return CondOp(expr.cond, unbox_local_carrier(expr.true, spec), unbox_local_carrier(expr.false, spec))
    if isinstance(expr, Field) and expr.field == spec.field and expr.field_type == spec.raw_type:
        if isinstance(expr.obj, PyConstant) and spec.matches_constant(expr.obj.value):
            return spec.raw_constant_expr(expr.obj.value)
        if isinstance(expr.obj, CreateObject) and expr.obj.type == spec.boxed_type:
            return _unbox_create_object(expr.obj, spec)
        if expr.obj.java_type() == spec.boxed_type:
            return expr
    if isinstance(expr, CastExpr) and expr.type == spec.boxed_type:
        if expr.expr.java_type() == spec.raw_type:
            return expr.expr
        return unbox_local_carrier(expr.expr, spec)
    if isinstance(expr, CreateObject) and expr.type == spec.boxed_type:
        return _unbox_create_object(expr, spec)
    if expr.java_type() == spec.boxed_type:
        return Field(expr, spec.field, spec.raw_type)
    assert expr.java_type() != spec.raw_type, expr
    return Field(CastExpr(spec.boxed_type, expr), spec.field, spec.raw_type)

def unbox_int(expr: Expr) -> Expr:
    return unbox_local_carrier(expr, ALL_LOCAL_CARRIER_SPECS['PyInt'])

def unbox_float(expr: Expr) -> Expr:
    return unbox_local_carrier(expr, ALL_LOCAL_CARRIER_SPECS['PyFloat'])

def unbox_str(expr: Expr) -> Expr:
    return unbox_local_carrier(expr, ALL_LOCAL_CARRIER_SPECS['PyString'])

def py_format(obj: Expr, spec: Expr) -> Expr:
    if isinstance(spec, PyConstant) and isinstance(spec.value, str) and not spec.value:
        java_type = obj.java_type()
        if java_type == 'PyInt':
            return CreateObject('PyString', [static_method_call('Long', 'toString', [unbox_int(obj)])])
        if java_type == 'PyString':
            return obj
    return CreateObject('PyString', [MethodCall(obj, 'format', [unbox_str(spec)], 'String')])

def py_index(obj: Expr) -> Expr:
    if obj.java_type() == 'PyInt':
        return unbox_int(obj)
    return MethodCall(obj, 'indexValue', [], 'long')

def py_iter(obj: Expr) -> Expr:
    match obj.java_type():
        case 'PyBytes':
            return CreateObject('PyBytesIter', [obj])
        case 'PyDict':
            return CreateObject('PyDictIter', [MethodCall(MethodCall(Field(obj, 'items', 'LinkedHashMap<PyObject, PyObject>'), 'keySet', []), 'iterator', [])])
        case 'PyEnumerate':
            return obj
        case 'PyList':
            return CreateObject('PyListIter', [MethodCall(Field(obj, 'items', 'ArrayList<PyObject>'), 'iterator', [])])
        case 'PyRange':
            return CreateObject('PyRangeIter', [obj])
        case 'PySet':
            return CreateObject('PySetIter', [MethodCall(Field(obj, 'items', 'HashSet<PyObject>'), 'iterator', [])])
        case 'PyString':
            return CreateObject('PyStringIter', [obj])
        case 'PyTuple':
            return CreateObject('PyTupleIter', [obj])
    return MethodCall(obj, 'iter', [], 'PyIter')

def py_len(obj: Expr) -> Expr:
    match obj.java_type():
        case 'PyTuple':
            return CreateObject('PyInt', [Field(Field(obj, 'items', 'PyObject[]'), 'length', 'int')])
    return CreateObject('PyInt', [MethodCall(obj, 'len', [], 'long')])

def static_method_call(class_name: str, method: str, args: list[Expr], return_type: str = JAVA_TYPE_UNKNOWN) -> Expr:
    match (class_name, method):
        case ('PyBool', 'create') if isinstance(args[0], Bool):
            return PyConstant(args[0].value)
        case ('PyRange', 'newObjPositional') if isinstance(args[1], Null) and isinstance(args[2], Null):
            return CreateObject('PyRange', [IntLiteral(0, 'L'), py_index(args[0]), IntLiteral(1, 'L')])
        case ('PyRange', 'newObjPositional') if isinstance(args[2], Null):
            return CreateObject('PyRange', [py_index(args[0]), py_index(args[1]), IntLiteral(1, 'L')])
        case ('PyRange', 'newObjPositional'):
            return CreateObject('PyRange', [py_index(args[i]) for i in range(3)])
        case ('PyType', 'newObjPositional') if isinstance(args[1], Null) and isinstance(args[2], Null):
            return MethodCall(args[0], 'type', [], 'PyType')
        case ('Runtime', 'pythonjBytesBuilder'):
            if isinstance(args[0], PyConstant) and args[0].value is None:
                return CreateObject('PyBytesBuilder', [])
            else:
                return CreateObject('PyBytesBuilder', [unbox_int(args[0])])
        case ('Runtime', 'pythonjBytesBuilderAppendByteArray'):
            return MethodCall(
                Field(CastExpr('PyBytesBuilder', args[0]), 'value', 'java.io.ByteArrayOutputStream'),
                'writeBytes',
                [Field(CastExpr('PyByteArray', args[1]), 'value', 'byte[]')],
                'void',
            )
        case ('Runtime', 'pythonjBytesBuilderAppendBytes'):
            return MethodCall(
                Field(CastExpr('PyBytesBuilder', args[0]), 'value', 'java.io.ByteArrayOutputStream'),
                'writeBytes',
                [Field(CastExpr('PyBytes', args[1]), 'value', 'byte[]')],
                'void',
            )
        case ('Runtime', 'pythonjBytesBuilderAppendInt'):
            return MethodCall(
                Field(CastExpr('PyBytesBuilder', args[0]), 'value', 'java.io.ByteArrayOutputStream'),
                'write',
                [CastExpr('int', unbox_int(args[1]))],
                'void',
            )
        case ('Runtime', 'pythonjBytesBuilderFinish'):
            return CreateObject('PyBytes', [MethodCall(Field(CastExpr('PyBytesBuilder', args[0]), 'value', 'java.io.ByteArrayOutputStream'), 'toByteArray', [], 'byte[]')])
        case ('Runtime', 'pythonjDelete'):
            return MethodCall(args[0], 'delete', [args[1], MethodCall(args[1], 'type', [], 'PyType')], 'void')
        case ('Runtime', 'pythonjDictGet'):
            return MethodCall(Field(CastExpr('PyDict', args[0]), 'items', 'LinkedHashMap<PyObject, PyObject>'), 'get', [args[1]], 'PyObject')
        case ('Runtime', 'pythonjDictRemove'):
            return MethodCall(Field(CastExpr('PyDict', args[0]), 'items', 'LinkedHashMap<PyObject, PyObject>'), 'remove', [args[1]], 'PyObject')
        case ('Runtime', 'pythonjFloatJavaBits'):
            return CreateObject('PyInt', [StaticMethodCall('Double', 'doubleToLongBits', [unbox_float(args[0])], 'long')])
        case ('Runtime', 'pythonjFloatJavaFormat'):
            return CreateObject('PyString', [
                StaticMethodCall('String', 'format', [Identifier('java.util.Locale.ROOT'), unbox_str(args[0]), unbox_float(args[1])], 'String')
            ])
        case ('Runtime', 'pythonjFloatJavaRint'):
            return CreateObject('PyFloat', [StaticMethodCall('Math', 'rint', [unbox_float(args[0])], 'double')])
        case ('Runtime', 'pythonjFloatJavaStr'):
            return CreateObject('PyString', [StaticMethodCall('Double', 'toString', [unbox_float(args[0])], 'String')])
        case ('Runtime', 'pythonjFormat'):
            return py_format(args[0], args[1])
        case ('Runtime', 'pythonjGet'):
            return MethodCall(args[0], 'get', [args[1], CastExpr('PyType', args[2])], 'PyObject')
        case ('Runtime', 'pythonjHasIndex'):
            return static_method_call('PyBool', 'create', [MethodCall(args[0], 'hasIndex', [], 'boolean')])
        case ('Runtime', 'pythonjHasIter'):
            return static_method_call('PyBool', 'create', [MethodCall(args[0], 'hasIter', [], 'boolean')])
        case ('Runtime', 'pythonjInstanceDict'):
            return MethodCall(args[0], 'getInstanceDict', [], 'PyDict')
        case ('Runtime', 'pythonjIntJavaBitCount'):
            return CreateObject('PyInt', [CastExpr('long', StaticMethodCall('Long', 'bitCount', [unbox_int(args[0])], 'int'))])
        case ('Runtime', 'pythonjIntJavaLeadingZeros'):
            return CreateObject('PyInt', [CastExpr('long', StaticMethodCall('Long', 'numberOfLeadingZeros', [unbox_int(args[0])], 'int'))])
        case ('Runtime', 'pythonjIntStr'):
            return CreateObject('PyString', [static_method_call('Long', 'toString', [unbox_int(args[0])])])
        case ('Runtime', 'pythonjIntStrBase'):
            return CreateObject('PyString', [static_method_call('Long', 'toString', [unbox_int(args[0]), CastExpr('int', unbox_int(args[1]))])])
        case ('Runtime', 'pythonjIsDataDescriptor'):
            return static_method_call('PyBool', 'create', [MethodCall(args[0], 'isDataDescriptor', [], 'boolean')])
        case ('Runtime', 'pythonjIter'):
            return py_iter(args[0])
        case ('Runtime', 'pythonjLen'):
            return py_len(args[0])
        case ('Runtime', 'pythonjLookupAttr'):
            return MethodCall(CastExpr('PyType', args[0]), 'lookupAttr', [unbox_str(args[1])], 'PyObject')
        case ('Runtime', 'pythonjNext'):
            return MethodCall(args[0], 'next', [], 'PyObject')
        case ('Runtime', 'pythonjRangeStart'):
            return CreateObject('PyInt', [Field(CastExpr('PyRange', args[0]), 'start', 'long')])
        case ('Runtime', 'pythonjRangeStep'):
            return CreateObject('PyInt', [Field(CastExpr('PyRange', args[0]), 'step', 'long')])
        case ('Runtime', 'pythonjRangeStop'):
            return CreateObject('PyInt', [Field(CastExpr('PyRange', args[0]), 'stop', 'long')])
        case ('Runtime', 'pythonjRepr'):
            if args[0].java_type() == 'PyString':
                return CreateObject('PyString', [static_method_call('PyString', 'reprOf', [unbox_str(args[0])])])
            return CreateObject('PyString', [MethodCall(args[0], 'repr', [], 'String')])
        case ('Runtime', 'pythonjSet'):
            return MethodCall(args[0], 'set', [args[1], MethodCall(args[1], 'type', [], 'PyType'), args[2]], 'void')
        case ('Runtime', 'pythonjSliceStart'):
            return Field(CastExpr('PySlice', args[0]), 'start', 'PyObject')
        case ('Runtime', 'pythonjSliceStep'):
            return Field(CastExpr('PySlice', args[0]), 'step', 'PyObject')
        case ('Runtime', 'pythonjSliceStop'):
            return Field(CastExpr('PySlice', args[0]), 'stop', 'PyObject')
        case ('Runtime', 'pythonjStrBuilder'):
            if isinstance(args[0], PyConstant) and args[0].value is None:
                return CreateObject('PyStringBuilder', [])
            else:
                return CreateObject('PyStringBuilder', [unbox_int(args[0])])
        case ('Runtime', 'pythonjStrBuilderAppend'):
            return MethodCall(Field(CastExpr('PyStringBuilder', args[0]), 'value', 'StringBuilder'), 'append', [unbox_str(args[1])], 'void')
        case ('Runtime', 'pythonjStrBuilderFinish'):
            return CreateObject('PyString', [MethodCall(Field(CastExpr('PyStringBuilder', args[0]), 'value', 'StringBuilder'), 'toString', [], 'String')])
        case ('Runtime', 'pythonjStrReplace'):
            return CreateObject('PyString', [MethodCall(unbox_str(args[0]), 'replace', [unbox_str(args[1]), unbox_str(args[2])], 'String')])
        case ('Runtime', 'pythonjZipNew'):
            return static_method_call('PyZip', 'newObjPositional', [Field(args[0], 'items', 'PyObject[]'), args[1]])
    return StaticMethodCall(class_name, method, args, STATIC_METHOD_RETURN_TYPES.get((class_name, method), return_type))

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
    decls = [lower_local_carrier_locals_in_decl(decl) for decl in decls]
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
