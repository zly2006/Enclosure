package com.github.zly2006.enclosure.events;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.utils.TrT;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

import static com.github.zly2006.enclosure.ServerMain.minecraftServer;

public class PaidPartEvents {
    public static PaidPartEvents INSTANCE = new PaidPartEvents();

    public void open(ServerPlayerEntity player, EnclosureArea area) {
        player.sendMessage(TrT.of("enclosure.message.not_paid")
                .styled(style -> style
                        .withColor(Formatting.RED)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, ""))),false);
    }
    public void syncSession(ServerPlayerEntity player) {

    }

    public void sendUuid(ServerPlayerEntity player) {

    }

    public CompletableFuture<Suggestions> suggestPlayerNames(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(minecraftServer.getPlayerManager().getPlayerList().stream().map(PlayerEntity::getEntityName), builder);
    }
}
