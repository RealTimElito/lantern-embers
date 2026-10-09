package dev.gigglebyte.lanternembers;

import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Confirm lantern acts as yes. Stall lantern toggles presence and writes nothing else. */
public final class LanternInteractListener implements Listener {
  private final Plots plots;
  private final LanternActions actions;

  public LanternInteractListener(Plots plots, LanternActions actions) {
    this.plots = plots;
    this.actions = actions;
  }

  @EventHandler(ignoreCancelled = false)
  public void onInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    if (event.getHand() == EquipmentSlot.OFF_HAND) {
      return;
    }
    if (MarkItems.isMarkTool(event.getItem())) {
      return;
    }
    Block block = event.getClickedBlock();
    if (block == null) {
      return;
    }
    if (plots.isConfirmLantern(block)) {
      deny(event);
      actions.yes(event.getPlayer());
      return;
    }
    if (plots.isStallLantern(block)) {
      deny(event);
      actions.togglePresence(event.getPlayer());
    }
  }

  private static void deny(PlayerInteractEvent event) {
    event.setCancelled(true);
    event.setUseInteractedBlock(Event.Result.DENY);
    event.setUseItemInHand(Event.Result.DENY);
  }
}
