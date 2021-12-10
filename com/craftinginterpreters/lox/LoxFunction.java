package com.craftinginterpreters.lox;

import java.util.List;

public class LoxFunction implements LoxCallable<Object> {

    // private final Stmt.Function declaration;
    private final List<Token> params;
    private final List<Stmt> body;
    private final Token name;
    private final Environment closure;

    LoxFunction(Stmt.Function declaration, Environment closure) {
        // this.declaration = declaration;
        this.params = declaration.params;
        this.body = declaration.body;
        this.name = declaration.name;
        this.closure = closure;
    }

    LoxFunction(Expr.Fun expr, Environment closure) {
        this.closure = closure;
        this.params = expr.params;
        this.body = expr.body;
        this.name = new Token(TokenType.IDENTIFIER, "anonymous", null, expr.keyword.line);
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
            return returnValue.value;
        }
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + name.lexeme + ">";
    }
    
}
