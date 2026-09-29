package com.ahorra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled methods (the day-close job and KeepAliveJob). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
