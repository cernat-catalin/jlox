package ccs.jlox.backend.ffi;

// XXX: How useful is this?
public interface NativeFunction {
  String getName();

  default String prettyName() {
    return String.format("<native fn %s>", getName());
  }
}
