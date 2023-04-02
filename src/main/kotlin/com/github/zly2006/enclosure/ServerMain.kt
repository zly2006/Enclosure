package com.github.zly2006.enclosure

import com.github.zly2006.enclosure.command.CONSOLE
import com.github.zly2006.enclosure.command.Session
import com.github.zly2006.enclosure.command.register
import com.github.zly2006.enclosure.config.Common
import com.github.zly2006.enclosure.config.Converter
import com.github.zly2006.enclosure.config.LandLimits
import com.github.zly2006.enclosure.events.PlayerUseEntityEvent
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler
import com.github.zly2006.enclosure.listeners.SessionListener
import com.github.zly2006.enclosure.network.EnclosureInstalledC2SPacket
import com.github.zly2006.enclosure.network.RequestOpenScreenC2SPPacket
import com.github.zly2006.enclosure.utils.Permission
import com.github.zly2006.enclosure.utils.ResourceLoader
import com.github.zly2006.enclosure.utils.Utils
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.*
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.Vec3ArgumentType
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.passive.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.*
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.RegistrationEnvironment
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


val MOD_ID = "enclosure" // 模组标识符
@JvmField
val MOD_VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).get().metadata.version // 模组版本
@JvmField
val LOGGER = LoggerFactory.getLogger(MOD_ID)
@JvmField
val GSON = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
@JvmField
val OLD_CONF_PATH = Path.of("config", "enclosure", "old-config")
private val LIMITS_PATH = Path.of("config", "enclosure", "limits.json")
private val COMMON_PATH = Path.of("config", "enclosure", "common.json")
@JvmField
val DATA_VERSION = 2
lateinit var Instance: ServerMain
lateinit var minecraftServer: MinecraftServer
@JvmField
var byUuid: MutableMap<UUID, String> = mutableMapOf()

class ServerMain: DedicatedServerModInitializer {
    init {
        val directory = File(FabricLoader.getInstance().configDir.toFile(), MOD_ID)
        if (!directory.exists() || directory.isFile) {
            directory.delete()
            directory.mkdirs()
        }
    }
    internal data class UseContext(
        val player: ServerPlayerEntity,
        val pos: BlockPos?,
        val state: BlockState?,
        val block: Block?,
        val item: Item,
        val entity: Entity?
    )
    val HEADER: Text = Text.empty()
        .append(Text.literal("[").styled { style: Style ->
            style.withColor(
                TextColor.fromRgb(0x00FF00)
            )
        })
        .append(Text.literal("Enclosure").styled { style: Style ->
            style.withColor(
                TextColor.fromRgb(0x00FFFF)
            )
        })
        .append(Text.literal("]").styled { style: Style ->
            style.withColor(
                TextColor.fromRgb(0x00FF00)
            )
        })
        .append(Text.literal(" ").formatted(Formatting.RESET))
    var enclosures: MutableMap<RegistryKey<World>, EnclosureList> = HashMap()
    var operationItem: Item? = null
    var playerSessions: MutableMap<UUID, Session> = HashMap()
    lateinit var groups: EnclosureGroup.Groups
    var limits: LandLimits by readWriteLazy {
        try {
            val limits = GSON.fromJson(
                Files.readString(LIMITS_PATH),
                LandLimits::class.java
            )
            LOGGER.info("Loaded limits config")
            limits
        } catch (e: IOException) {
            val limits = LandLimits()
            saveLimits(limits)
            LOGGER.info("Created limits config")
            limits
        }
    }
    var commonConfig: Common by readWriteLazy {
        try {
             val common = GSON.fromJson(
                Files.readString(COMMON_PATH),
                Common::class.java
            )
            LOGGER.info("Loaded common config")
            common
        } catch (e: IOException) {
            val common = Common()
            saveCommon(common)
            LOGGER.info("Created common config")
            common
        }
    }
    private fun <T> readWriteLazy(initializer: () -> T): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {
            private var value: T? = null

            override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                if (value == null) {
                    value = initializer()
                }
                return value!!
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                this.value = value
            }
        }
    }
    val translation: JsonObject by lazy {
        try {
            val fromJson = GSON.fromJson(
                ResourceLoader.getLanguageFile(commonConfig.defaultLanguageKey),
                JsonObject::class.java
            )
            LOGGER.info("Loaded language file: " + commonConfig.defaultLanguageKey + ", there are " + translation.entrySet().size + " entries")
            return@lazy fromJson
        } catch (e: IOException) {
            LOGGER.error("Failed to load default language file: " + commonConfig.defaultLanguageKey)
            LOGGER.warn("Using en_us as default language")
            try {
                return@lazy GSON.fromJson(
                    ResourceLoader.getLanguageFile("en_us"),
                    JsonObject::class.java
                )
            } catch (ex: IOException) {
                LOGGER.error("Failed to load en_us language file")
                LOGGER.error("Please report this issue to the author")
                e.printStackTrace()
                return@lazy JsonObject()
            }
        }
    }
    var updateChecker = UpdateChecker()

    /**
     * 判断某个情况是否适用某个权限
     * 此处的使用不一定是唯一用途
     */
    private var USE_PREDICATES: Map<Permission, Predicate<UseContext>> =
        object : HashMap<Permission, Predicate<UseContext>>() {
            init {
                put(Permission.RESPAWN_ANCHOR) { it.block === Blocks.RESPAWN_ANCHOR }
                put(Permission.ANVIL) { it.block is AnvilBlock }
                put(Permission.BED) { it.block is BedBlock }
                put(Permission.BEACON) { it.block === Blocks.BEACON }
                put(Permission.CAKE) { it.block === Blocks.CAKE }
                put(Permission.DOOR) { it.block is DoorBlock || it.block is FenceGateBlock || it.block is TrapdoorBlock }
                put(Permission.HONEY) { it.block is BeehiveBlock && it.item == Items.GLASS_BOTTLE || it.item == Items.SHEARS }
                put(Permission.DRAGON_EGG) { it.block === Blocks.DRAGON_EGG }
                put(Permission.NOTE) { it.block === Blocks.NOTE_BLOCK }
                put(Permission.SHEAR) {
                    it.item === Items.SHEARS && (it.entity != null
                            || it.block is TwistingVinesBlock
                            || it.block is TwistingVinesPlantBlock
                            || it.block is WeepingVinesBlock
                            || it.block is WeepingVinesPlantBlock
                            || it.block is PumpkinBlock)
                }
                put(Permission.NAMETAG) { it.item === Items.NAME_TAG && it.entity != null }
                put(Permission.PICK_BERRIES) {
                    it.block === Blocks.SWEET_BERRY_BUSH || it.block === Blocks.CAVE_VINES_PLANT || it.block === Blocks.CAVE_VINES
                }
                put(Permission.DYE) { it.item is DyeItem && it.entity is SheepEntity }
                put(Permission.HORSE) { it.entity is AbstractHorseEntity || it.entity is StriderEntity || it.entity is PigEntity }
                put(Permission.FEED_ANIMAL) {
                    it.entity is AnimalEntity && it.entity.isBreedingItem(it.item.defaultStack)
                }
                put(Permission.FISH) { it.item === Items.FISHING_ROD }
                put(Permission.USE_BONE_MEAL) { it.item === Items.BONE_MEAL }
                put(Permission.USE_CAMPFIRE) { it.block === Blocks.CAMPFIRE || it.block === Blocks.SOUL_CAMPFIRE }
                put(Permission.USE_DIRT) {
                    it.block === Blocks.GRASS_BLOCK && (it.item is ShovelItem || it.item === Items.BONE_MEAL) || it.block === Blocks.DIRT && it.item is PotionItem
                }
                put(Permission.USE_JUKEBOX) { it.block === Blocks.JUKEBOX }
                put(Permission.REDSTONE) {
                    it.block is ButtonBlock || it.block === Blocks.LEVER || it.block === Blocks.DAYLIGHT_DETECTOR
                }
                put(Permission.STRIP_LOG) {
                    (it.block === Blocks.ACACIA_LOG || it.block === Blocks.BIRCH_LOG || it.block === Blocks.OAK_LOG || it.block === Blocks.DARK_OAK_LOG || it.block === Blocks.JUNGLE_LOG || it.block === Blocks.MANGROVE_LOG || it.block === Blocks.SPRUCE_LOG) &&
                            it.item is AxeItem
                }
                put(Permission.VEHICLE) { it.item is BoatItem || it.item is MinecartItem }
                put(Permission.ALLAY) { it.entity is AllayEntity }
                put(Permission.CAULDRON) { it.block is AbstractCauldronBlock }
            }
        }

    /**
     * Checks if an operation is allowed through the boundaries of the enclosure.
     *
     * @return if true, pass on, otherwise, set return value to false and cancel this callback
     */
    fun checkPermissionInDifferentEnclosure(
        world: ServerWorld,
        pos1: BlockPos,
        pos2: BlockPos,
        permission: Permission?
    ): Boolean {
        val list = Instance.getAllEnclosures(world)
        val from = list.getArea(pos1)
        val to = list.getArea(pos2)
        if (from === to) return true
        if (from != null && !from.areaOf(pos1).hasPubPerm(permission!!)) {
            return false
        }
        return if (to != null && !to.areaOf(pos2).hasPubPerm(permission!!)) {
            false
        } else true
    }

    fun checkPermission(world: World, pos: BlockPos, player: PlayerEntity?, permission: Permission): Boolean {
        if (world.isClient) return true
        val list = getAllEnclosures(world as ServerWorld)
        val area = list.getArea(pos) ?: return true
        return if (player != null) {
            area.areaOf(pos).hasPerm((player as ServerPlayerEntity?)!!, permission)
        } else area.areaOf(pos).hasPubPerm(permission)
    }

    fun getAllEnclosures(world: ServerWorld): EnclosureList {
        return enclosures[world.registryKey] ?: EnclosureList(world)
    }

    fun getAllEnclosures(uuid: UUID): List<Enclosure> {
        return getAllEnclosures().filter { res -> uuid == CONSOLE || uuid == res.owner }.toList()
    }

    fun getAllEnclosures(): List<Enclosure> {
        return enclosures.values
            .asSequence()
            .map { list: EnclosureList -> list.areas.values }
            .flatten()
            .filterIsInstance<Enclosure>()
            .map { res: EnclosureArea -> res as Enclosure }
            .toList()
    }

    fun getEnclosure(name: String): EnclosureArea? {
        var name = name
        var list = enclosures.values
            .map { l: EnclosureList -> l.areas.values }
            .flatten()
            .toList()
        while (name.contains(".")) {
            val father = name.substring(0, name.indexOf('.'))
            val oa = list.firstOrNull { r ->
                r!!.name.equals(
                    father,
                    ignoreCase = true
                )
            }
            if (oa == null || oa !is Enclosure) return null
            list = oa.subEnclosures.areas.values.toList()
            name = name.substring(name.indexOf('.') + 1)
        }
        val finalName = name
        return list.firstOrNull { a ->
            a.name.equals(
                finalName,
                ignoreCase = true
            )
        }
    }

    fun getSmallestEnclosure(world: ServerWorld, pos: BlockPos?): EnclosureArea? {
        return enclosures[world.registryKey]!!.areas.values
            .firstOrNull { area: EnclosureArea -> area.isInner(pos!!) }
            ?.let { s: EnclosureArea -> s.areaOf(pos!!) }
    }

    @Environment(EnvType.SERVER)
    fun checkPermission(player: ServerPlayerEntity, permission: Permission, pos: BlockPos?): Boolean {
        if (player.commandSource.hasPermissionLevel(4) && permission.isIgnoreOp) return true
        val enclosure = getAllEnclosures(player.getWorld()).getArea(pos!!)
        return enclosure?.areaOf(pos)?.hasPerm(player, permission) ?: true
    }

    @Throws(IOException::class)
    fun reloadLimits() {
        limits = GSON.fromJson(
            Files.readString(LIMITS_PATH),
            LandLimits::class.java
        )
    }

    fun reloadCommon() {
        commonConfig = GSON.fromJson(
            Files.readString(COMMON_PATH),
            Common::class.java
        )
    }

    private fun saveLimits(limits: LandLimits) {
        try {
            val file = LIMITS_PATH.toFile()
            if (!file.exists()) {
                file.createNewFile()
            }
            Files.writeString(LIMITS_PATH, GSON.toJson(limits))
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    private fun saveCommon(common: Common) {
        try {
            val file = COMMON_PATH.toFile()
            if (!file.exists()) {
                file.createNewFile()
            }
            Files.writeString(COMMON_PATH, GSON.toJson(common))
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    override fun onInitializeServer() {
        Instance = this
        operationItem = Items.WOODEN_HOE

        ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler: ServerPlayNetworkHandler, sender: PacketSender?, server: MinecraftServer? ->
            // warn the server ops that this server is running in development mode and not secure.
            if (minecraftServer.playerManager.isOperator(handler.player.gameProfile) && commonConfig.developMode
            ) {
                handler.player.sendMessage(
                    Text.literal("This server is running in development environment, and this is dangerous! To turn this feature off, please modify the config file.")
                        .formatted(Formatting.RED), false
                )
            }
            // let server ops know if there is a new version available
            if (updateChecker.latestVersion != null && updateChecker.latestVersion!!.versionNumber > MOD_VERSION && handler.player.hasPermissionLevel(4)) {
                updateChecker.notifyUpdate(handler.player)
            }
        })

        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher: CommandDispatcher<ServerCommandSource>, registryAccess: CommandRegistryAccess, environment: RegistrationEnvironment ->
            register(dispatcher)
            val node = dispatcher.root.getChild("enclosure")
            if (commonConfig.developMode) {
                dispatcher.register(
                    CommandManager.literal("notify_update")
                        .executes { context: CommandContext<ServerCommandSource> ->
                            updateChecker.notifyUpdate(context.source.player!!)
                            1
                        }
                )
                dispatcher.register(
                    CommandManager.literal("op-me")
                        .executes { context: CommandContext<ServerCommandSource> ->
                            val player = context.source.player ?: return@executes 1
                            minecraftServer.playerManager
                                .addToOperators(player.gameProfile)
                            1
                        }
                )
                dispatcher.register(
                    CommandManager.literal("logout")
                        .executes { context: CommandContext<ServerCommandSource> ->
                            val player = context.source.player ?: return@executes 1
                            player.networkHandler.disconnect(Text.of("Logout"))
                            0
                        }
                )
                dispatcher.register(
                    CommandManager.literal("explosion")
                        .then(
                            CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                .then(CommandManager.argument("radius", FloatArgumentType.floatArg(0f))
                                    .executes { context: CommandContext<ServerCommandSource> ->
                                        val pos = Vec3ArgumentType.getVec3(context, "pos")
                                        val radius = FloatArgumentType.getFloat(context, "radius")
                                        val world = context.source.world
                                        world.createExplosion(
                                            null,
                                            pos.x,
                                            pos.y,
                                            pos.z,
                                            radius,
                                            World.ExplosionSourceType.TNT
                                        )
                                        0
                                    }
                                )
                        )
                )
            }
            commonConfig.aliases.forEach(Consumer { alias: String? ->
                dispatcher.register(
                    CommandManager.literal(alias).redirect(node)
                )
            })
        })
        ServerLifecycleEvents.SERVER_STARTING.register(ServerStarting { server: MinecraftServer? ->
            minecraftServer = server!!
        })
        UseBlockCallback.EVENT.register { player: PlayerEntity, world: World, hand: Hand, hitResult: BlockHitResult ->
            if (player is ServerPlayerEntity) {
                val state = player.getWorld().getBlockState(hitResult.blockPos)
                val block = state.block
                val context = UseContext(
                    player,
                    hitResult.blockPos,
                    state,
                    block,
                    player.getStackInHand(hand).item,
                    null
                )
                val permissionList = USE_PREDICATES.entries.filter { it.value.test(context) }.map { it.key }.toList()
                if (permissionList.isEmpty() && (context.item is BlockItem || hitResult.side == Direction.UP && (context.item === Items.FLINT_AND_STEEL || context.item === Items.FIRE_CHARGE) || context.item === Items.ARMOR_STAND || context.item === Items.END_CRYSTAL || context.item is DecorationItem)) {
                    val pos = hitResult.blockPos.offset(hitResult.side)
                    if (checkPermission(player, Permission.PLACE_BLOCK, pos)) {
                        ActionResult.PASS
                    } else {
                        player.currentScreenHandler.syncState()
                        player.sendMessage(Permission.PLACE_BLOCK.getNoPermissionMsg(player))
                        ActionResult.FAIL
                    }
                }
                permissionList.map { permission ->
                    if (checkPermission(player, permission, context.pos)) {
                        return@map ActionResult.PASS
                    } else {
                        player.currentScreenHandler.syncState()
                        player.sendMessage(permission.getNoPermissionMsg(player))
                        return@map ActionResult.FAIL
                    }
                }.firstOrNull { it != ActionResult.PASS } ?: ActionResult.PASS
            }
            ActionResult.PASS
        }
        UseItemCallback.EVENT.register { player: PlayerEntity, _, hand: Hand ->
            if (player is ServerPlayerEntity) {
                val context = UseContext(player, null, null, null, player.getStackInHand(hand).item, null)
                USE_PREDICATES.entries
                    .asSequence()
                    .filter { it.value.test(context) }
                    .map { it.key }
                    .map { permission ->
                        if (checkPermission(player, permission, player.getBlockPos())) {
                            return@map TypedActionResult.pass(player.getStackInHand(hand))
                        } else {
                            player.currentScreenHandler.syncState()
                            player.sendMessage(permission.getNoPermissionMsg(player))
                            return@map TypedActionResult.fail(player.getStackInHand(hand))
                        }
                    }
                    .filter { result -> result.result != ActionResult.PASS }
                    .firstOrNull() ?: TypedActionResult.pass(player.getStackInHand(hand))
            }
             TypedActionResult.pass(player.getStackInHand(hand))
        }
        UseEntityCallback.EVENT.register { player: PlayerEntity, world: World?, _, entity: Entity, _ ->
            if (entity is ArmorStandEntity) {
                if (!checkPermission(world!!, entity.getBlockPos(), player, Permission.ARMOR_STAND)) {
                    player.sendMessage(Permission.ARMOR_STAND.getNoPermissionMsg(player))
                    ActionResult.FAIL
                }
            }
            ActionResult.PASS
        }
        PlayerUseEntityEvent.register { player, _, hand, entity, hitResult ->
            if (player is ServerPlayerEntity) {
                val usingItem = player.getStackInHand(hand).item
                val context = UseContext(player, Utils.toBlockPos(hitResult.pos), null, null, usingItem, entity)
                USE_PREDICATES.entries
                    .filter { it.value.test(context) }
                    .map { it.key }
                    .map { permission: Permission ->
                        if (checkPermission(player, permission, entity.blockPos)) {
                            ActionResult.PASS
                        } else {
                            player.currentScreenHandler.syncState()
                            player.sendMessage(permission.getNoPermissionMsg(player))
                            ActionResult.FAIL
                        }
                    }.firstOrNull { result: ActionResult -> result != ActionResult.PASS } ?: ActionResult.PASS
            }
            ActionResult.PASS
        }

        SessionListener.register()

        EnclosureScreenHandler.register()

        EnclosureInstalledC2SPacket.register()
        RequestOpenScreenC2SPPacket.register()

        // initialize enclosures
        ServerWorldEvents.LOAD.register(ServerWorldEvents.Load { _, world: ServerWorld ->
            LOGGER.info(
                "Loading enclosures in {}...",
                world.registryKey.value
            )
            val update = AtomicBoolean(false)
            world.chunkManager
                .persistentStateManager
                .getOrCreate({
                    var nbtCompound = it
                    val version = nbtCompound.getInt(EnclosureList.DATA_VERSION_KEY)
                    if (version != DATA_VERSION) {
                        LOGGER.info(
                            "Updating enclosure data from version {} to {}",
                            version,
                            DATA_VERSION
                        )
                        for (i in version until DATA_VERSION) {
                            nbtCompound = DataUpdater.update(i, nbtCompound)
                        }
                        update.set(true)
                    }
                    val enclosureList = EnclosureList(nbtCompound, world)
                    enclosures[world.registryKey] = enclosureList
                    enclosureList.markDirty()
                    enclosureList
                }, {
                    val enclosureList = EnclosureList(world)
                    enclosures[world.registryKey] = enclosureList
                    enclosureList.markDirty()
                    enclosureList
                }, EnclosureList.ENCLOSURE_LIST_KEY)
        })
        ServerLifecycleEvents.SERVER_STARTED.register(ServerStarted { server: MinecraftServer ->
            playerSessions[CONSOLE] = Session(null)
            groups = server.overworld.persistentStateManager
                .getOrCreate(
                    { EnclosureGroup.Groups(it) },
                    { EnclosureGroup.Groups() },
                    EnclosureGroup.GROUPS_KEY
                )
            Converter.convert()
            Thread {
                while (commonConfig.checkUpdate) {
                    updateChecker.check()
                    try {
                        Thread.sleep((1000 * 60 * 60 * 12).toLong()) // 12 hours
                    } catch (e: InterruptedException) {
                        return@Thread
                    }
                }
            }.start()
        })

        LOGGER.info("Enclosure enabled now!")
    }
}