package com.paddle.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class TimeConfig {

    @Bean
    fun clock(): Clock {
        // This tells Spring to use the real system clock in production
        return Clock.systemDefaultZone()
    }
}