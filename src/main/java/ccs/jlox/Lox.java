package ccs.jlox;

import ccs.jlox.ast.Stmt;
import ccs.jlox.ast.Token;
import ccs.jlox.backend.Interpreter;
import ccs.jlox.error.ErrorHandler;
import ccs.jlox.frontend.Parser;
import ccs.jlox.frontend.Scanner;
import ccs.jlox.interm.Resolver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
  private static final ErrorHandler ERROR_HANDLER = new ErrorHandler();

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  static void runFile(String path) throws IOException {
    Path filePath = Paths.get(path);
    runSource(Files.readString(filePath));
    printFileErrors(filePath.getFileName().toString());
    if (ERROR_HANDLER.hadCompileError()) System.exit(65);
    if (ERROR_HANDLER.hadRuntimeError()) System.exit(70);
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

  static void runSource(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    Interpreter interpreter = new Interpreter();
    Parser parser = new Parser(tokens);
    List<Stmt> stmts = parser.parse();

    if (ERROR_HANDLER.hadCompileError()) return;

    Resolver resolver = new Resolver(interpreter);
    resolver.resolve(stmts);
    if (ERROR_HANDLER.hadCompileError()) return;

    interpreter.execute(stmts);
  }

  public static ErrorHandler getErrorHandler() {
    return ERROR_HANDLER;
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
