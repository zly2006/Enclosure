package com.github.zly2006.enclosure.access;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;

import java.util.Optional;

public interface ServerMetadataAccess {
    String getModName();
    Version getModVersion();

    void setModVersion(Version version);

    void setModName(String name);

    static ServerMetadata newMetadata(Text text, Optional<ServerMetadata.Players> players, Optional<ServerMetadata.Version> version, Optional<ServerMetadata.Favicon> favicon, Boolean onlineMode, String s, String s1) {
        ServerMetadata metadata = new ServerMetadata(text, players, version, favicon, onlineMode);
        ((ServerMetadataAccess) metadata).setModName(s);
        try {
            ((ServerMetadataAccess) metadata).setModVersion(Version.parse(s1));
        } catch (VersionParsingException e) {
            ((ServerMetadataAccess) metadata).setModVersion(null);
        }
        return metadata;
    }
}
