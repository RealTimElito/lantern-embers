package dev.gigglebyte.lanternembers;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Stream;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Two-account transcript. Ada and Bea are test doubles. Item stacks are mocked
 * because Paper ItemStack needs a server.
 */
class RealmTranscriptTest {
  private static final Path TRANSCRIPT = Path.of("../proof/transcript.txt");
  private static final List<String> BANNED =
      List.of(
          "missed",
          "failed",
          "streak",
          "absent",
          "missing",
          "nobody",
          "the ai built this",
          "was not here",
          "empty week",
          "blank week",
          "did not show");

  private final StringBuilder transcript = new StringBuilder();
  private final List<String> productFails = new ArrayList<>();
  private final List<String> adaChat = new ArrayList<>();
  private final List<String> beaChat = new ArrayList<>();
  private final List<String> adaTab = new ArrayList<>();

  private EventLog eventLog;
  private Strings strings;
  private Plots plots;
  private BoardBook boardBook;
  private LanternActions actions;
  private World world;
  private Player ada;
  private Player bea;
  private String bookGiveNote = "";

  @Test
  void twoAccounts() throws Exception {
    try {
      runTranscript();
    } finally {
      Files.createDirectories(TRANSCRIPT.getParent());
      Files.writeString(TRANSCRIPT, transcript.toString(), StandardCharsets.UTF_8);
    }
    if (!productFails.isEmpty()) {
      fail(String.join("\n", productFails));
    }
  }

  private void runTranscript() throws Exception {
    Path folder = Files.createTempDirectory("lantern-embers-proof");
    transcript.append("world folder: ").append(folder).append('\n');
    world = mock(World.class);
    when(world.getWorldFolder()).thenReturn(folder.toFile());
    when(world.getName()).thenReturn("world");
    eventLog = EventLog.open(Logger.getLogger("proof"), world);
    JavaPlugin plugin = pluginWithResources();
    strings = Strings.load(plugin);
    plots = Plots.load(plugin);
    boardBook = new BoardBook(strings, eventLog);
    actions = new LanternActions(eventLog, strings, boardBook);
    primeRegistries();
    ada = player("Ada", adaChat, adaTab);
    bea = player("Bea", beaChat, null);
    stubJoinInventory(ada);

    phase1();
    phase2();
    phase3();
    phase4();
    scanSources();

    transcript.append("\nAda chat:\n");
    adaChat.forEach(line -> transcript.append("  ").append(line).append('\n'));
    transcript.append("Bea chat:\n");
    beaChat.forEach(line -> transcript.append("  ").append(line).append('\n'));
    transcript.append("Ada tab: ").append(adaTab).append('\n');
    transcript.append("book-give: ").append(bookGiveNote).append('\n');
    transcript.append("log:\n");
    for (String line : Files.readAllLines(eventLog.path())) {
      transcript.append(line).append('\n');
    }
  }

  private void phase1() throws Exception {
    transcript.append("\n# Phase 1\n");
    OffsetDateTime joined = OffsetDateTime.now();
    transcript.append("eventLog.join(\"Ada\", ").append(joined).append(")\n");
    eventLog.join("Ada", joined);
    Map<String, String> join = findJoin("Ada");
    check(join != null, "phase 1: join event missing");
    if (join != null) {
      transcript.append("join event: ").append(join).append('\n');
      check("builder".equals(join.get("role")), "phase 1: role");
      check("lantern".equals(join.get("feed")), "phase 1: feed");
      check("Ada".equals(join.get("player")), "phase 1: player");
      String time = join.get("time");
      check(time != null && time.matches(".*[+-]\\d{2}:\\d{2}.*"), "phase 1: offset " + time);
      check(time != null && !time.endsWith("Z"), "phase 1: bare Z " + time);
    }
    String pages = pagesOf(ada);
    transcript.append("pages after eventLog.join:\n").append(pages).append('\n');
    check(pages.contains("Ada joined the hour."), "phase 1: book join line");
    checkBanned(pages, "phase 1 pages");

    int before = eventLog.copy().size();
    transcript.append("LanternActions.join(ada)\n");
    try {
      actions.join(ada);
      bookGiveNote = "join() returned without throwing; book give path ran";
      transcript.append("join() returned\n");
    } catch (Throwable thrown) {
      bookGiveNote = thrown.toString();
      transcript.append("join() threw: ").append(thrown).append('\n');
      Throwable cause = thrown;
      while (cause != null) {
        transcript.append("  at: ").append(cause).append('\n');
        cause = cause.getCause();
      }
      boolean itemStack =
          stackContains(thrown, "ItemStack")
              || stackContains(thrown, "MarkItems")
              || stackContains(thrown, "BoardBook");
      if (!itemStack) {
        productFails.add("phase 1: join() threw outside the item factory: " + thrown);
      }
    }
    check(eventLog.copy().size() > before, "phase 1: join event written before give");
    Map<String, String> after = latestOf("join");
    check(after != null && "Ada".equals(after.get("player")), "phase 1: join still Ada");
    String tab = adaTab.isEmpty() ? "" : adaTab.get(adaTab.size() - 1);
    transcript.append("playerListName: ").append(tab).append('\n');
    check("● Ada".equals(tab), "phase 1: tab prefix " + tab);
    String pagesAfter = pagesOf(ada);
    check(pagesAfter.contains("Ada joined the hour."), "phase 1: reflected pages");
    checkBanned(pagesAfter, "phase 1 pages after join");
  }

  private void phase2() throws Exception {
    transcript.append("\n# Phase 2\n");
    eventLog.mark("Ada", "world", 12, 65, 18, "north");
    transcript.append("eventLog.mark Ada world 12 65 18 north\n");
    Map<String, String> mark = latestOf("mark");
    check(mark != null, "phase 2: mark missing");
    if (mark != null) {
      check("Ada".equals(mark.get("player")), "phase 2: mark player");
      check("world".equals(mark.get("world")), "phase 2: mark world");
      check("12".equals(mark.get("x")), "phase 2: x");
      check("65".equals(mark.get("y")), "phase 2: y");
      check("18".equals(mark.get("z")), "phase 2: z");
      check("north".equals(mark.get("face")), "phase 2: face");
      transcript.append("mark event: ").append(mark).append('\n');
    }
    Block marked = blockAt(12, 65, 18);
    ProtectListener protect = new ProtectListener(eventLog, strings);
    int beaBefore = beaChat.size();
    BlockBreakEvent beaBreak = breakEvent(marked, bea);
    protect.onBreak(beaBreak);
    transcript.append("Bea break cancelled: ").append(beaBreak.isCancelled()).append('\n');
    check(beaBreak.isCancelled(), "phase 2: Bea break cancelled");
    String told = beaChat.size() > beaBefore ? beaChat.get(beaChat.size() - 1) : "";
    transcript.append("Bea protect text: ").append(told).append('\n');
    check("That edge stays with Ada.".equals(told), "phase 2: protect text " + told);

    BlockBreakEvent adaBreak = breakEvent(marked, ada);
    protect.onBreak(adaBreak);
    transcript.append("Ada break cancelled: ").append(adaBreak.isCancelled()).append('\n');
    check(!adaBreak.isCancelled(), "phase 2: Ada break stays open");

    BlockPistonExtendEvent piston = mock(BlockPistonExtendEvent.class);
    when(piston.getBlocks()).thenReturn(List.of(marked));
    trackCancel(piston);
    protect.onPistonExtend(piston);
    transcript.append("piston cancelled: ").append(piston.isCancelled()).append('\n');
    check(piston.isCancelled(), "phase 2: piston cancelled");

    String sign = Files.readString(Path.of("../access/SIGN.md"));
    check(sign.contains("That edge stays with {player}."), "phase 2: SIGN.md protect sentence");
    transcript.append("SIGN.md contains the protect template\n");
  }

  private void phase3() throws Exception {
    transcript.append("\n# Phase 3\n");
    eventLog.join("Bea", OffsetDateTime.now());
    transcript.append("eventLog.join Bea\n");
    int beaBefore = beaChat.size();
    actions.offer(ada, "roof line @Bea");
    transcript.append("offer roof line @Bea\n");
    check(beaChat.subList(beaBefore, beaChat.size()).stream().noneMatch(s -> s.contains("roof line")),
        "phase 3: Bea heard the draft");
    check(adaChat.contains("Draft held."), "phase 3: Ada draft held");
    Map<String, String> offer = latestOf("offer");
    check(offer != null && "roof line @Bea".equals(offer.get("draft")), "phase 3: offer stored");
    transcript.append("offer event: ").append(offer).append('\n');
    String offerId = offer == null ? "" : offer.get("id");

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(ada, bea));
      actions.yes(ada);
    }
    transcript.append("yes\n");
    Map<String, String> confirm = latestOf("confirm");
    Map<String, String> line = latestOf("line");
    transcript.append("confirm: ").append(confirm).append('\n');
    transcript.append("line: ").append(line).append('\n');
    check(confirm != null && "Ada".equals(confirm.get("player")), "phase 3: confirm player");
    check(confirm != null && offerId.equals(confirm.get("target")), "phase 3: confirm target");
    check(line != null && "Ada".equals(line.get("player")), "phase 3: line player");
    check(line != null && "gold".equals(line.get("colour")), "phase 3: line colour");
    check(line != null && "roof line @Bea".equals(line.get("text")), "phase 3: line text");
    check(beaChat.contains("Ada: roof line @Bea"), "phase 3: Bea published line");
    check(beaChat.contains("Ada added a line."), "phase 3: Bea summary");

    int beaAfterYes = beaChat.size();
    actions.offer(ada, "secret beam");
    transcript.append("offer secret beam\n");
    check(
        beaChat.subList(beaAfterYes, beaChat.size()).stream().noneMatch(s -> s.contains("secret beam")),
        "phase 3: secret beam reached Bea");
    check(beaChat.stream().noneMatch(s -> s.contains("secret beam")), "phase 3: Bea ever saw secret beam");
  }

  private void phase4() throws Exception {
    transcript.append("\n# Phase 4\n");
    int sizeBeforeLantern = eventLog.copy().size();
    long nonPresence = nonPresence();
    Block stall = blockAt(12, 66, 22);
    PlayerInteractEvent stallClick = interact(ada, stall, null);
    new LanternInteractListener(plots, actions).onInteract(stallClick);
    List<LogEvent> added = eventLog.copy().subList(sizeBeforeLantern, eventLog.copy().size());
    transcript.append("stall lantern new events: ").append(added.size()).append('\n');
    check(added.size() == 1, "phase 4: stall lantern event count " + added.size());
    if (!added.isEmpty()) {
      LogEvent presence = added.get(0);
      transcript.append("presence: ").append(presence.type()).append(' ')
          .append(presence.player()).append(' ').append(presence.get("state")).append('\n');
      check("presence".equals(presence.type()), "phase 4: type presence");
      check("Ada".equals(presence.player()), "phase 4: presence player");
      check("lit".equals(presence.get("state")), "phase 4: presence lit");
    }
    check(nonPresence() == nonPresence, "phase 4: stall click added a non-presence event");

    long nonPresenceBeforeDark = nonPresence();
    actions.dark(ada);
    Map<String, String> dark = latestOf("presence");
    transcript.append("dark: ").append(dark).append('\n');
    check(dark != null && "unlit".equals(dark.get("state")), "phase 4: dark unlit");
    check("Ada".equals(dark == null ? null : dark.get("player")), "phase 4: dark player");
    check(nonPresence() == nonPresenceBeforeDark, "phase 4: dark grew non-presence events");

    int beforeChest = eventLog.copy().size();
    InventoryClickEvent intoStall = chestClick(ada, 12, 65, 17);
    new ChestListener(eventLog, plots).onClick(intoStall);
    Map<String, String> chest = latestOf("chest");
    transcript.append("chest: ").append(chest).append('\n');
    check(chest != null && "Ada".equals(chest.get("player")), "phase 4: chest player");
    check(chest != null && "OAK_PLANKS".equals(chest.get("item")), "phase 4: chest item");
    check(chest != null && "4".equals(chest.get("count")), "phase 4: chest count");
    check(chest != null && "in".equals(chest.get("direction")), "phase 4: chest direction");

    int afterChest = eventLog.copy().size();
    InventoryClickEvent other = chestClick(ada, 1, 65, 1);
    new ChestListener(eventLog, plots).onClick(other);
    check(eventLog.copy().size() == afterChest, "phase 4: other chest wrote an event");
    transcript.append("other chest events added: ")
        .append(eventLog.copy().size() - afterChest).append('\n');
    check(beforeChest < afterChest, "phase 4: stall chest wrote");

    actions.offer(ada, "porch");
    transcript.append("offer porch\n");
    check(beaChat.stream().noneMatch(s -> s.contains("porch")), "phase 4: porch leaked before yes");
    int presenceBefore = presenceCount();
    Block confirmLantern = blockAt(0, 65, 4);
    PlayerInteractEvent confirmClick = interact(ada, confirmLantern, null);
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(ada, bea));
      new LanternInteractListener(plots, actions).onInteract(confirmClick);
    }
    transcript.append("confirm lantern click\n");
    check(presenceCount() == presenceBefore, "phase 4: confirm lantern wrote presence");
    Map<String, String> porchConfirm = latestOf("confirm");
    Map<String, String> porchLine = latestOf("line");
    transcript.append("porch confirm: ").append(porchConfirm).append('\n');
    transcript.append("porch line: ").append(porchLine).append('\n');
    check(porchConfirm != null && "Ada".equals(porchConfirm.get("player")), "phase 4: porch confirm");
    check(porchLine != null && "porch".equals(porchLine.get("text")), "phase 4: porch text");
    check(beaChat.contains("Ada: porch"), "phase 4: Bea porch line");
    check(beaChat.contains("Ada added a line."), "phase 4: Bea porch summary");

    String pages = pagesOf(ada);
    transcript.append("final pages:\n").append(pages).append('\n');
    check(pages.contains("Ada lantern unlit."), "phase 4: lantern line");
    check(pages.contains("Ada: roof line @Bea"), "phase 4: confirmed roof line");
    check(pages.contains("Ada: porch"), "phase 4: confirmed porch line");
    check(pages.contains("Ada in 4 OAK_PLANKS"), "phase 4: chest row");
    check(pages.contains("Ada 12 65 18 north"), "phase 4: open mark");
    check(!pages.contains("secret beam"), "phase 4: draft on the board");
    checkBanned(pages, "phase 4 pages");
    for (LogEvent event : eventLog.copy()) {
      check(event.player() != null && !event.player().isEmpty(), "event missing player " + event.type());
    }
  }

  private void scanSources() throws Exception {
    transcript.append("\n# Source scan\n");
    List<String> hits = new ArrayList<>();
    Path src = Path.of("src/main/java");
    try (Stream<Path> walk = Files.walk(src)) {
      List<Path> files = walk.filter(path -> path.toString().endsWith(".java")).toList();
      for (Path file : files) {
        String text = Files.readString(file);
        for (String needle : List.of("openai", "http://", "https://", "generate")) {
          if (text.contains(needle)) {
            hits.add(file + " contains " + needle);
          }
        }
      }
    }
    if (hits.isEmpty()) {
      transcript.append("source scan: none\n");
    } else {
      hits.forEach(hit -> transcript.append("source scan hit: ").append(hit).append('\n'));
      productFails.add("phase 3: source scan " + hits);
    }
  }

  /**
   * InventoryType touches Bukkit registries. Paper only installs RegistryAccess on a
   * live server, so the test supplies a stand-in before any item stack is built.
   */
  private void primeRegistries() throws Exception {
    Class<?> holder = Class.forName("io.papermc.paper.registry.RegistryAccessHolder");
    Field instance = holder.getDeclaredField("INSTANCE");
    Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
    Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
    theUnsafe.setAccessible(true);
    Object unsafe = theUnsafe.get(null);
    Object base = unsafeClass.getMethod("staticFieldBase", Field.class).invoke(unsafe, instance);
    long offset = (Long) unsafeClass.getMethod("staticFieldOffset", Field.class).invoke(unsafe, instance);
    unsafeClass.getMethod("putObject", Object.class, long.class, Object.class)
        .invoke(unsafe, base, offset, Optional.of(new StandInAccess()));
    transcript.append("registry stand-in: ").append(InventoryType.CHEST).append('\n');
  }

  private static Object defaultValue(Class<?> type) {
    if (type.equals(boolean.class)) {
      return false;
    }
    if (type.equals(int.class) || type.equals(short.class) || type.equals(byte.class)) {
      return 0;
    }
    if (type.equals(long.class)) {
      return 0L;
    }
    if (type.equals(float.class)) {
      return 0F;
    }
    if (type.equals(double.class)) {
      return 0D;
    }
    if (type.equals(void.class)) {
      return null;
    }
    return null;
  }

  private JavaPlugin pluginWithResources() {
    JavaPlugin plugin = mock(JavaPlugin.class);
    when(plugin.getLogger()).thenReturn(Logger.getLogger("proof"));
    when(plugin.getResource(any(String.class))).thenAnswer(invocation -> {
      String name = invocation.getArgument(0);
      Path path = Path.of("src/main/resources").resolve(name);
      if (!Files.exists(path)) {
        return null;
      }
      return Files.newInputStream(path);
    });
    return plugin;
  }

  private Player player(String name, List<String> chat, List<String> tab) {
    Player player = mock(Player.class);
    when(player.getName()).thenReturn(name);
    doAnswer(invocation -> {
      Component component = invocation.getArgument(0);
      chat.add(PlainTextComponentSerializer.plainText().serialize(component));
      return null;
    }).when(player).sendMessage(any(Component.class));
    if (tab != null) {
      doAnswer(invocation -> {
        Component component = invocation.getArgument(0);
        tab.add(PlainTextComponentSerializer.plainText().serialize(component));
        return null;
      }).when(player).playerListName(any(Component.class));
    }
    return player;
  }

  private void stubJoinInventory(Player player) {
    PlayerInventory inventory = mock(PlayerInventory.class);
    when(player.getInventory()).thenReturn(inventory);
    when(inventory.getStorageContents()).thenReturn(new ItemStack[36]);
    when(inventory.getItemInOffHand()).thenReturn(null);
    lenient().when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
    lenient().doAnswer(invocation -> null).when(player).openBook(any(ItemStack.class));
  }

  private Block blockAt(int x, int y, int z) {
    Block block = mock(Block.class);
    when(block.getWorld()).thenReturn(world);
    when(block.getX()).thenReturn(x);
    when(block.getY()).thenReturn(y);
    when(block.getZ()).thenReturn(z);
    return block;
  }

  private BlockBreakEvent breakEvent(Block block, Player player) {
    try {
      return new BlockBreakEvent(block, player);
    } catch (Throwable thrown) {
      transcript.append("BlockBreakEvent constructor failed: ").append(thrown).append('\n');
      BlockBreakEvent event = mock(BlockBreakEvent.class);
      when(event.getBlock()).thenReturn(block);
      when(event.getPlayer()).thenReturn(player);
      trackCancel(event);
      return event;
    }
  }

  private static void trackCancel(Cancellable event) {
    AtomicBoolean cancelled = new AtomicBoolean(false);
    lenient().doAnswer(invocation -> {
      cancelled.set(invocation.getArgument(0));
      return null;
    }).when(event).setCancelled(anyBoolean());
    lenient().when(event.isCancelled()).thenAnswer(invocation -> cancelled.get());
  }

  private PlayerInteractEvent interact(Player player, Block block, ItemStack item) {
    PlayerInteractEvent event = mock(PlayerInteractEvent.class);
    when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
    when(event.getHand()).thenReturn(EquipmentSlot.HAND);
    when(event.getItem()).thenReturn(item);
    when(event.getClickedBlock()).thenReturn(block);
    when(event.getPlayer()).thenReturn(player);
    lenient().doAnswer(invocation -> null).when(event).setCancelled(anyBoolean());
    lenient().doAnswer(invocation -> null).when(event).setUseInteractedBlock(any());
    lenient().doAnswer(invocation -> null).when(event).setUseItemInHand(any());
    return event;
  }

  private InventoryClickEvent chestClick(Player player, int x, int y, int z) {
    Inventory top = mock(Inventory.class);
    when(top.getType()).thenReturn(InventoryType.CHEST);
    when(top.getSize()).thenReturn(27);
    Location location = mock(Location.class);
    when(location.getWorld()).thenReturn(world);
    when(location.getBlockX()).thenReturn(x);
    when(location.getBlockY()).thenReturn(y);
    when(location.getBlockZ()).thenReturn(z);
    when(top.getLocation()).thenReturn(location);
    ItemStack cursor = mock(ItemStack.class);
    when(cursor.getType()).thenReturn(Material.OAK_PLANKS);
    when(cursor.getAmount()).thenReturn(4);
    when(cursor.getMaxStackSize()).thenReturn(64);
    InventoryView view = mock(InventoryView.class);
    when(view.getTopInventory()).thenReturn(top);
    InventoryClickEvent event = mock(InventoryClickEvent.class);
    when(event.getWhoClicked()).thenReturn(player);
    when(event.getView()).thenReturn(view);
    when(event.getRawSlot()).thenReturn(0);
    when(event.getAction()).thenReturn(InventoryAction.PLACE_ALL);
    when(event.getCurrentItem()).thenReturn(null);
    when(event.getCursor()).thenReturn(cursor);
    return event;
  }

  private String pagesOf(Player player) throws Exception {
    Method pages = BoardBook.class.getDeclaredMethod("pages", Player.class);
    pages.setAccessible(true);
    @SuppressWarnings("unchecked")
    List<String> rendered = (List<String>) pages.invoke(boardBook, player);
    return String.join("\n", rendered);
  }

  private Map<String, String> findJoin(String player) throws Exception {
    Map<String, String> found = null;
    for (String line : Files.readAllLines(eventLog.path())) {
      Map<String, String> values = JsonLines.read(line);
      if (values != null
          && "join".equals(values.get("type"))
          && player.equals(values.get("player"))) {
        found = values;
      }
    }
    return found;
  }

  private Map<String, String> latestOf(String type) {
    Map<String, String> found = null;
    for (LogEvent event : eventLog.copy()) {
      if (type.equals(event.type())) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("id", orEmpty(event.id()));
        values.put("type", orEmpty(event.type()));
        values.put("player", orEmpty(event.player()));
        values.put("role", orEmpty(event.get("role")));
        values.put("feed", orEmpty(event.get("feed")));
        values.put("time", orEmpty(event.get("time")));
        values.put("world", orEmpty(event.get("world")));
        values.put("x", orEmpty(event.get("x")));
        values.put("y", orEmpty(event.get("y")));
        values.put("z", orEmpty(event.get("z")));
        values.put("face", orEmpty(event.get("face")));
        values.put("draft", orEmpty(event.get("draft")));
        values.put("target", orEmpty(event.get("target")));
        values.put("text", orEmpty(event.get("text")));
        values.put("colour", orEmpty(event.get("colour")));
        values.put("state", orEmpty(event.get("state")));
        values.put("item", orEmpty(event.get("item")));
        values.put("count", orEmpty(event.get("count")));
        values.put("direction", orEmpty(event.get("direction")));
        found = values;
      }
    }
    return found;
  }

  private long nonPresence() {
    return eventLog.copy().stream().filter(event -> !"presence".equals(event.type())).count();
  }

  private int presenceCount() {
    int count = 0;
    for (LogEvent event : eventLog.copy()) {
      if ("presence".equals(event.type())) {
        count++;
      }
    }
    return count;
  }

  private void check(boolean condition, String message) {
    if (!condition) {
      productFails.add(message);
      transcript.append("FAIL: ").append(message).append('\n');
    }
  }

  private void checkBanned(String pages, String where) {
    String lower = pages.toLowerCase(Locale.ROOT);
    for (String word : BANNED) {
      if (lower.contains(word)) {
        productFails.add(where + " contains " + word);
        transcript.append("FAIL: ").append(where).append(" contains ").append(word).append('\n');
      }
    }
  }

  private static boolean stackContains(Throwable thrown, String needle) {
    Throwable current = thrown;
    while (current != null) {
      if (String.valueOf(current).contains(needle)) {
        return true;
      }
      for (StackTraceElement frame : current.getStackTrace()) {
        if (frame.getClassName().contains(needle) || frame.toString().contains(needle)) {
          return true;
        }
      }
      current = current.getCause();
    }
    return false;
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }

  /** RegistryAccess stand-in so InventoryType can load without a Paper server. */
  private static final class StandInAccess implements RegistryAccess {
    private Registry<?> registry;

    @Override
    public <T extends Keyed> Registry<T> getRegistry(Class<T> type) {
      return registry();
    }

    @Override
    public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> registryKey) {
      return registry();
    }

    @SuppressWarnings("unchecked")
    private <T extends Keyed> Registry<T> registry() {
      if (registry == null) {
        ClassLoader loader = StandInAccess.class.getClassLoader();
        Class<?> registryType;
        Class<?> menuTyped;
        try {
          registryType = Class.forName("org.bukkit.Registry", false, loader);
          menuTyped = Class.forName("org.bukkit.inventory.MenuType$Typed", false, loader);
        } catch (ClassNotFoundException ex) {
          throw new IllegalStateException(ex);
        }
        Object menu = Proxy.newProxyInstance(loader, new Class<?>[] {menuTyped}, StandInAccess::quiet);
        registry = (Registry<?>) Proxy.newProxyInstance(
            loader,
            new Class<?>[] {registryType},
            (proxy, method, args) -> {
              if ("get".equals(method.getName()) || "getOrThrow".equals(method.getName())) {
                return menu;
              }
              return quiet(proxy, method, args);
            });
      }
      return (Registry<T>) registry;
    }

    private static Object quiet(Object proxy, Method method, Object[] args) {
      if ("equals".equals(method.getName())) {
        return proxy == args[0];
      }
      if ("hashCode".equals(method.getName())) {
        return System.identityHashCode(proxy);
      }
      if ("toString".equals(method.getName())) {
        return "stand-in";
      }
      return defaultValue(method.getReturnType());
    }
  }
}
