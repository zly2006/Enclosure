package com.github.zly2006.enclosure.mixinadatper

import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.access.LecternInventoryAccess
import com.github.zly2006.enclosure.utils.Permission
import com.github.zly2006.enclosure.utils.contains
import com.github.zly2006.enclosure.utils.getEnclosure
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.Hopper
import net.minecraft.entity.Entity
import net.minecraft.inventory.DoubleInventory
import net.minecraft.inventory.Inventory
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Box
import net.minecraft.world.World

fun Inventory.world(): World? = when (this) {
    is BlockEntity -> this.world
    is Entity -> this.world
    is DoubleInventory -> this.first.world()
    else -> null
}

fun Inventory.enclosure(): EnclosureArea? {
    val world = world() as? ServerWorld ?: return null
    return when (this) {
        is BlockEntity -> return world.getEnclosure(this.pos)
        is Entity -> return world.getEnclosure(this.blockPos)
        is DoubleInventory -> return this.first.enclosure() ?: this.second.enclosure()
        else -> null
    }
}

fun canExtractFromInventory(
    hopper: Hopper,
    inputInventory: Inventory
): Boolean {
    val area = inputInventory.enclosure()
    if (area?.hasPubPerm(Permission.CONTAINER) == false) { // null means no enclosure, allow access
        val box = when (hopper) {
            is BlockEntity -> Box(hopper.pos)
            is Entity -> hopper.boundingBox
            else -> return true
        }
        return area.toBox().contains(box) // allow access if the hopper is in the enclosure
    }
    return true
}
