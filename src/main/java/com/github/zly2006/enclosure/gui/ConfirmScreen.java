package com.github.zly2006.enclosure.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ConfirmScreen extends Screen {
    final Screen parent;
    final Text message;
    final Runnable action;

    private NarratedMultilineTextWidget narratedWidget;

    public ConfirmScreen(Screen parent, Text message, Runnable action) {
        super(Text.of("Confirm"));
        this.parent = parent;
        this.message = message;
        this.action = action;
    }

    @Override
    protected void init() {
//        addDrawableChild(ButtonWidget.builder(Text.translatable("enclosure.widget.yes"), button -> {
//            action.run();
//            assert client != null;
//            client.setScreen(parent);
//        }).width(90).position(width / 2 - 95, height - 50).build());
//
//
//        addDrawableChild(ButtonWidget.builder(Text.translatable("enclosure.widget.no"), button -> {
//            assert client != null;
//            client.setScreen(parent);
//        }).width(90).position(width / 2 + 5, height - 50).build());

        addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, button -> client.setScreen(parent))
                .width(200)
                .position((width - 200) / 2, height - 50)
                .build()
        );

        narratedWidget = new NarratedMultilineTextWidget(width / 2, message, textRenderer) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                action.run();
                assert client != null;
                client.setScreen(parent);
            }
        };
        narratedWidget.setPosition(this.width / 2 - narratedWidget.getWidth() / 2, this.height / 2 - narratedWidget.getHeight() / 2);
        addDrawableChild(narratedWidget);
    }


    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        boolean mouseHoverWidget = mouseX > narratedWidget.getX() && mouseX < narratedWidget.getX() + narratedWidget.getWidth()
                && mouseY > narratedWidget.getY() && mouseY < narratedWidget.getY() + narratedWidget.getHeight();

        if (mouseHoverWidget) {
            long shakeTime = (System.currentTimeMillis() / 20);
            float shakeX = (float) (Math.sin(shakeTime * 15) * 2);
            float shakeY = (float) (Math.cos(shakeTime * 15) * 2);
            narratedWidget.setPosition(
                    this.width / 2 - narratedWidget.getWidth() / 2 + (int) shakeX,
                    this.height / 2 - narratedWidget.getHeight() / 2 + (int) shakeY
            );
        } else {
            narratedWidget.setPosition(
                    this.width / 2 - narratedWidget.getWidth() / 2,
                    this.height / 2 - narratedWidget.getHeight() / 2
            );
        }
    }
}
