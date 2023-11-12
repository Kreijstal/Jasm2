package me.darknet.assembler.instructions.jvm;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.helper.Handle;
import me.darknet.assembler.instructions.Operand;
import me.darknet.assembler.instructions.Operands;
import me.darknet.assembler.parser.processor.ASTProcessor;

import java.util.List;

public enum JvmOperands implements Operands {

    CONSTANT(JvmOperands::verifyConstant),
    TABLE_SWITCH((context, element) -> {
        // table switch can be: default label, low value, high value and labels
        ASTObject object = context.validateObject(element, "table switch", element, "min", "max", "default", "cases");

        if (object == null)
            return;

        // start, end should be numbers
        if (context.validateCorrect(object.value("min"), ElementType.NUMBER, "number", object))
            return;
        if (context.validateCorrect(object.value("max"), ElementType.NUMBER, "number", object))
            return;

        ASTNumber min = object.value("min");
        ASTNumber max = object.value("max");

        if(min.isFloatingPoint())
            context.throwUnexpectedElementError("integer literal", min);
        if(max.isFloatingPoint())
            context.throwUnexpectedElementError("integer literal", max);

        // default should be identifier
        if (context.validateCorrect(object.value("default"), ElementType.IDENTIFIER, "identifier",
                object))
            return;

        // cases should be array
        ASTArray array = context.validateEmptyableElement(object.value("cases"), ElementType.ARRAY, "cases", object);
        if (array == null)
            return;

        context.validateArray(array, ElementType.IDENTIFIER, "label", element);
    }),
    LOOKUP_SWITCH((context, element) -> {
        // lookup switch can be: default label, pairs
        if (context.isNotType(element, ElementType.OBJECT, "lookup switch"))
            return;

        ASTObject object = (ASTObject) element;
        if (object.values().size() < 1) {
            context.throwUnexpectedElementError("default label", element);
            return;
        }

        // default should be identifier
        if (context.validateCorrect(object.value("default"), ElementType.IDENTIFIER, "identifier",
                object))
            return;
        for (ASTElement elem : object.values().elements()) {
            if (context.isNotType(elem, ElementType.IDENTIFIER, "identifier"))
                return;
        }
    }),
    HANDLE(JvmOperands::verifyHandle),
    ARGS((context, element) -> {
        ASTArray array = context.validateEmptyableElement(element, ElementType.ARRAY, "args", element);
        for (ASTElement value : array.values()) {
            if (context.isNull(value, "args element", array.location()))
                return;
            assert value != null;
            JvmOperands.verifyConstant(context, value);
        }
    }),
    TYPE(((context, element) -> context.isNotType(element, ElementType.IDENTIFIER, "type"))),
    NEW_ARRAY_TYPE(((context, element) -> {
        if (context.isNotType(element, ElementType.IDENTIFIER, "new array type"))
            return;
        ASTIdentifier identifier = (ASTIdentifier) element;
        switch (identifier.content()) {
            case "boolean", "byte", "char", "short", "int", "float", "long", "double" -> {
            }
            default -> context
                    .throwUnexpectedElementError("boolean, byte, char, short, int, float, long or double", element);
        }
    }));

    private final Operand operand;

    JvmOperands(Operand.Processor operand) {
        this.operand = new Operand(operand);
    }

    public static void verifyConstant(ASTProcessor.ParserContext context, ASTElement element) {
        switch (element.type()) {
            case NUMBER, STRING -> {
            }
            case IDENTIFIER -> {
                char first = element.content().charAt(0);
                switch (first) {
                    case 'L', '(', '[' -> {}
                    default -> context.throwUnexpectedElementError("class, method or array descriptor", element);
                }
            }
            case ARRAY -> {
                ASTArray array = (ASTArray) element;
                ASTElement last = array.values().get(array.values().size() - 1);
                if(last == null) {
                    context.throwUnexpectedElementError("constant", element);
                    return;
                }
                switch (last.type()) {
                    case ARRAY -> verifyConstantDynamic(context, array);
                    case IDENTIFIER -> verifyHandle(context, array);
                }
            }
            default -> context.throwUnexpectedElementError("constant", element);
        }
    }

    public static void verifyConstantDynamic(ASTProcessor.ParserContext context, ASTArray array) {
        // constant dynamic structure: { name, type, { <handle > }, { <args> } }
        if(array.values().size() != 4) {
            context.throwUnexpectedElementError("name, type, handle and args", array);
            return;
        }

        if (context.validateCorrect(array.value(0), ElementType.IDENTIFIER, "name", array))
            return;

        if (context.validateCorrect(array.value(1), ElementType.IDENTIFIER, "type", array))
            return;

        if(verifyHandle(context, array.value(2)))
            return;

        ASTElement argsElement = array.value(3);
        ASTArray args = context.validateEmptyableElement(argsElement, ElementType.ARRAY, "args", argsElement);
        for (ASTElement value : args.values()) {
            if (context.isNull(value, "args element", args.location()))
                return;
            assert value != null;
            verifyConstant(context, value);
        }
    }

    public static boolean verifyHandle(ASTProcessor.ParserContext context, ASTElement element) {
        if (context.isNotType(element, ElementType.ARRAY, "handle"))
            return true;

        ASTArray array = (ASTArray) element;
        List<ASTIdentifier> values = context.validateArray(array, ElementType.IDENTIFIER, "handle element", element);
        if (values.size() != 3) {
            context.throwUnexpectedElementError("kind, name and descriptor", element);
            return true;
        }
        // first should be kind
        if (!Handle.KINDS.containsKey(values.get(0).content())) {
            context.throwUnexpectedElementError("kind", values.get(0));
            return true;
        }

        return false;
    }

    @Override
    public Operand getOperand() {
        return operand;
    }
}
