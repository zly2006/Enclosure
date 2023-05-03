package com.github.zly2006.enclosure.utils;

import java.io.IOException;
import java.io.InputStream;

public class ResourceLoader {
    private static final String LANGUAGE_FILES_PATH = "/assets/enclosure/lang/";

    public ResourceLoader(){}

    public static String getLanguageFile(String langCode) throws IOException {
        try (InputStream resource = ResourceLoader.class.getResourceAsStream(LANGUAGE_FILES_PATH + langCode + ".json")) {
            if (resource == null) {
                throw new IOException("Language file not found: " + langCode);
            }
            return new String(resource.readAllBytes());
        }
    }
}
