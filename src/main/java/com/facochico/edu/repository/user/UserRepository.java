package com.facochico.edu.repository.user;

import com.facochico.edu.domain.user.User;
import com.facochico.edu.domain.user.UserDetails;

import java.util.Optional;

public interface UserRepository {

    User insert(UserDetails details);

    Optional<User> findById(long id);

    Optional<User> update(long id, UserDetails details);

    boolean deleteById(long id);
}
