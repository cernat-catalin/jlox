package ccs.jlox.backend.ffi;

import ccs.jlox.ast.Token;
import ccs.jlox.backend.Interpreter;
import ccs.jlox.backend.LoxCallable;
import java.util.List;

public final class ClockFunction implements LoxCallable, NativeFunction {
  @Override
  public int arity() {
    return 0;
  }

  @Override
  public Object call(Interpreter interpreter, Token callSite, List<Object> arguments) {
    return System.currentTimeMillis();
  }

  @Override
  public String toString() {
    return prettyName();
  }

  @Override
  public String getName() {
    return "clock";
  }
}
