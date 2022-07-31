package ccs.jlox.ffi;

import ccs.jlox.Interpreter;
import ccs.jlox.LoxCallable;
import ccs.jlox.RuntimeError;
import ccs.jlox.Token;
import java.util.List;

public class AssertFunction implements LoxCallable {
  @Override
  public int arity() {
    return 2;
  }

  @Override
  public Object call(Interpreter interpreter, Token callSite, List<Object> arguments) {
    boolean truthValue = (Boolean) arguments.get(0);
    String failureMessage = (String) arguments.get(1);

    if (!truthValue) {
      throw new RuntimeError(callSite, failureMessage);
    }

    return null;
  }

  @Override
  public String toString() {
    return "<native fn>";
  }
}
