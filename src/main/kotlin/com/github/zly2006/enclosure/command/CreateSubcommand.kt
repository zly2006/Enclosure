package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.Enclosure
import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.LOGGER
import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import com.github.zly2006.enclosure.utils.TrT
import com.github.zly2006.enclosure.utils.plus
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos

fun BuilderScope<*>.registerCreate() {
    literal("create") {
        permission("enclosure.command.create", BuilderScope.Companion.DefaultPermission.TRUE)
        argument("name", StringArgumentType.word()) {
            executes {
                createEnclosure(this)
            }
        }
    }
    literal("auto") {
        permission("enclosure.command.create", BuilderScope.Companion.DefaultPermission.TRUE)
        argument("name", StringArgumentType.word()) {
            executes {
                val pos = BlockPos.ofFloored(source.position)
                val limits = getLimits(this)
                val session = sessionOf(source)
                val expandX = (limits.maxXRange - 1) / 2
                val expandZ = (limits.maxZRange - 1) / 2
                session.enabled = true
                session.world = source.world
                session.pos1 = BlockPos(pos.x - expandX, limits.minY, pos.z - expandZ)
                session.pos2 = BlockPos(
                    pos.x + expandX,
                    limits.maxY.coerceAtMost(limits.maxHeight + limits.minY - 1),
                    pos.z + expandZ
                )
                session.owner = source.uuid
                createEnclosure(this)
            }
        }
    }
    literal("subzone") {
        permission("enclosure.command.subzone", BuilderScope.Companion.DefaultPermission.TRUE)
        argument("name", StringArgumentType.string()) {
            executes {
                val name = StringArgumentType.getString(this, "name")
                if (name.length > ServerMain.commonConfig.maxEnclosureNameLength) {
                    error(TrT.of("enclosure.message.res_name_too_long"), this)
                }
                if (name.chars().anyMatch { c: Int -> !Character.isLetterOrDigit(c) && c != '_'.code }) {
                    error(TrT.of("enclosure.message.res_name_invalid"), this)
                }
                val session = sessionOf(source)
                val list = ServerMain.getAllEnclosures(sessionOf(source).world)
                val area: EnclosureArea = Enclosure(session, name)
                val enclosure = list.areas.firstOrNull { res ->
                    res is Enclosure && res.includesArea(area)
                } as Enclosure? ?: error(TrT.of("enclosure.message.no_father_enclosure"), this)
                if (enclosure.subEnclosures.areas.any { it.name.equals(name, ignoreCase = true) }) {
                    error(TrT.of("enclosure.message.name_in_use"), this)
                }
                if (!enclosure.isOwner(source)) {
                    error(TrT.of("enclosure.message.not_owner"), this)
                }
                val intersectArea = sessionOf(source).intersect(enclosure.subEnclosures)
                if (intersectArea != null) {
                    error(
                        TrT.of("enclosure.message.intersected")
                            .append(intersectArea.serialize(SerializationSettings.Name, source.player)), this
                    )
                }
                val limits = getLimits(this)
                if (!source.hasPermissionLevel(4)) {
                    checkSessionSize(session, this)
                    val count = enclosure.subEnclosures.areas.size.toLong()
                    if (count > limits.maxSubLands) {
                        error(
                            TrT.of("enclosure.message.scle").append(Text.literal(limits.maxSubLands.toString())),
                            this
                        )
                    }
                }
                area.changeWorld(session.world)
                enclosure.addChild(area)
                source.sendMessage(
                    TrT.of("enclosure.message.created")
                        .append(area.serialize(SerializationSettings.Name, source.player))
                )
                LOGGER.info("Created subzone {} by {}", area.fullName, source.name)
            }
        }
    }
}

private fun createEnclosure(context: CommandContext<ServerCommandSource>) {
    val name = StringArgumentType.getString(context, "name")
    if (ServerMain.getEnclosure(name) != null) {
        error(TrT.of("enclosure.message.name_in_use"), context)
    }
    if (!context.source.hasPermissionLevel(4)) {
        if (name.length > ServerMain.commonConfig.maxEnclosureNameLength) {
            error(TrT.of("enclosure.message.res_name_too_long"), context)
        }
    }
    if (name.chars().anyMatch { c: Int -> !Character.isLetterOrDigit(c) && c != '_'.code }) {
        error(TrT.of("enclosure.message.res_name_invalid"), context)
    }
    val session = sessionOf(context.source)
    checkSession(context)
    val list = ServerMain.getAllEnclosures(session.world)
    val intersectArea = sessionOf(context.source).intersect(list)
    if (intersectArea != null) {
        error(TrT.of("enclosure.message.intersected") + intersectArea.serialize(SerializationSettings.Name, context.source.player), context)
    }
    val enclosure = Enclosure(session, name)
    val limits = getLimits(context)
    if (!context.source.hasPermissionLevel(4)) {
        checkSessionSize(session, context)
        if (context.source.player != null) {
            val count = ServerMain.getAllEnclosuresForSuggestion(context.source.uuid).size.toLong()
            if (count >= limits.maxLands) {
                error(
                    TrT.of("enclosure.message.rcle.self",
                        Text.literal(limits.maxLands.toString())
                    ), context
                )
            }
        }
    }
    enclosure.changeWorld(session.world)
    session.reset(session.world)
    list.addArea(enclosure)
    context.source.sendMessage(
        TrT.of("enclosure.message.created")
            .append(enclosure.serialize(SerializationSettings.Name, context.source.player))
    )
    LOGGER.info(context.source.name + " created enclosure " + enclosure.name)
}
