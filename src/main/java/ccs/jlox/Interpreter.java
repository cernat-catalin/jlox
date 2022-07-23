package ccs.jlox;

import static ccs.jlox.Expr.Binary;
import static ccs.jlox.Expr.Grouping;
import static ccs.jlox.Expr.Literal;
import static ccs.jlox.Expr.Unary;

import java.util.List;

public final class Interpreter {
  public void interpret(List<Stmt> stmts) {
    try {
      for (Stmt stmt : stmts) {
        interpretStmt(stmt);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private void interpretStmt(Stmt stmt) {
    switch (stmt) {
      case Stmt.Print printStmt-> interpretPrintStmt(printStmt);
      case Stmt.Expression exprStmt -> interpretExprStmt(exprStmt);
    };
  }

  private void interpretPrintStmt(Stmt.Print printStmt) {
    Object value = interpretExpr(printStmt.expr());
    System.out.println(stringify(value));
  }

  private void interpretExprStmt(Stmt.Expression exprStmt) {
    interpretExpr(exprStmt.expr());
  }

  private Object interpretExpr(Expr expr) {
    return switch (expr) {
      case Literal lit -> interpretLiteral(lit);
      case Unary unary -> interpretUnary(unary);
      case Binary binary -> interpretBinary(binary);
      case Grouping group -> interpretGrouping(group);
    };
  }

  private Object interpretLiteral(Literal lit) {
    return lit.value();
  }

  private Object interpretGrouping(Grouping group) {
    return interpretExpr(group.expr());
  }

  private Object interpretUnary(Unary unary) {
    Object right = interpretExpr(unary.right());

    return switch (unary.operator().type()) {
      case MINUS -> {
        checkNumberOperand(unary.operator(), right);
        yield -(double) right;
      }
      case BANG -> !isTruthy(right);
      default -> throw new IllegalStateException();
    };
  }

  private Object interpretBinary(Binary binary) {
    Object left = interpretExpr(binary.left());
    Object right = interpretExpr(binary.right());

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
