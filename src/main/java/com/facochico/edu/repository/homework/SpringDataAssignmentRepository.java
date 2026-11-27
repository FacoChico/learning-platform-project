package com.facochico.edu.repository.homework;

import com.facochico.edu.domain.homework.AssignmentEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

interface SpringDataAssignmentRepository extends JpaRepository<AssignmentEntity, Long> {

    List<AssignmentEntity> findByLessonIdOrderById(long lessonId, Pageable pageable);

    // Deadline and score-limit updates must use the same lock as submissions and grading.
    @Lock(PESSIMISTIC_WRITE)
    @Query("select a from AssignmentEntity a where a.id = :id")
    Optional<AssignmentEntity> findForUpdate(@Param("id") long id);
}
