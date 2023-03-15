package com.github.zly2006.enclosure.utils;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;

public interface Serializable2Text {
    MutableText serialize(SerializationSettings settings, ServerPlayerEntity player);

    enum SerializationSettings {
        Hover,
        Name,
        Summarize,
        Full,
        BarredFull,
    }
    static Serializable2Text of(MutableText text) {
        return (settings, player) -> text;
    }
}
