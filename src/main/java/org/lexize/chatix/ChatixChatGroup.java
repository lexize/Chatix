package org.lexize.chatix;


import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

public class ChatixChatGroup {
    public String name;
    public float distance = -1;
    public String prefix = "";
    public String output;
    public int priority = 0;
    public boolean defaultPermission = true;
    public ChatixDeserializer deserializer = ChatixDeserializer.JSON;
    public ChatixChatGroup() {}
    public static ChatixChatGroup fromNode(String groupName, MappingNode node) {
        ChatixChatGroup group = new ChatixChatGroup();
        for (NodeTuple tuple :
                node.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode keyNode && tuple.getValueNode() instanceof ScalarNode valueNode) {
                String key = keyNode.getValue();
                String value = valueNode.getValue();
                switch (key) {
                    case "distance" -> group.distance = Float.parseFloat(value);
                    case "priority" -> group.priority = Integer.parseInt(value);
                    case "prefix" -> group.prefix = value;
                    case "output" -> group.output = value;
                    case "defaultPermission" -> group.defaultPermission = Boolean.parseBoolean(value);
                    case "deserializer" -> group.deserializer = ChatixDeserializer.valueOf(value.toUpperCase());
                }
            }
        }
        group.name = groupName;
        return group;
    }
}
