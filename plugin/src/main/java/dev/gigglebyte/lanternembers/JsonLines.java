package dev.gigglebyte.lanternembers;

import java.util.LinkedHashMap;
import java.util.Map;

/** One-line JSON objects for the append-only event log. */
final class JsonLines {
  private JsonLines() {}

  static String write(Map<String, Object> fields) {
    StringBuilder out = new StringBuilder();
    out.append('{');
    boolean first = true;
    for (Map.Entry<String, Object> field : fields.entrySet()) {
      if (!first) {
        out.append(',');
      }
      first = false;
      out.append('"').append(escape(field.getKey())).append('"').append(':');
      Object value = field.getValue();
      if (value instanceof Integer || value instanceof Long) {
        out.append(value);
      } else {
        out.append('"').append(escape(String.valueOf(value))).append('"');
      }
    }
    out.append('}');
    return out.toString();
  }

  static Map<String, String> read(String line) {
    Parser parser = new Parser(line);
    Map<String, String> values = parser.parseObject();
    if (values == null || !parser.end()) {
      return null;
    }
    return values;
  }

  private static String escape(String text) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\b' -> out.append("\\b");
        case '\f' -> out.append("\\f");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    return out.toString();
  }

  private static final class Parser {
    private final String text;
    private int index;

    Parser(String text) {
      this.text = text;
    }

    boolean end() {
      skip();
      return index >= text.length();
    }

    Map<String, String> parseObject() {
      skip();
      if (!eat('{')) {
        return null;
      }
      Map<String, String> values = new LinkedHashMap<>();
      skip();
      if (eat('}')) {
        return values;
      }
      while (index < text.length()) {
        skip();
        String key = parseString();
        if (key == null) {
          return null;
        }
        skip();
        if (!eat(':')) {
          return null;
        }
        skip();
        String value = parseValue();
        if (value == null) {
          return null;
        }
        values.put(key, value);
        skip();
        if (eat('}')) {
          return values;
        }
        if (!eat(',')) {
          return null;
        }
      }
      return null;
    }

    private String parseValue() {
      skip();
      if (index >= text.length()) {
        return null;
      }
      char c = text.charAt(index);
      if (c == '"') {
        return parseString();
      }
      if (c == '-' || (c >= '0' && c <= '9')) {
        return parseNumber();
      }
      return null;
    }

    private String parseNumber() {
      int start = index;
      if (eat('-')) {
        // sign
      }
      if (index >= text.length() || text.charAt(index) < '0' || text.charAt(index) > '9') {
        return null;
      }
      while (index < text.length()) {
        char c = text.charAt(index);
        if (c < '0' || c > '9') {
          break;
        }
        index++;
      }
      return text.substring(start, index);
    }

    private String parseString() {
      if (!eat('"')) {
        return null;
      }
      StringBuilder out = new StringBuilder();
      while (index < text.length()) {
        char c = text.charAt(index++);
        if (c == '"') {
          return out.toString();
        }
        if (c != '\\') {
          out.append(c);
          continue;
        }
        if (index >= text.length()) {
          return null;
        }
        char esc = text.charAt(index++);
        switch (esc) {
          case '"', '\\', '/' -> out.append(esc);
          case 'b' -> out.append('\b');
          case 'f' -> out.append('\f');
          case 'n' -> out.append('\n');
          case 'r' -> out.append('\r');
          case 't' -> out.append('\t');
          case 'u' -> {
            if (index + 4 > text.length()) {
              return null;
            }
            int code = 0;
            for (int i = 0; i < 4; i++) {
              int digit = Character.digit(text.charAt(index++), 16);
              if (digit < 0) {
                return null;
              }
              code = (code << 4) + digit;
            }
            out.append((char) code);
          }
          default -> {
            return null;
          }
        }
      }
      return null;
    }

    private void skip() {
      while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
        index++;
      }
    }

    private boolean eat(char expected) {
      if (index < text.length() && text.charAt(index) == expected) {
        index++;
        return true;
      }
      return false;
    }
  }
}
