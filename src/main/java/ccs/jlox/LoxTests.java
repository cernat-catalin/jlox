package ccs.jlox;

import java.io.IOException;

public class LoxTests {
  public static void main(String[] args) throws IOException {
    String path = "tests/operators.lox";
    Lox.runFile(path);
  }
}
