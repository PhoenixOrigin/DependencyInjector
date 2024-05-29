package net.phoenix.handlers.dependencyinjection;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;
import net.phoenix.handlers.AbstractHandler;
import net.phoenix.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.Objects;

/**
 * Handles parameters annotated with {@link net.phoenix.annotations.Inject}.
 *
 * @author Phoenix
 */
public class ParameterHandler extends AbstractHandler {
    /**
     * The handled methods.
     */
    private List<JCTree.JCMethodDecl> handled;

    /**
     * Creates a new parameter handler.
     *
     * @param treeMaker The tree maker.
     * @param trees     The trees.
     * @param names     The names.
     * @param handled   The handled methods.
     */
    public ParameterHandler(TreeMaker treeMaker, Trees trees, Names names, List<JCTree.JCMethodDecl> handled) {
        super(trees, treeMaker, names);
        this.handled = handled;
    }

    /**
     * Handles the parameters annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param parameterElement The parameter element.
     */
    @Override
    public void handle(@NotNull Element parameterElement) {
        ExecutableElement methodElement = (ExecutableElement) parameterElement.getEnclosingElement();
        JCTree.JCMethodDecl originalMethodDecl = (JCTree.JCMethodDecl) trees.getTree(methodElement);
        JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) trees.getPath(parameterElement).getCompilationUnit();
        Util.importDIValues(compilationUnit, treeMaker, names);
        handle(originalMethodDecl, getClassDecl((VariableElement) parameterElement));
    }

    /**
     * Gets the class declaration
     * of the parameter element.
     *
     * @param parameterElement The parameter element.
     * @return The class declaration.
     */
    private JCTree.JCClassDecl getClassDecl(@NotNull VariableElement parameterElement) {
        ExecutableElement methodElement = (ExecutableElement) parameterElement.getEnclosingElement();
        Element classElement = methodElement.getEnclosingElement();
        return (JCTree.JCClassDecl) trees.getTree(classElement);
    }

    /**
     * Handles the method declaration.
     *
     * @param methodDecl The method declaration.
     * @param classDecl  The class declaration.
     */
    void handle(JCTree.@NotNull JCMethodDecl methodDecl, JCTree.@NotNull JCClassDecl classDecl) {
        if (handled.contains(methodDecl)) return;
        JCTree.JCMethodDecl newMethodDecl = copy(methodDecl);
        sortParams(newMethodDecl, names);
        List<JCTree> defs = List.nil();
        for (JCTree def : classDecl.defs) {
            if (def instanceof JCTree.JCMethodDecl m) {
                if (m.name.equals(methodDecl.name)) {
                    defs = defs.prepend(newMethodDecl);
                } else {
                    defs = defs.prepend(m);
                }
            } else {
                defs = defs.prepend(def);
            }
        }
        classDecl.defs = defs.reverse();
        classDecl.defs = classDecl.defs.prepend(newMethodDecl);
        handled = handled.prepend(newMethodDecl);
    }

    /**
     * Copies the method declaration.
     *
     * @param method The method declaration.
     * @return The copied method declaration.
     */
    private JCTree.JCMethodDecl copy(@NotNull JCTree.JCMethodDecl method) {
        return treeMaker.MethodDef(method.mods, method.name, method.restype, method.typarams, method.params, method.thrown, method.body, method.defaultValue);
    }

    /**
     * Sorts the parameters. It will take the parameters annotated with {@link net.phoenix.annotations.Inject} and inject them into the method body.
     *
     * @param methodDecl The method declaration.
     * @param names      The names.
     */
    private void sortParams(@NotNull JCTree.JCMethodDecl methodDecl, @NotNull Names names) {
        ListBuffer<JCTree.JCVariableDecl> newParams = new ListBuffer<>();
        for (JCTree.JCVariableDecl param : methodDecl.params) {
            if (shouldInject(param)) {
                methodDecl.body.stats = methodDecl.body.stats.prepend(treeMaker.VarDef(treeMaker.Modifiers(0), names.fromString(param.getName().toString()), param.vartype, Util.initGen(param, treeMaker, names)));
                continue;
            }
            newParams.append(param);
        }
        methodDecl.params = newParams.toList();
    }

    /**
     * Checks if the parameter should be injected.
     *
     * @param param The parameter.
     * @return If the parameter should be injected.
     */
    private boolean shouldInject(@NotNull JCTree.JCVariableDecl param) {
        for (JCTree.JCAnnotation annotation : param.mods.annotations) {
            if (Objects.equals(annotation.getAnnotationType().toString(), "Inject")) {
                return true;
            }
        }
        return false;
    }
}
