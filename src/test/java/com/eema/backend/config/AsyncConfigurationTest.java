package com.eema.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigurationTest {
    @Test void createsBoundedNamedExecutor() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfiguration().notificationExecutor(2, 4, 25);
        assertThat(executor.getCorePoolSize()).isEqualTo(2);
        assertThat(executor.getMaxPoolSize()).isEqualTo(4);
        assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(25);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("eema-async-");
        executor.shutdown();
    }
}
