package com.github.zly2006.enclosure.utils

import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.command.BuilderScope
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.entity.Entity
import net.minecraft.entity.vehicle.HopperMinecartEntity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.*
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

fun MutableText.hoverText(text: Text): MutableText {
    return this.styled { it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, text)) }
}

fun Style.hoverText(text: Text): Style {
    return this.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, text))
}

fun Style.clickRun(command: String): Style {
    return this.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
}

operator fun MutableText.plusAssign(text: Text) {
    this.append(text)
}

operator fun Text.plus(text: Text): MutableText {
    return this.copy().append(text)
}

operator fun Text.plus(text: String): MutableText {
    return this.copy().append(text)
}

fun MutableText.green(): MutableText {
    return this.formatted(Formatting.GREEN)
}

fun MutableText.darkGreen(): MutableText {
    return this.formatted(Formatting.DARK_GREEN)
}

fun MutableText.gold(): MutableText {
    return this.formatted(Formatting.GOLD)
}

fun MutableText.red(): MutableText {
    return this.formatted(Formatting.RED)
}

fun MutableText.white(): MutableText {
    return this.formatted(Formatting.WHITE)
}

fun literalText(text: Any): MutableText {
    return when (text) {
        is Text -> text.copy()
        is String -> Text.literal(text)
        else -> Text.literal(text.toString())
    }
}

fun ServerWorld.mark4updateChecked(pos: BlockPos): Boolean {
    if (worldBorder.contains(pos) && pos.y >= bottomY && pos.y < bottomY + height) {
        chunkManager.markForUpdate(pos)
        return true
    }
    return false
}

fun ServerWorld.getEnclosure(pos: BlockPos): EnclosureArea? = ServerMain.getSmallestEnclosure(this, pos)

fun Box.contains(box: Box) =
    this.minX <= box.minX && this.minY <= box.minY && this.minZ <= box.minZ
            && this.maxX >= box.maxX && this.maxY >= box.maxY && this.maxZ >= box.maxZ

fun isCrossBorderRestricted(entity: Entity): Boolean {
    return entity is HopperMinecartEntity
}

operator fun <E> List<E>.component1(): E = this[0]
operator fun <E> List<E>.component2(): E = this[1]
operator fun <E> List<E>.component3(): E = this[2]
operator fun <E> List<E>.component4(): E = this[3]
operator fun <E> List<E>.component5(): E = this[4]
operator fun <E> List<E>.component6(): E = this[5]

fun formatSelection(
    worldId: Identifier,
    x1: Int, y1: Int, z1: Int,
    x2: Int, y2: Int, z2: Int
): MutableText {
    val fromStr = TrT.of("enclosure.message.select.coordinate",
        literalText(x1).green(),
        literalText(y1).green(),
        literalText(z1).green()
    )
    val toStr = TrT.of("enclosure.message.select.coordinate",
        literalText(x2).green(),
        literalText(y2).green(),
        literalText(z2).green()
    )
    return TrT.of("enclosure.message.selection",
        fromStr,
        toStr,
        literalText(worldId).green()
    )
}

fun checkPermission(player: ServerPlayerEntity, perm: String) = checkPermission(player.commandSource, perm)

fun checkPermission(source: ServerCommandSource, perm: String) =
    Permissions.check(source, perm, BuilderScope.map[perm]?.get(source) ?: false)
