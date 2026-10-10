package dev.gigglebyte.lanternembers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.World;

/** Append-only event log at lantern-embers/events.jsonl in the plots world. */
public final class EventLog {
  private final Logger logger;
  private final Path path;
  private final List<LogEvent> events = new ArrayList<>();

  private EventLog(Logger logger, Path path, List<LogEvent> loaded) throws IOException {
    this.logger = logger;
    this.path = path;
    this.events.addAll(loaded);
    ensureTrailingNewline();
  }

  static EventLog open(Logger logger, World world) throws IOException {
    Path path = world.getWorldFolder().toPath().resolve("lantern-embers").resolve("events.jsonl");
    Files.createDirectories(path.getParent());
    List<LogEvent> loaded = new ArrayList<>();
    if (Files.exists(path)) {
      int lineNumber = 0;
      for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
        lineNumber++;
        if (line.isBlank()) {
          continue;
        }
        Map<String, String> values = JsonLines.read(line);
        if (values == null || values.get("type") == null || values.get("id") == null) {
          logger.warning("event line skipped: " + lineNumber);
          continue;
        }
        loaded.add(new LogEvent(values));
      }
    }
    if (!Files.exists(path)) {
      Files.createFile(path);
    }
    return new EventLog(logger, path, loaded);
  }

  public Path path() {
    return path;
  }

  public synchronized void close() {
    // Each append flushes its own stream. Nothing stays open.
  }

  public synchronized List<LogEvent> copy() {
    return List.copyOf(events);
  }

  public synchronized void join(String player, String role, String feed, OffsetDateTime time) {
    LinkedHashMap<String, Object> extra = new LinkedHashMap<>();
    extra.put("role", role);
    extra.put("feed", feed);
    extra.put("time", time.toString());
    append("join", player, extra);
  }

  /** Join-shaped feed change. Does not leave the hour. */
  public synchronized void feed(String player, String role, String feed, OffsetDateTime time) {
    LinkedHashMap<String, Object> extra = new LinkedHashMap<>();
    extra.put("role", role == null ? "" : role);
    extra.put("feed", feed);
    extra.put("time", time.toString());
    append("feed", player, extra);
  }

  public synchronized void mark(
      String player, String world, int x, int y, int z, String face) {
    LinkedHashMap<String, Object> extra = new LinkedHashMap<>();
    extra.put("world", world);
    extra.put("x", x);
    extra.put("y", y);
    extra.put("z", z);
    extra.put("face", face);
    append("mark", player, extra);
  }

  public synchronized void offer(String player, String draft) {
    LinkedHashMap<String, Object> extra = new LinkedHashMap<>();
    extra.put("draft", draft);
    append("offer", player, extra);
  }

  public synchronized void stop(String player, OffsetDateTime time) {
    LinkedHashMap<String, Object> extra = new LinkedHashMap<>();
    extra.put("time", time.toString());
    append("stop", player, extra);
  }

  public synchronized void togglePresence(String player) {
    String next = "lit".equals(latestPresence(player)) ? "unlit" : "lit";
    presence(player, next);
  }

  public synchronized boolean light(String player) {
    if ("lit".equals(latestPresence(player))) {
      return false;
    }
    presence(player, "lit");
    return true;
  }

  public synchronized boolean dark(String player) {
    if ("unlit".equals(latestPresence(player))) {
      return false;
    }
    presence(player, "unlit");
    return true;
  }

  /**
   * Confirms the latest unconfirmed offer or mark for this player.
   * Returns the offer draft when a line was written, or null when the target was a mark.
   * Returns a sentinel via {@link Confirm#waiting()} when nothing is pending.
   */
  public synchronized Confirm confirmLatest(String player) {
    LogEvent target = pending(player);
    if (target == null) {
      return Confirm.waiting();
    }
    LinkedHashMap<String, Object> extra = new LinkedHashMap<>();
    extra.put("target", target.id());
    if (append("confirm", player, extra) == null) {
      return Confirm.waiting();
    }
    if ("offer".equals(target.type())) {
      String draft = target.get("draft");
      if (draft == null) {
        draft = "";
      }
      LinkedHashMap<String, Object> line = new LinkedHashMap<>();
      line.put("text", draft);
      line.put("colour", "gold");
      append("line", player, line);
      return Confirm.published(draft);
    }
    return Confirm.markOnly();
  }

  public synchronized void chest(String player, String item, int count, String direction) {
    if (count <= 0 || item == null || item.isEmpty()) {
      return;
    }
    LinkedHashMap<String, Object> extra = new LinkedHashMap<>();
    extra.put("item", item);
    extra.put("count", count);
    extra.put("direction", direction);
    append("chest", player, extra);
  }

  public synchronized boolean inHour(String player) {
    String state = null;
    for (LogEvent event : events) {
      if (!player.equals(event.player())) {
        continue;
      }
      if ("join".equals(event.type())) {
        state = "join";
      } else if ("stop".equals(event.type())) {
        state = "stop";
      }
    }
    return "join".equals(state);
  }

  public synchronized boolean everJoined(String player) {
    for (LogEvent event : events) {
      if (player.equals(event.player()) && "join".equals(event.type())) {
        return true;
      }
    }
    return false;
  }

  public synchronized String latestPresence(String player) {
    String state = null;
    for (LogEvent event : events) {
      if (player.equals(event.player()) && "presence".equals(event.type())) {
        state = event.get("state");
      }
    }
    return state;
  }

  public synchronized String roleOf(String player) {
    String role = null;
    for (LogEvent event : events) {
      if (!player.equals(event.player())) {
        continue;
      }
      if (!"join".equals(event.type()) && !"feed".equals(event.type())) {
        continue;
      }
      String value = event.get("role");
      if (value != null && !value.isEmpty()) {
        role = value;
      }
    }
    return role;
  }

  /** Latest feed. Old lines that say lantern are read as full. */
  public synchronized String feedOf(String player) {
    String feed = null;
    for (LogEvent event : events) {
      if (!player.equals(event.player())) {
        continue;
      }
      if (!"join".equals(event.type()) && !"feed".equals(event.type())) {
        continue;
      }
      String value = event.get("feed");
      if (value == null || value.isEmpty()) {
        continue;
      }
      feed = "lantern".equals(value) ? "full" : value;
    }
    return feed;
  }

  /** Latest unconfirmed offer draft for this player, or null. */
  public synchronized String heldDraft(String player) {
    Set<String> confirmed = confirmedIds();
    String draft = null;
    for (LogEvent event : events) {
      if (!player.equals(event.player()) || !"offer".equals(event.type())) {
        continue;
      }
      if (event.id() == null || confirmed.contains(event.id())) {
        continue;
      }
      draft = event.get("draft");
    }
    return draft;
  }

  public synchronized Edge edgeAt(String world, int x, int y, int z) {
    LogEvent latest = null;
    for (LogEvent event : events) {
      if (!"mark".equals(event.type())) {
        continue;
      }
      if (!world.equals(event.get("world"))) {
        continue;
      }
      if (event.integer("x") == x && event.integer("y") == y && event.integer("z") == z) {
        latest = event;
      }
    }
    if (latest == null || latest.id() == null) {
      return null;
    }
    boolean confirmed = confirmedIds().contains(latest.id());
    return new Edge(latest.player(), confirmed);
  }

  public synchronized String markOwner(String world, int x, int y, int z) {
    Edge edge = edgeAt(world, x, y, z);
    if (edge == null) {
      return null;
    }
    return edge.marker();
  }

  private void presence(String player, String state) {
    LinkedHashMap<String, Object> extra = new LinkedHashMap<>();
    extra.put("state", state);
    append("presence", player, extra);
  }

  private Set<String> confirmedIds() {
    Set<String> confirmed = new HashSet<>();
    for (LogEvent event : events) {
      if ("confirm".equals(event.type()) && event.get("target") != null) {
        confirmed.add(event.get("target"));
      }
    }
    return confirmed;
  }

  public synchronized LogEvent pendingFor(String player) {
    return pending(player);
  }

  private LogEvent pending(String player) {
    Set<String> confirmed = confirmedIds();
    LogEvent found = null;
    for (LogEvent event : events) {
      if (!player.equals(event.player())) {
        continue;
      }
      if (!"offer".equals(event.type()) && !"mark".equals(event.type())) {
        continue;
      }
      if (event.id() == null || confirmed.contains(event.id())) {
        continue;
      }
      found = event;
    }
    return found;
  }

  private LogEvent append(String type, String player, LinkedHashMap<String, Object> extra) {
    if (player == null || player.isEmpty()) {
      logger.warning("event needs a player");
      return null;
    }
    LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
    fields.put("id", UUID.randomUUID().toString());
    fields.put("type", type);
    fields.put("player", player);
    fields.putAll(extra);
    String json = JsonLines.write(fields);
    try {
      writeLine(json);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "could not append event", ex);
      return null;
    }
    Map<String, String> stored = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : fields.entrySet()) {
      stored.put(entry.getKey(), String.valueOf(entry.getValue()));
    }
    LogEvent event = new LogEvent(stored);
    events.add(event);
    return event;
  }

  private void ensureTrailingNewline() throws IOException {
    if (!Files.exists(path) || Files.size(path) == 0) {
      return;
    }
    byte[] bytes = Files.readAllBytes(path);
    if (bytes.length > 0 && bytes[bytes.length - 1] != '\n') {
      writeRaw("\n");
    }
  }

  private void writeLine(String json) throws IOException {
    writeRaw(json + "\n");
  }

  private void writeRaw(String text) throws IOException {
    try (FileOutputStream out = new FileOutputStream(path.toFile(), true)) {
      out.write(text.getBytes(StandardCharsets.UTF_8));
      out.flush();
      out.getFD().sync();
    }
  }

  /** One marked block and whether yes has confirmed that mark. */
  public static final class Edge {
    private final String marker;
    private final boolean confirmed;

    Edge(String marker, boolean confirmed) {
      this.marker = marker;
      this.confirmed = confirmed;
    }

    String marker() {
      return marker;
    }

    boolean confirmed() {
      return confirmed;
    }
  }

  /** Result of one yes. */
  public static final class Confirm {
    private final boolean pending;
    private final boolean line;
    private final String text;

    private Confirm(boolean pending, boolean line, String text) {
      this.pending = pending;
      this.line = line;
      this.text = text;
    }

    static Confirm waiting() {
      return new Confirm(false, false, null);
    }

    static Confirm markOnly() {
      return new Confirm(true, false, null);
    }

    static Confirm published(String text) {
      return new Confirm(true, true, text);
    }

    boolean pending() {
      return pending;
    }

    boolean line() {
      return line;
    }

    String text() {
      return text;
    }
  }
}
