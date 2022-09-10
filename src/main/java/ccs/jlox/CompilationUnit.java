package ccs.jlox;

import ccs.jlox.ast.Stmt;
import ccs.jlox.interm.VariableLocation;
import java.util.List;
import java.util.Map;

public record CompilationUnit(List<Stmt> statements, Map<Integer, VariableLocation> locals) {}
