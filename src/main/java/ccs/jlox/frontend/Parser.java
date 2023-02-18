package ccs.jlox.frontend;

import static ccs.jlox.ast.TokenType.AND;
import static ccs.jlox.ast.TokenType.AS;
import static ccs.jlox.ast.TokenType.BACKSLASH;
import static ccs.jlox.ast.TokenType.BANG;
import static ccs.jlox.ast.TokenType.BANG_EQUAL;
import static ccs.jlox.ast.TokenType.BREAK;
import static ccs.jlox.ast.TokenType.CLASS;
import static ccs.jlox.ast.TokenType.COLON;
import static ccs.jlox.ast.TokenType.COMMA;
import static ccs.jlox.ast.TokenType.DEBUG;
import static ccs.jlox.ast.TokenType.DOT;
import static ccs.jlox.ast.TokenType.ELSE;
import static ccs.jlox.ast.TokenType.EOF;
import static ccs.jlox.ast.TokenType.EQUAL;
import static ccs.jlox.ast.TokenType.EQUAL_EQUAL;
import static ccs.jlox.ast.TokenType.FALSE;
import static ccs.jlox.ast.TokenType.FOR;
import static ccs.jlox.ast.TokenType.FUN;
import static ccs.jlox.ast.TokenType.GREATER;
import static ccs.jlox.ast.TokenType.GREATER_EQUAL;
import static ccs.jlox.ast.TokenType.IDENTIFIER;
import static ccs.jlox.ast.TokenType.IF;
import static ccs.jlox.ast.TokenType.IMPORT;
import static ccs.jlox.ast.TokenType.LEFT_BRACE;
import static ccs.jlox.ast.TokenType.LEFT_PAREN;
import static ccs.jlox.ast.TokenType.LEFT_SQUARE_BRACKET;
import static ccs.jlox.ast.TokenType.LESS;
import static ccs.jlox.ast.TokenType.LESS_EQUAL;
import static ccs.jlox.ast.TokenType.MINUS;
import static ccs.jlox.ast.TokenType.MINUS_EQUAL;
import static ccs.jlox.ast.TokenType.NIL;
import static ccs.jlox.ast.TokenType.NUMBER;
import static ccs.jlox.ast.TokenType.OR;
import static ccs.jlox.ast.TokenType.PLUS;
import static ccs.jlox.ast.TokenType.PLUS_EQUAL;
import static ccs.jlox.ast.TokenType.QUESTION_MARK;
import static ccs.jlox.ast.TokenType.RETURN;
import static ccs.jlox.ast.TokenType.RIGHT_BRACE;
import static ccs.jlox.ast.TokenType.RIGHT_PAREN;
import static ccs.jlox.ast.TokenType.RIGHT_SQUARE_BRACKET;
import static ccs.jlox.ast.TokenType.SEMICOLON;
import static ccs.jlox.ast.TokenType.SLASH;
import static ccs.jlox.ast.TokenType.SLASH_EQUAL;
import static ccs.jlox.ast.TokenType.STAR;
import static ccs.jlox.ast.TokenType.STAR_EQUAL;
import static ccs.jlox.ast.TokenType.STRING;
import static ccs.jlox.ast.TokenType.SUPER;
import static ccs.jlox.ast.TokenType.THIS;
import static ccs.jlox.ast.TokenType.TRUE;
import static ccs.jlox.ast.TokenType.VAR;
import static ccs.jlox.ast.TokenType.WHILE;

import ccs.jlox.Lox;
import ccs.jlox.ast.Expr;
import ccs.jlox.ast.Stmt;
import ccs.jlox.ast.Token;
import ccs.jlox.ast.TokenType;
import ccs.jlox.error.ErrorHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public final class Parser {
  private static final ErrorHandler ERROR_HANDLER = Lox.getErrorHandler();

  private final List<Token> tokens;
  private int current = 0;

  public Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  public List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();

    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements;
  }

  private Stmt declaration() {
    try {
      if (match(CLASS)) return classDeclaration();
      if (match(VAR)) return varDeclaration();
      if (match(FUN)) return funDeclaration("function");
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt classDeclaration() {
    Token name = consume(IDENTIFIER, "Expect class name.");

    Expr.Variable superclass = null;
    if (match(LESS)) {
      consume(IDENTIFIER, "Expect superclass name.");
      superclass = new Expr.Variable(previous());
    }

    consume(LEFT_BRACE, "Expect '{' before class body.");
    List<Stmt.Function> methods = new ArrayList<>();
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      methods.add(funDeclaration("method"));
    }
    consume(RIGHT_BRACE, "Expect '}' after class body.");
    return new Stmt.Class(name, superclass, methods);
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");
    Expr initializer = null;

    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  private Stmt.Function funDeclaration(String kind) {
    Token name = consume(IDENTIFIER, "Expect " + kind + " name.");

    consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
    List<Token> parameters = functionParameters();
    consume(RIGHT_PAREN, "Expect ')' after parameters.");

    List<Stmt> body = functionBody(kind);
    return new Stmt.Function(name, new Expr.Function(parameters, body));
  }

  private Stmt statement() {
    if (match(FOR)) return forStatement();
    if (match(IF)) return ifStatement();
    if (match(RETURN)) return returnStatement();
    if (match(WHILE)) return whileStatement();
    if (match(LEFT_BRACE)) return blockStatement();
    if (match(IMPORT)) return importStatement();
    if (match(DEBUG)) return debugStatement();
    if (match(BREAK)) return breakStatement();
    return expressionStatement();
  }

  private Stmt forStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'for'.");

    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }

    Expr condition = null;
    if (!check(SEMICOLON)) {
      condition = expression();
    }
    consume(SEMICOLON, "Expect ';' after loop condition.");

    Expr increment = null;
    if (!check(RIGHT_PAREN)) {
      increment = expression();
    }
    consume(RIGHT_PAREN, "Expect ')' after for clauses.");

    Stmt body = statement();
    if (increment != null) {
      body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
    }

    if (condition == null) condition = new Expr.Literal(true);
    body = new Stmt.While(condition, body);

    if (initializer != null) {
      body = new Stmt.Block(Arrays.asList(initializer, body));
    }

    return body;
  }

  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition.");
    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(ELSE)) {
      elseBranch = statement();
    }
    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = null;
    if (!check(SEMICOLON)) {
      value = expression();
    }
    consume(SEMICOLON, "Expect ';' after return value.");
    return new Stmt.Return(keyword, value);
  }

  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after condition.");
    Stmt body = statement();
    return new Stmt.While(condition, body);
  }

  private Stmt blockStatement() {
    List<Stmt> statements = new ArrayList<>();
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }
    consume(RIGHT_BRACE, "Expect '}' after block.");
    return new Stmt.Block(statements);
  }

  private Stmt importStatement() {
    // XXX: Change to do-while ?
    Token subPath = consume(IDENTIFIER, "Expected identifier.");
    List<Token> path = new ArrayList<>();
    path.add(subPath);

    while (match(DOT)) {
      path.add(consume(IDENTIFIER, "Expected identifier."));
    }

    consume(AS, "Expected 'as' qualifier.");
    Token moduleName = consume(IDENTIFIER, "Expected module qualifier.");

    consume(SEMICOLON, "Expect ';' after import statement.");

    return new Stmt.Import(path, moduleName);
  }

  private Stmt debugStatement() {
    Stmt.Debug debug = new Stmt.Debug(previous().line());
    consume(SEMICOLON, "Expect ';' after value.");
    return debug;
  }

  private Stmt breakStatement() {
    Stmt.Break breakStmt = new Stmt.Break(previous().line());
    consume(SEMICOLON, "Expect ';' after break statement.");
    return breakStmt;
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Expression(expr);
  }

  private Expr expression() {
    return assignment();
  }

  private Expr assignment() {
    Expr expr = assignmentAndOperation();

    // XXX: simplify
    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();
      if (expr instanceof Expr.Variable variable) {
        return new Expr.Assignment(variable, equals, value);
      } else if (expr instanceof Expr.Get get) {
        return new Expr.Assignment(get, equals, value);
      } else if (expr instanceof Expr.ArrayIndex arrayIndex) {
        return new Expr.Assignment(arrayIndex, equals, value);
      }
      error(equals, "Invalid assignment target.");
    }

    return expr;
  }

  private Expr assignmentAndOperation() {
    Expr expr = ternary();

    while (match(PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL)) {
      // XXX: Maybe we should have a binary operation type instead of relying on the token
      Token operator = previous();
      TokenType singleType =
          switch (operator.type()) {
            case PLUS_EQUAL -> PLUS;
            case MINUS_EQUAL -> MINUS;
            case STAR_EQUAL -> STAR;
            case SLASH_EQUAL -> SLASH;
            default -> throw error(peek(), "Invalid expression.");
          };
      Token singleOperator =
          new Token(singleType, operator.lexeme().substring(0, 1), null, operator.line());

      Expr right = ternary();
      Expr newValue = new Expr.Binary(expr, singleOperator, right);
      expr = new Expr.Assignment(expr, operator, newValue);
    }
    return expr;
  }

  private Expr ternary() {
    Expr expr = or();
    if (match(QUESTION_MARK)) {
      Expr left = ternary();
      Token colon = consume(COLON, "Expected ':' in ternary expression");
      Expr right = ternary();
      return new Expr.Ternary(expr, left, colon, right);
    }
    return expr;
  }

  private Expr or() {
    Expr expr = and();
    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
  }

  private Expr and() {
    Expr expr = equality();
    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
  }

  private Expr equality() {
    Expr expr = comparison();
    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr comparison() {
    Expr expr = term();
    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr term() {
    Expr expr = factor();
    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr factor() {
    Expr expr = unary();
    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }
    return call();
  }

  private Expr call() {
    Expr expr = primary();
    while (true) {
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else if (match(DOT)) {
        Token name = consume(IDENTIFIER, "Expect property name after '.'.");
        expr = new Expr.Get(expr, name);
      } else if (match(LEFT_SQUARE_BRACKET)) {
        Expr index = expression();
        Token rightParen = consume(RIGHT_SQUARE_BRACKET, "Expected ']' in array indexing.");
        expr = new Expr.ArrayIndex(expr, rightParen, index);
      } else {
        break;
      }
    }
    return expr;
  }

  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }
        arguments.add(expression());
      } while (match(COMMA));
    }
    Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
    return new Expr.Call(callee, paren, arguments);
  }

  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);
    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal());
    }
    if (match(THIS)) return new Expr.This(previous());
    if (match(SUPER)) {
      Token keyword = previous();
      consume(DOT, "Expect '.' after 'super'.");
      Token method = consume(IDENTIFIER, "Expect superclass method name.");
      return new Expr.Super(keyword, method);
    }
    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }
    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }
    if (match(LEFT_SQUARE_BRACKET)) {
      Expr size = expression();
      Token rightBracket =
          consume(RIGHT_SQUARE_BRACKET, "Expect closing square bracket in array creation.");
      consume(LEFT_BRACE, "Expect opening brace in array creation.");
      consume(RIGHT_BRACE, "Expect closing brace in array creation.");
      return new Expr.ArrayCreation(size, rightBracket);
    }
    if (match(BACKSLASH)) return lambda();

    throw error(peek(), "Expect expression.");
  }

  private Expr lambda() {
    // XXX: Is this error message still correct for anonymous functions?
    Token arrow = previous();
    List<Token> parameters = functionParameters();

    if (check(LEFT_BRACE)) {
      List<Stmt> body = functionBody("anonymous_fn");
      return new Expr.Function(parameters, body);
    } else {
      Expr expr = expression();
      return new Expr.Function(parameters, List.of(new Stmt.Return(arrow, expr)));
    }
  }

  private List<Token> functionParameters() {
    List<Token> parameters = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 parameters.");
        }
        parameters.add(consume(IDENTIFIER, "Expect parameter name."));
      } while (match(COMMA));
    }
    return parameters;
  }

  private List<Stmt> functionBody(String kind) {
    consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
    List<Stmt> body = new ArrayList<>();
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      body.add(declaration());
    }
    consume(RIGHT_BRACE, "Expect '}' after block.");
    return body;
  }

  // Helper functions

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type() == type;
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type() == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  // Error handling

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
    throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    ERROR_HANDLER.error(token, message);
    return new ParseError();
  }

  // Move to the next statement
  private void synchronize() {
    advance();
    while (!isAtEnd()) {
      if (previous().type() == SEMICOLON) return;
      if (EnumSet.of(CLASS, FUN, VAR, FOR, IF, WHILE, RETURN).contains(peek().type())) return;
      advance();
    }
  }
}
