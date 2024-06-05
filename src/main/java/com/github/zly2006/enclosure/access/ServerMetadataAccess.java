package com.github.zly2006.enclosure.access;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;

import java.util.Optional;

public interface ServerMetadataAccess {
    String enclosure$getModName();
    Version enclosure$getModVersion();

    void enclosure$setModVersion(Version version);

    void enclosure$setModName(String name);

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "DataFlowIssue"})
    static ServerMetadata newMetadata(Text text, Optional<ServerMetadata.Players> players, Optional<ServerMetadata.Version> version, Optional<ServerMetadata.Favicon> favicon, Boolean onlineMode, String s, String s1) {
        ServerMetadata metadata = new ServerMetadata(text, players, version, favicon, onlineMode);
        ((ServerMetadataAccess) metadata).enclosure$setModName(s);
        try {
            ((ServerMetadataAccess) metadata).enclosure$setModVersion(Version.parse(s1));
        } catch (VersionParsingException e) {
            ((ServerMetadataAccess) metadata).enclosure$setModVersion(null);
        }
        return metadata;
    }
}
