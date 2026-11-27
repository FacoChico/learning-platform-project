package com.facochico.edu.domain.user;

import com.facochico.edu.util.user.UserValidationUtil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.NoArgsConstructor;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = UserEntity.EMAIL_CONSTRAINT, columnNames = "email"))
@NoArgsConstructor(access = PROTECTED)
public class UserEntity {
    public static final String EMAIL_CONSTRAINT = "uk_users_email";

    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @Column(nullable = false, length = UserValidationUtil.MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false, length = UserValidationUtil.MAX_EMAIL_LENGTH)
    private String email;

    @Enumerated(STRING)
    @Column(nullable = false)
    private UserRole role;

    @OneToOne(mappedBy = "user", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    private ProfileEntity profile;

    public UserEntity(UserDetails details) {
        update(details);
    }

    public void update(UserDetails details) {
        name = details.getName();
        email = details.getEmail();
        role = details.getRole();

        final var profileDetails = details.getProfile();
        if (profileDetails == null) {
            profile = null;
        } else if (profile == null) {
            profile = new ProfileEntity(this, profileDetails);
        } else {
            profile.update(profileDetails);
        }
    }

    public User toUser() {
        final var profileDetails = profile == null
            ? null
            : profile.toProfile();
        return new User(id, new UserDetails(name, email, role, profileDetails));
    }
}
