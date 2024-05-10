package net.phoenix;

import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import net.phoenix.annotations.Get;
import net.phoenix.annotations.Getters;
import net.phoenix.annotations.InjectParameter;
import net.phoenix.annotations.Setters;
import net.phoenix.handlers.FieldHandler;
import net.phoenix.handlers.GetHandler;
import net.phoenix.handlers.GettersHandler;
import net.phoenix.handlers.InjectParameterHandler;
import net.phoenix.handlers.POJOHandler;
import net.phoenix.handlers.SetHandler;
import net.phoenix.handlers.SettersHandler;
import net.phoenix.handlers.SneakyThrowHandler;
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
        annotations.add(Get.class.getCanonicalName());
        annotations.add(net.phoenix.annotations.Set.class.getCanonicalName());
        annotations.add(InjectParameter.class.getCanonicalName());
        annotations.add(Getters.class.getCanonicalName());
        annotations.add(Setters.class.getCanonicalName());
        annotations.add(net.phoenix.annotations.POJO.class.getCanonicalName());
        annotations.add(net.phoenix.annotations.InjectField.class.getCanonicalName());
        annotations.add(net.phoenix.annotations.SneakyThrow.class.getCanonicalName());
        return annotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, @NotNull RoundEnvironment roundEnv) {
        GetHandler getHandler = new GetHandler(trees, treeMaker, context);
        SetHandler setHandler = new SetHandler(trees, treeMaker, context);
        InjectParameterHandler injectParameterHandler = new InjectParameterHandler(trees, treeMaker, context);
        GettersHandler gettersHandler = new GettersHandler(trees, treeMaker, context);
        SettersHandler settersHandler = new SettersHandler(trees, treeMaker, context);
        POJOHandler pojoHandler = new POJOHandler(trees, treeMaker, context);
        FieldHandler fieldHandler = new FieldHandler(trees, treeMaker, context);
        SneakyThrowHandler sneakyThrowHandler = new SneakyThrowHandler(trees, treeMaker, context);

        for (Element element : roundEnv.getElementsAnnotatedWith(Get.class)) {
            getHandler.handle(element);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(net.phoenix.annotations.Set.class)) {
            setHandler.handle(element);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(InjectParameter.class)) {
            injectParameterHandler.handle(element);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(net.phoenix.annotations.Getters.class)) {
            gettersHandler.handle(element);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(net.phoenix.annotations.Setters.class)) {
            settersHandler.handle(element);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(net.phoenix.annotations.POJO.class)) {
            pojoHandler.handle(element);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(net.phoenix.annotations.InjectField.class)) {
            fieldHandler.handle(element);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(net.phoenix.annotations.SneakyThrow.class)) {
            sneakyThrowHandler.handle(element);
        }

        return true;
    }
}