package com.github.zly2006.enclosure.gui;

import com.github.zly2006.enclosure.EnclosureView;
import com.github.zly2006.enclosure.command.EnclosureCommandKt;
import com.github.zly2006.enclosure.network.UUIDCacheS2CPacket;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.HashMap;

public class TransferScreen extends Screen implements EnclosureGui {
    private final String fullName;
    private final Screen parent;
    EnclosureView.ReadOnly data;
    private PermissionTargetListWidget<ButtonWidget> permissionTargetListWidget;
    public TransferScreen(EnclosureView.ReadOnly data, String fullName, Screen parent) {
        super(Text.literal("Transfer"));
        this.fullName = fullName;
        this.parent = parent;
        this.data = data.clone();
        this.data.getPermissionsMap().clear();
        UUIDCacheS2CPacket.uuid2name.forEach((uuid, s) -> {
            if (!uuid.equals(EnclosureCommandKt.CONSOLE)) {
                this.data.getPermissionsMap().put(uuid, new HashMap<>());
            }
        });
        System.out.println(data.getPermissionsMap().size());
    }

    @Override
    protected void init() {
        this.permissionTargetListWidget = new PermissionTargetListWidget<>(client, data, fullName, this, width, height, 20, height, (widget, uuid) ->
                ButtonWidget.builder(Text.translatable("enclosure.widget.transfer"), button -> {
                    assert client != null;
                    assert client.player != null;
                    client.player.networkHandler.sendCommand("enclosure give " + fullName + " " + uuid);
                    client.setScreen(parent);
                }).size(80, 20).build());
        addDrawableChild(permissionTargetListWidget);
        permissionTargetListWidget.showPlayers();
        super.init();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        permissionTargetListWidget.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }
}
