package com.github.zly2006.enclosure.config;

import com.github.zly2006.enclosure.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import static com.github.zly2006.enclosure.ServerMainKt.LOGGER;
import static com.github.zly2006.enclosure.ServerMainKt.minecraftServer;

public class Converter {

    private static final Yaml YAML_INSTANCE = new Yaml();
    private static final Map<String, String> PERMISSION_MAP = new HashMap<>() {{
        /*
         key为bukkit-enclosure插件的，value为fabric-enclosure mod的
         如果value中带有!，则最终需要把权限进行反转（即读到true写false，反之亦然）
         */
        put("admin", "admin");
        put("anchor", "respawn_anchor");
        put("animalkilling", "attack_animal");
        put("cmonsters", "attack_monster");
        put("anvil", "anvil");
        put("beacon", "beacon");
        put("bed", "bed");
        put("cake", "cake");
        put("chorustp", "chorustp");
        put("container", "container");
        put("dye", "dye");
        put("destroy", "break_block");
        put("egg", "dragon_egg");
        put("firespread", "fire_spreading");
        put("glow", "glowing");
        put("honey", "honey");
        put("hook", "fish");
        put("leash", "leash");
        put("move", "move");
        put("nametag", "nametag");
        put("note", "note");
        put("pvp", "pvp");
        put("place", "place_block");
        put("pistonprotection", "!piston");
        put("riding", "backup");
        put("shear", "shear");
        put("shoot", "shoot");
        put("tnt", "explosion");
        put("use", "use_item");
        put("vehicledestroy", "vehicle_destroy");
        put("witherspawn", "wither_spawn");
    }};

    public static void convert() {
        // 不存在储存旧配置文件的文件夹时创建一个
        File oldConfDictionary = ServerMainKt.OLD_CONF_PATH.toFile();
        if (!oldConfDictionary.exists() || oldConfDictionary.isFile()) {
            oldConfDictionary.mkdirs();
        }

        Map<RegistryKey<World>, File> oldConfFiles = new HashMap<>() {{
            File oldConfOverworld = new File(oldConfDictionary, "res_world.yml");
            File oldConfNether = new File(oldConfDictionary, "res_world_nether.yml");
            File oldConfEnd = new File(oldConfDictionary, "res_world_the_end.yml");
            if (oldConfOverworld.exists()) {
                put(World.OVERWORLD, oldConfOverworld);
            }
            if (oldConfNether.exists()) {
                put(World.NETHER, oldConfNether);
            }
            if (oldConfEnd.exists()) {
                put(World.END, oldConfEnd);
            }
        }};

        if (!oldConfFiles.isEmpty()) {
            LOGGER.info("Found the old config file(s), converting!");

            minecraftServer.getWorlds().forEach(serverWorld -> {
                EnclosureList enclosureList = ServerMain.INSTANCE.getAllEnclosures(serverWorld);

                try {
                    File oldConf = oldConfFiles.get(serverWorld.getRegistryKey());
                    Map<String, Object> confData = YAML_INSTANCE.load(new FileInputStream(oldConf));
                    LOGGER.info("Converting [%s] data from residence format to enclosures...".formatted(serverWorld.getRegistryKey().getValue().toString()));
                    convertToList(confData, enclosureList, serverWorld);
                    oldConf.renameTo(new File(oldConfDictionary, oldConf.getName() + ".converted"));
                } catch (Exception ignore) {
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static void convertToList(Map<String, Object> data, EnclosureList enclosureList, ServerWorld world) {
        List<NbtCompound> nbtList = new ArrayList<>();

        Map<String, Map<Object, Object>> enclosuresList = (Map<String, Map<Object, Object>>) data.get("Residences");
        Map<Integer, Map<String, Boolean>> permissionList = (Map<Integer, Map<String, Boolean>>) data.get("Flags");
        Map<Integer, Map<String, String>> messageList = (Map<Integer, Map<String, String>>) data.get("Messages");

        try {
            enclosuresList.forEach((key, value) -> {
                NbtCompound nbt = new NbtCompound();
                if (value.get("Messages") != null) {
                    nbt.putString("enter_msg", messageList.get((Integer) value.get("Messages")).get("EnterMessage")
                            .replace("%player", "%player%")
                            .replace("%residence", "%full_name%")
                            .replace("%owner", "%owner%")
                            .replace("&", "§")
                            .replace("§§", "&"));
                    nbt.putString("leave_msg", messageList.get((Integer) value.get("Messages")).get("LeaveMessage")
                            .replace("%player", "%player%")
                            .replace("%residence", "%full_name%")
                            .replace("%owner", "%owner%")
                            .replace("&", "§")
                            .replace("§§", "&"));
                }

                nbt.putString("name", key);
                nbt.putLong("created_on", (Long) value.get("CreatedOn"));

                String[] landPosition = ((Map<String, String>) (value.get("Areas"))).get("main").split(":");
                if (landPosition.length < 6) {
                    throw new RuntimeException("Incorrect area positions pattern");
                }
                nbt.putInt("min_x", Integer.parseInt(landPosition[0]));
                nbt.putInt("min_y", Integer.parseInt(landPosition[1]));
                nbt.putInt("min_z", Integer.parseInt(landPosition[2]));
                nbt.putInt("max_x", Integer.parseInt(landPosition[3]));
                nbt.putInt("max_y", Integer.parseInt(landPosition[4]));
                nbt.putInt("max_z", Integer.parseInt(landPosition[5]));

                UUID ownerUuid = UUID.fromString((String) (((Map<String, Object>) (value.get("Permissions"))).get("OwnerUUID")));
                String ownerName = (String) ((Map<String, Object>) (value.get("Permissions"))).get("OwnerLastKnownName");

                nbt.putUuid("owner", ownerUuid);

                minecraftServer.getUserCache().add(new GameProfile(ownerUuid, ownerName));

                String tpLocationString = (String) value.get("TPLoc");
                if (tpLocationString != null) {
                    String[] teleportPosition = (tpLocationString).split(":");
                    if (teleportPosition.length < 3) {
                        throw new RuntimeException("Incorrect area positions pattern");
                    }
                    NbtList nbtTpPos = new NbtList();
                    nbtTpPos.add(NbtInt.of((int) Double.parseDouble(teleportPosition[0])));
                    nbtTpPos.add(NbtInt.of((int) Double.parseDouble(teleportPosition[1])));
                    nbtTpPos.add(NbtInt.of((int) Double.parseDouble(teleportPosition[2])));
                    nbt.put("tp_pos", nbtTpPos);
                }
                else {
                    NbtList nbtTpPos = new NbtList();
                    nbtTpPos.add(NbtInt.of(Integer.parseInt(landPosition[0])));
                    nbtTpPos.add(NbtInt.of(Integer.parseInt(landPosition[1])));
                    nbtTpPos.add(NbtInt.of(Integer.parseInt(landPosition[2])));
                    nbt.put("tp_pos", nbtTpPos);
                }

                NbtCompound nbtPermission = new NbtCompound();
                Map<String, Integer> playerPermissionMap = ((Map<String, Map<String, Integer>>) (value.get("Permissions"))).get("PlayerFlags");
                int areaFlags = ((Map<String, Integer>) (value.get("Permissions"))).get("AreaFlags");

                if (playerPermissionMap != null) {
                    playerPermissionMap.forEach((playerUuid, permissionPool) ->
                            nbtPermission.put(playerUuid, parsePermissions(permissionList, permissionPool)));
                }

                if (areaFlags > 0) {
                    nbtPermission.put("0000000-0000-0000-0000-000000000000", parsePermissions(permissionList, areaFlags));
                }

                nbt.put("permission", nbtPermission);
                nbtList.add(nbt);
            });

            nbtList.forEach(item -> {
                if (item != null) {
                    Enclosure enclosure = new Enclosure(item, world);
                    MutableText status = null;
                    for (EnclosureArea area : enclosureList.getAreas()) {
                        if (enclosure.equals(area)) {
                            status = Text.literal(ServerMain.INSTANCE.getTranslation().get("enclosure.message.existed").getAsString());
                        }
                        else if (enclosure.intersect(area)) {
                            status = Text.literal(ServerMain.INSTANCE.getTranslation().get("enclosure.message.intersected").getAsString()).append(area.getFullName());
                        }
                        else if (enclosure.getName().equals(area.getName())) {
                            status = Text.literal(ServerMain.INSTANCE.getTranslation().get("enclosure.message.name_in_use").getAsString());
                        }
                    }
                    if (status == null) {
                        enclosureList.addArea(enclosure);
                    }
                    else {
                        LOGGER.error("There was a error land which named \"" + enclosure.getFullName() + "\" while converting.");
                        LOGGER.error("Error type:" + status);
                    }
                }
            });
            LOGGER.info("Convert finished!");
        } catch (Exception e) {
            LOGGER.error("An error occurred during the conversion of the configuration file. The following is the error stack information:");
            e.printStackTrace();
        }
    }

    private static NbtCompound parsePermissions(Map<Integer, Map<String, Boolean>> permissionList, Integer permissionPool) {
        Map<String, Boolean> permissionConfData = permissionList.get(permissionPool);
        NbtCompound compound = new NbtCompound();
        for (Map.Entry<String, Boolean> entry : permissionConfData.entrySet()) {
            String permissionKey = PERMISSION_MAP.getOrDefault(entry.getKey(), null);
            Boolean permissionVal = entry.getValue();

            if (permissionKey == null) {
                continue;
            }

            if (permissionKey.startsWith("!")) {
                permissionVal = !permissionVal;
                permissionKey = permissionKey.substring(1);
            }

            compound.putBoolean(permissionKey, permissionVal);
        }
        return compound;
    }
}
