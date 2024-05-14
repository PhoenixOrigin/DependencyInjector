package net.phoenix;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores values for dependency injection
 *
 * @author Phoenix
 */
@SuppressWarnings("unused")
public class DIValues {
    /**
     * A thread-safe hashmap that contains the values
     */
    private static final @NotNull ConcurrentHashMap<Class<?>, List<Value>> diValues = new ConcurrentHashMap<>();

    /**
     * Stores a value in the hashmap
     *
     * @param clazz the class of the value
     * @param value the value
     */
    public static void storeValue(@NotNull Class<?> clazz, @NotNull Object value) {
        storeValue(clazz, value, "default");
    }

    /**
     * Stores a value in the hashmap. The type is inferred from the value
     *
     * @param value the value
     */
    public static void storeValue(@NotNull Object value) {
        storeValue(value.getClass(), value);
    }

    /**
     * Stores a value in the hashmap
     *
     * @param clazz the class of the value
     * @param value the value
     * @param name  the name of the value
     */
    public static void storeValue(Class<?> clazz, Object value, String name) {
        List<Value> values = diValues.get(clazz);
        if(values == null) {
            values = new ArrayList<>();
        }
        values.add(new Value(value, name));
        diValues.put(clazz, values);
    }

    /**
     * Stores a value in the hashmap. The type is inferred from the value
     *
     * @param value the value
     * @param name  the name of the value
     */
    public static void storeValue(Value value, String name) {
        storeValue(value.value.getClass(), value.value, name);
    }

    /**
     * Stores a value in the hashmap. The type is inferred from the value
     *
     * @param value the value
     */
    public static void storeValue(Value value) {
        storeValue(value, "default");
    }

    /**
     * Gets a value from the hashmap
     * Note: this is mostly for internal use but can still be used externally
     * @param clazz the class of the value
     * @return the value
     */
    public static Object getValue(Class<?> clazz) {
        return getValue(clazz, "default");
    }

    /**
     * Gets a value from the hashmap
     * Note: this is mostly for internal use but can still be used externally
     * @param clazz the class of the value
     * @param name the name of the value
     * @return the value
     */
    public static Object getValue(Class<?> clazz, String name) {
        if(!diValues.containsKey(clazz)) {
            return null;
        }
        return diValues.get(clazz).stream().filter(value -> value.name().equals(name)).map(Value::value).findFirst().orElse(null);
    }

    /**
     * A record that represents a value
     */
    public record Value(Object value, String name) {
    }
}
