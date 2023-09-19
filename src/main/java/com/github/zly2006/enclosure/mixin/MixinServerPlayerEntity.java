package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.access.PlayerAccess;
import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.TrT;
import com.github.zly2006.enclosure.utils.Utils;
import com.github.zly2006.enclosure.utils.UtilsKt;
import com.mojang.authlib.GameProfile;
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

import java.util.Optional;

import static com.github.zly2006.enclosure.command.EnclosureCommandKt.CONSOLE;
import static com.github.zly2006.enclosure.utils.Permission.*;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements PlayerAccess {
    @Shadow public ServerPlayNetworkHandler networkHandler;
    private long lastTeleportTime = 0;
    @Nullable private Vec3d lastPos = null;
    @Nullable private EnclosureArea lastArea = null;
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

    @Shadow public abstract void sendMessage(Text message, boolean overlay);

    @Shadow @Final public MinecraftServer server;

    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "openHorseInventory", at = @At("HEAD"), cancellable = true)
    private void onOpenHorseInventory(CallbackInfo ci) {
        EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) this.getWorld(), getBlockPos());
        if (area != null && !area.hasPerm((ServerPlayerEntity) (Object) this, Permission.CONTAINER)) {
            sendMessage(Permission.CONTAINER.getNoPermissionMsg(this));
            ci.cancel();
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void protectPVP(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
            //pvp
            EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) getWorld(), getBlockPos());
            EnclosureArea attackerArea = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) attacker.getWorld(), attacker.getBlockPos());
            if (area != null && !area.hasPubPerm(Permission.PVP)) {
                cir.setReturnValue(false);
            }
            if (attackerArea != null && !attackerArea.hasPubPerm(Permission.PVP)
                    && UtilsKt.checkPermission(attacker, "enclosure.bypass")) {
                attacker.sendMessage(PVP.getNoPermissionMsg(attacker));
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "dropItem", at = @At("RETURN"), cancellable = true)
    private void protectDropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) getWorld(), getBlockPos());
        if (area == null) {
            return;
        }
        if (!area.hasPerm(networkHandler.player, Permission.DROP_ITEM)) {
            if (!isDead() && getInventory().insertStack(stack)) {
                this.sendMessageWithCD(DROP_ITEM::getNoPermissionMsg);
                cir.setReturnValue(null);
            }
            else {
                if (cir.getReturnValue() != null) {
                    this.sendMessageWithCD(TrT.of("enclosure.message.item_only_self_pickup"));
                    cir.getReturnValue().setOwner(getUuid());
                }
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
        player.sendMessage(text, ServerMain.INSTANCE.getCommonConfig().useActionBarMessage);
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (server.getTicks() % 10 == 0) {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            EnclosureArea area = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) getWorld()).getArea(getBlockPos());
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
            lastWorld = (ServerWorld) player.getWorld();
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
