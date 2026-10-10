# Command list

This matches the plugin. `/lantern` and `/embers` only append to the event log.

| Input | Events | Who hears it |
| --- | --- | --- |
| `/lantern join <role> <feed>` | join | The joining player gets the hour book. Lookout also gets the mark stick. Lookout and storykeeper also get the phrase stick. Tab mark is `{role} {player}`. |
| `/lantern feed <mode>` | feed | No chat. Feed changes. The player stays in the hour. |
| `/lantern stop` | stop | The stopping player only. |
| `/lantern offer <draft>` | offer | The offerer only, with "Draft held." The draft is not chat. |
| Phrase stick click | offer | The clicker only, with "Held: {text}". Nobody else. |
| `/lantern yes` | confirm, and line when the target is an offer | See feed below. Yes on a mark writes confirm and no line. |
| Confirm lantern click | same as `/lantern yes` | Same as yes. |
| Mark stick click | mark | No chat. Lookout only. |
| Stall lantern click | presence, and nothing else | No chat. |
| `/embers light` | presence `lit` when the latest state is not already lit | No chat. |
| `/embers dark` | presence `unlit` when the latest state is not already unlit | No chat. |
| `/embers` | none | Opens the board book for that player. |
| Stall chest insert or take | chest | No chat. Quartermaster only. |

Role is `lookout`, `builder`, `quartermaster`, or `storykeeper`. Feed is `full`, `mentions`, or `summary`. Missing join args write nothing and reply with both lists.

Tab completion lists `join`, `stop`, `offer`, `yes`, and `feed` under `/lantern`, then the role and feed words. Under `/embers` it lists `light` and `dark`.

`/lantern yes` and the confirm lantern confirm that player's latest unconfirmed offer or mark, whichever is later. One yes, one target. If nothing is waiting, that player is told and no event is written. Lookout may confirm a mark or an offer. Storykeeper may confirm an offer.

Yes respects feed for players in the hour:

| Feed | What they hear |
| --- | --- |
| full | The published line. |
| mentions | The published line only if the text has `@Name` or they wrote it. |
| summary | The template only: `{player} added a line.` |

An online player who is not in the hour hears the published line only when the text names them with `@Name`. Summary never includes the draft. The summary template is not a model call.

Before yes, only the marker can break or replace that block. After yes, the marker can break it, and so can a builder who is in the hour. Other roles cannot. Pistons and explosions still cannot move a mark.

The confirm lantern is not an Embers lantern. A stall lantern is not a confirm. Lighting never writes confirm, line, mark, or chat.

The board lists lanterns, lines, chest rows, and open marks. The held phrase is on the offerer's book only. The plugin places no blocks.
