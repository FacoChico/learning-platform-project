package com.facochico.edu.repository.course;

import com.facochico.edu.domain.enrollment.EnrollmentEntity;
import com.facochico.edu.domain.enrollment.dto.EnrolledCourse;
import com.facochico.edu.domain.enrollment.dto.EnrolledStudent;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface SpringDataEnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {

    Optional<EnrollmentEntity> findByCourseIdAndStudentId(long courseId, long studentId);

    @Modifying
    @Query("delete from EnrollmentEntity e where e.course.id = :courseId and e.student.id = :studentId")
    int deleteEnrollment(@Param("courseId") long courseId, @Param("studentId") long studentId);

    @Query("""
        select new com.facochico.edu.domain.enrollment.dto.EnrolledStudent(e.student.id, e.student.name, e.status)
        from EnrollmentEntity e
        where e.course.id = :courseId
        order by e.student.id
        """)
    List<EnrolledStudent> findStudents(@Param("courseId") long courseId, Pageable pageable);

    @Query("""
        select new com.facochico.edu.domain.enrollment.dto.EnrolledCourse(e.course.id, e.course.title, e.status)
        from EnrollmentEntity e
        where e.student.id = :studentId
        order by e.course.id
        """)
    List<EnrolledCourse> findCourses(@Param("studentId") long studentId, Pageable pageable);
}
