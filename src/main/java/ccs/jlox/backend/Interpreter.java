package ccs.jlox.backend;

import ccs.jlox.error.ErrorHandler;
import ccs.jlox.Lox;
import ccs.jlox.error.RuntimeError;
import ccs.jlox.ast.Expr;
import ccs.jlox.ast.Stmt;
import ccs.jlox.ast.Token;
import ccs.jlox.ast.TokenType;
import ccs.jlox.backend.ffi.AssertFunction;
import ccs.jlox.backend.ffi.ClockFunction;
import ccs.jlox.backend.ffi.PrintFunction;
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
    globals.define("print", new PrintFunction());
    globals.define("clock", new ClockFunction());
    globals.define("assert", new AssertFunction());
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

  private void execute(Stmt stmt) {
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

  public void executeIfStmt(Stmt.If ifStmt) {
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
      case Expr.Literal lit -> evaluateLiteral(lit);
      case Expr.Logical log -> evaluateLogical(log);
      case Expr.Variable variable -> evaluateVariable(variable);
      case Expr.Assignment assignment -> evaluateAssignment(assignment);
      case Expr.Unary unary -> evaluateUnary(unary);
      case Expr.Binary binary -> evaluateBinary(binary);
      case Expr.Grouping group -> evaluateGrouping(group);
      case Expr.Call call -> evaluateCall(call);
      case Expr.Get get -> evaluateGet(get);
      case Expr.Set set -> evaluateSet(set);
      case Expr.This thisExpr -> evaluateThis(thisExpr);
      case Expr.Super superExpr -> evaluateSuper(superExpr);
    };
  }

  private Object evaluateLiteral(Expr.Literal lit) {
    return lit.value();
  }

  private Object evaluateLogical(Expr.Logical log) {
    Object left = evaluate(log.left());
    if (log.operator().type() == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }
    return evaluate(log.right());
  }

  private Object evaluateVariable(Expr.Variable variable) {
    return lookUpVariable(variable.name(), variable);
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

  private Object evaluateAssignment(Expr.Assignment assignment) {
    Object value = evaluate(assignment.value());
    int key = System.identityHashCode(assignment);
    Integer distance = locals.get(key);
    if (distance != null) {
      environment.assignAt(distance, assignment.name(), value);
    } else {
      globals.assign(assignment.name(), value);
    }
    return value;
  }

  private Object evaluateUnary(Expr.Unary unary) {
    Object right = evaluate(unary.right());

    return switch (unary.operator().type()) {
      case MINUS -> {
        checkNumberOperand(unary.operator(), right);
        yield -(double) right;
      }
      case BANG -> !isTruthy(right);
      default -> throw new IllegalStateException();
    };
  }

  private Object evaluateBinary(Expr.Binary binary) {
    Object left = evaluate(binary.left());
    Object right = evaluate(binary.right());

    return switch (binary.operator().type()) {
      case GREATER -> {
        checkNumberOperands(binary.operator(), left, right);
        yield (double) left > (double) right;
      }
      case GREATER_EQUAL -> {
        checkNumberOperands(binary.operator(), left, right);
        yield (double) left >= (double) right;
      }
      case LESS -> {
        checkNumberOperands(binary.operator(), left, right);
        yield (double) left < (double) right;
      }
      case LESS_EQUAL -> {
        checkNumberOperands(binary.operator(), left, right);
        yield (double) left <= (double) right;
      }
      case BANG_EQUAL -> !isEqual(left, right);
      case EQUAL_EQUAL -> isEqual(left, right);
      case MINUS -> {
        checkNumberOperands(binary.operator(), left, right);
        yield (double) left - (double) right;
      }
      case SLASH -> {
        checkNumberOperands(binary.operator(), left, right);
        yield (double) left / (double) right;
      }
      case STAR -> {
        checkNumberOperands(binary.operator(), left, right);
        yield (double) left * (double) right;
      }
      case PLUS -> {
        if (left instanceof Double dLeft && right instanceof Double dRight) {
          yield dLeft + dRight;
        }
        if (left instanceof String sLeft && right instanceof String sRight) {
          yield sLeft + sRight;
        }
        throw new RuntimeError(binary.operator(), "Operands must be two numbers or two strings.");
      }
      default -> new IllegalStateException();
    };
  }

  private Object evaluateGrouping(Expr.Grouping group) {
    return evaluate(group.expr());
  }

  private Object evaluateCall(Expr.Call call) {
    Object callee = evaluate(call.callee());
    List<Object> arguments = new ArrayList<>();
    for (Expr argument : call.arguments()) {
      arguments.add(evaluate(argument));
    }

    if (!(callee instanceof LoxCallable function)) {
      throw new RuntimeError(call.paren(), "Can only call functions and classes.");
    }

    if (arguments.size() != function.arity()) {
      throw new RuntimeError(
          call.paren(),
          "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
    }

    return function.call(this, call.paren(), arguments);
  }

  private Object evaluateGet(Expr.Get get) {
    Object object = evaluate(get.object());
    if (object instanceof LoxInstance loxInstance) {
      return loxInstance.get(get.name());
    }
    throw new RuntimeError(get.name(), "Only instances have properties.");
  }

  private Object evaluateSet(Expr.Set set) {
    Object object = evaluate(set.object());
    if (!(object instanceof LoxInstance loxInstance)) {
      throw new RuntimeError(set.name(), "Only instances have fields.");
    }
    Object value = evaluate(set.value());
    loxInstance.set(set.name(), value);
    return value;
  }

  private Object evaluateThis(Expr.This thisExpr) {
    return lookUpVariable(thisExpr.keyword(), thisExpr);
  }

  private Object evaluateSuper(Expr.Super superExpr) {
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
