package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.Enclosure
import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.utils.*
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.DimensionArgumentType
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

fun BuilderScope<*>.registerSelection() {
    literal("select") {
        permission("enclosure.command.select", BuilderScope.Companion.DefaultPermission.TRUE)
        fun <T : argT> BuilderScope<T>.setPos(name: String, action: Session.(BlockPos) -> Unit) {
            literal(name) {
                argument("position", BlockPosArgumentType.blockPos()) {
                    executes {
                        val pos = BlockPosArgumentType.getBlockPos(this, "position")
                        val session = sessionOf(source)
                        session.world = source.world
                        session.action(pos)
                        session.enable()
                        source.sendMessage(TrT.of("enclosure.message.set_$name").append(pos.toShortString()))
                        session.trySync()
                    }
                }
            }
        }
        setPos("pos_1") { pos1 = it }
        setPos("pos_2") { pos2 = it }
        literal("world") {
            argument("world", DimensionArgumentType.dimension()) {
                executes {
                    val world = DimensionArgumentType.getDimensionArgument(this, "world")
                    val session = sessionOf(source)
                    session.world = world
                    source.sendMessage(TrT.of("enclosure.message.selected_world", world.registryKey.value.toString()))
                    session.trySync()
                }
            }
        }
        literal("view") {
            executes {
                val session = sessionOf(source)
                checkSession(this)
                session.trySync()
                val intersectArea = session.intersect(ServerMain.getAllEnclosures(session.world))
                source.sendMessage(
                    TrT.of("enclosure.message.select.from")
                        .append(session.pos1.toShortString())
                        .append(TrT.of("enclosure.message.select.to"))
                        .append(session.pos2.toShortString())
                        .append(TrT.of("enclosure.message.select.world"))
                        .append(session.world.registryKey.value.toString())
                )
                source.sendMessage(
                    TrT.of("enclosure.message.total_size")
                        .append(session.size().toString())
                )
                if (intersectArea != null) {
                    source.sendMessage(
                        TrT.of("enclosure.message.intersected")
                            .append(intersectArea.serialize(SerializationSettings.Name, source.player))
                    )
                }
            }
        }
        literal("clear") {
            executes {
                val session = sessionOf(source)
                session.pos1 = BlockPos.ORIGIN
                session.pos2 = BlockPos.ORIGIN
                session.trySync()
                session.enabled = false
                source.sendMessage(TrT.of("enclosure.message.select.clear"))
            }
        }
        fun <T, A : argT> BuilderScope<A>.withDirectionAndAmount(name: String, action: Session.(Direction, Int) -> T, key: String = "enclosure.message.$name") {
            literal(name) {
                argument("amount", IntegerArgumentType.integer(1)) {
                    executes {
                        val executor = source.player ?: error(TrT.of("enclosure.message.not_player"), this)
                        checkSession(this)
                        val session = sessionOf(source)
                        val amount = IntegerArgumentType.getInteger(this, "amount")
                        val direction = Direction.getEntityFacingOrder(executor)[0]
                        session.action(direction, amount)
                        session.trySync()
                        source.sendMessage(
                            TrT.of(key).append(amount.toString())
                                .append(TrT.of("enclosure.message.resized." + direction.getName()))
                        )
                    }
                }
            }
        }
        withDirectionAndAmount("shrink", Session::shrink, "enclosure.message.shrunk")
        withDirectionAndAmount("expand", Session::expand, "enclosure.message.expanded")
        withDirectionAndAmount("shift", Session::shift, "enclosure.message.shifted")
        literal("max_height") {
            executes {
                val session = sessionOf(source)
                val limits = getLimits(this)
                checkSession(this)
                session.run {
                    pos1 = BlockPos(pos1.x, limits.minY, pos1.z)
                    pos2 = BlockPos(
                        pos2.x,
                        limits.maxY.coerceAtMost(limits.maxHeight + limits.minY - 1),
                        pos2.z
                    )
                }
                session.trySync()
                source.sendMessage(TrT.of("enclosure.message.max_height"))
            }
        }
        literal("max_square") {
            executes {
                val limits = getLimits(this)
                val session = sessionOf(source)
                val (x1, y1, z1, x2, y2, z2) = session.ordered()
                val expandX = (limits.maxXRange - x2 + x1 - 1) / 2
                val expandZ = (limits.maxZRange - z2 + z1 - 1) / 2
                session.pos1 = BlockPos(x1 - expandX, y1, z1 - expandZ)
                session.pos2 = BlockPos(x2 + expandX, y2, z2 + expandZ)
                session.trySync()
                source.sendMessage(TrT.of("enclosure.message.max_square"))
            }
        }
        literal("land") {
            optionalEnclosure {
                val session = sessionOf(source)
                session.world = it.world
                session.pos1 = BlockPos(it.minX, it.minY, it.minZ)
                session.pos2 = BlockPos(it.maxX, it.maxY, it.maxZ)
                session.enable()
                session.trySync()
                source.sendMessage(TrT.of("enclosure.message.selection_updated"))
            }
        }
        literal("resize") {
            optionalEnclosure { area ->
                val session = sessionOf(source)
                checkSession(this)
                checkSessionSize(session, this)
                if (!area.isOwner(source)) {
                    error(TrT.of("enclosure.message.not_owner"), this)
                }
                val after = EnclosureArea(session, "")
                if (area is Enclosure) {
                    area.subEnclosures.areas.firstOrNull { sub -> !after.includesArea(sub) }?.let {
                        error(TrT.of("enclosure.message.sub_enclosure_outside", it.fullName), this)
                    }
                }
                val (minX, minY, minZ, maxX, maxY, maxZ) = session.ordered()
                ConfirmManager.confirm(
                    TrT.of(
                        "enclosure.message.resizing",
                        area.fullName,
                        minX - area.minX,
                        minY - area.minY,
                        minZ - area.minZ,
                        maxX - area.maxX,
                        maxY - area.maxY,
                        maxZ - area.maxZ
                    ),
                    source.player
                ) {
                    area.minX = minX
                    area.minY = minY
                    area.minZ = minZ
                    area.maxX = maxX
                    area.maxY = maxY
                    area.maxZ = maxZ
                    source.sendMessage(
                        TrT.of("enclosure.message.resized")
                            .append(area.serialize(SerializationSettings.Name, source.player))
                    )
                }
            }
        }
    }
}
