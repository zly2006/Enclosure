package com.github.zly2006.enclosure.gui;

import com.github.zly2006.enclosure.ReadOnlyEnclosureArea;
import com.github.zly2006.enclosure.network.UUIDCacheS2CPacket;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.zly2006.enclosure.command.EnclosureCommandKt.CONSOLE;

public class PermissionScreen extends Screen {
    public static final Identifier PERMISSION_SCREEN_ID = new Identifier("enclosure", "screen.permission");

    final ReadOnlyEnclosureArea area;
    public final UUID uuid;
    final String fullName;
    final Screen parent;
    PermissionListWidget permissionWidgetList;
    public PermissionScreen(ReadOnlyEnclosureArea area, UUID uuid, String fullName, Screen parent) {
        super(Text.of("Set permission"));
        this.area = area;
        this.uuid = uuid;
        this.fullName = fullName;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        permissionWidgetList = new PermissionListWidget(client, this,
                fullName, area, uuid, width, height, 20, height);
        addDrawableChild(permissionWidgetList);
        setFocused(permissionWidgetList);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            this.close();
            return true;
        }
        if (getFocused() == null) {
            return false;
        }
        return getFocused().keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        super.close();
        assert client != null;
        client.setScreen(parent);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        MutableText title = Text.translatable("enclosure.widget.set_permission").append(" ");
        if (CONSOLE.equals(uuid)) {
            title.append(Text.translatable("enclosure.widget.global"));
        }
        else {
            title.append(Text.translatable("enclosure.widget.player"))
                    .append(" ")
                    .append(UUIDCacheS2CPacket.getName(uuid));
        }
        title.append(" ")
                .append(Text.translatable("enclosure.widget.in_enclosure"))
                .append(" ")
                .append(fullName);
        textRenderer.draw(matrices, title, 10, 10, 0xffffff);
    }

    public void requestConfirm(Text readString) {
        assert client != null;
        client.execute(() -> client.setScreen(new ConfirmScreen(this, readString, () -> {
            assert client.player != null;
            client.player.networkHandler.sendCommand("enclosure confirm");
        })));
    }

    public void syncPermission(NbtCompound permission) {
        Map<String, Boolean> perms = new HashMap<>();
        for (String key : permission.getKeys()) {
            perms.put(key, permission.getBoolean(key));
        }
        area.getPermissionsMap().put(uuid, perms);
    }
}
