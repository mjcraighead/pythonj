// pythonj (https://github.com/mjcraighead/pythonj)
// Copyright (c) 2012-2026 Matt Craighead
// SPDX-License-Identifier: MIT

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class PyFloat extends PyObject {
    public final double value;

    PyFloat(double _value) { value = _value; }

    private static String zeros(int n) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < n; i++) {
            ret.append('0');
        }
        return ret.toString();
    }
    private static String trimFixedFraction(String s) {
        while (s.endsWith("0") && !s.endsWith(".0")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
    private static String pythonStyleFiniteStr(double value) {
        String s = Double.toString(value);
        int eIndex = s.indexOf('E');
        if (eIndex == -1) {
            return s;
        }

        String sign = "";
        if (s.startsWith("-")) {
            sign = "-";
            s = s.substring(1);
            eIndex -= 1;
        }

        String mantissa = s.substring(0, eIndex);
        int exponent = Integer.parseInt(s.substring(eIndex + 1));
        String digits = mantissa.replace(".", "");
        int dotIndex = mantissa.indexOf('.');
        int fractionalDigits = (dotIndex == -1) ? 0 : (mantissa.length() - dotIndex - 1);

        if ((exponent >= -4) && (exponent < 16)) {
            int decimalPos = digits.length() - fractionalDigits + exponent;
            String ret;
            if (decimalPos <= 0) {
                ret = "0." + zeros(-decimalPos) + digits;
            } else if (decimalPos >= digits.length()) {
                ret = digits + zeros(decimalPos - digits.length()) + ".0";
            } else {
                ret = digits.substring(0, decimalPos) + "." + digits.substring(decimalPos);
            }
            return sign + trimFixedFraction(ret);
        }

        String expDigits = Integer.toString(Math.abs(exponent));
        if (expDigits.length() < 2) {
            expDigits = "0" + expDigits;
        }
        if (mantissa.endsWith(".0")) {
            mantissa = mantissa.substring(0, mantissa.length() - 2);
        }
        return sign + mantissa + "e" + ((exponent >= 0) ? "+" : "-") + expDigits;
    }

    private static long[] finiteIntegerRatio(double value) {
        long bits = Double.doubleToRawLongBits(value);
        boolean negative = bits < 0;
        int exponentBits = (int)((bits >>> 52) & 0x7FF);
        long numerator = bits & ((1L << 52) - 1);
        int exponent;
        if (exponentBits == 0) {
            exponent = -1022 - 52;
        } else {
            numerator |= (1L << 52);
            exponent = exponentBits - 1023 - 52;
        }
        while (((numerator & 1) == 0) && (exponent < 0)) {
            numerator >>= 1;
            exponent++;
        }
        long denominator = 1;
        if (exponent > 0) {
            numerator = PyInt.lshift(numerator, exponent);
        } else if (exponent < 0) {
            denominator = PyInt.lshift(1, -exponent);
        }
        if (negative) {
            numerator = Math.negateExact(numerator);
        }
        return new long[]{numerator, denominator};
    }
    private static PyFloat parseString(String s, String repr) {
        String sl = s.toLowerCase();
        if (sl.equals("inf") || sl.equals("+inf") || sl.equals("infinity") || sl.equals("+infinity")) {
            return new PyFloat(Double.POSITIVE_INFINITY);
        } else if (sl.equals("-inf") || sl.equals("-infinity")) {
            return new PyFloat(Double.NEGATIVE_INFINITY);
        } else if (sl.equals("nan") || sl.equals("+nan") || sl.equals("-nan")) {
            return new PyFloat(Double.NaN);
        }
        try {
            return new PyFloat(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            throw PyValueError.raise("could not convert string to float: " + repr);
        }
    }
    private static double floatLikeValue(PyObject obj) {
        if ((obj instanceof PyFloat) || (obj instanceof PyInt) || (obj instanceof PyBool)) {
            return obj.floatValue();
        }
        throw new UnsupportedOperationException("not a float-like object");
    }
    private static PyFloat add(double lhs, double rhs) { return new PyFloat(lhs + rhs); }
    private static PyFloat sub(double lhs, double rhs) { return new PyFloat(lhs - rhs); }
    private static PyFloat mul(double lhs, double rhs) { return new PyFloat(lhs * rhs); }
    private static PyFloat trueDiv(double lhs, double rhs) {
        if (rhs == 0.0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        return new PyFloat(lhs / rhs);
    }
    private static PyFloat floorDiv(double lhs, double rhs) {
        if (rhs == 0.0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        return new PyFloat(Math.floor(lhs / rhs));
    }
    private static PyFloat mod(double lhs, double rhs) {
        if (rhs == 0.0) {
            throw PyZeroDivisionError.raise("division by zero");
        }
        double ret = lhs - (Math.floor(lhs / rhs) * rhs);
        if (ret == 0.0) {
            ret = Math.copySign(0.0, rhs);
        }
        return new PyFloat(ret);
    }
    private static PyFloat pow(double lhs, double rhs) {
        if ((lhs == 0.0) && (rhs < 0.0)) {
            throw PyZeroDivisionError.raise("zero to a negative power");
        }
        double ret = Math.pow(lhs, rhs);
        if (Double.isNaN(ret)) {
            throw new UnsupportedOperationException("float pow is unimplemented for this input");
        }
        return new PyFloat(ret);
    }
    private static String strOf(double value) {
        if (Double.isNaN(value)) {
            return "nan";
        } else if (value == Double.POSITIVE_INFINITY) {
            return "inf";
        } else if (value == Double.NEGATIVE_INFINITY) {
            return "-inf";
        } else {
            return pythonStyleFiniteStr(value);
        }
    }
    private static String formatFiniteCore(double value, boolean alt, String grouping, Long precision, String typeChar) {
        assert Double.isFinite(value) && (value >= 0.0) : value;
        boolean postprocessAlt = false;
        if (typeChar.isEmpty()) {
            if ((precision == null) && (grouping == null) && !alt) {
                return strOf(value);
            }
            typeChar = "g";
        } else if (typeChar.equals("n")) {
            typeChar = "g";
        } else if (typeChar.equals("F")) {
            typeChar = "f";
        }
        String javaType = typeChar;
        if (alt && (javaType.equals("g") || javaType.equals("G"))) {
            postprocessAlt = true;
            alt = false;
        }
        StringBuilder fmt = new StringBuilder("%");
        if (grouping != null) {
            fmt.append(",");
        }
        if (alt) {
            fmt.append("#");
        }
        if (precision != null) {
            fmt.append(".").append(precision);
        }
        fmt.append(javaType);
        String ret = String.format(Locale.ROOT, fmt.toString(), value);
        if ((grouping != null) && grouping.equals("_")) {
            ret = ret.replace(',', '_');
        }
        if (javaType.equals("g") || javaType.equals("G")) {
            int expIndex = ret.indexOf('e');
            if (expIndex == -1) {
                expIndex = ret.indexOf('E');
            }
            String expSuffix = "";
            String mantissa = ret;
            if (expIndex != -1) {
                mantissa = ret.substring(0, expIndex);
                expSuffix = ret.substring(expIndex);
            }
            int dotIndex = mantissa.indexOf('.');
            if (dotIndex != -1) {
                if (postprocessAlt) {
                    // Keep Java's trailing zeros for %#g/%#G.
                } else {
                    int i = mantissa.length();
                    while ((i > dotIndex + 1) && (mantissa.charAt(i - 1) == '0')) {
                        i--;
                    }
                    if ((i == dotIndex + 1) && (mantissa.charAt(dotIndex) == '.')) {
                        i--;
                    }
                    mantissa = mantissa.substring(0, i);
                }
            } else if (postprocessAlt) {
                mantissa += ".";
            }
            ret = mantissa + expSuffix;
        }
        return ret;
    }

    public static PyObject newObj(PyConcreteType type, PyObject[] args, PyDict kwargs) {
        Runtime.requireNoKwArgs(kwargs, type.name());
        Runtime.requireMaxArgs(args, 1, type.name());
        if (args.length == 0) {
            return new PyFloat(0.0);
        }
        PyObject arg = args[0];
        if (arg instanceof PyFloat argFloat) {
            return argFloat;
        } else if (arg.hasIndex()) {
            return new PyFloat(arg.indexValue());
        } else if (arg instanceof PyString argStr) {
            return parseString(argStr.value, PyString.reprOf(argStr.value));
        } else if (arg instanceof PyBytes argBytes) {
            String s = new String(argBytes.value, StandardCharsets.ISO_8859_1);
            return parseString(s, arg.repr());
        } else if (arg instanceof PyByteArray argByteArray) {
            String s = new String(argByteArray.value, StandardCharsets.ISO_8859_1);
            return parseString(s, arg.repr());
        } else {
            throw PyTypeError.raise("float() argument must be a string or a real number, not " + PyString.reprOf(arg.type().name()));
        }
    }

    @Override public PyFloat pos() { return this; }
    @Override public PyFloat neg() { return new PyFloat(-value); }
    @Override public PyFloat abs() { return new PyFloat(Math.abs(value)); }
    @Override public PyObject add(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return add(value, floatLikeValue(rhs));
        } else {
            return super.add(rhs);
        }
    }
    @Override public PyObject floorDiv(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return floorDiv(value, floatLikeValue(rhs));
        } else {
            return super.floorDiv(rhs);
        }
    }
    @Override public PyObject mod(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return mod(value, floatLikeValue(rhs));
        } else {
            return super.mod(rhs);
        }
    }
    @Override public PyObject mul(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return mul(value, floatLikeValue(rhs));
        } else {
            return super.mul(rhs);
        }
    }
    @Override public PyObject pow(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return pow(value, floatLikeValue(rhs));
        } else {
            return super.pow(rhs);
        }
    }
    @Override public PyObject radd(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return add(floatLikeValue(rhs), value);
        } else {
            return super.radd(rhs);
        }
    }
    @Override public PyObject rfloorDiv(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return floorDiv(floatLikeValue(rhs), value);
        } else {
            return super.rfloorDiv(rhs);
        }
    }
    @Override public PyObject rmod(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return mod(floatLikeValue(rhs), value);
        } else {
            return super.rmod(rhs);
        }
    }
    @Override public PyObject rmul(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return mul(floatLikeValue(rhs), value);
        } else {
            return super.rmul(rhs);
        }
    }
    @Override public PyObject rpow(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return pow(floatLikeValue(rhs), value);
        } else {
            return super.rpow(rhs);
        }
    }
    @Override public PyObject rsub(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return sub(floatLikeValue(rhs), value);
        } else {
            return super.rsub(rhs);
        }
    }
    @Override public PyObject rtrueDiv(PyObject rhs) {
        if ((rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return trueDiv(floatLikeValue(rhs), value);
        } else {
            return super.rtrueDiv(rhs);
        }
    }
    @Override public PyObject sub(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return sub(value, floatLikeValue(rhs));
        } else {
            return super.sub(rhs);
        }
    }
    @Override public PyObject trueDiv(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return trueDiv(value, floatLikeValue(rhs));
        } else {
            return super.trueDiv(rhs);
        }
    }
    @Override public final boolean boolValue() { return value != 0.0; }
    @Override public boolean equals(Object rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return value == floatLikeValue((PyObject)rhs);
        }
        return false;
    }
    @Override public double floatValue() { return value; }
    @Override public int hashCode() {
        if (Double.isNaN(value)) {
            throw new UnsupportedOperationException();
        } else if (value == Double.POSITIVE_INFINITY) {
            return 314159;
        } else if (value == Double.NEGATIVE_INFINITY) {
            return -314159;
        }
        long[] ratio = finiteIntegerRatio(value);
        return Runtime.hashRational(ratio[0], ratio[1]);
    }
    @Override public boolean ge(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return value >= floatLikeValue(rhs);
        } else {
            return super.ge(rhs);
        }
    }
    @Override public boolean gt(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return value > floatLikeValue(rhs);
        } else {
            return super.gt(rhs);
        }
    }
    @Override public boolean le(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return value <= floatLikeValue(rhs);
        } else {
            return super.le(rhs);
        }
    }
    @Override public boolean lt(PyObject rhs) {
        if ((rhs instanceof PyFloat) || (rhs instanceof PyInt) || (rhs instanceof PyBool)) {
            return value < floatLikeValue(rhs);
        } else {
            return super.lt(rhs);
        }
    }
    @Override public String repr() { return strOf(value); }
    @Override public String str() { return strOf(value); }
    @Override public PyConcreteType type() { return PyFloatType.singleton; }
    @Override public String format(String formatSpec) {
        PyTuple parsed = (PyTuple)PyRuntimePythonImpl.pyfunc_pyj_float_parse_spec(new PyString(formatSpec));
        String fill = ((PyString)parsed.items[0]).value;
        PyObject alignObj = parsed.items[1];
        String sign = ((PyString)parsed.items[2]).value;
        boolean z = parsed.items[3].boolValue();
        boolean alt = parsed.items[4].boolValue();
        PyObject widthObj = parsed.items[5];
        PyObject groupingObj = parsed.items[6];
        PyObject precisionObj = parsed.items[7];
        String typeChar = ((PyString)parsed.items[8]).value;
        String magnitudeText;
        PyObject specialText = PyRuntimePythonImpl.pyfunc_pyj_float_special_text(this, new PyString(typeChar));
        if (specialText != PyNone.singleton) {
            magnitudeText = ((PyString)specialText).value;
        } else {
            double coreValue = Math.abs(value);
            if (typeChar.equals("%")) {
                coreValue *= 100.0;
            }
            String coreTypeChar = typeChar.equals("%") ? "f" : typeChar;
            PyObject coreGroupingObj = groupingObj;
            if ((widthObj != PyNone.singleton) && (groupingObj != PyNone.singleton) &&
                (alignObj instanceof PyString alignStr) && alignStr.value.equals("=") && fill.equals("0")) {
                coreGroupingObj = PyNone.singleton;
            }
            String grouping = (coreGroupingObj == PyNone.singleton) ? null : ((PyString)coreGroupingObj).value;
            Long precision = (precisionObj == PyNone.singleton) ? null : precisionObj.indexValue();
            magnitudeText = formatFiniteCore(coreValue, alt, grouping, precision, coreTypeChar);
            if (typeChar.equals("%")) {
                magnitudeText += "%";
            }
        }
        return ((PyString)PyRuntimePythonImpl.pyfunc_pyj_float_finish_text(
            new PyString(fill),
            alignObj,
            new PyString(sign),
            PyBool.create(z),
            widthObj,
            groupingObj,
            this,
            new PyString(magnitudeText)
        )).value;
    }

    public PyObject pymethod_as_integer_ratio() {
        if (Double.isNaN(value)) {
            throw PyValueError.raise("cannot convert NaN to integer ratio");
        } else if (Double.isInfinite(value)) {
            throw PyOverflowError.raise("cannot convert Infinity to integer ratio");
        }
        long[] ratio = finiteIntegerRatio(value);
        return new PyTuple(new PyObject[]{new PyInt(ratio[0]), new PyInt(ratio[1])});
    }
    public static PyObject pymethod_from_number(PyType self, PyObject number) {
        if ((number instanceof PyFloat) || (number instanceof PyInt) || (number instanceof PyBool)) {
            return new PyFloat(number.floatValue());
        }
        throw new UnsupportedOperationException();
    }
    public static PyObject pymethod_fromhex(PyType self, PyObject s) {
        throw new UnsupportedOperationException();
    }
    public PyObject pymethod_hex() {
        throw new UnsupportedOperationException();
    }
    public PyBool pymethod_is_integer() {
        return PyBool.create(Double.isFinite(value) && (value == Math.rint(value)));
    }

    static PyObject pygetset_real(PyObject obj) { return obj; }
    static PyObject pygetset_imag(PyObject obj) { return new PyFloat(0.0); }
}
