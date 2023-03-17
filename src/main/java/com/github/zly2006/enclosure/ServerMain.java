package com.github.zly2006.enclosure;

import com.github.zly2006.enclosure.commands.ConfirmManager;
import com.github.zly2006.enclosure.commands.EnclosureCommand;
import com.github.zly2006.enclosure.commands.Session;
import com.github.zly2006.enclosure.config.Common;
import com.github.zly2006.enclosure.config.Converter;
import com.github.zly2006.enclosure.config.LandLimits;
import com.github.zly2006.enclosure.events.PlayerUseEntityEvent;
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler;
import com.github.zly2006.enclosure.listeners.SessionListener;
import com.github.zly2006.enclosure.network.EnclosureInstalledC2SPacket;
import com.github.zly2006.enclosure.network.RequestOpenScreenC2SPPacket;
import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.ResourceLoader;
import com.github.zly2006.enclosure.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static com.github.zly2006.enclosure.EnclosureGroup.GROUPS_KEY;
import static com.github.zly2006.enclosure.EnclosureGroup.Groups;
import static com.github.zly2006.enclosure.EnclosureList.DATA_VERSION_KEY;
import static com.github.zly2006.enclosure.EnclosureList.ENCLOSURE_LIST_KEY;
import static com.github.zly2006.enclosure.utils.Permission.*;
import static net.minecraft.server.command.CommandManager.literal;

public class ServerMain implements DedicatedServerModInitializer {
    public static final String MOD_ID = "enclosure"; // 模组标识符
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static final Version MOD_VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).get().getMetadata().getVersion(); // 模组版本
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    public static final Path OLD_CONF_PATH = Path.of("config", "enclosure", "old-config");
    private static final Path LIMITS_PATH = Path.of("config", "enclosure", "limits.json");
    private static final Path COMMON_PATH = Path.of("config", "enclosure", "common.json");
    public static final int DATA_VERSION = 2;
    public static ServerMain Instance;
    public static final Text HEADER = new LiteralText("")
            .append(new LiteralText("[").styled(style -> style.withColor(TextColor.fromRgb(0x00FF00))))
            .append(new LiteralText("Enclosure").styled(style -> style.withColor(TextColor.fromRgb(0x00FFFF))))
            .append(new LiteralText("]").styled(style -> style.withColor(TextColor.fromRgb(0x00FF00))))
            .append(new LiteralText(" ").formatted(Formatting.RESET));
    public static boolean PAID = true;
    Map<RegistryKey<World>, EnclosureList> enclosures = new HashMap<>();
    Item operationItem;
    Map<UUID, Session> playerSessions = new HashMap<>(Map.of(
            new UUID(0, 0), new Session()
    ));
    Groups groups;
    public static LandLimits limits;
    public static Common commonConfig;
    public static JsonObject translation;
    public static MinecraftServer minecraftServer;

    /**
     * 判断某个情况是否适用某个权限
     * 此处的使用不一定是唯一用途
     */
    public static Map<Permission, Predicate<UseContext>> USE_PREDICATES = new HashMap<>(){{
        put(RESPAWN_ANCHOR, context -> context.block == Blocks.RESPAWN_ANCHOR);
        put(ANVIL, context -> context.block instanceof AnvilBlock);
        put(BED, context -> context.block instanceof BedBlock);
        put(BEACON, context -> context.block == Blocks.BEACON);
        put(CAKE, context -> context.block == Blocks.CAKE);
        put(DOOR, context -> context.block instanceof DoorBlock
                || context.block instanceof FenceGateBlock
                || context.block instanceof TrapdoorBlock);
        put(HONEY, context -> context.block instanceof BeehiveBlock);
        put(DRAGON_EGG, context -> context.block == Blocks.DRAGON_EGG);
        put(NOTE, context -> context.block == Blocks.NOTE_BLOCK);
        put(SHEAR, context -> context.item == Items.SHEARS && (context.entity != null
                || context.block instanceof TwistingVinesBlock
                || context.block instanceof TwistingVinesPlantBlock
                || context.block instanceof WeepingVinesBlock
                || context.block instanceof WeepingVinesPlantBlock
                || context.block instanceof PumpkinBlock));
        put(NAMETAG, context -> context.item == Items.NAME_TAG && context.entity != null);
        put(PICK_BERRIES, context -> context.block == Blocks.SWEET_BERRY_BUSH ||
                context.block == Blocks.CAVE_VINES_PLANT ||
                context.block == Blocks.CAVE_VINES);
        put(DYE, context -> context.item instanceof DyeItem && context.entity instanceof SheepEntity);
        put(HORSE, context -> context.entity instanceof HorseBaseEntity || context.entity instanceof StriderEntity || context.entity instanceof PigEntity);
        put(FEED_ANIMAL, context -> context.entity instanceof AnimalEntity animal && animal.isBreedingItem(context.item.getDefaultStack()));
        put(FISH, context -> context.item == Items.FISHING_ROD);
        put(USE_BONE_MEAL, context -> context.item == Items.BONE_MEAL);
        put(USE_CAMPFIRE, context -> context.block == Blocks.CAMPFIRE || context.block == Blocks.SOUL_CAMPFIRE);
        put(USE_DIRT, context ->
                (context.block == Blocks.GRASS_BLOCK && (context.item instanceof ShovelItem || context.item == Items.BONE_MEAL)) ||
                (context.block == Blocks.DIRT && context.item instanceof PotionItem potion));
        put(USE_JUKEBOX, context -> context.block == Blocks.JUKEBOX);
        put(REDSTONE, context -> context.block instanceof AbstractButtonBlock
                || context.block == Blocks.LEVER
                || context.block == Blocks.DAYLIGHT_DETECTOR);
        put(STRIP_LOG, context ->
                (context.block == Blocks.ACACIA_LOG ||
                context.block == Blocks.BIRCH_LOG ||
                context.block == Blocks.OAK_LOG ||
                context.block == Blocks.DARK_OAK_LOG ||
                context.block == Blocks.JUNGLE_LOG ||
                context.block == Blocks.SPRUCE_LOG) &&
                context.item instanceof AxeItem);
        put(VEHICLE, context -> context.item instanceof BoatItem || context.item instanceof MinecartItem);
        put(CAULDRON, context -> context.block instanceof AbstractCauldronBlock);
    }};

    record UseContext(@NotNull ServerPlayerEntity player, @Nullable BlockPos pos, @Nullable BlockState state,
                      @Nullable Block block, @NotNull Item item, @Nullable Entity entity) { }

    public static String blockPos2string(BlockPos pos) {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }

    /**
     * Checks if an operation is allowed through the boundaries of the enclosure.
     *
     * @return if true, pass on, otherwise, set return value to false and cancel this callback
     */
    public static boolean checkPermissionInDifferentEnclosure(ServerWorld world, BlockPos pos1, BlockPos pos2, Permission permission) {
        EnclosureList list = Instance.getAllEnclosures(world);
        EnclosureArea from = list.getArea(pos1);
        EnclosureArea to = list.getArea(pos2);
        if (from == to) return true;
        if (from != null && !from.areaOf(pos1).hasPubPerm(permission)) {
            return false;
        }
        if (to != null && !to.areaOf(pos2).hasPubPerm(permission)) {
            return false;
        }
        return true;
    }

    public boolean checkPermission(World world, @NotNull BlockPos pos, @Nullable PlayerEntity player, @NotNull Permission permission) {
        if (world.isClient) return true;
        EnclosureList list = getAllEnclosures((ServerWorld) world);
        EnclosureArea area = list.getArea(pos);
        if (area == null) return true;
        if (player != null) {
            return area.areaOf(pos).hasPerm((ServerPlayerEntity) player, permission);
        }
        return area.areaOf(pos).hasPubPerm(permission);
    }

    public EnclosureList getAllEnclosures(ServerWorld world) {
        return Optional.ofNullable(enclosures.get(world.getRegistryKey()))
                .orElseGet(() -> {
                    EnclosureList list = new EnclosureList();
                    list.bind2world(world);
                    return list;
                });
    }

    public List<Enclosure> getAllEnclosures(UUID uuid) {
        return getAllEnclosures().stream()
                .filter(res -> (uuid == null) || (uuid.equals(res.owner)))
                .toList();
    }

    public List<Enclosure> getAllEnclosures() {
        return enclosures.values().stream()
                .map(list -> list.areas)
                .map(Map::values)
                .flatMap(Collection::stream)
                .filter(res -> res instanceof Enclosure)
                .map(res -> (Enclosure) res)
                .toList();
    }

    public @Nullable EnclosureArea getEnclosure(@NotNull String name) {
        List<EnclosureArea> list = enclosures.values().stream()
                .map(l -> l.areas)
                .map(Map::values)
                .flatMap(Collection::stream)
                .toList();
        while (name.contains(".")) {
            String father = name.substring(0, name.indexOf('.'));
            Optional<EnclosureArea> oa = list.stream().filter(r -> r.name.equalsIgnoreCase(father)).findAny();
            if (oa.isEmpty() || !(oa.get() instanceof Enclosure))
                return null;
            list = ((Enclosure) oa.get()).subEnclosures.areas.values().stream().toList();
            name = name.substring(name.indexOf('.') + 1);
        }
        String finalName = name;
        return list.stream()
                .filter(a -> a.name.equalsIgnoreCase(finalName))
                .findFirst()
                .orElse(null);
    }

    public Optional<EnclosureArea> getSmallestEnclosure(ServerWorld world, BlockPos pos) {
        return enclosures.get(world.getRegistryKey()).areas.values().stream()
                .filter(area -> area.isInner(pos))
                .findFirst()
                .map(s -> s.areaOf(pos));
    }

    @Environment(EnvType.SERVER)
    public boolean checkPermission(ServerPlayerEntity player, Permission permission, BlockPos pos) {
        if (player.getCommandSource().hasPermissionLevel(4) && permission.isIgnoreOp())
            return true;
        var enclosure = getAllEnclosures(player.getWorld()).getArea(pos);
        if (enclosure == null) {
            return true;
        } else {
            return enclosure.areaOf(pos).hasPerm(player, permission);
        }
    }

    public static void reloadLimits() throws IOException {
        limits = GSON.fromJson(Files.readString(LIMITS_PATH), LandLimits.class);
    }

    public static void reloadCommon() throws IOException {
        commonConfig = GSON.fromJson(Files.readString(COMMON_PATH), Common.class);
    }

    public static void saveLimits() {
        try {
            File file = LIMITS_PATH.toFile();
            if (!file.exists()) {
                file.createNewFile();
            }
            Files.writeString(LIMITS_PATH, GSON.toJson(limits));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void saveCommon() {
        try {
            File file = COMMON_PATH.toFile();
            if (!file.exists()) {
                file.createNewFile();
            }
            Files.writeString(COMMON_PATH, GSON.toJson(commonConfig));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void load() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            throw new RuntimeException("Server-side entrypoint called on client!");
        }
        File directory = new File(FabricLoader.getInstance().getConfigDir().toFile(), MOD_ID);
        if (!directory.exists() || directory.isFile()) {
            directory.delete();
            directory.mkdirs();
        }
        if (limits == null) {
            try {
                reloadLimits();
                saveLimits();
                LOGGER.info("Loaded limits config");
            } catch (IOException e) {
                limits = new LandLimits();
                saveLimits();
                LOGGER.info("Created limits config");
            }
        }
        if (commonConfig == null) {
            try {
                reloadCommon();
                saveCommon();
                LOGGER.info("Loaded common config");
            } catch (IOException e) {
                commonConfig = new Common();
                saveCommon();
                LOGGER.info("Created common config");
            }
        }
        if (translation == null || translation.size() == 0) {
            try {
                translation = GSON.fromJson(ResourceLoader.getLanguageFile(commonConfig.defaultLanguageKey), JsonObject.class);
                LOGGER.info("Loaded language file: " + commonConfig.defaultLanguageKey + ", there are " + translation.entrySet().size() + " entries");
            } catch (IOException e) {
                LOGGER.error("Failed to load default language file: " + commonConfig.defaultLanguageKey);
                LOGGER.warn("Using en_us as default language");
                try {
                    translation = GSON.fromJson(ResourceLoader.getLanguageFile("en_us"), JsonObject.class);
                } catch (IOException ex) {
                    LOGGER.error("Failed to load en_us language file");
                    LOGGER.error("Please report this issue to the author");
                    e.printStackTrace();
                    translation = new JsonObject();
                }
            }
        }
    }

    @Override
    public void onInitializeServer() {
        Instance = this;
        operationItem = Items.WOODEN_HOE;
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            PAID = true;
        }

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (minecraftServer.getPlayerManager().isOperator(handler.player.getGameProfile()) && commonConfig.developMode) {
                handler.player.sendMessage(new LiteralText("This server is running in development environment, and this is dangerous! To turn this feature off, please modify the config file.").formatted(Formatting.RED), false);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, d) -> {
            if (!d) return; // don't work on integrated server
            ConfirmManager.register(dispatcher);
            EnclosureCommand.register(dispatcher);
            CommandNode<ServerCommandSource> node = dispatcher.getRoot().getChild("enclosure");
            if (commonConfig.developMode) {
                dispatcher.register(CommandManager.literal("op-me").executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player == null) {
                        return 1;
                    }
                    minecraftServer.getPlayerManager().addToOperators(player.getGameProfile());
                    return 1;
                }));
                dispatcher.register(CommandManager.literal("logout").executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player == null) {
                        return 1;
                    }
                    player.networkHandler.disconnect(Text.of("Logout"));
                    return 0;
                }));
            }
            commonConfig.aliases.forEach(alias -> dispatcher.register(literal(alias).redirect(node)));
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> minecraftServer = server);

        // add listeners
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!checkPermission(serverPlayer, BREAK_BLOCK, pos)) {
                    player.sendMessage(BREAK_BLOCK.getNoPermissionMsg(player), false);
                    return false;
                }
            }
            return true;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                BlockState state = world.getBlockState(pos);
                if (state.getBlock() instanceof DragonEggBlock) {
                    if (checkPermission(serverPlayer, DRAGON_EGG, pos)) {
                        return ActionResult.PASS;
                    }
                    else {
                        return ActionResult.FAIL;
                    }
                }
            }
            return ActionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                BlockState state = player.getWorld().getBlockState(hitResult.getBlockPos());
                Block block = state.getBlock();
                UseContext context = new UseContext(serverPlayer, hitResult.getBlockPos(), state, block, player.getStackInHand(hand).getItem(), null);

                List<Permission> permissionList = USE_PREDICATES.entrySet().stream()
                        .filter(entry -> entry.getValue().test(context))
                        .map(Map.Entry::getKey)
                        .toList();
                if (permissionList.isEmpty() && (context.item instanceof BlockItem
                        || (hitResult.getSide() == Direction.UP && (context.item == Items.FLINT_AND_STEEL || context.item == Items.FIRE_CHARGE))
                        || context.item == Items.ARMOR_STAND
                        || context.item == Items.END_CRYSTAL
                        || context.item instanceof DecorationItem)) {
                    BlockPos pos = hitResult.getBlockPos().offset(hitResult.getSide());
                    if (checkPermission(serverPlayer, PLACE_BLOCK, pos)) {
                        return ActionResult.PASS;
                    }
                    else {
                        player.currentScreenHandler.syncState();
                        player.sendMessage(PLACE_BLOCK.getNoPermissionMsg(player), false);
                        return ActionResult.FAIL;
                    }
                }
                Optional<ActionResult> actionResult = permissionList.stream()
                        .map(permission -> {
                            if (checkPermission(serverPlayer, permission, context.pos)) {
                                return ActionResult.PASS;
                            }
                            else {
                                player.currentScreenHandler.syncState();
                                player.sendMessage(permission.getNoPermissionMsg(player), false);
                                return ActionResult.FAIL;
                            }
                        })
                        .filter(result -> result != ActionResult.PASS)
                        .findFirst();
                if (actionResult.isPresent()) {
                    return actionResult.get();
                }
            }
            return ActionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                UseContext context = new UseContext(serverPlayer, null, null, null, player.getStackInHand(hand).getItem(), null);

                Optional<TypedActionResult<ItemStack>> actionResult = USE_PREDICATES.entrySet().stream()
                        .filter(entry -> entry.getValue().test(context))
                        .map(Map.Entry::getKey)
                        .map(permission -> {
                            if (checkPermission(serverPlayer, permission, serverPlayer.getBlockPos())) {
                                return TypedActionResult.pass(serverPlayer.getStackInHand(hand));
                            }
                            else {
                                player.currentScreenHandler.syncState();
                                player.sendMessage(permission.getNoPermissionMsg(player), false);
                                return TypedActionResult.fail(serverPlayer.getStackInHand(hand));
                            }
                        })
                        .filter(result -> result.getResult() != ActionResult.PASS)
                        .findFirst();
                if (actionResult.isPresent()) {
                    return actionResult.get();
                }
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof ArmorStandEntity) {
                if (!checkPermission(world, entity.getBlockPos(), player, ARMOR_STAND)) {
                    player.sendMessage(ARMOR_STAND.getNoPermissionMsg(player), false);
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });
        PlayerUseEntityEvent.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                Item usingItem = player.getStackInHand(hand).getItem();
                UseContext context = new UseContext(serverPlayer, Utils.toBlockPos(hitResult.getPos()), null, null, usingItem, entity);

                Optional<ActionResult> actionResult = USE_PREDICATES.entrySet().stream()
                        .filter(entry -> entry.getValue().test(context))
                        .map(Map.Entry::getKey)
                        .map(permission -> {
                            if (checkPermission(serverPlayer, permission, entity.getBlockPos())) {
                                return ActionResult.PASS;
                            }
                            else {
                                player.currentScreenHandler.syncState();
                                player.sendMessage(permission.getNoPermissionMsg(player), false);
                                return ActionResult.FAIL;
                            }
                        })
                        .filter(result -> result != ActionResult.PASS)
                        .findFirst();
                if (actionResult.isPresent()) {
                    return actionResult.get();
                }
            }
            return ActionResult.PASS;
        });

        SessionListener.register();

        EnclosureScreenHandler.register();

        EnclosureInstalledC2SPacket.register();
        RequestOpenScreenC2SPPacket.register();

        // initialize enclosures
        ServerWorldEvents.LOAD.register((server, world) -> {
            LOGGER.info("Loading enclosures in {}...", world.getRegistryKey().getValue());
            AtomicBoolean update = new AtomicBoolean(false);
            EnclosureList list = world.getChunkManager()
                    .getPersistentStateManager().getOrCreate(nbtCompound -> {
                        int version = nbtCompound.getInt(DATA_VERSION_KEY);
                        if (version != DATA_VERSION) {
                            LOGGER.info("Updating enclosure data from version {} to {}", version, DATA_VERSION);
                            for (int i = version; i < DATA_VERSION; i++) {
                                nbtCompound = DataUpdater.update(i, nbtCompound);
                            }
                            update.set(true);
                        }
                        EnclosureList enclosureList = new EnclosureList(nbtCompound);
                        enclosures.put(world.getRegistryKey(), enclosureList);
                        enclosureList.bind2world(world);
                        enclosureList.markDirty();
                        return enclosureList;
                    }, () -> {
                        EnclosureList enclosureList = new EnclosureList();
                        enclosures.put(world.getRegistryKey(), enclosureList);
                        enclosureList.bind2world(world);
                        enclosureList.markDirty();
                        return enclosureList;
                    }, ENCLOSURE_LIST_KEY);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            groups = server.getOverworld().getPersistentStateManager()
                    .getOrCreate(Groups::new, Groups::new, GROUPS_KEY);
            Converter.convert();
        });

        LOGGER.info("Enclosure enabled now!");
    }

    public Item getOperationItem() {
        return this.operationItem;
    }

    public void setOperationItem(Item operationItem) {
        this.operationItem = operationItem;
    }

    public Map<UUID, Session> getPlayerSessions() {
        return this.playerSessions;
    }

    public Groups getEnclosureGroups() {
        return this.groups;
    }

    static {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            //加载配置文件
            load();
        }
    }
}
