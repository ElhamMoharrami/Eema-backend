package com.eema.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlywayLocationTest {
    @Test
    void developmentSeedsAreNotInCommonProductionLocation() {
        ClassLoader loader = getClass().getClassLoader();
        assertNull(loader.getResource("db/migration/common/V100__development_seed.sql"));
        assertNotNull(loader.getResource("db/migration/dev/V100__development_seed.sql"));
        assertNotNull(loader.getResource("db/migration/common/V1__initial_schema.sql"));
    }
}
