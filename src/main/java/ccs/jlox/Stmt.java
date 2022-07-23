package ccs.jlox;

public sealed interface Stmt {
  record Expression(Expr expr) implements Stmt {}
  record Print(Expr expr) implements Stmt {}
}
