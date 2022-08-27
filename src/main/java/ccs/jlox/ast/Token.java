package ccs.jlox.ast;

public record Token(TokenType type, String lexeme, Object literal, int line) {}
