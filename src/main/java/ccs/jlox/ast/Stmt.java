package ccs.jlox.ast;

import java.util.List;

public sealed interface Stmt {
  record Expression(Expr expr) implements Stmt {}

  record If(Expr condition, Stmt thenBranch, Stmt elseBranch) implements Stmt {}

  record While(Expr condition, Stmt body) implements Stmt {}

  record Var(Token name, Expr initializer) implements Stmt {}

  record Function(Token name, List<Token> params, List<Stmt> body) implements Stmt {}

  record Class(Token name, Expr.Variable superclass, List<Function> methods) implements Stmt {}

  record Return(Token keyword, Expr value) implements Stmt {}

  record Block(List<Stmt> statements) implements Stmt {}

  record Import(List<Token> path, Token name) implements Stmt {}

  record Debug(int line) implements Stmt {}

  record Break(int line) implements Stmt {}
}
