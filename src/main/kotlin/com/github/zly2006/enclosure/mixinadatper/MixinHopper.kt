package com.github.zly2006.enclosure.mixinadatper

import com.github.zly2006.enclosure.utils.Permission
import com.github.zly2006.enclosure.utils.contains
import com.github.zly2006.enclosure.utils.getEnclosure
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.Hopper
import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.inventory.Inventory
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Box
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

fun onExtractFromInventory(
    world: ServerWorld,
    hopper: Hopper,
    inputInventory: Inventory,
    cir: CallbackInfoReturnable<Boolean>,
) {
    if (inputInventory is BlockEntity) {
        val area = world.getEnclosure(inputInventory.pos)
        if (area?.hasPubPerm(Permission.CONTAINER) == false) { // null means no enclosure, allow access
            val box = when (hopper) {
                is BlockEntity -> Box(hopper.pos)
                is AbstractMinecartEntity -> hopper.boundingBox
                else -> return
            }
            if (!area.toBox().contains(box)) {
                cir.returnValue = false
            }
        }
    }
}
