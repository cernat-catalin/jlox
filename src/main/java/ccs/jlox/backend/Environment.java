package ccs.jlox.backend;

import ccs.jlox.interm.VariableLocation;
import java.util.ArrayList;
import java.util.List;

final class Environment {
  private final Environment enclosing;
  private final List<Object> values = new ArrayList<>();

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  void define(Object value) {
    values.add(value);
  }

  private void assign(int slot, Object value) {
    values.set(slot, value);
  }

  void assignAt(VariableLocation variableLocation, Object value) {
    ancestor(variableLocation.depth()).assign(variableLocation.slot(), value);
  }

  private Object get(int slot) {
    return values.get(slot);
  }

  Object getAt(VariableLocation variableLocation) {
    return ancestor(variableLocation.depth()).get(variableLocation.slot());
  }

  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing;
    }
    return environment;
  }
}
