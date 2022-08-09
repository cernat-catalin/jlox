package ccs.jlox;

import java.io.File;
import java.io.IOException;

public class LoxTests {
  public static void main(String[] args) throws IOException {
    File testDir = new File("tests");
    for (File file : testDir.listFiles()) {
      Lox.runFile(file.getPath());
    }
  }
}
