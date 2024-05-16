package net.phoenix;

import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import net.phoenix.annotations.Inject;
import net.phoenix.handlers.InjectHandler;
import net.phoenix.javac.AccessWidener;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * Generic Annotation processor
 *
 * @author Phoenix
 */
@AutoService(javax.annotation.processing.Processor.class)
public class AnnotationProcessor extends AbstractProcessor {

    private InjectHandler injectHandler;

    /**
     * Initializes the processor
     *
     * @param processingEnv the processing environment
     */
    @Override
    public synchronized void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        AccessWidener.addOpens();
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.injectHandler = new InjectHandler(Trees.instance(processingEnv), TreeMaker.instance(context), context);
    }

    /**
     * Returns the latest supported source version
     *
     * @return the latest supported source version
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * Returns the supported annotation types
     *
     * @return the supported annotation types
     */
    @Override
    public @NotNull Set<String> getSupportedAnnotationTypes() {
        return Set.of(Inject.class.getCanonicalName());
    }

    /**
     * Processes the annotations
     *
     * @param annotations the annotations
     * @param roundEnv    the round environment
     * @return true if the annotations were processed successfully
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, @NotNull RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Inject.class)) injectHandler.handle(element);
        return true;
    }
}