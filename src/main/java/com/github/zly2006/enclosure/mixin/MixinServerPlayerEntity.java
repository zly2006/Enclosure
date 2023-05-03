package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.access.PlayerAccess;
import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.TrT;
import com.github.zly2006.enclosure.utils.Utils;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.Environment;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.zly2006.enclosure.command.EnclosureCommandKt.CONSOLE;
import static com.github.zly2006.enclosure.utils.Permission.*;
import static net.fabricmc.api.EnvType.SERVER;

@Environment(SERVER)
@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements PlayerAccess {
    private long lastTeleportTime = 0;
    @Nullable private Vec3d lastPos = null;
    @Nullable private EnclosureArea lastArea = null;
    @NotNull private final List<ItemStack> drops = new ArrayList<>();
    @Shadow public ServerPlayNetworkHandler networkHandler;
    @Nullable private ServerWorld lastWorld;
    private long permissionDeniedMsgTime = 0;

    @Override
    public long getPermissionDeniedMsgTime() {
        return permissionDeniedMsgTime;
    }

    @Override
    public void setPermissionDeniedMsgTime(long permissionDeniedMsgTime) {
        this.permissionDeniedMsgTime = permissionDeniedMsgTime;
    }

    @Shadow public abstract void sendMessage(Text message);

    @Shadow public abstract ServerWorld getWorld();

    @Shadow public abstract void sendMessage(Text message, boolean overlay);

    @Shadow @Final public MinecraftServer server;

    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void protectPVP(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
            //pvp
            EnclosureArea area = ServerMain.INSTANCE.getAllEnclosures(this.getWorld()).getArea(getBlockPos());
            EnclosureArea attackerArea = ServerMain.INSTANCE.getAllEnclosures(attacker.getWorld()).getArea(attacker.getBlockPos());
            if (area != null && !area.areaOf(getBlockPos()).hasPubPerm(Permission.PVP)) {
                cir.setReturnValue(false);
            }
            if (attackerArea != null && !attackerArea.areaOf(attacker.getBlockPos()).hasPubPerm(Permission.PVP)
                    && !attacker.getCommandSource().hasPermissionLevel(4)) {
                attacker.sendMessage(PVP.getNoPermissionMsg(attacker));
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    private void protectDropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        EnclosureArea area = ServerMain.INSTANCE.getAllEnclosures(getWorld()).getArea(getBlockPos());
        if (area == null) {
            return;
        }
        area = area.areaOf(getBlockPos());
        if (!area.hasPerm(networkHandler.player, Permission.DROP_ITEM)) {
            if (!isDead() && getInventory().insertStack(stack)) {
                this.sendMessageWithCD(DROP_ITEM::getNoPermissionMsg);
                cir.setReturnValue(null);
            }
            else {
                this.sendMessageWithCD(TrT.of("enclosure.message.warn_quit_items"));
                drops.add(stack);
            }
        }
    }

    @Nullable
    private Text formatMessage(@NotNull String message, @NotNull EnclosureArea area, ServerPlayerEntity player) {
        if (message.equals("#none")) {
            return null;
        }
        if (message.startsWith("#rich:")) {
            if (ServerMain.INSTANCE.getCommonConfig().allowRichMessage) {
                return Text.Serializer.fromJson(message.substring(6));
            }
            else {
                message = message.substring(6);
            }
        }
        String username = area.getOwner() == CONSOLE ? "Server-Owned-Land" :
                Optional.ofNullable(Utils.getNameByUUID(area.getOwner()))
                        .orElse("§cUnknown§r");
        return Text.of(
                message.replace("%player%", player.getDisplayName().getString())
                        .replace("%name%", area.getName())
                        .replace("%owner%", username)
                        .replace("%world%", area.getWorld().getRegistryKey().getValue().toString())
                        .replace("%full_name%", area.getFullName())
        );
    }

    private void sendFormattedMessage(ServerPlayerEntity player, EnclosureArea area, boolean enter) {
        MutableText text = Text.of(enter ? ServerMain.INSTANCE.getCommonConfig().enterMessageHeader : ServerMain.INSTANCE.getCommonConfig().leaveMessageHeader).copy();
        if (enter) {
            if (area.getEnterMessage().equals("#none")) {
                return;
            } else if (area.getEnterMessage().isEmpty()) {
                text.append(formatMessage(ServerMain.INSTANCE.getCommonConfig().defaultEnterMessage, area, player));
            } else {
                text.append(formatMessage(area.getEnterMessage(), area, player));
            }
        } else {
            if (area.getLeaveMessage().equals("#none")) {
                return;
            } else if (area.getLeaveMessage().isEmpty()) {
                text.append(formatMessage(ServerMain.INSTANCE.getCommonConfig().defaultLeaveMessage, area, player));
            } else {
                text.append(formatMessage(area.getLeaveMessage(), area, player));
            }
        }
        player.sendMessage(text);
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!isDead() && !drops.isEmpty()) {
            drops.removeIf(itemStack -> getInventory().insertStack(itemStack));
        }
        if (server.getTicks() % 10 == 0) {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            EnclosureArea area = ServerMain.INSTANCE.getAllEnclosures(getWorld()).getArea(getBlockPos());
            if (area != null) {
                area = area.areaOf(getBlockPos());
            }
            if (lastArea != null) {
                if (area != lastArea) {
                    sendFormattedMessage(player, lastArea, false);
                }
            }
            if (area != null) {
                if (!area.hasPerm(player, MOVE)) {
                    player.sendMessage(MOVE.getNoPermissionMsg(player));
                    if (area != lastArea && lastWorld != null && lastPos != null) {
                        // teleport back
                        player.teleport(lastWorld, lastPos.x, lastPos.y, lastPos.z, 0, 0);
                    } else {
                        // kick
                        area.kickPlayer(player);
                    }
                }
                if (area != lastArea) {
                    sendFormattedMessage(player, area, true);
                }
                // glowing effect
                if (area.hasPerm(player, GLOWING)) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 15, 1, false, false, false));
                }
            }
            lastArea = area;
            lastPos = player.getPos();
            lastWorld = player.getWorld();
        }
    }

    @Override
    public long getLastTeleportTime() {
        return lastTeleportTime;
    }

    @Override
    public void setLastTeleportTime(long lastTeleportTime) {
        this.lastTeleportTime = lastTeleportTime;
    }
}
