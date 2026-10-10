# Lantern and Embers

Lantern is the live hour. Embers is the week between hours. One Paper 1.21.11 plugin writes one append-only log. A draft is not chat until yes.

Build with `cd plugin && ./gradlew jar`. The jar is `plugin/build/libs/LanternEmbers-1.0.0.jar`. Java 21.

`/lantern join <role> <feed>` starts the hour. Roles: lookout, builder, quartermaster, storykeeper. Feeds: full, mentions, summary. Missing words write nothing.

`/lantern feed <mode>` changes the feed and stays in the hour.

`/lantern offer <draft>` holds a typed line. That path is for a sentence. Lookout and storykeeper also get a phrase stick. Each click holds the next fixed phrase: oak, spruce, left side, hold, stop here.

`/lantern yes` confirms the latest held phrase or mark. The lantern at 0, 65, 4 does the same.

Full hears the published line. Mentions hears it only with `@Name` or as the author, and vanilla chat follows that rule. Summary hears `{player} added a line.` and not the draft.

`/embers` opens the board. `/embers light` and `/embers dark` record a lantern, and so does a stall lantern.

The log is `world/lantern-embers/events.jsonl`.
