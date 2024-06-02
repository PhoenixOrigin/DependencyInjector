package net.phoenix.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields, parameters, classes or methods that should be injected with values from  {@link net.phoenix.util.DIValues#getValue(Class)}.
 *
 * @author Phoenix
 */
@SuppressWarnings("unused")
@Target({java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.PARAMETER, ElementType.TYPE, ElementType.METHOD})
public @interface Inject {
    String key() default "default";
}
