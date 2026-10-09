package dev.gigglebyte.lanternembers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** The mark stick. Persistent data key lanternembers:mark_tool. */
final class MarkItems {
  static final NamespacedKey KEY = new NamespacedKey("lanternembers", "mark_tool");

  private MarkItems() {}

  static boolean isMarkTool(ItemStack stack) {
    if (stack == null || stack.getType() != Material.STICK || !stack.hasItemMeta()) {
      return false;
    }
    Byte mark = stack.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.BYTE);
    return mark != null && mark == (byte) 1;
  }

  static void giveIfAbsent(Player player, Strings strings) {
    if (hasMarkTool(player)) {
      return;
    }
    ItemStack stick = new ItemStack(Material.STICK);
    ItemMeta meta = stick.getItemMeta();
    meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
    String name = strings.text("stick-name");
    if (name != null) {
      meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
    }
    stick.setItemMeta(meta);
    player.getInventory().addItem(stick);
  }

  private static boolean hasMarkTool(Player player) {
    PlayerInventory inventory = player.getInventory();
    for (ItemStack stack : inventory.getStorageContents()) {
      if (isMarkTool(stack)) {
        return true;
      }
    }
    return isMarkTool(inventory.getItemInOffHand());
  }
}
