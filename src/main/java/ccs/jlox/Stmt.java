package ccs.jlox;

import java.util.List;

public sealed interface Stmt {
  record Expression(Expr expr) implements Stmt {}

  record If(Expr condition, Stmt thenBranch, Stmt elseBranch) implements Stmt {}

  record Var(Token name, Expr initializer) implements Stmt {}

  record Print(Expr expr) implements Stmt {}

  record Block(List<Stmt> statements) implements Stmt {}
}
