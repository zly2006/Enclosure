package com.github.zly2006.enclosure.gui;

import com.github.zly2006.enclosure.EnclosureView;
import com.github.zly2006.enclosure.client.ClientMain;
import com.github.zly2006.enclosure.command.EnclosureCommandKt;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.HashMap;

public class TransferScreen extends Screen implements EnclosureGui {
    private final String fullName;
    private final Screen parent;
    EnclosureView.ReadOnly data;
    private MemberTargetListWidget<ButtonWidget> memberTargetListWidget;
    public TransferScreen(EnclosureView.ReadOnly data, String fullName, Screen parent) {
        super(Text.literal("Transfer"));
        this.fullName = fullName;
        this.parent = parent;
        this.data = data.clone();
        this.data.getPermissionsMap().clear();
        ClientMain.uuid2name.forEach((uuid, s) -> {
            if (!uuid.equals(EnclosureCommandKt.CONSOLE)) {
                this.data.getPermissionsMap().put(uuid, new HashMap<>());
            }
        });
        System.out.println(data.getPermissionsMap().size());
    }

    @Override
    protected void init() {
        this.memberTargetListWidget = new MemberTargetListWidget<>(client, data, fullName, this,
                10 * 2 + textRenderer.fontHeight, 30, 0, 0,
                (widget, uuid) ->
                ButtonWidget.builder(Text.translatable("enclosure.widget.transfer"), button -> {
                    assert client != null;
                    assert client.player != null;
                    client.player.networkHandler.sendCommand("enclosure give " + fullName + " " + uuid);
                    client.setScreen(parent);
                }).size(80, 20).build());
        addDrawableChild(memberTargetListWidget);
        memberTargetListWidget.showPlayers();
        addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, button -> client.setScreen(parent))
                .width(200)
                .position((width - 200) / 2, height - 25)
                .build()
        );
        super.init();
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);

        // todo translate
        drawContext.drawCenteredTextWithShadow(textRenderer, Text.literal("转让领地 ").append(fullName).append(" 给:"), width / 2, 10, 0xffffff);
    }
}
