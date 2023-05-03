package com.github.zly2006.enclosure.utils

import net.minecraft.text.*
import net.minecraft.util.Formatting

fun MutableText.hoverText(text: Text): MutableText {
    return this.styled { it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, text)) }
}

fun Style.hoverText(text: Text): Style {
    return this.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, text))
}

fun Style.clickRun(command: String): Style {
    return this.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
}

operator fun MutableText.plusAssign(text: Text) {
    this.append(text)
}

operator fun Text.plus(text: Text): MutableText {
    return this.copy().append(text)
}

operator fun Text.plus(text: String): MutableText {
    return this.copy().append(text)
}

fun MutableText.green(): MutableText {
    return this.formatted(Formatting.GREEN)
}

fun MutableText.darkGreen(): MutableText {
    return this.formatted(Formatting.DARK_GREEN)
}

fun MutableText.gold(): MutableText {
    return this.formatted(Formatting.GOLD)
}

fun MutableText.red(): MutableText {
    return this.formatted(Formatting.RED)
}

fun MutableText.white(): MutableText {
    return this.formatted(Formatting.WHITE)
}

fun literalText(text: Any): MutableText {
    return when (text) {
        is Text -> text.copy()
        is String -> Text.literal(text)
        else -> Text.literal(text.toString())
    }
}