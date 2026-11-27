package com.facochico.edu.domain.user;

import org.junit.jupiter.api.Test;

import static com.facochico.edu.domain.user.UserRole.STUDENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class UserDetailsTest {

    @Test
    void shouldNormalizeNameAndEmailWhenInputContainsPaddingAndUppercase() {
        final var name = "  Гришаня  ";
        final var email = "  GRISHA@Example.org  ";

        final var details = new UserDetails(name, email, STUDENT, null);

        assertThat(details.getName()).isEqualTo("Гришаня");
        assertThat(details.getEmail()).isEqualTo("grisha@example.org");
    }

    @Test
    void shouldRejectEmailWhenAddressHasNoDomain() {
        final var email = "misha@";

        final var failure = catchThrowable(() -> new UserDetails("Мишаня", email, STUDENT, null));

        assertThat(failure)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User email is invalid");
    }
}
