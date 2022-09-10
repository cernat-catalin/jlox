package ccs.jlox.backend;

import ccs.jlox.backend.ffi.AssertFunction;
import ccs.jlox.backend.ffi.ClockFunction;
import ccs.jlox.backend.ffi.NativeFunction;
import ccs.jlox.backend.ffi.PrintFunction;
import ccs.jlox.backend.ffi.SleepFunction;
import ccs.jlox.interm.VariableLocation;
import java.util.HashMap;
import java.util.Map;

// XXX: Should locals live here?
public final class LoxModule {
  private final String fullyQualifiedName;
  private final Map<Integer, VariableLocation> locals;
  // XXX: Change to more efficient implementation. See notes
  private final Map<String, Object> globals = new HashMap<>();

  LoxModule(String fullyQualifiedName, Map<Integer, VariableLocation> locals) {
    this.fullyQualifiedName = fullyQualifiedName;
    this.locals = locals;

    // XXX; Find a way to only define them once
    addNativeFunction(new PrintFunction());
    addNativeFunction(new ClockFunction());
    addNativeFunction(new AssertFunction());
    addNativeFunction(new SleepFunction());
  }

  private void addNativeFunction(NativeFunction nativeFunction) {
    globals.put(nativeFunction.getName(), nativeFunction);
  }

  String getFullyQualifiedName() {
    return fullyQualifiedName;
  }

  Map<String, Object> getGlobals() {
    return globals;
  }

  Map<Integer, VariableLocation> getLocals() {
    return locals;
  }
}
