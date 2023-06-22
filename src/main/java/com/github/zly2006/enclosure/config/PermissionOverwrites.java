package com.github.zly2006.enclosure.config;

import com.github.zly2006.enclosure.utils.Permission;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class PermissionOverwrites {
    public static class PermissionOverwrite {
        final Permission.Target target;
        @SerializedName("can_player_set")
        final boolean canPlayerSet;

        public PermissionOverwrite(Permission.Target target, boolean canPlayerSet) {
            this.target = target;
            this.canPlayerSet = canPlayerSet;
        }
    }
    Map<String, PermissionOverwrite> overwrites;
    @SerializedName("sync_to_client")
    boolean syncToClient = true;
}
