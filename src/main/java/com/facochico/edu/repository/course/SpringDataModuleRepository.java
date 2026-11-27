package com.facochico.edu.repository.course;

import com.facochico.edu.domain.course.ModuleEntity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface SpringDataModuleRepository extends JpaRepository<ModuleEntity, Long> {

    @EntityGraph(attributePaths = "lessons")
    Optional<ModuleEntity> findWithLessonsById(long id);

    @Query("""
        select m from ModuleEntity m
        left join fetch m.lessons
        where m.course.id = :courseId
        order by m.orderIndex, m.id
        """)
    List<ModuleEntity> findWithLessonsByCourseId(@Param("courseId") long courseId);
}
