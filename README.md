# Lantern and Embers

Lantern is the live hour. Embers is the week between hours.

A small whitelist group shares one Minecraft build. The hour is for being there together: mark a block, hold a short line, confirm it. The week is a lantern left on a stall, so the place still shows who was here. A quiet week keeps that lantern. The log names the person who acted. It does not call the week empty, missed, or a streak.

Few clicks still count. The person who confirmed the line is the name on it, even when the path was one stick, one command, or one lantern. Typing a sentence is the long way. A draft stays with the writer until yes, so the group only hears what they chose to publish. A marked block stays with the person who marked it until they confirm it.

The plugin does not place blocks, guess tiredness, keep streaks, or call a model. The summary line is a fixed template over the log. The server never says a model built the place. Later, an AI builder may read the log and propose an outline or a build. A proposal is not placed until a person says yes. Saying no writes nothing. That worker is not in this jar.

This is one Paper plugin and a whitelist realm. It is not a client mod and not a chatbot. The week works in ordinary Minecraft. EyeMine can be used for the hour. The plugin still loads without it, and it does not replace that client.

The hour book and the spawn board both read one append-only log, `world/lantern-embers/events.jsonl`. One log, so the hour and the week are the same story.

The realm is a walk, not a puzzle. Spawn, a board, one confirm lantern, and four stalls with a chest and a lantern each. Paths are flat vanilla blocks, with clear air overhead, so the same place works with a gaze client and without one.

## Build

Java 21. Two jars, from `plugin/`:

```bash
./gradlew jar
./gradlew jar262
```

`jar` is for Paper 1.21.11: `plugin/build/libs/LanternEmbers-1.0.0.jar`. `jar262` is for Paper 26.2: `plugin/build/libs/LanternEmbers-1.0.0-26.2.jar`. Put the jar that matches the server in `plugins/`.

The realm layout is `world/layout.mcfunction`. Coordinates are in `world/README.md`.

## Hour

`/lantern join <role> <feed>` starts the hour. Leave out either word and nothing is written. The player list shows the role and the name.

Roles:

- Lookout marks a block, and can hold a line.
- Builder may break a confirmed mark, while they are in the hour.
- Quartermaster moves items in the stall chests.
- Storykeeper can hold a line.

Feeds, changed with `/lantern feed <mode>`:

- Full hears the published line.
- Mentions hears it only with `@Name`, or as the author. Vanilla chat follows the same rule.
- Summary hears `{player} added a line.` and not the draft.

`/lantern stop` leaves the hour.

Lookout gets a mark stick. A click marks the aimed block. Until yes, only that player can break it. After yes, they can still break it, and so can a builder who is in the hour.

Lookout and storykeeper also get a phrase stick. Each click stores the next fixed phrase, then repeats: oak, spruce, left side, hold, stop here. Only the clicker hears `Held: …`. `/lantern offer <draft>` stores a typed line the same way. Neither stick confirms.

`/lantern yes`, or the lantern at `0, 65, 4`, publishes that player's latest held line, or confirms their latest mark. One yes, one target. If nothing is waiting, nothing is written.

## Week

`/embers` opens the board. `/embers light`, `/embers dark`, and a click on a stall lantern record who set it. That is presence, and nothing else. The board lists lanterns, lines, chests, and open marks.

The lantern by the board confirms the hour. A stall lantern records the week.
