package net.phoenix.javac;

import net.phoenix.AnnotationProcessor;
import sun.misc.Unsafe;

import java.io.OutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings({"all"})
public class AccessWidener {
    private static final long ACCESSIBLE_OVERRIDE_FIELD_OFFSET;
    private static final IllegalAccessException INIT_ERROR;
    private static final Unsafe UNSAFE = (Unsafe) reflectiveStaticFieldAccess(Unsafe.class, "theUnsafe");

    static {
        long g;
        Throwable ex;
        try {
            g = 0; ex = null;
        } catch (Throwable t) {
            g = -1L; ex = t;
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
            for (String p : new String[]{"com.sun.tools.javac.code", "com.sun.tools.javac.comp", "com.sun.tools.javac.file", "com.sun.tools.javac.main", "com.sun.tools.javac.model", "com.sun.tools.javac.parser", "com.sun.tools.javac.processing", "com.sun.tools.javac.tree", "com.sun.tools.javac.util", "com.sun.tools.javac.jvm"}) m.invoke(jdkCompilerModule, p, ownModule);
        } catch (Exception ignore) {}
    }

    private static Object getJdkCompilerModule() throws Exception {
        Class<?> cModuleLayer = Class.forName("java.lang.ModuleLayer");
        Object bootLayer = cModuleLayer.getDeclaredMethod("boot").invoke(null);
        Object oCompilerO = cModuleLayer.getDeclaredMethod("findModule", String.class).invoke(bootLayer, "jdk.compiler");
        return Class.forName("java.util.Optional").getDeclaredMethod("get").invoke(oCompilerO);
    }

    private static Object reflectiveStaticFieldAccess(Class<?> c, String fName) {
        try {
            Field f = c.getDeclaredField(fName);
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static Method getMethod(Class<?> c, String mName, Class<?>... parameterTypes) throws NoSuchMethodException {
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

    public static <T extends AccessibleObject> T setAccessible(T accessor) {
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
        boolean first;
        static final Object staticObj = OutputStream.class;
        volatile Object second;
        private static volatile boolean staticSecond;
        private static volatile boolean staticThird;
    }
}