package com.facochico.edu.config;

import com.facochico.edu.repository.course.CourseRepository;
import com.facochico.edu.repository.course.CourseReviewRepository;
import com.facochico.edu.repository.course.EnrollmentRepository;
import com.facochico.edu.service.course.CourseReviewService;
import com.facochico.edu.service.course.CourseService;
import com.facochico.edu.service.enrollment.EnrollmentService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class CourseConfig {

    @Bean
    static CourseService courseService(CourseRepository courses) {
        return new CourseService(courses);
    }

    @Bean
    static EnrollmentService enrollmentService(EnrollmentRepository enrollments) {
        return new EnrollmentService(enrollments, Clock.systemUTC());
    }

    @Bean
    static CourseReviewService courseReviewService(CourseReviewRepository reviews) {
        return new CourseReviewService(reviews, Clock.systemUTC());
    }
}
