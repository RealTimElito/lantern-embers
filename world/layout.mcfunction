# Whitelist realm layout. Vanilla blocks only. Coordinates match orchestrator/plots.yml.

# Spawn path: 7 blocks wide in x, smooth stone under the feet at y=65.
fill -3 64 -2 3 64 15 minecraft:smooth_stone
# Front path: 7 blocks wide in z, from spawn to the east end of stall 4.
fill -3 64 9 76 64 15 minecraft:smooth_stone
# Flat paths between stalls. Each gap is 11 blocks wide.
fill 17 64 16 27 64 24 minecraft:smooth_stone
fill 37 64 16 47 64 24 minecraft:smooth_stone
fill 57 64 16 67 64 24 minecraft:smooth_stone

# Stall floors at y=64, each plot min through max.
fill 8 64 16 16 64 24 minecraft:oak_planks
fill 28 64 16 36 64 24 minecraft:oak_planks
fill 48 64 16 56 64 24 minecraft:oak_planks
fill 68 64 16 76 64 24 minecraft:oak_planks

# Open air over the paths and stalls so a gaze path stays clear through y=72.
fill -3 65 -2 76 72 24 minecraft:air

# Stall chests face north, toward spawn at z=0.
setblock 12 65 17 minecraft:chest[facing=north,type=single]
setblock 32 65 17 minecraft:chest[facing=north,type=single]
setblock 52 65 17 minecraft:chest[facing=north,type=single]
setblock 72 65 17 minecraft:chest[facing=north,type=single]

# Oak fence posts, then stall lanterns sitting on those posts.
setblock 12 65 22 minecraft:oak_fence
setblock 32 65 22 minecraft:oak_fence
setblock 52 65 22 minecraft:oak_fence
setblock 72 65 22 minecraft:oak_fence
setblock 12 66 22 minecraft:lantern[hanging=false]
setblock 32 66 22 minecraft:lantern[hanging=false]
setblock 52 66 22 minecraft:lantern[hanging=false]
setblock 72 66 22 minecraft:lantern[hanging=false]

# Confirm lantern sits on the smooth stone at the board.
setblock 0 65 4 minecraft:lantern[hanging=false]

# Board lectern, facing north. Placed before the book merge.
setblock 0 65 3 minecraft:lectern[facing=north]
data merge block 0 65 3 {Book:{id:"minecraft:written_book",count:1,components:{"minecraft:written_book_content":{pages:["Lantern is the live hour. Embers is the week between hours. Join with one command. Mark an edge with the stick. Yes with one command or the lantern by the board. Light a stall lantern to leave it lit."],title:"Hour book",author:"Lantern",generation:0,resolved:true}}},Page:0}
setblock 0 65 3 minecraft:lectern[facing=north,has_book=true]{Book:{id:"minecraft:written_book",count:1,components:{"minecraft:written_book_content":{pages:["Lantern is the live hour. Embers is the week between hours. Join with one command. Mark an edge with the stick. Yes with one command or the lantern by the board. Light a stall lantern to leave it lit."],title:"Hour book",author:"Lantern",generation:0,resolved:true}}},Page:0}
