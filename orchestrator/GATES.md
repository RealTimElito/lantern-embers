# Human gates

The orchestrator does not fill these and does not simulate a player. All three are unsigned.

1. After phase 2 proof: a person confirms the mark stick is usable without a keyboard. If it is not, stop. Do not add a model.
2. After phase 3 proof: a person confirms a drafted line cannot leak. The transcript shows Bea does not see a draft before yes, and does not see an unconfirmed second draft. A person still has to confirm that.
3. After phase 4: a person who is not the agent plays one hour and one away-day. Only that person may open shorthand, ghost outlines, or voice.

Later scope stays closed.

To play: start Paper 1.21.11 with Java 21, put `plugin/build/libs/LanternEmbers-1.0.0.jar` in `plugins/`, copy `world/server.properties.snippet` into `server.properties`, whitelist the players, and run `world/layout.mcfunction` once at spawn. Join with `/lantern join`. Mark with the stick. Confirm with `/lantern yes` or the lantern at 0, 65, 4. Between hours, light a stall lantern or use `/embers light`. The board is `/embers`. The log is `world/lantern-embers/events.jsonl`.
