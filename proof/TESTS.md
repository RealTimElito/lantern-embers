# Proof tests

Written before plugin code. Proof runs these. A phase is not done because the jar compiles.

Two accounts: Ada and Bea. Transcript path after the run: `proof/transcript.txt`. Fail list: `proof/fail-list.md`.

## Phase 1

1. Plugin enables on Paper 1.21.11.
2. Ada runs `/lantern join`.
3. The log gains one join event with player Ada, role builder, feed lantern, and a time that has a timezone offset.
4. Ada's tab name starts with the tab prefix.
5. The hour book Ada receives contains `Ada joined the hour.`

## Phase 2

1. Ada joins, then clicks the mark stick on the north face of block (12, 65, 18) in world.
2. The log gains a mark with that player, world, x, y, z, and face.
3. Bea breaks that block. The break is cancelled. Bea is told the protect string, which names Ada.
4. Ada can break that block.
5. A piston that would move the block is cancelled.
6. Player-facing protect text has been signed in `access/SIGN.md`.

## Phase 3

1. Ada joins and Bea joins.
2. Ada runs `/lantern offer roof line @Bea`.
3. An offer event is stored. Bea's chat does not contain `roof line`. Ada's chat does not broadcast it. Ada sees `Draft held.`
4. Ada runs `/lantern yes`.
5. A confirm event targets that offer id and names Ada. A line event names Ada, keeps the text, colour gold.
6. Bea then sees the published line, including `@Bea`, and the summary `Ada added a line.`
7. The summary is that template. No model call exists on the path.
8. A second offer that is never confirmed still does not appear in Bea's chat.

## Phase 4

1. Ada right-clicks stall 1 lantern at (12, 66, 22). The only new event is presence lit for Ada.
2. `/embers dark` writes presence unlit and nothing else.
3. Ada puts 4 oak planks into the stall 1 chest at (12, 65, 17). A chest event records player, item, count, direction in. A chest at another coordinate writes nothing.
4. The board book lists the lantern state, the confirmed line, the chest row, and any mark with no confirm. It does not describe absence.
5. Confirm lantern click confirms a pending offer the same way `/lantern yes` does, and does not write presence.

## Negatives that fail the build

- A confirm, line, mark, or chest with no player name.
- Bea moves a marked edge.
- A summary that describes absence.
- Draft text in the other account before yes.
- Plugin places a block.
- Any player-facing string from the banned list in the product rules.
