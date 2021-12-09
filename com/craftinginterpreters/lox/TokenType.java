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
   MINUS, MINUS_MINUS, MINUS_EQUAL,
   PLUS, PLUS_PLUS, PLUS_EQUAL,

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

   /** List of token types that are parsed as increment operators. */
   public static final TokenType[] incrementOperators = {
      TokenType.PLUS_PLUS,
      TokenType.MINUS_MINUS
   };

   /** List of token types that are parsed as assignment operators. */
   public static final TokenType[] assignmentOperators = {
      TokenType.EQUAL,
      TokenType.PLUS_EQUAL,
      TokenType.MINUS_EQUAL
   };
}
