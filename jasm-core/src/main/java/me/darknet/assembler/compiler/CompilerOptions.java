package me.darknet.assembler.compiler;

public interface CompilerOptions<B extends CompilerOptions<?>> {

    /**
     * Set to overlay a class representation as a sort of "template" for the class,
     * If you just have a method/field/annotation in the ast, it will insert
     * it into the overlayed class.
     *
     * @param representation
     *              The representation to overlay
     */
    B overlay(ClassRepresentation representation);

    /**
     * @return The overlayed class representation
     */
    ClassRepresentation overlay();

    /**
     * Set the version of the class to compile
     * @param version The version to compile to
     * @return The options
     */
    B version(int version);

    /**
     * @return The version of the class to compile
     */
    int version();

    /**
     * Set the annotation path determines where to place annotations when edited alone,
     * The path format is as follows:
     *
     * <pre>
     *     Class target: [class name].[index]
     *     Method target: [class name].method.[method name].[method descriptor].[index]
     *     Field target: [class name].field.[field name].[field descriptor].[index]
     * </pre>
     * @param path The path to set
     * @return The options
     */
    B annotationPath(String path);

    /**
     * @return The annotation path
     */
    String annotationPath();

    /**
     * Inheritance checker used when analyzing the compiled result to fill in the local type information
     * @param checker The checker to use
     * @return The options
     */
    B inheritanceChecker(InheritanceChecker checker);

    /**
     * @return The inheritance checker
     */
    InheritanceChecker inheritanceChecker();

}
