package ccs.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
  static boolean HAD_ERROR = false;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [scirpt]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  static void error(int line, String message) {
    report(line, "", message);
  }

  private static void runFile(String path) throws IOException {
    run(Files.readString(Paths.get(path)));
    if (HAD_ERROR) System.exit(65);
  }

  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (; ; ) {
      System.out.println("> ");
      String line = reader.readLine();
      if (line == null) break;
      run(line);
      HAD_ERROR = false;
    }
  }

  private static void run(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    for (Token token : tokens) {
      System.out.println(token);
    }
  }

  private static void report(int line, String where, String message) {
    System.err.printf("[line %d] Error%s: %s\n", line, where, message);
    HAD_ERROR = true;
  }
}
