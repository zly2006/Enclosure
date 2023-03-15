package com.github.zly2006.enclosure.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public interface PlayerPlaceBlockEvent {
    List<PlayerPlaceBlockEvent> EVENTS = new ArrayList<>();

    ActionResult interact(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult);

    static void register(PlayerPlaceBlockEvent event) {
        EVENTS.add(event);
    }

    static ActionResult onPlayerPlaceBlock(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        for (PlayerPlaceBlockEvent event : EVENTS) {
            ActionResult result = event.interact(player, world, hand, entity, hitResult);
            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    }
}
