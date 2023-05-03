package com.github.zly2006.enclosure.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings.Summarize;

public class Permission implements Serializable2Text {
    public static final Permission ADMIN = new Permission("admin", Permission.Target.Player, false, Items.COMMAND_BLOCK);

    public static final Permission ALL = new Permission("all", Permission.Target.Player, new HashSet<>(), false, Items.PLAYER_HEAD);

    public static final Permission TRUSTED = new Permission("trusted", Permission.Target.Player, new HashSet<>(), false, Items.PLAYER_HEAD);

    public static final Permission RESPAWN_ANCHOR = new Permission("respawn_anchor", Target.Both, false, Items.RESPAWN_ANCHOR);

    public static final Permission ANVIL = new Permission("anvil", Permission.Target.Both, false, Items.ANVIL);

    public static final Permission HORSE = new Permission("horse", Target.Both, false, Items.SADDLE);

    public static final Permission BEACON = new Permission("beacon", Permission.Target.Both, false, Items.BEACON);

    public static final Permission BED = new Permission("bed", Permission.Target.Both, false, Items.RED_BED);

    public static final Permission CAKE = new Permission("cake", Target.Both, false, Items.CAKE);

    public static final Permission TELEPORT = new Permission("teleport", Target.Both, true, Items.ENDER_PEARL);

    public static final Permission COMMAND_TP = new Permission("cmd_tp", Target.Enclosure, true, Items.CHAIN_COMMAND_BLOCK);

    public static final Permission CONTAINER = new Permission("container", Target.Both, false, Items.CHEST);

    public static final Permission DYE = new Permission("dye", Target.Both, false, Items.BLUE_DYE);

    public static final Permission PICK_BERRIES = new Permission("pick_berries", Target.Both, false, Items.SWEET_BERRIES);

    public static final Permission DOOR = new Permission("door", Target.Both, true, Items.OAK_DOOR);

    public static final Permission DRAGON_EGG = new Permission("dragon_egg", Target.Both, false, Items.DRAGON_EGG);

    public static final Permission HONEY = new Permission("honey", Target.Both, false, Items.HONEY_BOTTLE);

    public static final Permission LEASH = new Permission("leash", Target.Both, false, Items.LEAD);

    public static final Permission NAMETAG = new Permission("nametag", Target.Both, false, Items.NAME_TAG);

    public static final Permission NOTE = new Permission("note", Target.Both, false, Items.NOTE_BLOCK);

    public static final Permission PARROT_COOKIE = new Permission("parrot_cookie", Target.Both, false, Items.COOKIE);

    public static final Permission PVP = new Permission("pvp", Target.Enclosure, false, Items.DIAMOND_SWORD);

    public static final Permission PLACE_BLOCK = new Permission("place_block", Target.Both, false, Items.STONE);

    public static final Permission BREAK_BLOCK = new Permission("break_block", Target.Both, false, Items.DIAMOND_PICKAXE);

    public static final Permission REDSTONE = new Permission("redstone", Target.Both, false, Items.REDSTONE);

    public static final Permission SHEAR = new Permission("shear", Target.Both, false, Items.SHEARS);

    public static final Permission SHOOT = new Permission("shoot", Target.Both, false, Items.BOW);

    public static final Permission USE_BONE_MEAL = new Permission("use_bone_meal", Target.Both, false, Items.BONE_MEAL);

    public static final Permission USE_CAMPFIRE = new Permission("use_campfire", Target.Both, false, Items.CAMPFIRE);

    public static final Permission USE_DIRT = new Permission("use_dirt", Target.Both, false, Items.DIRT);

    public static final Permission USE_JUKEBOX = new Permission("use_jukebox", Target.Both, false, Items.JUKEBOX);

    public static final Permission TAKE_BOOK = new Permission("take_book", Target.Both, false, Items.LECTERN);

    public static final Permission STRIP_LOG = new Permission("strip_log", Target.Both, false, Items.OAK_LOG);

    public static final Permission VEHICLE = new Permission("vehicle", Target.Both, false, Items.MINECART);

    public static final Permission WITHER_SPAWN = new Permission("wither_spawn", Target.Enclosure, false, Items.WITHER_SKELETON_SKULL);

    public static final Permission ATTACK_ENTITY = new Permission("attack_entity", Target.Both, false, Items.IRON_SWORD);

    public static final Permission ATTACK_ANIMAL = new Permission("attack_animal", Target.Both, false, Items.CHICKEN);

    public static final Permission FEED_ANIMAL = new Permission("feed_animal", Target.Both, false, Items.WHEAT);

    public static final Permission ATTACK_MONSTER = new Permission("attack_monster", Target.Both, true, Items.ZOMBIE_HEAD);

    public static final Permission ATTACK_VILLAGER = new Permission("attack_villager", Target.Both, false, Items.EMERALD);

    public static final Permission MOVE = new Permission("move", Target.Both, true, Items.BARRIER);

    public static final Permission EXPLOSION = new Permission("explosion", Permission.Target.Enclosure, false, Items.TNT);

    public static final Permission FLUID = new Permission("fluid", Permission.Target.Enclosure, false, Items.WATER_BUCKET);

    public static final Permission FALLING_BLOCK = new Permission("falling_block", Permission.Target.Enclosure, false, Items.SAND);

    public static final Permission PISTON = new Permission("piston", Permission.Target.Enclosure, false, Items.PISTON);

    public static final Permission GLOWING = new Permission("glowing", Target.Enclosure, Set.of("glowing"), false, false, TrT.of("enclosure.permission.glowing"), Items.SPECTRAL_ARROW);

    public static final Permission FIRE_SPREADING = new Permission("fire_spreading", Permission.Target.Enclosure, false, Items.FLINT_AND_STEEL);

    public static final Permission DRAGON_DESTROY = new Permission("dragon_destroy", Permission.Target.Enclosure, false, Items.DRAGON_HEAD);

    public static final Permission WITHER_DESTROY = new Permission("wither_destroy", Permission.Target.Enclosure, false, Items.WITHER_SKELETON_SKULL);

    public static final Permission WITHER_ENTER = new Permission("wither_enter", Permission.Target.Enclosure, true, Items.WITHER_SKELETON_SKULL);

    public static final Permission SCULK_SPREAD = new Permission("sculk_spread", Permission.Target.Enclosure, false, Items.SCULK_CATALYST);

    public static final Permission DROP_ITEM = new Permission("drop_item", Target.Both, true, Items.DIRT);

    public static final Permission PICKUP_ITEM = new Permission("pickup_item", Target.Both, true, Items.DIRT);

    public static final Permission FISH = new Permission("fish", Target.Both, false, Items.FISHING_ROD);

    public static final Permission FARMLAND_DESTROY = new Permission("farmland_destroy", Target.Both, false, Items.FARMLAND);

    public static final Permission ARMOR_STAND = new Permission("armor_stand", Target.Both, false, Items.ARMOR_STAND);

    public static final Permission ALLAY = new Permission("allay", Target.Both, false, Items.ALLAY_SPAWN_EGG);

    public static final Permission CONSUMPTIVELY_EXTINGUISH = new Permission("consumptively_extinguish", Target.Enclosure, false, Items.POWDER_SNOW_BUCKET);

    public static final Permission CAULDRON = new Permission("cauldron", Target.Both, false, Items.CAULDRON);

    public static final Permission BREAK_TURTLE_EGG = new Permission("break_turtle_egg", Target.Enclosure, true, Items.TURTLE_EGG);

    public static final Map<String, Permission> PERMISSIONS = new HashMap<>();

    static {
        PERMISSIONS.put(ALL.name, ALL);
        PERMISSIONS.put(TRUSTED.name, TRUSTED);
        PERMISSIONS.put(ADMIN.name, ADMIN);
        ALL.permissions.add(ADMIN.name);
        register(BREAK_TURTLE_EGG);
        register(RESPAWN_ANCHOR);
        register(ANVIL);
        register(HORSE);
        register(BEACON);
        register(BED);
        register(CAKE);
        register(TELEPORT);
        register(COMMAND_TP);
        register(CONTAINER);
        register(DOOR);
        register(DYE);
        register(PICK_BERRIES);
        register(DRAGON_EGG);
        register(HONEY);
        register(LEASH);
        register(NAMETAG);
        register(NOTE);
        register(PARROT_COOKIE);
        register(PVP);
        register(PLACE_BLOCK);
        register(BREAK_BLOCK);
        register(USE_BONE_MEAL);
        register(USE_CAMPFIRE);
        register(USE_DIRT);
        register(USE_JUKEBOX);
        register(TAKE_BOOK);
        register(REDSTONE);
        register(SHEAR);
        register(SHOOT);
        register(STRIP_LOG);
        register(VEHICLE);
        register(WITHER_SPAWN);
        register(ATTACK_ENTITY);
        register(ATTACK_ANIMAL);
        register(ATTACK_MONSTER);
        register(ATTACK_VILLAGER);
        register(MOVE);
        register(EXPLOSION);
        register(FLUID);
        register(FALLING_BLOCK);
        register(PISTON);
        register(GLOWING);
        register(FIRE_SPREADING);
        register(DRAGON_DESTROY);
        register(WITHER_DESTROY);
        register(WITHER_ENTER);
        register(SCULK_SPREAD);
        register(FEED_ANIMAL);
        register(DROP_ITEM);
        register(PICKUP_ITEM);
        register(FISH);
        register(FARMLAND_DESTROY);
        register(ARMOR_STAND);
        register(ALLAY);
        register(CONSUMPTIVELY_EXTINGUISH);
        register(CAULDRON);
    }

    final String name;
    final Target target;
    final Set<String> permissions;
    final boolean defaultValue;
    final boolean ignoreOp;
    final Text description;
    final Item icon;

    public Permission(String name, Target target, Set<String> permissions, boolean defaultValue, Item icon) {
        this(name, target, permissions, defaultValue, true, TrT.of("enclosure.permission." + name), icon);
    }

    public Permission(String name, Target target, Set<String> permissions, boolean defaultValue, boolean ignoreOp, Text description, Item icon) {
        this.name = name;
        this.target = target;
        this.permissions = permissions;
        this.defaultValue = defaultValue;
        this.ignoreOp = ignoreOp;
        this.description = description;
        this.icon = icon;
    }

    public Permission(String name, Target target, boolean defaultValue, Item icon) {
        this(name, target, Set.of(name), defaultValue, icon);
    }

    public static Permission getValue(String name) {
        return PERMISSIONS.get(name);
    }

    public static void register(Permission permission) {
        PERMISSIONS.put(permission.name, permission);
        if (permission.target.fitPlayer() && permission.isIgnoreOp()) {
            ALL.getPermissions().addAll(permission.getPermissions());
            TRUSTED.getPermissions().addAll(permission.getPermissions());
        }
    }

    public static Set<String> suggest(Target target) {
        return PERMISSIONS.values().stream().filter(permission ->
                        (permission.target.fitPlayer() && target.fitPlayer()) ||
                                (permission.target.fitEnclosure() && target.fitEnclosure()))
                .map(permission -> permission.name)
                .collect(Collectors.toSet());
    }

    public String getName() {
        return this.name;
    }

    public Target getTarget() {
        return this.target;
    }

    public Set<String> getPermissions() {
        return this.permissions;
    }

    public boolean getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isIgnoreOp() {
        return this.ignoreOp;
    }

    public Optional<Boolean> getValue(Map<String, Boolean> map) {
        for (String permission : permissions) {
            if (map.containsKey(permission)) {
                if (!map.get(permission)) {
                    return Optional.of(false);
                }
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(true);
    }

    public void setValue(Map<String, Boolean> map, @Nullable Boolean value) {
        for (String permission : permissions) {
            if (value != null) {
                map.put(permission, value);
            } else {
                map.remove(permission);
            }
        }
    }

    public Text getDescription() {
        return this.description;
    }

    public Item getIcon() {
        return this.icon;
    }

    public MutableText getNoPermissionMsg(@Nullable PlayerEntity player) {
        return TrT.of("enclosure.message.no_permission").formatted(Formatting.GOLD)
                .append(serialize(Summarize, player instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null)
                        .styled(style -> style.withColor(Formatting.RED)));
    }

    @Override
    public MutableText serialize(SerializationSettings settings, ServerPlayerEntity player) {
        return switch (settings) {
            case Name -> Text.literal(this.name);
            case Full -> Text.literal(this.name).styled(style -> style.withColor(Formatting.YELLOW)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TrT.of("enclosure.widget.default_value_is")
                                    .append(" ").append(String.valueOf(defaultValue)))))
                    .append(" - ")
                    .append(description.copy().styled(style -> style.withColor(Formatting.GOLD)));
            case Summarize ->
                    serialize(SerializationSettings.Name, player).styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, this.description)));
            default -> throw new RuntimeException("Unknown serialization settings: " + settings);
        };
    }

    public enum Target {
        Enclosure,
        Player,
        Both;

        public boolean fitEnclosure() {
            return this == Enclosure || this == Both;
        }

        public boolean fitPlayer() {
            return this == Player || this == Both;
        }
    }
}
