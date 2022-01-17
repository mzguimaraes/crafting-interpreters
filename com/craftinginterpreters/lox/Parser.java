package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

// TODO: implement:
// doWhile → "do" statement "while" "(" expression ")" ;
// error production for calling init() directly on a class type (i.e. Foo.init()).

/**
 * Parses a Lox string into an AST by implementing recursive descent.
 * 
 * Full grammar implemented by this parser:
 * 
 * program        → declaration* EOF ;
 * declaration    → classDecl
 *                 | funDecl
 *                 | varDecl
 *                 | statement ;
 * classDecl      → "class" IDENTIFIER ( "<" IDENTIFIER )? "{" ( "class"? function )* "}" ;
 * funDecl        → "fun" function ;
 * function       → IDENTIFIER ( "(" parameter? ")" )? block ;
 * parameter      → IDENTIFIER ( "," IDENTIFIER )* ;
 * varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
 * statement      → exprStmt
 *                 | forStmt
 *                 | ifElseStmt
 *                 | printStmt
 *                 | whileStmt
 *                 | loopKywd
 *                 | returnStmt
 *                 | block ;
 * forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
 *                   expression? ";"
 *                   expression? ")" statement ;
 * whileStmt      → "while" "(" expression ")" statement ;
 * loopKywd       → ( "break" | "continue" ) ";" ;
 * ifElseStmt     → ifStmt ( "else" ifStmt )* ( "else" statement )? ;
 * ifStmt         → "if" "(" expression ")" statement ;
 * exprStmt       → expression ";" ;
 * printStmt      → "print" expression ";" ;
 * returnStmt     → "return" expression? ";"
 * block          → "{" declaration* "}" ;
 * expression     → comma ;
 * comma          → assignment ( "," assignment )* ;
 * assignment     → ( call "." )? IDENTIFIER ( "=" | "+=" | "-=" ) assignment
 *                 | logic_or 
 *                 | funExpr ;
 * funExpr        → "fun" "(" parameter? ")" block ;
 * logic_or       → logic_and ( "or" logic_and )* ;
 * logic_and      → conditional ( "and" conditional)* ;
 * conditional    → equality ( "?" expression ":" expression )* ; --right-associative
 * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term           → factor ( ( "-" | "+" ) factor )* ;
 * factor         → unary ( ( "/" | "*" ) unary )* ;
 * unary          → ( "!" | "-" ) unary
 *                 | increment ;
 * increment      → postIncrement | preIncrement ;
 * postIncrement  → call ( "++" | "--" )? ;
 * preIncrement   → ( "++" | "--" ) IDENTIFIER ;
 * call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
 * // using assignment instead of expression to avoid arg list being parsed as comma operator
 * arguments      → assignment ("," assignment )* ; 
 * primary        → NUMBER | STRING | "true" | "false" | "nil"
 *                | "(" expression ")"
 *                | IDENTIFIER
 *                | "super" "." IDENTIFIER ;
 *                | primaryError ;
 * 
 * Error productions:
 * primaryError  → binaryError ;
 * binaryError   → ( "!=" | "==" | ">" | ">=" | "<" | "<=" | "," | "-" | "+" | "/" | "*" ) expression ;
 */
public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    private int loopsInside = 0;

    private enum FunctionType {
        FUNCTION,
        INSTANCE_METHOD,
        STATIC_METHOD
    }

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // program → declaration* EOF ;
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            Stmt stmt = declaration();
            if (stmt != null) {
                statements.add(stmt);
            }
        }

        return statements;
    }

    // declaration → classDecl | funDecl | varDecl | statement ;
    private Stmt declaration() {
        try {
            if (match(TokenType.CLASS)) return classDeclaration();
            if (match(TokenType.FUN)) return function(FunctionType.FUNCTION);
            if (match(TokenType.VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    // classDecl → "class" IDENTIFIER ( "<" IDENTIFIER )? "{" ( "class"? function )* "}" ;
    private Stmt.Class classDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect class name.");

        Expr.Variable superclass = null;
        if (match(TokenType.LESS)) {
            consume(TokenType.IDENTIFIER, "Expect superclass name.");
            superclass = new Expr.Variable(previous());
        }

        consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (match(TokenType.CLASS)) {
                methods.add(function(FunctionType.STATIC_METHOD));
            } else {
                methods.add(function(FunctionType.INSTANCE_METHOD));
            }
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, superclass, methods);
    }

    // function → IDENTIFIER ( "(" parameter? ")" )? block ;
    private Stmt.Function function(FunctionType kind) {
        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        List<Token> parameters;
        Boolean isGetter = false;

        if (check(TokenType.LEFT_PAREN)) {
            consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");

            parameters = parameter();

            consume(TokenType.RIGHT_PAREN, "Expect ')' after parameter list.");
        } else {
            // if the param list is omitted from the definition, user is defining a getter.
            parameters = new ArrayList<>();
            isGetter = true;
        }

        consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body, kind == FunctionType.STATIC_METHOD, isGetter);
    }

    // parameter → IDENTIFIER ( "," IDENTIFIER )* ;
    private List<Token> parameter() {
        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Cannot have more than 254 parameters.");
                }

                parameters.add(
                    consume(TokenType.IDENTIFIER, "Expect parameter name.")
                );
            } while (match(TokenType.COMMA));
        }
        return parameters;
    }

    // varDecl → "var" IDENTIFIER ( "=" expression )? ";" ;
    private Stmt varDeclaration() {
        // var token already consumed
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    // statement → exprStmt | ifElseStmt | printStmt | loopKywd | returnStmt | block ;
    private Stmt statement() {
        if (match(TokenType.IF)) return ifElseStatement();
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.loopKeywords)) return loopKeyword();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    // returnStmt     → "return" expression? ";"
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    // forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
    //                   expression? ";"
    //                   expression? ")" statement ;
    private Stmt forStatement() {
        // syntactic sugar over while statements.
        loopsInside ++;
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");
        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after 'for' loop condition.");

        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        } 
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'for' loop clauses.");

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(
                Arrays.asList(
                    body,
                    new Stmt.Expression(increment)
                )
            );
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        loopsInside --;
        return body;
    }

    // whileStmt → "while" "(" expression ")" statement ;
    private Stmt whileStatement() {
        loopsInside ++;
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'while' condition expression.");
        Stmt body = statement();
        loopsInside --;
        return new Stmt.While(condition, body);
    }

    // loopKywd → ( "break" | "continue" ) ";" ;
    private Stmt.LoopKeyword loopKeyword() {
        // already matched on { in loopBody() or loopBlock().

        // throw syntax error if we're not in a loop.
        if (loopsInside <= 0) {
            throw error(previous(), "Loop keyword not allowed outside loop.");
        }

        Token token = previous();
        consume(TokenType.SEMICOLON, "Expect ';' after '" + token.lexeme + "' statement.");
        return new Stmt.LoopKeyword(token);
    }

    // ifElseStmt → ifStmt ( "else" ifStmt )* ("else" statement)? ;
    private Stmt ifElseStatement() {
        List<Stmt.If> ifBranches = new ArrayList<>();

        ifBranches.add(ifStatement());

        Stmt elseBranch = null;

        while (match(TokenType.ELSE)) {
            if (match(TokenType.IF)) {
                ifBranches.add(ifStatement());
            } else if (elseBranch == null) {
                elseBranch = statement();
            } else {
                throw error(previous(), "Else branch already defined.");
            }
        }

        return new Stmt.IfElse(ifBranches, elseBranch);
    }

    // ifStmt → "if" "(" expression ")" statement ;
    private Stmt.If ifStatement() {
        // "if" token was consumed by statement()
        consume(TokenType.LEFT_PAREN, "Expect '(' after if.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");
        Stmt body = statement();
        return new Stmt.If(condition, body);
    }

    // block → "{" declaration* "}" ;
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    // exprStmt → expression ";" ;
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    // printStmt → "print" expression ";" ;
    // already matched PRINT token in statement(), no need to consume it here.
    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    // expression → comma ;
    private Expr expression() {
        return comma();
    }

    // funExpr → "fun" "(" parameter? ")" block ;
    private Expr.Fun funExpr() {
        Token keyword = previous();

        consume(TokenType.LEFT_PAREN, "Expect '(' after 'fun' keyword.");

        List<Token> params = parameter();

        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameter list.");

        consume(TokenType.LEFT_BRACE, "Expect '{' before function body.");
        List<Stmt> body = block();

        return new Expr.Fun(params, body, keyword);
    }

    // comma → assignment ( "," assignment )* ;
    private Expr comma() {
        Expr expr = assignment();
        while (match(TokenType.COMMA)) {
            Token operator = previous();
            Expr right = assignment();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // assignment → ( call "." )? IDENTIFIER ( "=" | "+=" | "-=" ) assignment
    //              | logic_or
    //              | funExpr
    private Expr assignment() {
        if (match(TokenType.FUN)) {
            return funExpr();
        }
        Expr identifier = or(); // if we're not in an assignment, then this var holds the result of the logic_or production.

        if (match(TokenType.assignmentOperators)) {
            Token operator = previous();
            Expr value = assignment();

            if (identifier instanceof Expr.Variable) {
                Token name = ((Expr.Variable)identifier).name;
                // Syntactic sugar: parse "a += <exp>;" as "a = a + <exp>;"
                if (operator.type == TokenType.PLUS_EQUAL) {
                    // create binary addition expression
                    value = new Expr.Binary(
                        identifier, 
                        new Token(TokenType.PLUS, "+", null, operator.line), 
                        value);
                } else if (operator.type == TokenType.MINUS_EQUAL) {
                    // create binary subtraction expression
                    value = new Expr.Binary(
                        identifier, 
                        new Token(TokenType.MINUS, "-", null, operator.line), 
                        value);
                }
                return new Expr.Assign(name, value);
            } else if (identifier instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)identifier;
                return new Expr.Set(get.object, get.name, value);
            }

            error(operator, "Invalid assignment target.");
        }

        return identifier;
    }

    // logic_or → logic_and ( "or" logic_and )* ;
    private Expr or() {
        Expr expr = and();
        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    // logic_and → conditional ( "and" conditional)* ;
    private Expr and() {
        Expr expr = conditional();

        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = conditional();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    // conditional → equality ( "?" expression ":" expression )* ; --right-associative
    private Expr conditional() {
        Expr predicate = equality();
        while (match(TokenType.QUESTION_MARK)) {
            Token leftOperator = previous();
            Expr consequent = expression();
            consume(TokenType.COLON, "Expect ':' following ternary operator '?'");
            Token rightOperator = previous();
            Expr alternative = expression();
            predicate = new Expr.Ternary(
                predicate, 
                consequent, 
                alternative,
                leftOperator, 
                rightOperator
            );
        }
        return predicate;
    }


    // equality → comparison ( ( "!=" | "==" ) comparison )* ;
    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // term → factor ( ( "-" | "+" ) factor )* ;
    private Expr term() {
        Expr expr = factor();
        while(match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // factor → unary ( ( "/" | "*" ) unary )* ;
    private Expr factor() {
        Expr expr = unary();
        while(match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // unary → ( "!" | "-" ) unary | increment ;
    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        } 

        return increment();
    }

    // increment → postIncrement | preIncrement ;
    private Expr increment() throws ParseError {
        if (!match(TokenType.incrementOperators)) {
            // post-inc or higher precedence expression
            return postIncrement();
        } else {
            // pre-inc
            return preIncrement();
        }
    }

    // postIncrement → call ( "++" | "--" )? ;
    private Expr postIncrement() throws RuntimeError {
        Expr identifier = call();
        Token operator;
        IncrementType type;
        if ((identifier instanceof Expr.Variable)) {
            // valid: variable expr followed by an inc/dec operator
            if (match(TokenType.incrementOperators)) {
                operator = previous();
                switch (operator.type) {
                    case PLUS_PLUS:
                        type = IncrementType.POST_INCREMENT;
                        break;
                    case MINUS_MINUS:
                        type = IncrementType.POST_DECREMENT;
                        break;
                    default:
                        throw error(operator, "Unimplemented increment/decrement operator");
                }

                return new Expr.Increment(((Expr.Variable)identifier), operator, type);
            } 

        } else {
            // invalid: non-variable primary followed by an inc/dec operator 
            if (match(TokenType.incrementOperators)) {
                throw error(previous(), "Expect variable identifier before post-increment operator.");
            } 
        }
        // Valid: 
        // - non-variable primary not followed by inc/dec operator
        // - variable expr not followed by an inc/dec operator
        return identifier;
    }

    // preIncrement → ( "++" | "--" ) IDENTIFIER ;
    private Expr preIncrement() throws RuntimeError {
        Token operator = previous();
        Expr identifier = primary();
        IncrementType type;
        if (!(identifier instanceof Expr.Variable)) {
            throw error(previous(), "Expect variable identifier after pre-increment operator.");
        }

        switch (operator.type) {
            case PLUS_PLUS:
                type = IncrementType.PRE_INCREMENT;
                break;
            case MINUS_MINUS:
                type = IncrementType.PRE_DECREMENT;
                break;
            default:
                throw error(operator, "Unimplemented increment/decrement operator");
        }
        return new Expr.Increment(((Expr.Variable)identifier), operator, type);
    }

    // call → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Expect property name after dot access '.'.");
                expr = new Expr.Get(expr, name); 
            } else {
                break;
            }
        }

        return expr;
    }

    // arguments → assignment ("," assignment )* ; 
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(assignment());
            } while (match(TokenType.COMMA));
            if (arguments.size() >= 255) {
                error(peek(), "Can't have more than 255 arguments.");
            }
        }

        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after argument list. ");

        return new Expr.Call(callee, paren, arguments);
    }

    // primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | "super" "." IDENTIFIER | IDENTIFIER | primaryError ;
    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NIL)) return new Expr.Literal(null);

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(TokenType.SUPER)) {
            Token keyword = previous();
            consume(TokenType.DOT, "Expect '.' after 'super'.");
            Token method = consume(TokenType.IDENTIFIER, "Expect superclass method name.");
            return new Expr.Super(keyword, method);
        }

        if (match(TokenType.THIS)) return new Expr.This(previous());

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        return primaryError();

    }

    // primaryError  → binaryError ;
    // binaryError   → ( "!=" | "==" | ">" | ">=" | "<" | "<=" | "," | "-" | "+" | "/" | "*" ) expression ;
    private Expr primaryError() {
        // binaryError
        if (match(TokenType.BinaryOperators)) {
            ParseError err = error(previous(), "Expect expression before binary operator.");
            // consume right-hand operand, parsing and discarding it.
            expression();
            throw err;
        }

        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) throws ParseError {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous(); 
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;

            switch (peek().type) {
                case CLASS:  
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default:
                    advance();
            }
        }
    }
}
