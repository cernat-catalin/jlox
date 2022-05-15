package ccs.jlox;

public record Token(TokenType type, String lexeme, Object literal, int line) {}
