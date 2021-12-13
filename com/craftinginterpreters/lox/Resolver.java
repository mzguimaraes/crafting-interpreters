package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, VarState>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION
    }

    void resolve(List<Stmt> stmts) {
        for (Stmt stmt : stmts) {
            resolve(stmt);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        resolveFunction(function.params, function.body, type);
    }

    private void resolveFunction(Expr.Fun function, FunctionType type) {
        resolveFunction(function.params, function.body, type);
    }

    private void resolveFunction(List<Token> params, List<Stmt> body, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for (Token param : params) {
            declare(param);
            define(param);
        }
        resolve(body);
        endScope();
        currentFunction = enclosingFunction;
    }

    private void beginScope() {
        scopes.push(new HashMap<String, VarState>());
    }

    private void endScope() {
        Map<String, VarState> scope = scopes.pop();
        checkVarUsage(scope);
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, VarState> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Cannot re-declare variable '" + name.lexeme + "' in this scope.");
        }
        VarState state = new VarState(VarStatus.DECLARED, name);
        scope.put(name.lexeme, state);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, VarState> scope = scopes.peek();
        VarState state = scope.get(name.lexeme);
        state.status = VarStatus.DEFINED;

        scope.put(name.lexeme, state);
    }

    /**
     * Checks that all variables declared in scope have been used at least once.
     * If an unused variable is found in scope, a warning is generated.
     */
    private void checkVarUsage(Map<String, VarState> scope) {
        for (VarState state : scope.values()) {
            if (state.status != VarStatus.USED) {
                Lox.warning(state.declaration, "Variable unused.");
            }
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitIfElseStmt(Stmt.IfElse stmt) {
        if (stmt.ifBranches != null) {
            for (Stmt.If ifBranch : stmt.ifBranches) {
                resolve(ifBranch);
            }
        }
        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }
        if (stmt.value != null) {
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitLoopKeywordStmt(Stmt.LoopKeyword stmt) {
        // this statement is just a keyword, no vars to resolve here.
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitFunExpr(Expr.Fun expr) {
        resolveFunction(expr, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitTernaryExpr(Expr.Ternary expr) {
        resolve(expr.left);
        resolve(expr.center);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty()) {
            Map<String, VarState> scope = scopes.peek();
            VarState state = scope.get(expr.name.lexeme);
            if (state != null) {
                if (state.status == VarStatus.DECLARED) {
                    Lox.error(expr.name, "Cannot read local variable in its own initializer.");
                }
                state.status = VarStatus.USED;
                scope.put(expr.name.lexeme, state);
            }
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitIncrementExpr(Expr.Increment expr) {
        resolve(expr.identifier);
        return null;
    }
    
}
