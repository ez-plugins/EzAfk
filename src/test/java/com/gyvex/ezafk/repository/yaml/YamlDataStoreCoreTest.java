package com.gyvex.ezafk.repository.yaml;

import com.github.ezframework.javaquerybuilder.query.Query;
import com.github.ezframework.javaquerybuilder.query.condition.Condition;
import com.github.ezframework.javaquerybuilder.query.condition.ConditionEntry;
import com.github.ezframework.javaquerybuilder.query.condition.Connector;
import com.github.ezframework.javaquerybuilder.query.condition.Operator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlDataStoreCoreTest {

    @TempDir
    File tempDir;

    @Test
    void datastore_save_load_exists_delete_and_flush_reload_work() throws Exception {
        File file = new File(tempDir, "afk_times.yml");
        YamlDataStore store = new YamlDataStore(file);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("seconds", 42L);

        store.save("afk_times/id-1", data);
        assertTrue(store.exists("afk_times/id-1"));

        Optional<Map<String, Object>> loaded = store.load("afk_times/id-1");
        assertTrue(loaded.isPresent());
        assertEquals(42L, ((Number) loaded.get().get("seconds")).longValue());

        store.flush();

        YamlDataStore reloaded = new YamlDataStore(file);
        Optional<Map<String, Object>> loadedAfterReload = reloaded.load("afk_times/id-1");
        assertTrue(loadedAfterReload.isPresent());
        assertEquals(42L, ((Number) loadedAfterReload.get().get("seconds")).longValue());

        reloaded.delete("afk_times/id-1");
        assertFalse(reloaded.exists("afk_times/id-1"));
    }

    @Test
    void datastore_query_supports_limit_and_and_or_conditions() throws Exception {
        File file = new File(tempDir, "query.yml");
        YamlDataStore store = new YamlDataStore(file);

        Map<String, Object> one = new LinkedHashMap<>();
        one.put("seconds", 10L);
        one.put("status", "afk");

        Map<String, Object> two = new LinkedHashMap<>();
        two.put("seconds", 200L);
        two.put("status", "active");

        Map<String, Object> three = new LinkedHashMap<>();
        three.put("seconds", 300L);
        three.put("status", "afk");

        store.save("afk_times/id-1", one);
        store.save("afk_times/id-2", two);
        store.save("afk_times/id-3", three);

        Query all = new Query();
        all.setLimit(-1);
        List<String> allIds = store.query(all);
        assertEquals(3, allIds.size());

        Query limited = new Query();
        limited.setLimit(2);
        List<String> limitedIds = store.query(limited);
        assertEquals(2, limitedIds.size());

        Query andQuery = new Query();
        andQuery.setLimit(-1);
        andQuery.setConditions(List.of(
                new ConditionEntry("status", new Condition(Operator.EQ, "afk"), Connector.AND),
            new ConditionEntry("status", new Condition(Operator.NEQ, "active"), Connector.AND)
        ));
        List<String> andIds = store.query(andQuery);
        assertEquals(2, andIds.size());
        assertTrue(andIds.contains("id-1"));
        assertTrue(andIds.contains("id-3"));

        Query orQuery = new Query();
        orQuery.setLimit(-1);
        orQuery.setConditions(List.of(
                new ConditionEntry("status", new Condition(Operator.EQ, "active"), Connector.AND),
            new ConditionEntry("status", new Condition(Operator.EQ, "afk"), Connector.OR)
        ));
        List<String> orIds = store.query(orQuery);
        assertEquals(3, orIds.size());
    }
}
