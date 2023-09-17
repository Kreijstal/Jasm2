package me.darknet.assembler.printer;

import dev.xdark.blw.BytecodeLibrary;
import dev.xdark.blw.asm.AsmBytecodeLibrary;
import dev.xdark.blw.asm.ClassWriterProvider;
import dev.xdark.blw.classfile.ClassBuilder;
import dev.xdark.blw.classfile.ClassFileView;
import dev.xdark.blw.classfile.Field;
import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.classfile.attribute.InnerClass;
import dev.xdark.blw.type.InstanceType;
import me.darknet.assembler.util.BlwModifiers;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.io.InputStream;

public class JvmClassPrinter implements ClassPrinter {

    protected ClassFileView view;
    protected MemberPrinter memberPrinter;
    private static final BytecodeLibrary library = new AsmBytecodeLibrary(
            ClassWriterProvider.flags(ClassWriter.COMPUTE_FRAMES)
    );

    public JvmClassPrinter(InputStream stream) throws IOException {
        ClassBuilder builder = ClassBuilder.builder();
        library.read(stream, builder);
        view = builder.build();
        this.memberPrinter = new MemberPrinter(view, view, view, MemberPrinter.Type.CLASS);
    }

    @Override
    public void print(PrintContext<?> ctx) {
        memberPrinter.printAttributes(ctx);
        var superClass = view.superClass();
        if (superClass != null)
            ctx.begin().element(".super").print(superClass.internalName()).end();
        for (InstanceType anInterface : view.interfaces()) {
            ctx.begin().element(".implements").print(anInterface.internalName()).end();
        }
        for (InnerClass innerClass : view.innerClasses()) {
            var obj = ctx.begin().element(".inner")
                    .print(BlwModifiers.modifiers(innerClass.accessFlags(), BlwModifiers.CLASS)).object();
            String name = innerClass.innerName();
            if (name != null) {
                obj.value("name").literal(name).next();
            }
            obj.value("inner").print(innerClass.type().internalName()).next();
            InstanceType outer = innerClass.outerType();
            if (outer != null) {
                obj.value("outer").print(outer.internalName()).next();
            }
            obj.end();
            ctx.end();
        }
        var obj = memberPrinter.printDeclaration(ctx).element(view.type().internalName()).declObject().newline();
        for (Field field : view.fields()) {
            JvmFieldPrinter printer = new JvmFieldPrinter(field);
            printer.print(obj);
            obj.next();
        }
        obj.line();
        for (Method method : view.methods()) {
            JvmMethodPrinter printer = new JvmMethodPrinter(method);
            printer.print(obj);
            obj.doubleNext();
        }
        obj.end();
    }

    @Override
    public AnnotationPrinter annotation(int index) {
        return memberPrinter.printAnnotation(index);
    }

    @Override
    public MethodPrinter method(String name, String descriptor) {
        // find method
        for (Method method : view.methods()) {
            if (method.name().equals(name) && method.type().descriptor().equals(descriptor)) {
                return new JvmMethodPrinter(method);
            }
        }
        return null;
    }

    @Override
    public FieldPrinter field(String name, String descriptor) {
        // find field
        for (Field field : view.fields()) {
            if (field.name().equals(name) && field.type().descriptor().equals(descriptor)) {
                return new JvmFieldPrinter(field);
            }
        }
        return null;
    }
}