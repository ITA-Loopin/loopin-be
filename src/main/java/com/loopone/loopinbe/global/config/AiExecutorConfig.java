package com.loopone.loopinbe.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class AiExecutorConfig {
    @Bean(name = "openAiExecutor")
    public Executor openAiExecutor() {
        return new ThreadPoolExecutor(
                5,
                10,
                30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(50),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
