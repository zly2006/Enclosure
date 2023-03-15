package com.github.zly2006.enclosure.gui;

import com.github.zly2006.enclosure.EnclosureArea;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public class EnclosureScreenHandler extends ScreenHandler {
    public static final Identifier ENCLOSURE_SCREEN_ID = new Identifier("enclosure", "screen.enclosure");
    public static final ExtendedScreenHandlerType<EnclosureScreenHandler> ENCLOSURE_SCREEN_HANDLER =
            new ExtendedScreenHandlerType<>((syncId, inventory, buf) -> {
                String fullName = buf.readString();
                String fatherFullName = buf.readString();
                Identifier worldId = buf.readIdentifier();
                NbtCompound compound = buf.readNbt();
                assert compound != null;
                EnclosureArea area = new EnclosureArea(compound);
                List<String> subAreaNames = new ArrayList<>();
                int size = buf.readVarInt();
                for (int i = 0; i < size; i++) {
                    subAreaNames.add(buf.readString());
                }
                return new EnclosureScreenHandler(syncId, area, fullName, fatherFullName, worldId, subAreaNames);
            });
    public EnclosureArea area;
    public final String fullName;
    public final String fatherFullName;
    public final Identifier worldId;
    public final List<String> subAreaNames;

    private EnclosureScreenHandler(int syncId, EnclosureArea area, String fullName, String fatherFullName, Identifier worldId, List<String> subAreaNames) {
        super(ENCLOSURE_SCREEN_HANDLER, syncId);
        this.area = area;
        this.fullName = fullName;
        this.fatherFullName = fatherFullName;
        this.worldId = worldId;
        this.subAreaNames = subAreaNames;
    }

    public static void register() {
        Registry.register(Registries.SCREEN_HANDLER, ENCLOSURE_SCREEN_ID, ENCLOSURE_SCREEN_HANDLER);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
