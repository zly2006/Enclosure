package com.github.zly2006.enclosure.utils;

import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.network.EnclosureInstalledC2SPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrT {
    public static @NotNull MutableText of(String key, @Nullable ServerPlayerEntity player, Object... arguments) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            if (ServerMain.INSTANCE.getTranslation().has(key)) {
                return new ServerSideTranslatable(key, List.of(arguments)).get(player);
            }
        }
        return new TranslatableText(key, arguments);
    }
    public static @NotNull ServerSideTranslatable sst(String key, Object... arguments) {
        return new ServerSideTranslatable(key, List.of(arguments));
    }

    public static @NotNull MutableText of(String key, Object... arguments) {
        return new TranslatableText(key, arguments);
    }



    public static @NotNull MutableText limit(Field field) {
        return of("enclosure.limit." + Utils.camelCaseToSnakeCase(field.getName()));
    }

    public static class ServerSideTranslatable {
        final List<Object> siblings = new ArrayList<>();
        final List<Object> arguments;
        final String key;
        Style style;

        public ServerSideTranslatable(String key) {
            this.key = key;
            arguments = null;
        }

        public ServerSideTranslatable(String key, List<Object> arguments) {
            this.arguments = arguments;
            this.key = key;
        }

        public Style getStyle() {
            return style;
        }

        public String asString() {
            return get(null).asString();
        }

        public List<Object> getSiblings() {
            return siblings;
        }

        @Contract(value = "_ -> new", pure = true)
        public static @NotNull ServerSideTranslatable of(String key) {
            return new ServerSideTranslatable(key);
        }

        public static @NotNull ServerSideTranslatable of(String key, Object... arguments) {
            return new ServerSideTranslatable(key, Arrays.stream(arguments).toList());
        }

        public MutableText get(@Nullable PlayerEntity player) {
            MutableText ret;
            if (key.isEmpty()) {
                ret = new LiteralText("");
            } else {
                if (player instanceof ServerPlayerEntity serverPlayerEntity && EnclosureInstalledC2SPacket.isInstalled(serverPlayerEntity)) {
                    if (arguments != null) {
                        ret = new TranslatableText(key, arguments.toArray());
                    } else {
                        ret = new TranslatableText(key);
                    }
                } else {
                    if (ServerMain.INSTANCE.getTranslation().has(key)) {
                        if (arguments != null) {
                            ret = new LiteralText(ServerMain.INSTANCE.getTranslation().get(key).getAsString().formatted(arguments.toArray()));
                        } else {
                            ret = new LiteralText(ServerMain.INSTANCE.getTranslation().get(key).getAsString());
                        }
                    } else {
                        ret = new LiteralText("§<Require Translation>§r").styled(style -> style
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(key))));
                    }
                }
            }
            for (Object sibling : siblings) {
                if (sibling instanceof MutableText) {
                    ret.append((MutableText) sibling);
                } else if (sibling instanceof ServerSideTranslatable) {
                    ret.append(((ServerSideTranslatable) sibling).get(player));
                }
            }
            return ret;
        }

        public MutableText get() {
            MutableText ret;
            if (key.isEmpty()) {
                ret = new LiteralText("");
            } else if (arguments != null) {
                ret = new TranslatableText(key, arguments.toArray());
            } else {
                ret = new TranslatableText(key);
            }
            for (Object sibling : siblings) {
                if (sibling instanceof MutableText) {
                    ret.append((MutableText) sibling);
                } else if (sibling instanceof ServerSideTranslatable sst) {
                    ret.append(sst.get());
                }
            }
            return ret;
        }

        public ServerSideTranslatable append(Text text) {
            siblings.add(text);
            return this;
        }

        public ServerSideTranslatable append(ServerSideTranslatable text) {
            siblings.add(text);
            return this;
        }

        public ServerSideTranslatable append(String text) {
            return appendString(text);
        }


        public ServerSideTranslatable appendString(Object text) {
            siblings.add(Text.of(String.valueOf(text)));
            return this;
        }
        public ServerSideTranslatable appendString(Object text, Style style) {
            siblings.add(new LiteralText(String.valueOf(text)).setStyle(style));
            return this;
        }
        public ServerSideTranslatable appendString(Object text, Formatting formatting) {
            siblings.add(new LiteralText(String.valueOf(text)).formatted(formatting));
            return this;
        }
    }
}
