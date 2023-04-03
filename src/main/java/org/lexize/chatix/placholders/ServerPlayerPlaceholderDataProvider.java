package org.lexize.chatix.placholders;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ServerPlayerPlaceholderDataProvider implements PlaceholderDataProvider<Component> {
    private final ServerPlayer player;
    private final String message;
    public ServerPlayerPlaceholderDataProvider(ServerPlayer player, String message) {
        this.player = player;
        this.message = message;
    }

    @Override
    public Component get(String placeholder) {
        return switch (placeholder) {
            case "formatted_player_name" -> {
                var team = player.getTeam();
                net.minecraft.network.chat.Component c;
                if (team == null) c = player.getName();
                else c = team.getFormattedName(player.getName());
                yield c;
            }
            case "player_name" -> player.getName();
            case "message" -> Component.literal(message);
            default -> Component.empty();
        };
    }

    @Override
    public boolean has(String placeholder) {
        return switch (placeholder) {
            case "formatted_player_name", "player_name", "message" -> true;
            default -> false;
        };
    }
}
