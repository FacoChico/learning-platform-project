package com.facochico.edu.config;

import com.facochico.edu.repository.course.CourseRepository;
import com.facochico.edu.service.course.CourseService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class CourseConfig {

    @Bean
    static CourseService courseService(CourseRepository courses) {
        return new CourseService(courses);
    }
}
