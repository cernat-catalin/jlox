package ccs.jlox.backend;

import ccs.jlox.ast.Token;
import ccs.jlox.error.RuntimeError;
import java.util.HashMap;
import java.util.Map;

final class Environment {
  private final Environment enclosing;
  private final Map<String, Object> values = new HashMap<>();

  Environment() {
    this.enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  void define(String name, Object value) {
    values.put(name, value);
  }

  boolean contains(Token name) {
    if (values.containsKey(name.lexeme())) {
      return true;
    }

    return enclosing != null && enclosing.contains(name);
  }

  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme())) {
      values.put(name.lexeme(), value);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }

    throw new RuntimeError(name, "Undefined variable '" + name.lexeme() + "'.");
  }

  void assignAt(int distance, Token name, Object value) {
    ancestor(distance).values.put(name.lexeme(), value);
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme())) {
      return values.get(name.lexeme());
    }

    if (enclosing != null) return enclosing.get(name);

    throw new RuntimeError(name, "Undefined variable '" + name.lexeme() + "'.");
  }

  Object getAt(int distance, String name) {
    return ancestor(distance).values.get(name);
  }

  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing;
    }
    return environment;
  }
}