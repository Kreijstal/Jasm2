package me.darknet.assembler.printer;

import dev.xdark.blw.code.*;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.type.*;
import me.darknet.assembler.util.IndexedExecutionEngine;
import me.darknet.assembler.util.LabelUtil;

import java.util.HashMap;
import java.util.Map;

public class InstructionPrinter implements IndexedExecutionEngine {

    private final static String[] OPCODES = new String[256];

    static {
        for (var field : Opcodes.class.getFields()) {
            try {
                OPCODES[field.getInt(null)] = field.getName().toLowerCase();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    protected PrintContext.CodePrint ctx;
    protected Code code;
    protected Map<Integer, String> labelNames = new HashMap<>();
    protected Names names;
    private int currentIndex = 0;

    public InstructionPrinter(PrintContext.CodePrint ctx, Code code, Names names) {
        this.ctx = ctx;
        this.code = code;
        this.names = names;

        // map labels
        int index = 0;
        for (CodeElement codeElement : code.elements()) {
            if (codeElement instanceof Label label) {
                labelNames.put(label.index(), LabelUtil.getLabelName(index++));
            }
        }
    }

    @Override
    public void index(int index) {
        currentIndex = index;
    }

    @Override
    public void label(Label label) {
        ctx.label(labelNames.get(label.index())).next();
    }

    @Override
    public void execute(SimpleInstruction instruction) {
        ctx.instruction(OPCODES[instruction.opcode()]).next();
    }

    @Override
	public void execute(ConstantInstruction<?> instruction) {
		String opcode;
		switch (instruction) {
			case ConstantInstruction.Int i -> {
				// decide on opcode
				int val = i.constant().value();
				if (val == -1) {
					ctx.instruction("iconst_m1").next();
					return;
				} else if (val >= 0 && val <= 5) {
					ctx.instruction("iconst_" + val).next();
					return;
				} else if (val >= -128 && val <= 127) {
					opcode = "bipush";
				} else if (val >= -32768 && val <= 32767) {
					opcode = "sipush";
				} else {
					opcode = "ldc";
				}
			}
			case ConstantInstruction.Long i -> {
				long val = i.constant().value();
				if (val == 0 || val == 1) {
					ctx.instruction("lconst_" + val).next();
					return;
				} else {
					opcode = "ldc2_w";
				}
			}
			case ConstantInstruction.Float i -> {
				float val = i.constant().value();
				if (val == 0 || val == 1 || val == 2) {
					ctx.instruction("fconst_" + (int) val).next();
					return;
				} else {
					opcode = "ldc";
				}
			}
			case ConstantInstruction.Double i -> {
				double val = i.constant().value();
				if (val == 0 || val == 1) {
					ctx.instruction("dconst_" + (int) val).next();
					return;
				} else {
					opcode = "ldc2_w";
				}
			}
			default -> opcode = "ldc";
		}
		ctx.instruction(opcode);
		instruction.constant().accept(new ConstantPrinter(ctx));
		ctx.next();
	}

    @Override
    public void execute(VarInstruction instruction) {
        ctx.instruction(OPCODES[instruction.opcode()])
                .literal(names.getName(instruction.variableIndex(), currentIndex + 1)).next();
    }

    @Override
    public void execute(LookupSwitchInstruction instruction) {
        var obj = ctx.instruction("lookupswitch").object();
        obj.value("default").print(labelNames.get(instruction.defaultTarget().index())).next();
        for (int i = 0; i < instruction.keys().length; i++) {
            Label target = instruction.targets().get(i);
            int key = instruction.keys()[i];
            obj.value(String.valueOf(key)).print(labelNames.get(target.index())).next();
        }
        obj.end();
        ctx.next();
    }

    @Override
    public void execute(TableSwitchInstruction instruction) {
        var obj = ctx.instruction("tableswitch").object();
        obj.value("min").print(String.valueOf(instruction.min())).next();
        obj.value("max").print(String.valueOf(instruction.min() + instruction.targets().size())).next();
        var arr = obj.value("targets").array();
        for (Label target : instruction.targets()) {
            arr.print(labelNames.get(target.index())).arg();
        }
        arr.end();
        obj.value("default").print(labelNames.get(instruction.defaultTarget().index()));
    }

    @Override
    public void execute(InstanceofInstruction instruction) {
        ctx.instruction("instanceof").literal(instruction.type().descriptor()).next();
    }

    @Override
    public void execute(CheckCastInstruction instruction) {
        ctx.instruction("checkcast").literal(instruction.type().descriptor()).next();
    }

    @Override
    public void execute(AllocateInstruction instruction) {
        Type type = instruction.type();
        if (type instanceof InstanceType instance) {
            ctx.instruction("new").literal(instance.internalName()).next();
        } else {
            ArrayType arrayType = (ArrayType) type;
            String descriptor = arrayType.descriptor();
            int dimensions = arrayType.dimensions();
            if (dimensions == 1) {
                ClassType component = arrayType.componentType();
                if (!(component instanceof PrimitiveType primitiveType)) {
                    ctx.instruction("anewarray").literal(component.descriptor()).next();
                } else {
                    ctx.instruction("newarray").print(primitiveType.name()).next();
                }
            } else {
                ctx.instruction("multianewarray").literal(descriptor).arg().print(Integer.toString(dimensions)).next();
            }
        }
    }

    @Override
    public void execute(MethodInstruction instruction) {
        String opcode = OPCODES[instruction.opcode()];
        if (instruction.isInterface() && instruction.opcode() != Opcodes.INVOKEINTERFACE) {
            opcode += "interface";
        }
        ctx.instruction(opcode).literal(instruction.owner().internalName()).print(".").literal(instruction.name())
                .print(" ").literal(instruction.type().descriptor()).next();
    }

    @Override
    public void execute(FieldInstruction instruction) {
        ctx.instruction(OPCODES[instruction.opcode()]).literal(instruction.owner().internalName()).print(".")
                .literal(instruction.name()).print(" ").literal(instruction.type().descriptor()).next();
    }

    @Override
    public void execute(InvokeDynamicInstruction instruction) {
        ctx.instruction("invokedynamic").literal(instruction.name()).arg().literal(instruction.type().descriptor())
                .arg();
        ConstantPrinter.printMethodHandle(instruction.bootstrapHandle(), ctx);
        var bsmArray = ctx.arg().array();
        ConstantPrinter printer = new ConstantPrinter(bsmArray);
        for (var arg : instruction.args()) {
            arg.accept(printer);
            bsmArray.arg();
        }
        bsmArray.end();
        ctx.next();
    }

    @Override
    public void execute(ImmediateJumpInstruction instruction) {
        ctx.instruction(OPCODES[instruction.opcode()]).print(labelNames.get(instruction.target().index())).next();
    }

    @Override
    public void execute(ConditionalJumpInstruction instruction) {
        ctx.instruction(OPCODES[instruction.opcode()]).print(labelNames.get(instruction.target().index())).next();
    }

    @Override
    public void execute(VariableIncrementInstruction instruction) {
        ctx.instruction(OPCODES[instruction.opcode()]).arg()
                .literal(names.getName(instruction.variableIndex(), currentIndex + 1)).arg()
                .literal(Integer.toString(instruction.incrementBy())).next();
    }

    @Override
    public void execute(Instruction instruction) {

    }
}