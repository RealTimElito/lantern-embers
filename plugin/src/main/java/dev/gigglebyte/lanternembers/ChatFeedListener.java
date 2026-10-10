package dev.gigglebyte.lanternembers;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Feed filter for vanilla chat. Paper delivers chat through AsyncChatEvent.
 * AsyncPlayerChatEvent is the legacy path, filtered the same way when it fires.
 */
public final class ChatFeedListener implements Listener {
  private final EventLog eventLog;

  public ChatFeedListener(EventLog eventLog) {
    this.eventLog = eventLog;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onPaperChat(AsyncChatEvent event) {
    String sender = event.getPlayer().getName();
    String text = PlainTextComponentSerializer.plainText().serialize(event.message());
    event.viewers().removeIf(viewer -> !allows(viewer, sender, text));
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onLegacyChat(AsyncPlayerChatEvent event) {
    String sender = event.getPlayer().getName();
    String text = event.getMessage();
    event.getRecipients().removeIf(player -> !allows(player, sender, text));
  }

  boolean allows(Audience viewer, String sender, String text) {
    if (!(viewer instanceof Player player)) {
      return true;
    }
    if (!eventLog.inHour(player.getName())) {
      return true;
    }
    String feed = eventLog.feedOf(player.getName());
    if ("full".equals(feed) || feed == null) {
      return true;
    }
    if ("summary".equals(feed)) {
      return false;
    }
    if ("mentions".equals(feed)) {
      return player.getName().equals(sender) || mentions(text, player.getName());
    }
    return true;
  }

  static boolean mentions(String text, String name) {
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
