package org.lexize.chatix.groups;

import org.lexize.chatix.ChatixDeserializer;
import org.yaml.snakeyaml.nodes.MappingNode;

public abstract class ChatixGroup {
    public String name;

    public float distance = -1;

    public String output;

    public boolean defaultPermission = true;

    public ChatixDeserializer deserializer = ChatixDeserializer.JSON;
}
