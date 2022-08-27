package ccs.jlox.error;

import ccs.jlox.ast.Token;
import ccs.jlox.ast.TokenType;
import java.util.ArrayList;
import java.util.List;

public final class ErrorHandler {
  private List<CompileError> compileErrors = new ArrayList<>();
  private List<RuntimeError> runtimeErrors = new ArrayList<>();

  public static String errorRepresentation(CompileError compileError) {
    return String.format(
        "[line %d] Error%s: %s%n",
        compileError.line(), compileError.where(), compileError.message());
  }

  public static String errorRepresentation(CompileError compileError, String filename) {
    return String.format(
        "[file %s] [line %d] Error%s: %s%n",
        filename, compileError.line(), compileError.where(), compileError.message());
  }

  public static String errorRepresentation(RuntimeError runtimeError, String filename) {
    return String.format(
        "[file %s] [line %d] Error: %s%n",
        filename, runtimeError.getToken().line(), runtimeError.getMessage());
  }

  public static String errorRepresentation(RuntimeError runtimeError) {
    return String.format(
        "[line %d] Error: %s%n", runtimeError.getToken().line(), runtimeError.getMessage());
  }

  public void runtimeError(RuntimeError error) {
    runtimeErrors.add(error);
  }

  public void error(Token token, String message) {
    if (token.type() == TokenType.EOF) {
      compileErrors.add(new CompileError(token.line(), " at end", message));
    } else {
      compileErrors.add(new CompileError(token.line(), " at '" + token.lexeme() + "'", message));
    }
  }

  public void error(int line, String message) {
    compileErrors.add(new CompileError(line, "", message));
  }

  public boolean hadCompileError() {
    return !compileErrors.isEmpty();
  }

  public void reset() {
    compileErrors = new ArrayList<>();
    runtimeErrors = new ArrayList<>();
  }

  public boolean hadRuntimeError() {
    return !runtimeErrors.isEmpty();
  }

  public List<CompileError> getCompileErrors() {
    return List.copyOf(compileErrors);
  }

  public List<RuntimeError> getRuntimeErrors() {
    return List.copyOf(runtimeErrors);
  }
}
