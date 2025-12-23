package com.nested.server.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import lombok.extern.slf4j.Slf4j;

/**
 * Async configuration for non-blocking operations like karma updates,
 * cache invalidation, and other background tasks.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - minimum threads always alive
        executor.setCorePoolSize(4);

        // Max pool size - maximum threads during peak load
        executor.setMaxPoolSize(10);

        // Queue capacity - tasks waiting when all threads busy
        executor.setQueueCapacity(100);

        // Thread naming for debugging
        executor.setThreadNamePrefix("async-");

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            log.error("Async exception in method: {} with message: {}",
                    method.getName(), throwable.getMessage(), throwable);
        };
    }
}
