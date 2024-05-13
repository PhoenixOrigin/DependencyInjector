package net.phoenix;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class DIValues {
    private static final @NotNull ConcurrentHashMap<Class<?>, Object> values = new ConcurrentHashMap<>();

    public static void storeValue(@NotNull Class<?> clazz, @NotNull Object value) {
        values.put(clazz, value);
    }

    public static void storeValue(@NotNull Object value) {
        values.put(value.getClass(), value);
    }

    public static Object getValue(Class<?> clazz) {
        return values.get(clazz);
    }
}
