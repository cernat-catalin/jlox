package ccs.jlox.backend.ffi;

import ccs.jlox.ast.Token;
import ccs.jlox.backend.Interpreter;
import ccs.jlox.backend.LoxCallable;
import java.util.List;

public class SleepFunction implements LoxCallable, NativeFunction {
  @Override
  public int arity() {
    return 1;
  }

  @Override
  public Object call(Interpreter interpreter, Token callSite, List<Object> arguments) {
    double millis = (double) arguments.get(0);
    try {
      Thread.sleep((long) millis);
      return null;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return prettyName();
  }

  @Override
  public String getName() {
    return "sleep";
  }
}
