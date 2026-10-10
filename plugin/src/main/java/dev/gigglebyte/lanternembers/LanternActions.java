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

  void join(Player player, String role, String feed) {
    eventLog.join(player.getName(), role, feed, OffsetDateTime.now());
    String tab = strings.format("tab-mark", Map.of("role", role, "player", player.getName()));
    if (tab != null) {
      player.playerListName(Component.text(tab));
    }
    if (RolePolicy.mayMark(role)) {
      MarkItems.giveIfAbsent(player, strings);
    }
    if (RolePolicy.mayPhrase(role)) {
      PhraseItems.giveIfAbsent(player, strings);
    }
    boardBook.give(player);
  }

  void lists(Player player) {
    strings.send(player, "join-lists", Map.of());
  }

  void feedLists(Player player) {
    strings.send(player, "feed-lists", Map.of());
  }

  void changeFeed(Player player, String feed) {
    if (!eventLog.inHour(player.getName())) {
      feedLists(player);
      return;
    }
    String role = eventLog.roleOf(player.getName());
    eventLog.feed(player.getName(), role, feed, OffsetDateTime.now());
  }

  void stop(Player player) {
    eventLog.stop(player.getName(), OffsetDateTime.now());
    strings.send(player, "stop-line", Map.of("player", player.getName()));
  }

  void offer(Player player, String draft) {
    eventLog.offer(player.getName(), draft);
    strings.send(player, "draft-held", Map.of());
  }

  /** Phrase stick. The offer is stored and not sent. */
  void offerQuiet(Player player, String draft) {
    eventLog.offer(player.getName(), draft);
  }

  void needsWords(Player player) {
    strings.send(player, "offer-needs-words", Map.of());
  }

  void yes(Player player) {
    LogEvent pending = eventLog.pendingFor(player.getName());
    if (pending == null) {
      strings.send(player, "yes-waiting", Map.of());
      return;
    }
    String role = eventLog.roleOf(player.getName());
    if ("mark".equals(pending.type()) && !RolePolicy.mayConfirmMark(role)) {
      strings.send(player, "stays-lookout", Map.of());
      return;
    }
    if ("offer".equals(pending.type()) && !RolePolicy.mayConfirmOffer(role)) {
      strings.send(player, "stays-storykeeper", Map.of());
      return;
    }
    EventLog.Confirm confirm = eventLog.confirmLatest(player.getName());
    if (!confirm.pending() || !confirm.line()) {
      return;
    }
    String text = confirm.text() == null ? "" : confirm.text();
    deliver(player.getName(), text);
  }

  private void deliver(String author, String text) {
    Map<String, String> published = Map.of("player", author, "text", text);
    Map<String, String> summary = Map.of("player", author);
    for (Player online : Bukkit.getOnlinePlayers()) {
      boolean named = mentioned(text, online.getName());
      if (!eventLog.inHour(online.getName())) {
        if (named) {
          strings.sendGold(online, "published-line", published);
        }
        continue;
      }
      String feed = eventLog.feedOf(online.getName());
      if ("summary".equals(feed)) {
        strings.send(online, "summary-line", summary);
        continue;
      }
      boolean authorSelf = online.getName().equals(author);
      if ("mentions".equals(feed) && !authorSelf && !named) {
        continue;
      }
      strings.sendGold(online, "published-line", published);
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
