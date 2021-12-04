package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * Parses a Lox string by implementing recursive descent.
 * 
 * Full grammar implemented by this parser:
 * 
 * program        → declaration* EOF ;
 * declaration    → varDecl
 *                 | statement ;
 * varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
 * statement      → exprStmt
 *                 | forStmt
 *                 | ifElseStmt
 *                 | printStmt
 *                 | whileStmt
 *                 | loopKywd
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
 * block          → "{" declaration* "}" ;
 * expression     → assignment ;
 * assignment     → IDENTIFIER "=" assignment
 *                 | logic_or ;
 * logic_or       → logic_and ( "or" logic_and )* ;
 * logic_and      → conditional ( "and" conditional)* ;
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
 *                | IDENTIFIER
 *                | produceError ;
 * 
 * Error productions:
 * produceError  → binaryError ;
 * binaryError   → ( "!=" | "==" | ">" | ">=" | "<" | "<=" | "," | "-" | "+" | "/" | "*" ) expression ;
 */
public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    private int loopsInside = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // program → declaration* EOF ;
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    // declaration → varDecl | statement ;
    private Stmt declaration() {
        try {
            if (match(TokenType.VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
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

    // statement → exprStmt | ifElseStmt | printStmt | loopKywd | block ;
    private Stmt statement() {
        if (match(TokenType.IF)) return ifElseStatement();
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.BREAK, TokenType.CONTINUE)) return loopKeyword();
        if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
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

    // loopBody → exprStmt | forStmt | ifElseStmt | printStmt | whileStmt | loopKywd | loopBlock ;
    // private Stmt loopBody() {
    //     if (match(TokenType.IF)) return ifElseStatement();
    //     if (match(TokenType.PRINT)) return printStatement();
    //     if (match(TokenType.FOR)) return forStatement();
    //     if (match(TokenType.WHILE)) return whileStatement();
    //     if (match(TokenType.BREAK, TokenType.CONTINUE)) return loopKeyword();
    //     if (match(TokenType.LEFT_BRACE)) return loopBlock();

    //     return expressionStatement();
    // }

    // loopBlock      → "{" ( declaration | loopKywd )* "}" ;
    // private Stmt.Block loopBlock() {

    //     List<Stmt> statements = new ArrayList<>();

    //     while (!isAtEnd() && !check(TokenType.RIGHT_BRACE)) {

    //         if (match(TokenType.BREAK, TokenType.CONTINUE)) {
    //             statements.add(loopKeyword());
    //         } else {
    //             statements.add(declaration());
    //         }
    //     }

    //     return new Stmt.Block(statements);
    // }

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

    // expression → assignment ;
    private Expr expression() {
        return assignment();
    }

    // assignment → IDENTIFIER "=" assignment | logic_or ; 
    private Expr assignment() {
        Expr expr = or();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
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

    // primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER | produceError ;
    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NIL)) return new Expr.Literal(null);

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        return produceError();

    }

    // produceError  → binaryError ;
    // binaryError   → ( "!=" | "==" | ">" | ">=" | "<" | "<=" | "," | "-" | "+" | "/" | "*" ) expression ;
    private Expr produceError() {
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
