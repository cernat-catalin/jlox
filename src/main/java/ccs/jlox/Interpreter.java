package ccs.jlox;

import java.util.ArrayList;
import java.util.List;

public final class Interpreter {
  private final Environment globals = new Environment();
  private Environment environment = globals;

  Interpreter() {
    globals.define(
        "clock",
        new LoxCallable() {
          @Override
          public int arity() {
            return 0;
          }

          @Override
          public Object call(Interpreter interpreter, List<Object> arguments) {
            return System.currentTimeMillis() / 1000.0;
          }

          @Override
          public String toString() {
            return "<native fn>";
          }
        });
  }

  public void execute(List<Stmt> stmts) {
    try {
      for (Stmt stmt : stmts) {
        execute(stmt);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private void execute(Stmt stmt) {
    switch (stmt) {
      case Stmt.If ifStmt -> executeIfStmt(ifStmt);
      case Stmt.Print printStmt -> executePrintStmt(printStmt);
      case Stmt.Return returnStmt -> executeReturnStmt(returnStmt);
      case Stmt.While whileStmt -> executeWhileStmt(whileStmt);
      case Stmt.Expression exprStmt -> executeExprStmt(exprStmt);
      case Stmt.Var varStmt -> executeVarStmt(varStmt);
      case Stmt.Function functionStmt -> executeFunctionStmt(functionStmt);
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

  private void executePrintStmt(Stmt.Print printStmt) {
    Object value = evaluate(printStmt.expr());
    System.out.println(stringify(value));
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
    LoxFunction function = new LoxFunction(functionStmt);
    environment.define(functionStmt.name().lexeme(), function);
  }

  private void executeBlockStmt(Stmt.Block blockStmt) {
    executeBlock(blockStmt.statements(), new Environment(environment));
  }

  void executeBlock(List<Stmt> statements, Environment environment) {
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
    return environment.get(variable.name());
  }

  private Object evaluateAssignment(Expr.Assignment assignment) {
    Object value = evaluate(assignment.value());
    environment.assign(assignment.name(), value);
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

    return function.call(this, arguments);
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

  private static String stringify(Object object) {
    if (object == null) return "nil";
    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }
    return object.toString();
  }

  public Environment getGlobals() {
    return globals;
  }
}
