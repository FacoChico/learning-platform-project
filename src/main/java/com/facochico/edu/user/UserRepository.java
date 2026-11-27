package com.facochico.edu.user;

import java.util.Optional;

public interface UserRepository {

    User insert(UserDetails details);

    Optional<User> findById(long id);

    Optional<User> update(long id, UserDetails details);

    boolean deleteById(long id);
}
