package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.Enclosure
import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings.Name
import com.github.zly2006.enclosure.utils.checkPermission
import com.mojang.authlib.GameProfile
import me.lucko.fabric.api.permissions.v0.Options
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ChunkLevelType
import net.minecraft.server.world.ChunkLevels
import net.minecraft.text.Text

fun BuilderScope<*>.registerForceLoad() {
    fun forceLoad(source: ServerCommandSource, area: Enclosure, ticks: Int, level: Int) {
        val ticket = EnclosureArea.ForceLoadTicket(
            source.player?.gameProfile ?: GameProfile(CONSOLE, "Server"),
            if (checkPermission(source, "enclosure.bypass")) Int.MAX_VALUE else ticks,
            level
        )
        area.ticket = ticket
        area.markDirty()
        source.sendMessage(
            Text.literal("Force loading ")
                .append(area.serialize(Name, null))
                .append(" with level $level")
        )
    }

    literal("force-load") {
        argument(landArgument()) {
            literal("blocks") {
                permission("enclosure.command.force_load")
                val level = ChunkLevels.getLevelFromType(ChunkLevelType.BLOCK_TICKING)
                executes {
                    val maxTime = Options.get(source, "enclosure.load.max_time", 21600) { it.toInt() }
                    val land = getEnclosure(this)
                    if (land !is Enclosure) {
                        error(Text.literal("Only enclosure can be force loaded"), this)
                    }
                    forceLoad(source, land, maxTime * 20, level)
                }
            }
            literal("entities") {
                permission("enclosure.command.force_load")
                val level = ChunkLevels.getLevelFromType(ChunkLevelType.ENTITY_TICKING)
                executes {
                    val maxTime = Options.get(source, "enclosure.load.max_time", 21600) { it.toInt() }
                    val land = getEnclosure(this)
                    if (land !is Enclosure) {
                        error(Text.literal("Only enclosure can be force loaded"), this)
                    }
                    forceLoad(source, land, maxTime * 20, level)
                }
            }
        }
    }
}
