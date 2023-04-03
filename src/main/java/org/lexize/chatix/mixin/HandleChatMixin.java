package org.lexize.chatix.mixin;

import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.lexize.chatix.Chatix;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class HandleChatMixin {
    @Shadow public ServerPlayer player;

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "handleChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;tryHandleChat(Ljava/lang/String;Ljava/time/Instant;Lnet/minecraft/network/chat/LastSeenMessages$Update;)Ljava/util/Optional;"), cancellable = true)
    private void onChatMessage(ServerboundChatPacket packet, CallbackInfo ci) {
        if (Chatix.handleMessage(server, player, packet)) {
            ci.cancel();
        }
    }
}
