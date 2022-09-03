package ccs.jlox.backend;

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
    // The distance is always zero
    int distance = 0;
    values.put(name, value);
  }

  private void assign(String name, Object value, int line) {
    if (values.containsKey(name)) {
      values.put(name, value);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(name, value, line);
      return;
    }

    throw new RuntimeError(line, "Undefined variable '" + name + "'.");
  }

  void assignAt(int distance, String name, Object value, int line) {
    ancestor(distance).assign(name, value, line);
  }

  private Object get(String name, int line) {
    if (values.containsKey(name)) {
      return values.get(name);
    }

    if (enclosing != null) return enclosing.get(name, line);

    throw new RuntimeError(line, "Undefined variable '" + name + "'.");
  }

  Object getAt(int distance, String name, int line) {
    return ancestor(distance).get(name, line);
  }

  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing;
    }
    return environment;
  }
}
