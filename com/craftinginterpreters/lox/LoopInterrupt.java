package com.craftinginterpreters.lox;

public class LoopInterrupt extends RuntimeException {
    final Token token;

    LoopInterrupt(Token token, String message) {
        super(message);
        this.token = token;
    }
}
