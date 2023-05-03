package com.github.zly2006.enclosure;

import com.github.zly2006.enclosure.command.Session;
import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.TrT;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static com.github.zly2006.enclosure.EnclosureListKt.SUB_ENCLOSURES_KEY;

public class Enclosure extends EnclosureArea {
    final EnclosureList subEnclosures;

    /**
     * Create an instance from nbt for a specific world.
     * @param compound the nbt compound tag
     */
    public Enclosure(NbtCompound compound, ServerWorld world) {
        super(compound, world);
        // process sub enclosures
        NbtCompound sub = compound.getCompound(SUB_ENCLOSURES_KEY);
        subEnclosures = new EnclosureList(sub, world, false);
        subEnclosures.getAreas().forEach(this::addChild);
    }

    public Enclosure(Session session, String name) {
        super(session, name);
        subEnclosures = new EnclosureList(session.getWorld(), false);
    }

    @Override
    public @NotNull NbtCompound writeNbt(@NotNull NbtCompound nbt) {
        NbtCompound compound = super.writeNbt(nbt);
        NbtCompound sub = new NbtCompound();
        subEnclosures.writeNbt(sub);
        compound.put(SUB_ENCLOSURES_KEY, sub);
        return compound;
    }

    @Override
    public void changeWorld(@NotNull ServerWorld world) {
        if (world == this.getWorld()) return;
        super.setWorld(world);
    }

    @Override
    public @NotNull EnclosureArea areaOf(@NotNull BlockPos pos) {
        for (EnclosureArea area : subEnclosures.getAreas()) {
            if (area.isInner(pos)) {
                return area.areaOf(pos);
            }
        }
        return super.areaOf(pos);
    }

    public @NotNull EnclosureList getSubEnclosures() {
        return this.subEnclosures;
    }

    @Override
    public @NotNull MutableText serialize(@NotNull SerializationSettings settings, @Nullable ServerPlayerEntity player) {
        if (settings == SerializationSettings.Full) {
            MutableText text = super.serialize(settings, player);
            if (subEnclosures.getAreas().size() > 0) {
                text.append("\n");
                text.append(TrT.of("enclosure.message.sub_lands"));
                for (EnclosureArea area : subEnclosures.getAreas()) {
                    text.append(area.serialize(SerializationSettings.Name, player).styled(
                            style -> style.withColor(Formatting.GOLD)
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, area.serialize(SerializationSettings.Hover, player)))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/enclosure info " + area.getFullName()))));
                    text.append(" ");
                }
            }
            return text;
        }
        else {
            return super.serialize(settings, player);
        }
    }

    @Override
    public boolean hasPerm(@NotNull UUID uuid, @NotNull Permission perm) {
        return super.hasPerm(uuid, perm);
    }

    @Override
    public void onRemoveChild(@NotNull PermissionHolder child) {
        if (child instanceof EnclosureArea)
            ((EnclosureArea) child).setFather(null);
        subEnclosures.remove(child.getName());
        markDirty();
    }

    @Override
    public void addChild(@NotNull PermissionHolder child) {
        if (child instanceof EnclosureArea area) {
            area.setFather(this);
            subEnclosures.addArea(area);
            markDirty();
        }
        else {
            throw new IllegalArgumentException("child must be an instance of EnclosureArea");
        }
    }
}
