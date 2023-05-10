package com.github.zly2006.enclosure.utils

import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class Permission(
    val name: String,
    val target: Target,
    val permissions: Set<String>,
    val defaultValue: Boolean,
    val isIgnoreOp: Boolean,
    val description: Text,
    val icon: Item
) : Serializable2Text {
    constructor(name: String, target: Target = Target.Both, permissions: Set<String> = mutableSetOf(), defaultValue: Boolean = false, icon: Item):
            this(name, target, permissions, defaultValue, true, TrT.of("enclosure.permission.$name"), icon)

    constructor(name: String, target: Target = Target.Both, defaultValue: Boolean = false, icon: Item):
            this(name, target, setOf<String>(name), defaultValue, icon)

    fun getValue(map: Map<String, Boolean>): Boolean? {
        for (permission in permissions) {
            if (map.containsKey(permission)) {
                if (!map[permission]!!) {
                    return false
                }
            } else {
                return null
            }
        }
        return true
    }

    fun setValue(map: MutableMap<String, Boolean>, value: Boolean?) {
        for (permission in permissions) {
            if (value != null) {
                map[permission] = value
            } else {
                map.remove(permission)
            }
        }
    }

    fun getNoPermissionMsg(player: PlayerEntity?): MutableText {
        return TrT.of("enclosure.message.no_permission").formatted(Formatting.GOLD)
            .append(serialize(SerializationSettings.Summarize, (player as? ServerPlayerEntity)!!)
                .styled { style: Style -> style.withColor(Formatting.RED) })
    }

    override fun serialize(settings: SerializationSettings, player: ServerPlayerEntity?): MutableText {
        return when (settings) {
            SerializationSettings.Name -> Text.literal(name)
            SerializationSettings.Full -> Text.literal(name)
                .formatted(Formatting.YELLOW)
                .hoverText(
                    TrT.of("enclosure.widget.default_value_is")
                        .append(" ").append(defaultValue.toString())
                )

                .append(" - ")
                .append(description.copy().formatted(Formatting.GOLD))

            SerializationSettings.Summarize -> serialize(
                SerializationSettings.Name,
                player
            ).hoverText(description)

            else -> throw RuntimeException("Unknown serialization settings: $settings")
        }
    }

    enum class Target {
        Enclosure,
        Player,
        Both;

        fun fitEnclosure(): Boolean {
            return this == Enclosure || this == Both
        }

        fun fitPlayer(): Boolean {
            return this == Player || this == Both
        }
    }

    companion object {
        @JvmField val PERMISSIONS: MutableMap<String, Permission> = mutableMapOf()
        // Special permissions
        @JvmField val ADMIN = Permission("admin", Target.Player, false, Items.COMMAND_BLOCK)
        @JvmField val ALL = Permission("all", Target.Player, mutableSetOf(), false, Items.PLAYER_HEAD)
        @JvmField val TRUSTED = Permission("trusted", Target.Player, mutableSetOf(), false, Items.PLAYER_HEAD)
        init {
            PERMISSIONS[ALL.name] = ALL
            PERMISSIONS[TRUSTED.name] = TRUSTED
            PERMISSIONS[ADMIN.name] = ADMIN
            (ALL.permissions as MutableSet).add(ADMIN.name)
        }
        // Common permissions
        @JvmField val RESPAWN_ANCHOR = Permission("respawn_anchor", icon = Items.RESPAWN_ANCHOR).apply(::register)
        @JvmField val ANVIL = Permission("anvil", icon = Items.ANVIL).apply(::register)
        @JvmField val HORSE = Permission("horse", icon = Items.SADDLE).apply(::register)
        @JvmField val BEACON = Permission("beacon", icon = Items.BEACON).apply(::register)
        @JvmField val BED = Permission("bed", icon = Items.RED_BED).apply(::register)
        @JvmField val CAKE = Permission("cake", icon = Items.CAKE).apply(::register)
        @JvmField val TELEPORT = Permission("teleport", Target.Both, true, Items.ENDER_PEARL).apply(::register)
        @JvmField val COMMAND_TP = Permission("cmd_tp", Target.Enclosure, true, Items.CHAIN_COMMAND_BLOCK).apply(::register)
        @JvmField val CONTAINER = Permission("container", icon = Items.CHEST).apply(::register)
        @JvmField val DYE = Permission("dye", icon = Items.BLUE_DYE).apply(::register)
        @JvmField val PICK_BERRIES = Permission("pick_berries", icon = Items.SWEET_BERRIES).apply(::register)
        @JvmField val DOOR = Permission("door", icon = Items.OAK_DOOR).apply(::register)
        @JvmField val DRAGON_EGG = Permission("dragon_egg", icon = Items.DRAGON_EGG).apply(::register)
        @JvmField val HONEY = Permission("honey", icon = Items.HONEY_BOTTLE).apply(::register)
        @JvmField val LEASH = Permission("leash", icon = Items.LEAD).apply(::register)
        @JvmField val NAMETAG = Permission("nametag", icon = Items.NAME_TAG).apply(::register)
        @JvmField val NOTE = Permission("note", icon = Items.NOTE_BLOCK).apply(::register)
        @JvmField val PARROT_COOKIE = Permission("parrot_cookie", icon = Items.COOKIE).apply(::register)
        @JvmField val PVP = Permission("pvp", Target.Enclosure, false, Items.DIAMOND_SWORD).apply(::register)
        @JvmField val PLACE_BLOCK = Permission("place_block", icon = Items.STONE).apply(::register)
        @JvmField val BREAK_BLOCK = Permission("break_block", icon = Items.DIAMOND_PICKAXE).apply(::register)
        @JvmField val REDSTONE = Permission("redstone", icon = Items.REDSTONE).apply(::register)
        @JvmField val SHEAR = Permission("shear", icon = Items.SHEARS).apply(::register)
        @JvmField val SHOOT = Permission("shoot", icon = Items.BOW).apply(::register)
        @JvmField val USE_BONE_MEAL = Permission("use_bone_meal", icon = Items.BONE_MEAL).apply(::register)
        @JvmField val USE_CAMPFIRE = Permission("use_campfire", icon = Items.CAMPFIRE).apply(::register)
        @JvmField val USE_DIRT = Permission("use_dirt", icon = Items.DIRT).apply(::register)
        @JvmField val USE_JUKEBOX = Permission("use_jukebox", icon = Items.JUKEBOX).apply(::register)
        @JvmField val TAKE_BOOK = Permission("take_book", icon = Items.LECTERN).apply(::register)
        @JvmField val STRIP_LOG = Permission("strip_log", icon = Items.OAK_LOG).apply(::register)
        @JvmField val VEHICLE = Permission("vehicle", icon = Items.MINECART).apply(::register)
        @JvmField val WITHER_SPAWN = Permission("wither_spawn", Target.Enclosure, false, Items.WITHER_SKELETON_SKULL).apply(::register)
        @JvmField val ATTACK_ENTITY = Permission("attack_entity", icon = Items.IRON_SWORD).apply(::register)
        @JvmField val ATTACK_ANIMAL = Permission("attack_animal", icon = Items.CHICKEN).apply(::register)
        @JvmField val FEED_ANIMAL = Permission("feed_animal", icon = Items.WHEAT).apply(::register)
        @JvmField val ATTACK_MONSTER = Permission("attack_monster", Target.Both, true, Items.ZOMBIE_HEAD).apply(::register)
        @JvmField val ATTACK_VILLAGER = Permission("attack_villager", icon = Items.EMERALD).apply(::register)
        @JvmField val MOVE = Permission("move", Target.Both, true, Items.BARRIER).apply(::register)
        @JvmField val EXPLOSION = Permission("explosion", Target.Enclosure, false, Items.TNT).apply(::register)
        @JvmField val FLUID = Permission("fluid", Target.Enclosure, false, Items.WATER_BUCKET).apply(::register)
        @JvmField val FALLING_BLOCK = Permission("falling_block", Target.Enclosure, false, Items.SAND).apply(::register)
        @JvmField val PISTON = Permission("piston", Target.Enclosure, false, Items.PISTON).apply(::register)
        @JvmField val GLOWING = Permission("glowing", Target.Enclosure, setOf("glowing"), false, false, TrT.of("enclosure.permission.glowing"), Items.SPECTRAL_ARROW).apply(::register)
        @JvmField val FIRE_SPREADING = Permission("fire_spreading", Target.Enclosure, false, Items.FLINT_AND_STEEL).apply(::register)
        @JvmField val DRAGON_DESTROY = Permission("dragon_destroy", Target.Enclosure, false, Items.DRAGON_HEAD).apply(::register)
        @JvmField val WITHER_DESTROY = Permission("wither_destroy", Target.Enclosure, false, Items.WITHER_SKELETON_SKULL).apply(::register)
        @JvmField val WITHER_ENTER = Permission("wither_enter", Target.Enclosure, true, Items.WITHER_SKELETON_SKULL).apply(::register)
        @JvmField val SCULK_SPREAD = Permission("sculk_spread", Target.Enclosure, false, Items.SCULK_CATALYST).apply(::register)
        @JvmField val DROP_ITEM = Permission("drop_item", Target.Both, true, Items.DIRT).apply(::register)
        @JvmField val PICKUP_ITEM = Permission("pickup_item", Target.Both, true, Items.DIRT).apply(::register)
        @JvmField val FISH = Permission("fish", icon = Items.FISHING_ROD).apply(::register)
        @JvmField val FARMLAND_DESTROY = Permission("farmland_destroy", icon = Items.FARMLAND).apply(::register)
        @JvmField val ARMOR_STAND = Permission("armor_stand", icon = Items.ARMOR_STAND).apply(::register)
        @JvmField val ALLAY = Permission("allay", icon = Items.ALLAY_SPAWN_EGG).apply(::register)
        @JvmField val CONSUMPTIVELY_EXTINGUISH = Permission("consumptively_extinguish", Target.Enclosure, false, Items.POWDER_SNOW_BUCKET).apply(::register)
        @JvmField val CAULDRON = Permission("cauldron", icon = Items.CAULDRON).apply(::register)
        @JvmField val BREAK_TURTLE_EGG = Permission("break_turtle_egg", Target.Both, true, Items.TURTLE_EGG).apply(::register)
        @JvmField val ITEM_FRAME = Permission("item_frame", icon = Items.ITEM_FRAME).apply(::register)
        @JvmField val PRIME_TNT = Permission("prime_tnt", icon = Items.TNT).apply(::register)
        @JvmField val DISPENSER = Permission("dispenser", Target.Enclosure, icon = Items.DISPENSER).apply(::register)

        fun getValue(name: String): Permission? {
            return PERMISSIONS[name]
        }

        fun register(permission: Permission) {
            PERMISSIONS[permission.name] = permission
            if (permission.target.fitPlayer() && permission.isIgnoreOp) {
                (ALL.permissions as MutableSet).addAll(permission.permissions)
                (TRUSTED.permissions as MutableSet).addAll(permission.permissions)
            }
        }

        fun suggest(target: Target): Set<String> {
            return PERMISSIONS.values.asSequence()
                .filter { permission -> permission.target.fitPlayer() && target.fitPlayer() || permission.target.fitEnclosure() && target.fitEnclosure() }
                .map { permission -> permission.name }
                .toSet()
        }
    }
}
