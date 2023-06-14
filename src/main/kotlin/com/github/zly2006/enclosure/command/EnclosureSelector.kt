package com.github.zly2006.enclosure.command

import com.google.gson.JsonObject
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.serialize.ArgumentSerializer
import net.minecraft.command.argument.serialize.ArgumentSerializer.ArgumentTypeProperties
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import java.util.*
import java.util.concurrent.CompletableFuture

class EnclosureSelector(
    var owner: UUID? = null,
    var name: String,
    var parent: String,
    var createdBefore: Long,
    var createdAfter: Long,
) {

}

class EnclosureArgumentType: ArgumentType<EnclosureSelector> {
    override fun parse(reader: StringReader?): EnclosureSelector {
        TODO("Not yet implemented")
    }

    override fun <S> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        return super.listSuggestions(context, builder)
    }


}

class EnclosureArgumentSerializer: ArgumentSerializer<EnclosureArgumentType, EnclosureArgumentSerializer.Properties> {
    class Properties: ArgumentTypeProperties<EnclosureArgumentType> {
        override fun createType(commandRegistryAccess: CommandRegistryAccess?): EnclosureArgumentType {
            TODO("Not yet implemented")
        }

        override fun getSerializer(): ArgumentSerializer<EnclosureArgumentType, *> {
            TODO("Not yet implemented")
        }
    }

    override fun writePacket(properties: Properties?, buf: PacketByteBuf?) {
        TODO("Not yet implemented")
    }

    override fun fromPacket(buf: PacketByteBuf?): Properties {
        TODO("Not yet implemented")
    }

    override fun getArgumentTypeProperties(argumentType: EnclosureArgumentType?): Properties {
        TODO("Not yet implemented")
    }

    override fun writeJson(properties: Properties?, json: JsonObject?) {
        TODO("Not yet implemented")
    }
}

fun registerArgType(commandRegistryAccess: CommandRegistryAccess) {
    Registry.register(
        Registries.COMMAND_ARGUMENT_TYPE,
        "enclosure:enclosure",
     //   EnclosureArgumentType::class.java,
        EnclosureArgumentSerializer()
    )
}
