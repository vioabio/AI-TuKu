package com.vio.aitukuviobe.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务调度配置 — 对应优化文档 8.2 节（数据归档） + 12.7 节（定时任务框架）
 *
 * <p>启用方式：{@code aituku.scheduling.enabled=true} 或激活 archive profile
 * <p>默认关闭，避免开发环境不必要的调度
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "aituku.scheduling.enabled", havingValue = "true")
public class ScheduledTaskConfig {

    /**
     * 显式配置任务调度线程池
     *
     * <p>Spring Boot 默认使用单线程执行所有 @Scheduled 任务。
     * 多任务场景下，前一个任务阻塞会导致后续任务延迟执行。
     * 显式配置线程池避免此问题。
     */
    @Bean("taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("aituku-scheduled-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setErrorHandler(t -> log.error("[定时任务] 未捕获异常", t));

        log.info("[定时任务] 调度线程池已初始化: poolSize=4, threadPrefix=aituku-scheduled-");
        return scheduler;
    }
}
