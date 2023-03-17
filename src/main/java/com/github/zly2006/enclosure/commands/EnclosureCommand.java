package com.github.zly2006.enclosure.commands;

import com.github.zly2006.enclosure.*;
import com.github.zly2006.enclosure.access.PlayerAccess;
import com.github.zly2006.enclosure.config.LandLimits;
import com.github.zly2006.enclosure.events.PaidPartEvents;
import com.github.zly2006.enclosure.exceptions.PermissionTargetException;
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler;
import com.github.zly2006.enclosure.network.EnclosureInstalledC2SPacket;
import com.github.zly2006.enclosure.network.NetworkChannels;
import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.Serializable2Text;
import com.github.zly2006.enclosure.utils.TrT;
import com.github.zly2006.enclosure.utils.Utils;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.Version;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.TeleportTarget;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

import static com.github.zly2006.enclosure.ServerMain.*;
import static com.github.zly2006.enclosure.utils.Permission.*;
import static com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class EnclosureCommand {
    public final static UUID CONSOLE = new UUID(0, 0);
    private static CommandNode<ServerCommandSource> node;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("enclosure")
            .then(literal("about")
                .executes(handleException(context -> {
                    ServerCommandSource source = context.getSource();
                    ServerPlayerEntity player = source.getPlayer();
                    source.sendMessage(TrT.of("enclosure.about.author"));
                    source.sendMessage(TrT.of("enclosure.about.translator"));
                    source.sendMessage(TrT.of("enclosure.about.team_page"));
                    source.sendMessage(TrT.of("enclosure.about.version.server").append(MOD_VERSION.getFriendlyString()));
                    if (source.getPlayer() != null && EnclosureInstalledC2SPacket.isInstalled(source.getPlayer())) {
                        Version version = EnclosureInstalledC2SPacket.clientVersion(source.getPlayer());
                        source.sendMessage(TrT.of("enclosure.about.version.client").append(version.getFriendlyString()));
                    }
                    source.sendMessage(TrT.of("enclosure.about.copyright"));
                    return 0;
                }))
            )
            .then(literal("admin")
                .then(literal("reload")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(literal("limits")
                        .executes(handleException(context -> {
                            try {
                                reloadLimits();
                                context.getSource().sendMessage(Text.literal("Reloaded"));
                            } catch (IOException e) {
                                context.getSource().sendError(Text.of(e.toString()));
                            }
                            return 0;
                        })))
                    .then(literal("common")
                        .executes(handleException(context -> {
                            try {
                                reloadCommon();
                                context.getSource().sendMessage(Text.literal("Reloaded, some changes may not take effect until server restart"));
                            } catch (IOException e) {
                                context.getSource().sendError(Text.of(e.toString()));
                            }
                            return 0;
                        }))))
                .then(literal("limit_exceeded")
                    .then(literal("count").executes(context -> {
                        Map<UUID, List<Enclosure>> map = new HashMap<>();
                        for (Enclosure e : Instance.getAllEnclosures()) {
                            List<Enclosure> l = map.get(e.getOwner());
                            if (l == null) {
                                l = new ArrayList<>();
                                l.add(e);
                                map.put(e.getOwner(), l);
                            } else {
                                l.add(e);
                            }
                        }
                        map.entrySet().stream().filter(e -> e.getValue().size() > limits.maxLands)
                            .sorted(Comparator.comparingInt(e -> -e.getValue().size()))
                            .map(entry -> Text.literal("Player ")
                                .append(Utils.getDisplayNameByUUID(entry.getKey()))
                                .append(Text.literal(" has %d enclosures: ".formatted(entry.getValue().size())))
                                .append(entry.getValue().stream()
                                    .map(en -> en.serialize(Name, context.getSource().getPlayer())
                                        .styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, en.serialize(Hover, context.getSource().getPlayer())))))
                                    .reduce(Text.empty(), (t1, t2) -> {
                                        Text.empty().append("");
                                        if (t1.getContent() == TextContent.EMPTY) {
                                            return t2;
                                        } else {
                                            return t1.append(Text.literal(", ")).append(t2);
                                        }
                                    })))
                            .forEach(context.getSource()::sendMessage);
                        return 0;
                    })))
                .then(literal("closest")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        assert player != null;
                        BlockPos pos = player.getBlockPos();
                        Optional<EnclosureArea> enclosure = Instance.getAllEnclosures(player.getWorld()).getAreas().stream()
                            .min(Comparator.comparingDouble(e -> e.distanceTo(pos).getSquaredDistance(Vec3i.ZERO)));
                        if (enclosure.isEmpty()) {
                            context.getSource().sendMessage(Text.literal("No enclosure found"));
                        } else {
                            context.getSource().sendMessage(Text.literal("Closest enclosure: " + enclosure.get().getFullName() + ", click to show info").styled(style ->
                                style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enclosure info " + enclosure.get().getFullName()))));
                        }
                        return 0;
                    }))
                .then(literal("perm-info")
                    .then(permissionArgument(Target.Both)
                        .executes(handleException(context -> {
                            Permission permission = get(StringArgumentType.getString(context, "permission"));
                            if (permission == null) {
                                error(TrT.of("enclosure.message.invalid_permission"), context);
                            }
                            MutableText text = Text.empty()
                                .append(Text.literal("Name: "))
                                .append(permission.getName())
                                .append(Text.literal(" Target: "))
                                .append(permission.getTarget().toString())
                                .append(Text.literal("\nDescription: "))
                                .append(permission.getDescription())
                                .append(Text.literal("\nDefault: "))
                                .append(String.valueOf(permission.getDefaultValue()))
                                .append(Text.literal("\nComponents: "))
                                .append(permission.getPermissions().stream().reduce("", (s, p) -> s.isEmpty() ? p : s + ", " + p));
                            context.getSource().sendMessage(text);
                            return 0;
                        }))))
                .then(literal("clients")
                    .executes(context -> {
                        EnclosureInstalledC2SPacket.installedClientMod.forEach((player, version) ->
                            context.getSource().sendMessage(player.getDisplayName().copy()
                                .append(" installed enclosure client version ")
                                .append(version.getFriendlyString())));
                        return 0;
                    })))
            .then(optionalEnclosure(literal("gui").requires(ServerCommandSource::isExecutedByPlayer), (area, context) -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                assert player != null;
                if (EnclosureInstalledC2SPacket.isInstalled(player)) {
                    PaidPartEvents.INSTANCE.open(player, area);
                }
            }))
            .then(literal("flags")
                .executes(handleException(context -> {
                    if (context.getSource().getPlayer() == null) {
                        PERMISSIONS.values().stream()
                            .sorted(Comparator.comparing(Permission::getName))
                            .map(perm -> perm.serialize(Full, null))
                            .forEach(context.getSource()::sendMessage);
                    } else {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        player.sendMessage(Utils.pager(5, 1, PERMISSIONS.values().stream()
                            .sorted(Comparator.comparing(Permission::getName))
                            .map(perm -> perm.serialize(Full, player))
                            .map(Serializable2Text::of)
                            .toList(), "/enclosure flags", player));
                    }
                    return 0;
                }))
                .then(argument("page", IntegerArgumentType.integer(0))
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        context.getSource().sendMessage(Utils.pager(5, IntegerArgumentType.getInteger(context, "page"), PERMISSIONS.values().stream()
                            .sorted(Comparator.comparing(Permission::getName))
                            .map(perm -> perm.serialize(Full, player))
                            .map(Serializable2Text::of)
                            .toList(), "/enclosure flags", player));
                        return 0;
                    })
                )
            )
            .then(literal("list")
                .executes(handleException(context -> {
                    context.getSource().sendMessage(Utils.pager(5, 1, Instance.getAllEnclosures(), "/enclosure list", context.getSource().getPlayer()));
                    return 0;
                }))
                .then(argument("page", IntegerArgumentType.integer(1))
                    .executes(handleException(context -> {
                        int page = IntegerArgumentType.getInteger(context, "page");
                        context.getSource().sendMessage(Utils.pager(5, page, Instance.getAllEnclosures(), "/enclosure list", context.getSource().getPlayer()));
                        return 0;
                    }))
                )
                .then(literal("user")
                    .then(offlinePlayerArgument()
                        .executes(handleException(context -> {
                            UUID uuid = Utils.getUUIDByName(StringArgumentType.getString(context, "player"));
                            if (uuid == null) {
                                error(TrT.of("enclosure.message.user_not_found"), context);
                            } else {
                                List<Enclosure> list = Instance.getAllEnclosures(uuid);
                                MutableText ret = TrT.of("enclosure.message.list.user", Utils.getDisplayNameByUUID(uuid), list.size());
                                list.forEach(e -> ret.append("\n").append(e.serialize(Summarize, context.getSource().getPlayer())));
                                context.getSource().sendMessage(ret);
                            }
                            return 0;
                        }))))
                .then(literal("world")
                    .then(argument("world", DimensionArgumentType.dimension())
                        .executes(handleException(context -> {
                            ServerWorld world = DimensionArgumentType.getDimensionArgument(context, "world");
                            EnclosureList enclosure = Instance.getAllEnclosures(world);
                            context.getSource().sendMessage(Utils.pager(5, 1, enclosure.getAreas().stream().toList(), "/enclosure list world " + world.getRegistryKey().getValue().toString(), context.getSource().getPlayer()));
                            return 0;
                        }))
                        .then(argument("page", IntegerArgumentType.integer(1))
                            .executes(handleException(context -> {
                                ServerWorld world = DimensionArgumentType.getDimensionArgument(context, "world");
                                int page = IntegerArgumentType.getInteger(context, "page");
                                EnclosureList enclosure = Instance.getAllEnclosures(world);
                                context.getSource().sendMessage(Utils.pager(5, page, enclosure.getAreas().stream().toList(), "/enclosure list world " + world.getRegistryKey().getValue().toString(), context.getSource().getPlayer()));
                                return 0;
                            }))
                        )
                    )
                ))
            .then(literal("help")
                .executes(handleException(context -> {
                    showHelp(context.getSource());
                    return 0;
                }))
                .then(argument("subcommand", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        for (CommandNode<ServerCommandSource> child : node.getChildren()) {
                            builder.suggest(child.getName());
                        }
                        return builder.buildFuture();
                    })
                    .executes(handleException(context -> {
                        showHelp(context.getSource(), StringArgumentType.getString(context, "subcommand"));
                        return 0;
                    }))))
            .then(literal("auto")
                .then(argument("name", StringArgumentType.word())
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .executes(handleException(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        assert player != null;
                        Session session = sessionOf(context.getSource());
                        int expandX = (limits.maxXRange - 1) / 2;
                        int expandZ = (limits.maxZRange - 1) / 2;
                        session.setWorld(player.getWorld());
                        session.setPos1(new BlockPos(player.getBlockPos().getX() - expandX, limits.minY, player.getBlockPos().getZ() - expandZ));
                        session.setPos2(new BlockPos(player.getBlockPos().getX() + expandX, Math.min(limits.maxY, limits.maxHeight + limits.minY - 1), player.getBlockPos().getZ() + expandZ));
                        session.setOwner(player.getUuid());
                        return createEnclosure(context);
                    }))))
            .then(literal("select")
                .then(optionalEnclosure(literal("land"), (area, context) -> {
                    Session session = sessionOf(context.getSource());
                    session.setWorld(area.getWorld());
                    session.setPos1(new BlockPos(area.getMinX(), area.getMinY(), area.getMinZ()));
                    session.setPos2(new BlockPos(area.getMaxX(), area.getMaxY(), area.getMaxZ()));
                    session.trySync();
                    context.getSource().sendMessage(TrT.of("enclosure.message.selection_updated"));
                }))
                .then(literal("pos1").then(argument("block", BlockPosArgumentType.blockPos())
                    .executes(handleException(context -> {
                        sessionOf(context.getSource()).setPos1(BlockPosArgumentType.getBlockPos(context, "block"))
                            .trySync();
                        return 0;
                    }))))
                .then(literal("pos2").then(argument("block", BlockPosArgumentType.blockPos())
                    .executes(handleException(context -> {
                        sessionOf(context.getSource()).setPos2(BlockPosArgumentType.getBlockPos(context, "block"))
                            .trySync();
                        return 0;
                    }))))
                .then(literal("world").then(argument("world", DimensionArgumentType.dimension())
                    .executes(handleException(context -> {
                        sessionOf(context.getSource()).setWorld(DimensionArgumentType.getDimensionArgument(context, "world"))
                            .trySync();
                        return 0;
                    }))))
                .then(literal("view")
                    .executes(handleException(context -> {
                        ServerPlayerEntity executor = context.getSource().getPlayer();
                        Session session = sessionOf(context.getSource());
                        checkSession(context);
                        session.trySync();
                        EnclosureArea intersectArea = session.intersect(Instance.getAllEnclosures(session.world));
                        ServerCommandSource source = context.getSource();
                        source.sendMessage(TrT.of("enclosure.message.select.from")
                            .append((blockPos2string(session.pos1)))
                            .append(TrT.of("enclosure.message.select.to"))
                            .append((blockPos2string(session.pos2)))
                            .append(TrT.of("enclosure.message.select.world"))
                            .append((session.world.getRegistryKey().getValue().toString()))
                        );
                        source.sendMessage(TrT.of("enclosure.message.total_size")
                            .append(String.valueOf(session.size()))
                        );
                        if (intersectArea != null) {
                            source.sendMessage(TrT.of("enclosure.message.intersected")
                                .append(intersectArea.serialize(Name, context.getSource().getPlayer()))
                            );
                        }
                        return 0;
                    })))
                .then(optionalEnclosure(literal("resize"), (area, context) -> {
                    Session session = sessionOf(context.getSource());
                    checkSession(context);
                    session.pos1 = new BlockPos(area.getMinX(), area.getMinY(), area.getMinZ());
                    session.pos2 = new BlockPos(area.getMaxX(), area.getMaxY(), area.getMaxZ());
                    area.setWorld(area.getWorld());
                    if (context.getSource().getPlayer() != null) {
                        session.sync(context.getSource().getPlayer());
                    }
                    context.getSource().sendMessage(TrT.of("enclosure.message.selection_updated"));
                }))
                .then(literal("shrink")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .then(argument("amount", IntegerArgumentType.integer(1))
                        .executes(handleException(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayer();
                            assert executor != null;
                            checkSession(context);
                            Session session = sessionOf(context.getSource());
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            Direction direction = Direction.getEntityFacingOrder(executor)[0];
                            session.shrink(direction, amount);
                            session.trySync();
                            context.getSource().sendFeedback(TrT.of("enclosure.message.shrunk")
                                    .append(String.valueOf(amount))
                                    .append(TrT.of("enclosure.message.resized." + direction.getName()))
                                , false);
                            return 0;
                        }))))
                .then(literal("shift")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .then(argument("amount", IntegerArgumentType.integer(1))
                        .executes(handleException(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayer();
                            assert executor != null;
                            checkSession(context);
                            Session session = sessionOf(context.getSource());
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            Direction direction = Direction.getEntityFacingOrder(executor)[0];
                            session.shift(direction, amount);
                            session.trySync();
                            session.trySync();
                            context.getSource().sendFeedback(TrT.of("enclosure.message.shifted")
                                    .append(String.valueOf(amount))
                                    .append(TrT.of("enclosure.message.resized." + direction.getName()))
                                , false);
                            return 0;
                        }))))
                .then(literal("expand")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .then(argument("amount", IntegerArgumentType.integer(1))
                        .executes(handleException(context -> {
                            @SuppressWarnings("DuplicatedCode")
                            ServerPlayerEntity executor = context.getSource().getPlayer();
                            assert executor != null;
                            checkSession(context);
                            Session session = sessionOf(context.getSource());
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            Direction direction = Direction.getEntityFacingOrder(executor)[0];
                            session.expand(direction, amount);
                            session.trySync();
                            context.getSource().sendFeedback(TrT.of("enclosure.message.expanded")
                                    .append(String.valueOf(amount))
                                    .append(TrT.of("enclosure.message.resized." + direction.getName().toLowerCase()))
                                , false);
                            return 0;
                        }))))
                .then(literal("max_height")
                    .executes(handleException(context -> {
                        checkSession(context);
                        Session session = sessionOf(context.getSource());
                        session.setPos1(new BlockPos(session.pos1.getX(), limits.minY, session.pos1.getZ()));
                        session.setPos2(new BlockPos(session.pos2.getX(), limits.maxY, session.pos2.getZ()));
                        session.trySync();
                        return 0;
                    })))
                .then(literal("max_square")
                    .executes(handleException(context -> {
                        Session session = sessionOf(context.getSource());
                        checkSession(context);
                        int centerX = (session.pos1.getX() + session.pos2.getX()) / 2;
                        int centerZ = (session.pos1.getZ() + session.pos2.getZ()) / 2;
                        int expandX = (limits.maxXRange - Math.abs(session.getPos1().getX() - session.getPos2().getX()) - 1) / 2;
                        int expandZ = (limits.maxZRange - Math.abs(session.getPos1().getZ() - session.getPos2().getZ()) - 1) / 2;
                        if (session.getPos1().getX() < session.getPos2().getX()) {
                            session.setPos1(new BlockPos(centerX - expandX, session.getPos1().getY(), session.getPos1().getZ()));
                            session.setPos2(new BlockPos(centerX + expandX, session.getPos2().getY(), session.getPos2().getZ()));
                        } else {
                            session.setPos1(new BlockPos(centerX + expandX, session.getPos1().getY(), session.getPos1().getZ()));
                            session.setPos2(new BlockPos(centerX - expandX, session.getPos2().getY(), session.getPos2().getZ()));
                        }
                        if (session.getPos1().getZ() < session.getPos2().getZ()) {
                            session.setPos1(new BlockPos(session.getPos1().getX(), session.getPos1().getY(), centerZ - expandZ));
                            session.setPos2(new BlockPos(session.getPos2().getX(), session.getPos2().getY(), centerZ + expandZ));
                        } else {
                            session.setPos1(new BlockPos(session.getPos1().getX(), session.getPos1().getY(), centerZ + expandZ));
                            session.setPos2(new BlockPos(session.getPos2().getX(), session.getPos2().getY(), centerZ - expandZ));
                        }
                        session.trySync();
                        return 0;
                    })))
                .then(optionalEnclosure(literal("resize").requires(source -> source.hasPermissionLevel(2)),
                    (area, context) -> {
                        Session session = sessionOf(context.getSource());
                        checkSession(context);
                        int minX = Math.min(session.pos1.getX(), session.pos2.getX());
                        int minY = Math.min(session.pos1.getY(), session.pos2.getY());
                        int minZ = Math.min(session.pos1.getZ(), session.pos2.getZ());
                        int maxX = Math.max(session.pos1.getX(), session.pos2.getX());
                        int maxY = Math.max(session.pos1.getY(), session.pos2.getY());
                        int maxZ = Math.max(session.pos1.getZ(), session.pos2.getZ());
                        area.setMinX(minX);
                        area.setMinY(minY);
                        area.setMinZ(minZ);
                        area.setMaxX(maxX);
                        area.setMaxY(maxY);
                        area.setMaxZ(maxZ);
                        context.getSource().sendFeedback(TrT.of("enclosure.message.resized")
                            .append(area.serialize(Name, context.getSource().getPlayer())), false);
                    })))
            .then(literal("remove")
                .then(landArgument()
                    .executes(handleException(context -> {
                        EnclosureArea res = getEnclosure(context);
                        if (!res.isOwner(context.getSource())) {
                            error(ADMIN.getNoPermissionMsg(context.getSource().getPlayer()));
                        }
                        ConfirmManager.confirm(context.getSource().getPlayer(), () -> {
                            EnclosureList list = Instance.getAllEnclosures(res.getWorld());
                            boolean success;
                            if (res.getFather() != null) {
                                res.setFather(null);
                                success = true;
                            } else {
                                success = list.remove(res.getName());
                                list.markDirty();
                            }
                            if (success) {
                                context.getSource().sendMessage(TrT.of("enclosure.message.deleted")
                                    .append(res.getFullName())
                                );
                                LOGGER.info(context.getSource().getName() + " removed " + res.getFullName());
                            } else {
                                error(TrT.of("enclosure.message.no_enclosure"), context);
                            }
                        });
                        return 0;
                    }))))
            .then(literal("rename")
                .then(landArgument()
                    .then(argument("name", StringArgumentType.string())
                        .executes(handleException(context -> {
                            EnclosureArea res = getEnclosure(context);
                            String name = StringArgumentType.getString(context, "name");
                            if (!context.getSource().hasPermissionLevel(4) &&
                                context.getSource().getPlayer() != null &&
                                !res.isOwner(context.getSource())) {
                                error(ADMIN.getNoPermissionMsg(context.getSource().getPlayer()));
                            }
                            if (Instance.getEnclosure(name) != null) {
                                error(TrT.of("enclosure.message.name_in_use"), context);
                            }
                            EnclosureList list = Optional.ofNullable(res.getFather())
                                .filter(father -> father instanceof Enclosure)
                                .map(x -> ((Enclosure) x).getSubEnclosures())
                                .orElse(Instance.getAllEnclosures(res.getWorld()));
                            list.remove(res.getName());
                            res.setName(name);
                            list.addArea(res);
                            Instance.getAllEnclosures(res.getWorld())
                                .markDirty();
                            return 0;
                        })))))
            .then(literal("tp")
                .requires(ServerCommandSource::isExecutedByPlayer)
                .then(landArgument()
                    .executes(handleException(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        assert player != null;
                        long lastTeleportTimeSpan = System.currentTimeMillis() - ((PlayerAccess) player).getLastTeleportTime();
                        if (!context.getSource().hasPermissionLevel(4) && commonConfig.teleportCooldown > 0 && lastTeleportTimeSpan < commonConfig.teleportCooldown) {
                            error(TrT.of("enclosure.message.teleport_too_fast", "%.1f".formatted((commonConfig.teleportCooldown - lastTeleportTimeSpan) / 1000.0)), context);
                        }
                        ((PlayerAccess) player).setLastTeleportTime(System.currentTimeMillis());
                        EnclosureArea area = getEnclosure(context);
                        if (commonConfig.showTeleportWarning) {
                            ServerWorld world = area.getWorld();
                            BlockPos pos = Utils.toBlockPos(area.getTeleportPos());
                            BlockState down = world.getBlockState(pos.down());
                            BlockState state = world.getBlockState(pos);
                            BlockState up = world.getBlockState(pos.up());
                            if (!down.getMaterial().blocksMovement() ||
                                (state.getMaterial().blocksMovement() && up.getMaterial().blocksMovement())) {
                                context.getSource().sendMessage(TrT.of("enclosure.message.teleport_warning")

                                    .formatted(Formatting.YELLOW));
                                ConfirmManager.confirm(context.getSource().getPlayer(), () -> area.teleport(player));
                            } else {
                                area.teleport(player);
                            }
                        } else {
                            area.teleport(player);
                        }
                        return 0;
                    }))))
            .then(literal("limits")
                .executes(handleException(context -> {
                    LandLimits limits = ServerMain.limits;
                    MutableText translatable = TrT.of("enclosure.message.limit.header");
                    Arrays.stream(limits.getClass().getFields()).map(field -> {
                            try {
                                return Text.literal("\n")
                                    .append(TrT.limit(field).append(": ").styled(style -> style.withColor(Formatting.GOLD)))
                                    .append(field.get(limits).toString());
                            } catch (IllegalAccessException ignored) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .forEach(translatable::append);
                    context.getSource().sendMessage(translatable);
                    return 0;
                }))
            )
            .then(literal("create")
                .then(argument("name", StringArgumentType.word())
                    .executes(handleException(EnclosureCommand::createEnclosure))))
            .then(optionalEnclosure(literal("info"),
                (area, context) -> {
                    MutableText text = area.serialize(BarredFull, context.getSource().getPlayer());
                    if (context.getSource().getPlayer() != null &&
                        EnclosureInstalledC2SPacket.isInstalled(context.getSource().getPlayer())) {
                        text.append(Text.literal("\n(Open GUI)")
                            .styled(style -> style.withColor(Formatting.AQUA)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enclosure.message.suggest_gui")))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enclosure gui " + area.getFullName()))));
                    }
                    context.getSource().sendMessage(text);
                }
            ))
            .then(optionalEnclosure(literal("add_user"), (area, context) -> {
                ServerPlayerEntity executor = context.getSource().getPlayer();
                assert executor != null;
                UUID uuid = Utils.getUUIDByName(StringArgumentType.getString(context, "player"));
                if (uuid == null) {
                    error(TrT.of("enclosure.message.player_not_found"), context);
                }
                if (context.getSource().hasPermissionLevel(4) || area.hasPerm(executor, ADMIN)) {
                    area.setPermission(context.getSource(), uuid, TRUSTED, true);
                    context.getSource().sendFeedback(TrT.of("enclosure.message.added_user", Utils.getDisplayNameByUUID(uuid)), true);
                } else {
                    error(ADMIN.getNoPermissionMsg(executor));
                }
            }, (node, command) -> node.then(offlinePlayerArgument().executes(command))))
            .then(optionalEnclosure(literal("set"),
                string -> (area, context, extra) -> {
                    ServerPlayerEntity playerExecutor = context.getSource().getPlayer();
                    if (playerExecutor != null && !Boolean.TRUE.equals(area.hasPerm(playerExecutor, ADMIN))) {
                        error(ADMIN.getNoPermissionMsg(playerExecutor));
                    }
                    Boolean value = getOptionalBoolean(context).orElse(null);
                    Permission permission = get(StringArgumentType.getString(context, "permission"));
                    if (permission == null) {
                        context.getSource().sendMessage(TrT.of("enclosure.message.invalid_permission"));
                        return;
                    }
                    MutableText warning = null;
                    if (permission == ADMIN) {
                        warning = TrT.of("enclosure.message.setting_admin").formatted(Formatting.RED);
                    } else if (permission.getPermissions().size() > 1) {
                        warning = TrT.of("enclosure.message.setting_multiple").formatted(Formatting.RED);
                    }
                    if (warning != null) {
                        if (playerExecutor != null && playerExecutor.currentScreenHandler instanceof EnclosureScreenHandler) {
                            PacketByteBuf buf = PacketByteBufs.create();
                            buf.writeText(warning);
                            ServerPlayNetworking.send(playerExecutor, NetworkChannels.CONFIRM, buf);
                            ConfirmManager.runnableMap.put(playerExecutor.getUuid(), () ->
                                setPermissionFromCommand(string, area, context, playerExecutor, value, permission));
                        } else {
                            context.getSource().sendMessage(warning);
                            ConfirmManager.confirm(playerExecutor, () ->
                                setPermissionFromCommand(string, area, context, playerExecutor, value, permission));
                            if (playerExecutor != null && playerExecutor.currentScreenHandler instanceof EnclosureScreenHandler) {
                                playerExecutor.closeHandledScreen();
                            }
                        }
                    } else {
                        setPermissionFromCommand(string, area, context, playerExecutor, value, permission);
                    }
                },
                (node, command, string) -> {
                    switch (string) {
                        case "global" -> node.then(literal("global")
                            .then(permissionArgument(Target.Enclosure)
                                .then(optionalBooleanArgument()
                                    .executes(command))));
                        case "user" -> node.then(literal("user")
                            .then(offlinePlayerArgument()
                                .then(permissionArgument(Target.Player)
                                    .then(optionalBooleanArgument()
                                        .executes(command)))));
                        case "uuid" -> node.then(literal("uuid")
                            .then(argument("uuid", UuidArgumentType.uuid())
                                .then(permissionArgument(Target.Both)
                                    .then(optionalBooleanArgument()
                                        .executes(command)))));
                    }
                },
                List.of("global", "user", "uuid")
            ))
            .then(optionalEnclosure(literal("check"),
                string -> (area, context, extra) -> {
                    boolean value;
                    Permission permission = get(StringArgumentType.getString(context, "permission"));
                    if (permission == null) {
                        throw new SimpleCommandExceptionType(TrT.of("enclosure.message.invalid_permission")).create();
                    }
                    UUID uuid;
                    switch (string) {
                        case "global" -> {
                            uuid = CONSOLE;
                            value = area.hasPubPerm(permission);
                        }
                        case "uuid" -> {
                            uuid = UuidArgumentType.getUuid(context, "uuid");
                            value = area.hasPerm(uuid, permission);
                        }
                        case "user" -> {
                            uuid = Utils.getUUIDByName(StringArgumentType.getString(context, "player"));
                            if (uuid == null) {
                                error(TrT.of("enclosure.message.user_not_found"), context);
                            }
                            value = area.hasPerm(uuid, permission);
                        }
                        default -> {
                            return;
                        }
                    }
                    context.getSource().sendMessage(
                        TrT.of("enclosure.message.check_permission",
                            Utils.getDisplayNameByUUID(uuid).formatted(Formatting.GOLD),
                            permission.serialize(Summarize, context.getSource().getPlayer()).formatted(Formatting.GOLD),
                            area.serialize(Name, context.getSource().getPlayer()).formatted(Formatting.GOLD),
                            Text.literal(value ? "true" : "false").formatted(value ? Formatting.GREEN : Formatting.RED))
                    );
                },
                (node, command, string) -> {
                    switch (string) {
                        case "global" -> node.then(literal("global")
                            .then(permissionArgument(Target.Enclosure)
                                .executes(command)));
                        case "user" -> node.then(literal("user")
                            .then(offlinePlayerArgument()
                                .then(permissionArgument(Target.Player)
                                    .executes(command))));
                        case "uuid" -> node.then(literal("uuid")
                            .then(argument("uuid", UuidArgumentType.uuid())
                                .then(permissionArgument(Target.Both)
                                    .executes(command))));
                    }
                },
                List.of("global", "user", "uuid")
            ))
            .then(literal("give")
                .then(landArgument()
                    .then(offlinePlayerArgument()
                        .executes(handleException(context -> {
                            EnclosureArea res = getEnclosure(context);
                            UUID uuid = Utils.getUUIDByName(StringArgumentType.getString(context, "player"));
                            if (uuid == null) {
                                error(TrT.of("enclosure.message.user_not_found"), context);
                            }
                            PlayerEntity target = minecraftServer.getPlayerManager().getPlayer(uuid);
                            if (!context.getSource().hasPermissionLevel(4) &&
                                context.getSource().getPlayer() != null &&
                                !res.isOwner(context.getSource())) {
                                error(ADMIN.getNoPermissionMsg(context.getSource().getPlayer()));
                            }
                            ConfirmManager.confirm(context.getSource().getPlayer(), () -> {
                                LandLimits limitsOfReceiver = limits;
                                if (!context.getSource().hasPermissionLevel(4) &&
                                    context.getSource().getPlayer() != null) {
                                    long count = Instance.getAllEnclosures(uuid).size();
                                    if (count > limitsOfReceiver.maxLands) {
                                        error(TrT.of("enclosure.message.rcle.receiver")
                                            .append(String.valueOf(limitsOfReceiver.maxLands)), context);
                                    }
                                }
                                res.setPermission(context.getSource(), res.getOwner(), ALL, null);
                                res.setOwner(uuid);
                                res.setPermission(context.getSource(), uuid, ALL, true);
                                context.getSource().sendFeedback(TrT.of("enclosure.message.given.1")
                                        .append(res.serialize(Name, context.getSource().getPlayer()))
                                        .append(TrT.of("enclosure.message.given.2"))
                                        .append(Utils.getDisplayNameByUUID(uuid)),
                                    true);
                                if (target != null) {
                                    target.sendMessage(TrT.of("enclosure.message.received.1")
                                        .append(res.serialize(Name, context.getSource().getPlayer()))
                                        .append(TrT.of("enclosure.message.received.2"))
                                        .append(Utils.getDisplayNameByUUID(res.getOwner())));
                                }
                            });
                            return 0;
                        })))))
            .then(optionalEnclosure(literal("settp"),
                (area, context) -> {
                    ServerPlayerEntity executor = context.getSource().getPlayer();
                    if (executor == null || (!context.getSource().hasPermissionLevel(4) && !area.hasPerm(executor, ADMIN))) {
                        error(ADMIN.getNoPermissionMsg(executor));
                    }
                    if (!area.isInner(executor.getBlockPos())) {
                        error(TrT.of("enclosure.message.res_settp_pos_error"), context);
                    }
                    new TeleportTarget(executor.getPos(), Vec3d.ZERO, executor.headYaw, executor.getPitch());
                    area.setTeleportPos(executor.getPos(), executor.headYaw, executor.getPitch());
                    context.getSource().sendMessage(TrT.of("enclosure.message.change_teleport_position.0")
                        .append(area.serialize(Name, context.getSource().getPlayer()))
                        .append(TrT.of("enclosure.message.change_teleport_position.1"))
                        .append("[%f, %f, %f](yaw: %f, pitch: %f)".formatted(
                            executor.getPos().x,
                            executor.getPos().y,
                            executor.getPos().z,
                            executor.headYaw,
                            executor.getPitch()
                        )));
                }))
            .then(literal("message")
                // Common message
                .then(optionalEnclosure(literal("set"),
                    string -> (area, context, extra) -> {
                        if (!hasAdmin(area, context)) {
                            error(ADMIN.getNoPermissionMsg(context.getSource().getPlayer()));
                        }
                        String message = StringArgumentType.getString(context, "message")
                            .replace("&", "§")
                            .replace("§§", "&");
                        if (message.equals("#default")) {
                            message = "";
                        }
                        setEnclosureMessage(string, area, context, message);
                    }, (node, runnable, extra) -> node.then(literal(extra)
                        .then(argument("message", StringArgumentType.greedyString())
                            .suggests((context, builder) ->
                                builder.suggest(commonConfig.defaultEnterMessage.replace("&", "&&").replace("§", "&"))
                                    .suggest(commonConfig.defaultLeaveMessage.replace("&", "&&").replace("§", "&"))
                                    .suggest("#none")
                                    .suggest("#default")
                                    .buildFuture())
                            .executes(runnable))), List.of("enter", "leave")))
                // Rich message
                .then(optionalEnclosure(literal("rich").requires(s -> commonConfig.allowRichMessage),
                    string -> (area, context, extra) -> {
                        if (!hasAdmin(area, context)) {
                            error(TrT.of("enclosure.message.no_permission"), context);
                        }
                        String message = Text.Serializer.toJson(TextArgumentType.getTextArgument(context, "message"));
                        setEnclosureMessage(string, area, context, "#rich:" + message);
                    }, (node, runnable, extra) -> node.then(literal(extra)
                        .then(argument("message", TextArgumentType.text())
                            .requires(source -> commonConfig.allowRichMessage || source.hasPermissionLevel(4))
                            .executes(runnable))), List.of("enter", "leave")))
                // View message
                .then(optionalEnclosure(literal("view"),
                    string -> (area, context, extra) -> {
                        String msg;
                        switch (string) {
                            case "enter" -> msg = area.getEnterMessage()
                                .replace("&", "&&")
                                .replace("§", "&");
                            case "leave" -> msg = area.getLeaveMessage()
                                .replace("&", "&&")
                                .replace("§", "&");
                            default -> {
                                Text text = TrT.of("enclosure.message.unexpected_info");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(text), text);
                            }
                        }
                        if (msg.equals("#none")) {
                            context.getSource().sendMessage(TrT.of("enclosure.message.null_message", area.getFullName()));
                        } else if (msg.isEmpty()) {
                            context.getSource().sendMessage(TrT.of("enclosure.message.default_message"));
                        } else {
                            Text ctp = TrT.of("enclosure.message.click_to_copy").styled(style -> style.withColor(Formatting.AQUA));
                            context.getSource().sendMessage(Text.literal(msg).append(" ").append(ctp).styled(style -> style
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, ctp))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, msg))));
                        }
                    }, (node, runnable, extra) -> node.then(literal(extra).executes(runnable)),
                    List.of("enter", "leave"))))
            .then(literal("subzone")
                .then(argument("name", StringArgumentType.string())
                    .executes(handleException(context -> {
                        ServerPlayerEntity executor = context.getSource().getPlayer();
                        String name = StringArgumentType.getString(context, "name");
                        if (name.length() > commonConfig.maxEnclosureNameLength) {
                            error(TrT.of("enclosure.message.res_name_too_long"), context);
                        }
                        Session session = sessionOf(context.getSource());
                        EnclosureList list = Instance.getAllEnclosures(sessionOf(context.getSource()).getWorld());
                        EnclosureArea area = new Enclosure(session, name);
                        Optional<Enclosure> enclosure = list.getAreas().stream()
                            .filter(res -> res.includesArea(area))
                            .map(res -> ((Enclosure) res))
                            .findFirst();
                        if (enclosure.isEmpty()) {
                            error(TrT.of("enclosure.message.no_father_enclosure"), context);
                        }
                        if (enclosure.get().getSubEnclosures().getAreas().stream().anyMatch(a -> a.getName().equalsIgnoreCase(name))) {
                            error(TrT.of("enclosure.message.name_in_use"), context);
                        }
                        if (executor != null && !enclosure.get().isOwner(context.getSource())) {
                            error(ADMIN.getNoPermissionMsg(executor));
                        }
                        EnclosureArea intersectArea = sessionOf(context.getSource()).intersect(enclosure.get().getSubEnclosures());
                        if (intersectArea != null) {
                            error(TrT.of("enclosure.message.intersected")
                                .append(intersectArea.serialize(Name, context.getSource().getPlayer())), context);
                        }
                        LandLimits limits = ServerMain.limits;
                        if (!context.getSource().hasPermissionLevel(4)) {
                            checkSessionSize(session, context);
                            long count = enclosure.get().getSubEnclosures().getAreas().size();
                            if (count > limits.maxSubLands) {
                                error(TrT.of("enclosure.message.scle").append(Text.literal(String.valueOf(limits.maxSubLands))), context);
                            }
                        }
                        area.setWorld(session.getWorld()); // setFather 要求在同一个世界
                        area.setFather(enclosure.get()); // 自动化了，调用一次即可
                        list.markDirty();
                        context.getSource().sendMessage(TrT.of("enclosure.message.created")
                            .append(area.serialize(Name, context.getSource().getPlayer()))
                        );
                        LOGGER.info("Created subzone {} by {}", area.getFullName(), context.getSource().getName());
                        return 0;
                    }))
                )
            )
            .then(literal("experimental")
                .requires(source -> source.hasPermissionLevel(4))
                .then(literal("backup")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(literal("save")
                        .then(landArgument()
                            .executes(handleException(context -> {
                                EnclosureArea area = getEnclosure(context);
                                Path enclosuresBackup = minecraftServer.getSavePath(WorldSavePath.ROOT)
                                    .resolve("enclosures_backup")
                                    .resolve(area.getFullName());
                                try {
                                    Files.createDirectories(enclosuresBackup);
                                    Path path = enclosuresBackup.resolve(new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".dat");
                                    NbtIo.writeCompressed(area.saveTerrain(), path.toFile());
                                    context.getSource().sendMessage(TrT.of("enclosure.message.saved_backup", path.getFileName()));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                return 0;
                            }))))
                    .then(literal("load")
                        .then(landArgument()
                            .then(argument("file", StringArgumentType.greedyString())
                                .suggests((context, builder) -> {
                                    try {
                                        EnclosureArea area = getEnclosure(context.getChild());
                                        File enclosuresBackup = minecraftServer.getSavePath(WorldSavePath.ROOT)
                                            .resolve("enclosures_backup")
                                            .resolve(area.getFullName()).toFile();
                                        if (enclosuresBackup.exists()) {
                                            for (File file : enclosuresBackup.listFiles()) {
                                                if (file.isFile()) {
                                                    builder.suggest(file.getName());
                                                }
                                            }
                                        }
                                    } catch (CommandSyntaxException e) {
                                        e.printStackTrace();
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(handleException(context -> {
                                    String filename = StringArgumentType.getString(context, "file");
                                    EnclosureArea area = getEnclosure(context);
                                    File enclosuresBackup = minecraftServer.getSavePath(WorldSavePath.ROOT)
                                        .resolve("enclosures_backup")
                                        .resolve(area.getFullName())
                                        .resolve(filename).toFile();
                                    if (enclosuresBackup.exists()) {
                                        try {
                                            NbtCompound nbt = NbtIo.readCompressed(enclosuresBackup);
                                            area.rollback(nbt);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        context.getSource().sendError(TrT.of("enclosure.message.backup_file_not_found"));
                                    }
                                    return 1;
                                }))))))
                .then(literal("groups")
                    .then(literal("list").executes(context -> {
                        Instance.getEnclosureGroups().getGroups().stream()
                            .map(group -> group.serialize(Name, context.getSource().getPlayer()))
                            .forEach(context.getSource()::sendMessage);
                        return 0;
                    }))
                    .then(literal("info").then(argument("group", StringArgumentType.word()).executes(context -> {
                        String name = StringArgumentType.getString(context, "group");
                        EnclosureGroup group = Instance.getEnclosureGroups().getGroup(name);
                        if (group == null) {
                            error(TrT.of("enclosure.message.null_group"), context);
                        }
                        context.getSource().sendMessage(group.serialize(Full, context.getSource().getPlayer()));
                        return 0;
                    })))
                    .then(literal("create").then(argument("name", StringArgumentType.word()).executes(context -> {
                        String name = StringArgumentType.getString(context, "name");
                        if (Instance.getEnclosureGroups().getGroup(name) != null) {
                            error(TrT.of("enclosure.message.duplicate_group"), context);
                        }
                        EnclosureGroup group = new EnclosureGroup();
                        group.setName(name);
                        group.setOwner(uuidOf(context.getSource()));
                        Instance.getEnclosureGroups().addGroup(group);
                        Instance.getEnclosureGroups().markDirty();
                        // todo:检查数量上限
                        context.getSource().sendFeedback(TrT.of("enclosure.message.created_group")
                            .append(group.serialize(Name, context.getSource().getPlayer())), false);
                        return 0;
                    })))
                    .then(literal("rename")
                        .then(argument("group", StringArgumentType.word())
                            .then(argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    // todo:检查名称合法性
                                    String name = StringArgumentType.getString(context, "name");
                                    String oldName = StringArgumentType.getString(context, "group");
                                    // todo: 重名
                                    // todo: 设置map（remove & put）
                                    // todo: markDirty
                                    return 1;
                                }))))
                    .then(literal("give")/*todo: 参考领地的give*/)
                    .then(literal("delete")/*todo: res remove*/)
                    .then(literal("check")
                        .then(argument("group", StringArgumentType.word())
                            .then(literal("global")
                                .then(permissionArgument(Permission.Target.Enclosure)
                                    .executes(context -> {
                                        return 1;
                                    })))
                            .then(literal("uuid")
                                .then(argument("uuid", UuidArgumentType.uuid())
                                    .then(permissionArgument(Permission.Target.Enclosure)
                                        .executes(context -> {
                                            return 1;
                                        }))))
                            .then(literal("user")
                                .then(offlinePlayerArgument()
                                    .then(permissionArgument(Permission.Target.Enclosure)
                                        .executes(context -> {
                                            return 1;
                                        }))))))
                    .then(literal("set")
                        .then(argument("group", StringArgumentType.word())
                            .then(literal("global")
                                .then(permissionArgument(Permission.Target.Enclosure)
                                    .then(optionalBooleanArgument()
                                        .executes(context -> {
                                            return 1;
                                        }))))
                            .then(literal("uuid")
                                .then(argument("uuid", UuidArgumentType.uuid())
                                    .then(permissionArgument(Permission.Target.Enclosure)
                                        .then(optionalBooleanArgument()
                                            .executes(context -> {
                                                return 1;
                                            })))))
                            .then(literal("user")
                                .then(offlinePlayerArgument()
                                    .then(permissionArgument(Permission.Target.Enclosure)
                                        .then(optionalBooleanArgument()
                                            .executes(context -> {
                                                return 1;
                                            })))))))
                    .then(literal("add_enclosure")/*todo:权限要求（同下）：group的admin权限+，领地owner(isOwner函数)*/)
                    .then(literal("remove_enclosure")))
            )
        );
        node = dispatcher.getRoot().getChild("enclosure");
    }

    private static boolean hasAdmin(EnclosureArea area, CommandContext<ServerCommandSource> context) {
        return context.getSource().hasPermissionLevel(4)
            || context.getSource().getPlayer() == null
            || area.hasPerm(context.getSource().getPlayer(), ADMIN);
    }

    private static RequiredArgumentBuilder<ServerCommandSource, String> offlinePlayerArgument() {
        return argument("player", StringArgumentType.string())
            .suggests(PaidPartEvents.INSTANCE::suggestPlayerNames);
    }

    private static UUID getOfflinePlayerUUID(String name) {
        return minecraftServer.getUserCache().findByName(name).map(GameProfile::getId).orElse(null);
    }

    private static void setPermissionFromCommand(@NotNull String type, EnclosureArea area, CommandContext<ServerCommandSource> context, ServerPlayerEntity playerExecutor, Boolean value, Permission permission) throws CommandSyntaxException {
        UUID uuid;
        switch (type) {
            case "global" -> {
                area.setPermission(context.getSource(), CONSOLE, permission, value);
                uuid = CONSOLE;
            }
            case "uuid" -> {
                uuid = UuidArgumentType.getUuid(context, "uuid");
                area.setPermission(context.getSource(), uuid, permission, value);
            }
            case "user" -> {
                uuid = Utils.getUUIDByName(StringArgumentType.getString(context, "player"));
                if (uuid == null) {
                    error(TrT.of("enclosure.message.user_not_found"), context);
                }
                area.setPermission(context.getSource(), uuid, permission, value);
            }
            default -> throw new SimpleCommandExceptionType(Text.literal("Invalid argument")).create();
        }

        area.markDirty();
        context.getSource().sendFeedback(TrT.of("enclosure.message.set_permission",
            Utils.getDisplayNameByUUID(uuid),
            permission.serialize(Summarize, context.getSource().getPlayer()),
            value == null ? "none" : value.toString(),
            area.getFullName()).formatted(Formatting.YELLOW), true);
    }

    private static void setEnclosureMessage(@NotNull String string, EnclosureArea area, CommandContext<ServerCommandSource> context, String message) throws CommandSyntaxException {
        switch (string) {
            case "enter" -> area.setEnterMessage(message.equals(ServerMain.commonConfig.defaultEnterMessage) ? "" : message);
            case "leave" -> area.setLeaveMessage(message.equals(ServerMain.commonConfig.defaultLeaveMessage) ? "" : message);
            default -> {
                Text text = TrT.of("enclosure.message.unexpected_info");
                throw new CommandSyntaxException(new SimpleCommandExceptionType(text), text);
            }
        }
        Instance.getAllEnclosures(area.getWorld())
            .markDirty();
        context.getSource().sendMessage(TrT.of("enclosure.message.set_message", string)
        );
    }

    static ArgumentBuilder<ServerCommandSource, ?> optionalEnclosure(@NotNull ArgumentBuilder<ServerCommandSource, ?> node, RunnableWithEnclosure runnable) {
        return optionalEnclosure(node, runnable, Builder4ArgumentNodeBuilder.UNIT);
    }

    static ArgumentBuilder<ServerCommandSource, ?> optionalEnclosure(@NotNull ArgumentBuilder<ServerCommandSource, ?> node, RunnableWithEnclosure runnable, Builder4ArgumentNodeBuilder builder) {
        return optionalEnclosure(node,
            extra -> (area, context, extra2) -> runnable.run(area, context),
            (node1, runnable1, extra) -> builder.build(node1, runnable1),
            List.of(""));
    }

    @Contract("_, _, _, _ -> param1")
    static <T> ArgumentBuilder<ServerCommandSource, ?> optionalEnclosure(@NotNull ArgumentBuilder<ServerCommandSource, ?> node, Function<T, RunnableWithEnclosureAndExtra<T>> converter, BuilderWithExtra4ArgumentNodeBuilder<T> builder, @NotNull Collection<T> collection) {
        var resNode = landArgument();
        collection.forEach(it -> {
            builder.build(node, handleException(context -> {
                if (context.getSource().getPlayer() == null) {
                    error(TrT.of("enclosure.message.no_enclosure"), context);
                }
                ServerPlayerEntity player = context.getSource().getPlayer();
                EnclosureArea area = Instance.getAllEnclosures(player.getWorld()).getArea(player.getBlockPos());
                if (area == null) {
                    error(TrT.of("enclosure.message.no_enclosure"), context);
                }
                area = area.areaOf(player.getBlockPos());
                converter.apply(it).run(area, context, it);
                return 0;
            }), it);
            builder.build(resNode, handleException(context -> {
                EnclosureArea area = getEnclosure(context);
                converter.apply(it).run(area, context, it);
                return 0;
            }), it);
            node.then(resNode);
        });
        return node;
    }

    static @NotNull Optional<Boolean> getOptionalBoolean(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String val = StringArgumentType.getString(context, "value");
        if (val.equals("true")) {
            return Optional.of(true);
        }
        if (val.equals("false")) {
            return Optional.of(false);
        }
        if (val.equals("none")) {
            return Optional.empty();
        }
        Text text = TrT.of("enclosure.message.unexpected_optional_boolean");
        throw new CommandSyntaxException(new SimpleCommandExceptionType(text), text);
    }

    static RequiredArgumentBuilder<ServerCommandSource, String> optionalBooleanArgument() {
        return argument("value", StringArgumentType.word())
            .suggests((context, builder) -> {
                builder.suggest("true");
                builder.suggest("false");
                builder.suggest("none");
                return builder.buildFuture();
            });
    }

    static RequiredArgumentBuilder<ServerCommandSource, String> landArgument() {
        return argument("land", StringArgumentType.string())
            .suggests((context, builder) -> {
                String res = builder.getRemainingLowerCase();
                if (res.contains(".")) {
                    if (Instance.getEnclosure(res.substring(0, res.lastIndexOf('.'))) instanceof Enclosure enclosure) {
                        String subRes = res.substring(res.lastIndexOf('.') + 1);
                        enclosure.getSubEnclosures().getAreas().stream()
                            .filter(it -> it.getName().toLowerCase().startsWith(subRes))
                            .map(EnclosureArea::getFullName)
                            .forEach(builder::suggest);
                    }
                } else {
                    Instance.getAllEnclosures().stream()
                        .map(EnclosureArea::getFullName)
                        .filter(name -> name.toLowerCase().startsWith(res))
                        .forEach(builder::suggest);
                }
                return builder.buildFuture();
            });
    }

    static RequiredArgumentBuilder<ServerCommandSource, String> permissionArgument(Permission.Target target) {
        return argument("permission", StringArgumentType.word())
            .suggests((context, builder) -> CommandSource.suggestMatching(Permission.suggest(target), builder));
    }

    static UUID uuidOf(@NotNull ServerCommandSource source) {
        if (source.getPlayer() != null) {
            return source.getPlayer().getUuid();
        } else {
            return CONSOLE;
        }
    }

    public static Session sessionOf(ServerCommandSource source) {
        return Instance.getPlayerSessions().get(uuidOf(source));
    }

    static void showHelp(@NotNull ServerCommandSource source, String subcommand) throws CommandSyntaxException {
        CommandNode<ServerCommandSource> child = node.getChild(subcommand);
        if (child == null) {
            EnclosureCommand.error(TrT.of("enclosure.help.no_child"));
        }
        source.sendMessage(Text.literal("/enclosure " + child.getUsageText()).styled(style -> style.withColor(Formatting.GOLD))
            .append(": ")
            .append(TrT.of("enclosure.help." + subcommand).styled(style -> style.withColor(Formatting.WHITE))));
    }

    static void showHelp(@NotNull ServerCommandSource source) {
        source.sendMessage(TrT.of("enclosure.help.header"));
        minecraftServer.getCommandManager().getDispatcher()
            .getSmartUsage(node, source)
            .forEach((name, s) -> source.sendMessage(
                Text.literal("/enclosure " + s).styled(style -> style.withColor(Formatting.GOLD))
                    .append(": ")
                    .append(TrT.of("enclosure.help." + name.getName()).styled(style -> style.withColor(Formatting.WHITE)))));
    }

    @Contract(pure = true)
    static @NotNull Command<ServerCommandSource> handleException(Command<ServerCommandSource> command) {
        return context -> {
            try {
                return command.run(context);
            } catch (PermissionTargetException e) {
                error(e.getText());
            } catch (CommandSyntaxException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Error while executing command: " + context.getInput(), e);
                error(TrT.of("enclosure.message.error")
                    .append(e.getMessage()), context);
            }
            return 0;
        };
    }

    private static int createEnclosure(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        if (Instance.getEnclosure(name) != null) {
            error(TrT.of("enclosure.message.name_in_use"), context);
        }
        if (!context.getSource().hasPermissionLevel(4)) {
            if (name.length() > ServerMain.commonConfig.maxEnclosureNameLength) {
                error(TrT.of("enclosure.message.res_name_too_long"), context);
            }
            if (name.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && c != '_')) {
                error(TrT.of("enclosure.message.res_name_invalid"), context);
            }
        }

        Session session = sessionOf(context.getSource());
        checkSession(context);
        EnclosureList list = Instance.getAllEnclosures(session.world);
        EnclosureArea intersectArea = sessionOf(context.getSource()).intersect(list);
        if (intersectArea != null) {
            error(TrT.of("enclosure.message.intersected")
                .append(intersectArea.serialize(Name, context.getSource().getPlayer())), context);
        }
        Enclosure enclosure = new Enclosure(session, name);
        LandLimits limits = ServerMain.limits;
        if (!context.getSource().hasPermissionLevel(4)) {
            checkSessionSize(session, context);
            if (context.getSource().getPlayer() != null) {
                ServerPlayerEntity player = context.getSource().getPlayer();
                long count = Instance.getAllEnclosures(player.getUuid()).size();
                if (count >= limits.maxLands) {
                    error(TrT.of("enclosure.message.rcle.self")
                        .append(Text.literal(String.valueOf(limits.maxLands))), context);
                }
            }
        }
        enclosure.setWorld(session.world);
        session.reset(session.world);
        list.addArea(enclosure);
        context.getSource().sendMessage(TrT.of("enclosure.message.created")
            .append(enclosure.serialize(Name, context.getSource().getPlayer()))
        );
        LOGGER.info(context.getSource().getName() + " created enclosure " + enclosure.getName());
        return 0;
    }

    private static void error(Text sst, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        throw new SimpleCommandExceptionType(sst)
            .createWithContext(new StringReader(context.getInput()));
    }

    private static void error(Text sst, CommandContext<ServerCommandSource> context, int cursor) throws CommandSyntaxException {
        StringReader reader = new StringReader(context.getInput());
        reader.setCursor(cursor);
        throw new SimpleCommandExceptionType(sst)
            .createWithContext(reader);
    }

    @Contract("_ -> fail")
    private static void error(Text text) throws CommandSyntaxException {
        throw new SimpleCommandExceptionType(text).create();
    }

    private static void checkSession(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Session session = sessionOf(context.getSource());
        if (session.world == null || session.pos1 == null || session.pos2 == null) {
            error(TrT.of("enclosure.message.null_select_point"));
        }
    }

    private static void checkSessionSize(Session session, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Text text = session.isValid(ServerMain.limits);
        if (text != null) {
            throw new SimpleCommandExceptionType(text).createWithContext(new StringReader(context.getInput()));
        }
    }

    static @NotNull EnclosureArea getEnclosure(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EnclosureArea enclosure = Instance.getEnclosure(StringArgumentType.getString(context, "land"));
        if (enclosure == null) {
            Optional<ParsedCommandNode<ServerCommandSource>> land = context.getNodes().stream().filter(n -> n.getNode() instanceof ArgumentCommandNode<?, ?> arg
                && arg.getName().equals("land")
                && arg.getType() instanceof StringArgumentType).findFirst();
            if (land.isPresent()) {
                error(TrT.of("enclosure.message.no_enclosure"), context, land.get().getRange().getStart());
            } else {
                error(TrT.of("enclosure.message.no_enclosure"), context);
            }
        }
        assert enclosure != null; // stupid compiler
        return enclosure;
    }

    interface RunnableWithEnclosure {
        void run(EnclosureArea area, CommandContext<ServerCommandSource> context) throws CommandSyntaxException;
    }

    interface RunnableWithEnclosureAndExtra<T> {
        void run(EnclosureArea area, CommandContext<ServerCommandSource> context, T extra) throws CommandSyntaxException, PermissionTargetException;
    }

    interface Builder4ArgumentNodeBuilder {
        Builder4ArgumentNodeBuilder UNIT = ArgumentBuilder::executes;

        void build(ArgumentBuilder<ServerCommandSource, ?> node, Command<ServerCommandSource> command);
    }

    interface BuilderWithExtra4ArgumentNodeBuilder<T> {
        void build(ArgumentBuilder<ServerCommandSource, ?> node, Command<ServerCommandSource> runnable, T extra);
    }
}
