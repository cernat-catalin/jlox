package ccs.jlox.backend;

import ccs.jlox.ast.Token;
import ccs.jlox.backend.ffi.AssertFunction;
import ccs.jlox.backend.ffi.ClockFunction;
import ccs.jlox.backend.ffi.NativeFunction;
import ccs.jlox.backend.ffi.PrintFunction;
import ccs.jlox.backend.ffi.SleepFunction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class LoxModule {
  private final String name;
  // XXX: Keep token or move to <String> ?
  private final List<Token> path;

  private final Environment globals = new Environment();
  private final Map<Integer, Integer> locals = new HashMap<>();
  private Environment environment = globals;

  LoxModule(String name, List<Token> path) {
    this.name = name;
    this.path = path;

    addNativeFunction(new PrintFunction());
    addNativeFunction(new ClockFunction());
    addNativeFunction(new AssertFunction());
    addNativeFunction(new SleepFunction());
  }

  private void addNativeFunction(NativeFunction nativeFunction) {
    globals.define(nativeFunction.getName(), nativeFunction);
  }

  String getName() {
    return name;
  }

  Environment getGlobals() {
    return globals;
  }

  Environment getEnvironment() {
    return environment;
  }

  void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  Map<Integer, Integer> getLocals() {
    return locals;
  }
}
