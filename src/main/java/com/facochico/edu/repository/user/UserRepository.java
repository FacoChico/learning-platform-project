package com.facochico.edu.repository.user;

import com.facochico.edu.domain.user.User;
import com.facochico.edu.domain.user.UserDetails;
import com.facochico.edu.domain.user.UserRole;
import com.facochico.edu.domain.user.dto.UserSummary;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User insert(UserDetails details);

    Optional<User> findById(long id);

    List<UserSummary> listUsers(UserRole role, int page, int size);

    Optional<User> update(long id, UserDetails details);

    boolean deleteById(long id);
}
