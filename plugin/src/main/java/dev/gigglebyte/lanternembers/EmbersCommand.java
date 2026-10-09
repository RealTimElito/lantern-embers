package dev.gigglebyte.lanternembers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** /embers, /embers light, and /embers dark. */
public final class EmbersCommand implements CommandExecutor, TabCompleter {
  private static final List<String> TOKENS = List.of("light", "dark");

  private final LanternActions actions;
  private final BoardBook boardBook;

  public EmbersCommand(LanternActions actions, BoardBook boardBook) {
    this.actions = actions;
    this.boardBook = boardBook;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (!(sender instanceof Player player)) {
      return true;
    }
    if (args.length == 0) {
      boardBook.open(player);
      return true;
    }
    if (args.length != 1) {
      return Usage.send(player, command);
    }
    switch (args[0]) {
      case "light" -> {
        actions.light(player);
        return true;
      }
      case "dark" -> {
        actions.dark(player);
        return true;
      }
      default -> {
        return Usage.send(player, command);
      }
    }
  }

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (args.length > 1) {
      return List.of();
    }
    String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
    List<String> matches = new ArrayList<>();
    for (String token : TOKENS) {
      if (token.startsWith(prefix)) {
        matches.add(token);
      }
    }
    return matches;
  }
}
