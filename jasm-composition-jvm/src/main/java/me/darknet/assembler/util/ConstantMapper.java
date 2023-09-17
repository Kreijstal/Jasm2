package me.darknet.assembler.util;

import dev.xdark.blw.constant.*;
import dev.xdark.blw.type.MethodHandle;
import dev.xdark.blw.type.ObjectType;
import dev.xdark.blw.type.Type;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.helper.Handle;

public class ConstantMapper {

    public static MethodHandle fromArray(ASTArray array) {
        int kind = Handle.Kind.from(array.values().get(0).content()).ordinal();
        String name = array.values().get(1).content();
        String descriptor = array.values().get(2).content();

        var split = name.split("\\.");
        String className = split[0];
        String methodName = split[1];

        ObjectType owner = Types.instanceTypeFromInternalName(className);

        Type methodType = Types.methodType(descriptor);

        // TODO: ITF
        return new MethodHandle(kind, owner, methodName, methodType, false);
    }

    public static Constant fromConstant(ASTElement element) {
        return switch (element.type()) {
            case NUMBER -> {
                ASTNumber number = (ASTNumber) element;
                if (number.isFloatingPoint()) {
                    if (number.isWide()) {
                        yield new OfDouble(number.asDouble());
                    } else {
                        yield new OfFloat(number.asFloat());
                    }
                } else {
                    if (number.isWide()) {
                        yield new OfLong(number.asLong());
                    } else {
                        yield new OfInt(number.asInt());
                    }
                }
            }
            case STRING -> new OfString(element.value().content());
            case IDENTIFIER -> {
                ASTIdentifier identifier = (ASTIdentifier) element;
                if (identifier.content().startsWith("L")) { // is class
                    yield new OfType(Types.instanceTypeFromDescriptor(identifier.literal()));
                } else {
                    // must be method `(` or method type `L`
                    yield new OfType(Types.methodType(identifier.literal()));
                }
            }
            case ARRAY -> {
                ASTArray array = (ASTArray) element;
                yield new OfMethodHandle(fromArray(array));
            }
            default -> throw new IllegalStateException("Unexpected value: " + element.type());
        };
    }

}