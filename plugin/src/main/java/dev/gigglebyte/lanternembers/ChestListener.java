package dev.gigglebyte.lanternembers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Stall chest inserts and takes. Other chests write nothing. */
public final class ChestListener implements Listener {
  private final EventLog eventLog;
  private final Plots plots;

  public ChestListener(EventLog eventLog, Plots plots) {
    this.eventLog = eventLog;
    this.plots = plots;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    Inventory top = event.getView().getTopInventory();
    if (!isStallChest(top)) {
      return;
    }
    int topSize = top.getSize();
    int raw = event.getRawSlot();
    InventoryAction action = event.getAction();
    if (action == InventoryAction.NOTHING
        || action == InventoryAction.CLONE_STACK
        || action == InventoryAction.DROP_ALL_CURSOR
        || action == InventoryAction.DROP_ONE_CURSOR) {
      return;
    }
    if (action == InventoryAction.COLLECT_TO_CURSOR) {
      record(player, collect(event, top, topSize));
      return;
    }
    if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
      ItemStack moving = event.getCurrentItem();
      if (moving == null || moving.getType() == Material.AIR) {
        return;
      }
      if (raw < topSize) {
        int count = Math.min(moving.getAmount(), room(event.getView().getBottomInventory(), moving));
        recordOne(player, moving.getType(), count, "out");
      } else {
        int count = Math.min(moving.getAmount(), room(top, moving));
        recordOne(player, moving.getType(), count, "in");
      }
      return;
    }
    if (raw < 0 || raw >= topSize) {
      return;
    }
    ItemStack slot = event.getCurrentItem();
    ItemStack cursor = event.getCursor();
    switch (action) {
      case PLACE_ALL, PLACE_SOME -> {
        int count = placed(slot, cursor, cursor == null ? 0 : cursor.getAmount());
        recordOne(player, typeOf(cursor), count, "in");
      }
      case PLACE_ONE -> {
        int count = placed(slot, cursor, 1);
        recordOne(player, typeOf(cursor), count, "in");
      }
      case PICKUP_ALL -> recordOne(player, typeOf(slot), amount(slot), "out");
      case PICKUP_HALF -> recordOne(player, typeOf(slot), (amount(slot) + 1) / 2, "out");
      case PICKUP_ONE, DROP_ONE_SLOT -> recordOne(player, typeOf(slot), Math.min(1, amount(slot)), "out");
      case PICKUP_SOME -> recordOne(player, typeOf(slot), pickupSome(slot, cursor), "out");
      case DROP_ALL_SLOT -> recordOne(player, typeOf(slot), amount(slot), "out");
      case SWAP_WITH_CURSOR -> {
        recordOne(player, typeOf(slot), amount(slot), "out");
        recordOne(player, typeOf(cursor), amount(cursor), "in");
      }
      case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
        int button = event.getHotbarButton();
        ItemStack hotbar = button >= 0
            ? player.getInventory().getItem(button)
            : player.getInventory().getItemInOffHand();
        recordOne(player, typeOf(slot), amount(slot), "out");
        recordOne(player, typeOf(hotbar), amount(hotbar), "in");
      }
      default -> {
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    Inventory top = event.getView().getTopInventory();
    if (!isStallChest(top)) {
      return;
    }
    int topSize = top.getSize();
    for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
      int raw = entry.getKey();
      if (raw < 0 || raw >= topSize) {
        continue;
      }
      diff(player, top.getItem(raw), entry.getValue());
    }
  }

  private boolean isStallChest(Inventory top) {
    if (top.getType() != InventoryType.CHEST) {
      return false;
    }
    Location location = top.getLocation();
    return plots.isStallChest(location);
  }

  private void record(Player player, List<Move> moves) {
    for (Move move : moves) {
      eventLog.chest(player.getName(), move.item, move.count, move.direction);
    }
  }

  private void recordOne(Player player, Material material, int count, String direction) {
    if (material == null || material == Material.AIR || count <= 0) {
      return;
    }
    eventLog.chest(player.getName(), material.name(), count, direction);
  }

  private void diff(Player player, ItemStack before, ItemStack after) {
    Material beforeType = typeOf(before);
    Material afterType = typeOf(after);
    int beforeCount = amount(before);
    int afterCount = amount(after);
    if (beforeType == afterType) {
      int delta = afterCount - beforeCount;
      if (delta > 0) {
        recordOne(player, afterType, delta, "in");
      } else if (delta < 0) {
        recordOne(player, beforeType, -delta, "out");
      }
      return;
    }
    recordOne(player, beforeType, beforeCount, "out");
    recordOne(player, afterType, afterCount, "in");
  }

  private List<Move> collect(InventoryClickEvent event, Inventory top, int topSize) {
    ItemStack cursor = event.getCursor();
    if (cursor == null || cursor.getType() == Material.AIR) {
      return List.of();
    }
    int room = cursor.getMaxStackSize() - cursor.getAmount();
    if (room <= 0) {
      return List.of();
    }
    if (event.getRawSlot() >= topSize) {
      int fromPlayer = similar(event.getView().getBottomInventory(), cursor);
      room -= Math.min(room, fromPlayer);
    }
    int take = Math.min(room, similar(top, cursor));
    if (take <= 0) {
      return List.of();
    }
    List<Move> moves = new ArrayList<>();
    moves.add(new Move(cursor.getType().name(), take, "out"));
    return moves;
  }

  private static int similar(Inventory inventory, ItemStack cursor) {
    int total = 0;
    for (ItemStack slot : inventory.getStorageContents()) {
      if (slot != null && slot.isSimilar(cursor)) {
        total += slot.getAmount();
      }
    }
    return total;
  }

  private static int room(Inventory inventory, ItemStack stack) {
    int max = stack.getMaxStackSize();
    int space = 0;
    ItemStack[] contents = inventory.getStorageContents();
    if (contents == null) {
      contents = inventory.getContents();
    }
    if (contents == null) {
      return 0;
    }
    for (ItemStack slot : contents) {
      if (slot == null || slot.getType() == Material.AIR) {
        space += max;
      } else if (slot.isSimilar(stack) && slot.getAmount() < Math.min(max, slot.getMaxStackSize())) {
        space += Math.min(max, slot.getMaxStackSize()) - slot.getAmount();
      }
    }
    return space;
  }

  private static int placed(ItemStack slot, ItemStack cursor, int want) {
    if (cursor == null || cursor.getType() == Material.AIR || want <= 0) {
      return 0;
    }
    int current = amount(slot);
    if (current > 0 && (slot == null || !slot.isSimilar(cursor))) {
      return 0;
    }
    int max = cursor.getMaxStackSize();
    int space = max - current;
    return Math.max(0, Math.min(want, Math.min(space, cursor.getAmount())));
  }

  private static int pickupSome(ItemStack slot, ItemStack cursor) {
    if (cursor == null || cursor.getType() == Material.AIR) {
      return 0;
    }
    int room = cursor.getMaxStackSize() - cursor.getAmount();
    return Math.max(0, Math.min(room, amount(slot)));
  }

  private static int amount(ItemStack stack) {
    if (stack == null || stack.getType() == Material.AIR) {
      return 0;
    }
    return stack.getAmount();
  }

  private static Material typeOf(ItemStack stack) {
    if (stack == null) {
      return Material.AIR;
    }
    return stack.getType();
  }

  private record Move(String item, int count, String direction) {}
}
