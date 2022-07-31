package ccs.jlox;

import java.util.List;

public class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;

  LoxFunction(Stmt.Function declaration) {
    this.declaration = declaration;
  }

  @Override
  public Object call(Interpreter interpreter, Token callSite, List<Object> arguments) {
    Environment environment = new Environment(interpreter.getGlobals());
    for (int i = 0; i < declaration.params().size(); i++) {
      environment.define(declaration.params().get(i).lexeme(), arguments.get(i));
    }
    try {
      interpreter.executeBlock(declaration.body(), environment);
    } catch (Return returnValue) {
      return returnValue.value;
    }
    return null;
  }

  @Override
  public int arity() {
    return declaration.params().size();
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name().lexeme() + ">";
  }
}