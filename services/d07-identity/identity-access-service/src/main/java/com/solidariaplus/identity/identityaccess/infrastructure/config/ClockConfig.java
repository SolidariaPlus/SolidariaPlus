package com.solidariaplus.identity.identityaccess.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
