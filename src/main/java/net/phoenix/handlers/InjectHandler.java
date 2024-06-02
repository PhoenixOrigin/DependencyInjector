package net.phoenix.handlers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import net.phoenix.handlers.dependencyinjection.ClassHandler;
import net.phoenix.handlers.dependencyinjection.FieldHandler;
import net.phoenix.handlers.dependencyinjection.MethodHandler;
import net.phoenix.handlers.dependencyinjection.ParameterHandler;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * Handler for the {@link net.phoenix.annotations.Inject} annotation.
 * Injects values from {@link net.phoenix.util.DIValues#getValue(Class)} into fields, parameters, classes or methods annotated with {@link net.phoenix.annotations.Inject}.
 *
 * @author Phoenix
 * @see net.phoenix.annotations.Inject
 * @see net.phoenix.util.DIValues
 */
public class InjectHandler {

    /**
     * The field handler.
     */
    private final @NotNull FieldHandler fieldHandler;
    /**
     * The parameter handler.
     */
    private final @NotNull ParameterHandler parameterHandler;
    /**
     * The class handler.
     */
    private final @NotNull ClassHandler classHandler;
    /**
     * The method handler.
     */
    private final @NotNull MethodHandler methodHandler;

    /**
     * Creates a new inject handler.
     *
     * @param trees     The trees.
     * @param treeMaker The tree maker.
     * @param context   The context.
     */
    public InjectHandler(Trees trees, TreeMaker treeMaker, @NotNull Context context) {
        Names names = Names.instance(context);
        this.fieldHandler = new FieldHandler(treeMaker, trees, names);
        this.parameterHandler = new ParameterHandler(treeMaker, trees, names, List.nil());
        this.classHandler = new ClassHandler(trees, fieldHandler);
        this.methodHandler = new MethodHandler(treeMaker, trees, names, parameterHandler);
    }

    /**
     * Handles the element, injecting values from {@link net.phoenix.util.DIValues#getValue(Class)} into fields, parameters, classes or methods annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param element The element to handle.
     */
    public void handle(@NotNull Element element) {
        if (element.getKind() == ElementKind.FIELD) {
            fieldHandler.handle(element);
        } else if (element.getKind() == ElementKind.PARAMETER) {
            parameterHandler.handle(element);
        } else if (element.getKind() == ElementKind.CLASS) {
            classHandler.handle(element);
        } else if (element.getKind() == ElementKind.METHOD) {
            methodHandler.handle(element);
        } else {
            throw new IllegalArgumentException("Unsupported element kind: " + element.getKind());
        }
    }
}
