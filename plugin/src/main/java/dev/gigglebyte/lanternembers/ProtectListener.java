package dev.gigglebyte.lanternembers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/** Keeps a marked block in place for every player except the one named on the mark. */
public final class ProtectListener implements Listener {
  private final EventLog eventLog;
  private final Strings strings;

  public ProtectListener(EventLog eventLog, Strings strings) {
    this.eventLog = eventLog;
    this.strings = strings;
  }

  @EventHandler(ignoreCancelled = true)
  public void onBreak(BlockBreakEvent event) {
    String owner = owner(event.getBlock());
    if (owner == null || owner.equals(event.getPlayer().getName())) {
      return;
    }
    event.setCancelled(true);
    strings.send(event.getPlayer(), "protect", Map.of("player", owner));
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlace(BlockPlaceEvent event) {
    if (event instanceof BlockMultiPlaceEvent multi) {
      for (BlockState state : multi.getReplacedBlockStates()) {
        if (blocked(state.getBlock(), event.getPlayer().getName())) {
          event.setCancelled(true);
          return;
        }
      }
      return;
    }
    if (blocked(event.getBlockPlaced(), event.getPlayer().getName())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPistonExtend(BlockPistonExtendEvent event) {
    cancelPiston(event, event.getBlocks());
  }

  @EventHandler(ignoreCancelled = true)
  public void onPistonRetract(BlockPistonRetractEvent event) {
    cancelPiston(event, event.getBlocks());
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityExplode(EntityExplodeEvent event) {
    if (hitsMarked(event.blockList())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockExplode(BlockExplodeEvent event) {
    if (hitsMarked(event.blockList())) {
      event.setCancelled(true);
    }
  }

  private void cancelPiston(Cancellable event, List<Block> blocks) {
    for (Block block : blocks) {
      if (owner(block) != null) {
        event.setCancelled(true);
        return;
      }
    }
  }

  private boolean hitsMarked(List<Block> blocks) {
    boolean hit = false;
    Iterator<Block> iterator = blocks.iterator();
    while (iterator.hasNext()) {
      if (owner(iterator.next()) != null) {
        iterator.remove();
        hit = true;
      }
    }
    return hit;
  }

  private boolean blocked(Block block, String player) {
    String owner = owner(block);
    return owner != null && !owner.equals(player);
  }

  private String owner(Block block) {
    if (block.getWorld() == null) {
      return null;
    }
    return eventLog.markOwner(
        block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
  }
}
