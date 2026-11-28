package com.facochico.edu.repository.user;

import com.facochico.edu.domain.user.UserEntity;
import com.facochico.edu.domain.user.UserRole;
import com.facochico.edu.domain.user.dto.UserSummary;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, Long> {

    @EntityGraph(attributePaths = "profile")
    Optional<UserEntity> findWithProfileById(long id);

    List<UserSummary> findAllProjectedByOrderById(Pageable pageable);

    List<UserSummary> findByRoleOrderById(UserRole role, Pageable pageable);
}
