package ccs.jlox.backend;

import ccs.jlox.ast.Expr;
import ccs.jlox.ast.Token;
import ccs.jlox.interm.VariableLocation;
import java.util.List;

final class LoxFunction implements LoxCallable {
  private final String namespace;
  private final String name;
  private final Expr.Function functionExpr;
  private final Environment closure;
  private final boolean isInitializer;

  LoxFunction(
      String namespace,
      String name,
      Expr.Function functionExpr,
      Environment closure,
      boolean isInitializer) {
    this.name = name;
    this.namespace = namespace;
    this.functionExpr = functionExpr;
    this.closure = closure;
    this.isInitializer = isInitializer;
  }

  @Override
  public Object call(Interpreter interpreter, Token callSite, List<Object> arguments) {
    Environment environment = new Environment(closure);
    for (int i = 0; i < functionExpr.params().size(); i++) {
      environment.define(arguments.get(i));
    }
    String previousNamespace = interpreter.getCurrentNamespace();
    try {
      interpreter.setCurrentNamespace(namespace);
      interpreter.executeBlock(functionExpr.body(), environment);
    } catch (Return returnValue) {
      if (isInitializer) {
        // XXX: Hacky. We know that this was the first thing defined in the closure environment
        return closure.getAt(new VariableLocation(0, 0));
      }
      return returnValue.getValue();
    } finally {
      interpreter.setCurrentNamespace(previousNamespace);
    }

    // XXX: Hacky. We know that this was the first thing defined in the closure environment
    if (isInitializer) return closure.getAt(new VariableLocation(0, 0));
    return null;
  }

  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    // Define "this"
    environment.define(instance);
    return new LoxFunction(namespace, name, functionExpr, environment, isInitializer);
  }

  @Override
  public int arity() {
    return functionExpr.params().size();
  }

  @Override
  public String toString() {
    return String.format("<fn %s>", name);
  }
}
