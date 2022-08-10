package ccs.jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver {
  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;

  public Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  private void resolve(Stmt stmt) {
    switch (stmt) {
      case Stmt.If ifStmt -> resolveIfStmt(ifStmt);
      case Stmt.Return returnStmt -> resolveReturnStmt(returnStmt);
      case Stmt.While whileStmt -> resolveWhileStmt(whileStmt);
      case Stmt.Expression exprStmt -> resolveExpressionStmt(exprStmt);
      case Stmt.Var varStmt -> resolveVar(varStmt);
      case Stmt.Function functionStmt -> resolveFunctionStmt(functionStmt);
      case Stmt.Class classStmt -> resolveClassStmt(classStmt);
      case Stmt.Block blockStmt -> resolveBlock(blockStmt);
    }
  }

  private void resolveIfStmt(Stmt.If stmt) {
    resolve(stmt.condition());
    resolve(stmt.thenBranch());
    if (stmt.elseBranch() != null) resolve(stmt.elseBranch());
  }

  private void resolveReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword(), "Can't return from top-level code.");
    }

    if (stmt.value() != null) {
      resolve(stmt.value());
    }
  }

  private void resolveWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition());
    resolve(stmt.body());
  }

  private void resolveExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expr());
  }

  private void resolveVar(Stmt.Var varStmt) {
    declare(varStmt.name());
    if (varStmt.initializer() != null) {
      resolve(varStmt.initializer());
    }
    define(varStmt.name());
  }

  private void resolveClassStmt(Stmt.Class stmt) {
    declare(stmt.name());
    for (Stmt.Function method : stmt.methods()) {
      FunctionType declaration = FunctionType.METHOD;
      resolveFunction(method, declaration);
    }
    define(stmt.name());
  }

  private void resolveFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name());
    define(stmt.name());
    resolveFunction(stmt, FunctionType.FUNCTION);
  }

  private void resolveFunction(Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

    beginScope();
    for (Token param : function.params()) {
      declare(param);
      define(param);
    }
    resolve(function.body());
    endScope();

    currentFunction = enclosingFunction;
  }

  private void resolveBlock(Stmt.Block blockStmt) {
    beginScope();
    resolve(blockStmt.statements());
    endScope();
  }

  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }

  private void resolve(Expr expr) {
    switch (expr) {
      case Expr.Literal lit -> resolveLiteralExpr(lit);
      case Expr.Logical log -> resolveLogicalExpr(log);
      case Expr.Variable variable -> resolveVarExpr(variable);
      case Expr.Assignment assignment -> resolveAssignExpr(assignment);
      case Expr.Unary unary -> resolveUnaryExpr(unary);
      case Expr.Binary binary -> resolveBinaryExpr(binary);
      case Expr.Grouping group -> resolveGroupingExpr(group);
      case Expr.Call call -> resolveCallExpr(call);
      case Expr.Get get -> resolveGetExpr(get);
      case Expr.Set set -> resolveSetExpr(set);
    }
  }

  private void resolveLiteralExpr(Expr.Literal expr) {
    // NO-OP
  }

  private void resolveLogicalExpr(Expr.Logical expr) {
    resolve(expr.left());
    resolve(expr.right());
  }

  private void resolveVarExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() && scopes.peek().get(expr.name().lexeme()) == Boolean.FALSE) {
      Lox.error(expr.name(), "Can't read local variable in its own initializer.");
    }
    resolveLocal(expr, expr.name());
  }

  private void resolveAssignExpr(Expr.Assignment expr) {
    resolve(expr.value());
    resolveLocal(expr, expr.name());
  }

  private void resolveUnaryExpr(Expr.Unary expr) {
    resolve(expr.right());
  }

  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme())) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
  }

  private void resolveBinaryExpr(Expr.Binary expr) {
    resolve(expr.left());
    resolve(expr.right());
  }

  private void resolveGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expr());
  }

  private void resolveCallExpr(Expr.Call expr) {
    resolve(expr.callee());
    for (Expr argument : expr.arguments()) {
      resolve(argument);
    }
  }

  private void resolveGetExpr(Expr.Get expr) {
    resolve(expr.object());
  }

  private void resolveSetExpr(Expr.Set expr) {
    resolve(expr.value());
    resolve(expr.object());
  }

  private void beginScope() {
    scopes.push(new HashMap<>());
  }

  private void endScope() {
    scopes.pop();
  }

  private void declare(Token name) {
    if (scopes.isEmpty()) return;
    Map<String, Boolean> scope = scopes.peek();

    if (scope.containsKey(name.lexeme())) {
      Lox.error(name, "Already variable with this name in this scope.");
    }

    scope.put(name.lexeme(), false);
  }

  private void define(Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().put(name.lexeme(), true);
  }
}
