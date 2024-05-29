package net.phoenix.handlers.dependencyinjection;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import net.phoenix.handlers.AbstractHandler;

import javax.lang.model.element.Element;

/**
 * Handles classes annotated with {@link net.phoenix.annotations.Inject}.
 */
public class ClassHandler extends AbstractHandler {

    /**
     * The field handler.
     */
    private final FieldHandler fieldHandler;

    /**
     * Creates a new class handler.
     *
     * @param trees        The trees.
     * @param fieldHandler The field handler.
     */
    public ClassHandler(Trees trees, FieldHandler fieldHandler) {
        super(trees, null, null);
        this.trees = trees;
        this.fieldHandler = fieldHandler;
    }

    /**
     * Handles any classes annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param element The element.
     */
    @Override
    public void handle(Element element) {
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getTree(element);
        classDecl.defs.forEach(m -> {
            if (m instanceof JCTree.JCVariableDecl variableDecl) {
                fieldHandler.handle(trees.getElement(trees.getPath(trees.getPath(element).getCompilationUnit(), variableDecl)));
            }
        });
    }

}
