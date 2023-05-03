package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.github.zly2006.enclosure.utils.Permission.FEED_ANIMAL;

@Mixin(AnimalEntity.class)
public abstract class MixinAnimalEntity extends Entity {
    public MixinAnimalEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(at = @At("HEAD"), method = "eat", cancellable = true)
    private void onEating(PlayerEntity player, Hand hand, ItemStack stack, CallbackInfo ci) {
        if (getWorld().isClient) {
            return;
        }
        EnclosureArea area = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) this.getWorld()).getArea(getBlockPos());
        if (area != null && !area.areaOf(getBlockPos()).hasPubPerm(FEED_ANIMAL)) {
            player.sendMessage(FEED_ANIMAL.getNoPermissionMsg(player));
            ci.cancel();
        }
    }
}
