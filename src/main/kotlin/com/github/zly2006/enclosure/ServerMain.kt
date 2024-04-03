package com.github.zly2006.enclosure

import com.github.zly2006.enclosure.backup.BackupManager
import com.github.zly2006.enclosure.command.BuilderScope
import com.github.zly2006.enclosure.command.CONSOLE
import com.github.zly2006.enclosure.command.Session
import com.github.zly2006.enclosure.command.register
import com.github.zly2006.enclosure.config.Common
import com.github.zly2006.enclosure.config.Converter
import com.github.zly2006.enclosure.config.LandLimits
import com.github.zly2006.enclosure.listeners.SessionListener
import com.github.zly2006.enclosure.utils.Permission
import com.github.zly2006.enclosure.utils.ResourceLoader
import com.github.zly2006.enclosure.utils.checkPermission
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType
import me.lucko.fabric.api.permissions.v0.Options
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.Version
import net.minecraft.block.*
import net.minecraft.command.argument.Vec3ArgumentType
import net.minecraft.entity.Entity
import net.minecraft.entity.Saddleable
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.entity.passive.AllayEntity
import net.minecraft.entity.passive.AnimalEntity
import net.minecraft.entity.passive.SheepEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.*
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager
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
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.function.Predicate

const val MOD_ID = "enclosure" // 模组标识符
@JvmField
val MOD_VERSION: Version = FabricLoader.getInstance().getModContainer(MOD_ID).get().metadata.version // 模组版本
@JvmField
val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
@JvmField
val GSON: Gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
@JvmField
val OLD_CONF_PATH: Path = Path.of("config", "enclosure", "old-config")
private val limitPath = Path.of("config", "enclosure", "limits.json")
private val commonConfigPath = Path.of("config", "enclosure", "common.json")
const val DATA_VERSION = 2
lateinit var minecraftServer: MinecraftServer

object ServerMain: ModInitializer {
    lateinit var backupManager: BackupManager
        private set
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
    internal val enclosures: MutableMap<RegistryKey<World>, EnclosureList> = HashMap()
    var operationItem: Item? = null
    var playerSessions: MutableMap<UUID, Session> = HashMap()
    var limits: Map<String, LandLimits> = run {
        try {
            val limits = reloadLimits()
            LOGGER.info("Loaded limits config")
            limits
        } catch (e: IOException) {
            val limits = mapOf("default" to LandLimits())
            BuilderScope.map["enclosure.limits.default"] = BuilderScope.Companion.DefaultPermission.TRUE
            saveLimits(limits)
            LOGGER.info("Created limits config")
            limits
        }
    }
    var commonConfig: Common = run {
        try {
             val common = GSON.fromJson(
                Files.readString(commonConfigPath),
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
    val translation: JsonObject by lazy {
        try {
            val fromJson = GSON.fromJson(
                ResourceLoader.getLanguageFile(commonConfig.defaultLanguageKey),
                JsonObject::class.java
            )
            LOGGER.info("Loaded language file: " + commonConfig.defaultLanguageKey + ", there are " + fromJson.entrySet().size + " entries")
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
    private val updateChecker = UpdateChecker()
    private val checkUpdateThread = Thread {
        while (commonConfig.checkUpdate) {
            updateChecker.check()
            try {
                Thread.sleep((1000 * 60 * 60 * 12).toLong()) // 12 hours
            } catch (e: InterruptedException) {
                return@Thread
            }
        }
    }

    /**
     * 判断某个情况是否适用某个权限
     * 此处的使用不一定是唯一用途
     */
    private val USE_PREDICATES: MutableMap<Permission, Predicate<UseContext>> =
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
                put(Permission.DYE) {
                    when (it.item) {
                        is DyeItem -> it.entity is SheepEntity || it.block is AbstractSignBlock
                        Items.INK_SAC, Items.GLOW_INK_SAC, Items.HONEYCOMB -> it.block is AbstractSignBlock
                        else -> false
                    }
                }
                put(Permission.HORSE) { it.entity is Saddleable }
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
                            || it.block === Blocks.REPEATER || it.block === Blocks.COMPARATOR || it.block === Blocks.REDSTONE_WIRE
                }
                put(Permission.STRIP_LOG) { (it.state?.isIn(BlockTags.LOGS) ?: false) && it.item is AxeItem }
                put(Permission.VEHICLE) { it.item is BoatItem || it.item is MinecartItem }
                put(Permission.ALLAY) { it.entity is AllayEntity }
                put(Permission.CAULDRON) { it.block is AbstractCauldronBlock }
                put(Permission.COMMAND_TP) { it.entity is ItemFrameEntity }
                put(Permission.PLACE_BLOCK) {
                    if (it.entity is ItemFrameEntity)
                        it.entity.heldItemStack.isOf(Items.AIR) && it.item != Items.AIR
                    else false
                }
            }
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
        return enclosures[world.registryKey] ?: EnclosureList(world, true)
    }

    fun getAllEnclosures(uuid: UUID): List<Enclosure> {
        return getAllEnclosures().filter { res -> uuid == CONSOLE || uuid == res.owner }
    }

    fun getAllEnclosures(): List<Enclosure> {
        return enclosures.values
            .asSequence()
            .map { list: EnclosureList -> list.areas }
            .flatten()
            .filterIsInstance<Enclosure>()
            .toList()
    }

    fun getEnclosure(name: String): EnclosureArea? {
        var a: EnclosureArea? = null
        var list: Sequence<EnclosureArea> = getAllEnclosures().asSequence()
        for (r in name.split('.')) {
            a = list.firstOrNull { it.name.equals(r, ignoreCase = true) }
            if (a == null || a !is Enclosure) return null
            list = a.subEnclosures.areas.asSequence()
        }
        return a
    }

    fun getSmallestEnclosure(world: ServerWorld, pos: BlockPos?): EnclosureArea? {
        return enclosures[world.registryKey]!!.areas
            .firstOrNull { area: EnclosureArea -> area.isInner(pos!!) }
            ?.areaOf(pos!!)
    }

    fun checkPermission(player: ServerPlayerEntity, permission: Permission, pos: BlockPos): Boolean {
        if (checkPermission(player, "enclosure.bypass") && permission.canBypass) return true
        val enclosure = getAllEnclosures(player.getServerWorld()).getArea(pos)
        return enclosure?.areaOf(pos)?.hasPerm(player, permission) ?: true
    }

    fun reloadLimits(): Map<String, LandLimits> {
        limits = GSON.fromJson(
            Files.readString(limitPath),
            JsonObject::class.java
        ).asMap().mapValues { GSON.fromJson(it.value, LandLimits::class.java) }
        return limits
    }

    fun getLimitsForPlayer(player: ServerPlayerEntity) =
        limits[Options.get(player, "enclosure.limits", "default")]

    fun reloadCommon() {
        commonConfig = GSON.fromJson(
            Files.readString(commonConfigPath),
            Common::class.java
        )
    }

    private fun saveLimits(limits: Map<String, LandLimits>) {
        try {
            val file = limitPath.toFile()
            if (!file.exists()) {
                file.createNewFile()
            }
            Files.writeString(limitPath, GSON.toJson(limits))
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    private fun saveCommon(common: Common) {
        try {
            val file = commonConfigPath.toFile()
            if (!file.exists()) {
                file.createNewFile()
            }
            Files.writeString(commonConfigPath, GSON.toJson(common))
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    override fun onInitialize() {
        operationItem = Items.WOODEN_HOE

        ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler: ServerPlayNetworkHandler, _, _ ->
            // warn the server ops that this server is running in development mode and not secure.
            if (minecraftServer.playerManager.isOperator(handler.player.gameProfile) && commonConfig.developMode) {
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

        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher: CommandDispatcher<ServerCommandSource>, access, _ ->
            val node = register(dispatcher, access)
            if (commonConfig.developMode) {
                dispatcher.register(
                    CommandManager.literal("notify_update")
                        .executes {
                            updateChecker.notifyUpdate(it.source.player!!)
                            1
                        }
                )
                dispatcher.register(
                    CommandManager.literal("op-me")
                        .executes {
                            val player = it.source.player ?: return@executes 0
                            minecraftServer.playerManager.addToOperators(player.gameProfile)
                            1
                        }
                )
                dispatcher.register(
                    CommandManager.literal("logout")
                        .executes {
                            val player = it.source.player ?: return@executes 1
                            player.networkHandler.disconnect(Text.of("Logout"))
                            0
                        }
                )
                dispatcher.register(
                    CommandManager.literal("explosion")
                        .then(
                            CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                .then(CommandManager.argument("radius", FloatArgumentType.floatArg(0f))
                                    .executes {
                                        val pos = Vec3ArgumentType.getVec3(it, "pos")
                                        val radius = FloatArgumentType.getFloat(it, "radius")
                                        val world = it.source.world
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
                val state = world.getBlockState(hitResult.blockPos)
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
                if (permissionList.isEmpty() && (context.item === Items.FLINT_AND_STEEL
                            || context.item === Items.FIRE_CHARGE
                            || context.item === Items.ARMOR_STAND
                            || context.item === Items.END_CRYSTAL
                            || context.item is DecorationItem)) {
                    val pos = hitResult.blockPos.offset(hitResult.side)
                    if (checkPermission(player, Permission.PLACE_BLOCK, pos)) {
                        return@register ActionResult.PASS
                    } else {
                        player.currentScreenHandler.syncState()
                        player.sendMessage(Permission.PLACE_BLOCK.getNoPermissionMsg(player))
                        return@register ActionResult.FAIL
                    }
                }
                return@register permissionList.map { permission ->
                    if (checkPermission(player, permission, context.pos!!)) {
                        return@map ActionResult.PASS
                    } else {
                        player.currentScreenHandler.syncState()
                        player.sendMessage(permission.getNoPermissionMsg(player))
                        return@map ActionResult.FAIL
                    }
                }.firstOrNull { it != ActionResult.PASS } ?: ActionResult.PASS
            }
            return@register ActionResult.PASS
        }
        UseItemCallback.EVENT.register { player: PlayerEntity, _, hand: Hand ->
            if (player is ServerPlayerEntity) {
                val hitResult = Item.raycast(player.world, player, RaycastContext.FluidHandling.ANY)
                val blockPos = if (hitResult.type == HitResult.Type.MISS) player.blockPos else hitResult.blockPos
                val context = UseContext(player, null, null, null, player.getStackInHand(hand).item, null)
                return@register USE_PREDICATES.entries
                    .asSequence()
                    .filter { it.value.test(context) }
                    .map { it.key }
                    .map { permission ->
                        if (checkPermission(player, permission, blockPos)) {
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
            return@register TypedActionResult.pass(player.getStackInHand(hand))
        }
        AttackBlockCallback.EVENT.register { player, world, _, pos, _ ->
            if (player is ServerPlayerEntity) {
                val state = world.getBlockState(pos)
                if (state.block is DragonEggBlock) {
                    if (checkPermission(player, Permission.DRAGON_EGG, pos)) {
                        return@register ActionResult.PASS
                    }
                    else {
                        return@register ActionResult.FAIL
                    }
                }
                if (!checkPermission(player, Permission.BREAK_BLOCK, pos)) {
                    player.sendMessage(Permission.BREAK_BLOCK.getNoPermissionMsg(player))
                    return@register ActionResult.FAIL
                }
            }
            return@register ActionResult.PASS
        }
        UseEntityCallback.EVENT.register { player: PlayerEntity, world: World?, hand, entity: Entity, _ ->
            if (entity is ArmorStandEntity) {
                if (!checkPermission(world!!, entity.getBlockPos(), player, Permission.ARMOR_STAND)) {
                    player.sendMessage(Permission.ARMOR_STAND.getNoPermissionMsg(player))
                    player.currentScreenHandler.syncState()
                    // We don't need to sync entity in this situation
                    return@register ActionResult.FAIL
                }
            }
            if (player is ServerPlayerEntity) {
                val usingItem = player.getStackInHand(hand).item
                val context = UseContext(player, entity.blockPos, null, null, usingItem, entity)
                return@register USE_PREDICATES.entries
                    .asSequence()
                    .filter { it.value.test(context) }
                    .map { it.key }
                    .map { permission: Permission ->
                        if (checkPermission(player, permission, entity.blockPos)) {
                            ActionResult.PASS
                        } else {
                            player.currentScreenHandler.syncState()
                            player.sendMessage(permission.getNoPermissionMsg(player))
                            player.networkHandler.sendPacket(EntityTrackerUpdateS2CPacket(
                                entity.id, entity.dataTracker.entries.map { it.toSerialized() }
                            ))
                            ActionResult.FAIL
                        }
                    }.firstOrNull { result: ActionResult -> result != ActionResult.PASS } ?: ActionResult.PASS
            }
            return@register ActionResult.PASS
        }

        SessionListener.register()

        // initialize enclosures

        ServerWorldEvents.LOAD.register(ServerWorldEvents.Load { _, world: ServerWorld ->
            LOGGER.info(
                "Loading enclosures in {}...",
                world.registryKey.value
            )
            val update = AtomicBoolean(false)
            val type = PersistentState.Type({
                val enclosureList = EnclosureList(world, true)
                enclosureList.markDirty()
                enclosureList
            }, { it, _ ->
                var nbtCompound = it
                val version = nbtCompound.getInt(DATA_VERSION_KEY)
                if (version != DATA_VERSION) {
                    LOGGER.info(
                        "Updating enclosure data from version {} to {}",
                        version,
                        DATA_VERSION
                    )
                    for (i in version until DATA_VERSION) {
                        nbtCompound = DataUpdater.update(i, nbtCompound)
                    }
                    update.plain = true
                    update.set(true)
                }
                val enclosureList = EnclosureList(nbtCompound, world, true)
                if (update.get()) {
                    enclosureList.markDirty()
                }
                enclosureList
            }, null)
            world.chunkManager.persistentStateManager.getOrCreate(type, ENCLOSURE_LIST_KEY)
        })
        ServerLifecycleEvents.SERVER_STARTED.register {
            //backupManager = BackupManager()
            playerSessions[CONSOLE] = Session(null)
            Converter.convert()
            runCatching { checkUpdateThread.start() }
        }
        ServerLifecycleEvents.SERVER_STOPPING.register {
            checkUpdateThread.interrupt()
            playerSessions.clear()
        }
        ServerLifecycleEvents.SERVER_STOPPED.register {
            enclosures.clear()
        }

        LOGGER.info("Enclosure enabled now!")
    }
}
