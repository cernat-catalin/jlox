package ccs.jlox.interm;

import ccs.jlox.Lox;
import ccs.jlox.ast.Expr;
import ccs.jlox.ast.Stmt;
import ccs.jlox.ast.Token;
import ccs.jlox.backend.Interpreter;
import ccs.jlox.error.ErrorHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver {
  private static final ErrorHandler ERROR_HANDLER = Lox.getErrorHandler();

  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;
  private ClassType currentClass = ClassType.NONE;

  public Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  public void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }

  private void resolve(Stmt stmt) {
    switch (stmt) {
      case Stmt.If ifStmt -> resolveIfStmt(ifStmt);
      case Stmt.Return returnStmt -> resolveReturnStmt(returnStmt);
      case Stmt.While whileStmt -> resolveWhileStmt(whileStmt);
      case Stmt.Expression exprStmt -> resolveExpressionStmt(exprStmt);
      case Stmt.Var varStmt -> resolveVarStmt(varStmt);
      case Stmt.Function functionStmt -> resolveFunctionStmt(functionStmt);
      case Stmt.Class classStmt -> resolveClassStmt(classStmt);
      case Stmt.Block blockStmt -> resolveBlockStmt(blockStmt);
      case Stmt.Import importStmt -> resolveImportStmt(importStmt);
    }
  }

  private void resolveIfStmt(Stmt.If stmt) {
    resolve(stmt.condition());
    resolve(stmt.thenBranch());
    if (stmt.elseBranch() != null) resolve(stmt.elseBranch());
  }

  private void resolveReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      ERROR_HANDLER.error(stmt.keyword(), "Can't return from top-level code.");
    }

    if (stmt.value() != null) {
      if (currentFunction == FunctionType.INITIALIZER) {
        ERROR_HANDLER.error(stmt.keyword(), "Can't return a value from an initializer.");
      }
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

  private void resolveVarStmt(Stmt.Var varStmt) {
    declare(varStmt.name());
    if (varStmt.initializer() != null) {
      resolve(varStmt.initializer());
    }
    define(varStmt.name());
  }

  private void resolveClassStmt(Stmt.Class stmt) {
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;
    declare(stmt.name());
    define(stmt.name());

    if (stmt.superclass() != null
        && stmt.name().lexeme().equals(stmt.superclass().name().lexeme())) {
      ERROR_HANDLER.error(stmt.superclass().name(), "A class can't inherit from itself.");
    }

    if (stmt.superclass() != null) {
      currentClass = ClassType.SUBCLASS;
      resolve(stmt.superclass());
    }

    if (stmt.superclass() != null) {
      beginScope();
      scopes.peek().put("super", true);
    }

    beginScope();
    scopes.peek().put("this", true);

    for (Stmt.Function method : stmt.methods()) {
      FunctionType declaration = FunctionType.METHOD;
      if (method.name().lexeme().equals("init")) {
        declaration = FunctionType.INITIALIZER;
      }
      resolveFunction(method, declaration);
    }

    endScope();

    if (stmt.superclass() != null) {
      endScope();
    }

    currentClass = enclosingClass;
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

  private void resolveBlockStmt(Stmt.Block blockStmt) {
    beginScope();
    resolve(blockStmt.statements());
    endScope();
  }

  private void resolveImportStmt(Stmt.Import importStmt) {
    // XXX: Need to do something here ?
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
      case Expr.This thisExpr -> resolveThisExpr(thisExpr);
      case Expr.Super superExpr -> resolveSuperExpr(superExpr);
      case Expr.ArrayCreation arrayCExpr -> resolveArrayCreationExpr(arrayCExpr);
      case Expr.ArrayIndex arrayIndex -> resolveArrayIndexExpr(arrayIndex);
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
      ERROR_HANDLER.error(expr.name(), "Can't read local variable in its own initializer.");
    }
    resolveLocal(expr, expr.name());
  }

  private void resolveAssignExpr(Expr.Assignment expr) {
    resolve(expr.value());

    // XXX: Need more cases here (ArrayIndex)?
    if (expr.variable() instanceof Expr.Variable variable) {
      resolveLocal(expr, variable.name());
    } else if (expr.variable() instanceof Expr.Get get) {
      resolve(get.object());
    } else if (expr.variable() instanceof Expr.ArrayIndex index) {
      resolve(index.array());
      resolve(index.idx());
    }
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

  private void resolveThisExpr(Expr.This thisExpr) {
    if (currentClass == ClassType.NONE) {
      ERROR_HANDLER.error(thisExpr.keyword(), "Can't use 'this' outside of a class.");
    }

    resolveLocal(thisExpr, thisExpr.keyword());
  }

  private void resolveSuperExpr(Expr.Super superExpr) {
    if (currentClass == ClassType.NONE) {
      ERROR_HANDLER.error(superExpr.keyword(), "Can't use 'super' outside of a class.");
    } else if (currentClass != ClassType.SUBCLASS) {
      ERROR_HANDLER.error(superExpr.keyword(), "Can't use 'super' in a class with no superclass.");
    }

    resolveLocal(superExpr, superExpr.keyword());
  }

  private void resolveArrayCreationExpr(Expr.ArrayCreation arrayCExpr) {
    resolve(arrayCExpr.size());
  }

  private void resolveArrayIndexExpr(Expr.ArrayIndex arrayIndexExpr) {
    resolve(arrayIndexExpr.array());
    resolve(arrayIndexExpr.idx());
    // XXX: Need to do something here ?
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
      ERROR_HANDLER.error(name, "Already variable with this name in this scope.");
    }

    scope.put(name.lexeme(), false);
  }

  private void define(Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().put(name.lexeme(), true);
  }
}
