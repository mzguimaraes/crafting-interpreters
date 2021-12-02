package com.craftinginterpreters.lox;

public class Interpreter implements Expr.Visitor<Object> {

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        
        switch (expr.operator.type) {
            case MINUS:
                return (double)left - (double)right;
            case SLASH:
                return (double)left / (double)right;
            case STAR:
                return (double)left * (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }

                break;

            case GREATER:
                return (double)left > (double)right;
            case GREATER_EQUAL:
                return (double)left >= (double)right;
            case LESS:
                return (double)left < (double)right;
            case LESS_EQUAL:
                return (double)left <= (double)right;
            
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                return -(double)right;
            case BANG:
                return !isTruthy(right);
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        if (expr.leftOperator.type == TokenType.QUESTION_MARK &&
                expr.rightOperator.type == TokenType.COLON) {
            // conditional operation.
            Object condition = evaluate(expr.left);
            if (isTruthy(condition)) {
                return evaluate(expr.center);
            } else {
                return evaluate(expr.right);
            }
        }

        // Unreachable.
        return null;
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

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }
}
