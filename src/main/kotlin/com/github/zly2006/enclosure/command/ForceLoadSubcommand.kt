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
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.text.Text
import net.minecraft.util.math.ChunkPos

const val MAX_CHUNK_LEVEL = 33 // ChunkLevels.getLevelFromType(ChunkLevelType.FULL)
val FORCED = ChunkTicketType.create<ChunkPos>("enclosure.forced", Comparator.comparingLong { it.toLong() })

fun BuilderScope<*>.registerForceLoad() {
    fun forceLoad(source: ServerCommandSource, area: Enclosure, ticks: Int, level: Int) {
        val ticket = EnclosureArea.ForceLoadTicket(
            source.player?.gameProfile ?: GameProfile(CONSOLE, "Server"),
            if (checkPermission(source, "enclosure.bypass")) Int.MAX_VALUE else ticks,
            level
        )
        area.ticket = ticket
        area.markDirty()
        source.sendFeedback(
            {
                Text.literal("Force loading ")
                    .append(area.serialize(Name, null))
                    .append(" with level $level")
            }, true
        )
    }

    fun cancelForceLoad(source: ServerCommandSource, area: Enclosure) {
        if (area.ticket != null) {
            area.ticket!!.remainingTicks = 0
            area.markDirty()
            source.sendFeedback(
                {
                    Text.literal("Force loading for ")
                        .append(area.serialize(Name, null))
                        .append(" canceled")
                },
                true
            )
        }
        else {
            source.sendError(Text.literal("No force loading tickets for ").append(area.serialize(Name, null)))
        }
    }

    literal("force-load") {
        argument(landArgument()) {
            permission("enclosure.command.force_load")
            literal("blocks") {
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
            literal("cancel") {
                executes {
                    val land = getEnclosure(this)
                    if (land !is Enclosure) {
                        error(Text.literal("Only enclosure can be force loaded"), this)
                    }
                    cancelForceLoad(source, land)
                }
            }
        }
        literal("list") {
            executes {
                TODO()
            }
        }
    }
}
