# Lantern and Embers

Lantern is the live hour. Embers is the week between hours.

A small group shares one Minecraft build. During the hour they mark an edge, hold a line, and confirm it. During the week they leave a lantern lit. The person who acted is the name on the line. A dark week still leaves that lantern.

This is one Paper plugin and a whitelist realm. It is not a client mod and not a chatbot. Embers runs with no client mod. The plugin still loads if EyeMine is not installed.

The happy path is one confirm: a command, a stick click, or a lantern. A draft is not chat until yes. The plugin does not place blocks, infer fatigue, keep streaks, or call a model. Summary text is a template over the log.

The hour book and the spawn board are views of one append-only log, `world/lantern-embers/events.jsonl`.

Later, an AI builder may read that log and propose a ghost outline or a build. A proposal is not placed, and it is not a confirm, until the next person says yes. A decline writes nothing. That worker is not in this jar.

## Build

Java 21 and Paper 1.21.11. A second jar targets Paper 26.2.

```bash
cd plugin
./gradlew jar
./gradlew jar262
```

The 1.21 jar is `plugin/build/libs/LanternEmbers-1.0.0.jar`. The 26.2 jar is `plugin/build/libs/LanternEmbers-1.0.0-26.2.jar`. The realm layout is `world/layout.mcfunction`. Coordinates are in `world/README.md`.

## Hour

`/lantern join <role> <feed>` starts the hour. Missing words write nothing.

Roles: lookout, builder, quartermaster, storykeeper. The tab mark is the role and the name.

Feeds: full, mentions, summary.

- Full hears the published line.
- Mentions hears it only with `@Name`, or as the author. Vanilla chat follows the same rule.
- Summary hears `{player} added a line.` and not the draft.

`/lantern feed <mode>` changes the feed and stays in the hour. `/lantern stop` leaves it.

Lookout marks an edge with the mark stick. Until yes, only they can break it. After yes, they can still break it, and so can a builder who is in the hour.

Lookout and storykeeper also get a phrase stick. Each click holds the next fixed phrase: oak, spruce, left side, hold, stop here. Only the clicker hears `Held: …`. `/lantern offer <draft>` is the typed path. `/lantern yes`, or the lantern at `0, 65, 4`, publishes the latest held phrase or confirms the latest mark.

## Week

`/embers` opens the board. `/embers light` and `/embers dark` record a lantern, and so does a stall lantern. That writes presence and nothing else. The quartermaster moves items in the stall chests. The board lists lanterns, lines, chests, and open marks.
