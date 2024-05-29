package net.phoenix.util;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class.
 * Contains utility methods.
 *
 * @author Phoenix
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "unused"})
public class Util {
    /**
     * Generates the initialization of a parameter.
     *
     * @param param     The parameter.
     * @param treeMaker The tree maker.
     * @param names     The names.
     * @return The initialization of the parameter.
     */
    public static JCTree.JCExpression initGen(@NotNull JCTree.JCVariableDecl param, TreeMaker treeMaker, Names names) {
        JCTree.JCExpression paramTypeClassExpr = treeMaker.Select(
                treeMaker.Ident(names.fromString(param.vartype.toString())),
                names.fromString("class")
        );
        JCTree.JCMethodInvocation methodInvocation;
        if (param.mods.annotations.stream().anyMatch(a -> !a.getArguments().isEmpty())) {
            JCTree.JCAnnotation annotation = param.mods.annotations.stream().filter(a -> a.getAnnotationType().toString().equals("Inject")).findFirst().get();
            JCTree.JCLiteral key = (JCTree.JCLiteral) ((JCTree.JCAssign) annotation.getArguments().get(0)).rhs;
            methodInvocation = treeMaker.Apply(
                    List.nil(),
                    treeMaker.Select(treeMaker.Ident(names.fromString("DIValues")), names.fromString("getValue")),
                    List.of(paramTypeClassExpr, key)
            );
        } else {
            methodInvocation = treeMaker.Apply(
                    List.nil(),
                    treeMaker.Select(treeMaker.Ident(names.fromString("DIValues")), names.fromString("getValue")),
                    List.of(paramTypeClassExpr)
            );
        }
        return treeMaker.TypeCast(
                treeMaker.Ident(names.fromString(param.vartype.toString())),
                methodInvocation
        );
    }

    /**
     * Adds an import for DIValues to the class
     *
     * @param compilationUnit The class's compilation unit
     * @param treeMaker       The tree maker
     * @param names           The names
     */
    public static void importDIValues(JCTree.JCCompilationUnit compilationUnit, TreeMaker treeMaker, Names names) {
        JCTree.JCFieldAccess importClass = treeMaker.Select(
                treeMaker.Ident(names.fromString("net.phoenix.util")),
                names.fromString("DIValues")
        );
        importJCFA(importClass, compilationUnit, treeMaker);
    }

    /**
     * Adds an import for a class to the class
     *
     * @param importClass     The class to import
     * @param compilationUnit The class's compilation unit
     * @param treeMaker       The tree maker
     */
    private static void importJCFA(JCTree.JCFieldAccess importClass, JCTree.JCCompilationUnit compilationUnit, TreeMaker treeMaker) {
        JCTree.JCImport importStatement = treeMaker.Import(importClass, false);
        List<JCTree> def = List.nil();
        def = def.append(compilationUnit.defs.get(0));
        def = def.append(importStatement);
        for (int i = 1; i < compilationUnit.defs.size(); i++) {
            def = def.append(compilationUnit.defs.get(i));
        }

        compilationUnit.defs = def;
    }
}