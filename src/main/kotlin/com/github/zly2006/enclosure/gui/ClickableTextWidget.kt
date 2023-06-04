package com.github.zly2006.enclosure.gui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

class ClickableTextWidget(
    client: MinecraftClient,
    private val parent: Screen,
    val text: Text,
    private val hover: Text?,
    private val onClick: Consumer<Int>?,
    var x: Int,
    var y: Int,
    var width: Int
) : Element, Drawable, Selectable {
    private var hovered = false
    private var selected = false
    override fun getType(): Selectable.SelectionType {
        return if (hovered) {
            Selectable.SelectionType.HOVERED
        } else if (selected) {
            Selectable.SelectionType.FOCUSED
        } else {
            Selectable.SelectionType.NONE
        }
    }

    override fun appendNarrations(builder: NarrationMessageBuilder) {}
    var renderedWidth = 0
    var height = 0
        private set
    val textRenderer: TextRenderer
    fun calcHeight(): Int {
        return textRenderer.wrapLines(text, width).size * (textRenderer.fontHeight + 1)
    }

    init {
        textRenderer = client.textRenderer
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX >= x && mouseX <= x + renderedWidth) {
            if (mouseY >= y && mouseY <= y + height) {
                selected = true
                onClick?.accept(button)
                return true
            }
        }
        return false
    }

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        if (mouseX >= x && mouseX <= x + renderedWidth) {
            if (mouseY >= y && mouseY <= y + height) {
                hovered = true
                if (hover != null) {
                    parent.renderOrderedTooltip(
                        matrices,
                        textRenderer.wrapLines(hover, max(200, parent.width / 2)),
                        mouseX,
                        mouseY
                    )
                }
            }
        }
        height = 0
        if (width == 0) {
            width = min(textRenderer.getWidth(text), parent.width)
        }
        val orderedTexts = textRenderer.wrapLines(text, width)
        renderedWidth = if (orderedTexts.size == 1) {
            textRenderer.getWidth(orderedTexts[0])
        } else {
            width
        }
        for (orderedText in orderedTexts) {
            textRenderer.draw(matrices, orderedText, x.toFloat(), (y + height).toFloat(), 0xffffff)
            height += textRenderer.fontHeight + 1
        }
    }
}
