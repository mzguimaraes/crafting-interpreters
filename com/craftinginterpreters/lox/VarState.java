package com.craftinginterpreters.lox;

public class VarState {
    public VarLifecycle status;
    public final Token declaration;

    public VarState(VarLifecycle status, Token declaration) {
        this.status = status;
        this.declaration = declaration;
    }
}
