package com.facochico.edu.repository.course;

import com.facochico.edu.domain.course.TagEntity;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTagRepository extends JpaRepository<TagEntity, Long> {
}
