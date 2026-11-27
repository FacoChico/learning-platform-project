package com.facochico.edu.user.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SpringDataUserRepository extends JpaRepository<UserEntity, Long> {

    @EntityGraph(attributePaths = "profile")
    Optional<UserEntity> findWithProfileById(long id);
}
