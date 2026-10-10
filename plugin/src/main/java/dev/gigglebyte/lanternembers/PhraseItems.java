package dev.gigglebyte.lanternembers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Fixed phrases. Each click writes one offer and advances the list. */
final class PhraseItems {
  static final NamespacedKey KEY = new NamespacedKey("lanternembers", "phrase_tool");
  private static final List<String> KEYS =
      List.of("phrase-1", "phrase-2", "phrase-3", "phrase-4", "phrase-5");

  private final Map<String, Integer> index = new HashMap<>();

  static boolean isPhraseTool(ItemStack stack) {
    if (stack == null || stack.getType() != Material.STICK || !stack.hasItemMeta()) {
      return false;
    }
    Byte mark = stack.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.BYTE);
    return mark != null && mark == (byte) 1;
  }

  static void giveIfAbsent(Player player, Strings strings) {
    if (has(player)) {
      return;
    }
    ItemStack stick = new ItemStack(Material.STICK);
    ItemMeta meta = stick.getItemMeta();
    meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
    String name = strings.text("phrase-stick-name");
    if (name != null) {
      meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
    }
    stick.setItemMeta(meta);
    player.getInventory().addItem(stick);
  }

  String next(String player, Strings strings) {
    List<String> phrases = new ArrayList<>();
    for (String key : KEYS) {
      String phrase = strings.text(key);
      if (phrase != null && !phrase.isEmpty()) {
        phrases.add(phrase);
      }
    }
    if (phrases.isEmpty()) {
      return null;
    }
    int at = index.getOrDefault(player, 0);
    String phrase = phrases.get(at % phrases.size());
    index.put(player, at + 1);
    return phrase;
  }

  private static boolean has(Player player) {
    PlayerInventory inventory = player.getInventory();
    for (ItemStack stack : inventory.getStorageContents()) {
      if (isPhraseTool(stack)) {
        return true;
      }
    }
    return isPhraseTool(inventory.getItemInOffHand());
  }
}
