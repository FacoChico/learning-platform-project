package com.facochico.edu.service.user;

import com.facochico.edu.domain.user.User;
import com.facochico.edu.domain.user.UserDetails;
import com.facochico.edu.domain.user.exception.UserNotFoundException;
import com.facochico.edu.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;

    public User create(UserDetails details) {
        return users.insert(details);
    }

    public User findById(long id) {
        return users.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User update(long id, UserDetails details) {
        return users.update(id, details)
            .orElseThrow(() -> new UserNotFoundException(id));
    }

    public void delete(long id) {
        if (!users.deleteById(id)) {
            throw new UserNotFoundException(id);
        }
    }
}
