package com.github.zly2006.enclosure.mixinadatper

import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.utils.Permission
import net.minecraft.block.piston.PistonHandler
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.*
import kotlin.jvm.optionals.getOrNull

fun protectPiston(
    world: ServerWorld,
    pistonPos: BlockPos,
    dir: Direction, // facing
    extend: Boolean,
    cir: CallbackInfoReturnable<Boolean?>,
    newPos: BlockPos,
    pistonHandler: PistonHandler
) {
    var positions = pistonHandler.brokenBlocks + pistonHandler.movedBlocks + pistonPos
    positions = positions + positions.map { it.offset(if (extend) dir else dir.opposite) }
    positions = positions.distinct()
    val areas = positions.map { ServerMain.getSmallestEnclosure(world, it) }
        .map { Optional.ofNullable(it) } // null is also considered as an "area"
        .distinct()
    val cancel = areas.size > 1 && areas.mapNotNull { it.getOrNull()?.hasPubPerm(Permission.PISTON)?.not() }.any { it }
    if (cancel) {
        // this method will be called even the target pos is out of the world.
        positions.forEach(world.chunkManager::markForUpdate)
        cir.returnValue = false
    }
}
