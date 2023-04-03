package org.lexize.chatix.placholders;

public interface PlaceholderDataProvider<T> {
    T get(String placeholder);
    boolean has(String placeholder);
}
