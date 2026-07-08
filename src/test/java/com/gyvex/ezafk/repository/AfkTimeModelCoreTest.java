package com.gyvex.ezafk.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AfkTimeModelCoreTest {

    @Test
    void model_exposes_prefix_factory_and_seconds_accessors() {
        AfkTimeModel model = new AfkTimeModel("player-id");

        assertEquals("afk_times", AfkTimeModel.TABLE_PREFIX);
        assertNotNull(AfkTimeModel.FACTORY);
        assertNotNull(AfkTimeModel.factory());

        assertEquals(0L, model.getSeconds());
        model.setSeconds(123L);
        assertEquals(123L, model.getSeconds());
    }
}
