package ccs.jlox.backend.ffi;

public interface NativeFunction {
  String getName();

  default String prettyName() {
    return String.format("<native fn %s>", getName());
  }
}
