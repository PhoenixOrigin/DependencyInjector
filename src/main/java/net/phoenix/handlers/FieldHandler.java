package net.phoenix.handlers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.Element;

public class FieldHandler extends Abstract {
    public FieldHandler(Trees trees, TreeMaker treeMaker, Context context) {
        super(trees, treeMaker, context);
    }

    @Override
    public void handle(Element element) {
        JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) trees.getTree(element);
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getPath(element).getCompilationUnit().getTypeDecls().get(0);

        variableDecl.init = initGen(variableDecl);

        classDecl.defs.forEach(m -> {
            if(m instanceof JCTree.JCMethodDecl methodDecl) {
                modifyMethod(methodDecl, variableDecl, classDecl);
            }
        });
    }

    private void modifyMethod(JCTree.JCMethodDecl method, JCTree.JCVariableDecl variableDecl, JCTree.JCClassDecl classDecl) {
        List<JCTree.JCExpression> exp = List.of(treeMaker.Exec(treeMaker.Assign(treeMaker.Select(treeMaker.Ident(classDecl.name), variableDecl.getName()), initGen(variableDecl))).getExpression());
        method.body.stats.prepend()
    }

    private JCTree.JCExpression initGen(JCTree.JCVariableDecl param) {
        Names names = Names.instance(context);
        JCTree.JCExpression paramTypeClassExpr = treeMaker.Select(
                treeMaker.Ident(names.fromString(param.vartype.toString())),
                names.fromString("class")
        );
        JCTree.JCMethodInvocation methodInvocation = treeMaker.Apply(
                List.nil(),
                treeMaker.Select(treeMaker.Ident(names.fromString("InjectionValues")), names.fromString("getValue")),
                List.of(paramTypeClassExpr)
        );
        return treeMaker.TypeCast(
                treeMaker.Ident(names.fromString(param.vartype.toString())),
                methodInvocation
        );
    }
}
