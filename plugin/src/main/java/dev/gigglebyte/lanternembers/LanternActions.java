package dev.gigglebyte.lanternembers;

import java.time.OffsetDateTime;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Shared command and lantern actions. Chat goes out only after the log write. */
final class LanternActions {
  private final EventLog eventLog;
  private final Strings strings;
  private final BoardBook boardBook;

  LanternActions(EventLog eventLog, Strings strings, BoardBook boardBook) {
    this.eventLog = eventLog;
    this.strings = strings;
    this.boardBook = boardBook;
  }

  void join(Player player) {
    eventLog.join(player.getName(), OffsetDateTime.now());
    String prefix = strings.text("tab-prefix");
    if (prefix != null) {
      player.playerListName(Component.text(prefix + player.getName()));
    }
    MarkItems.giveIfAbsent(player, strings);
    boardBook.give(player);
  }

  void stop(Player player) {
    eventLog.stop(player.getName(), OffsetDateTime.now());
    strings.send(player, "stop-line", Map.of("player", player.getName()));
  }

  void offer(Player player, String draft) {
    eventLog.offer(player.getName(), draft);
    strings.send(player, "draft-held", Map.of());
  }

  void needsWords(Player player) {
    strings.send(player, "offer-needs-words", Map.of());
  }

  void yes(Player player) {
    EventLog.Confirm confirm = eventLog.confirmLatest(player.getName());
    if (!confirm.pending()) {
      strings.send(player, "yes-waiting", Map.of());
      return;
    }
    if (!confirm.line()) {
      return;
    }
    String text = confirm.text() == null ? "" : confirm.text();
    Map<String, String> published = Map.of("player", player.getName(), "text", text);
    Map<String, String> summary = Map.of("player", player.getName());
    for (Player online : Bukkit.getOnlinePlayers()) {
      boolean joined = eventLog.inHour(online.getName());
      boolean named = mentioned(text, online.getName());
      if (!joined && !named) {
        continue;
      }
      strings.sendGold(online, "published-line", published);
      strings.send(online, "summary-line", summary);
    }
  }

  void togglePresence(Player player) {
    eventLog.togglePresence(player.getName());
  }

  void light(Player player) {
    eventLog.light(player.getName());
  }

  void dark(Player player) {
    eventLog.dark(player.getName());
  }

  private static boolean mentioned(String text, String name) {
    String token = "@" + name;
    int from = 0;
    while (from < text.length()) {
      int at = text.indexOf(token, from);
      if (at < 0) {
        return false;
      }
      int end = at + token.length();
      boolean left = at == 0 || !isNameChar(text.charAt(at - 1));
      boolean right = end >= text.length() || !isNameChar(text.charAt(end));
      if (left && right) {
        return true;
      }
      from = at + 1;
    }
    return false;
  }

  private static boolean isNameChar(char c) {
    return (c >= 'A' && c <= 'Z')
        || (c >= 'a' && c <= 'z')
        || (c >= '0' && c <= '9')
        || c == '_';
  }
}
