package net.phoenix.javac;

import net.phoenix.AnnotationProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.io.OutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * AccessWidener
 * <br> <br>
 * <p>
 * Code was taken and rewritten from Lombok
 * </p>
 *
 * @author Phoenix
 */
@SuppressWarnings({"all"})
public class AccessWidener {
    private static final long ACCESSIBLE_OVERRIDE_FIELD_OFFSET;
    private static final IllegalAccessException INIT_ERROR;
    private static final @Nullable Unsafe UNSAFE = (Unsafe) reflectiveStaticFieldAccess(Unsafe.class, "theUnsafe");

    static {
        long g;
        Throwable ex;
        try {
            g = 0;
            ex = null;
        } catch (Throwable t) {
            g = -1L;
            ex = t;
        }
        ACCESSIBLE_OVERRIDE_FIELD_OFFSET = g;
        INIT_ERROR = ex instanceof IllegalAccessException ? (IllegalAccessException) ex : (IllegalAccessException) new IllegalAccessException("Cannot initialize Unsafe-based permit").initCause(ex);
    }

    public static void addOpens() {
        try {
            Class<?> cModule = Class.forName("java.lang.Module");
            Unsafe unsafe = (Unsafe) reflectiveStaticFieldAccess(Unsafe.class, "theUnsafe");
            Object jdkCompilerModule = getJdkCompilerModule();
            Object ownModule = Class.class.getDeclaredMethod("getModule").invoke(AnnotationProcessor.class);
            Method m = cModule.getDeclaredMethod("implAddOpens", String.class, cModule);
            unsafe.putBooleanVolatile(m, unsafe.objectFieldOffset(Parent.class.getDeclaredField("first")), true);
            for (String p : new String[]{"com.sun.tools.javac.processing", "com.sun.tools.javac.tree", "com.sun.tools.javac.util"})
                m.invoke(jdkCompilerModule, p, ownModule);
        } catch (Exception ignore) {
        }
    }

    private static Object getJdkCompilerModule() throws Exception {
        Class<?> cModuleLayer = Class.forName("java.lang.ModuleLayer");
        return Class.forName("java.util.Optional").getDeclaredMethod("get").invoke(cModuleLayer.getDeclaredMethod("findModule", String.class).invoke(cModuleLayer.getDeclaredMethod("boot").invoke(null), "jdk.compiler"));
    }

    private static @Nullable Object reflectiveStaticFieldAccess(@NotNull Class<?> c, @NotNull String fName) {
        try {
            Field f = c.getDeclaredField(fName);
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static @NotNull Method getMethod(@Nullable Class<?> c, @NotNull String mName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method m = null;
        while (c != null && m == null) {
            try {
                m = c.getDeclaredMethod(mName, parameterTypes);
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
            }
        }
        if (m == null) throw new NoSuchMethodException(c.getName() + " :: " + mName + "(args)");
        return setAccessible(m);
    }

    private static <T extends AccessibleObject> T setAccessible(@NotNull T accessor) {
        if (INIT_ERROR == null) {
            UNSAFE.putBoolean(accessor, ACCESSIBLE_OVERRIDE_FIELD_OFFSET, true);
        } else {
            accessor.setAccessible(true);
        }
        return accessor;
    }

    private static long getOverrideFieldOffset() throws Throwable {
        try {
            return UNSAFE.objectFieldOffset(AccessibleObject.class.getDeclaredField("override"));
        } catch (Throwable saved) {
            try {
                return UNSAFE.objectFieldOffset(Fake.class.getDeclaredField("override"));
            } catch (Throwable t) {
                throw saved;
            }
        }
    }

    static class Fake {
        boolean override;
        Object accessCheckCache;
    }

    static class Parent {
        static final Object staticObj = OutputStream.class;
        private static volatile boolean staticSecond, staticThird;
        boolean first;
        volatile Object second;
    }
}