package ccs.jlox.backend;

import ccs.jlox.CompilationUnit;
import ccs.jlox.Lox;
import ccs.jlox.ast.Expr;
import ccs.jlox.ast.Stmt;
import ccs.jlox.ast.Token;
import ccs.jlox.ast.TokenType;
import ccs.jlox.error.ErrorHandler;
import ccs.jlox.error.RuntimeError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Interpreter {
  private static final ErrorHandler ERROR_HANDLER = Lox.getErrorHandler();

  private final Map<String, CompilationUnit> compilationUnits;
  private final Map<String, LoxModule> modules = new HashMap<>();
  private String currentNamespace = "__main__";

  public Interpreter(Map<String, CompilationUnit> compilationUnits) {
    this.compilationUnits = compilationUnits;
    CompilationUnit mainCompilationUnit = compilationUnits.get("__main__");
    LoxModule mainModule =
        new LoxModule("__main__", mainCompilationUnit.locals());
    modules.put("__main__", mainModule);
  }

  public void execute(String namespace) {
    setCurrentNamespace(namespace);
    try {
      for (Stmt stmt : compilationUnits.get(currentNamespace).statements()) {
        execute(stmt);
      }
    } catch (RuntimeError error) {
      ERROR_HANDLER.runtimeError(error);
    }
  }

  private void execute(Stmt stmt) {
    switch (stmt) {
      case Stmt.If ifStmt -> executeIfStmt(ifStmt);
      case Stmt.Return returnStmt -> executeReturnStmt(returnStmt);
      case Stmt.While whileStmt -> executeWhileStmt(whileStmt);
      case Stmt.Expression exprStmt -> executeExprStmt(exprStmt);
      case Stmt.Var varStmt -> executeVarStmt(varStmt);
      case Stmt.Function functionStmt -> executeFunctionStmt(functionStmt);
      case Stmt.Class classStmt -> executeClassStmt(classStmt);
      case Stmt.Block blockStmt -> executeBlockStmt(blockStmt);
      case Stmt.Import importStmt -> executeImportStmt(importStmt);
      case Stmt.Debug debugStmt -> executeDebugStmt(debugStmt);
    }
  }

  private void executeIfStmt(Stmt.If ifStmt) {
    if (isTruthy(evaluate(ifStmt.condition()))) {
      execute(ifStmt.thenBranch());
    } else if (ifStmt.elseBranch() != null) {
      execute(ifStmt.elseBranch());
    }
  }

  private void executeReturnStmt(Stmt.Return returnStmt) {
    Object value = null;
    if (returnStmt.value() != null) value = evaluate(returnStmt.value());
    throw new Return(value);
  }

  private void executeWhileStmt(Stmt.While whileStmt) {
    while (isTruthy(evaluate(whileStmt.condition()))) {
      execute(whileStmt.body());
    }
  }

  private void executeExprStmt(Stmt.Expression exprStmt) {
    evaluate(exprStmt.expr());
  }

  private void executeVarStmt(Stmt.Var varStmt) {
    Object value = null;
    if (varStmt.initializer() != null) {
      value = evaluate(varStmt.initializer());
    }
    define(varStmt.name().lexeme(), value);
  }

  private void executeFunctionStmt(Stmt.Function functionStmt) {
    LoxFunction function =
        new LoxFunction(currentNamespace, functionStmt, getEnvironment(), false);
    define(functionStmt.name().lexeme(), function);
  }

  private void executeClassStmt(Stmt.Class classStmt) {
    Object superclass = null;
    if (classStmt.superclass() != null) {
      superclass = evaluate(classStmt.superclass());
      if (!(superclass instanceof LoxClass)) {
        throw new RuntimeError(classStmt.superclass().name().line(), "Superclass must be a class.");
      }
    }
    // XXX: Why two step process (first define class then assign it)?
    define(classStmt.name().lexeme(), null);

    if (classStmt.superclass() != null) {
      setEnvironment(new Environment(getEnvironment()));
      define("super", superclass);
    }

    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : classStmt.methods()) {
      LoxFunction function =
          new LoxFunction(
              currentNamespace,
              method,
              getEnvironment(),
              method.name().lexeme().equals("init"));
      methods.put(method.name().lexeme(), function);
    }

    LoxClass klass = new LoxClass(classStmt.name().lexeme(), (LoxClass) superclass, methods);

    if (superclass != null) {
      setEnvironment(getEnvironment().ancestor(1));
    }

    assignAt(0, classStmt.name().lexeme(), klass, classStmt.name().line());
  }

  private void executeBlockStmt(Stmt.Block blockStmt) {
    executeBlock(blockStmt.statements(), new Environment(getEnvironment()));
  }

  // XXX: Keep like this?
  public void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = getEnvironment();
    try {
      setEnvironment(environment);
      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      setEnvironment(previous);
    }
  }

  // XXX; Very hacky; find a better way
  private void executeImportStmt(Stmt.Import importStmt) {
    String fullyQualifiedName =
        importStmt.path().stream().map(Token::lexeme).collect(Collectors.joining("."));
    String qualifier = importStmt.name().lexeme();

    // Check if we have already executed the lox module
    if (modules.containsKey(fullyQualifiedName)) {
      LoxModule loxModule = modules.get(fullyQualifiedName);
      define(qualifier, loxModule);
      return;
    }

    CompilationUnit compilationUnit = compilationUnits.get(fullyQualifiedName);
    LoxModule loxModule =
        new LoxModule(fullyQualifiedName, compilationUnit.locals());
    modules.put(fullyQualifiedName, loxModule);
    define(qualifier, loxModule);

    String previousNamespace = currentNamespace;
    currentNamespace = fullyQualifiedName;
    execute(fullyQualifiedName);
    currentNamespace = previousNamespace;
    if (ERROR_HANDLER.hadCompileError()) {
      throw new RuntimeError(
          importStmt.path().get(0).line(),
          String.format("Could not parse module %s.", fullyQualifiedName));
    }
  }

  private void executeDebugStmt(Stmt.Debug debugStmt) {
    System.out.printf("[DEBUG] Line %d%n", debugStmt.line());
  }

  private Object evaluate(Expr expr) {
    return switch (expr) {
      case Expr.Literal lit -> evaluateLiteralExpr(lit);
      case Expr.Logical log -> evaluateLogicalExpr(log);
      case Expr.Variable variable -> evaluateVariableExpr(variable);
      case Expr.Assignment assignment -> evaluateAssignmentExpr(assignment);
      case Expr.Unary unary -> evaluateUnaryExpr(unary);
      case Expr.Binary binary -> evaluateBinaryExpr(binary);
      case Expr.Grouping group -> evaluateGroupingExpr(group);
      case Expr.Call call -> evaluateCallExpr(call);
      case Expr.Get get -> evaluateGetExpr(get);
      case Expr.This thisExpr -> evaluateThisExpr(thisExpr);
      case Expr.Super superExpr -> evaluateSuperExpr(superExpr);
      case Expr.ArrayCreation arrayCExpr -> evaluateArrayCreationExpr(arrayCExpr);
      case Expr.ArrayIndex arrayIndexExpr -> evaluateArrayIndexExpr(arrayIndexExpr);
    };
  }

  private Object evaluateLiteralExpr(Expr.Literal literalExpr) {
    return literalExpr.value();
  }

  private Object evaluateLogicalExpr(Expr.Logical logExpr) {
    Object left = evaluate(logExpr.left());
    if (logExpr.operator().type() == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }
    return evaluate(logExpr.right());
  }

  private Object evaluateVariableExpr(Expr.Variable variableExpr) {
    return lookUpVariable(variableExpr.name(), variableExpr);
  }

  private Object lookUpVariable(Token name, Expr expr) {
    int key = System.identityHashCode(expr);
    Integer distance = getLocals().get(key);
    if (distance != null) {
      return getAt(distance, name.lexeme(), name.line());
    } else {
      return getGlobal(name.lexeme(), name.line());
    }
  }

  // XXX: make pretty and make better error handling with all the nested instanceof
  private Object evaluateAssignmentExpr(Expr.Assignment assignmentExpr) {
    Object value = evaluate(assignmentExpr.value());

    if (assignmentExpr.variable() instanceof Expr.Variable variable) {
      int key = System.identityHashCode(assignmentExpr);

      Integer distance = getLocals().get(key);
      if (distance != null) {
        assignAt(distance, variable.name().lexeme(), value, variable.name().line());
      } else {
        assignGlobal(variable.name().lexeme(), value, variable.name().line());
      }
      return value;
    } else if (assignmentExpr.variable() instanceof Expr.Get get) {
      Object object = evaluate(get.object());
      if (object instanceof LoxInstance loxInstance) {
        loxInstance.set(get.name(), value);
        return value;
      }
    } else if (assignmentExpr.variable() instanceof Expr.ArrayIndex indexExpr) {
      Object object = evaluate(indexExpr.array());
      if (object instanceof LoxArray loxArray) {
        Object index = evaluate(indexExpr.idx());

        if (index instanceof Double doubleIndex) {
          loxArray.set(doubleIndex.intValue(), value);
          return value;
        }
      }
    }

    // XXX: Change name of error?
    throw new RuntimeError(
        assignmentExpr.equals().line(), "Invalid left value to assignment operator.");
  }

  private Object evaluateUnaryExpr(Expr.Unary unaryExpr) {
    Object right = evaluate(unaryExpr.right());

    return switch (unaryExpr.operator().type()) {
      case MINUS -> {
        checkNumberOperand(unaryExpr.operator(), right);
        yield -(double) right;
      }
      case BANG -> !isTruthy(right);
      default -> throw new IllegalStateException();
    };
  }

  private Object evaluateBinaryExpr(Expr.Binary binaryExpr) {
    Object left = evaluate(binaryExpr.left());
    Object right = evaluate(binaryExpr.right());

    return switch (binaryExpr.operator().type()) {
      case GREATER -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left > (double) right;
      }
      case GREATER_EQUAL -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left >= (double) right;
      }
      case LESS -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left < (double) right;
      }
      case LESS_EQUAL -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left <= (double) right;
      }
      case BANG_EQUAL -> !isEqual(left, right);
      case EQUAL_EQUAL -> isEqual(left, right);
      case MINUS -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left - (double) right;
      }
      case SLASH -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left / (double) right;
      }
      case STAR -> {
        checkNumberOperands(binaryExpr.operator(), left, right);
        yield (double) left * (double) right;
      }
      case PLUS -> {
        if (left instanceof Double dLeft && right instanceof Double dRight) {
          yield dLeft + dRight;
        }
        if (left instanceof String sLeft && right instanceof String sRight) {
          yield sLeft + sRight;
        }
        throw new RuntimeError(
            binaryExpr.operator().line(), "Operands must be two numbers or two strings.");
      }
      default -> new IllegalStateException();
    };
  }

  private Object evaluateGroupingExpr(Expr.Grouping groupExpr) {
    return evaluate(groupExpr.expr());
  }

  private Object evaluateCallExpr(Expr.Call callExpr) {
    Object callee = evaluate(callExpr.callee());
    List<Object> arguments = new ArrayList<>();
    for (Expr argument : callExpr.arguments()) {
      arguments.add(evaluate(argument));
    }

    if (!(callee instanceof LoxCallable function)) {
      throw new RuntimeError(callExpr.paren().line(), "Can only call functions and classes.");
    }

    if (arguments.size() != function.arity()) {
      throw new RuntimeError(
          callExpr.paren().line(),
          "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
    }

    return function.call(this, callExpr.paren(), arguments);
  }

  private Object evaluateGetExpr(Expr.Get getExpr) {
    Object object = evaluate(getExpr.object());
    if (object instanceof LoxInstance loxInstance) {
      return loxInstance.get(getExpr.name());
    }
    if (object instanceof LoxModule loxModule) {
      String previousNamespace = currentNamespace;
      currentNamespace = loxModule.getFullyQualifiedName();
      // XXX: This might work for globals (as there is a fallback there when the key is not in the
      // locals)
      Object value = evaluateVariableExpr(new Expr.Variable(getExpr.name()));
      currentNamespace = previousNamespace;
      return value;
    }
    throw new RuntimeError(getExpr.name().line(), "Only instances or modules have properties.");
  }

  private Object evaluateThisExpr(Expr.This thisExpr) {
    return lookUpVariable(thisExpr.keyword(), thisExpr);
  }

  private Object evaluateSuperExpr(Expr.Super superExpr) {
    // XXX: Ugly hack fix this
    int key = System.identityHashCode(superExpr);
    int distance = getLocals().get(key);
    LoxClass superClass = (LoxClass) getAt(distance, "super", superExpr.keyword().line());
    LoxInstance object = (LoxInstance) getAt(distance - 1, "this", -1);
    LoxFunction method = superClass.findMethod(superExpr.method().lexeme());

    if (method == null) {
      throw new RuntimeError(
          superExpr.method().line(), "Undefined property '" + superExpr.method().lexeme() + "'.");
    }

    return method.bind(object);
  }

  private Object evaluateArrayCreationExpr(Expr.ArrayCreation arrayCExpr) {
    Object size = evaluate(arrayCExpr.size());

    if (size instanceof Double doubleSize) {
      return new LoxArray(doubleSize.intValue());
    }

    throw new RuntimeError(arrayCExpr.rightBracket().line(), "Array size must be a number.");
  }

  private Object evaluateArrayIndexExpr(Expr.ArrayIndex arrayIndexExpr) {
    Object object = evaluate(arrayIndexExpr.array());

    if (object instanceof LoxArray loxArray) {
      Object index = evaluate(arrayIndexExpr.idx());
      if (index instanceof Double doubleIndex) {
        return loxArray.get(doubleIndex.intValue());
      }
      throw new RuntimeError(arrayIndexExpr.rightParen().line(), "Cannot index non array object.");
    }

    throw new RuntimeError(arrayIndexExpr.rightParen().line(), "Cannot index non array object.");
  }

  private LoxModule getCurrentModule() {
    return modules.get(currentNamespace);
  }

  private Environment getEnvironment() {
    return getCurrentModule().getEnvironment();
  }

  private void setEnvironment(Environment environment) {
    getCurrentModule().setEnvironment(environment);
  }

  private Map<Integer, Integer> getLocals() {
    return getCurrentModule().getLocals();
  }

  String getCurrentNamespace() {
    return currentNamespace;
  }

  void setCurrentNamespace(String namespace) {
    this.currentNamespace = namespace;
  }

  private void define(String name, Object value) {
    getEnvironment().define(name, value);
  }

  private void assignAt(int distance, String name, Object value, int line) {
    getEnvironment().assignAt(distance, name, value, line);
  }

  private Object getAt(int distance, String name, int line) {
    return getEnvironment().getAt(distance, name, line);
  }

  private void assignGlobal(String name, Object value, int line) {
    getCurrentModule().getGlobals().assignAt(0, name, value, line);
  }

  private Object getGlobal(String name, int line) {
    return getCurrentModule().getGlobals().getAt(0, name, line);
  }

  private static boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;
    return a.equals(b);
  }

  private static boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean) object;
    return true;
  }

  private static void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator.line(), "Operand must be a number.");
  }

  private static void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator.line(), "Operands must be numbers.");
  }
}
