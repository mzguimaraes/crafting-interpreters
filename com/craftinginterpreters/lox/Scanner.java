package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",      TokenType.AND);
        keywords.put("class",    TokenType.CLASS);
        keywords.put("else",     TokenType.ELSE);
        keywords.put("false",    TokenType.FALSE);
        keywords.put("for",      TokenType.FOR);
        keywords.put("fun",      TokenType.FUN);
        keywords.put("if",       TokenType.IF);
        keywords.put("nil",      TokenType.NIL);
        keywords.put("or",       TokenType.OR);
        keywords.put("print",    TokenType.PRINT);
        keywords.put("return",   TokenType.RETURN);
        keywords.put("super",    TokenType.SUPER);
        keywords.put("this",     TokenType.THIS);
        keywords.put("true",     TokenType.TRUE);
        keywords.put("var",      TokenType.VAR);
        keywords.put("while",    TokenType.WHILE);
        keywords.put("break",    TokenType.BREAK);
        keywords.put("continue", TokenType.CONTINUE);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // we are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    List<Token> scanTokensInteractive() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        // if user entered an expression without a terminating semicolon,
        // add a semicolon to the token string to allow REPL to parse the statement.
        if (tokens.get(tokens.size() - 1).type != TokenType.SEMICOLON) {
            tokens.add(new Token(TokenType.SEMICOLON, ";", null, line));
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            // single-char tokens.
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case '-': addToken(TokenType.MINUS); break;
            case '+': addToken(TokenType.PLUS); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;
            case '?': addToken(TokenType.QUESTION_MARK); break;
            case ':': addToken(TokenType.COLON); break;

            case '.': 
                // check if this characgter is a leading dot for a number.
                if (isDigit(peek())) {
                    number();
                } else {
                    addToken(TokenType.DOT); 
                }
                break;

            // single- or double-char tokens.
            case '!': 
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;

            case '/':
                if (match('/')) {
                    // comments go to end of line.
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    // C-style block comments.
                    while ((peek() != '*' || peekNext() != '/') && !isAtEnd()) {
                        if (peek() == '\n') line++;
                        advance();
                    }
                    // consume closing star-slash chars.
                    advance();
                    advance();
                } else {
                    addToken(TokenType.SLASH);
                }
                break;

            // Ignore whitespace.
            case ' ':
            case '\r':
            case '\t':
                break;
            
            case '\n':
                line++;
                break;
            
            // strings
            case '"': string('"'); break;
            case '\'': string('\''); break;

            default:
                // catch number literals, identifiers, keywords, and unparseable tokens.
                if (isDigit(c)) {
                    number();
                    break;
                } else if (isAlpha(c)) {
                    identifier();
                    break;
                } else {
                    // unparseable tokens.
                    Lox.error(line, "Unexpected character '" + c + "'.");
                    break;
                }
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        // addToken(TokenType.IDENTIFIER);
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void number() {
        if (peek() == '.') advance();

        while (isDigit(peek())) advance();

        // Look for a non-integral part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the dot character.
            advance();
        }

        while (isDigit(peek())) advance();

        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private void string(char terminator) {
        // tracks if current char is part of an escape sequence.  
        // used to prevent escaped terminators like "\"" from prematurely ending the string scan.
        boolean isEscapeActive = false;
        while ((peek() != terminator || isEscapeActive) && !isAtEnd()) {
            if (peek() == '\n') line++;
            isEscapeActive = !isEscapeActive && peek() == '\\'; // only flip to true if is false and we see a \
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // consume the closing quote mark.
        advance();

        // Trim off the enclosing quote marks.
        String value = source.substring(start + 1, current - 1);

        // escape any escape sequences present.
        try {
            value = StringUtils.unescapeString(value);
        } catch (Exception e) {
            Lox.error(line, e.getMessage());
        }
        addToken(TokenType.STRING, value);
    }

    // looks ahead one character, returning the value seen.
    // returns EOF if encountered.
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    // looks ahead two characters, returning the value seen.
    // returns EOF if encountered while looking ahead.
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    // if current character matches expected, returns true and consumes current.  else returns false.
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    // advances current character pointer, returning the old value pointed at.
    private char advance() {
        return source.charAt(current++);
    }

    // records a token with no literal value.
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    // records a token with a literal value attached, such as a string.
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}