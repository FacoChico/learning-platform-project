package com.facochico.edu.repository.course;

import com.facochico.edu.domain.course.CategoryEntity;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCategoryRepository extends JpaRepository<CategoryEntity, Long> {
}
