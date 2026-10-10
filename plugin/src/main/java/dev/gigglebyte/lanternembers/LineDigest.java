package dev.gigglebyte.lanternembers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Every few minutes, summary feeds hear who added a line. No message bodies. */
final class LineDigest {
  private static final long EVERY_TICKS = 20L * 60L * 3L;

  private final EventLog eventLog;
  private final Strings strings;
  private final Set<String> told = new HashSet<>();

  LineDigest(EventLog eventLog, Strings strings) {
    this.eventLog = eventLog;
    this.strings = strings;
    for (LogEvent event : eventLog.copy()) {
      if ("line".equals(event.type()) && event.id() != null) {
        told.add(event.id());
      }
    }
  }

  long periodTicks() {
    return EVERY_TICKS;
  }

  void run() {
    List<String> names = new ArrayList<>();
    for (LogEvent event : eventLog.copy()) {
      if (!"line".equals(event.type()) || event.id() == null || event.player() == null) {
        continue;
      }
      if (!told.add(event.id())) {
        continue;
      }
      if (!names.contains(event.player())) {
        names.add(event.player());
      }
    }
    if (names.isEmpty()) {
      return;
    }
    String players = String.join(", ", names);
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!eventLog.inHour(player.getName())) {
        continue;
      }
      if (!"summary".equals(eventLog.feedOf(player.getName()))) {
        continue;
      }
      strings.send(player, "digest-line", Map.of("players", players));
    }
  }
}
