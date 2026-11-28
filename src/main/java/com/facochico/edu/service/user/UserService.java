package com.facochico.edu.service.user;

import com.facochico.edu.domain.user.User;
import com.facochico.edu.domain.user.UserDetails;
import com.facochico.edu.domain.user.UserRole;
import com.facochico.edu.domain.user.dto.UserSummary;
import com.facochico.edu.domain.user.exception.UserNotFoundException;
import com.facochico.edu.repository.user.UserRepository;
import com.facochico.edu.util.PaginationValidationUtil;

import java.util.List;

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

    public List<UserSummary> listUsers(UserRole role, int page, int size) {
        PaginationValidationUtil.requirePage(page, size);
        return users.listUsers(role, page, size);
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
