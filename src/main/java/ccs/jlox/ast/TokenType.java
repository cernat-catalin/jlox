package ccs.jlox.ast;

public enum TokenType {
  // single character tokens
  LEFT_PAREN,
  RIGHT_PAREN,
  LEFT_SQUARE_BRACKET,
  RIGHT_SQUARE_BRACKET,
  LEFT_BRACE,
  RIGHT_BRACE,
  COMMA,
  DOT,
  SEMICOLON,
  QUESTION_MARK,
  COLON,

  // one or two character tokens
  PLUS,
  PLUS_EQUAL,
  STAR,
  STAR_EQUAL,
  MINUS,
  MINUS_EQUAL,
  SLASH,
  SLASH_EQUAL,
  BANG,
  BANG_EQUAL,
  EQUAL,
  EQUAL_EQUAL,
  GREATER,
  GREATER_EQUAL,
  LESS,
  LESS_EQUAL,

  // literals
  IDENTIFIER,
  STRING,
  NUMBER,

  // keywords
  AND,
  AS,
  BREAK,
  CLASS,
  DEBUG,
  ELSE,
  FALSE,
  FUN,
  FOR,
  IF,
  IMPORT,
  NIL,
  OR,
  RETURN,
  SUPER,
  THIS,
  TRUE,
  VAR,
  WHILE,
  EOF
}
