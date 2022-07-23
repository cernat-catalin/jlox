package ccs.jlox;

import static ccs.jlox.TokenType.BANG;
import static ccs.jlox.TokenType.BANG_EQUAL;
import static ccs.jlox.TokenType.CLASS;
import static ccs.jlox.TokenType.EOF;
import static ccs.jlox.TokenType.EQUAL;
import static ccs.jlox.TokenType.EQUAL_EQUAL;
import static ccs.jlox.TokenType.FALSE;
import static ccs.jlox.TokenType.FOR;
import static ccs.jlox.TokenType.FUN;
import static ccs.jlox.TokenType.GREATER;
import static ccs.jlox.TokenType.GREATER_EQUAL;
import static ccs.jlox.TokenType.IDENTIFIER;
import static ccs.jlox.TokenType.IF;
import static ccs.jlox.TokenType.LEFT_PAREN;
import static ccs.jlox.TokenType.LESS;
import static ccs.jlox.TokenType.LESS_EQUAL;
import static ccs.jlox.TokenType.MINUS;
import static ccs.jlox.TokenType.NIL;
import static ccs.jlox.TokenType.NUMBER;
import static ccs.jlox.TokenType.PLUS;
import static ccs.jlox.TokenType.PRINT;
import static ccs.jlox.TokenType.RETURN;
import static ccs.jlox.TokenType.RIGHT_PAREN;
import static ccs.jlox.TokenType.SEMICOLON;
import static ccs.jlox.TokenType.SLASH;
import static ccs.jlox.TokenType.STAR;
import static ccs.jlox.TokenType.STRING;
import static ccs.jlox.TokenType.TRUE;
import static ccs.jlox.TokenType.VAR;
import static ccs.jlox.TokenType.WHILE;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class Parser {
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
      if (match(VAR)) return varDeclaration();
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
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

  private Stmt statement() {
    if (match(PRINT)) return printStatement();
    return expressionStatement();
  }

  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
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
    Expr expr = equality();

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();
      if (expr instanceof Expr.Variable variable) {
        Token name = variable.name();
        return new Expr.Assignment(name, value);
      }
      error(equals, "Invalid assignment target.");
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
    return primary();
  }

  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);
    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal());
    }
    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }
    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
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
    Lox.error(token, message);
    return new ParseError();
  }

  // Move to the next statement
  private void synchronize() {
    advance();
    while (!isAtEnd()) {
      if (previous().type() == SEMICOLON) return;
      if (EnumSet.of(CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN).contains(peek().type()))
        return;
      advance();
    }
  }
}
