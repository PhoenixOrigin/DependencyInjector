package net.phoenix.handlers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.Element;

public class SneakyThrowHandler extends AbstractHandler {
    public SneakyThrowHandler(Trees trees, TreeMaker treeMaker, Context context) {
        super(trees, treeMaker, context);
    }

    @Override
    public void handle(Element element) {
        JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) trees.getTree(element);
        Names names = Names.instance(context);
        JCTree.JCBlock body = methodDecl.body;
        if (body == null) {
            return;
        }

        for(JCTree.JCStatement statement : body.stats) {
            if (statement instanceof JCTree.JCThrow throwStatement) {
                List<JCTree.JCStatement> copy = List.nil();
                for (JCTree.JCStatement stat : body.stats) {
                    if(stat == statement) {
                        copy = copy.append(treeMaker.Throw(
                                treeMaker.Apply(
                                        com.sun.tools.javac.util.List.nil(),
                                        treeMaker.Select(
                                                treeMaker.Ident(names.fromString("JavaPlusPlus")),
                                                names.fromString("sneakyThrow")
                                        ),
                                        com.sun.tools.javac.util.List.of(throwStatement.getExpression())
                                )
                        ));
                        continue;
                    }
                    copy = copy.append(stat);
                }
                body.stats = copy;
            }
        }
        System.out.println(body);
    }

}
