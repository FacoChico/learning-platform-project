package com.facochico.edu.repository.course;

import com.facochico.edu.domain.course.CourseReviewEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataCourseReviewRepository extends JpaRepository<CourseReviewEntity, Long> {

    List<CourseReviewEntity> findByCourseIdOrderById(long courseId, Pageable pageable);
}
