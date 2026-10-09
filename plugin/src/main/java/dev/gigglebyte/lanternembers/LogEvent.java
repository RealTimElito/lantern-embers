package dev.gigglebyte.lanternembers;

import java.util.Map;

/** One stored event. Values are the JSON fields as text. */
final class LogEvent {
  private final Map<String, String> values;

  LogEvent(Map<String, String> values) {
    this.values = Map.copyOf(values);
  }

  String type() {
    return values.get("type");
  }

  String player() {
    return values.get("player");
  }

  String id() {
    return values.get("id");
  }

  String get(String key) {
    return values.get(key);
  }

  int integer(String key) {
    String raw = values.get(key);
    if (raw == null) {
      return 0;
    }
    return Integer.parseInt(raw);
  }
}
