package ccs.jlox;

import static ccs.jlox.TokenType.AND;
import static ccs.jlox.TokenType.BANG;
import static ccs.jlox.TokenType.BANG_EQUAL;
import static ccs.jlox.TokenType.CLASS;
import static ccs.jlox.TokenType.COMMA;
import static ccs.jlox.TokenType.DOT;
import static ccs.jlox.TokenType.ELSE;
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
import static ccs.jlox.TokenType.LEFT_BRACE;
import static ccs.jlox.TokenType.LEFT_PAREN;
import static ccs.jlox.TokenType.LESS;
import static ccs.jlox.TokenType.LESS_EQUAL;
import static ccs.jlox.TokenType.MINUS;
import static ccs.jlox.TokenType.NIL;
import static ccs.jlox.TokenType.NUMBER;
import static ccs.jlox.TokenType.OR;
import static ccs.jlox.TokenType.PLUS;
import static ccs.jlox.TokenType.RETURN;
import static ccs.jlox.TokenType.RIGHT_BRACE;
import static ccs.jlox.TokenType.RIGHT_PAREN;
import static ccs.jlox.TokenType.SEMICOLON;
import static ccs.jlox.TokenType.SLASH;
import static ccs.jlox.TokenType.STAR;
import static ccs.jlox.TokenType.STRING;
import static ccs.jlox.TokenType.SUPER;
import static ccs.jlox.TokenType.THIS;
import static ccs.jlox.TokenType.TRUE;
import static ccs.jlox.TokenType.VAR;
import static ccs.jlox.TokenType.WHILE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Scanner {
  private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

  static {
    KEYWORDS.put("and", AND);
    KEYWORDS.put("class", CLASS);
    KEYWORDS.put("else", ELSE);
    KEYWORDS.put("false", FALSE);
    KEYWORDS.put("for", FOR);
    KEYWORDS.put("fun", FUN);
    KEYWORDS.put("if", IF);
    KEYWORDS.put("nil", NIL);
    KEYWORDS.put("or", OR);
    KEYWORDS.put("return", RETURN);
    KEYWORDS.put("super", SUPER);
    KEYWORDS.put("this", THIS);
    KEYWORDS.put("true", TRUE);
    KEYWORDS.put("var", VAR);
    KEYWORDS.put("while", WHILE);
  }

  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0;
  private int current = 0;
  private int line = 1;

  public Scanner(String source) {
    this.source = source;
  }

  public List<Token> scanTokens() {
    while (!isAtEnd()) {
      // We are at the beginning of the next lexeme.
      start = current;
      scanToken();
    }
    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(' -> addToken(LEFT_PAREN);
      case ')' -> addToken(RIGHT_PAREN);
      case '{' -> addToken(LEFT_BRACE);
      case '}' -> addToken(RIGHT_BRACE);
      case ',' -> addToken(COMMA);
      case '.' -> addToken(DOT);
      case '-' -> addToken(MINUS);
      case '+' -> addToken(PLUS);
      case ';' -> addToken(SEMICOLON);
      case '*' -> addToken(STAR);
      case '!' -> addToken(match('=') ? BANG_EQUAL : BANG);
      case '=' -> addToken(match('=') ? EQUAL_EQUAL : EQUAL);
      case '<' -> addToken(match('=') ? LESS_EQUAL : LESS);
      case '>' -> addToken(match('=') ? GREATER_EQUAL : GREATER);
      case '/' -> handleSlash();
      case ' ', '\r', '\t' -> handleWhiteSpace();
      case '\n' -> incrementLine();
      case '"' -> string();
      default -> handleDefault(c);
    }
  }

  private void incrementLine() {
    line++;
  }

  private void handleWhiteSpace() {
    // NO-OP
  }

  private void handleSlash() {
    if (match('/')) {
      comment();
    } else {
      addToken(SLASH);
    }
  }

  private void handleDefault(char c) {
    if (isDigit(c)) {
      number();
    } else if (isAlpha(c)) {
      identifier();
    } else {
      Lox.error(line, "Unexpected character.");
    }
  }

  private void identifier() {
    while (isAlphaNumeric(peek())) advance();
    String text = source.substring(start, current);
    TokenType type = KEYWORDS.getOrDefault(text, IDENTIFIER);
    addToken(type);
  }

  private void comment() {
    while (peek() != '\n' && !isAtEnd()) advance();
  }

  private void string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') line++;
      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }

    advance();

    String value = source.substring(start + 1, current - 1);
    addToken(STRING, value);
  }

  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;
    current++;
    return true;
  }

  private char advance() {
    current++;
    return source.charAt(current - 1);
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private void number() {
    while (isDigit(peek())) advance();
    // Look for a fractional part.
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the "."
      advance();
      while (isDigit(peek())) advance();
    }
    addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }
}
