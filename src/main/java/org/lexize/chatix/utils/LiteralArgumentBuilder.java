package org.lexize.chatix.utils;

import net.minecraft.commands.CommandSourceStack;

public class LiteralArgumentBuilder extends com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> {
    protected LiteralArgumentBuilder(String literal) {
        super(literal);
    }

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return new LiteralArgumentBuilder(name);
    }
}
