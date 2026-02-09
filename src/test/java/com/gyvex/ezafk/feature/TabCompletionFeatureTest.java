package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.command.EzAfkTabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TabCompletionFeatureTest {
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
    public void mainCommandCompletionSuggestsGui() {
        Player p = server.addPlayer("User1");
        EzAfkTabCompleter completer = new EzAfkTabCompleter();

        List<String> completions = completer.onTabComplete(p, null, "afk", new String[]{"g"});
        assertNotNull(completions);
        assertTrue(completions.contains("gui"), "Expected 'gui' suggestion for prefix 'g'");
    }

    @Test
    public void playerNameSuggestionsIncludeOnlinePlayers() {
        Player a = server.addPlayer("Alice");
        Player g = server.addPlayer("Gamma");
        EzAfkTabCompleter completer = new EzAfkTabCompleter();

        List<String> completions = completer.onTabComplete(a, null, "afk", new String[]{"toggle", "G"});
        assertNotNull(completions);
        assertTrue(completions.stream().anyMatch(s -> s.equalsIgnoreCase("Gamma")), "Expected online player 'Gamma' to be suggested");
    }
}
