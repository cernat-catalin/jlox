package ccs.jlox.error;

public class RuntimeError extends RuntimeException {
  private final int line;

  public RuntimeError(int line, String message) {
    super(message);
    this.line = line;
  }

  public int getLine() {
    return line;
  }
}
