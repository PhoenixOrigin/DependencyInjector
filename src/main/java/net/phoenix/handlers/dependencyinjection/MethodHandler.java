package net.phoenix.handlers.dependencyinjection;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import net.phoenix.handlers.AbstractHandler;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;

public class MethodHandler extends AbstractHandler {
    /**
     * The parameter handler.
     */
    private final ParameterHandler parameterHandler;

    /**
     * Creates a new method handler.
     *
     * @param treeMaker        The tree maker.
     * @param trees            The trees.
     * @param names            The names.
     * @param parameterHandler The parameter handler.
     */
    public MethodHandler(TreeMaker treeMaker, Trees trees, Names names, ParameterHandler parameterHandler) {
        super(trees, treeMaker, names);
        this.parameterHandler = parameterHandler;
    }

    /**
     * Handles the methods annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param element The element.
     */
    @Override
    public void handle(@NotNull Element element) {
        JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) trees.getTree(element);
        List<JCTree.JCVariableDecl> params = methodDecl.params;
        for (JCTree.JCVariableDecl param : params) {
            param.mods.annotations = param.mods.annotations.prepend(treeMaker.Annotation(treeMaker.Ident(names.fromString("Inject")), List.nil()));
        }
        parameterHandler.handle(methodDecl, (JCTree.JCClassDecl) trees.getTree(element.getEnclosingElement()));
    }

}
