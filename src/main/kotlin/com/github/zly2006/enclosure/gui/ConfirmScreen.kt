package com.github.zly2006.enclosure.gui

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class ConfirmScreen(private val parent: Screen, val message: Text, val action: Runnable) : Screen(Text.of("Confirm")), EnclosureGui {
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

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        parent.render(matrices, 0, 0, delta)
        renderBackground(matrices)
        RenderSystem.setShaderTexture(0, TEXTURE)
        val height = 150
        val x = (parent.width - 200) / 2
        val y = (parent.height - height) / 2
        var linesY = y + 10
        drawTexture(matrices, x, y, 0f, 0f, 200, 150, 200, 150)
        val lines = textRenderer.wrapLines(message, 180)
        for (line in lines) {
            textRenderer.draw(matrices, line, (x + 10).toFloat(), linesY.toFloat(), 0xFFFFFF)
            linesY += 10
        }
        yesButton.y = y + height - 30
        noButton.y = y + height - 30
        yesButton.render(matrices, mouseX, mouseY, delta)
        noButton.render(matrices, mouseX, mouseY, delta)
    }

    companion object {
        private val TEXTURE = Identifier("textures/gui/demo_background.png")
    }
}
