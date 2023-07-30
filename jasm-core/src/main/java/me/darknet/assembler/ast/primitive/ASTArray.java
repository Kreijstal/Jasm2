package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ASTArray extends ASTElement {

    private final List<@Nullable ASTElement> values;

    public ASTArray(List<@Nullable ASTElement> values) {
        super(ElementType.ARRAY, values);
        this.values = values;
    }

    public List<@Nullable ASTElement> getValues() {
        return values;
    }

}
