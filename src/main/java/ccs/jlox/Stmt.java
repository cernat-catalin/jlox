package ccs.jlox;

import java.util.List;

public sealed interface Stmt {
  record Print(Expr expr) implements Stmt {}

  record Expression(Expr expr) implements Stmt {}

  record Var(Token name, Expr initializer) implements Stmt {}

  record Block(List<Stmt> statements) implements Stmt {}
}
