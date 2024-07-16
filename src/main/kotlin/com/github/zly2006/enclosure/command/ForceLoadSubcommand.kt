package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.utils.checkPermission
import me.lucko.fabric.api.permissions.v0.Options
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ChunkLevelType
import net.minecraft.server.world.ChunkLevels
import net.minecraft.server.world.ChunkTicketType

fun BuilderScope<*>.registerForceLoad() {
    fun forceLoad(source: ServerCommandSource, area: EnclosureArea, ticks: Int, level: Int) {
        if (checkPermission(source, "enclosure.bypass")) {

        }
    }

    literal("force-load") {
        argument(landArgument()) {
            literal("blocks") {
                permission("enclosure.command.force_load")
                val level = ChunkLevels.getLevelFromType(ChunkLevelType.BLOCK_TICKING)
                executes {
                    val maxTime = Options.get(source, "enclosure.load.max_time", 21600) { it.toInt() }
                    val land = getEnclosure(this)
                    ChunkTicketType.FORCED
                    forceLoad(source, land, maxTime * 20, level)
                }
            }
            literal("entities") {
                permission("enclosure.command.force_load")
                val level = ChunkLevels.getLevelFromType(ChunkLevelType.ENTITY_TICKING)
                executes {
                    val maxTime = Options.get(source, "enclosure.load.max_time", 21600) { it.toInt() }
                    val land = getEnclosure(this)
                    forceLoad(source, land, maxTime * 20, level)
                }
            }
        }
    }
}
