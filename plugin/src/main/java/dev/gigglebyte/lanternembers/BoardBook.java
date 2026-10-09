package dev.gigglebyte.lanternembers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/** Hour book and board book. Both are views over the event log. */
public final class BoardBook {
  private static final int PAGE_LIMIT = 200;

  private final Strings strings;
  private final EventLog eventLog;

  public BoardBook(Strings strings, EventLog eventLog) {
    this.strings = strings;
    this.eventLog = eventLog;
  }

  public void open(Player player) {
    String title = strings.text("book-title");
    ItemStack book = create(player);
    if (book == null) {
      return;
    }
    if (title != null && holds(player, title)) {
      removeOlder(player, title);
      player.getInventory().addItem(book);
    }
    player.openBook(book);
  }

  public void give(Player player) {
    String title = strings.text("book-title");
    if (title != null) {
      removeOlder(player, title);
    }
    ItemStack book = create(player);
    if (book == null) {
      return;
    }
    player.getInventory().addItem(book);
    player.openBook(book);
  }

  public ItemStack create(Player player) {
    String title = strings.text("book-title");
    String author = strings.text("book-author");
    if (title == null || author == null) {
      return null;
    }
    List<String> pages = pages(player);
    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
    BookMeta meta = (BookMeta) book.getItemMeta();
    meta.title(Component.text(title));
    meta.author(Component.text(author));
    meta.setTitle(title);
    meta.setAuthor(author);
    List<Component> components = new ArrayList<>();
    for (String page : pages) {
      components.add(Component.text(page));
    }
    meta.pages(components);
    book.setItemMeta(meta);
    return book;
  }

  private List<String> pages(Player player) {
    List<LogEvent> events = eventLog.copy();
    StringBuilder current = new StringBuilder();
    String sample = strings.text("sample-page");
    if (sample != null) {
      current.append(sample);
    }
    if (eventLog.everJoined(player.getName())) {
      String join = strings.format("join-line", Map.of("player", player.getName()));
      if (join != null) {
        if (current.length() > 0) {
          current.append('\n');
        }
        current.append(join);
      }
    }
    List<String> pages = new ArrayList<>();
    appendSection(
        pages,
        current,
        strings.text("board-lanterns"),
        presenceRows(events),
        strings.text("board-lanterns-wait"));
    appendSection(
        pages,
        current,
        strings.text("board-lines"),
        lineRows(events),
        strings.text("board-lines-wait"));
    appendSection(
        pages,
        current,
        strings.text("board-chests"),
        chestRows(events),
        strings.text("board-chests-wait"));
    appendSection(
        pages,
        current,
        strings.text("board-open-marks"),
        markRows(events),
        strings.text("board-marks-wait"));
    if (current.length() > 0) {
      pages.add(current.toString());
    }
    if (pages.isEmpty()) {
      pages.add("");
    }
    return pages;
  }

  private void appendSection(
      List<String> pages,
      StringBuilder current,
      String heading,
      List<String> rows,
      String wait) {
    addLine(pages, current, heading);
    if (rows.isEmpty()) {
      addLine(pages, current, wait);
      return;
    }
    for (String row : rows) {
      addLine(pages, current, row);
    }
  }

  private void addLine(List<String> pages, StringBuilder current, String line) {
    if (line == null || line.isEmpty()) {
      return;
    }
    String rest = line;
    while (!rest.isEmpty()) {
      if (current.length() >= PAGE_LIMIT) {
        pages.add(current.toString());
        current.setLength(0);
      }
      int room = current.length() == 0 ? PAGE_LIMIT : PAGE_LIMIT - current.length() - 1;
      if (room <= 0) {
        pages.add(current.toString());
        current.setLength(0);
        continue;
      }
      if (rest.length() <= room) {
        if (current.length() > 0) {
          current.append('\n');
        }
        current.append(rest);
        return;
      }
      if (current.length() > 0) {
        pages.add(current.toString());
        current.setLength(0);
        continue;
      }
      current.append(rest.substring(0, room));
      pages.add(current.toString());
      current.setLength(0);
      rest = rest.substring(room);
    }
  }

  private List<String> presenceRows(List<LogEvent> events) {
    Map<String, String> states = new LinkedHashMap<>();
    for (LogEvent event : events) {
      if (!"presence".equals(event.type()) || event.player() == null) {
        continue;
      }
      String state = event.get("state");
      if (state != null) {
        states.put(event.player(), state);
      }
    }
    List<String> rows = new ArrayList<>();
    for (Map.Entry<String, String> entry : states.entrySet()) {
      String key = "lit".equals(entry.getValue()) ? "presence-lit" : "presence-unlit";
      String row = strings.format(key, Map.of("player", entry.getKey()));
      if (row != null) {
        rows.add(row);
      }
    }
    return rows;
  }

  private List<String> lineRows(List<LogEvent> events) {
    List<String> rows = new ArrayList<>();
    for (LogEvent event : events) {
      if (!"line".equals(event.type()) || event.player() == null) {
        continue;
      }
      String text = event.get("text");
      if (text == null) {
        text = "";
      }
      String row = strings.format(
          "published-line", Map.of("player", event.player(), "text", text));
      if (row != null) {
        rows.add(row);
      }
    }
    return rows;
  }

  private List<String> chestRows(List<LogEvent> events) {
    List<String> rows = new ArrayList<>();
    for (LogEvent event : events) {
      if (!"chest".equals(event.type()) || event.player() == null) {
        continue;
      }
      Map<String, String> values = new LinkedHashMap<>();
      values.put("player", event.player());
      values.put("direction", orEmpty(event.get("direction")));
      values.put("count", orEmpty(event.get("count")));
      values.put("item", orEmpty(event.get("item")));
      String row = strings.format("chest-row", values);
      if (row != null) {
        rows.add(row);
      }
    }
    return rows;
  }

  private List<String> markRows(List<LogEvent> events) {
    Set<String> confirmed = new HashSet<>();
    for (LogEvent event : events) {
      if ("confirm".equals(event.type()) && event.get("target") != null) {
        confirmed.add(event.get("target"));
      }
    }
    List<String> rows = new ArrayList<>();
    for (LogEvent event : events) {
      if (!"mark".equals(event.type()) || event.player() == null || event.id() == null) {
        continue;
      }
      if (confirmed.contains(event.id())) {
        continue;
      }
      Map<String, String> values = new LinkedHashMap<>();
      values.put("player", event.player());
      values.put("x", orEmpty(event.get("x")));
      values.put("y", orEmpty(event.get("y")));
      values.put("z", orEmpty(event.get("z")));
      values.put("face", orEmpty(event.get("face")));
      String row = strings.format("mark-row", values);
      if (row != null) {
        rows.add(row);
      }
    }
    return rows;
  }

  private boolean holds(Player player, String title) {
    for (ItemStack stack : player.getInventory().getStorageContents()) {
      if (isHourBook(stack, title)) {
        return true;
      }
    }
    return isHourBook(player.getInventory().getItemInOffHand(), title);
  }

  private void removeOlder(Player player, String title) {
    ItemStack[] storage = player.getInventory().getStorageContents();
    boolean changed = false;
    for (int i = 0; i < storage.length; i++) {
      if (isHourBook(storage[i], title)) {
        storage[i] = null;
        changed = true;
      }
    }
    if (changed) {
      player.getInventory().setStorageContents(storage);
    }
    if (isHourBook(player.getInventory().getItemInOffHand(), title)) {
      player.getInventory().setItemInOffHand(null);
    }
  }

  private boolean isHourBook(ItemStack stack, String title) {
    if (stack == null || stack.getType() != Material.WRITTEN_BOOK) {
      return false;
    }
    if (!(stack.getItemMeta() instanceof BookMeta meta)) {
      return false;
    }
    return title.equals(meta.getTitle());
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }
}
