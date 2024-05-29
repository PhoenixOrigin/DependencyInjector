package net.phoenix.handlers.dependencyinjection;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import net.phoenix.handlers.AbstractHandler;
import net.phoenix.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;

/**
 * A field handler that handles the fields annotated with {@link net.phoenix.annotations.Inject}.
 *
 * @author Phoenix
 */
public class FieldHandler extends AbstractHandler {
    /**
     * Creates a new field handler.
     *
     * @param treeMaker The tree maker.
     * @param trees     The trees.
     * @param names     The names.
     */
    public FieldHandler(TreeMaker treeMaker, Trees trees, Names names) {
        super(trees, treeMaker, names);
    }

    /**
     * Handles the fields annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param element The element.
     */
    @Override
    public void handle(Element element) {
        JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) trees.getTree(element);
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getPath(element).getCompilationUnit().getTypeDecls().get(0);
        variableDecl.init = Util.initGen(variableDecl, treeMaker, names);
        classDecl.defs.forEach(m -> {
            JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) trees.getPath(element).getCompilationUnit();
            Util.importDIValues(compilationUnit, treeMaker, names);
            if (m instanceof JCTree.JCMethodDecl methodDecl)
                if (new TreeChecker(variableDecl.getName()).check(methodDecl)) {
                    methodDecl.body.stats = methodDecl.body.stats.prepend(treeMaker.Exec(treeMaker.Assign(treeMaker.Select(treeMaker.Ident(classDecl.name), variableDecl.getName()), Util.initGen(variableDecl, treeMaker, names))));
                }
        });
    }

    /**
     * A util tree scanner that checks if a variable is referenced in a method.
     */
    static class TreeChecker extends TreeScanner {
        Name n;
        boolean referenced = false;

        public TreeChecker(Name n) {
            this.n = n;
        }

        public boolean check(JCTree.JCMethodDecl tree) {
            scan(tree);
            return referenced;
        }

        @Override
        public void visitIdent(@NotNull JCTree.JCIdent jcIdent) {
            if (jcIdent.name.equals(n)) referenced = true;
            super.visitIdent(jcIdent);
        }
    }
}
