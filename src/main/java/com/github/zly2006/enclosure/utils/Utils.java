package com.github.zly2006.enclosure.utils;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.command.EnclosureCommandKt;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

import static com.github.zly2006.enclosure.ServerMainKt.minecraftServer;

public class Utils {
    public static boolean isAnimal(Entity entity) {
        return entity instanceof PassiveEntity
                || entity instanceof IronGolemEntity
                || entity instanceof SnowGolemEntity
                || entity instanceof AllayEntity;
    }

    public static boolean isMonster(Entity entity) {
        return entity instanceof Monster;
    }

    public static <T extends Serializable2Text> Text pager(int size, int page, List<T> list, @Nullable ServerPlayerEntity player) {
        return pager(size, page, list, "", player);
    }

    public static <T extends Serializable2Text> Text pager(int size, int page, List<T> list, String command, @Nullable ServerPlayerEntity player) {
        int totalPage = (list.size() + size - 1) / size;
        if (page < 1 || page > totalPage) { // 如果选取页码超过范围限制，则采用第一页
            page = 1;
        }
        boolean firstPage = page == 1;
        boolean lastPage = page == totalPage;

        MutableText ret = TrT.of("enclosure.menu.page.0")
                .append(String.valueOf(page))
                .append(TrT.of("enclosure.menu.page.1"))
                .append(String.valueOf(totalPage))
                .append("\n");
        for (int i = size * (page - 1); i < size * page && i < list.size(); i++) {
            ret.append(list.get(i).serialize(Serializable2Text.SerializationSettings.Summarize, player));
            ret.append("\n");
        }
        ret.append(TrT.of("enclosure.menu.previous")
                .setStyle(Style.EMPTY.withColor(firstPage ? Formatting.GRAY : Formatting.DARK_GREEN)
                        .withHoverEvent(firstPage ? null : new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Page " + (page - 1))))
                        .withClickEvent(firstPage ? null : new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "%s %d".formatted(command, page - 1)))));
        ret.append("    ");
        ret.append(TrT.of("enclosure.menu.next")
                .setStyle(Style.EMPTY.withColor(lastPage ? Formatting.GRAY : Formatting.DARK_GREEN)
                        .withHoverEvent(lastPage ? null : new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Page " + (page + 1))))
                        .withClickEvent(lastPage ? null : new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "%s %d".formatted(command, page + 1)))));
        return ret;
    }

    public static int topYOf(ServerWorld world, int x, int z) {
        return topYOf(world, x, z, world.getTopY() - 1);
    }

    public static int topYOf(ServerWorld world, int x, int z, int startY) {
        int secondary = Integer.MAX_VALUE;
        int airCount = 0;
        for (int y = startY; y >= world.getBottomY(); y--) {
            BlockState state = world.getBlockState(new BlockPos(x, y, z));
            if (state.getMaterial().blocksMovement()) {
                if (state.isOf(Blocks.BEDROCK)) {
                    secondary = y;
                }
                if (airCount >= 2) {
                    return y + 1;
                }
                airCount = 0;
            } else {
                airCount++;
            }
        }
        return secondary + 1;
    }

    public static @Nullable String getNameByUUID(@NotNull UUID uuid) {
        return minecraftServer.getUserCache().getByUuid(uuid).map(GameProfile::getName).orElse(null);
    }

    public static @Nullable UUID getUUIDByName(@NotNull String name) {
        return minecraftServer.getUserCache().findByName(name).map(GameProfile::getId).orElse(null);
    }

    public static String camelCaseToSnakeCase(String camelCase) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i != 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean commonOnPlayerDamage(DamageSource source, BlockPos pos, World world, Permission permission) {
        if (world.isClient) {
            return true;
        }
        EnclosureArea area = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) world).getArea(pos);
        if (area != null) {
            area = area.areaOf(pos);
        }
        if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
            if (area != null && !area.hasPerm(attacker, permission)) {
                attacker.sendMessage(permission.getNoPermissionMsg(attacker));
                return false;
            }
        }
        return true;
    }

    public static boolean commonOnDamage(DamageSource source, BlockPos pos, World world, Permission permission) {
        if (world.isClient) {
            return true;
        }
        EnclosureArea area = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) world).getArea(pos);
        if (area != null) {
            area = area.areaOf(pos);
        }
        if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
            if (area != null && !area.hasPerm(attacker, permission)) {
                attacker.sendMessage(permission.getNoPermissionMsg(attacker));
                return false;
            }
        } else {
            return area == null || area.hasPubPerm(permission);
        }
        return true;
    }

    public static MutableText getDisplayNameByUUID(UUID uuid) {
        return getDisplayNameByUUID(uuid, "@GLOBAL");
    }

    public static MutableText getDisplayNameByUUID(UUID uuid, String serverName) {
        ServerPlayerEntity player = minecraftServer.getPlayerManager().getPlayer(uuid);
        if (player != null) {
            return player.getDisplayName().copy();
        }
        if (uuid.equals(EnclosureCommandKt.CONSOLE)) {
            return Text.literal(serverName);
        }
        String name = getNameByUUID(uuid);
        if (name != null) {
            return Text.literal(name);
        }
        return Text.literal(uuid.toString());
    }

    public static BlockPos toBlockPos(Vec3d vec3d) {
        return new BlockPos((int) vec3d.x, (int) vec3d.y, (int) vec3d.z);
    }

    public static BlockPos toBlockPos(double g, double h, double j) {
        return new BlockPos((int) g, (int) h, (int) j);
    }

    public static boolean mark4updateChecked(ServerWorld world, BlockPos pos) {
        if (world.getWorldBorder().contains(pos) && pos.getY() >= world.getBottomY() && pos.getY() < world.getTopY()) {
            world.getChunkManager().markForUpdate(pos);
            return true;
        }
        return false;
    }
}
