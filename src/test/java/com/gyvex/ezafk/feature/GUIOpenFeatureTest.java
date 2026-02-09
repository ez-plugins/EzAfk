package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class GUIOpenFeatureTest {
    private org.mockbukkit.mockbukkit.ServerMock server;
    private JavaPlugin plugin;
    private com.gyvex.ezafk.EzAfk ezafk;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        plugin = TestHelpers.loadPlugin();
        ezafk = (com.gyvex.ezafk.EzAfk) plugin;
    }

    @AfterEach
    public void tearDown() {
        TestHelpers.stopServer();
    }

    @Test
    public void guiCommandOpensInventoryForPlayer() throws Exception {
        Player admin = server.addPlayer("GuiAdmin");
        admin.setOp(true);

        com.gyvex.ezafk.command.EzAfkCommand cmd = new com.gyvex.ezafk.command.EzAfkCommand(ezafk);

        Method handleGui = com.gyvex.ezafk.command.EzAfkCommand.class.getDeclaredMethod("handleGui", CommandSender.class);
        handleGui.setAccessible(true);

        assertDoesNotThrow(() -> handleGui.invoke(cmd, admin));
        assertNotNull(admin.getOpenInventory(), "Player should have an open inventory after /afk gui");
    }
}
