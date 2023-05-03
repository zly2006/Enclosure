package com.github.zly2006.enclosure.command

import net.minecraft.util.math.BlockPos
import kotlin.math.abs

open class ClientSession {
    var pos1: BlockPos = BlockPos(0, 0, 0)
    var pos2: BlockPos = BlockPos(0, 0, 0)
    var enabled = false

    fun size(): Int {
        return (abs(pos1.x - pos2.x) + 1) *
                (abs(pos1.y - pos2.y) + 1) *
                (abs(pos1.z - pos2.z) + 1)
    }
}