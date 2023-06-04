package com.github.zly2006.enclosure.gui

import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text

internal class ButtonWidgetBuilder(private val text: Text, private val onPress: ButtonWidget.PressAction) {
    private var x = 0
    private var y = 0
    private var width = 0
    private var height = 0
    fun position(x: Int, y: Int): ButtonWidgetBuilder {
        this.x = x
        this.y = y
        return this
    }

    fun size(width: Int, height: Int): ButtonWidgetBuilder {
        this.width = width
        this.height = height
        return this
    }

    fun build(): ButtonWidget {
        return ButtonWidget(x, y, width, height, text, onPress)
    }
}