package com.facochico.edu.config;

import com.facochico.edu.repository.quiz.QuizRepository;
import com.facochico.edu.service.quiz.QuizService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class QuizConfig {

    @Bean
    static QuizService quizService(QuizRepository quizzes, Clock clock) {
        return new QuizService(quizzes, clock);
    }
}
