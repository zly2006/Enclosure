package com.github.zly2006.enclosure.access;

import net.fabricmc.loader.api.Version;

public interface ServerMetadataAccess {
    String getModName();
    Version getModVersion();

    void setModVersion(Version version);

    void setModName(String name);
}
