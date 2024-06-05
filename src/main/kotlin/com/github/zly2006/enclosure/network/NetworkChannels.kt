package com.github.zly2006.enclosure.network;

import net.minecraft.util.Identifier;

public class NetworkChannels {
    public static final Identifier ENCLOSURE_INSTALLED = Identifier.of("enclosure", "packet.installed");
    public static final Identifier OPEN_REQUEST = Identifier.of("enclosure", "packet.request_open_screen");
    public static final Identifier SYNC_SELECTION = Identifier.of("enclosure", "packet.sync_selection");
    public static final Identifier SYNC_UUID = Identifier.of("enclosure", "packet.uuid");
    public static final Identifier CONFIRM = Identifier.of("enclosure", "packet.confirm");
    public static final Identifier SYNC_PERMISSION = Identifier.of("enclosure", "packet.sync_permission");
}
