package net.phoenix;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class JavaPlusPlus {
    private static final @NotNull ConcurrentHashMap<Class<?>, Object> values = new ConcurrentHashMap<>();

    public static void storeValue(@NotNull Class<?> clazz, @NotNull Object value) {
        values.put(clazz, value);
    }

    public static void store(@NotNull Object value) {
        values.put(value.getClass(), value);
    }

    public static Object getValue(Class<?> clazz) {
        return values.get(clazz);
    }


    public static @NotNull RuntimeException sneakyThrow(@NotNull Throwable t) {
        return sneakyThrow0(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneakyThrow0(Throwable t) throws T {
        throw (T)t;
    }
}
