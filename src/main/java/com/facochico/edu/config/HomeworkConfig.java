package com.facochico.edu.config;

import com.facochico.edu.repository.homework.HomeworkRepository;
import com.facochico.edu.service.homework.HomeworkService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class HomeworkConfig {

    @Bean
    static Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    static HomeworkService homeworkService(HomeworkRepository homework, Clock clock) {
        return new HomeworkService(homework, clock);
    }
}
