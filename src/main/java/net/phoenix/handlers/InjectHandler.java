package net.phoenix.handlers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.*;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.Objects;

/**
 * Handler for the {@link net.phoenix.annotations.Inject} annotation.
 * Injects values from {@link net.phoenix.DIValues#getValue(Class)} into fields, parameters, classes or methods annotated with {@link net.phoenix.annotations.Inject}.
 *
 * @author Phoenix
 * @see net.phoenix.annotations.Inject
 * @see net.phoenix.DIValues
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class InjectHandler {

    private final Trees trees;
    private final TreeMaker treeMaker;
    private final Names names;
    private List<JCMethodDecl> handled = List.nil();

    public InjectHandler(Trees trees, TreeMaker treeMaker, @NotNull Context context) {
        this.trees = trees;
        this.treeMaker = treeMaker;
        this.names = Names.instance(context);
    }

    /**
     * Handles the element, injecting values from {@link net.phoenix.DIValues#getValue(Class)} into fields, parameters, classes or methods annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param element The element to handle.
     */
    public void handle(@NotNull Element element) {
        if (element.getKind() == ElementKind.FIELD) {
            field(element);
        } else if (element.getKind() == ElementKind.PARAMETER) {
            parameter((VariableElement) element);
        } else if (element.getKind() == ElementKind.CLASS) {
            clazz(element);
        } else if (element.getKind() == ElementKind.METHOD) {
            method(element);
        } else {
            throw new IllegalArgumentException("Unsupported element kind: " + element.getKind());
        }
    }

    /**
     * Injects values from {@link net.phoenix.DIValues#getValue(Class)} into fields annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param element The element to handle.
     */
    private void field(Element element) {
        JCVariableDecl variableDecl = (JCVariableDecl) trees.getTree(element);
        JCClassDecl classDecl = (JCClassDecl) trees.getPath(element).getCompilationUnit().getTypeDecls().get(0);
        variableDecl.init = initGen(variableDecl);
        classDecl.defs.forEach(m -> {if (m instanceof JCMethodDecl methodDecl) if (new TreeChecker(variableDecl.getName()).check(methodDecl)) methodDecl.body.stats = methodDecl.body.stats.prepend(treeMaker.Exec(treeMaker.Assign(treeMaker.Select(treeMaker.Ident(classDecl.name), variableDecl.getName()), initGen(variableDecl))));});
    }

    /**
     * Injects values from {@link net.phoenix.DIValues#getValue(Class)} into parameters annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param methodDecl The method declaration to handle.
     * @param classDecl  The class declaration to handle.
     */
    private void parameter(@NotNull JCMethodDecl methodDecl, @NotNull JCClassDecl classDecl) {
        if (handled.contains(methodDecl)) return;
        JCMethodDecl newMethodDecl = copy(methodDecl);
        sortParams(newMethodDecl, names);
        classDecl.defs.remove(methodDecl);
        classDecl.defs = classDecl.defs.prepend(newMethodDecl);
        handled = handled.prepend(newMethodDecl);
    }

    /**
     * Injects values from {@link net.phoenix.DIValues#getValue(Class)} into parameters annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param parameterElement The parameter element to handle.
     */
    private void parameter(@NotNull VariableElement parameterElement) {
        ExecutableElement methodElement = (ExecutableElement) parameterElement.getEnclosingElement();
        JCMethodDecl originalMethodDecl = (JCMethodDecl) trees.getTree(methodElement);
        parameter(originalMethodDecl, getClassDecl(parameterElement));
    }

    /**
     * Gets the {@link JCClassDecl} from a parameter element.
     *
     * @param parameterElement The parameter element to get the class declaration from.
     * @return The class declaration
     **/
    private JCClassDecl getClassDecl(@NotNull VariableElement parameterElement) {
        ExecutableElement methodElement = (ExecutableElement) parameterElement.getEnclosingElement();
        Element classElement = methodElement.getEnclosingElement();
        return (JCClassDecl) trees.getTree(classElement);
    }

    /**
     * Injects values from {@link net.phoenix.DIValues#getValue(Class)} into classes annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param element The element to handle.
     */
    private void clazz(Element element) {
        JCClassDecl classDecl = (JCClassDecl) trees.getTree(element);
        classDecl.defs.forEach(m -> {
            if (m instanceof JCVariableDecl variableDecl) {
                field(trees.getElement(trees.getPath(trees.getPath(element).getCompilationUnit(), variableDecl)));
            }
        });
    }

    /**
     * Injects values from {@link net.phoenix.DIValues#getValue(Class)} into methods annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param element The element to handle.
     */
    private void method(@NotNull Element element) {
        JCMethodDecl methodDecl = (JCMethodDecl) trees.getTree(element);
        List<JCVariableDecl> params = methodDecl.params;
        for (JCVariableDecl param : params) {
            param.mods.annotations = param.mods.annotations.prepend(treeMaker.Annotation(treeMaker.Ident(names.fromString("Inject")), List.nil()));
        }
        parameter(methodDecl, (JCClassDecl) trees.getTree(element.getEnclosingElement()));
    }

    /**
     * Generates an expression to inject a value from {@link net.phoenix.DIValues#getValue(Class)} into a field.
     *
     * @param param The variable declaration to inject.
     * @return The expression to inject a value from {@link net.phoenix.DIValues#getValue(Class)} into a field.
     */
    private JCExpression initGen(@NotNull JCVariableDecl param) {
        JCExpression paramTypeClassExpr = treeMaker.Select(
                treeMaker.Ident(names.fromString(param.vartype.toString())),
                names.fromString("class")
        );
        JCMethodInvocation methodInvocation;
        if (param.mods.annotations.stream().anyMatch(a -> !a.getArguments().isEmpty())) {
            JCAnnotation annotation = param.mods.annotations.stream().filter(a -> a.getAnnotationType().toString().equals("Inject")).findFirst().get();
            JCLiteral key = (JCLiteral) ((JCAssign) annotation.getArguments().get(0)).rhs;
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
     * Determines if a parameter should be injected.
     *
     * @param param The parameter to check.
     * @return True if the parameter should be injected, false otherwise.
     */
    private boolean shouldInject(@NotNull JCVariableDecl param) {
        for (JCAnnotation annotation : param.mods.annotations) {
            if (Objects.equals(annotation.getAnnotationType().toString(), "Inject")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sorts the parameters of a method, injecting values from {@link net.phoenix.DIValues#getValue(Class)} into parameters annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param methodDecl The method declaration to sort.
     * @param names      The names
     */
    private void sortParams(@NotNull JCMethodDecl methodDecl, @NotNull Names names) {
        ListBuffer<JCVariableDecl> newParams = new ListBuffer<>();
        for (JCVariableDecl param : methodDecl.params) {
            if (shouldInject(param)) {
                methodDecl.body.stats = methodDecl.body.stats.prepend(treeMaker.VarDef(treeMaker.Modifiers(0), names.fromString(param.getName().toString()), param.vartype, initGen(param)));
                continue;
            }
            newParams.append(param);
        }
        methodDecl.params = newParams.toList();
    }

    /**
     * Copies a method declaration.
     *
     * @param method The method declaration to copy.
     * @return The copied method declaration.
     */
    private JCMethodDecl copy(@NotNull JCMethodDecl method) {
        return treeMaker.MethodDef(method.mods, method.name, method.restype, method.typarams, method.params, method.thrown, method.body, method.defaultValue);
    }

    static class TreeChecker extends TreeScanner {
        Name n;boolean referenced = false;
        public TreeChecker(Name n) {this.n = n;}
        public boolean check(JCMethodDecl tree) {scan(tree);return referenced;}
        @Override public void visitIdent(@NotNull JCIdent jcIdent) {if (jcIdent.name.equals(n)) referenced = true; super.visitIdent(jcIdent);}
    }

}
