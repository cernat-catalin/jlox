package ccs.jlox;

public final class AstPrinter {
  public String print(Expr expr) {
    return switch (expr) {
      case Expr.Literal lit -> (lit.value() == null) ? "nil" : lit.value().toString();
      case Expr.Logical log -> parenthesize(log.operator().lexeme(), log.left(), log.right());
      case Expr.Unary unary -> parenthesize(unary.operator().lexeme(), unary.right());
      case Expr.Binary binary -> parenthesize(
          binary.operator().lexeme(), binary.left(), binary.right());
      case Expr.Call call -> print(call.callee());
      case Expr.Grouping grouping -> parenthesize("group", grouping.expr());
      case Expr.Variable variable -> parenthesize("var", variable);
      case Expr.Assignment assignment -> parenthesize("assign", assignment);
    };
  }

  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ");
      builder.append(print(expr));
    }
    builder.append(")");
    return builder.toString();
  }

  public static void main(String[] args) {
    Expr expression =
        new Expr.Binary(
            new Expr.Unary(new Token(TokenType.MINUS, "-", null, 1), new Expr.Literal(123)),
            new Token(TokenType.STAR, "*", null, 1),
            new Expr.Grouping(new Expr.Literal(45.67)));
    System.out.println(new AstPrinter().print(expression));
  }
}
