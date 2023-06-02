package com.github.zly2006.enclosure.client

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.LiteralText

class ClientConfigScreen: Screen(LiteralText("Enclosure Config")) {
    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(matrices, mouseX, mouseY, delta)
    }

    override fun init() {
        super.init()
    }
}