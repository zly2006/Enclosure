package com.github.zly2006.enclosure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static com.github.zly2006.enclosure.ServerMain.*;

public class UpdateChecker {
    long lastCheckTime = 0;
    String latestVersion = null;
    Version latestVersionParsed = null;
    String latestVersionUrl = null;

    void notifyUpdate(ServerPlayerEntity serverPlayer) {
        if (latestVersion == null) {
            return;
        }
        serverPlayer.sendMessage(Text.literal("A new version of enclosure is available: " + latestVersion)
            .styled(style -> style
                .withColor(Formatting.YELLOW)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, latestVersionUrl)))
            .append(Text.literal("\nClick here to download.").formatted(Formatting.AQUA)), false);
    }
    void check() {
        if (Thread.currentThread() == minecraftServer.getThread()) {
            throw new RuntimeException("UpdateChecker.check() can not be called on the main thread");
        }
        if (System.currentTimeMillis() - lastCheckTime < 1000 * 60 * 60) {
            return;
        }
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            // Only check release version if not in develop mode
            // fit for this minecraft version
            Optional<JsonObject> latest = GSON.fromJson(httpClient.send(
                    HttpRequest.newBuilder(new URI("https://api.modrinth.com/v2/project/enclosure/version")).build(),
                    HttpResponse.BodyHandlers.ofString()
                ).body(), JsonArray.class).asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(je -> {
                    if (!commonConfig.developMode) {
                        // Only check release version if not in develop mode
                        if (!je.get("version_type").getAsString().equals("release")) {
                            return false;
                        }
                    }
                    // fit for this minecraft version
                    return je.get("game_versions").getAsJsonArray().contains(new JsonPrimitive(minecraftServer.getVersion()));
                }).max((je1, je2) -> {
                    try {
                        return Version.parse(je1.get("version_number").getAsString()).compareTo(Version.parse(je2.get("version_number").getAsString()));
                    } catch (VersionParsingException e) {
                        return 0;
                    }
                });
            if (latest.isPresent()) {
                latestVersion = latest.get().get("version_number").getAsString();
                String versionType = latest.get().get("version_type").getAsString();
                if (!"release".equals(versionType)) {
                    latestVersion += " (" + versionType + ")";
                }
                latestVersionParsed = Version.parse(latest.get().get("version_number").getAsString());
                latestVersionUrl = "https://modrinth.com/mod/enclosure/version/" + latest.get().get("id").getAsString();
                LOGGER.info("Found latest version: " + latestVersion + ", url: " + latestVersionUrl);
            }
            lastCheckTime = System.currentTimeMillis();
        } catch (IOException | URISyntaxException | InterruptedException | VersionParsingException ignored) {
        }
    }
}
