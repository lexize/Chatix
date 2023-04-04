package org.lexize.chatix.utils;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;

public class RequiredArgumentBuilder<T> extends ArgumentBuilder<CommandSourceStack, RequiredArgumentBuilder<T>> {
    private final String name;
    private final ArgumentType<T> type;
    private SuggestionProvider<CommandSourceStack> suggestionsProvider = null;

    protected RequiredArgumentBuilder(final String name, final ArgumentType<T> type) {
        this.name = name;
        this.type = type;
    }

    public static <T> RequiredArgumentBuilder<T> argument(final String name, final ArgumentType<T> type) {
        return new RequiredArgumentBuilder<>(name, type);
    }

    public RequiredArgumentBuilder<T> suggests(final SuggestionProvider<CommandSourceStack> provider) {
        this.suggestionsProvider = provider;
        return getThis();
    }
    public SuggestionProvider<CommandSourceStack> getSuggestionsProvider() {
        return suggestionsProvider;
    }

    public ArgumentType<T> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    protected RequiredArgumentBuilder<T> getThis() {
        return this;
    }

    @Override
    public CommandNode<CommandSourceStack> build() {
        return null;
    }
}
