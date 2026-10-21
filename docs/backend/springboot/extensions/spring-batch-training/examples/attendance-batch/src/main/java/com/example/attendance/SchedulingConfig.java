package com.example.attendance;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables scheduling infrastructure; task beans themselves remain opt-in. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
