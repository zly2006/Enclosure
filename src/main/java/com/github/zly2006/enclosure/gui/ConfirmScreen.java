package com.github.zly2006.enclosure.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class ConfirmScreen extends Screen {
    private static final Identifier TEXTURE = new Identifier("textures/gui/demo_background.png");
    final Screen parent;
    final Text message;
    final Runnable action;
    final ButtonWidget yesButton;
    final ButtonWidget noButton;

    public ConfirmScreen(Screen parent, Text message, Runnable action) {
        super(Text.of("Confirm"));
        this.parent = parent;
        this.message = message;
        this.action = action;
        yesButton = ButtonWidget.builder(Text.translatable("enclosure.widget.yes"), button -> {
                action.run();
                assert client != null;
                client.setScreen(parent);
            })
            .position(parent.width / 2 - 95, 0)
            .size(90, 20)
            .build();
        noButton = ButtonWidget.builder(Text.translatable("enclosure.widget.no"), button -> {
                assert client != null;
                client.setScreen(parent);
            })
            .position(parent.width / 2 + 5, 0)
            .size(90, 20)
            .build();
        addDrawableChild(yesButton);
        addDrawableChild(noButton);
    }
    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        parent.render(matrices, 0, 0, delta);
        renderBackground(matrices);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int height = 150;
        int x = (parent.width - 200) / 2;
        int y = (parent.height - height) / 2;
        int linesY = y + 10;
        drawTexture(matrices, x, y, 0, 0, 200, 150, 200, 150);
        List<OrderedText> lines = textRenderer.wrapLines(message, 180);
        for (OrderedText line : lines) {
            textRenderer.draw(matrices, line, x + 10, linesY, 0xFFFFFF);
            linesY += 10;
        }
        yesButton.setY(y + height - 30);
        noButton.setY(y + height - 30);
        yesButton.render(matrices, mouseX, mouseY, delta);
        noButton.render(matrices, mouseX, mouseY, delta);
    }
}
