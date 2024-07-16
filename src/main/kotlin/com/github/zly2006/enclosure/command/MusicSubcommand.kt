package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.utils.TrT
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.command.suggestion.SuggestionProviders
import net.minecraft.registry.Registries
import net.minecraft.server.command.CommandManager

fun BuilderScope<*>.registerMusic() {
    literal("music") {
        literal("set") {
            permission("enclosure.command.music.set")
            argument(
                CommandManager.argument("music", IdentifierArgumentType.identifier())
                    .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
            ) {
                optionalEnclosure { area ->
                    val music = IdentifierArgumentType.getIdentifier(this, "music")
                    if (!music.path.startsWith("music.") && !music.path.startsWith("music_disc.")) {
                        error(TrT.of("Invalid music."), this)
                    }
                    val soundEvent = Registries.SOUND_EVENT.get(music)
                        ?: error(TrT.of("Invalid music."), this)
                    area.music = music
                    source.sendMessage(TrT.of("Set music to $music in ${area.fullName}."))
                }
            }
        }
    }
}
