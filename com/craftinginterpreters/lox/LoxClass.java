package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable<Object>, MemberStore {
    final String name;
    final LoxClass superclass;
    private final Map<String, LoxFunction> instanceMethods;
    private final Map<String, Object> statics;

    LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> instanceMethods, Map<String, Object> statics) {
        this.name = name;
        this.instanceMethods = instanceMethods;
        this.statics = statics;
        this.superclass = superclass;
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

        if (superclass != null) {
            return superclass.findMethod(name);
        }
        return null;
    }

    @Override
    public Object get(Token name) {
        if (statics.containsKey(name.lexeme)) {
            Object member = statics.get(name.lexeme);
            if (member instanceof LoxFunction) {
                // bind function to class scope
                member = ((LoxFunction)member).bind(this);
            }
            return member;
        } else {
            throw new RuntimeError(name, "Undefined static member " + name.lexeme);
        }
    }

    @Override
    public void set(Token memberId, Object value) {
        statics.put(memberId.lexeme, value);
    }
}
