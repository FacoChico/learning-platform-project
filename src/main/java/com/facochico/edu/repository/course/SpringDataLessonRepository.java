package com.facochico.edu.repository.course;

import com.facochico.edu.domain.course.LessonEntity;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLessonRepository extends JpaRepository<LessonEntity, Long> {
}
