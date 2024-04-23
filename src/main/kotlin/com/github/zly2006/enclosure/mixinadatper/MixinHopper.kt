package com.github.zly2006.enclosure.mixinadatper

import com.github.zly2006.enclosure.utils.Permission
import com.github.zly2006.enclosure.utils.Permission.Companion.permissions
import com.github.zly2006.enclosure.utils.contains
import com.github.zly2006.enclosure.utils.getEnclosure
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.Hopper
import net.minecraft.entity.Entity
import net.minecraft.inventory.Inventory
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Box

fun canExtractFromInventory(
    hopper: Hopper,
    inputInventory: Inventory
): Boolean {
    val pos = when (inputInventory) {
        is BlockEntity -> inputInventory.pos
        is Entity -> inputInventory.blockPos
        else -> return true
    }
    val world = when (inputInventory) {
        is BlockEntity -> inputInventory.world
        is Entity -> inputInventory.world
        else -> return true
    } as? ServerWorld ?: return true
    val area = world.getEnclosure(pos)
    if (area?.hasPubPerm(permissions.CONTAINER) == false) { // null means no enclosure, allow access
        val box = when (hopper) {
            is BlockEntity -> Box(hopper.pos)
            is Entity -> hopper.boundingBox
            else -> return true
        }
        if (!area.toBox().contains(box)) {
            return false
        }
    }
    return true
}
