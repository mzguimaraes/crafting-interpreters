package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a Lox string by implementing recursive descent.
 * Grammar for the recursive descent is defined here: http://craftinginterpreters.com/parsing-expressions.html#ambiguity-and-the-parsing-game
 * 
 * Copied here for convenience:
 * program        → statement* EOF ;
 * statement      → exprStmt
 *                 | printStmt ;
 * exprStmt       → expression ";" ;
 * printStmt      → "print" expression ";" ;
 * expression     → conditional ;
 * conditional    → comma ( "?" expression ":" expression )* ; --right-associative
 * comma          → equality ( "," equality)* ;
 * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term           → factor ( ( "-" | "+" ) factor )* ;
 * factor         → unary ( ( "/" | "*" ) unary )* ;
 * unary          → ( "!" | "-" ) unary
 *                | primary ;
 * primary        → NUMBER | STRING | "true" | "false" | "nil"
 *                | "(" expression ")"
 *                | produce_error ;
 * produce_error  → binary_error ;
 * binary_error   → ( "!=" | "==" | ">" | ">=" | "<" | "<=" | "," | "-" | "+" | "/" | "*" ) expression ;
 */
public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // program → statement* EOF ;
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(statement());
        }

        return statements;
    }

    // statement → exprStmt | printStmt ;
    private Stmt statement() {
        if (match(TokenType.PRINT)) return printStatement();

        return expressionStatement();
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

    // expression → conditional ;
    private Expr expression() {
        return conditional();
    }

    // conditional → comma ( "?" expression ":" expression )* ; --right-associative
    private Expr conditional() {
        Expr predicate = comma();
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

    // comma → equality ( "," equality )* ;
    private Expr comma() {
        Expr expr = equality();
        while (match(TokenType.COMMA)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
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

    // unary → ( "!" | "-" ) unary | primary ;
    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        } 

        return primary();
    }

    // primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | produce_error ;
    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NIL)) return new Expr.Literal(null);

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        return produceError();

    }

    // produce_error  → binary_error ;
    // binary_error   → ( "!=" | "==" | ">" | ">=" | "<" | "<=" | "," | "-" | "+" | "/" | "*" ) expression ;
    private Expr produceError() {
        // binary_error
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

    private Token consume(TokenType type, String message) {
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
            }
        }

        advance();
    }
}
