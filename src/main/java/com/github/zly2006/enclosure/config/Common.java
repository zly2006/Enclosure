package com.github.zly2006.enclosure.config;

import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

public class Common {
    @SerializedName("default_enter_message")
    public String defaultEnterMessage = "§eWelcome §a%player% §eto %full_name% owned by %owner%.";
    @SerializedName("default_leave_message")
    public String defaultLeaveMessage = "§eGoodbye, §a%player%!";
    @SerializedName("max_enclosure_name_length")
    public int maxEnclosureNameLength = 16;
    @SerializedName("default_language_key")
    public String defaultLanguageKey = "en_us";
    @SerializedName("aliases")
    public List<String> aliases = new ArrayList<>() {{
        add("res");
        add("land");
    }};
    @SerializedName("develop_mode")
    public boolean developMode = FabricLoader.getInstance().isDevelopmentEnvironment();
    @SerializedName("allow_rich_message")
    public boolean allowRichMessage = false;
    @SerializedName("inject_server_language")
    public boolean injectServerLanguage = true;
    @SerializedName("show_teleport_warning")
    public boolean showTeleportWarning = true;
    @SerializedName("show_message_prefix")
    public boolean showMessagePrefix = false;
    @SerializedName("teleport_cooldown")
    public int teleportCooldown = 10000;
    @SerializedName("enter_message_header")
    public String enterMessageHeader = "";
    @SerializedName("leave_message_header")
    public String leaveMessageHeader = "";
    @SerializedName("check_update")
    public boolean checkUpdate = true;
}
