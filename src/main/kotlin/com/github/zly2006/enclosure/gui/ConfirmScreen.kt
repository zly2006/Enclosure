package com.github.zly2006.enclosure.gui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class ConfirmScreen(private val parent: Screen, val message: Text, val action: Runnable) : Screen(Text.of("Confirm")) {
    private val yesButton: ButtonWidget =
        ButtonWidget.builder(Text.translatable("enclosure.widget.yes")) { button: ButtonWidget? ->
            action.run()
            assert(client != null)
            client!!.setScreen(parent)
        }
            .position(parent.width / 2 - 95, 0)
            .size(90, 20)
            .build()
    private val noButton: ButtonWidget =
        ButtonWidget.builder(Text.translatable("enclosure.widget.no")) { button: ButtonWidget? ->
            assert(client != null)
            client!!.setScreen(parent)
        }
            .position(parent.width / 2 + 5, 0)
            .size(90, 20)
            .build()

    init {
        addDrawableChild(yesButton)
        addDrawableChild(noButton)
    }

    override fun render(drawContext: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        parent.render(drawContext, 0, 0, delta)
        renderBackground(drawContext)
        val height = 150
        val x = (parent.width - 200) / 2
        val y = (parent.height - height) / 2
        var linesY = y + 10
        drawContext.drawTexture(TEXTURE, 0, 0, 0, 0, 200, 150)
        val lines = textRenderer.wrapLines(message, 180)
        for (line in lines) {
            drawContext.drawText(textRenderer, line, x + 10, linesY, 0xFFFFFF, true)
            linesY += 10
        }
        yesButton.y = y + height - 30
        noButton.y = y + height - 30
        yesButton.render(drawContext, mouseX, mouseY, delta)
        noButton.render(drawContext, mouseX, mouseY, delta)
    }

    companion object {
        private val TEXTURE = Identifier("textures/gui/demo_background.png")
    }
}
