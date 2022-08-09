package ccs.jlox;

import java.util.List;

public class LoxClass implements LoxCallable {
  final String name;

  LoxClass(String name) {
    this.name = name;
  }

  @Override
  public Object call(Interpreter interpreter, Token callSite, List<Object> arguments) {
    LoxInstance instance = new LoxInstance(this);
    return instance;
  }

  @Override
  public int arity() {
    return 0;
  }

  @Override
  public String toString() {
    return name;
  }
}
