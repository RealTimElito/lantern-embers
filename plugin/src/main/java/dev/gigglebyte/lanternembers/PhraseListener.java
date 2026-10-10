package dev.gigglebyte.lanternembers;

import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Phrase stick writes an offer and tells the clicker the held phrase. */
public final class PhraseListener implements Listener {
  private final EventLog eventLog;
  private final Strings strings;
  private final PhraseItems phrases;
  private final LanternActions actions;

  public PhraseListener(
      EventLog eventLog, Strings strings, PhraseItems phrases, LanternActions actions) {
    this.eventLog = eventLog;
    this.strings = strings;
    this.phrases = phrases;
    this.actions = actions;
  }

  @EventHandler(ignoreCancelled = false)
  public void onInteract(PlayerInteractEvent event) {
    Action action = event.getAction();
    if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
      return;
    }
    if (!PhraseItems.isPhraseTool(event.getItem())) {
      return;
    }
    if (event.getHand() == EquipmentSlot.OFF_HAND
        && PhraseItems.isPhraseTool(event.getPlayer().getInventory().getItemInMainHand())) {
      return;
    }
    deny(event);
    Player player = event.getPlayer();
    String role = eventLog.roleOf(player.getName());
    if (!eventLog.inHour(player.getName()) || !RolePolicy.mayPhrase(role)) {
      strings.send(player, "stays-lookout", Map.of());
      return;
    }
    String phrase = phrases.next(player.getName(), strings);
    if (phrase == null) {
      return;
    }
    actions.offerQuiet(player, phrase);
    strings.send(player, "held-phrase", Map.of("text", phrase));
  }

  private static void deny(PlayerInteractEvent event) {
    event.setCancelled(true);
    event.setUseInteractedBlock(Event.Result.DENY);
    event.setUseItemInHand(Event.Result.DENY);
    Block block = event.getClickedBlock();
    if (block != null) {
      event.setUseInteractedBlock(Event.Result.DENY);
    }
  }
}
