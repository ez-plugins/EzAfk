package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.TestHelpers;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

public class EconomyManagerTest {

    private ServerMock server;
    private Player player;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        TestHelpers.loadPlugin();
        player = server.addPlayer("EconPlayer");
    }

    @AfterEach
    public void tearDown() {
        EconomyManager.reset();
        TestHelpers.stopServer();
    }

    @Test
    public void reset_clears_internal_state_without_throwing() {
        assertDoesNotThrow(EconomyManager::reset, "EconomyManager.reset() should not throw");
    }

    @Test
    public void isEconomyBlocked_returns_false_when_economy_disabled() {
        // Economy is disabled in default config (economy.enabled=false)
        assertFalse(EconomyManager.isEconomyBlocked(player),
            "isEconomyBlocked should return false when economy integration is inactive");
    }

    @Test
    public void isEconomyBlocked_returns_false_for_null_player() {
        assertFalse(EconomyManager.isEconomyBlocked(null),
            "isEconomyBlocked should return false for null player");
    }

    @Test
    public void onActivity_does_not_throw_for_valid_player() {
        assertDoesNotThrow(() -> EconomyManager.onActivity(player),
            "onActivity should not throw for a valid player");
    }

    @Test
    public void onActivity_does_not_throw_for_null_player() {
        assertDoesNotThrow(() -> EconomyManager.onActivity(null),
            "onActivity should not throw for null player");
    }

    @Test
    public void onDisable_does_not_throw_for_valid_uuid() {
        assertDoesNotThrow(() -> EconomyManager.onDisable(player.getUniqueId()),
            "onDisable should not throw for a valid UUID");
    }

    @Test
    public void onDisable_does_not_throw_for_null_uuid() {
        assertDoesNotThrow(() -> EconomyManager.onDisable(null),
            "onDisable should not throw for null UUID");
    }

    @Test
    public void handleEnter_returns_true_when_economy_disabled() {
        // No economy configured -> EconomyManager proceeds and returns true
        assertTrue(EconomyManager.handleEnter(player, true),
            "handleEnter should return true when economy integration is inactive");
    }
}
