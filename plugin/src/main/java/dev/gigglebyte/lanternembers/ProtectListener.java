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
    deny(
        event,
        event.getPlayer(),
        event.getBlock(),
        RolePolicy.mayBreak(
            event.getPlayer().getName(),
            role(event.getPlayer()),
            eventLog.inHour(event.getPlayer().getName()),
            marker(event.getBlock()),
            confirmed(event.getBlock()),
            marked(event.getBlock())));
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlace(BlockPlaceEvent event) {
    if (event instanceof BlockMultiPlaceEvent multi) {
      for (BlockState state : multi.getReplacedBlockStates()) {
        if (!placedOk(event.getPlayer(), state.getBlock())) {
          denyPlace(event, event.getPlayer(), state.getBlock());
          return;
        }
      }
      return;
    }
    if (!placedOk(event.getPlayer(), event.getBlockPlaced())) {
      denyPlace(event, event.getPlayer(), event.getBlockPlaced());
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

  private void denyPlace(BlockPlaceEvent event, org.bukkit.entity.Player player, Block block) {
    event.setCancelled(true);
    tell(player, block);
  }

  private void deny(BlockBreakEvent event, org.bukkit.entity.Player player, Block block, boolean allowed) {
    if (allowed) {
      return;
    }
    event.setCancelled(true);
    tell(player, block);
  }

  private boolean placedOk(org.bukkit.entity.Player player, Block block) {
    return RolePolicy.mayPlace(
        player.getName(),
        role(player),
        eventLog.inHour(player.getName()),
        marker(block),
        confirmed(block),
        marked(block));
  }

  private void tell(org.bukkit.entity.Player player, Block block) {
    EventLog.Edge edge = edge(block);
    if (edge != null && !edge.confirmed() && !player.getName().equals(edge.marker())) {
      strings.send(player, "protect", Map.of("player", edge.marker()));
      return;
    }
    if (edge != null && edge.confirmed()) {
      strings.send(player, "stays-builder", Map.of());
      return;
    }
    if ("storykeeper".equals(role(player))) {
      strings.send(player, "stays-builder", Map.of());
      return;
    }
    strings.send(player, "stays-mark", Map.of());
  }

  private String role(org.bukkit.entity.Player player) {
    return eventLog.roleOf(player.getName());
  }

  private boolean marked(Block block) {
    return edge(block) != null;
  }

  private boolean confirmed(Block block) {
    EventLog.Edge edge = edge(block);
    return edge != null && edge.confirmed();
  }

  private String marker(Block block) {
    EventLog.Edge edge = edge(block);
    if (edge == null) {
      return null;
    }
    return edge.marker();
  }

  private EventLog.Edge edge(Block block) {
    if (block.getWorld() == null) {
      return null;
    }
    return eventLog.edgeAt(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
  }

  private String owner(Block block) {
    if (block.getWorld() == null) {
      return null;
    }
    return eventLog.markOwner(
        block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
  }
}
