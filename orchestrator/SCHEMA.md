# Event schema

Frozen until a human opens it. Append-only JSON lines. One object per line. No deletes, no rewrites.

Path: `<world-container>/<world>/lantern-embers/events.jsonl` where `world` is `world`.

Time fields use the server's local offset, ISO-8601, not UTC-converted client stamps.

Every event has `id` (UUID string) and `type`. Every event has `player` (the acting player's name). A decision with no player name is a failed build.

| type | fields | who may write it |
| --- | --- | --- |
| join | player, role, feed, time | Plugin, after `/lantern join <role> <feed>` only |
| feed | player, role, feed, time | Plugin, after `/lantern feed <mode>` |
| mark | player, world, x, y, z, face | Plugin, after the mark stick clicks a block face |
| offer | player, draft | Plugin, never sent to anyone else |
| confirm | player, target | Plugin, only `/lantern yes` or a click on the confirm lantern |
| stop | player, time | Plugin, after `/lantern stop` |
| presence | player, state | Plugin, Embers lantern or `/embers light` or `/embers dark` |
| line | player, text, colour | Plugin, only after confirm of an offer |
| chest | player, item, count, direction | Plugin, stall chest only |

`role` is `lookout`, `builder`, `quartermaster`, or `storykeeper`. `feed` is `full`, `mentions`, or `summary`. An old join with role `builder` and feed `lantern` still reads: builder, and full. New fields are optional on old lines. `feed` events do not leave the hour.

`face` is `up`, `down`, `north`, `south`, `east`, or `west`.

`state` is `lit` or `unlit`.

`colour` is `gold` on every line in v1. The player does not pick a colour.

`direction` is `in` or `out`.

`target` is the `id` of an offer or a mark.

`x`, `y`, `z` are integers. `count` is an integer. `item` is the Bukkit material name.

Example:

```json
{"id":"…","type":"join","player":"Ada","role":"builder","feed":"lantern","time":"2026-10-09T20:40:00+02:00"}
```
