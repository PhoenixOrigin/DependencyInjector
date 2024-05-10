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

@SuppressWarnings("DuplicatedCode")
public class GetHandler extends AbstractHandler {
    public GetHandler(Trees trees, TreeMaker treeMaker, Context context) {
        super(trees, treeMaker, context);
    }

    @Override
    public void handle(@NotNull Element element) {
        getter(element);
    }

    void getter(@NotNull Element element) {
        JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) trees.getTree(element);
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getPath(element).getCompilationUnit().getTypeDecls().get(0);
        if (element.getModifiers().contains(Modifier.STATIC)) {
            if (element.getModifiers().contains(Modifier.PUBLIC)) {
                inject(classDecl, createStaticGetter(Flags.STATIC | Flags.PUBLIC, variableDecl, classDecl));
            } else {
                inject(classDecl, createStaticGetter(Flags.STATIC, variableDecl, classDecl));
            }
        } else {
            if (element.getModifiers().contains(Modifier.PUBLIC)) {
                inject(classDecl, createGetter(Flags.PUBLIC, variableDecl));
            } else {
                inject(classDecl, createGetter(0, variableDecl));
            }
        }
    }

    private JCTree.JCMethodDecl createStaticGetter(long flags, @NotNull JCTree.JCVariableDecl field, JCTree.@NotNull JCClassDecl classDecl) {
        Names names = Names.instance(context);

        Name name = getName(field, names);
        JCTree.JCExpression returnType = field.vartype;
        List<JCTree.JCTypeParameter> typeParams = List.nil();
        List<JCTree.JCVariableDecl> params = List.nil();
        List<JCTree.JCExpression> thrown = List.nil();
        List<JCTree.JCStatement> body = List.of(treeMaker.Return(treeMaker.Select(treeMaker.Ident(classDecl.name), field.getName())));
        return treeMaker.MethodDef(treeMaker.Modifiers(Flags.STATIC | flags), name, returnType, typeParams, params, thrown, treeMaker.Block(0, body), null);
    }

    private JCTree.JCMethodDecl createGetter(int flags, @NotNull JCTree.JCVariableDecl field) {
        Names names = Names.instance(context);

        Name name = getName(field, names);
        JCTree.JCExpression returnType = field.vartype;
        List<JCTree.JCTypeParameter> typeParams = List.nil();
        List<JCTree.JCVariableDecl> params = List.nil();
        List<JCTree.JCExpression> thrown = List.nil();
        List<JCTree.JCStatement> body = List.of(treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), field.getName())));

        return treeMaker.MethodDef(treeMaker.Modifiers(flags), name, returnType, typeParams, params, thrown, treeMaker.Block(0, body), null);
    }

    private Name getName(@NotNull JCTree.JCVariableDecl variableDecl, @NotNull Names names) {
        return names.fromString("get" + variableDecl.getName().toString().substring(0, 1).toUpperCase() + variableDecl.getName().toString().substring(1));
    }
}
