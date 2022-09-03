package ccs.jlox.backend;

import ccs.jlox.ast.Stmt;
import ccs.jlox.backend.ffi.AssertFunction;
import ccs.jlox.backend.ffi.ClockFunction;
import ccs.jlox.backend.ffi.NativeFunction;
import ccs.jlox.backend.ffi.PrintFunction;
import ccs.jlox.backend.ffi.SleepFunction;
import java.util.List;
import java.util.Map;

// XXX: Move that non interpreter stuff outside (i.e. locals + statements)
// XXX: Parse every module first
public final class LoxModule {
  private final String fullyQualifiedName;
  private final Environment globals = new Environment();
  private final Map<Integer, Integer> locals;
  private Environment environment = globals;

  LoxModule(String fullyQualifiedName, Map<Integer, Integer> locals) {
    this.fullyQualifiedName = fullyQualifiedName;
    this.locals = locals;

    // XXX; Find a way to only define them once
    addNativeFunction(new PrintFunction());
    addNativeFunction(new ClockFunction());
    addNativeFunction(new AssertFunction());
    addNativeFunction(new SleepFunction());
  }

  private void addNativeFunction(NativeFunction nativeFunction) {
    globals.define(nativeFunction.getName(), nativeFunction);
  }

  String getFullyQualifiedName() {
    return fullyQualifiedName;
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
