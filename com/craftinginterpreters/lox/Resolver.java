package com.craftinginterpreters.lox;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, VarState>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private ClassType currentClass = ClassType.NONE;

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum ClassType {
        NONE,
        CLASS,
        SUBCLASS
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
        VarState state = new VarState(VarLifecycle.DECLARED, name);
        scope.put(name.lexeme, state);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, VarState> scope = scopes.peek();
        VarState state = scope.get(name.lexeme);
        state.status = VarLifecycle.DEFINED;

        scope.put(name.lexeme, state);
    }

    /**
     * Checks that all variables declared in scope have been used at least once.
     * If an unused variable is found in scope, a warning is generated.
     */
    private void checkVarUsage(Map<String, VarState> scope) {
        for (Entry<String, VarState> entry : scope.entrySet()) {
            VarState state = entry.getValue();
            if (state.status != VarLifecycle.USED && !isClassKeyword(entry.getKey())) {
                Lox.warning(state.declaration, "Variable unused.");
            }
        }
    }

    /** returns true if identifier is "this" or "super" */
    private Boolean isClassKeyword(String identifier) {
        return Arrays.asList("this", "super").contains(identifier);
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
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return a value from an initializer.");
            }
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
                if (state.status == VarLifecycle.DECLARED) {
                    Lox.error(expr.name, "Cannot read local variable in its own initializer.");
                }
                state.status = VarLifecycle.USED;
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

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;
        declare(stmt.name);
        define(stmt.name);

        if (stmt.superclass != null) {
            if (stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
                Lox.error(stmt.superclass.name, "A class can't inherit from itself.");
            } else {
                currentClass = ClassType.SUBCLASS;
                resolve(stmt.superclass);

                // create scope with bound "super" for later calls.
                beginScope();
                scopes.peek().put("super", new VarState(VarLifecycle.DEFINED, stmt.name));
            }
        }

        beginScope();
        scopes.peek().put("this", new VarState(VarLifecycle.DEFINED, stmt.name));
        for (Stmt.Function method : stmt.methods) {
            FunctionType declaration = method.name.lexeme.equals("init") ?
                FunctionType.INITIALIZER :
                FunctionType.METHOD;
            resolveFunction(method, declaration);
        }
        endScope();

        if (stmt.superclass != null) endScope();

        currentClass = enclosingClass;
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitSuperExpr(Expr.Super expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'super' outside of a class.");
        } else if (currentClass != ClassType.SUBCLASS) {
            Lox.error(expr.keyword, "Can't use 'super' in a class with no superclass.");
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }
    
}
