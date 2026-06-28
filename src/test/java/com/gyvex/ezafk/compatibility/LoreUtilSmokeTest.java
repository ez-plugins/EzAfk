package com.gyvex.ezafk.compatibility;

import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Smoke tests for {@link LoreUtil}, verifying that:
 * <ul>
 *   <li>Legacy {@code &}-color codes are translated to {@code §} codes.</li>
 *   <li>MiniMessage tags ({@code <red>}) are resolved to {@code §} codes via
 *       the bundled Adventure library.</li>
 *   <li>Null / blank inputs are handled gracefully.</li>
 * </ul>
 *
 * These tests run without a live Bukkit server; {@link ItemMeta} is mocked with
 * Mockito and {@link org.bukkit.ChatColor#translateAlternateColorCodes} is pure
 * string manipulation that requires no server instance.
 */
@SuppressWarnings("unchecked")
class LoreUtilSmokeTest {

    // §c is the section-sign + 'c' color code for red (U+00A7 + 'c')
    private static final char SECTION = '\u00a7';

    // ── setLore ──────────────────────────────────────────────────────────────

    @Test
    void setLore_translates_legacy_amp_color_codes() {
        ItemMeta meta = mock(ItemMeta.class);
        LoreUtil.setLore(meta, List.of("&cRed line", "&aGreen line"), null, null);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(meta).setLore(captor.capture());

        List<String> lore = captor.getValue();
        assertEquals(2, lore.size());
        assertTrue(lore.get(0).startsWith(SECTION + "c"), "§c prefix expected for &c");
        assertTrue(lore.get(1).startsWith(SECTION + "a"), "§a prefix expected for &a");
    }

    @Test
    void setLore_parses_minimessage_tags() {
        ItemMeta meta = mock(ItemMeta.class);
        LoreUtil.setLore(meta, List.of("<red>hello</red>"), null, null);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(meta).setLore(captor.capture());

        String line = captor.getValue().get(0);
        // LegacyComponentSerializer should emit §c for <red>
        assertTrue(line.contains(SECTION + "c"), "expected §c from <red> tag, got: " + line);
    }

    @Test
    void setLore_handles_null_lore_list_without_interacting_with_meta() {
        ItemMeta meta = mock(ItemMeta.class);
        LoreUtil.setLore(meta, null, null, null);
        verifyNoInteractions(meta);
    }

    @Test
    void setLore_handles_empty_lore_list_without_interacting_with_meta() {
        ItemMeta meta = mock(ItemMeta.class);
        LoreUtil.setLore(meta, List.of(), null, null);
        verifyNoInteractions(meta);
    }

    @Test
    void setLore_preserves_plain_text_without_color_codes() {
        ItemMeta meta = mock(ItemMeta.class);
        LoreUtil.setLore(meta, List.of("Plain text line"), null, null);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(meta).setLore(captor.capture());
        assertEquals("Plain text line", captor.getValue().get(0));
    }

    // ── setDisplayName ───────────────────────────────────────────────────────

    @Test
    void setDisplayName_translates_legacy_amp_color_codes() {
        ItemMeta meta = mock(ItemMeta.class);
        LoreUtil.setDisplayName(meta, "&6Gold Name", null, null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(meta).setDisplayName(captor.capture());
        assertTrue(captor.getValue().startsWith(SECTION + "6"), "§6 prefix expected");
    }

    @Test
    void setDisplayName_parses_minimessage_tags() {
        ItemMeta meta = mock(ItemMeta.class);
        LoreUtil.setDisplayName(meta, "<bold>Title</bold>", null, null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(meta).setDisplayName(captor.capture());
        // §l is the bold code
        assertTrue(captor.getValue().contains(SECTION + "l"), "§l expected for <bold>");
    }

    @Test
    void setDisplayName_handles_blank_without_interacting_with_meta() {
        ItemMeta meta = mock(ItemMeta.class);
        LoreUtil.setDisplayName(meta, "   ", null, null);
        verifyNoInteractions(meta);
    }

    @Test
    void setDisplayName_handles_null_without_interacting_with_meta() {
        ItemMeta meta = mock(ItemMeta.class);
        LoreUtil.setDisplayName(meta, null, null, null);
        verifyNoInteractions(meta);
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
