package com.github.zly2006.enclosure.network;

import net.minecraft.util.Identifier;

public class NetworkChannels {
    public static final Identifier ENCLOSURE_INSTALLED = new Identifier("enclosure", "packet.installed");
    public static final Identifier OPEN_REQUEST = new Identifier("enclosure", "packet.request_open_screen");
    public static final Identifier SYNC_SELECTION = new Identifier("enclosure", "packet.sync_selection");
    public static final Identifier SYNC_UUID = new Identifier("enclosure", "packet.uuid");
    public static final Identifier SYNC_ENCLOSURES = new Identifier("enclosure", "packet.sync_enclosures");
    public static final Identifier CONFIRM = new Identifier("enclosure", "packet.confirm");
    public static final Identifier SYNC_PERMISSION = new Identifier("enclosure", "packet.sync_permission");
}
