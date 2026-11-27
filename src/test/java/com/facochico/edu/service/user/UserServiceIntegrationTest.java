package com.facochico.edu.service.user;

import com.facochico.edu.domain.user.Profile;
import com.facochico.edu.domain.user.UserDetails;
import com.facochico.edu.domain.user.exception.EmailAlreadyInUseException;
import com.facochico.edu.domain.user.exception.UserNotFoundException;
import com.facochico.edu.testutil.WithPostgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import lombok.RequiredArgsConstructor;

import static com.facochico.edu.domain.user.UserRole.STUDENT;
import static com.facochico.edu.domain.user.UserRole.TEACHER;
import static lombok.AccessLevel.PACKAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@WithPostgres
@RequiredArgsConstructor(access = PACKAGE, onConstructor_ = @Autowired)
class UserServiceIntegrationTest {
    private final UserService users;
    private final JdbcClient jdbc;

    @Test
    void shouldReadStoredProfileWhenUserIsCreatedWithOne() {
        final var profile = new Profile("Учусь на третьем курсе", "https://example.org/grigory.png");
        final var details = new UserDetails("Григорий", "grigory@example.org", STUDENT, profile);
        final var created = users.create(details);

        final var stored = users.findById(created.getId());

        assertThat(stored.getDetails()).isEqualTo(details);
        assertThat(countProfiles(created.getId())).isEqualTo(1);
    }

    @Test
    void shouldPersistUserChangesWhenDetailsAndProfileAreUpdated() {
        final var profile = new Profile("Учусь на втором курсе", null);
        final var details = new UserDetails("Аня", "anya@example.org", STUDENT, profile);
        final var user = users.create(details);
        final var updated = new UserDetails("Аня", "anna@example.org", TEACHER,
            new Profile("Веду практику по базам данных", "https://example.org/anya.png"));

        users.update(user.getId(), updated);

        assertThat(users.findById(user.getId()).getDetails()).isEqualTo(updated);
        assertThat(countProfiles(user.getId())).isEqualTo(1);
    }

    @Test
    void shouldCreateProfileWhenItIsAddedToExistingUser() {
        final var details = new UserDetails("Таня", "tanya@example.org", TEACHER, null);
        final var user = users.create(details);
        final var profile = new Profile("Веду семинары по проектированию ПО", null);

        users.update(user.getId(), new UserDetails(details.getName(), details.getEmail(), details.getRole(), profile));

        assertThat(users.findById(user.getId()).getDetails().getProfile()).isEqualTo(profile);
        assertThat(countProfiles(user.getId())).isEqualTo(1);
    }

    @Test
    void shouldRemoveProfileRowWhenProfileIsCleared() {
        final var profile = new Profile("Веду практику по Java", null);
        final var details = new UserDetails("Анна", "anna.s@example.org", TEACHER, profile);
        final var user = users.create(details);

        users.update(user.getId(), new UserDetails(details.getName(), details.getEmail(), details.getRole(), null));

        assertThat(users.findById(user.getId()).getDetails().getProfile()).isNull();
        assertThat(countProfiles(user.getId())).isZero();
    }

    @Test
    void shouldDeleteProfileWhenUserIsDeleted() {
        final var profile = new Profile("Веду семинары по алгоритмам", null);
        final var details = new UserDetails("Михаил", "mikhail@example.org", TEACHER, profile);
        final var user = users.create(details);

        users.delete(user.getId());

        assertThatExceptionOfType(UserNotFoundException.class).isThrownBy(() -> users.findById(user.getId()));
        assertThat(countProfiles(user.getId())).isZero();
    }

    @Test
    void shouldRejectRegistrationWhenNormalizedEmailAlreadyExists() {
        users.create(new UserDetails("Сергей", "sergey@example.org", TEACHER, null));
        final var duplicate = new UserDetails("Гришаня", " SERGEY@example.org ", STUDENT, null);

        final var failure = catchThrowable(() -> users.create(duplicate));

        assertThat(failure).isInstanceOf(EmailAlreadyInUseException.class);
    }

    @Test
    void shouldKeepOriginalDetailsWhenEmailUpdateConflicts() {
        final var original = new UserDetails("Лена", "lena@example.org", STUDENT, null);
        final var user = users.create(original);
        users.create(new UserDetails("Илья", "ilya@example.org", TEACHER, null));
        final var profile = new Profile("Готовлюсь к зачёту по базам данных", null);
        final var duplicate = new UserDetails(original.getName(), "ilya@example.org", STUDENT, profile);

        final var failure = catchThrowable(() -> users.update(user.getId(), duplicate));

        assertThat(failure).isInstanceOf(EmailAlreadyInUseException.class);
        assertThat(users.findById(user.getId()).getDetails()).isEqualTo(original);
        assertThat(countProfiles(user.getId())).isZero();
    }

    private long countProfiles(long userId) {
        return jdbc.sql("SELECT COUNT(*) FROM profiles WHERE user_id = :userId")
            .param("userId", userId)
            .query(Long.class)
            .single();
    }
}
