package ccs.jlox.backend;

import ccs.jlox.ast.Token;
import ccs.jlox.error.RuntimeError;
import java.util.HashMap;
import java.util.Map;

final class LoxInstance {
  private final Map<String, Object> fields = new HashMap<>();
  private final LoxClass klass;

  LoxInstance(LoxClass klass) {
    this.klass = klass;
  }

  public Object get(Token name) {
    if (fields.containsKey(name.lexeme())) {
      return fields.get(name.lexeme());
    }

    LoxFunction method = klass.findMethod(name.lexeme());
    if (method != null) return method.bind(this);

    throw new RuntimeError(name, "Undefined property '" + name.lexeme() + "'.");
  }

  public void set(Token name, Object value) {
    fields.put(name.lexeme(), value);
  }

  @Override
  public String toString() {
    return klass.getName() + " instance";
  }
}
