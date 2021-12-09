package com.craftinginterpreters.lox;

import java.util.List;

import java.util.ArrayList;

public class Interpreter implements Expr.Visitor<Object>, 
                                    Stmt.Visitor<Void> {
    
    final Environment globals = new Environment();
    private Environment environment = globals;

    Interpreter() {
        globals.define("clock", new LoxCallable<Double>() {

            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Double call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            if (Lox.isInteractive()) {
                echo(statements);
            } else {
                for (Stmt statement : statements) {
                    execute(statement);
                }
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    /**
     * Executes the statement list.
     * Echoes the value of the final statement to System.out (nil in most cases).
     * If the final statement is an Expression statement, echoes the value of that expression instead.
     * This is useful for interactive contexts where the user expects to see a result for their input.
     * @throws RuntimeError if a statement fails to execute.
     */
    private void echo(List<Stmt> statements) throws RuntimeError {
        Stmt finalStatement = statements.remove(statements.size() - 1);
        for (Stmt statement : statements) {
            execute(statement);
        }
        echo(finalStatement);
    }

    /**
     * Executes the statement, echoing its value to System.out.
     * All statements return void when executed, so most statement types will cause "nil" to be echoed.
     * As a special case, if statement is an expression statement, we echo the value that expression evaluates to.
     * @throws RuntimeError if the statement fails to execute.
     */
    private void echo(Stmt statement) throws RuntimeError {
        if (statement instanceof Stmt.Expression) {
            Object result = evaluate(((Stmt.Expression)statement).expression);
            System.out.println(Util.ANSI_GREEN + Util.stringify(result) + Util.ANSI_RESET);
        } else {
            // all other statement types evaluate to nil.
            execute(statement);
            System.out.println(Util.ANSI_GREY + "nil" + Util.ANSI_RESET);
        }
    }

    private Object evaluate(Expr expr) throws RuntimeError {
        return expr.accept(this);
    }

    public void execute(Stmt stmt) {
        stmt.accept(this);
    }

    public void executeBlock(List<Stmt> statements, Environment environment) {
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
                    if (!(left instanceof String))   left = Util.stringify(left);
                    if (!(right instanceof String)) right = Util.stringify(right);
                    return (String)left + (String)right;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or one string and one string-castable object.");

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
            case COMMA:
                return right;
            
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
        System.out.println(Util.stringify(value));
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
        // Do nothing--If statements will be executed by visitIfElseStmt(). 
        // This function should never be called.
        throw new RuntimeError(
            new Token(TokenType.IF, "if", null, 0), 
            "Attempted to interpret bare 'if' statement.\n" +  
            "This should never happen.  If  you see this error, please let the language maintainers know."
        );
    }

    @Override
    public Void visitIfElseStmt(Stmt.IfElse stmt) {
        // parser returns all if statements within an ifElse statement.
        boolean hasExecutedBranch = false;
        for (Stmt.If statement : stmt.ifBranches) {
            if (isTruthy(evaluate(statement.condition))) {
                execute(statement.body);
                hasExecutedBranch = true;
                break;
            }
        }

        if (!hasExecutedBranch && stmt.elseBranch != null) {
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
    public Void visitWhileStmt(Stmt.While stmt) throws RuntimeError {
        Object condition = evaluate(stmt.condition);
        while (isTruthy(condition)) {
            try {
                execute(stmt.body);
                condition = evaluate(stmt.condition);
            } catch (LoopInterrupt interrupt) {
                if (interrupt.token.type == TokenType.CONTINUE) {
                    condition = evaluate(stmt.condition);
                    continue;
                } else if (interrupt.token.type == TokenType.BREAK) {
                    break;
                } else {
                    throw new RuntimeError(interrupt.token, "Unimplmented loop interrupt.");
                }
            } 
        }

        return null;
    }

    @Override
    public Void visitLoopKeywordStmt(Stmt.LoopKeyword stmt) {
        throw new LoopInterrupt(stmt.token, "Loop interrupt not semantically valid.");
    }

    @Override
    public Object visitIncrementExpr(Expr.Increment expr) throws RuntimeError {
        Object value = evaluate(expr.identifier);

        if (!(value instanceof Double) || !Util.isInteger((double)value)) {
            throw new RuntimeError(expr.identifier.name, "Cannot apply increment operation to non-integer value.");
        }

        Double d = (double)value;

        switch (expr.type) {
            case POST_DECREMENT:
                environment.assign(expr.identifier.name, d - 1);
                return d;
            case POST_INCREMENT:
                environment.assign(expr.identifier.name, d + 1);
                return d;
            case PRE_DECREMENT:
                environment.assign(expr.identifier.name, d - 1);
                return d - 1;
            case PRE_INCREMENT:
                environment.assign(expr.identifier.name, d + 1);
                return d + 1;
            default:
                throw new RuntimeError(expr.operator, "Unrecognized increment operator.");
        }
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) throws RuntimeError {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        // we checked type above.  As far as checking the generic type here, 
        // casting to Object should be safe for any implementation.
        @SuppressWarnings("unchecked")
        LoxCallable<Object> function = (LoxCallable<Object>)callee;

        if (arguments.size() != function.arity()){
            throw new RuntimeError(expr.paren, "Expected " + 
                function.arity() + " arguments but got " + 
                arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) throws Return {
        Object value = stmt.value != null ? evaluate(stmt.value) : null;
        throw new Return(value);
    }

}
