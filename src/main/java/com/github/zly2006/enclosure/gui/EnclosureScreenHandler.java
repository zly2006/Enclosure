package com.github.zly2006.enclosure.gui;

import com.github.zly2006.enclosure.Enclosure;
import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.EnclosureView;
import com.github.zly2006.enclosure.utils.Serializable2Text;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.codec.PacketCodec;
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
    public static final Identifier ENCLOSURE_SCREEN_ID = Identifier.of("enclosure", "screen.enclosure");
    public static class Data {
        public String fullName;
        public String fatherFullName;
        public Identifier worldId;
        public NbtCompound compound;
        public List<String> subAreaNames;
    }
    public static final ExtendedScreenHandlerType<EnclosureScreenHandler, Data> ENCLOSURE_SCREEN_HANDLER =
        new ExtendedScreenHandlerType<>((syncId, inventory, data) -> {
            EnclosureView.ReadOnly area = EnclosureView.ReadOnly.Companion.readonly(data.compound);
            return new EnclosureScreenHandler(syncId, area, data.fullName, data.fatherFullName, data.worldId, data.subAreaNames);
        }, PacketCodec.of((value, buf) -> {
            buf.writeString(value.fullName);
            buf.writeString(value.fatherFullName);
            buf.writeIdentifier(value.worldId);
            buf.writeNbt(value.compound);
            buf.writeVarInt(value.subAreaNames.size());
            for (String subAreaName : value.subAreaNames) {
                buf.writeString(subAreaName);
            }
        }, buf -> {
            Data data = new Data();
            data.fullName = buf.readString();
            data.fatherFullName = buf.readString();
            data.worldId = buf.readIdentifier();
            data.compound = buf.readNbt();
            int size = buf.readVarInt();
            data.subAreaNames = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                data.subAreaNames.add(buf.readString());
            }
            return data;
        }));
    public final EnclosureView.ReadOnly area;
    public final String fullName;
    public final String fatherFullName;
    public final Identifier worldId;
    public final List<String> subAreaNames;

    private EnclosureScreenHandler(int syncId, EnclosureView.ReadOnly area, String fullName, String fatherFullName, Identifier worldId, List<String> subAreaNames) {
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
        player.openHandledScreen(new ExtendedScreenHandlerFactory<Data>() {
            @Override
            public Data getScreenOpeningData(ServerPlayerEntity player) {
                Data data = new Data();
                data.fullName = area.getFullName();
                if (area.getFather() instanceof Enclosure) {
                    data.fatherFullName = area.getFather().getFullName();
                } else if (area.getFather() != null) {
                    data.fatherFullName = "$" + area.getFather().getFullName();
                } else {
                    data.fatherFullName = "";
                }
                data.worldId = area.getWorld().getRegistryKey().getValue();
                NbtCompound compound = new NbtCompound();
                area.writeNbt(compound, null);
                data.compound = compound;
                if (area instanceof Enclosure enclosure) {
                    data.subAreaNames = new ArrayList<>(enclosure.getSubEnclosures().getAreas().size());
                    for (EnclosureArea subArea : enclosure.getSubEnclosures().getAreas()) {
                        data.subAreaNames.add(subArea.getName());
                    }
                } else {
                    data.subAreaNames = new ArrayList<>(0);
                }
                return data;
            }

            @Override
            public Text getDisplayName() {
                return area.serialize(Serializable2Text.SerializationSettings.Name, player);
            }

            @Nullable
            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                return EnclosureScreenHandler.ENCLOSURE_SCREEN_HANDLER
                        .create(syncId, inv, getScreenOpeningData((ServerPlayerEntity) player));
            }
        });
    }
}
