package net.phoenix;

import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import net.phoenix.annotations.InjectParameter;
import net.phoenix.handlers.FieldHandler;
import net.phoenix.handlers.InjectParameterHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.Set;

@AutoService(javax.annotation.processing.Processor.class)
public class AnnotationProcessor extends AbstractProcessor {

    private Trees trees;
    private TreeMaker treeMaker;
    private Context context;

    @Override
    public synchronized void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.context = context;
        this.trees = Trees.instance(processingEnv);
        this.treeMaker = TreeMaker.instance(context);
        System.out.println("init");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public @NotNull Set<String> getSupportedAnnotationTypes() {
        HashSet<String> annotations = new HashSet<>();
        annotations.add(InjectParameter.class.getCanonicalName());
        annotations.add(net.phoenix.annotations.InjectField.class.getCanonicalName());
        return annotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, @NotNull RoundEnvironment roundEnv) {
        InjectParameterHandler injectParameterHandler = new InjectParameterHandler(trees, treeMaker, context);
        FieldHandler fieldHandler = new FieldHandler(trees, treeMaker, context);

        for (Element element : roundEnv.getElementsAnnotatedWith(InjectParameter.class)) {
            injectParameterHandler.handle(element);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(net.phoenix.annotations.InjectField.class)) {
            fieldHandler.handle(element);
        }

        return true;
    }
}