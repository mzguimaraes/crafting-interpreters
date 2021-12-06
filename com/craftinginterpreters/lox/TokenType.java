package com.craftinginterpreters.lox;

enum TokenType {
   // Single-char tokens. 
   LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, COMMA, DOT, SEMICOLON, SLASH, STAR, BREAK, CONTINUE,

   // Ternary components (treated as single-char tokens).
   QUESTION_MARK, COLON,

   // One or two char tokens.
   BANG, BANG_EQUAL, 
   EQUAL, EQUAL_EQUAL, 
   GREATER, GREATER_EQUAL,
   LESS, LESS_EQUAL,
   MINUS, MINUS_MINUS,
   PLUS, PLUS_PLUS,

   // Literals.
   IDENTIFIER, STRING, NUMBER,

   // Keywords.
   AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR, PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

   EOF;

   /** List of token types that can be parsed as binary operators. */
   public static final TokenType[] BinaryOperators = {
      TokenType.COMMA,

      TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL,

      TokenType.LESS, TokenType.LESS_EQUAL,
      TokenType.GREATER, TokenType.GREATER_EQUAL,

      TokenType.MINUS, TokenType.PLUS,
      TokenType.SLASH, TokenType.STAR
   };

   /** List of token types that are parsed as loop keywords. */
   public static final TokenType[] loopKeywords = {
      TokenType.BREAK, 
      TokenType.CONTINUE
   };

   public static final TokenType[] incrementOperators = {
      TokenType.PLUS_PLUS,
      TokenType.MINUS_MINUS
   };
}
