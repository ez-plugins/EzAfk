package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.listener.PlayerActivityListener;
import com.gyvex.ezafk.state.LastActiveState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerActivityListenerAllEventsTest {

    private ServerMock server;
    private JavaPlugin plugin;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        plugin = TestHelpers.loadPlugin();
        server.getPluginManager().registerEvents(new PlayerActivityListener(), plugin);
    }

    @AfterEach
    public void tearDown() {
        LastActiveState.lastActive.clear();
        TestHelpers.stopServer();
    }

    @Test
    public void player_interact_event_updates_last_active() {
        Player p = server.addPlayer("InteractPlayer");
        LastActiveState.lastActive.put(p.getUniqueId(), 0L);

        server.getPluginManager().callEvent(
            new PlayerInteractEvent(p, Action.LEFT_CLICK_AIR, null, null, null)
        );

        assertTrue(LastActiveState.getLastActive(p.getUniqueId()) > 0L,
            "PlayerInteractEvent should update lastActive timestamp");
    }

    @Test
    public void async_player_chat_event_updates_last_active() {
        Player p = server.addPlayer("ChatPlayer");
        LastActiveState.lastActive.put(p.getUniqueId(), 0L);

        server.getPluginManager().callEvent(
            new AsyncPlayerChatEvent(false, p, "hello", new HashSet<>())
        );

        assertTrue(LastActiveState.getLastActive(p.getUniqueId()) > 0L,
            "AsyncPlayerChatEvent should update lastActive timestamp");
    }

    @Test
    public void player_command_event_updates_last_active() {
        Player p = server.addPlayer("CmdPlayer");
        LastActiveState.lastActive.put(p.getUniqueId(), 0L);

        server.getPluginManager().callEvent(
            new org.bukkit.event.player.PlayerCommandPreprocessEvent(p, "/afk")
        );

        assertTrue(LastActiveState.getLastActive(p.getUniqueId()) > 0L,
            "PlayerCommandPreprocessEvent should update lastActive timestamp");
    }
}
