package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

// TODO: implement static fields.
public class LoxClass implements LoxCallable<Object>, MemberStore {
    final String name;
    private final Map<String, LoxFunction> methods;
    private final Map<String, LoxFunction> staticMethods;

    LoxClass(String name, Map<String, LoxFunction> methods, Map<String, LoxFunction> staticMethods) {
        this.name = name;
        this.methods = methods;
        this.staticMethods = staticMethods;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        return null;
    }

    @Override
    public Object get(Token name) {
        if (staticMethods.containsKey(name.lexeme)) {
            LoxFunction method = staticMethods.get(name.lexeme);
            return method.bind(this);
        } else {
            throw new RuntimeError(name, "Undefined static method " + name.lexeme);
        }
    }

    @Override
    public void set(Token name, Object value) {
        if (value instanceof LoxFunction) {
            staticMethods.put(name.lexeme, (LoxFunction)value);
        } else {
            throw new RuntimeError(name, "Static fields aren't implemented in Lox yet.");
        }
    }
}
