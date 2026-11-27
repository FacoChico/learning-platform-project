package com.facochico.edu.user;

import org.junit.jupiter.api.Test;

import static com.facochico.edu.user.UserRole.STUDENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class UserDetailsTest {

    @Test
    void shouldNormalizeNameAndEmailWhenInputContainsPaddingAndUppercase() {
        final var name = "  Ada Lovelace  ";
        final var email = "  ADA@Example.org  ";

        final var details = new UserDetails(name, email, STUDENT, null);

        assertThat(details.name()).isEqualTo("Ada Lovelace");
        assertThat(details.email()).isEqualTo("ada@example.org");
    }

    @Test
    void shouldRejectEmailWhenAddressHasNoDomain() {
        final var email = "grace@";

        final var failure = catchThrowable(() -> new UserDetails("Grace Hopper", email, STUDENT, null));

        assertThat(failure)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User email is invalid");
    }
}
