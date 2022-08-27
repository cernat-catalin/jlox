package ccs.jlox.backend;

import ccs.jlox.ast.Token;
import java.util.List;

public interface LoxCallable {
  int arity();

  Object call(Interpreter interpreter, Token callSite, List<Object> arguments);
}
