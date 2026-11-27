package com.facochico.edu.repository.course;

import com.facochico.edu.domain.course.CourseEntity;
import com.facochico.edu.domain.course.dto.CourseSummary;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface SpringDataCourseRepository extends JpaRepository<CourseEntity, Long> {

    @EntityGraph(attributePaths = {"category", "tags"})
    Optional<CourseEntity> findWithCatalogById(long id);

    @Query("""
        select new com.facochico.edu.domain.course.dto.CourseSummary(c.id, c.title, c.category.id, c.teacher.id)
        from CourseEntity c
        order by c.id
        """)
    List<CourseSummary> findSummaries(Pageable pageable);
}
