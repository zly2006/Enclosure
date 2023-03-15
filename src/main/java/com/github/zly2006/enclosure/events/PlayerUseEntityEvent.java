package com.github.zly2006.enclosure.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public interface PlayerUseEntityEvent {
    List<PlayerUseEntityEvent> EVENTS = new ArrayList<>();

    ActionResult interact(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult);

    static void register(PlayerUseEntityEvent event) {
        EVENTS.add(event);
    }

    static ActionResult onPlayerUseEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        for (PlayerUseEntityEvent event : EVENTS) {
            ActionResult result = event.interact(player, world, hand, entity, hitResult);
            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    }
}
