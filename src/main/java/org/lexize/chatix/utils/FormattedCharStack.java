package org.lexize.chatix.utils;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.*;

public class FormattedCharStack {
    private final Stack<StyleCharPair> buffer = new Stack<>();
    public FormattedCharStack() {

    }
    public FormattedCharStack(FormattedCharSequence source) {
        source.accept((ind, style, codePoint) -> {
            char[] chars = Character.toChars(codePoint);
            for (char c :
                    chars) {
                push(style, c);
            }
            return true;
        });
    }
    public void push(Style s, Character c) {
        buffer.push(new StyleCharPair(s,c));
    }
    public void push(StyleCharPair pair) {
        buffer.push(pair);
    }
    public String getString() {
        return getString(0, buffer.size());
    }
    public String getString(int start) {
        return getString(start, Math.max(0, buffer.size() - start));
    }
    public String getString(int start, int length) {
        StringBuilder sb = new StringBuilder();
        IndexedIterator<StyleCharPair> iterator = new IndexedIterator<>(buffer, start, Math.min(length, buffer.size() - start));
        while (iterator.hasNext()) sb.append(iterator.next().character);
        return sb.toString();
    }
    public List<StyleCharPair> getSlice() {
        return buffer;
    }
    public List<StyleCharPair> getSlice(int start) {
        return getSlice(start, Math.max(0, buffer.size() - start));
    }
    public List<StyleCharPair> getSlice(int start, int length) {
        List<StyleCharPair> list = new ArrayList<>();
        IndexedIterator<StyleCharPair> iterator = new IndexedIterator<>(buffer, start, Math.min(length, Math.max(buffer.size() - start,0)));
        while (iterator.hasNext()) list.add(iterator.next());
        return list;
    }
    public List<Style> getStyle() {
        return getStyle(0, buffer.size());
    }
    public List<Style> getStyle(int start) {
        return getStyle(start, Math.max(0, buffer.size() - start));
    }
    public List<Style> getStyle(int start, int length) {
        List<Style> list = new ArrayList<>();
        IndexedIterator<StyleCharPair> iterator = new IndexedIterator<>(buffer, start, Math.min(length, Math.max(buffer.size() - start,0)));
        while (iterator.hasNext()) list.add(iterator.next().style);
        return list;
    }
    public StyleCharPair pop() {
        return buffer.pop();
    }
    public StyleCharPair peek() {
        return buffer.peek();
    }
    public StyleCharPair remove(int index) {
        return buffer.remove(index);
    }
    public int getLength() {
        return buffer.size();
    }
    public void clear() {
        buffer.clear();
    }
    public record StyleCharPair(Style style, Character character) {}
    static class IndexedIterator<T> implements Iterator<T> {
        private final int startIndex;
        private final int length;
        private final List<T> sourceList;
        private int indexOffset;

        IndexedIterator(List<T> sourceList, int startIndex, int length) {
            this.startIndex = startIndex;
            this.length = length;
            this.sourceList = sourceList;
        }

        @Override
        public boolean hasNext() {
            return indexOffset < length;
        }

        @Override
        public T next() {
            var elem = sourceList.get(startIndex+indexOffset);
            indexOffset++;
            return elem;
        }
    }
}
