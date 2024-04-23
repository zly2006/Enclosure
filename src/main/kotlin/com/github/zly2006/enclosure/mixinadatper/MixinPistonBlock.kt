package com.github.zly2006.enclosure.mixinadatper

import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.utils.Permission
import com.github.zly2006.enclosure.utils.Permission.Companion.permissions
import com.github.zly2006.enclosure.utils.mark4updateChecked
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
    cir: CallbackInfoReturnable<Boolean>,
    pistonHandler: PistonHandler
) {
    val positions = (pistonHandler.brokenBlocks + pistonHandler.movedBlocks + pistonPos
            + pistonHandler.movedBlocks.map { it.offset(pistonHandler.motionDirection) }).distinct()
    val areas = positions.map { ServerMain.getSmallestEnclosure(world, it) }
        .map { Optional.ofNullable(it) } // null is also considered as an "area"
        .distinct()
    val cancel = areas.size > 1 && areas.mapNotNull { it.getOrNull()?.hasPubPerm(permissions.PISTON)?.not() }.any { it }
    if (cancel) {
        positions.forEach(world::mark4updateChecked) // some position may be invalid
        cir.returnValue = false
    }
}
