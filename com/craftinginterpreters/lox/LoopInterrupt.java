package com.craftinginterpreters.lox;

public class LoopInterrupt extends RuntimeError { 

    LoopInterrupt(Token token, String message) {
        super(token, message);
    }
}
