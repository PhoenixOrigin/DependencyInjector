package net.phoenix.handlers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;

import javax.lang.model.element.Element;

/**
 * Abstract handler for annotation processors, extended and implemented for each annotation.
 *
 * @author Phoenix
 */
public abstract class AbstractHandler {
    final Trees trees;
    final TreeMaker treeMaker;
    final Context context;

    public AbstractHandler(Trees trees, TreeMaker treeMaker, Context context) {
        this.trees = trees;
        this.treeMaker = treeMaker;
        this.context = context;
    }

    public abstract void handle(Element element);
}
