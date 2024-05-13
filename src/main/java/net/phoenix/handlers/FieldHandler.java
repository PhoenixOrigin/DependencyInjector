package net.phoenix.handlers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;

public class FieldHandler extends AbstractHandler {
    public FieldHandler(Trees trees, TreeMaker treeMaker, Context context) {
        super(trees, treeMaker, context);
    }

    @Override
    public void handle(Element element) {
        JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) trees.getTree(element);
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getPath(element).getCompilationUnit().getTypeDecls().get(0);

        variableDecl.init = initGen(variableDecl);

        classDecl.defs.forEach(m -> {
            if (m instanceof JCTree.JCMethodDecl methodDecl) {
                if (shouldInject(methodDecl, variableDecl)) modifyMethod(methodDecl, variableDecl, classDecl);
            }
        });
    }

    private boolean shouldInject(JCTree.JCMethodDecl methodDecl, JCTree.@NotNull JCVariableDecl variableDecl) {
        final boolean[] isReferenced = {false};
        TreeScanner scanner = new TreeScanner() {
            private final Name name;

            {
                name = variableDecl.getName();
            }

            @Override
            public void visitIdent(JCTree.@NotNull JCIdent jcIdent) {
                if (jcIdent.name.equals(name)) {
                    isReferenced[0] = true;
                }
                super.visitIdent(jcIdent);
            }

        };
        scanner.scan(methodDecl);
        return isReferenced[0];
    }

    private void modifyMethod(JCTree.@NotNull JCMethodDecl method, JCTree.@NotNull JCVariableDecl variableDecl, JCTree.@NotNull JCClassDecl classDecl) {
        method.body.stats = method.body.stats.prepend(treeMaker.Exec(treeMaker.Assign(treeMaker.Select(treeMaker.Ident(classDecl.name), variableDecl.getName()), initGen(variableDecl))));
    }

    private JCTree.JCExpression initGen(JCTree.@NotNull JCVariableDecl param) {
        Names names = Names.instance(context);
        JCTree.JCExpression paramTypeClassExpr = treeMaker.Select(
                treeMaker.Ident(names.fromString(param.vartype.toString())),
                names.fromString("class")
        );
        JCTree.JCMethodInvocation methodInvocation = treeMaker.Apply(
                List.nil(),
                treeMaker.Select(treeMaker.Ident(names.fromString("DIValues")), names.fromString("getValue")),
                List.of(paramTypeClassExpr)
        );
        return treeMaker.TypeCast(
                treeMaker.Ident(names.fromString(param.vartype.toString())),
                methodInvocation
        );
    }
}
