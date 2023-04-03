package org.lexize.chatix;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lexize.chatix.placholders.PlaceholderDataProvider;
import org.lexize.chatix.placholders.ServerPlayerPlaceholderDataProvider;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chatix implements DedicatedServerModInitializer {
    private static Chatix INSTANCE;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9\\-_]+)}");
    private static final File CONFIG_FOLDER = new File("config/chatix");
    private static final File CHAT_GROUPS_FOLDER = new File("config/chatix/chat_groups");
    private static final String LOAD_ERROR_MISSING_OUTPUT = "Group \"%s\" from file \"%s\" wasnt loaded because of missing \"output\" field.";
    private static final String LOAD_ERROR_READING_FILE = "Error occurred while reading file \"%s\", skipping.";
    private static final Yaml YAML = new Yaml();
    private final List<ChatixChatGroup> loadedChatGroups = new ArrayList<>();
    private static final Logger LOGGER = LogManager.getLogger("Chatix");

    @Override
    public void onInitializeServer() {
        INSTANCE = this;
        if (!CONFIG_FOLDER.exists()) CONFIG_FOLDER.mkdirs();
        reload();
    }

    public void reload() {
        loadChatGroups();
        loadedChatGroups.sort((a, b) -> b.priority - a.priority);
        String[] loadedGroupNames = new String[loadedChatGroups.size()];
        for (int i = 0; i < loadedGroupNames.length; i++) {
            loadedGroupNames[i] = loadedChatGroups.get(i).name;
        }
        LOGGER.info("Loaded chat groups: %s".formatted(String.join(" ,", loadedGroupNames)));
    }

    public static Chatix getInstance() {
        return INSTANCE;
    }

    public static boolean handleMessage(MinecraftServer server, ServerPlayer sender, ServerboundChatPacket packet) {
        var groups = INSTANCE.loadedChatGroups;
        String source = packet.message();
        for (ChatixChatGroup group :
                groups) {
            try {
                if (!Permissions.check(sender.getGameProfile(), "chatix.group.%s.send".formatted(group.name), group.defaultPermission).get()) continue;
                if (source.startsWith(group.prefix)) {
                    String message = source.substring(group.prefix.length());
                    ServerPlayerPlaceholderDataProvider placeholderDataProvider = new ServerPlayerPlaceholderDataProvider(sender, message);
                    var c = group.deserializer.deserialize(group.output);
                    var output = Utils.replacePlaceholdersInComponent(c != null ? c : Component.empty(), placeholderDataProvider);
                    ClientboundSystemChatPacket chatPacket = new ClientboundSystemChatPacket(output, false);
                    if (group.distance < 0) {
                        server.getPlayerList().getPlayers().forEach((pl) -> sendChatixMessage(pl, chatPacket, group));
                        server.logChatMessage(output, ChatType.bind(ChatType.CHAT, sender), null);
                    }

                    else if (group.distance >= 0) {
                        var world = sender.getCommandSenderWorld();
                        List<ServerPlayer> players = new ArrayList<>();
                        for (var pl:
                                world.players()) {
                            if (pl instanceof ServerPlayer spl && (group.distance == 0 || spl.position().distanceTo(sender.position()) <= group.distance)) {
                                players.add(spl);
                            }
                        }
                        players.forEach((pl) -> sendChatixMessage(pl, chatPacket, group));
                    }
                    return true;
                }
            } catch (ExecutionException | RuntimeException | InterruptedException ignored) {}
        }
        return false;
    }
    public static void sendChatixMessage(ServerPlayer receiver, ClientboundSystemChatPacket packet, ChatixChatGroup group) {
        Permissions.check(receiver.getGameProfile(), "chatix.group.%s.receive".formatted(group.name), group.defaultPermission).thenAcceptAsync(state -> {
            receiver.connection.send(packet);
        });
    }
    private void loadChatGroups() {
        loadedChatGroups.clear();
        if(!CHAT_GROUPS_FOLDER.exists()) CHAT_GROUPS_FOLDER.mkdirs();
        File[] chatGroupFiles = CHAT_GROUPS_FOLDER.listFiles((dir, name) -> name.endsWith(".yml"));
        for (File f :
                chatGroupFiles) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
                var nodes = YAML.composeAll(new InputStreamReader(fis));
                for (var node :
                        nodes) {
                    if (node instanceof MappingNode mn) {
                        for (NodeTuple t :
                                mn.getValue()) {
                            var keyNode = t.getKeyNode();
                            var valueNode = t.getValueNode();
                            if (keyNode instanceof ScalarNode scalarNode && valueNode instanceof MappingNode groupNode) {
                                var key = scalarNode.getValue();
                                var group = ChatixChatGroup.fromNode(key, groupNode);
                                if (group.output != null) loadedChatGroups.add(group);
                                else LOGGER.warn(LOAD_ERROR_MISSING_OUTPUT.formatted(key,f.getName()));
                            }
                        }
                    }
                }
            }
            catch (Exception exception) {
                LOGGER.warn(LOAD_ERROR_READING_FILE.formatted(f.getName()));
            }
            try {
                if (fis != null) fis.close();
            } catch (IOException ignored) {}
        }
    }
    public static class Utils {
        /*public static Component getFormattedComponent(String message, PlaceholderDataProvider<Component> placeholderDataProvider) {
            try {
                StringReader reader = new StringReader(message);
                StringBuilder buffer = new StringBuilder();
                List<Component> components = new ArrayList<>();
                boolean readingPlaceholder = false;
                boolean nextEscaped = false;
                for (int i = reader.read(); i != -1; i = reader.read()) {
                    char c = (char) i;
                    if (nextEscaped) {
                        buffer.append(c);
                        nextEscaped = false;
                        continue;
                    }
                    if (!readingPlaceholder) {
                        switch (c) {
                            case '$' -> {
                                char sc = (char) reader.read();
                                if (sc == '{') {
                                    readingPlaceholder = true;
                                    if (buffer.length() > 0) {
                                        components.add(Component.literal(buffer.toString()));
                                        buffer.setLength(0);
                                    }
                                }
                                else {
                                    buffer.append(c);
                                    buffer.append(sc);
                                }
                            }
                            case '\\' -> nextEscaped = true;
                            default -> buffer.append(c);
                        }
                    }
                    else {
                        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
                            buffer.append(c);
                        }
                        else {
                            if (c == '}') {
                                components.add(placeholderDataProvider.get(buffer.toString()));
                                buffer.setLength(0);
                            } else {
                                String src = buffer.toString();
                                buffer.setLength(0);
                                buffer.append("${").append(src);
                            }
                            readingPlaceholder = false;
                        }
                    }
                }
                reader.close();
                if (buffer.length() > 0) {
                    components.add(Component.literal(buffer.toString()));
                }
                MutableComponent parentComponent = Component.empty();
                components.forEach(parentComponent::append);
                return parentComponent;
            }
            catch (IOException e) {
                return Component.empty();
            }
        }*/

        public static MutableComponent replacePlaceholdersInComponent(Component component, PlaceholderDataProvider<Component> placeholderDataProvider) {
            MutableComponent parentComponent = Component.empty();
            component.visit((style, string) -> {
                Matcher m = PLACEHOLDER_PATTERN.matcher(string);
                int startIndex = 0;
                while (m.find()) {
                    if (!placeholderDataProvider.has(m.group(1))) continue;
                    if ((m.start() - startIndex) > 0) {
                        parentComponent.append(Component.literal(string.substring(startIndex, m.start())).withStyle(style));
                    }
                    startIndex = m.end();
                    Component c = placeholderDataProvider.get(m.group(1)).copy();
                    c = c.copy().withStyle(c.getStyle().applyTo(style));
                    parentComponent.append(c);
                }
                if (startIndex < string.length()) parentComponent.append(Component.literal(string.substring(startIndex)).withStyle(style));
                return Optional.empty();
            }, Style.EMPTY);
            return parentComponent;
        }
    }
}
