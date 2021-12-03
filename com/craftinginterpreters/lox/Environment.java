package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();    
    private enum VarState {
        UNINITIALIZED
    }

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    void declare(String name) {
        values.put(name, VarState.UNINITIALIZED);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            if (values.get(name.lexeme) != VarState.UNINITIALIZED) {
                return values.get(name.lexeme);
            } else {
                throw new RuntimeError(name, "Illegal variable access before initialization.");
            }
        }

        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
