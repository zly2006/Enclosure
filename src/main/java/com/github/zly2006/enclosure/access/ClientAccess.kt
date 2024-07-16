package com.github.zly2006.enclosure.access

import net.minecraft.sound.MusicSound

@Suppress("INAPPLICABLE_JVM_NAME")
interface ClientAccess {
    @get:JvmName("enclosure\$getBgm")
    @set:JvmName("enclosure\$setBgm")
    var bgm: MusicSound?
}
