package com.eema.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfiguration {
    @Bean(name = {"applicationTaskExecutor", "notificationExecutor"})
    Executor notificationExecutor(@Value("${app.async.core-pool-size:2}") int core,
                                  @Value("${app.async.max-pool-size:4}") int max,
                                  @Value("${app.async.queue-capacity:100}") int capacity) {
        if (core < 1 || max < core || capacity < 1) throw new IllegalArgumentException("Invalid async executor configuration");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("eema-async-");
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(capacity);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
