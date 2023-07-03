package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTLiteral;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.parser.ASTProcessor;

import java.util.function.BiConsumer;

public enum DefaultOperands implements Operands {

	STRING((context, element) -> context.isNotType(element, ElementType.STRING, "string literal")),
	INTEGER((context, element) -> {
		if(!context.isNotType(element, ElementType.NUMBER, "number literal")) {
			// verify that it's an integer
			ASTNumber number = (ASTNumber) element;
			if(number.isFloatingPoint()) {
				context.throwUnexpectedElementError("integer literal", number);
			}
		}
	}),
	IDENTIFIER((context, element) -> context.isNotType(element, ElementType.IDENTIFIER, "identifier")),
	LITERAL((context, element) -> {
		if(!(element instanceof ASTLiteral)) {
			context.throwUnexpectedElementError("literal", element);
		}
	}),
	LABEL((context, element) -> {
		if(!(element instanceof ASTLabel)) {
			context.throwUnexpectedElementError("label", element);
		}
	});

	private final Operand operand;

	DefaultOperands(Operand.Processor operand) {
		this.operand = new Operand(operand);
	}

	@Override
	public Operand getOperand() {
		return operand;
	}

}
