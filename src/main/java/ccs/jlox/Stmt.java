package ccs.jlox;

public sealed interface Stmt {
  record Print(Expr expr) implements Stmt {}

  record Expression(Expr expr) implements Stmt {}

  record Var(Token name, Expr initializer) implements Stmt {}
}
