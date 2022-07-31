package ccs.jlox;

import java.util.List;

public final class Interpreter {
  private Environment environment = new Environment();

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
      case Stmt.Expression exprStmt -> executeExprStmt(exprStmt);
      case Stmt.Var varStmt -> executeVarStmt(varStmt);
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

  private void executeBlockStmt(Stmt.Block blockStmt) {
    Environment newEnvironment = new Environment(environment);
    Environment previousEnvironment = this.environment;

    try {
      this.environment = newEnvironment;
      for (Stmt stmt : blockStmt.statements()) {
        execute(stmt);
      }
    } finally {
      this.environment = previousEnvironment;
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
}
