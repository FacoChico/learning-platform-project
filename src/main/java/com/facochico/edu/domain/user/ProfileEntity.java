package com.facochico.edu.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "profiles")
@NoArgsConstructor(access = PROTECTED)
class ProfileEntity {
    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @OneToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(columnDefinition = "text")
    private String avatarUrl;

    ProfileEntity(UserEntity user, Profile profile) {
        this.user = user;
        update(profile);
    }

    void update(Profile profile) {
        bio = profile.getBio();
        avatarUrl = profile.getAvatarUrl();
    }

    Profile toProfile() {
        return new Profile(bio, avatarUrl);
    }
}
