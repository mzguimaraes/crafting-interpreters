package com.craftinginterpreters.lox;

public class LoopInterrupt extends RuntimeError { 
    // TODO: should this extend RuntimeException instead?  it's not an error...

    LoopInterrupt(Token token, String message) {
        super(token, message);
    }
}
