# Whitelist realm

This realm is whitelist-only. Copy the keys in `server.properties.snippet` into the local proof server's `server.properties`. The snippet does not launch a server.

Players stand with their feet at y=65 on blocks at y=64. Paths are smooth stone. Stall floors are oak planks. Air from y=65 through y=72 is clear over the paths and stalls, then the anchors below are placed.

## Anchors

| Anchor | Position | Block | Facing |
| --- | --- | --- | --- |
| Spawn | 0, 65, 0 (floor 0, 64, 0) | smooth_stone | — |
| Board lectern | 0, 65, 3 | lectern | north |
| Confirm lantern | 0, 65, 4 | lantern (hanging=false) | — |
| Stall 1 chest | 12, 65, 17 | chest | north, toward spawn at z=0 |
| Stall 1 post | 12, 65, 22 | oak_fence | — |
| Stall 1 lantern | 12, 66, 22 | lantern (hanging=false) | — |
| Stall 2 chest | 32, 65, 17 | chest | north, toward spawn at z=0 |
| Stall 2 post | 32, 65, 22 | oak_fence | — |
| Stall 2 lantern | 32, 66, 22 | lantern (hanging=false) | — |
| Stall 3 chest | 52, 65, 17 | chest | north, toward spawn at z=0 |
| Stall 3 post | 52, 65, 22 | oak_fence | — |
| Stall 3 lantern | 52, 66, 22 | lantern (hanging=false) | — |
| Stall 4 chest | 72, 65, 17 | chest | north, toward spawn at z=0 |
| Stall 4 post | 72, 65, 22 | oak_fence | — |
| Stall 4 lantern | 72, 66, 22 | lantern (hanging=false) | — |

Chests are single chests. Each stall lantern sits on the oak fence directly under it. The confirm lantern sits on smooth stone. It is not on a fence.

## Floors and paths

Stall floors, oak planks at y=64, covering each plot from min through max:

- Stall 1: x=8 to 16, z=16 to 24
- Stall 2: x=28 to 36, z=16 to 24
- Stall 3: x=48 to 56, z=16 to 24
- Stall 4: x=68 to 76, z=16 to 24

Smooth stone, all at y=64:

- Spawn path, 7 blocks wide: x=-3 to 3, z=-2 to 15
- Front path, 7 blocks wide: x=-3 to 76, z=9 to 15
- Between stall 1 and 2, 11 blocks wide: x=17 to 27, z=16 to 24
- Between stall 2 and 3, 11 blocks wide: x=37 to 47, z=16 to 24
- Between stall 3 and 4, 11 blocks wide: x=57 to 67, z=16 to 24

The lectern at 0, 65, 3 and the confirm lantern at 0, 65, 4 stand on the middle of the spawn path. Walk around them on the same 7-block path (x=-3 to -1, or x=1 to 3).

## Walk from spawn (0, 65, 0)

Every stall starts the same way. Walk south on the spawn path to the front path (z=9 to 15). Stay on that front path and walk east until your x matches the stall. Then walk south onto the oak floor.

- Stall 1. East to x=12, then south to the chest at 12, 65, 17. Continue south on the oak floor to the lantern at 12, 66, 22.
- Stall 2. East to x=32, then south to the chest at 32, 65, 17. Continue south to the lantern at 32, 66, 22. The smooth stone from x=17 to 27 is the flat path between stall 1 and stall 2.
- Stall 3. East to x=52, then south to the chest at 52, 65, 17. Continue south to the lantern at 52, 66, 22. The smooth stone from x=37 to 47 is the flat path between stall 2 and stall 3.
- Stall 4. East to x=72, then south to the chest at 72, 65, 17. Continue south to the lantern at 72, 66, 22. The smooth stone from x=57 to 67 is the flat path between stall 3 and stall 4.

## Board book

`layout.mcfunction` places the lectern first. It then runs `data merge block` to put a written book in the lectern, using the 1.21.11 `minecraft:written_book_content` item component. The page is one plain string, which that format accepts as a text component. Title is `Hour book`. Author is `Lantern`. The page text is only: "Lantern is the live hour. Embers is the week between hours. Join with one command. Mark an edge with the stick. Yes with one command or the lantern by the board. Light a stall lantern to leave it lit."

A following `setblock` sets `has_book=true` and repeats that same book, so the lectern state and the book stay together when the component command is accepted. If a server rejects the merge or that `setblock`, the lectern from the first command is still at 0, 65, 3, facing north.
