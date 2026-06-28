package com.gyvex.ezafk.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BypassListManager} and the underlying {@link BypassList} logic.
 *
 * <p>No MockBukkit server is started — only the Bukkit API classes that don't require a
 * running server ({@link YamlConfiguration}) are used.</p>
 */
class BypassListManagerTest {

    @BeforeEach
    void reset() {
        BypassListManager.reset();
    }

    @AfterEach
    void cleanup() {
        BypassListManager.reset();
    }

    // ── Whitelist: add ────────────────────────────────────────────────────────

    @Test
    void addToWhitelist_newId_returnsTrue() {
        assertTrue(BypassListManager.addToWhitelist(UUID.randomUUID()));
    }

    @Test
    void addToWhitelist_duplicateId_returnsFalse() {
        UUID id = UUID.randomUUID();
        BypassListManager.addToWhitelist(id);
        assertFalse(BypassListManager.addToWhitelist(id));
    }

    @Test
    void addToWhitelist_marksIdAsWhitelisted() {
        UUID id = UUID.randomUUID();
        BypassListManager.addToWhitelist(id);
        assertTrue(BypassListManager.isWhitelisted(id));
    }

    // ── Whitelist: check ──────────────────────────────────────────────────────

    @Test
    void isWhitelisted_unknownId_returnsFalse() {
        assertFalse(BypassListManager.isWhitelisted(UUID.randomUUID()));
    }

    // ── Whitelist: remove ─────────────────────────────────────────────────────

    @Test
    void removeFromWhitelist_presentId_returnsTrueAndClearsFlag() {
        UUID id = UUID.randomUUID();
        BypassListManager.addToWhitelist(id);
        assertTrue(BypassListManager.removeFromWhitelist(id));
        assertFalse(BypassListManager.isWhitelisted(id));
    }

    @Test
    void removeFromWhitelist_absentId_returnsFalse() {
        assertFalse(BypassListManager.removeFromWhitelist(UUID.randomUUID()));
    }

    // ── Whitelist: snapshot ───────────────────────────────────────────────────

    @Test
    void getWhitelist_containsAddedId() {
        UUID id = UUID.randomUUID();
        BypassListManager.addToWhitelist(id);
        assertTrue(BypassListManager.getWhitelist().contains(id));
    }

    @Test
    void getWhitelist_returnsImmutableView() {
        UUID id = UUID.randomUUID();
        BypassListManager.addToWhitelist(id);
        Set<UUID> view = BypassListManager.getWhitelist();
        assertThrows(UnsupportedOperationException.class, () -> view.add(UUID.randomUUID()));
    }

    // ── Blacklist: add ────────────────────────────────────────────────────────

    @Test
    void addToBlacklist_newId_returnsTrue() {
        assertTrue(BypassListManager.addToBlacklist(UUID.randomUUID()));
    }

    @Test
    void addToBlacklist_duplicateId_returnsFalse() {
        UUID id = UUID.randomUUID();
        BypassListManager.addToBlacklist(id);
        assertFalse(BypassListManager.addToBlacklist(id));
    }

    @Test
    void addToBlacklist_marksIdAsBlacklisted() {
        UUID id = UUID.randomUUID();
        BypassListManager.addToBlacklist(id);
        assertTrue(BypassListManager.isBlacklisted(id));
    }

    // ── Blacklist: check ──────────────────────────────────────────────────────

    @Test
    void isBlacklisted_unknownId_returnsFalse() {
        assertFalse(BypassListManager.isBlacklisted(UUID.randomUUID()));
    }

    // ── Blacklist: remove ─────────────────────────────────────────────────────

    @Test
    void removeFromBlacklist_presentId_returnsTrueAndClearsFlag() {
        UUID id = UUID.randomUUID();
        BypassListManager.addToBlacklist(id);
        assertTrue(BypassListManager.removeFromBlacklist(id));
        assertFalse(BypassListManager.isBlacklisted(id));
    }

    @Test
    void removeFromBlacklist_absentId_returnsFalse() {
        assertFalse(BypassListManager.removeFromBlacklist(UUID.randomUUID()));
    }

    // ── Blacklist: snapshot ───────────────────────────────────────────────────

    @Test
    void getBlacklist_containsAddedId() {
        UUID id = UUID.randomUUID();
        BypassListManager.addToBlacklist(id);
        assertTrue(BypassListManager.getBlacklist().contains(id));
    }

    @Test
    void getBlacklist_returnsImmutableView() {
        UUID id = UUID.randomUUID();
        BypassListManager.addToBlacklist(id);
        Set<UUID> view = BypassListManager.getBlacklist();
        assertThrows(UnsupportedOperationException.class, () -> view.add(UUID.randomUUID()));
    }

    // ── Independence ──────────────────────────────────────────────────────────

    @Test
    void whitelist_and_blacklist_are_independent() {
        UUID id = UUID.randomUUID();
        BypassListManager.addToWhitelist(id);
        assertFalse(BypassListManager.isBlacklisted(id));

        BypassListManager.addToBlacklist(id);
        // Now both lists contain the same id — each list tracks it separately
        assertTrue(BypassListManager.isWhitelisted(id));
        assertTrue(BypassListManager.isBlacklisted(id));
    }

    @Test
    void removing_from_whitelist_does_not_affect_blacklist() {
        UUID id = UUID.randomUUID();
        BypassListManager.addToWhitelist(id);
        BypassListManager.addToBlacklist(id);
        BypassListManager.removeFromWhitelist(id);
        assertFalse(BypassListManager.isWhitelisted(id));
        assertTrue(BypassListManager.isBlacklisted(id));
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    @Test
    void loadFromFile_readsWhitelistAndBlacklist(@TempDir Path tempDir) throws IOException {
        UUID wlId = UUID.randomUUID();
        UUID blId = UUID.randomUUID();

        File file = tempDir.resolve("bypass-lists.yml").toFile();
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("whitelist", List.of(wlId.toString()));
        cfg.set("blacklist", List.of(blId.toString()));
        cfg.save(file);

        BypassListManager.loadFromFile(file);

        assertTrue(BypassListManager.isWhitelisted(wlId));
        assertTrue(BypassListManager.isBlacklisted(blId));
    }

    @Test
    void save_and_reload_preserves_entries(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("bypass-lists.yml").toFile();
        file.createNewFile();
        // Point the manager at the temp file so save() will write to it
        BypassListManager.dataFile = file;

        UUID wlId = UUID.randomUUID();
        UUID blId = UUID.randomUUID();
        BypassListManager.addToWhitelist(wlId);  // triggers save()
        BypassListManager.addToBlacklist(blId);  // triggers save()

        // Full reset — clears in-memory state and dataFile pointer
        BypassListManager.reset();
        assertFalse(BypassListManager.isWhitelisted(wlId));
        assertFalse(BypassListManager.isBlacklisted(blId));

        // Reload from the file that save() wrote to
        BypassListManager.loadFromFile(file);
        assertTrue(BypassListManager.isWhitelisted(wlId));
        assertTrue(BypassListManager.isBlacklisted(blId));
    }

    @Test
    void loadFromFile_malformedUuids_areSkipped(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("bypass-lists.yml").toFile();
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("whitelist", List.of("not-a-uuid", "also-invalid", "12345"));
        cfg.set("blacklist", List.of());
        cfg.save(file);

        assertDoesNotThrow(() -> BypassListManager.loadFromFile(file));
        assertTrue(BypassListManager.getWhitelist().isEmpty());
    }

    @Test
    void loadFromFile_emptyFile_producesEmptyLists(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("bypass-lists.yml").toFile();
        file.createNewFile();
        assertDoesNotThrow(() -> BypassListManager.loadFromFile(file));
        assertTrue(BypassListManager.getWhitelist().isEmpty());
        assertTrue(BypassListManager.getBlacklist().isEmpty());
    }
}
