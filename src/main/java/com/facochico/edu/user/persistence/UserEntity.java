package com.facochico.edu.user.persistence;

import com.facochico.edu.user.User;
import com.facochico.edu.user.UserDetails;
import com.facochico.edu.user.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import static com.facochico.edu.user.UserDetails.MAX_EMAIL_LENGTH;
import static com.facochico.edu.user.UserDetails.MAX_NAME_LENGTH;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.SEQUENCE;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = UserEntity.EMAIL_CONSTRAINT, columnNames = "email"))
class UserEntity {
    static final String EMAIL_CONSTRAINT = "uk_users_email";

    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false, length = MAX_EMAIL_LENGTH)
    private String email;

    @Enumerated(STRING)
    @Column(nullable = false)
    private UserRole role;

    @OneToOne(mappedBy = "user", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    private ProfileEntity profile;

    protected UserEntity() {
    }

    UserEntity(UserDetails details) {
        update(details);
    }

    void update(UserDetails details) {
        name = details.name();
        email = details.email();
        role = details.role();

        final var profileDetails = details.profile();
        if (profileDetails == null) {
            profile = null;
        } else if (profile == null) {
            profile = new ProfileEntity(this, profileDetails);
        } else {
            profile.update(profileDetails);
        }
    }

    User toUser() {
        final var profileDetails = profile == null ? null : profile.toProfile();
        return new User(id, new UserDetails(name, email, role, profileDetails));
    }
}
