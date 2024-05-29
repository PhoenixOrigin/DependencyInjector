package net.phoenix.util;

import net.phoenix.AnnotationProcessor;

import java.lang.reflect.Method;

/**
 * AccessWidener
 *
 * @author Phoenix
 */
@SuppressWarnings({"all"})
public class AccessWidener {

    /**
     * Adds an opens directive to the module of the JDK compiler.
     * This is necessary to access the private method {@link Module#implAddOpens(String, Module)}.
     * <br> <br>
     * <p>
     * Note: This method is only available in JDK 9 and above.
     * </p>
     *
     * @param packages the packages to open
     */
    public static void addOpens(String... packages) {
        try {
            Class<?> cModule = Class.forName("java.lang.Module");
            Object jdkCompilerModule = getJdkCompilerModule();
            Object ownModule = Class.class.getDeclaredMethod("getModule").invoke(AnnotationProcessor.class);
            Method m = cModule.getDeclaredMethod("implAddOpens", String.class, cModule);
            m.setAccessible(true);
            for (String p : packages) {
                m.invoke(jdkCompilerModule, p, ownModule);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Gets the JDK compiler module.
     *
     * @return the JDK compiler module
     * @throws Exception if the module could not be found
     */
    private static Object getJdkCompilerModule() throws Exception {
        Class<?> cModuleLayer = Class.forName("java.lang.ModuleLayer");
        return Class.forName("java.util.Optional").getDeclaredMethod("get").invoke(cModuleLayer.getDeclaredMethod("findModule", String.class).invoke(cModuleLayer.getDeclaredMethod("boot").invoke(null), "jdk.compiler"));
    }

}
