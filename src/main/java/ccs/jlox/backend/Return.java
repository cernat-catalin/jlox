package ccs.jlox.backend;

final class Return extends RuntimeException {
  private final transient Object value;

  public Return(Object value) {
    super(null, null, false, false);
    this.value = value;
  }

  public Object getValue() {
    return value;
  }
}
