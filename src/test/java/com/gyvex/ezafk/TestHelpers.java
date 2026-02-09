package com.gyvex.ezafk;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class TestHelpers {

    private TestHelpers() {}

    public static ServerMock startServer() {
        // Prevent optional integrations from attempting registration during tests
        com.gyvex.ezafk.manager.IntegrationManager.setRegistrationAllowed("economy", false);
        com.gyvex.ezafk.manager.IntegrationManager.setRegistrationAllowed("metrics", false);
        com.gyvex.ezafk.manager.IntegrationManager.setRegistrationAllowed("tab", false);
        com.gyvex.ezafk.manager.IntegrationManager.setRegistrationAllowed("spigot", false);
        com.gyvex.ezafk.manager.IntegrationManager.setRegistrationAllowed("placeholderapi", false);
        com.gyvex.ezafk.manager.IntegrationManager.setRegistrationAllowed("worldguard", false);
        com.gyvex.ezafk.manager.IntegrationManager.setRegistrationAllowed("worldedit", false);
        return MockBukkit.mock();
    }

    public static JavaPlugin loadPlugin() {
        return MockBukkit.load(EzAfk.class);
    }

    public static JavaPlugin loadPlugin(Class<? extends JavaPlugin> pluginClass) {
        return MockBukkit.load(pluginClass);
    }


    public static boolean dispatchCommand(ServerMock server, CommandSender sender, String command) {
        return server.dispatchCommand(sender, command);
    }

    public static void stopServer() {
        MockBukkit.unmock();
    }
}
