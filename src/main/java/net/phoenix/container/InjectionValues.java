package net.phoenix.container;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

@SuppressWarnings("unused")
public class InjectionValues {
    private static final @NotNull HashMap<Class<?>, Object> values = new HashMap<>();

    public static void storeValue(Class<?> clazz, @NotNull Object value) {
        if (clazz != value.getClass()) throw new IllegalArgumentException("Class and value type mismatch");
        values.put(clazz, value);
    }

    public static Object getValue(Class<?> clazz) {
        return values.get(clazz);
    }
}
