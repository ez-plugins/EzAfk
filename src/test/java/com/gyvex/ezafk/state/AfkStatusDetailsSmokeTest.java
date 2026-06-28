package com.gyvex.ezafk.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for {@link AfkStatusDetails}, verifying the record-to-class
 * conversion preserved all construction, accessor, and guard behaviour.
 */
class AfkStatusDetailsSmokeTest {

    @Test
    void stores_reason_and_detail() {
        AfkStatusDetails d = new AfkStatusDetails(AfkReason.MANUAL, "via command");
        assertEquals(AfkReason.MANUAL, d.reason());
        assertEquals("via command", d.detail());
    }

    @Test
    void null_reason_defaults_to_OTHER() {
        AfkStatusDetails d = new AfkStatusDetails(null, "detail");
        assertEquals(AfkReason.OTHER, d.reason());
    }

    @Test
    void null_detail_means_hasDetail_is_false() {
        AfkStatusDetails d = new AfkStatusDetails(AfkReason.INACTIVITY, null);
        assertFalse(d.hasDetail());
        assertNull(d.getDetail());
    }

    @Test
    void blank_detail_means_hasDetail_is_false() {
        AfkStatusDetails d = new AfkStatusDetails(AfkReason.INACTIVITY, "   ");
        assertFalse(d.hasDetail());
    }

    @Test
    void non_empty_detail_means_hasDetail_is_true() {
        AfkStatusDetails d = new AfkStatusDetails(AfkReason.OTHER, "zone1");
        assertTrue(d.hasDetail());
        assertEquals("zone1", d.getDetail());
    }

    @Test
    void getReasonDisplayName_returns_enum_display_name() {
        AfkStatusDetails d = new AfkStatusDetails(AfkReason.MANUAL, null);
        assertEquals(AfkReason.MANUAL.getDisplayName(), d.getReasonDisplayName());
    }

    @Test
    void detail_accessor_and_getDetail_return_same_value() {
        AfkStatusDetails d = new AfkStatusDetails(AfkReason.COMMAND_FORCED, "admin");
        assertEquals(d.detail(), d.getDetail());
    }
}
