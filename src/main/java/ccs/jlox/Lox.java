package ccs.jlox;

import ccs.jlox.backend.Interpreter;
import ccs.jlox.error.ErrorHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Lox {
  private static final ErrorHandler ERROR_HANDLER = new ErrorHandler();

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0], true);
    } else {
      runPrompt();
    }
  }

  static void runFile(String path, boolean handleErrors) throws IOException {
    Path filePath = Paths.get(path);

    Map<String, CompilationUnit> compilationUnits = LoxCompiler.compile(filePath);
    Interpreter interpreter = new Interpreter(compilationUnits);
    interpreter.execute("__main__");

    if (handleErrors) {
      printFileErrors(filePath.getFileName().toString());
      if (ERROR_HANDLER.hadCompileError()) System.exit(65);
      if (ERROR_HANDLER.hadRuntimeError()) System.exit(70);
    }
  }

  static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (; ; ) {
      System.out.print("> ");
      String line = reader.readLine();
      if (line == null) break;
      runSource(line);
      printPromptErrors();
      ERROR_HANDLER.reset();
    }
  }

  public static ErrorHandler getErrorHandler() {
    return ERROR_HANDLER;
  }

  private static void runSource(String source) throws IOException {
    Map<String, CompilationUnit> compilationUnits = LoxCompiler.compile(source);
    if (ERROR_HANDLER.hadCompileError()) return;
    Interpreter interpreter = new Interpreter(compilationUnits);
    interpreter.execute("__main__");
  }

  // XXX: Kinda ugly. Try and find something else.
  private static void printPromptErrors() {
    ERROR_HANDLER
        .getCompileErrors()
        .forEach(error -> System.out.println((ErrorHandler.errorRepresentation(error))));
    ERROR_HANDLER
        .getRuntimeErrors()
        .forEach(error -> System.out.println((ErrorHandler.errorRepresentation(error))));
  }

  private static void printFileErrors(String filename) {
    ERROR_HANDLER
        .getCompileErrors()
        .forEach(error -> System.out.println((ErrorHandler.errorRepresentation(error, filename))));
    ERROR_HANDLER
        .getRuntimeErrors()
        .forEach(error -> System.out.println((ErrorHandler.errorRepresentation(error, filename))));
  }
}
