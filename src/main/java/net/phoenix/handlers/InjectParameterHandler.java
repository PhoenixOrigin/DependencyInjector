package net.phoenix.handlers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.Objects;

public class InjectParameterHandler extends Abstract {
    private List<JCTree.JCMethodDecl> handled = List.nil();

    public InjectParameterHandler(Trees trees, TreeMaker treeMaker, Context context) {
        super(trees, treeMaker, context);
    }

    @Override
    public void handle(Element element) {
        VariableElement parameterElement = (VariableElement) element;
        ExecutableElement methodElement = (ExecutableElement) parameterElement.getEnclosingElement();

        JCTree.JCMethodDecl originalMethodDecl = (JCTree.JCMethodDecl) trees.getTree(methodElement);

        if (handled.contains(originalMethodDecl)) {
            return;
        }
        Names names = Names.instance(context);

        JCTree.JCMethodDecl newMethodDecl = copy(originalMethodDecl);

        sortParams(newMethodDecl, names);

        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getPath(methodElement).getCompilationUnit().getTypeDecls().get(0);
        classDecl.defs = classDecl.defs.prepend(newMethodDecl);

        handled = handled.prepend(originalMethodDecl);
    }

    private boolean shouldInject(JCTree.@NotNull JCVariableDecl param) {
        for (JCTree.JCAnnotation annotation : param.mods.annotations) {
            if (Objects.equals(annotation.getAnnotationType().toString(), "InjectParameter")) {
                return true;
            }
        }
        return false;
    }

    private void sortParams(JCTree.@NotNull JCMethodDecl methodDecl, @NotNull Names names) {
        ListBuffer<JCTree.JCVariableDecl> newParams = new ListBuffer<>();
        for (JCTree.JCVariableDecl param : methodDecl.params) {
            if (shouldInject(param)) {
                methodDecl.body.stats = methodDecl.body.stats.prepend(setValue(param, names));
                continue;
            }
            newParams.append(param);
        }
        methodDecl.params = newParams.toList();
    }

    private JCTree.JCMethodDecl copy(JCTree.@NotNull JCMethodDecl method) {
        return treeMaker.MethodDef(
                method.mods,
                method.name,
                method.restype,
                method.typarams,
                method.params,
                method.thrown,
                method.body,
                method.defaultValue
        );
    }

    private JCTree.JCVariableDecl setValue(JCTree.@NotNull JCVariableDecl param, @NotNull Names names) {
        JCTree.JCExpression paramTypeClassExpr = treeMaker.Select(
                treeMaker.Ident(names.fromString(param.vartype.toString())),
                names.fromString("class")
        );
        JCTree.JCMethodInvocation methodInvocation = treeMaker.Apply(
                List.nil(),
                treeMaker.Select(treeMaker.Ident(names.fromString("InjectionValues")), names.fromString("getValue")),
                List.of(paramTypeClassExpr)
        );
        JCTree.JCExpression typeCastExpr = treeMaker.TypeCast(
                treeMaker.Ident(names.fromString(param.vartype.toString())),
                methodInvocation
        );
        return treeMaker.VarDef(
                treeMaker.Modifiers(0),
                names.fromString(param.getName().toString()),
                param.vartype,
                typeCastExpr
        );
    }
}
