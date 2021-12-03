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

    private Object evaluate(Expr expr) throws RuntimeError {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
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
                    if (!(left instanceof String)) left = stringify(left);
                    if (!(right instanceof String)) right = stringify(right);
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

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }

            return text;
        }

        return object.toString();
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }
}
