const fs = require("fs");
const path = require("path");
const mineflayer = require("mineflayer");

const root = path.resolve(__dirname, "..");
const port = Number(process.env.PORT || 25597);
const logPath = path.join(root, "proof/run/world/lantern-embers/events.jsonl");
const serverLog = path.join(root, "proof/run/logs/latest.log");
const transcriptPath = path.join(root, "proof/transcript.txt");
const failPath = path.join(root, "proof/fail-list.md");

const lines = [];
const fails = [];

function say(text) {
  lines.push(text);
}

function check(ok, message) {
  say((ok ? "pass: " : "FAIL: ") + message);
  if (!ok) {
    fails.push(message);
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function listen(bot, bucket) {
  bot.on("messagestr", (message) => {
    bucket.push(message);
  });
}

function create(username) {
  const bot = mineflayer.createBot({
    host: "127.0.0.1",
    port,
    username,
    auth: "offline",
    version: "1.21.11",
  });
  bot.spawned = waitSpawn(bot);
  return bot;
}

function waitSpawn(bot) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(bot.username + " did not spawn")), 30000);
    bot.once("spawn", () => {
      clearTimeout(timer);
      resolve();
    });
    bot.once("error", (err) => {
      clearTimeout(timer);
      reject(err);
    });
    bot.once("kicked", (reason) => {
      clearTimeout(timer);
      reject(new Error(bot.username + " kicked " + JSON.stringify(reason)));
    });
  });
}

function events() {
  if (!fs.existsSync(logPath)) {
    return [];
  }
  return fs
    .readFileSync(logPath, "utf8")
    .split("\n")
    .filter((line) => line.trim().length > 0)
    .map((line) => JSON.parse(line));
}

function since(before, type) {
  return events().slice(before).filter((event) => event.type === type);
}

async function command(bot, text) {
  bot.chat(text);
  await sleep(400);
}

function itemText(item) {
  return JSON.stringify(item);
}

async function main() {
  say("port: " + port);
  say("version: 1.21.11");
  const ada = create("Ada");
  const bea = create("Bea");
  const adaChat = [];
  const beaChat = [];
  listen(ada, adaChat);
  listen(bea, beaChat);
  await ada.spawned;
  await bea.spawned;
  say("spawned Ada and Bea");
  await sleep(500);

  const logBeforeJoin = fs.existsSync(serverLog) ? fs.readFileSync(serverLog, "utf8") : "";
  await command(ada, "/lantern join lookout full");
  await sleep(500);
  const adaItems = ada.inventory.items().map(itemText).join("\n");
  say("Ada inventory:\n" + adaItems);
  check(adaItems.includes("written_book"), "join gives a book");
  check(adaItems.includes("Mark stick"), "join gives the mark stick");
  check(adaItems.includes("Phrase stick"), "join gives the phrase stick");
  const logAfterJoin = fs.existsSync(serverLog) ? fs.readFileSync(serverLog, "utf8") : "";
  const joinSlice = logAfterJoin.slice(logBeforeJoin.length);
  check(!/Exception|SEVERE/.test(joinSlice), "join writes no exception in the server log");

  await command(bea, "/lantern join builder mentions");
  const beaAt = beaChat.length;
  await command(ada, "bring oak");
  await sleep(400);
  const vanilla = beaChat.slice(beaAt).join("\n");
  say("Bea after vanilla chat:\n" + vanilla);
  check(!vanilla.includes("bring oak"), "Bea on mentions does not see Ada vanilla chat");

  const beforeOffer = events().length;
  const beaBeforeDraft = beaChat.length;
  await command(ada, "/lantern offer roof line @Bea");
  await sleep(300);
  check(
    !beaChat.slice(beaBeforeDraft).join("\n").includes("roof line"),
    "draft stays out of Bea chat"
  );
  check(since(beforeOffer, "offer").some((event) => event.draft === "roof line @Bea"), "offer stored");

  const beaBeforeYes = beaChat.length;
  await command(ada, "/lantern yes");
  await sleep(400);
  const afterYes = beaChat.slice(beaBeforeYes).join("\n");
  say("Bea after yes @Bea:\n" + afterYes);
  check(afterYes.includes("@Bea"), "Bea on mentions sees a line that contains @Bea");
  check(since(beforeOffer, "line").some((event) => (event.text || "").includes("@Bea")), "yes writes the line");

  const beforePhrase = events().length;
  const beaBeforePhrase = beaChat.length;
  const adaBeforePhrase = adaChat.length;
  const phrase = ada.inventory.items().find((item) => itemText(item).includes("Phrase stick"));
  check(Boolean(phrase), "phrase stick is in hand range");
  if (phrase) {
    await ada.equip(phrase, "hand");
    ada.activateItem();
    await sleep(400);
  }
  const phraseOffers = since(beforePhrase, "offer");
  say("phrase offers: " + JSON.stringify(phraseOffers));
  check(phraseOffers.length === 1, "phrase-stick click writes one offer");
  const phraseText = phraseOffers[0] ? phraseOffers[0].draft : "";
  check(
    !beaChat.slice(beaBeforePhrase).join("\n").includes(phraseText || "oak"),
    "phrase-stick click sends no chat to Bea"
  );
  const adaPhraseChat = adaChat.slice(adaBeforePhrase).join("\n");
  say("Ada chat after phrase click:\n" + adaPhraseChat);
  check(adaPhraseChat.includes("Held:"), "phrase stick tells the clicker the held phrase");
  check(!adaPhraseChat.includes("Draft held"), "phrase stick does not announce the draft");

  await command(ada, "/lantern yes");
  await sleep(300);
  check(
    since(beforePhrase, "line").some((event) => event.text === phraseText && event.player === "Ada"),
    "yes after the phrase stick writes the line"
  );

  await command(bea, "/lantern feed summary");
  const beforeSummary = events().length;
  const beaBeforeSummary = beaChat.length;
  await command(ada, "/lantern offer secret spruce");
  await command(ada, "/lantern yes");
  await sleep(400);
  const summaryChat = beaChat.slice(beaBeforeSummary).join("\n");
  say("Bea on summary:\n" + summaryChat);
  check(!summaryChat.includes("secret spruce"), "Bea on summary does not see the draft text");
  check(summaryChat.includes("Ada added a line."), "Bea on summary hears the template");
  check(
    since(beforeSummary, "line").some((event) => event.text === "secret spruce"),
    "summary yes still writes the line in the log"
  );

  const feet = ada.entity.position.floored();
  await command(ada, "/setblock " + (feet.x + 1) + " " + feet.y + " " + feet.z + " oak_planks");
  await command(ada, "/setblock " + feet.x + " " + feet.y + " " + (feet.z + 1) + " oak_planks");
  await sleep(300);
  const markBlock = ada.blockAt(ada.entity.position.offset(1, 0, 0));
  check(Boolean(markBlock) && markBlock.name !== "air", "a block is in reach to mark");
  if (markBlock && markBlock.name !== "air") {
    const markStick = ada.inventory.items().find((item) => itemText(item).includes("Mark stick"));
    await ada.equip(markStick, "hand");
    await ada.lookAt(markBlock.position.offset(0.5, 0.5, 0.5));
    const beforeMark = events().length;
    ada.activateBlock(markBlock);
    await sleep(400);
    const marks = since(beforeMark, "mark");
    say("marks: " + JSON.stringify(marks));
    check(marks.length === 1 && marks[0].player === "Ada", "mark stick writes Ada's mark");

    if (bea.entity.position.distanceTo(ada.entity.position) > 6) {
      bea.chat("/tp Bea Ada");
      await sleep(400);
    }
    const target = bea.blockAt(markBlock.position);
    const nameBefore = target ? target.name : "missing";
    try {
      await bea.dig(target);
    } catch (err) {
      say("Bea dig before yes: " + err.message);
    }
    await sleep(300);
    const nameAfter = bea.blockAt(markBlock.position).name;
    say("block before yes: " + nameBefore + " after: " + nameAfter);
    check(nameAfter === nameBefore, "before yes, Bea the builder cannot break Ada's mark");

    await command(ada, "/lantern yes");
    await sleep(300);
    check(
      since(beforeMark, "confirm").length === 1 && since(beforeMark, "line").length === 0,
      "yes on a mark writes confirm and no line"
    );
    try {
      await bea.dig(bea.blockAt(markBlock.position));
    } catch (err) {
      say("Bea dig after yes: " + err.message);
    }
    await sleep(300);
    const nameBuilt = bea.blockAt(markBlock.position).name;
    say("block after builder yes: " + nameBuilt);
    check(nameBuilt !== nameBefore, "after yes, Bea as builder can break the mark");

    const next = ada.blockAt(ada.entity.position.offset(0, 0, 1));
    if (next && next.name !== "air") {
      await ada.lookAt(next.position.offset(0.5, 0.5, 0.5));
      const beforeSecond = events().length;
      ada.activateBlock(next);
      await sleep(300);
      await command(ada, "/lantern yes");
      await command(bea, "/lantern join lookout mentions");
      await sleep(600);
      const live = bea.blockAt(next.position);
      const lookName = live ? live.name : "missing";
      const beaBeforeDig = beaChat.length;
      try {
        await bea.dig(live);
      } catch (err) {
        say("Bea lookout dig: " + err.message);
      }
      await sleep(800);
      const lookAfter = bea.blockAt(next.position).name;
      const lookChat = beaChat.slice(beaBeforeDig).join("\n");
      say("lookout block " + lookName + " -> " + lookAfter);
      say("Bea chat on lookout dig:\n" + lookChat);
      check(lookAfter === lookName, "Bea as lookout still cannot break a confirmed mark");
      check(since(beforeSecond, "confirm").length === 1, "second mark was confirmed");
    }
  }

  const saved = events();
  say("\nlog:");
  for (const event of saved) {
    say(JSON.stringify(event));
  }
  fs.writeFileSync(transcriptPath, lines.join("\n") + "\n");
  if (fails.length === 0) {
    fs.writeFileSync(failPath, "No product fails in the two-client transcript.\n");
  } else {
    fs.writeFileSync(
      failPath,
      fails.map((fail) => "- " + fail).join("\n") + "\n"
    );
  }
  ada.end();
  bea.end();
  if (fails.length > 0) {
    process.exitCode = 1;
  }
}

main().catch((err) => {
  say("client error: " + err.stack);
  fs.writeFileSync(transcriptPath, lines.join("\n") + "\n");
  fs.writeFileSync(failPath, "- two clients did not finish: " + err.message + "\n");
  process.exit(1);
});
