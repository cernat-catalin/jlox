package ccs.jlox.frontend;

import static ccs.jlox.ast.TokenType.AND;
import static ccs.jlox.ast.TokenType.AS;
import static ccs.jlox.ast.TokenType.BANG;
import static ccs.jlox.ast.TokenType.BANG_EQUAL;
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
import static ccs.jlox.ast.TokenType.NIL;
import static ccs.jlox.ast.TokenType.NUMBER;
import static ccs.jlox.ast.TokenType.OR;
import static ccs.jlox.ast.TokenType.PLUS;
import static ccs.jlox.ast.TokenType.QUESTION_MARK;
import static ccs.jlox.ast.TokenType.RETURN;
import static ccs.jlox.ast.TokenType.RIGHT_BRACE;
import static ccs.jlox.ast.TokenType.RIGHT_PAREN;
import static ccs.jlox.ast.TokenType.RIGHT_SQUARE_BRACKET;
import static ccs.jlox.ast.TokenType.SEMICOLON;
import static ccs.jlox.ast.TokenType.SLASH;
import static ccs.jlox.ast.TokenType.STAR;
import static ccs.jlox.ast.TokenType.STRING;
import static ccs.jlox.ast.TokenType.SUPER;
import static ccs.jlox.ast.TokenType.THIS;
import static ccs.jlox.ast.TokenType.TRUE;
import static ccs.jlox.ast.TokenType.VAR;
import static ccs.jlox.ast.TokenType.WHILE;

import ccs.jlox.Lox;
import ccs.jlox.ast.Token;
import ccs.jlox.ast.TokenType;
import ccs.jlox.error.ErrorHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Scanner {
  private static final ErrorHandler ERROR_HANDLER = Lox.getErrorHandler();

  private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

  static {
    KEYWORDS.put("and", AND);
    KEYWORDS.put("as", AS);
    KEYWORDS.put("class", CLASS);
    KEYWORDS.put("debug", DEBUG);
    KEYWORDS.put("else", ELSE);
    KEYWORDS.put("false", FALSE);
    KEYWORDS.put("for", FOR);
    KEYWORDS.put("fun", FUN);
    KEYWORDS.put("if", IF);
    KEYWORDS.put("import", IMPORT);
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
      case '[' -> addToken(LEFT_SQUARE_BRACKET);
      case ']' -> addToken(RIGHT_SQUARE_BRACKET);
      case '{' -> addToken(LEFT_BRACE);
      case '}' -> addToken(RIGHT_BRACE);
      case ',' -> addToken(COMMA);
      case '.' -> addToken(DOT);
      case '-' -> addToken(MINUS);
      case '+' -> addToken(PLUS);
      case ';' -> addToken(SEMICOLON);
      case '*' -> addToken(STAR);
      case '?' -> addToken(QUESTION_MARK);
      case ':' -> addToken(COLON);
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
      ERROR_HANDLER.error(line, "Unexpected character.");
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
      ERROR_HANDLER.error(line, "Unterminated string.");
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
