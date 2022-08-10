package ccs.jlox;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
  final String name;
  private final Map<String, LoxFunction> methods;

  LoxClass(String name, Map<String, LoxFunction> methods) {
    this.name = name;
    this.methods = methods;
  }

  @Override
  public Object call(Interpreter interpreter, Token callSite, List<Object> arguments) {
    LoxInstance instance = new LoxInstance(this);
    return instance;
  }

  LoxFunction findMethod(String name) {
    if (methods.containsKey(name)) {
      return methods.get(name);
    }
    return null;
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
