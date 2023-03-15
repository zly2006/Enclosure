package com.github.zly2006.enclosure.commands;

import com.github.zly2006.enclosure.utils.TrT;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.zly2006.enclosure.ServerMain.minecraftServer;
import static net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import static net.minecraft.server.command.CommandManager.literal;

public class ConfirmManager {
    private final static UUID CONSOLE = new UUID(0, 0);
    public static final Map<UUID, Confirmable> runnableMap = new HashMap<>();

    public static void confirm(ServerPlayerEntity player, Confirmable runnable) {
        MutableText text = TrT.of("enclosure.message.dangerous");
        text.setStyle(Style.EMPTY
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TrT.of("enclosure.message.confirm_event")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enclosure confirm"))
                .withColor(Formatting.YELLOW)
        );
        if (player == null) {
            runnableMap.put(CONSOLE, runnable);
            minecraftServer.getCommandSource().sendMessage(TrT.of("enclosure.message.operation_confirm"));
        } else {
            runnableMap.put(player.getUuid(), runnable);
            player.sendMessage(text);
        }
    }

    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, RegistrationEnvironment environment) {
        dispatcher.register(literal("enclosure")
                .then(literal("confirm")
                        .executes(context -> {
                            if (context.getSource().isExecutedByPlayer()) {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                assert player != null;
                                if (runnableMap.containsKey(player.getUuid())) {
                                    runnableMap.get(player.getUuid()).confirm();
                                    runnableMap.remove(player.getUuid());
                                } else {
                                    player.getCommandSource().sendError(TrT.of("enclosure.message.no_task_to_confirm"));
                                }
                            } else {
                                if (runnableMap.containsKey(CONSOLE)) {
                                    runnableMap.get(CONSOLE).confirm();
                                    runnableMap.remove(CONSOLE);
                                } else {
                                    minecraftServer.getCommandSource().sendError(TrT.of("enclosure.message.no_task_to_confirm"));
                                }
                            }
                            return 0;
                        })
                )
        );
    }
    public interface Confirmable {
        void confirm() throws CommandSyntaxException;
    }
}
