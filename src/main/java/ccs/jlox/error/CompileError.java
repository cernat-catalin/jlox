package ccs.jlox.error;

public record CompileError(int line, String where, String message) {
}
