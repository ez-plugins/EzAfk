package com.gyvex.ezafk.feature;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.TestHelpers;
import com.gyvex.ezafk.command.EzAfkCommand;
import com.gyvex.ezafk.command.EzAfkTabCompleter;
import com.gyvex.ezafk.manager.BypassListManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feature tests for the {@code /afk bypass whitelist|blacklist} subcommands and their tab
 * completions.  Each test starts with an empty bypass list (via {@link BypassListManager#reset()})
 * to guarantee isolation.
 */
public class BypassListCommandFeatureTest {

    private ServerMock server;
    private EzAfk ezafk;
    private EzAfkCommand cmd;
    private EzAfkTabCompleter completer;

    @BeforeEach
    public void setUp() {
        server = TestHelpers.startServer();
        JavaPlugin plugin = TestHelpers.loadPlugin();
        ezafk = (EzAfk) plugin;
        cmd = new EzAfkCommand(ezafk);
        completer = new EzAfkTabCompleter();
        // Start each test with empty lists; dataFile=null so saves during tests are no-ops
        BypassListManager.reset();
    }

    @AfterEach
    public void tearDown() {
        BypassListManager.reset();
        TestHelpers.stopServer();
    }

    // ── Whitelist: add ────────────────────────────────────────────────────────

    @Test
    public void whitelist_add_adds_player_to_whitelist() {
        Player target = server.addPlayer("WLTarget");
        CommandSender console = server.getConsoleSender();

        assertTrue(cmd.onCommand(console, null, "afk",
                new String[]{"bypass", "whitelist", "add", "WLTarget"}));

        assertTrue(BypassListManager.isWhitelisted(target.getUniqueId()),
                "Player should be on the whitelist after add command");
    }

    @Test
    public void whitelist_add_duplicate_does_not_duplicate_entry() {
        Player target = server.addPlayer("WLDupe");
        CommandSender console = server.getConsoleSender();

        cmd.onCommand(console, null, "afk", new String[]{"bypass", "whitelist", "add", "WLDupe"});
        cmd.onCommand(console, null, "afk", new String[]{"bypass", "whitelist", "add", "WLDupe"});

        assertEquals(1, BypassListManager.getWhitelist().size(),
                "Duplicate add should not create duplicate entries");
    }

    // ── Whitelist: remove ─────────────────────────────────────────────────────

    @Test
    public void whitelist_remove_removes_player_from_whitelist() {
        Player target = server.addPlayer("WLRemove");
        BypassListManager.addToWhitelist(target.getUniqueId());

        CommandSender console = server.getConsoleSender();
        assertTrue(cmd.onCommand(console, null, "afk",
                new String[]{"bypass", "whitelist", "remove", "WLRemove"}));

        assertFalse(BypassListManager.isWhitelisted(target.getUniqueId()),
                "Player should be removed from whitelist");
    }

    @Test
    public void whitelist_remove_absent_player_runs_without_error() {
        Player target = server.addPlayer("WLAbsent");
        CommandSender console = server.getConsoleSender();

        // Target is not on the whitelist — command should still return true and not throw
        assertTrue(cmd.onCommand(console, null, "afk",
                new String[]{"bypass", "whitelist", "remove", "WLAbsent"}));
        assertFalse(BypassListManager.isWhitelisted(target.getUniqueId()));
    }

    // ── Whitelist: list ───────────────────────────────────────────────────────

    @Test
    public void whitelist_list_runs_without_error() {
        CommandSender console = server.getConsoleSender();
        // Empty list
        assertTrue(cmd.onCommand(console, null, "afk",
                new String[]{"bypass", "whitelist", "list"}));
        // Non-empty list
        Player target = server.addPlayer("WLListed");
        BypassListManager.addToWhitelist(target.getUniqueId());
        assertTrue(cmd.onCommand(console, null, "afk",
                new String[]{"bypass", "whitelist", "list"}));
    }

    // ── Blacklist: add ────────────────────────────────────────────────────────

    @Test
    public void blacklist_add_adds_player_to_blacklist() {
        Player target = server.addPlayer("BLTarget");
        CommandSender console = server.getConsoleSender();

        assertTrue(cmd.onCommand(console, null, "afk",
                new String[]{"bypass", "blacklist", "add", "BLTarget"}));

        assertTrue(BypassListManager.isBlacklisted(target.getUniqueId()),
                "Player should be on the blacklist after add command");
    }

    @Test
    public void blacklist_add_duplicate_does_not_duplicate_entry() {
        Player target = server.addPlayer("BLDupe");
        CommandSender console = server.getConsoleSender();

        cmd.onCommand(console, null, "afk", new String[]{"bypass", "blacklist", "add", "BLDupe"});
        cmd.onCommand(console, null, "afk", new String[]{"bypass", "blacklist", "add", "BLDupe"});

        assertEquals(1, BypassListManager.getBlacklist().size(),
                "Duplicate add should not create duplicate entries");
    }

    // ── Blacklist: remove ─────────────────────────────────────────────────────

    @Test
    public void blacklist_remove_removes_player_from_blacklist() {
        Player target = server.addPlayer("BLRemove");
        BypassListManager.addToBlacklist(target.getUniqueId());

        CommandSender console = server.getConsoleSender();
        assertTrue(cmd.onCommand(console, null, "afk",
                new String[]{"bypass", "blacklist", "remove", "BLRemove"}));

        assertFalse(BypassListManager.isBlacklisted(target.getUniqueId()),
                "Player should be removed from blacklist");
    }

    @Test
    public void blacklist_remove_absent_player_runs_without_error() {
        Player target = server.addPlayer("BLAbsent");
        CommandSender console = server.getConsoleSender();

        assertTrue(cmd.onCommand(console, null, "afk",
                new String[]{"bypass", "blacklist", "remove", "BLAbsent"}));
        assertFalse(BypassListManager.isBlacklisted(target.getUniqueId()));
    }

    // ── Blacklist: list ───────────────────────────────────────────────────────

    @Test
    public void blacklist_list_runs_without_error() {
        CommandSender console = server.getConsoleSender();
        // Empty list
        assertTrue(cmd.onCommand(console, null, "afk",
                new String[]{"bypass", "blacklist", "list"}));
        // Non-empty list
        Player target = server.addPlayer("BLListed");
        BypassListManager.addToBlacklist(target.getUniqueId());
        assertTrue(cmd.onCommand(console, null, "afk",
                new String[]{"bypass", "blacklist", "list"}));
    }

    // ── Permission guard ──────────────────────────────────────────────────────

    @Test
    public void player_without_permission_cannot_add_to_whitelist() {
        Player actor = server.addPlayer("NoPermActor");
        Player target = server.addPlayer("NoPermTarget");
        // actor is a regular player — has no "ezafk.bypass.manage" permission

        cmd.onCommand(actor, null, "afk",
                new String[]{"bypass", "whitelist", "add", "NoPermTarget"});

        assertFalse(BypassListManager.isWhitelisted(target.getUniqueId()),
                "Player without permission should not be able to modify the whitelist");
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Test
    public void tab_bypass_depth2_suggests_whitelist_and_blacklist() {
        Player actor = server.addPlayer("TabUser1");

        List<String> results = completer.onTabComplete(actor, null, "afk",
                new String[]{"bypass", ""});

        assertNotNull(results);
        assertTrue(results.contains("whitelist"),
                "Should suggest 'whitelist' at depth 2 of bypass subcommand");
        assertTrue(results.contains("blacklist"),
                "Should suggest 'blacklist' at depth 2 of bypass subcommand");
    }

    @Test
    public void tab_bypass_whitelist_depth3_suggests_actions() {
        Player actor = server.addPlayer("TabUser2");

        List<String> results = completer.onTabComplete(actor, null, "afk",
                new String[]{"bypass", "whitelist", ""});

        assertNotNull(results);
        assertTrue(results.contains("add"),    "Should suggest 'add' action");
        assertTrue(results.contains("remove"), "Should suggest 'remove' action");
        assertTrue(results.contains("list"),   "Should suggest 'list' action");
    }

    @Test
    public void tab_bypass_blacklist_depth3_suggests_actions() {
        Player actor = server.addPlayer("TabUser3");

        List<String> results = completer.onTabComplete(actor, null, "afk",
                new String[]{"bypass", "blacklist", ""});

        assertNotNull(results);
        assertTrue(results.contains("add"),    "Should suggest 'add' action");
        assertTrue(results.contains("remove"), "Should suggest 'remove' action");
        assertTrue(results.contains("list"),   "Should suggest 'list' action");
    }

    @Test
    public void tab_bypass_whitelist_add_depth4_suggests_player_names() {
        Player actor = server.addPlayer("TabUser4");
        server.addPlayer("Phoenix");

        List<String> results = completer.onTabComplete(actor, null, "afk",
                new String[]{"bypass", "whitelist", "add", "P"});

        assertNotNull(results);
        assertTrue(results.stream().anyMatch(s -> s.equalsIgnoreCase("Phoenix")),
                "Should suggest online player 'Phoenix' for prefix 'P'");
    }

    @Test
    public void tab_bypass_blacklist_remove_depth4_suggests_player_names() {
        Player actor = server.addPlayer("TabUser5");
        server.addPlayer("Storm");

        List<String> results = completer.onTabComplete(actor, null, "afk",
                new String[]{"bypass", "blacklist", "remove", "S"});

        assertNotNull(results);
        assertTrue(results.stream().anyMatch(s -> s.equalsIgnoreCase("Storm")),
                "Should suggest online player 'Storm' for prefix 'S'");
    }
}
