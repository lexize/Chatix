package org.lexize.chatix.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import java.util.Optional;

public class ComponentCharSequence implements FormattedCharSequence {
    private final Component component;

    public ComponentCharSequence(Component component) {
        this.component = component;
    }

    @Override
    public boolean accept(FormattedCharSink formattedCharSink) {
        int[] i = new int[1];
        boolean[] interrupted = new boolean[1];
        component.visit((style, string) -> {
            var iterator = string.chars().iterator();
            while (iterator.hasNext() && !interrupted[0]) {
                if (!formattedCharSink.accept(i[0], style, iterator.next())) {
                    interrupted[0] = true;
                    return Optional.empty();
                }
                i[0]++;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return !interrupted[0];
    }
}
