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

/** /lantern join, stop, offer, yes, and feed. */
public final class LanternCommand implements CommandExecutor, TabCompleter {
  private static final List<String> TOKENS = List.of("join", "stop", "offer", "yes", "feed");

  private final LanternActions actions;

  public LanternCommand(LanternActions actions) {
    this.actions = actions;
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
      return Usage.send(player, command);
    }
    switch (args[0]) {
      case "join" -> {
        if (args.length != 3
            || !RolePolicy.validRole(args[1])
            || !RolePolicy.validFeed(args[2])) {
          actions.lists(player);
          return true;
        }
        actions.join(player, args[1], args[2]);
        return true;
      }
      case "feed" -> {
        if (args.length != 2 || !RolePolicy.validFeed(args[1])) {
          actions.feedLists(player);
          return true;
        }
        actions.changeFeed(player, args[1]);
        return true;
      }
      case "stop" -> {
        if (args.length != 1) {
          return Usage.send(player, command);
        }
        actions.stop(player);
        return true;
      }
      case "yes" -> {
        if (args.length != 1) {
          return Usage.send(player, command);
        }
        actions.yes(player);
        return true;
      }
      case "offer" -> {
        if (args.length < 2) {
          actions.needsWords(player);
          return true;
        }
        String draft = joinDraft(args);
        if (draft.isEmpty()) {
          actions.needsWords(player);
          return true;
        }
        actions.offer(player, draft);
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
    if (args.length <= 1) {
      String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
      List<String> matches = new ArrayList<>();
      for (String token : TOKENS) {
        if (token.startsWith(prefix)) {
          matches.add(token);
        }
      }
      return matches;
    }
    if (args.length == 2 && "join".equals(args[0])) {
      return complete(RolePolicy.ROLES, args[1]);
    }
    if (args.length == 3 && "join".equals(args[0])) {
      return complete(RolePolicy.FEEDS, args[2]);
    }
    if (args.length == 2 && "feed".equals(args[0])) {
      return complete(RolePolicy.FEEDS, args[1]);
    }
    return List.of();
  }

  private static List<String> complete(List<String> tokens, String typed) {
    String prefix = typed.toLowerCase(Locale.ROOT);
    List<String> matches = new ArrayList<>();
    for (String token : tokens) {
      if (token.startsWith(prefix)) {
        matches.add(token);
      }
    }
    return matches;
  }

  private static String joinDraft(String[] args) {
    StringBuilder draft = new StringBuilder();
    for (int i = 1; i < args.length; i++) {
      if (i > 1) {
        draft.append(' ');
      }
      draft.append(args[i]);
    }
    return draft.toString().trim();
  }
}
