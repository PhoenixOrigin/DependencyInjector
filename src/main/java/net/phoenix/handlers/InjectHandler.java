package net.phoenix.handlers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
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

    private List<JCTree.JCMethodDecl> handled = List.nil();

    final Trees trees;
    final TreeMaker treeMaker;
    final Context context;

    public InjectHandler(Trees trees, TreeMaker treeMaker, Context context) {
        this.trees = trees;
        this.treeMaker = treeMaker;
        this.context = context;
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
        JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) trees.getTree(element);
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getPath(element).getCompilationUnit().getTypeDecls().get(0);

        variableDecl.init = initGen(variableDecl);

        classDecl.defs.forEach(m -> {
            if (m instanceof JCTree.JCMethodDecl methodDecl) {
                if (shouldInject(methodDecl, variableDecl)) modifyMethod(methodDecl, variableDecl, classDecl);
            }
        });
    }

    /**
     * Injects values from {@link net.phoenix.DIValues#getValue(Class)} into parameters annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param methodDecl The method declaration to handle.
     * @param classDecl  The class declaration to handle.
     */
    private void parameter(JCTree.@NotNull JCMethodDecl methodDecl, JCTree.@NotNull JCClassDecl classDecl) {
        if (handled.contains(methodDecl)) {
            return;
        }
        Names names = Names.instance(context);

        JCTree.JCMethodDecl newMethodDecl = copy(methodDecl);

        sortParams(newMethodDecl, names);
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

        JCTree.JCMethodDecl originalMethodDecl = (JCTree.JCMethodDecl) trees.getTree(methodElement);
        parameter(originalMethodDecl, getJCClassDeclFromParameter(parameterElement));
    }

    /**
     * Gets the {@link JCTree.JCClassDecl} from a parameter element.
     *
     * @param parameterElement The parameter element to get the class declaration from.
     * @return The class declaration
     **/
    public JCTree.JCClassDecl getJCClassDeclFromParameter(@NotNull VariableElement parameterElement) {
        ExecutableElement methodElement = (ExecutableElement) parameterElement.getEnclosingElement();
        Element classElement = methodElement.getEnclosingElement();
        return (JCTree.JCClassDecl) trees.getTree(classElement);
    }

    /**
     * Injects values from {@link net.phoenix.DIValues#getValue(Class)} into classes annotated with {@link net.phoenix.annotations.Inject}.
     *
     * @param element The element to handle.
     */
    private void clazz(Element element) {
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getTree(element);
        classDecl.defs.forEach(m -> {
            if (m instanceof JCTree.JCVariableDecl variableDecl) {
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
        Names names = Names.instance(context);
        JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) trees.getTree(element);
        List<JCTree.JCVariableDecl> params = methodDecl.params;
        for (JCTree.JCVariableDecl param : params) {
            param.mods.annotations = param.mods.annotations.prepend(treeMaker.Annotation(treeMaker.Ident(names.fromString("Inject")), List.nil()));
        }
        parameter(methodDecl, (JCTree.JCClassDecl) trees.getTree(element.getEnclosingElement()));
    }

    /**
     * Checks if a method should be injected.
     *
     * @param methodDecl   The method declaration to check.
     * @param variableDecl The variable declaration to check.
     * @return True if the method should be injected, false otherwise.
     */
    private boolean shouldInject(JCTree.JCMethodDecl methodDecl, JCTree.@NotNull JCVariableDecl variableDecl) {
        final boolean[] isReferenced = {false};
        TreeScanner scanner = new TreeScanner() {
            private final Name name;

            {
                name = variableDecl.getName();
            }

            @Override
            public void visitIdent(JCTree.@NotNull JCIdent jcIdent) {
                if (jcIdent.name.equals(name)) {
                    isReferenced[0] = true;
                }
                super.visitIdent(jcIdent);
            }

        };
        scanner.scan(methodDecl);
        return isReferenced[0];
    }

    /**
     * Modifies a method to inject a value from {@link net.phoenix.DIValues#getValue(Class)} into a field.
     *
     * @param method       The method to modify.
     * @param variableDecl The variable declaration to inject.
     * @param classDecl    The class declaration
     */
    private void modifyMethod(JCTree.@NotNull JCMethodDecl method, JCTree.@NotNull JCVariableDecl variableDecl, JCTree.@NotNull JCClassDecl classDecl) {
        System.out.println(method.body);
        method.body.stats = method.body.stats.prepend(treeMaker.Exec(treeMaker.Assign(treeMaker.Select(treeMaker.Ident(classDecl.name), variableDecl.getName()), initGen(variableDecl))));
        System.out.println(method.body);
    }

    /**
     * Generates an expression to inject a value from {@link net.phoenix.DIValues#getValue(Class)} into a field.
     *
     * @param param The variable declaration to inject.
     * @return The expression to inject a value from {@link net.phoenix.DIValues#getValue(Class)} into a field.
     */
    private JCTree.JCExpression initGen(JCTree.@NotNull JCVariableDecl param) {
        Names names = Names.instance(context);
        JCTree.JCExpression paramTypeClassExpr = treeMaker.Select(
                treeMaker.Ident(names.fromString(param.vartype.toString())),
                names.fromString("class")
        );
        JCTree.JCMethodInvocation methodInvocation;
        if (param.mods.annotations.stream().anyMatch(a -> !a.getArguments().isEmpty())) {
            JCTree.JCAnnotation annotation = param.mods.annotations.stream().filter(a -> a.getAnnotationType().toString().equals("Inject")).findFirst().get();
            JCTree.JCLiteral key = (JCTree.JCLiteral)((JCTree.JCAssign)annotation.getArguments().get(0)).rhs;
            System.out.println(key);
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
        System.out.println(treeMaker.TypeCast(
                treeMaker.Ident(names.fromString(param.vartype.toString())),
                methodInvocation
        ));
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
    private boolean shouldInject(JCTree.@NotNull JCVariableDecl param) {
        for (JCTree.JCAnnotation annotation : param.mods.annotations) {
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
    private void sortParams(JCTree.@NotNull JCMethodDecl methodDecl, @NotNull Names names) {
        ListBuffer<JCTree.JCVariableDecl> newParams = new ListBuffer<>();
        for (JCTree.JCVariableDecl param : methodDecl.params) {
            if (shouldInject(param)) {
                methodDecl.body.stats = methodDecl.body.stats.prepend(setValue(param, names));
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
    private JCTree.JCMethodDecl copy(JCTree.@NotNull JCMethodDecl method) {
        return treeMaker.MethodDef(
                method.mods,
                method.name,
                method.restype,
                method.typarams,
                method.params,
                method.thrown,
                method.body,
                method.defaultValue
        );
    }

    /**
     * Injects a value from {@link net.phoenix.DIValues#getValue(Class)} into a parameter.
     *
     * @param param The parameter to inject.
     * @param names The names
     * @return The injected parameter.
     */
    private JCTree.JCVariableDecl setValue(JCTree.@NotNull JCVariableDecl param, @NotNull Names names) {
        return treeMaker.VarDef(
                treeMaker.Modifiers(0),
                names.fromString(param.getName().toString()),
                param.vartype,
                initGen(param)
        );
    }

}
