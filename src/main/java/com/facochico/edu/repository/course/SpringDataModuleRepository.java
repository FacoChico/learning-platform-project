package com.facochico.edu.repository.course;

import com.facochico.edu.domain.course.ModuleEntity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface SpringDataModuleRepository extends JpaRepository<ModuleEntity, Long> {

    @EntityGraph(attributePaths = "lessons")
    Optional<ModuleEntity> findWithLessonsById(long id);

    @Query("""
        select m from ModuleEntity m
        left join fetch m.lessons
        where m.course.id = :courseId
        order by m.orderIndex, m.id
        """)
    List<ModuleEntity> findWithLessonsByCourseId(@Param("courseId") long courseId);

    @Lock(PESSIMISTIC_WRITE)
    @Query("select module from ModuleEntity module where module.id = :id")
    Optional<ModuleEntity> findForUpdate(@Param("id") long id);
}
