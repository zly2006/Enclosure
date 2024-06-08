package com.github.zly2006.enclosure.client

import com.github.zly2006.enclosure.MOD_VERSION
import com.github.zly2006.enclosure.command.ClientSession
import com.github.zly2006.enclosure.gui.EnclosureScreen
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler
import com.github.zly2006.enclosure.network.config.EnclosureInstalledC2SPacket
import com.github.zly2006.enclosure.network.play.RequestOpenScreenC2SPPacket
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.C2SConfigurationChannelEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreens
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.util.math.BlockPos
import org.lwjgl.glfw.GLFW
import java.util.*

@Environment(EnvType.CLIENT)
class ClientMain : ClientModInitializer {
    override fun onInitializeClient() {
        HandledScreens.register(EnclosureScreenHandler.ENCLOSURE_SCREEN_HANDLER, ::EnclosureScreen)
        EnclosureWorldRenderer.register()
        ClientConfigurationConnectionEvents.START.register { _, _ ->
            clientSession = ClientSession()
            uuid2name = hashMapOf()
            if (ClientConfigurationNetworking.canSend(EnclosureInstalledC2SPacket.ID))
                ClientConfigurationNetworking.send(EnclosureInstalledC2SPacket(MOD_VERSION))
        }
        ClientPlayConnectionEvents.DISCONNECT.register(ClientPlayConnectionEvents.Disconnect { handler: ClientPlayNetworkHandler?, client: MinecraftClient? ->
            isEnclosureInstalled = false
            clientSession = null
        })
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            if (client.player != null) {
                if (openScreenKey.wasPressed()) {
                    ClientPlayNetworking.send(
                        RequestOpenScreenC2SPPacket(
                            "",
                            client.player!!.world.registryKey.value,
                            client.player!!.blockPos
                        )
                    )
                }
            }
        })
    }

    companion object {
        val openScreenKey: KeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "enclosure.key.open_screen",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "enclosure.key._category"
            )
        )
        var clientSession: ClientSession? = null
        var isEnclosureInstalled: Boolean = false

        @JvmField
        var uuid2name: Map<UUID, String> = HashMap()
    }
}
