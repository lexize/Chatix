package org.lexize.chatix;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

public enum ChatixDeserializer {
    JSON(Component.Serializer::fromJson),
    MINIMESSAGE(input -> JSON.deserialize(GsonComponentSerializer.gson().serialize(MiniMessage.miniMessage().deserialize(input))));
    private final Function<String, Component> deserializer;
    ChatixDeserializer(Function<String, Component> deserializer) {
        this.deserializer = deserializer;
    }
    public Component deserialize(String string) {
        return deserializer.apply(string);
    }
}
