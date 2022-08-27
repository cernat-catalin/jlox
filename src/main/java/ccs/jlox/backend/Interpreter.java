package ccs.jlox.backend;

import ccs.jlox.Lox;
import ccs.jlox.ast.Expr;
import ccs.jlox.ast.Stmt;
import ccs.jlox.ast.Token;
import ccs.jlox.ast.TokenType;
import ccs.jlox.backend.ffi.AssertFunction;
import ccs.jlox.backend.ffi.ClockFunction;
import ccs.jlox.backend.ffi.NativeFunction;
import ccs.jlox.backend.ffi.PrintFunction;
import ccs.jlox.error.ErrorHandler;
import ccs.jlox.error.RuntimeError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Interpreter {
  private static final ErrorHandler ERROR_HANDLER = Lox.getErrorHandler();

  private final Environment globals = new Environment();
  private final Map<Integer, Integer> locals = new HashMap<>();
  private Environment environment = globals;

  public Interpreter() {
    addNativeFunction(new PrintFunction());
    addNativeFunction(new ClockFunction());
    addNativeFunction(new AssertFunction());
  }

  private void addNativeFunction(NativeFunction nativeFunction) {
    globals.define(nativeFunction.getName(), nativeFunction);
  }

  public void execute(List<Stmt> stmts) {
    try {
      for (Stmt stmt : stmts) {
        execute(stmt);
      }
    } catch (RuntimeError error) {
      ERROR_HANDLER.runtimeError(error);
    }
  }

  public void execute(Stmt stmt) {
    switch (stmt) {
      case Stmt.If ifStmt -> executeIfStmt(ifStmt);
      case Stmt.Return returnStmt -> executeReturnStmt(returnStmt);
      case Stmt.While whileStmt -> executeWhileStmt(whileStmt);
      case Stmt.Expression exprStmt -> executeExprStmt(exprStmt);
      case Stmt.Var varStmt -> executeVarStmt(varStmt);
      case Stmt.Function functionStmt -> executeFunctionStmt(functionStmt);
      case Stmt.Class classStmt -> executeClassStmt(classStmt);
      case Stmt.Block blockStmt -> executeBlockStmt(blockStmt);
    }
  }

  private void executeIfStmt(Stmt.If ifStmt) {
    if (isTruthy(evaluate(ifStmt.condition()))) {
      execute(ifStmt.thenBranch());
    } else if (ifStmt.elseBranch() != null) {
      execute(ifStmt.elseBranch());
    }
  }

  private void executeReturnStmt(Stmt.Return returnStmt) {
    Object value = null;
    if (returnStmt.value() != null) value = evaluate(returnStmt.value());
    throw new Return(value);
  }

  private void executeWhileStmt(Stmt.While whileStmt) {
    while (isTruthy(evaluate(whileStmt.condition()))) {
      execute(whileStmt.body());
    }
  }

  private void executeExprStmt(Stmt.Expression exprStmt) {
    evaluate(exprStmt.expr());
  }

  private void executeVarStmt(Stmt.Var varStmt) {
    Object value = null;
    if (varStmt.initializer() != null) {
      value = evaluate(varStmt.initializer());
    }
    environment.define(varStmt.name().lexeme(), value);
  }

  private void executeFunctionStmt(Stmt.Function functionStmt) {
    LoxFunction function = new LoxFunction(functionStmt, environment, false);
    environment.define(functionStmt.name().lexeme(), function);
  }

  private void executeClassStmt(Stmt.Class classStmt) {
    Object superclass = null;
    if (classStmt.superclass() != null) {
      superclass = evaluate(classStmt.superclass());
      if (!(superclass instanceof LoxClass)) {
        throw new RuntimeError(classStmt.superclass().name(), "Superclass must be a class.");
      }
    }
    // XXX: Why two step process (first define class then assign it)?
    environment.define(classStmt.name().lexeme(), null);

    if (classStmt.superclass() != null) {
      environment = new Environment(environment);
      environment.define("super", superclass);
    }

    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : classStmt.methods()) {
      LoxFunction function =
          new LoxFunction(method, environment, method.name().lexeme().equals("init"));
      methods.put(method.name().lexeme(), function);
    }

    LoxClass klass = new LoxClass(classStmt.name().lexeme(), (LoxClass) superclass, methods);

    if (superclass != null) {
      environment = environment.ancestor(1);
    }

    environment.assign(classStmt.name(), klass);
  }

  private void executeBlockStmt(Stmt.Block blockStmt) {
    executeBlock(blockStmt.statements(), new Environment(environment));
  }

  // XXX: Keep like this?
  public void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;
      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  private Object evaluate(Expr expr) {
    return switch (expr) {
      case Expr.Literal lit -> evaluateLiteralExpr(lit);
      case Expr.Logical log -> evaluateLogicalExpr(log);
      case Expr.Variable variable -> evaluateVariableExpr(variable);
      case Expr.Assignment assignment -> evaluateAssignmentExpr(assignment);
      case Expr.Unary unary -> evaluateUnaryExpr(unary);
      case Expr.Binary binary -> evaluateBinaryExpr(binary);
      case Expr.Grouping group -> evaluateGroupingExpr(group);
      case Expr.Call call -> evaluateCallExpr(call);
      case Expr.Get get -> evaluateGetExpr(get);
      case Expr.Set set -> evaluateSetExpr(set);
      case Expr.This thisExpr -> evaluateThisExpr(thisExpr);
      case Expr.Super superExpr -> evaluateSuperExpr(superExpr);
    };
  }

  private Object evaluateLiteralExpr(Expr.Literal literalExpr) {
    return literalExpr.value();
  }

  private Object evaluateLogicalExpr(Expr.Logical logExpr) {
    Object left = evaluate(logExpr.left());
    if (logExpr.operator().type() == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }
    return evaluate(logExpr.right());
  }

  private Object evaluateVariableExpr(Expr.Variable variableExpr) {
    return lookUpVariable(variableExpr.name(), variableExpr);
  }

  private Object lookUpVariable(Token name, Expr expr) {
    int key = System.identityHashCode(expr);
    Integer distance = locals.get(key);
    if (distance != null) {
      return environment.getAt(distance, name.lexeme());
    } else {
      return globals.get(name);
    }
  }

  private Object evaluateAssignmentExpr(Expr.Assignment assignmentExpr) {
    Object value = evaluate(assignmentExpr.value());
    int key = System.identityHashCode(assignmentExpr);
    Integer distance = locals.get(key);
    if (distance != null) {
      environment.assignAt(distance, assignmentExpr.name(), value);
    } else {
      globals.assign(assignmentExpr.name(), value);
    }
    return value;
  }

  private Object evaluateUnaryExpr(Expr.Unary unaryExpr) {
    Object right = evaluate(unaryExpr.right());

    return switch (unaryExpr.operator().type()) {
      case MINUS -> {
        checkNumberOperand(unaryExpr.operator(), right);
        yield -(double) right;
      }
      case BANG -> !isTruthy(right);
      default -> throw new IllegalStateException();
    };
  }

  private Object evaluateBinaryExpr(Expr.Binary binaryExpr) {
    Object left = evaluate(binaryExpr.left());
    Object right = evaluate(binaryExpr.right());

    return switch (binaryExpr.operator().type()) {
      case GREATER -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left > (double) right;
      }
      case GREATER_EQUAL -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left >= (double) right;
      }
      case LESS -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left < (double) right;
      }
      case LESS_EQUAL -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left <= (double) right;
      }
      case BANG_EQUAL -> !isEqual(left, right);
      case EQUAL_EQUAL -> isEqual(left, right);
      case MINUS -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left - (double) right;
      }
      case SLASH -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left / (double) right;
      }
      case STAR -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left * (double) right;
      }
      case PLUS -> {
        if (left instanceof Double dLeft && right instanceof Double dRight) {
          yield dLeft + dRight;
        }
        if (left instanceof String sLeft && right instanceof String sRight) {
          yield sLeft + sRight;
        }
        throw new RuntimeError(
            binaryExpr.operator(), "Operands must be two numbers or two strings.");
      }
      default -> new IllegalStateException();
    };
  }

  private Object evaluateGroupingExpr(Expr.Grouping groupExpr) {
    return evaluate(groupExpr.expr());
  }

  private Object evaluateCallExpr(Expr.Call callExpr) {
    Object callee = evaluate(callExpr.callee());
    List<Object> arguments = new ArrayList<>();
    for (Expr argument : callExpr.arguments()) {
      arguments.add(evaluate(argument));
    }

    if (!(callee instanceof LoxCallable function)) {
      throw new RuntimeError(callExpr.paren(), "Can only call functions and classes.");
    }

    if (arguments.size() != function.arity()) {
      throw new RuntimeError(
          callExpr.paren(),
          "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
    }

    return function.call(this, callExpr.paren(), arguments);
  }

  private Object evaluateGetExpr(Expr.Get getExpr) {
    Object object = evaluate(getExpr.object());
    if (object instanceof LoxInstance loxInstance) {
      return loxInstance.get(getExpr.name());
    }
    throw new RuntimeError(getExpr.name(), "Only instances have properties.");
  }

  private Object evaluateSetExpr(Expr.Set setExpr) {
    Object object = evaluate(setExpr.object());
    if (!(object instanceof LoxInstance loxInstance)) {
      throw new RuntimeError(setExpr.name(), "Only instances have fields.");
    }
    Object value = evaluate(setExpr.value());
    loxInstance.set(setExpr.name(), value);
    return value;
  }

  private Object evaluateThisExpr(Expr.This thisExpr) {
    return lookUpVariable(thisExpr.keyword(), thisExpr);
  }

  private Object evaluateSuperExpr(Expr.Super superExpr) {
    int key = System.identityHashCode(superExpr);
    int distance = locals.get(key);
    LoxClass superClass = (LoxClass) environment.getAt(distance, "super");
    LoxInstance object = (LoxInstance) environment.getAt(distance - 1, "this");
    LoxFunction method = superClass.findMethod(superExpr.method().lexeme());

    if (method == null) {
      throw new RuntimeError(
          superExpr.method(), "Undefined property '" + superExpr.method().lexeme() + "'.");
    }

    return method.bind(object);
  }

  // XXX: Ugly hack. Fix this!
  public void resolve(Expr expr, int depth) {
    int key = System.identityHashCode(expr);
    locals.put(key, depth);
  }

  private static boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;
    return a.equals(b);
  }

  private static boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean) object;
    return true;
  }

  private static void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private static void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be numbers.");
  }
}
