package ccs.jlox.backend.ffi;

import ccs.jlox.ast.Token;
import ccs.jlox.backend.Interpreter;
import ccs.jlox.backend.LoxCallable;
import java.util.List;

public final class PrintFunction implements LoxCallable, NativeFunction {
  @Override
  public int arity() {
    return 1;
  }

  @Override
  public Object call(Interpreter interpreter, Token callSite, List<Object> arguments) {
    Object object = arguments.get(0);
    System.out.println(stringify(object));
    return null;
  }

  @Override
  public String toString() {
    return prettyName();
  }

  @Override
  public String getName() {
    return "print";
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
