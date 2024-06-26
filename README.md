# Java Dependency Injector

A compile-time Java dependency injector that uses annotation processing to generate the necessary code. Using the
internal sun API and the annotation processing API, it modifies methods / fields with the `@Inject` annotation to inject
the necessary dependencies.

## Usage

### Setting up

To use the dependency injector, you need to add the following dependencies and repositories to your `build.gradle` file:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    compileOnly 'io.github.phoenixorigin:DependencyInjector:1.0.6'
    annotationProcessor 'io.github.phoenixorigin:DependencyInjector:1.0.6'
}
```
**Note**: The annotationProcessor should be put __before__ other annotation processors- it will modify methods / fields that other annotation handlers might read and process and the injected methods won't be injected if 
the other annotation processors run before this one.
<br>

### Injecting Dependencies

#### Methods of Injection

1. **Field Injection**:

```java
public class Foo {
    @Inject
    private Bar bar;
}
```

In this case, the `Bar` instance will be injected into the `Foo` instance. Generically, all fields with the `@Inject`
annotation will be injected with the necessary dependencies.

Note: The value *will* update as the dependencies are updated.

2. **Method Injection**:

```java
public class Foo {
    private Bar bar;

    @Inject
    public void setBar(Bar bar) {
        this.bar = bar;
    }
}
```

In this case, the `Bar` instance (or all the parameters) will be injected into the method. Generically, all methods with
the `@Inject` annotation will be injected with the necessary dependencies.

3. **Parameter Injection**:

```java
public class Foo {
    public void doSomething(@Inject Bar bar) {
        // do something with bar
    }
}
```

In this case, the `Bar` instance will be injected into the method parameter. Generically, all parameters with
the `@Inject` annotation will be injected with the necessary dependencies.

When using this method, you do **not** need to supply a parameter; instead you can just call it as `doSomething()`.

If there are multiple parameters with the `@Inject` annotation, the **order of the parameters will be the order of the
parameters in the method**. For example:

```java
public class Foo {
    public void doSomething(@Inject Bar bar, @Inject Baz baz, String str) {

    }
}
```

will be used as

```java
foo.doSomething("Hello, World!");
```

4. **Class Injection**:

```java

@Inject
public class Foo {
    private Bar bar;
}
```

In this case, the `Bar` instance will be injected into the `Foo` instance.

Note: The value *will* update as the dependencies are updated.

#### Storing Values

Storing values to be injected is simple; you can just do

```java
DIValues.store(Bar.class,new Bar());
```

**OR**

```java
DIValues.store(new Bar());
```

In the second scenario, the type to inject will be inferred from the type of the object.

These values can be updated at any time, and the values will be updated in the classes that use them.

#### Getting Values

While there shouldn't be a need to get values as they should be injected by themselves, you can do so by using
the `DIValues.get(Class<T> clazz)` method.

### Using IDs

Instead of just storing values by the class type, you can also add an ID to the value. This is useful for scenarios where you might have multiple instances of the same object e.g. two different db Connection objects. To use this, you can add a parameter to your inject annotation.

```java
public class Foo {
    @Inject(key = "bar")
    private Bar bar;
}
```

This will inject the value with the ID `bar`.

Then, you can store the value with the ID:

```java
DIValues.store("bar",new Bar());
```

### Examples

#### Field Injection

```java

public class Bar {
    @Inject
    private String str;

    public void doSomething() {
        
    }
}
```

```java
public class Main {
    public static void main(String[] args) {
        DIValues.store("Hello, World!");
        Bar bar = new Bar();
        bar.doSomething();
        // Output: Hello, World!
        DIValues.store("Goodbye, World!");
        bar.doSomething();
        // Output: Goodbye, World!
    }
}
```

#### Method Injection

```java
public class Bar {
    private String str;

    @Inject
    public void setStr(String str) {
        this.str = str;
    }

    public void doSomething() {
        
    }
}
```

```java
public class Main {
    public static void main(String[] args) {
        DIValues.store("Hello, World!");
        Bar bar = new Bar();
        bar.doSomething();
        // Output: Hello, World!
        DIValues.store("Goodbye, World!");
        bar.doSomething();
        // Output: Goodbye, World!
    }
}
```

#### Parameter Injection

```java
public class Bar {
    public void doSomething(@Inject String str) {
        
    }
}
```

```java
public class Main {
    public static void main(String[] args) {
        DIValues.store("Hello, World!");
        Bar bar = new Bar();
        bar.doSomething();
        // Output: Hello, World!
        DIValues.store("Goodbye, World!");
        bar.doSomething();
        // Output: Goodbye, World!
    }
}
```

#### Class Injection

```java

@Inject
public class Bar {
    private String str;

    public void doSomething() {
        
    }
}
```

```java
public class Main {
    public static void main(String[] args) {
        DIValues.store("Hello, World!");
        Bar bar = new Bar();
        bar.doSomething();
        // Output: Hello, World!
        DIValues.store("Goodbye, World!");
        bar.doSomething();
        // Output: Goodbye, World!
    }
}
```

## Contributing

If you would like to contribute to the project, you can do so by forking the repository and creating a pull request. If
you have any issues, you can create an issue on the repository.