package ccs.jlox;

import ccs.jlox.ast.Stmt;
import ccs.jlox.ast.Token;
import ccs.jlox.error.ErrorHandler;
import ccs.jlox.frontend.Parser;
import ccs.jlox.frontend.Scanner;
import ccs.jlox.interm.Resolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

public final class LoxCompiler {
  private static final ErrorHandler ERROR_HANDLER = Lox.getErrorHandler();

  public static Map<String, CompilationUnit> compile(Path path) throws IOException {
    Path projectRoot = path.getParent();
    String mainSource = Files.readString(path);
    return compile(mainSource, fqn -> loadProjectSource(projectRoot, fqn));
  }

  public static Map<String, CompilationUnit> compile(String mainSource) throws IOException {
    return compile(mainSource, LoxCompiler::loadStdSource);
  }

  private static Map<String, CompilationUnit> compile(String mainSource, SourceLoader sourceLoader)
      throws IOException {
    SingleFileOutput mainOutput = compileUnit(mainSource);
    Map<String, CompilationUnit> compiledUnits = new HashMap<>();
    compiledUnits.put("__main__", new CompilationUnit(mainOutput.stmts(), mainOutput.locals()));
    Queue<String> unitsToCompile = new LinkedList<>(mainOutput.imports());

    while (!unitsToCompile.isEmpty()) {
      String fullyQualifiedName = unitsToCompile.poll();
      if (!compiledUnits.containsKey(fullyQualifiedName)) {
        String source = sourceLoader.loadSource(fullyQualifiedName);
        SingleFileOutput output = compileUnit(source);
        compiledUnits.put(fullyQualifiedName, new CompilationUnit(output.stmts(), output.locals()));
        unitsToCompile.addAll(output.imports());
      }
    }

    return compiledUnits;
  }

  private static SingleFileOutput compileUnit(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    Parser parser = new Parser(tokens);
    List<Stmt> stmts = parser.parse();

    if (ERROR_HANDLER.hadCompileError()) return new SingleFileOutput(stmts, null, null);

    Resolver resolver = new Resolver();
    Resolver.ResolverContext resolverContext = resolver.resolve(stmts);
    return new SingleFileOutput(stmts, resolverContext.locals(), resolverContext.imports());
  }

  private static String loadProjectSource(Path projectRoot, String fullyQualifiedName)
      throws IOException {
    String packageName = parsePackageName(fullyQualifiedName);
    String unitName = parseUnitName(fullyQualifiedName);
    Path path = Paths.get(projectRoot.toString(), packageName.replace(".", "/"), unitName + ".lox");
    if (Files.exists(path)) {
      return Files.readString(path);
    } else {
      return loadStdSource(fullyQualifiedName);
    }
  }

  private static String loadStdSource(String fullyQualifiedName) throws IOException {
    String unitName = parseUnitName(fullyQualifiedName);
    return Files.readString(Path.of("std", unitName + ".lox"));
  }

  private static String parseUnitName(String fullyQualifiedName) {
    String[] tokens = fullyQualifiedName.split("\\.");
    return tokens[tokens.length - 1];
  }

  private static String parsePackageName(String fullyQualifiedName) {
    String[] tokens = fullyQualifiedName.split("\\.");
    return Arrays.stream(tokens).limit(tokens.length - 1L).collect(Collectors.joining("."));
  }

  @FunctionalInterface
  private interface SourceLoader {
    String loadSource(String fullyQualifiedName) throws IOException;
  }

  record SingleFileOutput(List<Stmt> stmts, Map<Integer, Integer> locals, List<String> imports) {}
}
