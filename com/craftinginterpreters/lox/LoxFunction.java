package com.craftinginterpreters.lox;

import java.util.List;

public class LoxFunction implements LoxCallable<Object> {

    // private final Stmt.Function declaration;
    private final List<Token> params;
    private final List<Stmt> body;
    private final Token name;
    private final Environment closure;

    public final Boolean isInitializer;
    public final Boolean isAutoInvoke;

    LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        // this.declaration = declaration;
        this.params = declaration.params;
        this.body = declaration.body;
        this.name = declaration.name;
        this.closure = closure;
        this.isInitializer = isInitializer;
        this.isAutoInvoke = declaration.isAutoInvoke;
    }

    LoxFunction(Expr.Fun expr, Environment closure) {
        this.closure = closure;
        this.params = expr.params;
        this.body = expr.body;
        this.name = new Token(TokenType.IDENTIFIER, "anonymous", null, expr.keyword.line);
        this.isAutoInvoke = false;
        // in our grammar, initializers are never expressions.
        this.isInitializer = false;
    }

    LoxFunction(List<Token> params, List<Stmt> body, Token name, Environment closure, boolean isInitializer, boolean isAutoInvoke) {
        this.params = params;
        this.body = body;
        this.name = name; 
        this.closure = closure;
        this.isAutoInvoke = isAutoInvoke;
        this.isInitializer = isInitializer;
    }

    @Override
    public int arity() {
        return params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < params.size(); i++) {
            environment.define(params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(body, environment);
        } catch (Return returnValue) {
            if (isInitializer) return closure.getAt(0, "this");
            return returnValue.value;
        }
        if (isInitializer) return closure.getAt(0, "this");
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + name.lexeme + ">";
    }

    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(this.params, this.body, this.name, environment, this.isInitializer, this.isAutoInvoke);
    }

    LoxFunction bind(LoxClass klass) {
        Environment environment = new Environment(closure);
        return new LoxFunction(this.params, this.body, this.name, environment, this.isInitializer, this.isAutoInvoke);
    }
    
}
