package com.github.zly2006.enclosure.gui;

import com.github.zly2006.enclosure.Enclosure;
import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ReadOnlyEnclosureArea;
import com.github.zly2006.enclosure.utils.Serializable2Text;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            ReadOnlyEnclosureArea area = ReadOnlyEnclosureArea.Companion.fromTag(compound);
            List<String> subAreaNames = new ArrayList<>();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                subAreaNames.add(buf.readString());
            }
            return new EnclosureScreenHandler(syncId, area, fullName, fatherFullName, worldId, subAreaNames);
        });
    public final ReadOnlyEnclosureArea area;
    public final String fullName;
    public final String fatherFullName;
    public final Identifier worldId;
    public final List<String> subAreaNames;

    private EnclosureScreenHandler(int syncId, ReadOnlyEnclosureArea area, String fullName, String fatherFullName, Identifier worldId, List<String> subAreaNames) {
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

    public static void open(@NotNull ServerPlayerEntity player, @NotNull EnclosureArea area) {
        player.openHandledScreen(new ExtendedScreenHandlerFactory() {
                @Override
                public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                    buf.writeString(area.getFullName());
                    if (area.getFather() instanceof Enclosure) {
                        buf.writeString(area.getFather().getFullName());
                    } else if (area.getFather() != null) {
                        buf.writeString("$" + area.getFather().getFullName());
                    } else {
                        buf.writeString("");
                    }
                    buf.writeIdentifier(area.getWorld().getRegistryKey().getValue());
                    NbtCompound compound = new NbtCompound();
                    area.writeNbt(compound);
                    buf.writeNbt(compound);
                    if (area instanceof Enclosure enclosure) {
                        buf.writeVarInt(enclosure.getSubEnclosures().getAreas().size());
                        for (EnclosureArea subArea : enclosure.getSubEnclosures().getAreas()) {
                            buf.writeString(subArea.getName());
                        }
                    } else {
                        buf.writeVarInt(0);
                    }
                }

                @Override
                public Text getDisplayName() {
                    return area.serialize(Serializable2Text.SerializationSettings.Name, player);
                }

                @Nullable
                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    writeScreenOpeningData(null, buf);
                    return EnclosureScreenHandler.ENCLOSURE_SCREEN_HANDLER
                        .create(syncId, inv, buf);
                }
            });
    }
}
