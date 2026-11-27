package com.facochico.edu.user;

public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

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
