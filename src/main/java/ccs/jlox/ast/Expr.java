package ccs.jlox.ast;

import java.util.List;

public sealed interface Expr {
  record Binary(Expr left, Token operator, Expr right) implements Expr {}

  // We keep the token paren here for error reporting
  record Call(Expr callee, Token paren, List<Expr> arguments) implements Expr {}

  // Property access
  record Get(Expr object, Token name) implements Expr {}

  record This(Token keyword) implements Expr {}

  record Super(Token keyword, Token method) implements Expr {}

  record Grouping(Expr expr) implements Expr {}

  record Literal(Object value) implements Expr {}

  record Logical(Expr left, Token operator, Expr right) implements Expr {}

  record Unary(Token operator, Expr right) implements Expr {}

  record Variable(Token name) implements Expr {}

  record Assignment(Expr variable, Token equals, Expr value) implements Expr {}

  record ArrayCreation(Expr size, Token rightBracket) implements Expr {}

  record ArrayIndex(Expr array, Token rightParen, Expr idx) implements Expr {}
}
