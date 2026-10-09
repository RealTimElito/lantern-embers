package dev.gigglebyte.lanternembers;

import java.util.Locale;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Right-click with the mark stick writes one mark and cancels the interact. */
public final class MarkListener implements Listener {
  private final EventLog eventLog;

  public MarkListener(EventLog eventLog) {
    this.eventLog = eventLog;
  }

  @EventHandler(ignoreCancelled = false)
  public void onInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    if (!MarkItems.isMarkTool(event.getItem())) {
      return;
    }
    if (event.getHand() == EquipmentSlot.OFF_HAND
        && MarkItems.isMarkTool(event.getPlayer().getInventory().getItemInMainHand())) {
      return;
    }
    Block block = event.getClickedBlock();
    if (block == null) {
      return;
    }
    event.setCancelled(true);
    event.setUseInteractedBlock(Event.Result.DENY);
    event.setUseItemInHand(Event.Result.DENY);
    BlockFace face = event.getBlockFace();
    eventLog.mark(
        event.getPlayer().getName(),
        block.getWorld().getName(),
        block.getX(),
        block.getY(),
        block.getZ(),
        face.name().toLowerCase(Locale.ROOT));
  }
}
