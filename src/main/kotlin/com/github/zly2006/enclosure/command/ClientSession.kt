package com.github.zly2006.enclosure.command

import net.minecraft.util.math.BlockPos
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

fun ClientSession.ordered(): List<Int> = listOf(
    min(pos1.x, pos2.x),
    min(pos1.y, pos2.y),
    min(pos1.z, pos2.z),
    max(pos1.x, pos2.x),
    max(pos1.y, pos2.y),
    max(pos1.z, pos2.z),
)

fun ClientSession.border(): List<Int> = listOf(
    min(pos1.x, pos2.x),
    min(pos1.y, pos2.y),
    min(pos1.z, pos2.z),
    max(pos1.x, pos2.x) + 1,
    max(pos1.y, pos2.y) + 1,
    max(pos1.z, pos2.z) + 1,
)

fun ClientSession.border(
    cameraX: Float,
    cameraY: Float,
    cameraZ: Float,
): List<Float> = listOf(
    min(pos1.x, pos2.x) - cameraX,
    min(pos1.y, pos2.y) - cameraY,
    min(pos1.z, pos2.z) - cameraZ,
    max(pos1.x, pos2.x) + 1 - cameraX,
    max(pos1.y, pos2.y) + 1 - cameraY,
    max(pos1.z, pos2.z) + 1 - cameraZ,
)