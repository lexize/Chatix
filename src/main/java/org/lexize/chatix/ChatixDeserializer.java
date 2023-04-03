package org.lexize.chatix;

import net.minecraft.network.chat.Component;

import java.util.function.Function;

public enum ChatixDeserializer {
    JSON(Component.Serializer::fromJson);
    private final Function<String, Component> deserializer;
    ChatixDeserializer(Function<String, Component> deserializer) {
        this.deserializer = deserializer;
    }
    public Component deserialize(String string) {
        return deserializer.apply(string);
    }
}
