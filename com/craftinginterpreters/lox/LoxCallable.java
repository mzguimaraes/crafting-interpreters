package com.craftinginterpreters.lox;

import java.util.List;

public interface LoxCallable<R> {
    int arity();
    R call(Interpreter interpreter, List<Object> arguments);    
}
