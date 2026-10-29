package com.eema.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlywayLocationTest {
    @Test
    void developmentSeedsAreNotInCommonProductionLocation() {
        ClassLoader loader = getClass().getClassLoader();
        assertNull(loader.getResource("db/migration/common/V8__seed_dev_data.sql"));
        assertNull(loader.getResource("db/migration/common/V11__seed_venues.sql"));
        assertNotNull(loader.getResource("db/migration/dev/V8__seed_dev_data.sql"));
        assertNotNull(loader.getResource("db/migration/dev/V11__seed_venues.sql"));
        assertNotNull(loader.getResource("db/migration/common/V13__payment_refund_reconciliation.sql"));
    }
}
