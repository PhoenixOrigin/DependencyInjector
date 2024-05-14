package net.phoenix;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores values for dependency injection
 */
@SuppressWarnings("unused")
public class DIValues {
    /**
     * A thread-safe hashmap that contains the values
     */
    private static final @NotNull ConcurrentHashMap<Class<?>, Object> values = new ConcurrentHashMap<>();

    /**
     * Stores a value in the hashmap
     *
     * @param clazz the class of the value
     * @param value the value
     */
    public static void storeValue(@NotNull Class<?> clazz, @NotNull Object value) {
        values.put(clazz, value);
    }

    /**
     * Stores a value in the hashmap. The type is inferred from the value
     *
     * @param value the value
     */
    public static void storeValue(@NotNull Object value) {
        values.put(value.getClass(), value);
    }

    /**
     * Gets a value from the hashmap
     * Note: this is mostly for internal use but can still be used externally
     * @param clazz the class of the value
     * @return the value
     */
    public static Object getValue(Class<?> clazz) {
        return values.get(clazz);
    }
}
