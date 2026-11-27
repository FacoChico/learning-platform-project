package com.facochico.edu.user;

import com.facochico.edu.testutil.PostgresTestConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import static com.facochico.edu.user.UserRole.STUDENT;
import static com.facochico.edu.user.UserRole.TEACHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class UserServiceIntegrationTest {
    private final UserService users;
    private final JdbcClient jdbc;

    @Autowired
    UserServiceIntegrationTest(UserService users, JdbcClient jdbc) {
        this.users = users;
        this.jdbc = jdbc;
    }

    @Test
    void shouldReadStoredProfileWhenUserIsCreatedWithOne() {
        final var profile = new Profile("Studies relational databases", "https://example.org/ada.png");
        final var details = new UserDetails("Ada Lovelace", "ada@example.org", STUDENT, profile);
        final var created = users.create(details);

        final var stored = users.findById(created.id());

        assertThat(stored.details()).isEqualTo(details);
        assertThat(countProfiles(created.id())).isEqualTo(1);
    }

    @Test
    void shouldPersistUserChangesWhenDetailsAndProfileAreUpdated() {
        final var details = new UserDetails("Grace Hopper", "grace@example.org", STUDENT, new Profile("Student", null));
        final var user = users.create(details);
        final var updated = new UserDetails("Grace Hopper", "grace.hopper@example.org", TEACHER,
                new Profile("Teaches database design", "https://example.org/grace.png"));

        users.update(user.id(), updated);

        assertThat(users.findById(user.id()).details()).isEqualTo(updated);
        assertThat(countProfiles(user.id())).isEqualTo(1);
    }

    @Test
    void shouldCreateProfileWhenItIsAddedToExistingUser() {
        final var details = new UserDetails("Margaret Hamilton", "margaret@example.org", TEACHER, null);
        final var user = users.create(details);
        final var profile = new Profile("Teaches software engineering", null);

        users.update(user.id(), new UserDetails(details.name(), details.email(), details.role(), profile));

        assertThat(users.findById(user.id()).details().profile()).isEqualTo(profile);
        assertThat(countProfiles(user.id())).isEqualTo(1);
    }

    @Test
    void shouldRemoveProfileRowWhenProfileIsCleared() {
        final var profile = new Profile("Teaches Java", null);
        final var details = new UserDetails("Barbara Liskov", "barbara@example.org", TEACHER, profile);
        final var user = users.create(details);

        users.update(user.id(), new UserDetails(details.name(), details.email(), details.role(), null));

        assertThat(users.findById(user.id()).details().profile()).isNull();
        assertThat(countProfiles(user.id())).isZero();
    }

    @Test
    void shouldDeleteProfileWhenUserIsDeleted() {
        final var profile = new Profile("Teaches algorithms", null);
        final var details = new UserDetails("Edsger Dijkstra", "edsger@example.org", TEACHER, profile);
        final var user = users.create(details);

        users.delete(user.id());

        assertThatExceptionOfType(UserNotFoundException.class).isThrownBy(() -> users.findById(user.id()));
        assertThat(countProfiles(user.id())).isZero();
    }

    @Test
    void shouldRejectRegistrationWhenNormalizedEmailAlreadyExists() {
        users.create(new UserDetails("Donald Knuth", "donald@example.org", TEACHER, null));
        final var duplicate = new UserDetails("Another student", " DONALD@example.org ", STUDENT, null);

        final var failure = catchThrowable(() -> users.create(duplicate));

        assertThat(failure).isInstanceOf(EmailAlreadyInUseException.class);
    }

    @Test
    void shouldKeepOriginalDetailsWhenEmailUpdateConflicts() {
        final var original = new UserDetails("Katherine Johnson", "katherine@example.org", STUDENT, null);
        final var user = users.create(original);
        users.create(new UserDetails("John Backus", "john@example.org", TEACHER, null));
        final var duplicate = new UserDetails(original.name(), "john@example.org", STUDENT, new Profile("New bio", null));

        final var failure = catchThrowable(() -> users.update(user.id(), duplicate));

        assertThat(failure).isInstanceOf(EmailAlreadyInUseException.class);
        assertThat(users.findById(user.id()).details()).isEqualTo(original);
        assertThat(countProfiles(user.id())).isZero();
    }

    private long countProfiles(long userId) {
        return jdbc.sql("select count(*) from profiles where user_id = :userId")
                .param("userId", userId)
                .query(Long.class)
                .single();
    }
}
