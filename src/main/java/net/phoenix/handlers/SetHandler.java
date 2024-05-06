package net.phoenix.handlers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import static com.sun.tools.javac.code.Flags.PARAMETER;
import static com.sun.tools.javac.code.TypeTag.VOID;

public class SetHandler extends Abstract {
    public SetHandler(Trees trees, TreeMaker treeMaker, Context context) {
        super(trees, treeMaker, context);
    }

    @Override
    public void handle(@NotNull Element element) {
        setter(element);
    }

    void setter(@NotNull Element element) {
        JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) trees.getTree(element);
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getPath(element).getCompilationUnit().getTypeDecls().get(0);
        if (element.getModifiers().contains(Modifier.STATIC)) {
            if (element.getModifiers().contains(Modifier.PUBLIC)) {
                inject(classDecl, createStaticSetter(Flags.PUBLIC, variableDecl, classDecl));
            } else {
                inject(classDecl, createStaticSetter(0, variableDecl, classDecl));
            }
        } else {
            if (element.getModifiers().contains(Modifier.PUBLIC)) {
                inject(classDecl, createSetter(Flags.PUBLIC, variableDecl));
            } else {
                inject(classDecl, createSetter(0, variableDecl));
            }
        }
    }

    private JCTree.JCMethodDecl createStaticSetter(long flags, @NotNull JCTree.JCVariableDecl field, JCTree.@NotNull JCClassDecl classDecl) {
        Names names = Names.instance(context);

        Name name = setName(field, names);
        JCTree.JCExpression returnType = treeMaker.TypeIdent(VOID);
        List<JCTree.JCTypeParameter> typeParams = List.nil();
        List<JCTree.JCVariableDecl> params = List.of(treeMaker.VarDef(treeMaker.Modifiers(PARAMETER), field.getName(), field.vartype, null));
        List<JCTree.JCExpression> thrown = List.nil();
        List<JCTree.JCStatement> body = List.of(treeMaker.Exec(treeMaker.Assign(treeMaker.Select(treeMaker.Ident(classDecl.name), field.getName()), treeMaker.Ident(field.getName()))));
        JCTree.JCMethodDecl methodDecl = treeMaker.MethodDef(treeMaker.Modifiers(Flags.STATIC | flags), name, returnType, typeParams, params, thrown, treeMaker.Block(0, body), null);
        System.out.println(methodDecl);
        return methodDecl;
    }


    private JCTree.JCMethodDecl createSetter(long flags, @NotNull JCTree.JCVariableDecl field) {
        Names names = Names.instance(context);

        Name name = setName(field, names);
        JCTree.JCExpression returnType = treeMaker.TypeIdent(VOID);
        List<JCTree.JCTypeParameter> typeParams = List.nil();
        List<JCTree.JCVariableDecl> params = List.of(treeMaker.VarDef(treeMaker.Modifiers(PARAMETER), field.getName(), field.vartype, null));
        List<JCTree.JCExpression> thrown = List.nil();
        List<JCTree.JCStatement> body = List.of(treeMaker.Exec(treeMaker.Assign(treeMaker.Select(treeMaker.Ident(names.fromString("this")), field.getName()), treeMaker.Ident(field.getName()))));

        return treeMaker.MethodDef(treeMaker.Modifiers(flags), name, returnType, typeParams, params, thrown, treeMaker.Block(0, body), null);
    }

    private static Name setName(@NotNull JCTree.JCVariableDecl variableDecl, @NotNull Names names) {
        return names.fromString("set" + variableDecl.getName().toString().substring(0, 1).toUpperCase() + variableDecl.getName().toString().substring(1));
    }
}
