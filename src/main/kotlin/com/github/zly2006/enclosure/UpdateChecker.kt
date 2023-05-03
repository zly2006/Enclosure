package com.github.zly2006.enclosure

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import net.fabricmc.loader.api.Version
import net.fabricmc.loader.api.VersionParsingException
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.text.SimpleDateFormat

class VersionEntry(
    var versionId: String,
    var url: String = "https://modrinth.com/mod/enclosure/version/$versionId",
    var releasedTime: Long = 0,
    var versionNumber: Version,
    var versionType: VersionType,
    var releaseMessage: String = "",
    var gameVersions: List<String> = emptyList(),
    var modLoaders: List<String> = emptyList()
) {
    enum class VersionType {
        RELEASE, ALPHA, BETA;
        fun toColor(): Formatting {
            return when (this) {
                RELEASE -> Formatting.GREEN
                ALPHA -> Formatting.RED
                BETA -> Formatting.YELLOW
            }
        }
        companion object {
            fun fromString(str: String): VersionType {
                return when (str) {
                    "release" -> RELEASE
                    "alpha" -> ALPHA
                    "beta" -> BETA
                    else -> throw IllegalArgumentException("Unknown version type: $str")
                }
            }
        }
    }
}

class UpdateChecker {
    private var lastCheckTime: Long = 0
    var latestVersion: VersionEntry? = null
    fun notifyUpdate(serverPlayer: ServerPlayerEntity) {
        if (latestVersion == null) {
            return
        }
        serverPlayer.sendMessage(Text.literal("A new version of enclosure is available: ")
            .styled {
                it
                    .withColor(Formatting.YELLOW)
                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, latestVersion!!.url))
            }
            .append(Text.literal(latestVersion!!.versionNumber.toString()).formatted(latestVersion!!.versionType.toColor()))
            .append(Text.literal("\nClick here to download.").formatted(Formatting.AQUA)), false)
    }

    fun check() {
        if (Thread.currentThread() === minecraftServer.thread) {
            throw RuntimeException("UpdateChecker.check() can not be called on the main thread")
        }
        if (System.currentTimeMillis() - lastCheckTime < 1000 * 60 * 60) {
            return
        }
        try {
            val httpClient = HttpClient.newHttpClient()
            // Only check release version if not in develop mode
            // fit for this minecraft version
            val versions = GSON.fromJson(
                httpClient.send(
                    HttpRequest.newBuilder(URI("https://api.modrinth.com/v2/project/enclosure/version")).build(),
                    HttpResponse.BodyHandlers.ofString()
                ).body(), JsonArray::class.java
            ).map { obj: JsonElement -> obj.asJsonObject }
                .map { v ->
                    VersionEntry(
                        versionId = v["id"].asString,
                        releasedTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(v["date_published"].asString).time,
                        versionNumber = Version.parse(v["version_number"].asString),
                        versionType = VersionEntry.VersionType.fromString(v["version_type"].asString),
                        gameVersions = v["game_versions"].asJsonArray.map { it.asString },
                        modLoaders = v["loaders"].asJsonArray.map { it.asString }
                    )
                }
            val filteredVersions = versions.filter {
                if (!ServerMain.commonConfig.developMode) {
                    // Only check release version if not in develop mode
                    if (it.versionType != VersionEntry.VersionType.RELEASE) {
                        return@filter false
                    }
                }
                return@filter it.gameVersions.contains(minecraftServer.version) && it.versionNumber > MOD_VERSION
            }.sortedWith{v1, v2 ->
                if (v1.versionNumber != v2.versionNumber) {
                    return@sortedWith v1.versionNumber.compareTo(v2.versionNumber)
                }
                return@sortedWith v1.releasedTime.compareTo(v2.releasedTime)
            }
            val latest = versions
                .filter {
                    if (!ServerMain.commonConfig.developMode) {
                        // Only check release version if not in develop mode
                        if (it.versionType != VersionEntry.VersionType.RELEASE) {
                            return@filter false
                        }
                    }
                    return@filter it.gameVersions.contains(minecraftServer.version)
                }.maxByOrNull { it.versionNumber }
            if (latest != null) {
                latestVersion = latest
                LOGGER.info("Found latest version: ${latestVersion?.versionNumber}, url: ${latestVersion?.url}")
            }
            lastCheckTime = System.currentTimeMillis()
        } catch (ignored: IOException) {
        } catch (ignored: URISyntaxException) {
        } catch (ignored: InterruptedException) {
        } catch (ignored: VersionParsingException) {
        }
    }
}