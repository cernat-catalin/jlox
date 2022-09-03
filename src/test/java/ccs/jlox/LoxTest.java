package ccs.jlox;

import static org.assertj.core.api.Assertions.assertThat;

import ccs.jlox.error.ErrorHandler;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LoxTest {
  private static final Logger LOG = LoggerFactory.getLogger(LoxTest.class);

  @Test
  void runLangTests() throws IOException {
    for (File file : getFilesInDir("tests/lang")) {
      if (file.getName().endsWith("_test.lox")) {
        //        if (!file.getName().equals("import_test.lox")) continue;
        LOG.info("Running tests in file: {}", file.getName());
        runTestFile(file);
      }
    }
  }

  @Test
  void runStdTests() throws IOException {
    for (File file : getFilesInDir("tests/std")) {
      if (file.getName().endsWith("_test.lox")) {
        //            if (!file.getName().equals("array_test.lox")) continue;
        LOG.info("Running tests in file: {}", file.getName());
        runTestFile(file);
      }
    }
  }

  private void runTestFile(File file) throws IOException {
    ErrorHandler errorHandler = Lox.getErrorHandler();

    try {
      Lox.runFile(file.getPath(), false);

      errorHandler
          .getCompileErrors()
          .forEach(error -> LOG.error(ErrorHandler.errorRepresentation(error, file.getName())));
      errorHandler
          .getRuntimeErrors()
          .forEach(error -> LOG.error(ErrorHandler.errorRepresentation(error, file.getName())));

      assertThat(errorHandler.hadCompileError())
          .overridingErrorMessage(() -> String.format("Compile error in file %s", file.getName()))
          .isFalse();
      assertThat(errorHandler.hadRuntimeError())
          .overridingErrorMessage(() -> String.format("Runtime error in file %s", file.getName()))
          .isFalse();
    } finally {
      errorHandler.reset();
    }
  }

  private File[] getFilesInDir(String dirPath) {
    File testDir = new File(dirPath);
    File[] files = testDir.listFiles();
    if (files == null) {
      throw new IllegalStateException("No test files found!");
    }
    return files;
  }
}
