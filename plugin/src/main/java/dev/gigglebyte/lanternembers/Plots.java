package dev.gigglebyte.lanternembers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Stall, chest, and lantern coordinates from plots.yml. */
public final class Plots {
  private final String world;
  private final int confirmX;
  private final int confirmY;
  private final int confirmZ;
  private final Set<Pos> chests;
  private final Set<Pos> lanterns;

  private Plots(
      String world,
      int confirmX,
      int confirmY,
      int confirmZ,
      Set<Pos> chests,
      Set<Pos> lanterns) {
    this.world = world;
    this.confirmX = confirmX;
    this.confirmY = confirmY;
    this.confirmZ = confirmZ;
    this.chests = chests;
    this.lanterns = lanterns;
  }

  static Plots load(JavaPlugin plugin) {
    Logger logger = plugin.getLogger();
    String world = "world";
    int confirmX = 0;
    int confirmY = 65;
    int confirmZ = 4;
    Set<Pos> chests = new HashSet<>();
    Set<Pos> lanterns = new HashSet<>();
    InputStream in = plugin.getResource("plots.yml");
    if (in == null) {
      logger.warning("plots.yml world");
      return new Plots(world, confirmX, confirmY, confirmZ, chests, lanterns);
    }
    try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
      String named = yaml.getString("world");
      if (named != null && !named.isEmpty()) {
        world = named;
      }
      confirmX = yaml.getInt("confirm-lantern.x");
      confirmY = yaml.getInt("confirm-lantern.y");
      confirmZ = yaml.getInt("confirm-lantern.z");
      List<Map<?, ?>> stalls = yaml.getMapList("stalls");
      for (Map<?, ?> stall : stalls) {
        Pos chest = pos(stall.get("chest"));
        Pos lantern = pos(stall.get("lantern"));
        if (chest != null) {
          chests.add(chest);
        }
        if (lantern != null) {
          lanterns.add(lantern);
        }
      }
      if (chests.isEmpty() || lanterns.isEmpty()) {
        logger.warning("plots.yml stalls");
      }
    } catch (Exception ex) {
      logger.warning("plots.yml stalls");
    }
    return new Plots(world, confirmX, confirmY, confirmZ, chests, lanterns);
  }

  public String world() {
    return world;
  }

  public boolean isConfirmLantern(Block block) {
    return sameWorld(block) && block.getX() == confirmX && block.getY() == confirmY
        && block.getZ() == confirmZ;
  }

  public boolean isStallLantern(Block block) {
    return sameWorld(block) && lanterns.contains(new Pos(block.getX(), block.getY(), block.getZ()));
  }

  public boolean isStallChest(Location location) {
    if (location == null || location.getWorld() == null) {
      return false;
    }
    if (!world.equals(location.getWorld().getName())) {
      return false;
    }
    return chests.contains(
        new Pos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
  }

  private boolean sameWorld(Block block) {
    return block.getWorld() != null && world.equals(block.getWorld().getName());
  }

  private static Pos pos(Object raw) {
    if (!(raw instanceof Map<?, ?> map)) {
      return null;
    }
    Object x = map.get("x");
    Object y = map.get("y");
    Object z = map.get("z");
    if (x == null || y == null || z == null) {
      return null;
    }
    return new Pos(num(x), num(y), num(z));
  }

  private static int num(Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    return Integer.parseInt(String.valueOf(value));
  }

  private record Pos(int x, int y, int z) {}
}
