package com.craftinginterpreters.lox;

public class VarState {
    public VarStatus status;
    public final Token declaration;

    public VarState(VarStatus status, Token declaration) {
        this.status = status;
        this.declaration = declaration;
    }
}
