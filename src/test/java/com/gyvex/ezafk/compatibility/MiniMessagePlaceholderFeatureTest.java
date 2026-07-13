package com.gyvex.ezafk.compatibility;

import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.manager.MessageManager;
import com.gyvex.ezafk.state.AfkState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class MiniMessagePlaceholderFeatureTest {

    private static final char SECTION = '\u00a7';
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = TestHelpers.startServer();
        TestHelpers.loadPlugin();
    }

    @AfterEach
    void tearDown() {
        AfkState.afkPlayers.clear();
        TestHelpers.stopServer();
    }

    @Test
    void getMessage_parses_minimessage_tags_in_fallback_message() {
        PlayerMock player = (PlayerMock) server.addPlayer("MiniMsgPlayer");

        String result = MessageManager.getMessage(
                "nonexistent.path",
                "<red>Hello</red> <bold>World</bold>",
                null,
                player
        );

        assertNotNull(result);
        assertTrue(result.contains(SECTION + "c"), "MiniMessage <red> should become §c");
        assertTrue(result.contains(SECTION + "l"), "MiniMessage <bold> should become §l");
    }

    @Test
    void getMessage_parses_minimessage_tags_with_legacy_colors() {
        PlayerMock player = (PlayerMock) server.addPlayer("MixedPlayer");

        String result = MessageManager.getMessage(
                "nonexistent.path",
                "&e[yellow] <red>%player%</red> &7[is AFK]",
                null,
                player
        );

        assertNotNull(result);
        assertTrue(result.contains(SECTION + "e"), "Amp color codes should be translated");
        assertTrue(result.contains(SECTION + "c"), "MiniMessage <red> should become §c");
        assertTrue(result.contains(SECTION + "7"), "Amp color codes should be translated");
        assertTrue(result.contains("%player%"), "PAPI placeholders remain when PAPI unavailable");
    }

    @Test
    void getMessage_handles_plain_text_without_colors() {
        PlayerMock player = (PlayerMock) server.addPlayer("PlainPlayer");

        String result = MessageManager.getMessage(
                "nonexistent.path",
                "Player %player% is now AFK",
                null,
                player
        );

        assertNotNull(result);
        assertEquals("Player %player% is now AFK", result);
    }

    @Test
    void getMessage_handles_only_legacy_amp_colors() {
        PlayerMock player = (PlayerMock) server.addPlayer("ColorPlayer");

        String result = MessageManager.getMessage(
                "nonexistent.path",
                "&c&lPlayer &7%player% &cis no longer AFK",
                null,
                player
        );

        assertNotNull(result);
        assertTrue(result.contains(SECTION + "c"), "Should contain §c");
        assertTrue(result.contains(SECTION + "l"), "Should contain §l for bold");
        assertTrue(result.contains(SECTION + "7"), "Should contain §7");
    }
}