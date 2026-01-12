package com.gyvex.ezafk.integration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

class TabApiPlayerListNameAdapter implements PlayerListNameAdapter {
    private enum ApplyValueType {
        STRING,
        KYORI_COMPONENT,
        BUNGEE_COMPONENT_ARRAY
    }

    private static final class TargetMethods {
        private final Method accessor;
        private final Method apply;
        private final Method reset;
        private final ApplyValueType valueType;

        private TargetMethods(Method accessor, Method apply, Method reset, ApplyValueType valueType) {
            this.accessor = accessor;
            this.apply = apply;
            this.reset = reset;
            this.valueType = valueType;
        }
    }

    private final TabAPI tabAPI;
    private final Set<UUID> managedPlayers = new HashSet<>();
    private final TargetMethods targetMethods;
    private final Logger logger = Bukkit.getLogger();

    TabApiPlayerListNameAdapter() {
        TabAPI api = TabAPI.getInstance();
        if (api == null) {
            throw new IllegalStateException("TAB API returned null instance");
        }
        this.tabAPI = api;
        this.targetMethods = resolveTargetMethods();
    }

    @Override
    public void removeInvalidEntries() {
        Iterator<UUID> iterator = managedPlayers.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Player player = Bukkit.getPlayer(uuid);
            TabPlayer tabPlayer = tabAPI.getPlayer(uuid);
            if (player == null || tabPlayer == null) {
                iterator.remove();
            }
        }
    }

    @Override
    public String getBaseName(Player player) {
        TabPlayer tabPlayer = tabAPI.getPlayer(player.getUniqueId());
        if (tabPlayer == null) {
            return null;
        }

        String currentListName = player.getPlayerListName();
        if (currentListName == null || currentListName.isEmpty()) {
            currentListName = player.getName();
        }
        return currentListName;
    }

    @Override
    public boolean apply(Player player, String targetName) {
        TabPlayer tabPlayer = tabAPI.getPlayer(player.getUniqueId());
        if (tabPlayer == null || targetMethods.apply == null) {
            return false;
        }

        try {
            invokeApplyMethod(tabPlayer, targetName);
            managedPlayers.add(player.getUniqueId());
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException | IllegalStateException ex) {
            logger.log(Level.FINE, "Failed to set temporary TAB player list name", ex);
            return false;
        }
    }

    @Override
    public boolean restore(UUID uuid) {
        if (!managedPlayers.remove(uuid)) {
            return false;
        }

        TabPlayer tabPlayer = tabAPI.getPlayer(uuid);
        if (tabPlayer != null && targetMethods.reset != null) {
            try {
                Object invocationTarget = resolveInvocationTarget(tabPlayer);
                if (invocationTarget != null) {
                    targetMethods.reset.invoke(invocationTarget);
                }
            } catch (ReflectiveOperationException | IllegalArgumentException | IllegalStateException ex) {
                logger.log(Level.FINE, "Failed to reset temporary TAB player list name", ex);
            }
        }
        return true;
    }

    @Override
    public void restoreAll() {
        Set<UUID> trackedPlayers = new HashSet<>(managedPlayers);
        for (UUID uuid : trackedPlayers) {
            restore(uuid);
        }
        managedPlayers.clear();
}

    private TargetMethods resolveTargetMethods() {
        Method applyMethod = resolveApplyMethod(TabPlayer.class);
        Method resetMethod = resolveResetMethod(TabPlayer.class);
        if (applyMethod != null) {
            return new TargetMethods(null, applyMethod, resetMethod, determineApplyValueType(applyMethod));
        }

        Method accessor = resolveTabListAccessor();
        if (accessor != null) {
            Class<?> targetClass = accessor.getReturnType();
            Method targetApply = resolveApplyMethod(targetClass);
            Method targetReset = resolveResetMethod(targetClass);
            if (targetApply != null) {
                return new TargetMethods(accessor, targetApply, targetReset, determineApplyValueType(targetApply));
            }
        }

        return new TargetMethods(null, null, null, null);
    }

    private Method resolveApplyMethod(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (!isPotentialApplyMethod(method)) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                continue;
            }

            Class<?> parameterType = parameterTypes[0];
            if (parameterType == String.class
                    || isKyoriComponent(parameterType)
                    || isBungeeComponentArray(parameterType)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private boolean isPotentialApplyMethod(Method method) {
        // Looks for methods that likely apply a name value
        String name = method.getName().toLowerCase(Locale.ROOT);
        return name.contains("name") && (name.contains("set") || name.contains("apply"));
    }

    private Method resolveResetMethod(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            if (name.contains("reset") && name.contains("name")) {
                if (method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        return null;
    }

    private Method resolveTabListAccessor() {
        Class<TabPlayer> clazz = TabPlayer.class;
        for (Method method : clazz.getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }

            Class<?> returnType = method.getReturnType();
            if (returnType == void.class) {
                continue;
            }

            String name = method.getName().toLowerCase(Locale.ROOT);
            if (name.contains("tab") && name.contains("list")) {
                method.setAccessible(true);
                return method;
            }

            if (returnType.getSimpleName().toLowerCase(Locale.ROOT).contains("tablist")) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private ApplyValueType determineApplyValueType(Method method) {
        if (method == null) {
            return null;
        }
        Class<?> parameterType = method.getParameterTypes()[0];
        if (parameterType == String.class) {
            return ApplyValueType.STRING;
        }
        if (isKyoriComponent(parameterType)) {
            return ApplyValueType.KYORI_COMPONENT;
        }
        if (isBungeeComponentArray(parameterType)) {
            return ApplyValueType.BUNGEE_COMPONENT_ARRAY;
        }
        return null;
    }

    private boolean isKyoriComponent(Class<?> type) {
        return KyoriComponentBridge.isComponentType(type);
    }

    private boolean isBungeeComponentArray(Class<?> type) {
        return type.isArray() && BaseComponent.class.isAssignableFrom(type.getComponentType());
    }

    private void invokeApplyMethod(TabPlayer tabPlayer, String targetName)
            throws InvocationTargetException, IllegalAccessException {
        if (targetMethods.apply == null || targetMethods.valueType == null) {
            throw new IllegalStateException("TAB temporary name method unavailable");
        }

        Object invocationTarget = resolveInvocationTarget(tabPlayer);
        if (invocationTarget == null) {
            throw new IllegalStateException("TAB temporary name target unavailable");
        }

        Object argument;
        switch (targetMethods.valueType) {
            case STRING -> argument = targetName;
            case KYORI_COMPONENT -> argument = KyoriComponentBridge.deserialize(targetName);
            case BUNGEE_COMPONENT_ARRAY -> argument = TextComponent.fromLegacyText(targetName);
            default -> throw new IllegalStateException("Unsupported TAB temporary name argument type");
        }

        targetMethods.apply.invoke(invocationTarget, argument);
    }

    private Object resolveInvocationTarget(TabPlayer tabPlayer)
            throws InvocationTargetException, IllegalAccessException {
        if (targetMethods.accessor == null) {
            return tabPlayer;
        }
        Object tabList = targetMethods.accessor.invoke(tabPlayer);
        if (tabList == null) {
            throw new IllegalStateException("TAB tab list accessor returned null");
        }
        return tabList;
    }

    private static final class KyoriComponentBridge {
        private static final Class<?> COMPONENT_CLASS = loadClass("net.kyori.adventure.text.Component");
        private static final Class<?> COMPONENT_LIKE_CLASS = loadClass("net.kyori.adventure.text.ComponentLike");
        private static final LegacySerializer LEGACY_SERIALIZER = resolveLegacySerializer();

        private static Class<?> loadClass(String className) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ex) {
                return null;
            }
        }

        private static boolean isComponentType(Class<?> type) {
            if (type == null) {
                return false;
            }
            if (COMPONENT_CLASS != null && COMPONENT_CLASS.isAssignableFrom(type)) {
                return true;
            }
            if (COMPONENT_LIKE_CLASS != null && COMPONENT_LIKE_CLASS.isAssignableFrom(type)) {
                return true;
            }
            return COMPONENT_CLASS != null && type.isAssignableFrom(COMPONENT_CLASS);
        }

        private static Object deserialize(String value) {
            if (LEGACY_SERIALIZER == null) {
                throw new IllegalStateException("Kyori legacy serializer unavailable");
            }
            return LEGACY_SERIALIZER.deserialize(value);
        }

        private static LegacySerializer resolveLegacySerializer() {
            Class<?> serializerClass = loadClass("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
            if (serializerClass == null) {
                return null;
            }
            try {
                Object instance = serializerClass.getMethod("legacySection").invoke(null);
                Method deserialize = serializerClass.getMethod("deserialize", String.class);
                return new LegacySerializer(instance, deserialize);
            } catch (ReflectiveOperationException | RuntimeException ex) {
                return null;
            }
        }

        private record LegacySerializer(Object instance, Method deserializeMethod) {
            private Object deserialize(String value) {
                try {
                    return deserializeMethod.invoke(instance, value);
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    throw new IllegalStateException("Failed to deserialize legacy component", ex);
                }
            }
        }
    }
}
