package dev.gigglebyte.lanternembers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Player-facing text loaded from strings.yml. A key that is not in the file is logged. */
public final class Strings {
  private final Logger logger;
  private final Map<String, String> values;
  private final Set<String> reported = new HashSet<>();

  private Strings(Logger logger, Map<String, String> values) {
    this.logger = logger;
    this.values = values;
  }

  static Strings load(JavaPlugin plugin) {
    Logger logger = plugin.getLogger();
    Map<String, String> values = new HashMap<>();
    InputStream in = plugin.getResource("strings.yml");
    if (in == null) {
      logger.warning("strings.yml key: strings.yml");
      return new Strings(logger, values);
    }
    try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
      for (String key : yaml.getKeys(false)) {
        String text = yaml.getString(key);
        if (text != null) {
          values.put(key, text);
        }
      }
    } catch (Exception ex) {
      logger.warning("strings.yml key: strings.yml");
    }
    return new Strings(logger, values);
  }

  public String text(String key) {
    String value = values.get(key);
    if (value == null) {
      if (reported.add(key)) {
        logger.warning("strings.yml key: " + key);
      }
      return null;
    }
    return value;
  }

  public String format(String key, Map<String, String> placeholders) {
    String template = text(key);
    if (template == null) {
      return null;
    }
    StringBuilder out = new StringBuilder();
    int index = 0;
    while (index < template.length()) {
      int start = template.indexOf('{', index);
      if (start < 0) {
        out.append(template, index, template.length());
        break;
      }
      int end = template.indexOf('}', start + 1);
      if (end < 0) {
        out.append(template, index, template.length());
        break;
      }
      out.append(template, index, start);
      String name = template.substring(start + 1, end);
      String replacement = placeholders.get(name);
      if (replacement == null) {
        out.append('{').append(name).append('}');
      } else {
        out.append(replacement);
      }
      index = end + 1;
    }
    return out.toString();
  }

  public void send(Player player, String key, Map<String, String> placeholders) {
    String rendered = format(key, placeholders);
    if (rendered == null) {
      return;
    }
    player.sendMessage(Component.text(rendered));
  }

  public void sendGold(Player player, String key, Map<String, String> placeholders) {
    String rendered = format(key, placeholders);
    if (rendered == null) {
      return;
    }
    player.sendMessage(Component.text(rendered, NamedTextColor.GOLD));
  }
}
