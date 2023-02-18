package ccs.jlox.interm;

import static ccs.jlox.interm.VariableState.DECLARED;
import static ccs.jlox.interm.VariableState.DEFINED;
import static ccs.jlox.interm.VariableState.UNDECLARED;

import ccs.jlox.Lox;
import ccs.jlox.ast.Expr;
import ccs.jlox.ast.Stmt;
import ccs.jlox.ast.Token;
import ccs.jlox.error.ErrorHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

public class Resolver {
  private static final ErrorHandler ERROR_HANDLER = Lox.getErrorHandler();

  private final Stack<Map<String, VarStateSlot>> scopes = new Stack<>();
  private final Map<Integer, VariableLocation> locals = new HashMap<>();
  private final List<String> imports = new ArrayList<>();
  private FunctionType currentFunction = FunctionType.NONE;
  private ClassType currentClass = ClassType.NONE;

  public ResolverContext resolve(List<Stmt> statements) {
    _resolve(statements);
    return new ResolverContext(locals, imports);
  }

  private void _resolve(List<Stmt> statements) {
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
      case Stmt.Debug debugStmt -> resolveDebugStmt(debugStmt);
      case Stmt.Break breakStmt -> resolveBreakStmt(breakStmt);
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
      // XXX: Or is it initialized here?
      define("super");
    }

    beginScope();
    // XXX: Or is it initialized here?
    define("this");

    for (Stmt.Function method : stmt.methods()) {
      FunctionType declaration = FunctionType.METHOD;
      if (method.name().lexeme().equals("init")) {
        declaration = FunctionType.INITIALIZER;
      }
      resolveFunction(method.function(), declaration);
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
    resolveFunction(stmt.function(), FunctionType.FUNCTION);
  }

  private void resolveFunction(Expr.Function functionExpr, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

    beginScope();
    for (Token param : functionExpr.params()) {
      declare(param);
      define(param);
    }
    _resolve(functionExpr.body());
    endScope();

    currentFunction = enclosingFunction;
  }

  private void resolveBlockStmt(Stmt.Block blockStmt) {
    beginScope();
    _resolve(blockStmt.statements());
    endScope();
  }

  private void resolveImportStmt(Stmt.Import importStmt) {
    String qualifier = importStmt.name().lexeme();
    String fullyQualifiedName =
        importStmt.path().stream().map(Token::lexeme).collect(Collectors.joining("."));
    imports.add(fullyQualifiedName);
    define(qualifier);
  }

  private void resolveDebugStmt(Stmt.Debug debugStmt) {
    // NO-OP
  }

  private void resolveBreakStmt(Stmt.Break breakStmt) {
    // NO-OP
  }

  private void resolve(Expr expr) {
    switch (expr) {
      case Expr.Literal lit -> resolveLiteralExpr(lit);
      case Expr.Logical log -> resolveLogicalExpr(log);
      case Expr.Variable variable -> resolveVarExpr(variable);
      case Expr.Assignment assignment -> resolveAssignExpr(assignment);
      case Expr.Unary unary -> resolveUnaryExpr(unary);
      case Expr.Binary binary -> resolveBinaryExpr(binary);
      case Expr.Ternary ternary -> resolveTernaryExpr(ternary);
      case Expr.Grouping group -> resolveGroupingExpr(group);
      case Expr.Call call -> resolveCallExpr(call);
      case Expr.Get get -> resolveGetExpr(get);
      case Expr.This thisExpr -> resolveThisExpr(thisExpr);
      case Expr.Super superExpr -> resolveSuperExpr(superExpr);
      case Expr.ArrayCreation arrayCExpr -> resolveArrayCreationExpr(arrayCExpr);
      case Expr.ArrayIndex arrayIndex -> resolveArrayIndexExpr(arrayIndex);
      case Expr.Function functionExpr -> resolveFunctionExpr(functionExpr);
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
    if (!scopes.empty()) {
      VariableState variableState =
          Optional.ofNullable(scopes.peek().get(expr.name().lexeme()))
              .map(VarStateSlot::state)
              .orElse(UNDECLARED);
      if (variableState == DECLARED) {
        ERROR_HANDLER.error(expr.name(), "Can't read local variable in its own initializer.");
      }
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
      // XXX: We should be able to only access the key once
      if (scopes.get(i).containsKey(name.lexeme())) {
        int slot = scopes.get(i).get(name.lexeme()).slot;
        addResolvedExpression(expr, scopes.size() - 1 - i, slot);
        return;
      }
    }
  }

  private void addResolvedExpression(Expr expr, int depth, int slot) {
    int key = System.identityHashCode(expr);
    locals.put(key, new VariableLocation(depth, slot));
  }

  private void resolveBinaryExpr(Expr.Binary expr) {
    resolve(expr.left());
    resolve(expr.right());
  }

  private void resolveTernaryExpr(Expr.Ternary expr) {
    resolve(expr.condition());
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

  private void resolveFunctionExpr(Expr.Function functionExpr) {
    resolveFunction(functionExpr, FunctionType.FUNCTION);
  }

  private void beginScope() {
    scopes.push(new HashMap<>());
  }

  private void endScope() {
    scopes.pop();
  }

  private void declare(Token name) {
    if (scopes.isEmpty()) return;
    var scope = scopes.peek();

    if (scope.containsKey(name.lexeme())) {
      ERROR_HANDLER.error(name, "Already variable with this name in this scope.");
    }

    int nextSlot = scope.size();
    scope.put(name.lexeme(), new VarStateSlot(DECLARED, nextSlot));
  }

  private void define(Token name) {
    define(name.lexeme());
  }

  private void define(String lexeme) {
    if (scopes.isEmpty()) return;
    var scope = scopes.peek();
    int nextSlot = scope.size();
    scope.merge(
        lexeme,
        new VarStateSlot(DEFINED, nextSlot),
        (old, __) -> new VarStateSlot(DEFINED, old.slot()));
  }

  // XXX: Something else
  public record ResolverContext(Map<Integer, VariableLocation> locals, List<String> imports) {}

  private record VarStateSlot(VariableState state, int slot) {}
}
