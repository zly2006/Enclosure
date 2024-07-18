package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.Enclosure
import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings.Name
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings.NameHover
import com.github.zly2006.enclosure.utils.checkPermission
import com.mojang.authlib.GameProfile
import com.mojang.brigadier.arguments.StringArgumentType
import me.lucko.fabric.api.permissions.v0.Options
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ChunkLevelType
import net.minecraft.server.world.ChunkLevels
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.text.Text
import net.minecraft.util.math.ChunkPos

const val MAX_CHUNK_LEVEL = 33 // ChunkLevels.getLevelFromType(ChunkLevelType.FULL)
val FORCED = ChunkTicketType.create<ChunkPos>("enclosure.forced", Comparator.comparingLong { it.toLong() })!!
val timeRegex = Regex("""^(\d{1,3}h)?(\d{1,2}m)?(\d{1,2}s)?(\d{1,2}gt)?$""")

fun BuilderScope<*>.registerForceLoad() {
    fun forceLoad(source: ServerCommandSource, area: Enclosure, ticks: Int, level: Int) {
        val maxTime = Options.get(source, "enclosure.load.max_time", 21600) { it.toInt() }
        val ticket = EnclosureArea.ForceLoadTicket(
            source.player?.gameProfile ?: GameProfile(CONSOLE, "Server"), ticks, level
        )
        if (!checkPermission(source, "enclosure.bypass")) {
            val totalTicksRemaining = ServerMain.getAllEnclosures()
                .filter { it.ticket?.executor?.id == source.uuid }
                .sumOf { it.ticket!!.remainingTicks }
            if (totalTicksRemaining > maxTime * 20) {
                source.sendError(
                    Text.literal(
                        "You have reached the maximum force loading time: " + getTimeString(
                            maxTime
                        )
                    )
                )
                return
            }
            if (totalTicksRemaining + ticks > maxTime * 20) {
                ticket.remainingTicks = maxTime * 20 - totalTicksRemaining
                source.sendError(
                    Text.literal(
                        "You can only force load for ${getTimeString(maxTime * 20)}. " +
                                "The remaining time is set to ${getTimeString(ticket.remainingTicks)}"
                    )
                )
                return
            }
        }
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
            if (!checkPermission(source, "enclosure.bypass")) {
                if (source.uuid != area.ticket!!.executor.id) {
                    source.sendError(Text.literal("You can only cancel your own force loading tickets"))
                    return
                }
            }
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

    fun BuilderScope<*>.registerCommands(level: Int) {
        executes {
            val maxTime =
                if (checkPermission(source, "enclosure.bypass")) Int.MAX_VALUE
                else Options.get(source, "enclosure.load.max_time", 21600) { it.toInt() }
            val land = getEnclosure(this)
            if (land !is Enclosure) {
                error(Text.literal("Only enclosure can be force loaded"), this)
            }
            forceLoad(source, land, maxTime * 20, level)
        }
        literal("--time") {
            argument("time", StringArgumentType.word()) {
                executes {
                    val timeString = getArgument("time", String::class.java)
                    timeRegex.matchEntire(timeString)?.let {
                        val hours = it.groups[1]?.value?.dropLast(1)?.toIntOrNull() ?: 0
                        val minutes = it.groups[2]?.value?.dropLast(1)?.toIntOrNull() ?: 0
                        val seconds = it.groups[3]?.value?.dropLast(1)?.toIntOrNull() ?: 0
                        val gameTicks = it.groups[4]?.value?.dropLast(2)?.toIntOrNull() ?: 0
                        val ticks = hours * 72000 + minutes * 1200 + seconds * 20 + gameTicks
                        if (ticks > 0) {
                            val land = getEnclosure(this)
                            if (land !is Enclosure) {
                                error(Text.literal("Only enclosure can be force loaded"), this)
                            }
                            forceLoad(source, land, ticks, level)
                        }
                        else {
                            error(Text.literal("Time must be positive"), this)
                        }
                    } ?: error(Text.literal("Invalid time format, must be 12h34m56s78gt"), this)
                }
            }
        }
    }

    literal("force-load") {
        argument(landArgument()) {
            permission("enclosure.command.force_load")
            literal("blocks") {
                registerCommands(ChunkLevels.getLevelFromType(ChunkLevelType.BLOCK_TICKING))
            }
            literal("entities") {
                registerCommands(ChunkLevels.getLevelFromType(ChunkLevelType.ENTITY_TICKING))
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
                var has = false
                for (enclosure in ServerMain.getAllEnclosures()) {
                    if (enclosure.ticket != null) {
                        has = true
                        val ticks = enclosure.ticket!!.remainingTicks
                        source.sendFeedback(
                            {
                                Text.literal("Force loading ")
                                    .append(enclosure.serialize(NameHover, null))
                                    .append(" with level ${enclosure.ticket!!.level}")
                                    .append(" for ")
                                    .append(getTimeString(ticks))
                                    .append(" by ")
                                    .append(
                                        source.server.playerManager.getPlayer(enclosure.ticket!!.executor.id)?.styledDisplayName
                                            ?: Text.literal(enclosure.ticket!!.executor.name)
                                    )
                            },
                            false
                        )
                    }
                }
                if (!has) {
                    source.sendFeedback({ Text.literal("No force loading tickets") }, false)
                }
            }
        }
    }
}

private fun getTimeString(ticks: Int) = buildString {
    val seconds = ticks / 20
    val minutes = seconds / 60
    val hours = minutes / 60
    if (hours != 0) append(hours).append("h")
    if (minutes % 60 != 0) append(minutes % 60).append("m")
    if (seconds % 60 != 0) append(seconds % 60).append("s")
    if (ticks % 20 != 0) append(ticks % 20).append("gt")
}
