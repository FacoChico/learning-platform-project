package com.facochico.edu.user.persistence;

import com.facochico.edu.user.EmailAlreadyInUseException;
import com.facochico.edu.user.User;
import com.facochico.edu.user.UserDetails;
import com.facochico.edu.user.UserRepository;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.facochico.edu.user.persistence.UserEntity.EMAIL_CONSTRAINT;

@Repository
@Transactional
class JpaUserRepository implements UserRepository {
    private final SpringDataUserRepository users;

    JpaUserRepository(SpringDataUserRepository users) {
        this.users = users;
    }

    @Override
    public User insert(UserDetails details) {
        final var user = users.save(new UserEntity(details));
        return flushAndMap(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(long id) {
        return users.findWithProfileById(id)
                .map(UserEntity::toUser);
    }

    @Override
    public Optional<User> update(long id, UserDetails details) {
        return users.findWithProfileById(id)
                .map(user -> {
                    user.update(details);
                    return flushAndMap(user);
                });
    }

    @Override
    public boolean deleteById(long id) {
        final var user = users.findWithProfileById(id);
        if (user.isEmpty()) {
            return false;
        }

        users.delete(user.get());
        return true;
    }

    private User flushAndMap(UserEntity user) {
        try {
            users.flush();
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException violation
                    && EMAIL_CONSTRAINT.equals(violation.getConstraintName())) {
                throw new EmailAlreadyInUseException(exception);
            }
            throw exception;
        }

        return user.toUser();
    }
}
