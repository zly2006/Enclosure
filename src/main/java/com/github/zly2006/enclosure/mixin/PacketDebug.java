package com.github.zly2006.enclosure.mixin;/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(targets = "net/minecraft/network/packet/CustomPayload$1" ,priority = 1001)
public class PacketDebug {
    @Final
    @Shadow
    Map<Identifier, PacketCodec<?, ? extends CustomPayload>> field_48658;
    @Inject(
            method = "getCodec",
            at = @At(
                    "RETURN"
            )
    )
    private void wrapConfigCodec(Identifier id, CallbackInfoReturnable<PacketCodec<?, ?>> cir, @Local PacketCodec<?, ?> packetCodec) {
        System.out.println("id " + id + " -> " + packetCodec);
        System.out.println("maps " + field_48658.keySet());
    }
}
