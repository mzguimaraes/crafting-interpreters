package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

// TODO: implement static fields.
public class LoxClass implements LoxCallable<Object>, MemberStore {
    final String name;
    private final Map<String, LoxFunction> instanceMethods;
    private final Map<String, LoxFunction> staticMethods;

    LoxClass(String name, Map<String, LoxFunction> instanceMethods, Map<String, LoxFunction> staticMethods) {
        this.name = name;
        this.instanceMethods = instanceMethods;
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
        if (instanceMethods.containsKey(name)) {
            return instanceMethods.get(name);
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
        // I don't think we will ever call this function as Lox syntax does not
        // support setting static members outside of the class definition,
        // and this class gets those through its constructor.
        // To make this usable, we'd have to support anonymous static method syntax like:
        //      class Foo {};
        //      Foo.bar = class fun (param) {};
        // We could do that, but it's an involved change, and this isn't a commonly supported
        // feature in other languages AFAIK.
        // NOTE: I was incorrect.  This function DOES get called if the user tries to assign
        // to the class definition, like:
        //      class Foo {};
        //      Foo.bar = "baz";
        // We don't support this uncommon feature either.  Lox does not allow you to define 
        // static members outside of the class definition.
        // TODO: above scenario should throw a syntax error.

        if (value instanceof LoxFunction) {
            Lox.warning(name, "We're calling LoxClass.set() on class definition " + name +", which we thought would never happen.");
            staticMethods.put(name.lexeme, (LoxFunction)value);
        } else {
            throw new RuntimeError(name, "Static fields aren't implemented in Lox yet.");
        }
    }
}
