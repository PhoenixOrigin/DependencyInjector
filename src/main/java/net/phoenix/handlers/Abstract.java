package net.phoenix.handlers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;

public abstract class Abstract {
    final Trees trees;
    final TreeMaker treeMaker;
    final Context context;

    public Abstract(Trees trees, TreeMaker treeMaker, Context context) {
        this.trees = trees;
        this.treeMaker = treeMaker;
        this.context = context;
    }

    public abstract void handle(Element element);

    void inject(JCTree.@NotNull JCClassDecl classDecl, JCTree.JCMethodDecl methodDecl) {
        classDecl.defs = classDecl.defs.prepend(methodDecl);
    }
}
