package com.github.zly2006.enclosure.listeners;

import com.github.zly2006.enclosure.commands.Session;
import com.github.zly2006.enclosure.utils.TrT;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.UUID;

import static com.github.zly2006.enclosure.ServerMain.Instance;
import static com.github.zly2006.enclosure.ServerMain.blockPos2string;

public class SessionListener implements
        PlayerBlockBreakEvents.Before,
        ServerPlayConnectionEvents.Join,
        ServerPlayConnectionEvents.Disconnect,
        AttackBlockCallback,
        UseBlockCallback,
        ServerEntityWorldChangeEvents.AfterPlayerChange {

    HitResult result;

    private SessionListener() {
    }

    public static void register() {
        SessionListener listener = new SessionListener();
        PlayerBlockBreakEvents.BEFORE.register(listener);
        ServerPlayConnectionEvents.JOIN.register(listener);
        ServerPlayConnectionEvents.DISCONNECT.register(listener);
        AttackBlockCallback.EVENT.register(listener);
        UseBlockCallback.EVENT.register(listener);
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(listener);
    }

    @Override
    public void afterChangeWorld(ServerPlayerEntity player, ServerWorld origin, ServerWorld destination) {
        getSession(player).reset(destination);
    }

    private static Session getSession(UUID player) {
        return Instance.getPlayerSessions().get(player);
    }

    private static Session getSession(PlayerEntity player) {
        return getSession(player.getUuid());
    }

    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        if (player.getMainHandStack().getItem() == Instance.getOperationItem()) {
            if (getSession(player).getPos1() != pos) {
                Session session = getSession(player);
                session.sync((ServerPlayerEntity) player);
                session.setPos1(pos);
                session.trySync();
                player.sendMessage(TrT.of("enclosure.message.set_pos_1").append(blockPos2string(pos)));
                return ActionResult.FAIL;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (player.getMainHandStack().getItem() == Instance.getOperationItem()) {
            if (getSession(player).getPos1() != pos) {
                Session session = getSession(player);
                session.sync((ServerPlayerEntity) player);
                session.setPos1(pos);
                session.trySync();
                player.sendMessage(TrT.of("enclosure.message.set_pos_1").append(blockPos2string(pos)));
                return false;
            }
        }
        return true;
    }

    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        this.result = hitResult;
        if ((player.getMainHandStack().getItem() == Instance.getOperationItem() && hand.equals(Hand.MAIN_HAND)) || (player.getOffHandStack().getItem() == Instance.getOperationItem() && hand.equals(Hand.OFF_HAND))) {
            if (getSession(player).getPos2() != hitResult.getBlockPos()) {
                Session session = getSession(player);
                session.sync((ServerPlayerEntity) player);
                session.setPos2(hitResult.getBlockPos());
                session.trySync();
                player.sendMessage(TrT.of("enclosure.message.set_pos_2").append(blockPos2string(hitResult.getBlockPos())));
                return ActionResult.FAIL;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        Instance.getPlayerSessions().remove(handler.player.getUuid());
    }

    @Override
    public void onPlayReady(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        Session session = new Session();
        session.setOwner(handler.player.getUuid());
        Instance.getPlayerSessions().put(handler.player.getGameProfile().getId(), session);
    }
}
