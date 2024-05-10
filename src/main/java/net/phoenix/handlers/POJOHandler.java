package net.phoenix.handlers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import net.phoenix.annotations.Exclude;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import static com.sun.tools.javac.code.Flags.PARAMETER;
import static com.sun.tools.javac.code.TypeTag.VOID;

@SuppressWarnings("DuplicatedCode")
public class POJOHandler extends AbstractHandler {
    public POJOHandler(Trees trees, TreeMaker treeMaker, Context context) {
        super(trees, treeMaker, context);
    }

    @Override
    public void handle(@NotNull Element element) {
        element.getEnclosedElements().stream()
                .filter(e -> e.getKind().isField())
                .forEach(fieldElement -> {
                    if (fieldElement.getAnnotation(Exclude.class) != null) {
                        return;
                    }
                    if (!fieldElement.getModifiers().contains(Modifier.FINAL)) {
                        setter(fieldElement);
                    }
                    getter(fieldElement);
                });
        toString((JCTree.JCClassDecl) trees.getTree(element));
        constructor((JCTree.JCClassDecl) trees.getTree(element));
    }

    void constructor(JCTree.@NotNull JCClassDecl classDecl) {
        Names names = Names.instance(context);

        List<JCTree.JCVariableDecl> fields = getFields(classDecl);
        List<JCTree.JCStatement> body = List.nil();
        List<JCTree.JCVariableDecl> params = List.nil();
        for (JCTree.JCVariableDecl field : fields) {
            JCTree.JCVariableDecl param = treeMaker.VarDef(treeMaker.Modifiers(PARAMETER), field.getName(), field.vartype, null);
            params = params.append(param);
            body = body.append(treeMaker.Exec(treeMaker.Assign(treeMaker.Select(treeMaker.Ident(names.fromString("this")), field.getName()), treeMaker.Ident(field.getName()))));
        }
        JCTree.JCBlock block = treeMaker.Block(0, body);
        JCTree.JCMethodDecl methodDecl = treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC), names.init, null, List.nil(), params, List.nil(), block, null);

        for (JCTree def : classDecl.defs) {
            if (def instanceof JCTree.JCMethodDecl method) {
                if (method.getName().toString().equals("<init>")) {
                    classDecl.defs.remove(methodDecl);
                    return;
                }
            }
        }
        inject(classDecl, methodDecl);
    }

    void toString(JCTree.@NotNull JCClassDecl classDecl) {
        Names names = Names.instance(context);
        List<JCTree.JCVariableDecl> fields = getFields(classDecl);
        List<JCTree.JCStatement> body = List.nil();
        body = body.append(treeMaker.VarDef(treeMaker.Modifiers(0), names.fromString("sb"), treeMaker.Ident(names.fromString("StringBuilder")), treeMaker.NewClass(null, List.nil(), treeMaker.Ident(names.fromString("StringBuilder")), List.nil(), null)));
        body = body.append(treeMaker.Exec(treeMaker.Apply(List.nil(), treeMaker.Select(treeMaker.Ident(names.fromString("sb")), names.fromString("append")), List.of(treeMaker.Literal("{")))));
        boolean first = true;
        for (JCTree.JCVariableDecl field : fields) {
            if (!first) {
                body = body.append(treeMaker.Exec(treeMaker.Apply(List.nil(), treeMaker.Select(treeMaker.Ident(names.fromString("sb")), names.fromString("append")), List.of(treeMaker.Literal(",")))));
            } else {
                first = false;
            }

            body = body.append(treeMaker.Exec(treeMaker.Apply(List.nil(), treeMaker.Select(treeMaker.Ident(names.fromString("sb")), names.fromString("append")), List.of(treeMaker.Literal("\"" + field.getName().toString() + "\": ")))));
            if (field.vartype.toString().equals("boolean") || field.vartype.toString().equals("int") || field.vartype.toString().equals("long")) {
                body = body.append(treeMaker.Exec(treeMaker.Apply(List.nil(), treeMaker.Select(treeMaker.Ident(names.fromString("sb")), names.fromString("append")), List.of(treeMaker.Select(treeMaker.Ident(names.fromString("this")), field.getName())))));
            } else {
                body = body.append(treeMaker.Exec(treeMaker.Apply(List.nil(), treeMaker.Select(treeMaker.Ident(names.fromString("sb")), names.fromString("append")), List.of(treeMaker.Literal("\"")))));
                body = body.append(treeMaker.Exec(treeMaker.Apply(List.nil(), treeMaker.Select(treeMaker.Ident(names.fromString("sb")), names.fromString("append")), List.of(treeMaker.Select(treeMaker.Ident(names.fromString("this")), field.getName())))));
                body = body.append(treeMaker.Exec(treeMaker.Apply(List.nil(), treeMaker.Select(treeMaker.Ident(names.fromString("sb")), names.fromString("append")), List.of(treeMaker.Literal("\"")))));
            }
        }
        body = body.append(treeMaker.Exec(treeMaker.Apply(List.nil(), treeMaker.Select(treeMaker.Ident(names.fromString("sb")), names.fromString("append")), List.of(treeMaker.Literal("}")))));
        body = body.append(treeMaker.Return(treeMaker.Apply(List.nil(), treeMaker.Select(treeMaker.Ident(names.fromString("sb")), names.fromString("toString")), List.nil())));
        JCTree.JCBlock block = treeMaker.Block(0, body);
        JCTree.JCMethodDecl methodDecl = treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC | Flags.GENERATEDCONSTR), names.fromString("toString"), treeMaker.Ident(names.fromString("String")), List.nil(), List.nil(), List.nil(), block, null);
        methodDecl.mods.annotations = methodDecl.mods.annotations.append(treeMaker.Annotation(treeMaker.Ident(names.fromString("Override")), List.nil()));
        inject(classDecl, methodDecl);
    }

    public List<JCTree.JCVariableDecl> getFields(JCTree.@NotNull JCClassDecl classDecl) {
        List<JCTree.JCVariableDecl> fields = List.nil();
        for (JCTree member : classDecl.getMembers()) {
            if (member instanceof JCTree.JCVariableDecl) {
                fields = fields.append((JCTree.JCVariableDecl) member);
            }
        }


        return fields;
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

        return treeMaker.MethodDef(treeMaker.Modifiers(Flags.STATIC | flags), name, returnType, typeParams, params, thrown, treeMaker.Block(0, body), null);
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

    private Name setName(@NotNull JCTree.JCVariableDecl variableDecl, @NotNull Names names) {
        return names.fromString("set" + variableDecl.getName().toString().substring(0, 1).toUpperCase() + variableDecl.getName().toString().substring(1));
    }
}
