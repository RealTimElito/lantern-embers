package dev.gigglebyte.lanternembers;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

/** Sends the plugin.yml usage line to one player. */
final class Usage {
  private Usage() {}

  static boolean send(Player player, Command command) {
    String usage = command.getUsage();
    if (usage != null && !usage.isEmpty()) {
      player.sendMessage(Component.text(usage));
    }
    return true;
  }
}
