package com.gyvex.ezafk.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class AfkTimeManagerTest {

    @BeforeEach
    void resetState() throws Exception {
        clearMap("totalAfkSeconds");
        clearSet("dirtyPlayers");
        clearMap("leaderboardEntries");
        clearLeaderboardCache();
        setTimesDirectory(null);
    }

    @Test
    void shouldReorderTopPlayersWhenTotalsChange() {
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID playerThree = UUID.randomUUID();
        UUID playerFour = UUID.randomUUID();

        recordSeconds(playerOne, 10);
        recordSeconds(playerTwo, 30);
        recordSeconds(playerThree, 20);
        recordSeconds(playerFour, 5);

        List<Map.Entry<UUID, Long>> initialTop = AfkTimeManager.getTopPlayers(3);
        assertEquals(List.of(playerTwo, playerThree, playerOne), extractPlayerOrder(initialTop));

        recordSeconds(playerOne, 30); // player one climbs to the top
        List<Map.Entry<UUID, Long>> updatedTop = AfkTimeManager.getTopPlayers(3);
        assertEquals(List.of(playerOne, playerTwo, playerThree), extractPlayerOrder(updatedTop));

        recordSeconds(playerFour, 45); // player four jumps from outside into the lead
        List<Map.Entry<UUID, Long>> finalTop = AfkTimeManager.getTopPlayers(3);
        assertEquals(List.of(playerFour, playerOne, playerTwo), extractPlayerOrder(finalTop));

        assertEquals(50L, finalTop.getFirst().getValue());
        assertEquals(40L, finalTop.get(1).getValue());
        assertEquals(30L, finalTop.get(2).getValue());
    }

    @Test
    void rebuildLeaderboardCacheReflectsStoredTotals() throws Exception {
        Map<UUID, Long> totals = getTotalsMap();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID playerThree = UUID.randomUUID();

        totals.put(playerOne, 15L);
        totals.put(playerTwo, 40L);
        totals.put(playerThree, 25L);

        invokeRebuild();

        List<Map.Entry<UUID, Long>> topPlayers = AfkTimeManager.getTopPlayers(0);
        assertEquals(List.of(playerTwo, playerThree, playerOne), extractPlayerOrder(topPlayers));
        assertIterableEquals(List.of(40L, 25L, 15L), extractTotals(topPlayers));
    }

    private void recordSeconds(UUID playerId, long seconds) {
        long start = 1_000L;
        long end = start + (seconds * 1_000L);
        AfkTimeManager.recordAfkSession(playerId, start, end);
    }

    private List<UUID> extractPlayerOrder(List<Map.Entry<UUID, Long>> entries) {
        return entries.stream().map(Map.Entry::getKey).toList();
    }

    private List<Long> extractTotals(List<Map.Entry<UUID, Long>> entries) {
        return entries.stream().map(Map.Entry::getValue).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Long> getTotalsMap() throws Exception {
        Field field = AfkTimeManager.class.getDeclaredField("totalAfkSeconds");
        field.setAccessible(true);
        return (Map<UUID, Long>) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private void clearMap(String fieldName) throws Exception {
        Field field = AfkTimeManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
    }

    private void clearSet(String fieldName) throws Exception {
        Field field = AfkTimeManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((Set<?>) field.get(null)).clear();
    }

    private void clearLeaderboardCache() throws Exception {
        Field cacheField = AfkTimeManager.class.getDeclaredField("leaderboardCache");
        cacheField.setAccessible(true);
        ((NavigableSet<?>) cacheField.get(null)).clear();
    }

    private void setTimesDirectory(Object value) throws Exception {
        Field field = AfkTimeManager.class.getDeclaredField("timesDirectory");
        field.setAccessible(true);
        field.set(null, value);
    }

    private void invokeRebuild() throws Exception {
        Method rebuild = AfkTimeManager.class.getDeclaredMethod("rebuildLeaderboardCache");
        rebuild.setAccessible(true);
        rebuild.invoke(null);
    }
}
