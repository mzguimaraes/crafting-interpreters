package com.craftinginterpreters.lox;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, 
                                    Stmt.Visitor<Void> {
    
    private Environment environment = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    public Object evaluate(Expr expr) throws RuntimeError {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) throws RuntimeError {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        
        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                // catch divide-by-zero and report.
                if ((double)right == 0) {
                    throw new RuntimeError(expr.operator, "Cannot divide by 0.");
                }
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                // if either operand is a string, cast the other to a string before concatenation.
                if (left instanceof String || right instanceof String) {
                    if (!(left instanceof String)) left = StringUtils.stringify(left);
                    if (!(right instanceof String)) right = StringUtils.stringify(right);
                    return (String)left + (String)right;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");

            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            
            default:
                throw new RuntimeError(expr.operator, "Unrecognized binary operator '" + expr.operator.lexeme + "'.");
        }
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) throws RuntimeError {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) throws RuntimeError {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case BANG:
                return !isTruthy(right);
            
            default:
                throw new RuntimeError(expr.operator, "Unrecognized unary operator '" + expr.operator.lexeme + "'.");
        }
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) throws RuntimeError {
        if (expr.leftOperator.type == TokenType.QUESTION_MARK &&
                expr.rightOperator.type == TokenType.COLON) {
            // conditional operation.
            Object condition = evaluate(expr.left);
            if (isTruthy(condition)) {
                return evaluate(expr.center);
            } else {
                return evaluate(expr.right);
            }
        } else {
            throw new RuntimeError(expr.leftOperator, "Unrecognized ternary operator pair '" 
                + expr.leftOperator.lexeme + "' and '" + expr.rightOperator.lexeme + "'.");
        }
    }

    private void checkNumberOperand(Token operator, Object operand) throws RuntimeError {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand '" + operand.toString() + "' must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) throws RuntimeError {
        checkNumberOperand(operator, left);
        checkNumberOperand(operator, right);
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(StringUtils.stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        if (stmt.initializer == null) {
            environment.declare(stmt.name.lexeme);
        } else {
            environment.define(stmt.name.lexeme, evaluate(stmt.initializer));
        }

        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
            else return evaluate(expr.right);
        } else if (expr.operator.type == TokenType.AND) {
            if (!isTruthy(left)) return left;
            else return evaluate(expr.right);
        } else {
            throw new RuntimeError(expr.operator, "Invalid logical operator.");
        }
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        Object condition = evaluate(stmt.condition);
        while (isTruthy(condition)) {
            execute(stmt.body);
            condition = evaluate(stmt.condition);
        }

        return null;
    }
}
