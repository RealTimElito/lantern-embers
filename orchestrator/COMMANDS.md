# Command list

Frozen. Plugin must not invent commands while Access edits strings.

| Input | Events | Who hears it |
| --- | --- | --- |
| `/lantern join <role> <feed>` | join | Role is lookout, builder, quartermaster, or storykeeper. Feed is full, mentions, or summary. Missing args write nothing and reply with both lists. Tab mark is the role and the name. |
| `/lantern feed <mode>` | feed | Changes feed without leaving the hour. |
| `/lantern stop` | stop | The stopping player only. |
| `/lantern offer <draft>` | offer | The offerer only, with the draft-held string. The draft is not chat. |
| `/lantern yes` | confirm, and line if the target is an offer | Joined players, and any online player named by an `@Name` in the text, hear the published line and the summary template. |
| Mark stick click on a block | mark | No chat. |
| Click the confirm lantern | same as `/lantern yes` | Same as yes. |
| Click a stall lantern | presence, and nothing else | No chat. |
| `/embers light` | presence `lit` when the latest state is not already lit | No chat. |
| `/embers dark` | presence `unlit` when the latest state is not already unlit | No chat. |
| `/embers` | none | Opens the board book for that player. |
| Stall chest insert or take | chest | No chat. |

Tab completion lists only: `join`, `stop`, `offer`, `yes` under `/lantern`, and `light`, `dark` under `/embers`.

`/lantern join` also gives one mark stick if the player has none. That is not a separate command.

`/lantern yes` and the confirm lantern confirm the player's latest unconfirmed offer or mark, whichever event is later. One yes confirms one target. If nothing is waiting, say so to that player and write no event.

A mark protects that block as soon as the mark event is written. Another player cannot break it, replace it, or let a piston move it. The player named on the mark can break it. The mark stays in the log.

The confirm lantern is not an Embers lantern. A stall lantern is not a confirm. Lighting never writes confirm, line, mark, or chat.

The summary is the template `{player} added a line.` It is not a model call.

Mentions: `@Name` stays in the text. It is not delivered before confirm.

The board book lists lanterns (lit or unlit), lines, chest rows, and open marks. An open mark is a mark with no confirm whose target is that mark id. Pages are views. They write nothing.

The plugin places no blocks.
