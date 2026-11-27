package com.facochico.edu.repository.homework;

import com.facochico.edu.domain.homework.SubmissionEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface SpringDataSubmissionRepository extends JpaRepository<SubmissionEntity, Long> {

    List<SubmissionEntity> findByAssignmentIdOrderById(long assignmentId, Pageable pageable);

    List<SubmissionEntity> findByStudentIdOrderById(long studentId, Pageable pageable);

    @Query("select s.assignment.id from SubmissionEntity s where s.id = :id")
    Optional<Long> findAssignmentId(@Param("id") long id);

    @Query("select max(s.score) from SubmissionEntity s where s.assignment.id = :assignmentId")
    Optional<Integer> findHighestScore(@Param("assignmentId") long assignmentId);
}
