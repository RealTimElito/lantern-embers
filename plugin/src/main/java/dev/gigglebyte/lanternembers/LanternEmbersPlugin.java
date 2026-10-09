package dev.gigglebyte.lanternembers;

import java.util.logging.Level;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/** Paper plugin. Commands and listeners are thin wrappers over one event log. */
public final class LanternEmbersPlugin extends JavaPlugin {
  private EventLog eventLog;

  @Override
  public void onEnable() {
    Strings strings = Strings.load(this);
    Plots plots = Plots.load(this);
    World world = getServer().getWorld(plots.world());
    if (world == null) {
      getLogger().severe("plots world is not loaded: " + plots.world());
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    try {
      eventLog = EventLog.open(getLogger(), world);
    } catch (Exception ex) {
      getLogger().log(Level.SEVERE, "could not open event log", ex);
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    BoardBook boardBook = new BoardBook(strings, eventLog);
    LanternActions actions = new LanternActions(eventLog, strings, boardBook);
    PluginManager plugins = getServer().getPluginManager();
    plugins.registerEvents(new MarkListener(eventLog), this);
    plugins.registerEvents(new ProtectListener(eventLog, strings), this);
    plugins.registerEvents(new LanternInteractListener(plots, actions), this);
    plugins.registerEvents(new ChestListener(eventLog, plots), this);
    register("lantern", new LanternCommand(actions));
    register("embers", new EmbersCommand(actions, boardBook));
  }

  @Override
  public void onDisable() {
    if (eventLog != null) {
      eventLog.close();
      eventLog = null;
    }
  }

  private void register(String name, Object handler) {
    PluginCommand command = getCommand(name);
    if (command == null) {
      getLogger().warning("command is not registered: " + name);
      return;
    }
    if (handler instanceof CommandExecutor executor) {
      command.setExecutor(executor);
    }
    if (handler instanceof TabCompleter completer) {
      command.setTabCompleter(completer);
    }
  }
}
