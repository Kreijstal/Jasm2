package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.parser.Token;

public class ASTNumber extends ASTValue {

    public ASTNumber(Token number) {
        super(ElementType.NUMBER, number);
    }

    public Number number() {
        String value = content().toLowerCase();
        int radix = 10;
        if (value.startsWith("0x")) {
            radix = 16;
            value = value.substring(2);
        } else if (value.startsWith("nan")) {
            if (value.endsWith("f"))
                return Float.NaN;
            return Double.NaN;
        } else if (value.contains("infinity")) {
            if (value.startsWith("-")) {
                if (value.endsWith("f")) return Float.NEGATIVE_INFINITY;
                return Double.NEGATIVE_INFINITY;
            }
            if (value.endsWith("f"))
                return Float.POSITIVE_INFINITY;
            return Double.POSITIVE_INFINITY;
        }
        if (value.endsWith("f")) {
            return Float.parseFloat(value.substring(0, value.length() - 1));
        } else if (value.endsWith("d")) {
            return Double.parseDouble(value.substring(0, value.length() - 1));
        } else if (value.contains(".")) {
            return Double.parseDouble(value);
        } else {
            if (value.endsWith("l")) {
                return Long.parseLong(value.substring(0, value.length() - 1), radix);
            }
            return Integer.parseInt(value, radix);
        }
    }

    public boolean isWide() {
        String value = content().toLowerCase();
        if (value.contains(".")) {
            return !value.endsWith("f");
        } else {
            return value.endsWith("l") || value.endsWith("d") ||
                    value.equals("nan") || value.equals("nand") ||
                    value.equals("infinity") || value.equals("+infinity") ||  value.equals("-infinity") ||
                    value.equals("infinityd") || value.equals("+infinityd") ||  value.equals("-infinityd");
        }
    }

    public int asInt() {
        return number().intValue();
    }

    public long asLong() {
        return number().longValue();
    }

    public float asFloat() {
        return number().floatValue();
    }

    public double asDouble() {
        return number().doubleValue();
    }

    public boolean isFloatingPoint() {
        String value = content();
        return value.contains(".") || value.endsWith("f") || value.endsWith("F") || isNaN() || isInfinity();
    }

    public boolean isNaN() {
        return Double.isNaN(asDouble()) || Float.isNaN(asFloat());
    }

    public boolean isInfinity() {
        return Double.isInfinite(asDouble()) || Float.isInfinite(asFloat());
    }
}
