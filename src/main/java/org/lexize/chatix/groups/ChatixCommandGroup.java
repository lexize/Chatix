package org.lexize.chatix.groups;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import org.lexize.chatix.utils.RequiredArgumentBuilder;

import java.util.HashMap;
// TODO: LUA integration
public class ChatixCommandGroup extends ChatixGroup {
    public abstract static class CommandArgument<S, T extends ArgumentBuilder<S,T>> {
        public HashMap<String, CommandArgument<S, ?>> arguments = new HashMap<>();
        public abstract void setParameter(String key, String value);
        public abstract ArgumentBuilder<S, T> getCommandNode(String name);
    }
    public static class CommandBooleanArgument extends CommandArgument<CommandSourceStack, RequiredArgumentBuilder<Boolean>> {

        @Override
        public void setParameter(String key, String value) {

        }

        @Override
        public ArgumentBuilder<CommandSourceStack, RequiredArgumentBuilder<Boolean>> getCommandNode(String name) {
            return RequiredArgumentBuilder.argument(name, BoolArgumentType.bool());
        }
    }
    public static class CommandIntegerArgument extends CommandArgument<CommandSourceStack, RequiredArgumentBuilder<Integer>> {
        public int minValue = Integer.MIN_VALUE;
        public int maxValue = Integer.MAX_VALUE;

        @Override
        public void setParameter(String key, String value) {
            switch (key) {
                case "min_value" -> minValue = Integer.parseInt(value);
                case "max_value" -> maxValue = Integer.parseInt(value);
            }
        }
        @Override
        public RequiredArgumentBuilder<Integer> getCommandNode(String name) {
            return RequiredArgumentBuilder.argument(name, IntegerArgumentType.integer(minValue, maxValue));
        }
    }
    public static class CommandFloatArgument extends CommandArgument<CommandSourceStack, RequiredArgumentBuilder<Float>> {
        public float minValue = Float.MIN_VALUE;
        public float maxValue = Float.MAX_VALUE;

        @Override
        public void setParameter(String key, String value) {
            switch (key) {
                case "min_value" -> minValue = Float.parseFloat(value);
                case "max_value" -> maxValue = Float.parseFloat(value);
            }
        }
        @Override
        public RequiredArgumentBuilder<Float> getCommandNode(String name) {
            return RequiredArgumentBuilder.argument(name, FloatArgumentType.floatArg(minValue, maxValue));
        }
    }
    public static class CommandDoubleArgument extends CommandArgument<CommandSourceStack, RequiredArgumentBuilder<Double>> {
        public double minValue = Double.MIN_VALUE;
        public double maxValue = Double.MAX_VALUE;

        @Override
        public void setParameter(String key, String value) {
            switch (key) {
                case "min_value" -> minValue = Double.parseDouble(value);
                case "max_value" -> maxValue = Double.parseDouble(value);
            }
        }
        @Override
        public RequiredArgumentBuilder<Double> getCommandNode(String name) {
            return RequiredArgumentBuilder.argument(name, DoubleArgumentType.doubleArg(minValue, maxValue));
        }
    }
    public static class CommandLongArgument extends CommandArgument<CommandSourceStack, RequiredArgumentBuilder<Long>> {
        public long minValue = Long.MIN_VALUE;
        public long maxValue = Long.MAX_VALUE;

        @Override
        public void setParameter(String key, String value) {
            switch (key) {
                case "min_value" -> minValue = Long.parseLong(value);
                case "max_value" -> maxValue = Long.parseLong(value);
            }
        }
        @Override
        public RequiredArgumentBuilder<Long> getCommandNode(String name) {
            return RequiredArgumentBuilder.argument(name, LongArgumentType.longArg(minValue, maxValue));
        }
    }
}
