package me.darknet.assembler.helper;

import me.darknet.assembler.ast.primitive.ASTArray;
import org.jetbrains.annotations.Contract;

import java.util.Map;

public record Handle(Kind kind, String name, String descriptor) {

    public static Map<String, Kind> KINDS = Map.of(
            "getfield", Kind.GET_FIELD, "getstatic", Kind.GET_STATIC, "putfield", Kind.PUT_FIELD, "putstatic",
            Kind.PUT_STATIC, "invokevirtual", Kind.INVOKE_VIRTUAL, "invokestatic", Kind.INVOKE_STATIC, "invokespecial",
            Kind.INVOKE_SPECIAL, "newinvokespecial", Kind.NEW_INVOKE_SPECIAL, "invokeinterface", Kind.INVOKE_INTERFACE
    );

    @Contract(pure = true)
    public static Handle from(ASTArray array) {
        // assert that the array has 3 elements
        Handle.Kind kind = Handle.Kind.valueOf(array.values().get(0).content());
        String name = array.values().get(1).content();
        String descriptor = array.values().get(2).content();

        return new Handle(kind, name, descriptor);
    }

    public enum Kind {
        GET_FIELD,
        GET_STATIC,
        PUT_FIELD,
        PUT_STATIC,
        INVOKE_VIRTUAL,
        INVOKE_STATIC,
        INVOKE_SPECIAL,
        NEW_INVOKE_SPECIAL,
        INVOKE_INTERFACE
    }

}
