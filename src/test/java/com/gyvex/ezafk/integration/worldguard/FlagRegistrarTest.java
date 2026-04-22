package com.gyvex.ezafk.integration.worldguard;

import com.gyvex.ezafk.integration.worldguard.flag.AfkBypassFlag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class FlagRegistrarTest {

    private Logger logger;
    private List<LogRecord> records;
    private Handler handler;

    @BeforeEach
    public void setupLogger() {
        logger = Logger.getLogger("FlagRegistrarTest");
        logger.setLevel(Level.ALL);
        records = new ArrayList<>();
        handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        };
        logger.addHandler(handler);
    }

    @AfterEach
    public void teardownLogger() {
        logger.removeHandler(handler);
    }

    @Test
    public void ensureRegistered_registersFlagWhenRegistryAccepts() {
        TestRegistry registry = new TestRegistry();

        // Ensure the candidate flag is constructed
        Object candidate = AfkBypassFlag.get();
        assertNotNull(candidate, "AfkBypassFlag.get() should construct a candidate StateFlag");

        Object active = AfkBypassFlag.ensureRegistered(registry, logger, "auto");
        assertNotNull(active, "ensureRegistered should return the active flag when registration succeeds");

        Object found = registry.get("afk-bypass");
        assertNotNull(found, "Registry should contain the registered flag");

        boolean logged = records.stream().anyMatch(r -> r.getMessage().contains("Registered WorldGuard flag: afk-bypass"));
        assertTrue(logged, "Should log registration message");
    }

    @Test
    public void ensureRegistered_usesExistingWhenRegisterFails() {
        TestRegistry registry = new TestRegistry();
        // Pre-populate with existing flag
        Object pre = AfkBypassFlag.get();
        registry.register((com.sk89q.worldguard.protection.flags.StateFlag) pre);

        // Now simulate register throwing and ensure existing is picked up
        registry.setThrowOnRegister(true);
        Object active = AfkBypassFlag.ensureRegistered(registry, logger, "auto");
        assertNotNull(active, "Should return existing flag when register fails and existing is present");

        boolean usedExisting = records.stream().anyMatch(r -> r.getMessage().contains("Using existing WorldGuard flag: afk-bypass"));
        assertTrue(usedExisting, "Should log that existing flag is used");
    }
}
