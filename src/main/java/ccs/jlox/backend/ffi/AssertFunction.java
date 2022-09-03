package ccs.jlox.backend.ffi;

import ccs.jlox.ast.Token;
import ccs.jlox.backend.Interpreter;
import ccs.jlox.backend.LoxCallable;
import ccs.jlox.error.RuntimeError;
import java.util.List;

public final class AssertFunction implements LoxCallable, NativeFunction {
  @Override
  public int arity() {
    return 2;
  }

  @Override
  public Object call(Interpreter interpreter, Token callSite, List<Object> arguments) {
    boolean truthValue = (Boolean) arguments.get(0);
    String failureMessage = (String) arguments.get(1);

    if (!truthValue) {
      throw new RuntimeError(callSite.line(), failureMessage);
    }

    return null;
  }

  @Override
  public String toString() {
    return prettyName();
  }

  @Override
  public String getName() {
    return "assert";
  }
}
