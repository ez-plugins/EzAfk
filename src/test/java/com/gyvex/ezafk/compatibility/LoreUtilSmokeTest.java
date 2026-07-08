package com.gyvex.ezafk.compatibility;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for {@link LoreUtil}, verifying that:
 * <ul>
 *   <li>Legacy {@code &}-color codes are translated to {@code §} codes.</li>
 *   <li>MiniMessage tags ({@code <red>}) are resolved to {@code §} codes via
 *       the bundled Adventure library.</li>
 *   <li>Null / blank inputs are handled gracefully.</li>
 * </ul>
 *
 * These tests use a MockBukkit server to obtain real {@link ItemMeta} instances,
 * which avoids JVM agent constraints around inline mocking on newer JDKs.
 */
class LoreUtilSmokeTest {

    // §c is the section-sign + 'c' color code for red (U+00A7 + 'c')
    private static final char SECTION = '\u00a7';

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemMeta newMeta() {
        ItemStack stack = new ItemStack(Material.STONE);
        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        return meta;
    }

    // ── setLore ──────────────────────────────────────────────────────────────

    @Test
    void setLore_translates_legacy_amp_color_codes() {
        ItemMeta meta = newMeta();
        LoreUtil.setLore(meta, List.of("&cRed line", "&aGreen line"), null, null);

        List<String> lore = meta.getLore();
        assertNotNull(lore);
        assertEquals(2, lore.size());
        assertTrue(lore.get(0).startsWith(SECTION + "c"), "§c prefix expected for &c");
        assertTrue(lore.get(1).startsWith(SECTION + "a"), "§a prefix expected for &a");
    }

    @Test
    void setLore_parses_minimessage_tags() {
        ItemMeta meta = newMeta();
        LoreUtil.setLore(meta, List.of("<red>hello</red>"), null, null);

        List<String> lore = meta.getLore();
        assertNotNull(lore);
        String line = lore.get(0);
        // LegacyComponentSerializer should emit §c for <red>
        assertTrue(line.contains(SECTION + "c"), "expected §c from <red> tag, got: " + line);
    }

    @Test
    void setLore_handles_null_lore_list_without_interacting_with_meta() {
        ItemMeta meta = newMeta();
        meta.setLore(List.of("existing"));
        LoreUtil.setLore(meta, null, null, null);
        assertEquals(List.of("existing"), meta.getLore());
    }

    @Test
    void setLore_handles_empty_lore_list_without_interacting_with_meta() {
        ItemMeta meta = newMeta();
        meta.setLore(List.of("existing"));
        LoreUtil.setLore(meta, List.of(), null, null);
        assertEquals(List.of("existing"), meta.getLore());
    }

    @Test
    void setLore_preserves_plain_text_without_color_codes() {
        ItemMeta meta = newMeta();
        LoreUtil.setLore(meta, List.of("Plain text line"), null, null);

        List<String> lore = meta.getLore();
        assertNotNull(lore);
        assertEquals("Plain text line", lore.get(0));
    }

    // ── setDisplayName ───────────────────────────────────────────────────────

    @Test
    void setDisplayName_translates_legacy_amp_color_codes() {
        ItemMeta meta = newMeta();
        LoreUtil.setDisplayName(meta, "&6Gold Name", null, null);

        String displayName = meta.getDisplayName();
        assertNotNull(displayName);
        assertTrue(displayName.startsWith(SECTION + "6"), "§6 prefix expected");
    }

    @Test
    void setDisplayName_parses_minimessage_tags() {
        ItemMeta meta = newMeta();
        LoreUtil.setDisplayName(meta, "<bold>Title</bold>", null, null);

        String displayName = meta.getDisplayName();
        assertNotNull(displayName);
        // §l is the bold code
        assertTrue(displayName.contains(SECTION + "l"), "§l expected for <bold>");
    }

    @Test
    void setDisplayName_handles_blank_without_interacting_with_meta() {
        ItemMeta meta = newMeta();
        meta.setDisplayName("existing");
        LoreUtil.setDisplayName(meta, "   ", null, null);
        assertEquals("existing", meta.getDisplayName());
    }

    @Test
    void setDisplayName_handles_null_without_interacting_with_meta() {
        ItemMeta meta = newMeta();
        meta.setDisplayName("existing");
        LoreUtil.setDisplayName(meta, null, null, null);
        assertEquals("existing", meta.getDisplayName());
    }

    // ── validateMiniMessage ──────────────────────────────────────────────────

    @Test
    void validateMiniMessage_returns_true_for_valid_tag() {
        assertTrue(LoreUtil.validateMiniMessage("<red>hello</red>", "test", null));
    }

    @Test
    void validateMiniMessage_returns_true_for_plain_text() {
        assertTrue(LoreUtil.validateMiniMessage("plain text", "test", null));
    }

    @Test
    void validateMiniMessage_returns_true_for_blank_string() {
        assertTrue(LoreUtil.validateMiniMessage("", "test", null));
    }

    @Test
    void validateMiniMessage_returns_true_for_null() {
        assertTrue(LoreUtil.validateMiniMessage(null, "test", null));
    }
}
