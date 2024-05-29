package net.phoenix.handlers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.Element;

/**
 * An abstract handler.
 *
 * @author Phoenix
 */
public abstract class AbstractHandler {

    public Trees trees;
    public TreeMaker treeMaker;
    public Names names;

    /**
     * Creates a new abstract handler.
     *
     * @param trees     The trees.
     * @param treeMaker The tree maker.
     * @param names     The names.
     */
    public AbstractHandler(Trees trees, TreeMaker treeMaker, Names names) {
        this.trees = trees;
        this.treeMaker = treeMaker;
        this.names = names;
    }

    /**
     * Handles the element.
     *
     * @param element The element.
     */
    public abstract void handle(Element element);

}
