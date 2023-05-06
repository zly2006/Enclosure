package com.github.zly2006.enclosure.utils

import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

class RayCast(
    val pos1: Vec3d,
    val pos2: Vec3d,
) {
    class Plane(
        val normal: Vec3d,
        val point: Vec3d,
    )
    fun intersect(plane: Plane): Vec3d? {
        val v = pos2.subtract(pos1)
        val n = plane.normal
        val p0 = plane.point
        val t = (n.dotProduct(p0) - n.dotProduct(pos1)) / n.dotProduct(v)
        if (t < 0) return null
        return pos1.add(v.multiply(t))
    }
    fun intersect(box: Box): Vec3d? {
        val planes = listOf(
            Plane(Vec3d(1.0, 0.0, 0.0), Vec3d(box.minX, 0.0, 0.0)),
            Plane(Vec3d(-1.0, 0.0, 0.0), Vec3d(box.maxX, 0.0, 0.0)),
            Plane(Vec3d(0.0, 1.0, 0.0), Vec3d(0.0, box.minY, 0.0)),
            Plane(Vec3d(0.0, -1.0, 0.0), Vec3d(0.0, box.maxY, 0.0)),
            Plane(Vec3d(0.0, 0.0, 1.0), Vec3d(0.0, 0.0, box.minZ)),
            Plane(Vec3d(0.0, 0.0, -1.0), Vec3d(0.0, 0.0, box.maxZ)),
        )
        val intersects = planes.mapNotNull { intersect(it) }
        if (intersects.size < 2) return intersects.firstOrNull()
        val (a, b) = intersects
        return if (a.distanceTo(pos1) < b.distanceTo(pos1)) a else b
    }
}