package org.lexize.chatix;

import com.mojang.brigadier.tree.CommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FormattedCharSequence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lexize.chatix.groups.ChatixChatGroup;
import org.lexize.chatix.placholders.PlaceholderDataProvider;
import org.lexize.chatix.placholders.ServerPlayerPlaceholderDataProvider;
import org.lexize.chatix.utils.ComponentCharSequence;
import org.lexize.chatix.utils.FormattedCharStack;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lexize.chatix.utils.LiteralArgumentBuilder.literal;

public class Chatix implements DedicatedServerModInitializer {
    private static Chatix INSTANCE;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9\\-_]+)}");
    private static final File CONFIG_FOLDER = new File("config/chatix");
    private static final File CHAT_GROUPS_FOLDER = new File("config/chatix/chat_groups");
    private static final File COMMAND_GROUPS_FOLDER = new File("config/chatix/command_groups");
    private static final String LOAD_ERROR_MISSING_OUTPUT = "Group \"%s\" from file \"%s\" wasnt loaded because of missing \"output\" field.";
    private static final String LOAD_ERROR_READING_FILE = "Error occurred while reading file \"%s\", skipping.";
    private static final Yaml YAML = new Yaml();
    private static final Logger LOGGER = LogManager.getLogger("Chatix");
    private final List<ChatixChatGroup> loadedChatGroups = new ArrayList<>();
    private final List<CommandNode<CommandSourceStack>> loadedChatCommands = new ArrayList<>();
    private MinecraftServer serverInstance;

    @Override
    public void onInitializeServer() {
        INSTANCE = this;
        reload();
        ServerLifecycleEvents.SERVER_STARTED.register(s -> serverInstance = s);
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment) -> {
            dispatcher.register(getChatixCommand());
        });
    }

    public void reload() {
        if (!CONFIG_FOLDER.exists()) CONFIG_FOLDER.mkdirs();
        loadChatGroups();
        loadedChatGroups.sort((a, b) -> b.priority - a.priority);
        String[] loadedGroupNames = new String[loadedChatGroups.size()];
        for (int i = 0; i < loadedGroupNames.length; i++) {
            loadedGroupNames[i] = loadedChatGroups.get(i).name;
        }
        LOGGER.info("Loaded chat groups: %s".formatted(String.join(", ", loadedGroupNames)));
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
                    if (c != null) {
                        var output = Utils.replacePlaceholdersInComponent(c, placeholderDataProvider);
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
    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> getChatixCommand() {
        var root = literal("chatix");
        var reloadCommand = literal("reload").executes((c) -> {
            INSTANCE.reload();
            return 0;
        }).requires(s -> Permissions.check(s, "chatix.command.reload", false));
        return root.then(reloadCommand);
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
        public static <S> boolean removeCommandChild(CommandNode<S> root, CommandNode<S> child) throws NoSuchFieldException, IllegalAccessException {
            Class<CommandNode> commandNodeClass = CommandNode.class;
            Field childrenField = commandNodeClass.getDeclaredField("children");
            childrenField.setAccessible(true);
            Field argumentsField = commandNodeClass.getDeclaredField("arguments");
            argumentsField.setAccessible(true);
            Field literalsField = commandNodeClass.getDeclaredField("literals");
            literalsField.setAccessible(true);
            Map<String, CommandNode<S>> children = (Map<String, CommandNode<S>>) childrenField.get(root);
            Map<String, CommandNode<S>> arguments = (Map<String, CommandNode<S>>) argumentsField.get(root);
            Map<String, CommandNode<S>> literals = (Map<String, CommandNode<S>>) literalsField.get(root);
            if (children.containsValue(child)) {
                children.remove(child.getName());
                arguments.remove(child.getName());
                literals.remove(child.getName());
                return true;
            }
            return false;
        }

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

        public static MutableComponent replacePlaceholdersInComponentTest(Component component, PlaceholderDataProvider<Component> placeholderDataProvider) {
            var sequence = charSequenceFromComponent(component);
            FormattedCharStack mainCharBuffer = new FormattedCharStack();
            FormattedCharStack placeholderCharBuffer = new FormattedCharStack();
            StringBuilder placeholderNameBuffer = new StringBuilder();
            AtomicBoolean readingPlaceholder = new AtomicBoolean(false);
            AtomicBoolean nextEscaped = new AtomicBoolean(false);
            AtomicBoolean prevEscaped = new AtomicBoolean(false);
            sequence.accept((index, style, codePoint) -> {
                char[] chars = Character.toChars(codePoint);
                for (char c :
                        chars) {
                    if (!readingPlaceholder.get()) {
                        if (nextEscaped.get()) {
                            nextEscaped.set(false);
                            mainCharBuffer.push(style, c);
                            prevEscaped.set(true);
                        }
                        else {
                            switch (c) {
                                case '\\' -> nextEscaped.set(true);
                                case '{' -> {
                                    if (mainCharBuffer.peek().character() == '$' && !prevEscaped.get()) {
                                        readingPlaceholder.set(true);
                                        placeholderCharBuffer.push(mainCharBuffer.peek());
                                        placeholderCharBuffer.push(style, c);
                                        mainCharBuffer.pop();
                                    }
                                    else mainCharBuffer.push(style, c);
                                }
                                default -> mainCharBuffer.push(style, c);
                            }
                            prevEscaped.set(false);
                        }
                    }
                    else {
                        boolean readInterrupted = false;
                        boolean readStopped = false;
                        if (c == '}') {
                            placeholderCharBuffer.push(style, c);
                            Component componentToInsert = null;
                            if (placeholderNameBuffer.length() > 0) {
                                componentToInsert = placeholderDataProvider.get(placeholderNameBuffer.toString());
                            }
                            if (componentToInsert != null) {
                                ComponentCharSequence ccs = new ComponentCharSequence(componentToInsert);
                                FormattedCharStack tempStack = new FormattedCharStack(ccs);
                                float placeholderLen = placeholderCharBuffer.getLength();
                                float componentLen = tempStack.getLength();
                                List<Style> placeholderStyle = placeholderCharBuffer.getStyle();
                                List<FormattedCharStack.StyleCharPair> tempSlice = tempStack.getSlice();
                                for (int i = 0; i < tempSlice.size(); i++) {
                                    FormattedCharStack.StyleCharPair scp = tempSlice.get(i);
                                    Style s = lerpStyle(placeholderStyle, (i / componentLen)*placeholderLen);
                                    mainCharBuffer.push(scp.style().applyTo(s), scp.character());
                                }
                                readStopped = true;
                            }
                            else {
                                readInterrupted = true;
                            }
                        }
                        else if ((c >= 'a' && c <= 'z') ||
                                (c >= 'A' && c <= 'Z') ||
                                (c >= '0' && c <= '9') ||
                                c == '-' || c == '_') {
                            placeholderCharBuffer.push(style, c);
                            placeholderNameBuffer.append(c);
                        }
                        else {
                            placeholderCharBuffer.push(style, c);
                            readInterrupted = true;
                        }
                        if (readInterrupted) {
                            for (FormattedCharStack.StyleCharPair scp :
                                    placeholderCharBuffer.getSlice()) {
                                mainCharBuffer.push(scp);
                            }
                        }
                        if (readStopped || readInterrupted) {
                            placeholderNameBuffer.setLength(0);
                            placeholderCharBuffer.clear();
                            readingPlaceholder.set(false);
                        }
                    }
                }
                return true;
            });
            MutableComponent parentComponent = null;
            StringBuilder buffer = new StringBuilder();
            AtomicReference<Style> prevStyle = new AtomicReference<>(Style.EMPTY);
            List<FormattedCharStack.StyleCharPair> finalSlice = mainCharBuffer.getSlice();
            for (FormattedCharStack.StyleCharPair scp :
                    finalSlice) {
                if (!prevStyle.get().equals(scp.style())) {
                    if (buffer.length() > 0) {
                        MutableComponent c = Component.literal(buffer.toString()).withStyle(scp.style());
                        buffer.setLength(0);
                        if (parentComponent == null) parentComponent = c;
                        else parentComponent = parentComponent.append(c);
                    }
                    prevStyle.set(scp.style());
                }
                buffer.append(scp.character());
            }
            if (buffer.length() > 0) {
                MutableComponent c = Component.literal(buffer.toString()).withStyle(prevStyle.get());
                buffer.setLength(0);
                if (parentComponent == null) parentComponent = c;
                else parentComponent = parentComponent.append(c);
            }
            return parentComponent != null ? parentComponent : Component.empty();
        }

        public static FormattedCharSequence charSequenceFromComponent(Component component) {
            return new ComponentCharSequence(component);
        }
        public static Style lerpStyle(List<Style> styleList, float progress) {
            int minIndex = Math.max(Math.min(styleList.size()-1, (int) Math.floor(progress)), 0);
            int maxIndex = Math.max(Math.min(styleList.size()-1, (int) Math.ceil(progress)), 0);
            float w = progress % 1;
            Style a = styleList.get(minIndex);
            Style b = styleList.get(maxIndex);
            return Style.EMPTY
                    .withClickEvent(w < 0.5 ? a.getClickEvent() : b.getClickEvent())
                    .withFont(w < 0.5 ? a.getFont() : b.getFont())
                    .withHoverEvent(w < 0.5 ? a.getHoverEvent() : b.getHoverEvent())
                    .withInsertion(w < 0.5 ? a.getInsertion() : b.getInsertion())
                    .withBold(w < 0.5 ? a.isBold() : b.isBold())
                    .withItalic(w < 0.5 ? a.isItalic() : b.isItalic())
                    .withStrikethrough(w < 0.5 ? a.isStrikethrough() : b.isStrikethrough())
                    .withUnderlined(w < 0.5 ? a.isUnderlined() : b.isUnderlined())
                    .withObfuscated(w < 0.5 ? a.isObfuscated() : b.isObfuscated())
                    .withColor(lerpTextColor(a.getColor(), b.getColor(), w));
        }
        private static TextColor lerpTextColor(TextColor a, TextColor b, float w) {
            if (a == null && b == null) return null;
            if (a == null) return w < 0.5 ? null : b;
            if (b == null) return w < 0.5 ? a : null;
            int ab = a.getValue() & 0xFF;
            int ag = (a.getValue() >> 8) & 0xFF;
            int ar = (a.getValue() >> 16) & 0xFF;

            int bb = b.getValue() & 0xFF;
            int bg = (b.getValue() >> 8) & 0xFF;
            int br = (b.getValue() >> 16) & 0xFF;

            int fr = (ar + (int)(w * (br-ar)));
            int fg = (ar + (int)(w * (bg-ag)));
            int fb = (ar + (int)(w * (bb-ab)));

            return TextColor.fromRgb(fb + (fg << 8) + (fr << 16));
        }
    }
}
