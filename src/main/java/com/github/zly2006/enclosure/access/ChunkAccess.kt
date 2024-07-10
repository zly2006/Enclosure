package com.github.zly2006.enclosure.access

import com.github.zly2006.enclosure.Enclosure

@Suppress("INAPPLICABLE_JVM_NAME")
interface ChunkAccess {
    @get:JvmName("enclosure\$cache")
    val cache: List<Enclosure>

    @JvmName("enclosure\$putCache")
    fun putCache(enclosure: Enclosure)

    @JvmName("enclosure\$removeCache")
    fun removeCache(enclosure: Enclosure)
}
