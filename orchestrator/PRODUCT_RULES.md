# Product rules

The separate product document was not in this workspace. These rules are the ones the agent plan states. They are frozen for v1. A worker may not add to them.

Lantern is the live hour. Embers is the week between hours. The artifact is one Paper plugin plus a whitelist realm. It is not a client mod and not a chatbot.

One jar. One append-only event log in the world folder. The build log and the spawn board are views over that log. There is no second source of truth.

Embers must run with no client mod. Lantern may assume EyeMine is already installed, and the plugin must still load without it. Do not fork EyeMine. Do not require it.

Paper API for the 1.21 line (1.21.11). Adventure components for tab and book text. No ProtocolLib. Protection uses Bukkit events. Persistent data is files under the world folder, not a database.

Commands are `/lantern` and `/embers`. Both are thin wrappers over the event log. No LLM call from a command handler. Summary text is a template over events. A model must not append a confirm.

No chat is sent, no block is placed by the plugin, and a stall is not finished, unless a confirm event was written. Lighting an Embers lantern writes presence and nothing else.

Every happy-path action is one confirm: a command, a stick click, or a lantern interact. Typing a sentence is flagged, not added as a new feature.

Out of scope, repeated: no fatigue inference, no auto-build, no diagnosis, no streaks, no EyeMine fork, no second plugin, no shorthand, no ghost outlines, no voice.

## Sentence that must never appear in player-facing text

missed, failed, streak, "the AI built this", and any wording that describes absence (absent, missing, nobody came, was not here, empty week, blank week, did not show).

A dark week still leaves a lantern. The person who acted is the name on the line.
