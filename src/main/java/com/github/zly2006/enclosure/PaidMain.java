package com.github.zly2006.enclosure;

import com.github.zly2006.enclosure.commands.Session;
import com.github.zly2006.enclosure.events.PaidPartEvents;
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler;
import com.github.zly2006.enclosure.utils.Serializable2Text;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.github.zly2006.enclosure.ServerMain.Instance;

public class PaidMain implements DedicatedServerModInitializer {
    public static Map<UUID, String> byUuid = new HashMap<>();

    @Override
    public void onInitializeServer() {
        PaidPartEvents.INSTANCE = new PaidPartEvents() {
            @Override
            public void sendUuid(ServerPlayerEntity player) {
                PacketByteBuf buf2 = PacketByteBufs.create();
                NbtCompound compound = new NbtCompound();
                PaidMain.byUuid.forEach((uuid, s) -> compound.putUuid(s, uuid));
                buf2.writeNbt(compound);
                compound.put("", new NbtCompound());
                ServerPlayNetworking.send(player, new Identifier("enclosure", "packet.uuid"), buf2);
            }

            @Override
            public void open(ServerPlayerEntity player, EnclosureArea area) {
                player.openHandledScreen(new ExtendedScreenHandlerFactory() {
                    @Override
                    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                        buf.writeString(area.getFullName());
                        if (area.father instanceof Enclosure) {
                            buf.writeString(area.getFather().getFullName());
                        } else if (area.father != null) {
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

            @Override
            public void syncSession(ServerPlayerEntity player) {
                Session session = Instance.getPlayerSessions().get(player.getUuid());
                if (session != null) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeBlockPos(session.getPos1());
                    buf.writeBlockPos(session.getPos2());
                    ServerPlayNetworking.send(player, new Identifier("enclosure", "packet.sync_selection"), buf);
                }
            }

            @Override
            public CompletableFuture<Suggestions> suggestPlayerNames(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
                return CommandSource.suggestMatching(byUuid.values(), builder);
            }
        };
    }
}
