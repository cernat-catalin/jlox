package ccs.jlox.error;

import ccs.jlox.ast.Token;

public class RuntimeError extends RuntimeException {
  private final Token token;

  public RuntimeError(Token token, String message) {
    super(message);
    this.token = token;
  }

  public Token getToken() {
    return token;
  }
}
