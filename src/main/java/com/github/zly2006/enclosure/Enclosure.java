package com.github.zly2006.enclosure;

import com.github.zly2006.enclosure.commands.Session;
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

import static com.github.zly2006.enclosure.EnclosureList.SUB_ENCLOSURES_KEY;

public class Enclosure extends EnclosureArea {
    EnclosureList subEnclosures;

    /**
     * Create an instance from nbt, note that this will not add the instance to the world. You need to call {@link EnclosureList#bind2world(ServerWorld)} to add it to the world.
     *
     * @param compound the nbt compound tag
     */
    public Enclosure(NbtCompound compound) {
        super(compound);
        // process sub enclosures
        NbtCompound sub = compound.getCompound(SUB_ENCLOSURES_KEY);
        subEnclosures = new EnclosureList(sub);
        subEnclosures.areas.forEach((name, land) -> land.father = this);
    }

    public Enclosure(Session session, String name) {
        super(session, name);
        subEnclosures = new EnclosureList();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound compound = super.writeNbt(nbt);
        NbtCompound sub = new NbtCompound();
        subEnclosures.writeNbt(sub);
        compound.put(SUB_ENCLOSURES_KEY, sub);
        return compound;
    }

    @Override
    public void setWorld(ServerWorld world) {
        super.setWorld(world);
        subEnclosures.setBoundWorld(world);
    }

    @Override
    public @Nullable EnclosureArea areaOf(BlockPos pos) {
        for (EnclosureArea area : subEnclosures.areas.values()) {
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
    public MutableText serialize(@NotNull SerializationSettings settings, @Nullable ServerPlayerEntity player) {
        if (settings == SerializationSettings.Full) {
            MutableText text = super.serialize(settings, player);
            if (subEnclosures.areas.size() > 0) {
                text.append("\n");
                text.append(TrT.of("enclosure.message.sub_lands"));
                for (EnclosureArea area : subEnclosures.areas.values()) {
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
    public void onRemoveChild(PermissionHolder child) {
        subEnclosures.remove(child.getName());
        markDirty();
    }

    @Override
    public void onAddChild(PermissionHolder child) {
        if (child instanceof EnclosureArea) {
            subEnclosures.areas.put(child.getName(), (EnclosureArea) child);
            markDirty();
        }
        else {
            throw new IllegalArgumentException("child must be an instance of EnclosureArea");
        }
    }
}
