package ccs.jlox.backend;

import ccs.jlox.Lox;
import ccs.jlox.ast.Expr;
import ccs.jlox.ast.Stmt;
import ccs.jlox.ast.Token;
import ccs.jlox.ast.TokenType;
import ccs.jlox.error.ErrorHandler;
import ccs.jlox.error.RuntimeError;
import ccs.jlox.frontend.Parser;
import ccs.jlox.frontend.Scanner;
import ccs.jlox.interm.Resolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Interpreter {
  private static final ErrorHandler ERROR_HANDLER = Lox.getErrorHandler();

  private final LoxModule entrypoint = new LoxModule("", List.of());
  private LoxModule currentLoxModule = entrypoint;

  public Interpreter() {}

  public void execute(List<Stmt> stmts) {
    try {
      for (Stmt stmt : stmts) {
        execute(stmt);
      }
    } catch (RuntimeError error) {
      ERROR_HANDLER.runtimeError(error);
    }
  }

  public void execute(Stmt stmt) {
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
    getEnvironment().define(varStmt.name().lexeme(), value);
  }

  private void executeFunctionStmt(Stmt.Function functionStmt) {
    LoxFunction function = new LoxFunction(functionStmt, getEnvironment(), false);
    getEnvironment().define(functionStmt.name().lexeme(), function);
  }

  private void executeClassStmt(Stmt.Class classStmt) {
    Object superclass = null;
    if (classStmt.superclass() != null) {
      superclass = evaluate(classStmt.superclass());
      if (!(superclass instanceof LoxClass)) {
        throw new RuntimeError(classStmt.superclass().name(), "Superclass must be a class.");
      }
    }
    // XXX: Why two step process (first define class then assign it)?
    getEnvironment().define(classStmt.name().lexeme(), null);

    if (classStmt.superclass() != null) {
      setEnvironment(new Environment(getEnvironment()));
      getEnvironment().define("super", superclass);
    }

    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : classStmt.methods()) {
      LoxFunction function =
          new LoxFunction(method, getEnvironment(), method.name().lexeme().equals("init"));
      methods.put(method.name().lexeme(), function);
    }

    LoxClass klass = new LoxClass(classStmt.name().lexeme(), (LoxClass) superclass, methods);

    if (superclass != null) {
      setEnvironment(getEnvironment().ancestor(1));
    }

    getEnvironment().assign(classStmt.name(), klass);
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
    String path = importStmt.path().stream().map(Token::lexeme).collect(Collectors.joining("."));
    // XXX: Add support for non-std modules
    try {
      String source = Files.readString(Path.of("std", path + ".lox"));
      LoxModule loxModule = loadModule(importStmt.name().lexeme(), importStmt.path(), source);
      if (loxModule == null) {
        throw new RuntimeError(importStmt.path().get(0), "Could not parse module.");
      }
      getEnvironment().define(loxModule.getName(), loxModule);
    } catch (IOException e) {
      throw new RuntimeError(importStmt.path().get(0), "Could not load module.");
    }
  }

  private LoxModule loadModule(String moduleName, List<Token> modulePath, String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    Parser parser = new Parser(tokens);
    List<Stmt> stmts = parser.parse();

    if (ERROR_HANDLER.hadCompileError()) return null;

    // XXX: Is this correct?
    Resolver resolver = new Resolver(this);
    resolver.resolve(stmts);
    if (ERROR_HANDLER.hadCompileError()) return null;

    LoxModule newLoxModule = new LoxModule(moduleName, modulePath);

    LoxModule previousModule = currentLoxModule;
    currentLoxModule = newLoxModule;
    execute(stmts);
    currentLoxModule = previousModule;

    return newLoxModule;
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
      case Expr.Set set -> evaluateSetExpr(set);
      case Expr.This thisExpr -> evaluateThisExpr(thisExpr);
      case Expr.Super superExpr -> evaluateSuperExpr(superExpr);
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
      return getEnvironment().getAt(distance, name.lexeme());
    } else {
      return getGlobals().get(name);
    }
  }

  private Object evaluateAssignmentExpr(Expr.Assignment assignmentExpr) {
    Object value = evaluate(assignmentExpr.value());
    int key = System.identityHashCode(assignmentExpr);
    Integer distance = getLocals().get(key);
    if (distance != null) {
      getEnvironment().assignAt(distance, assignmentExpr.name(), value);
    } else {
      getGlobals().assign(assignmentExpr.name(), value);
    }
    return value;
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
            binaryExpr.operator(), "Operands must be two numbers or two strings.");
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
      throw new RuntimeError(callExpr.paren(), "Can only call functions and classes.");
    }

    if (arguments.size() != function.arity()) {
      throw new RuntimeError(
          callExpr.paren(),
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
      LoxModule previousModule = currentLoxModule;
      currentLoxModule = loxModule;
      // XXX: This might work for globals (as there is a fallback there when the key is not in the
      // locals)
      Object value = evaluateVariableExpr(new Expr.Variable(getExpr.name()));
      currentLoxModule = previousModule;
      return value;
    }
    throw new RuntimeError(getExpr.name(), "Only instances or modules have properties.");
  }

  private Object evaluateSetExpr(Expr.Set setExpr) {
    Object object = evaluate(setExpr.object());
    if (!(object instanceof LoxInstance loxInstance)) {
      throw new RuntimeError(setExpr.name(), "Only instances have fields.");
    }
    Object value = evaluate(setExpr.value());
    loxInstance.set(setExpr.name(), value);
    return value;
  }

  private Object evaluateThisExpr(Expr.This thisExpr) {
    return lookUpVariable(thisExpr.keyword(), thisExpr);
  }

  private Object evaluateSuperExpr(Expr.Super superExpr) {
    int key = System.identityHashCode(superExpr);
    int distance = getLocals().get(key);
    LoxClass superClass = (LoxClass) getEnvironment().getAt(distance, "super");
    LoxInstance object = (LoxInstance) getEnvironment().getAt(distance - 1, "this");
    LoxFunction method = superClass.findMethod(superExpr.method().lexeme());

    if (method == null) {
      throw new RuntimeError(
          superExpr.method(), "Undefined property '" + superExpr.method().lexeme() + "'.");
    }

    return method.bind(object);
  }

  // XXX: Ugly hack. Fix this!
  public void resolve(Expr expr, int depth) {
    int key = System.identityHashCode(expr);
    getLocals().put(key, depth);
  }

  private Environment getGlobals() {
    return currentLoxModule.getGlobals();
  }

  private Environment getEnvironment() {
    return currentLoxModule.getEnvironment();
  }

  private void setEnvironment(Environment environment) {
    currentLoxModule.setEnvironment(environment);
  }

  private Map<Integer, Integer> getLocals() {
    return currentLoxModule.getLocals();
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
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private static void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be numbers.");
  }
}
