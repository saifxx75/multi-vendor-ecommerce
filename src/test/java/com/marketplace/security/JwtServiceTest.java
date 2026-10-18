package com.marketplace.security;

import com.marketplace.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-unit-test-secret-key-123456";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000);

    private UserPrincipal user(long id, String email) {
        return new UserPrincipal(id, "Test User", email, "hash", Role.CUSTOMER, true);
    }

    @Test
    @DisplayName("a generated token contains the user's email and validates for that user")
    void generatesAndValidatesToken() {
        UserPrincipal user = user(1L, "alice@example.com");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@example.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("a token is not valid for a different user")
    void rejectsTokenForOtherUser() {
        String token = jwtService.generateToken(user(1L, "alice@example.com"));

        assertThat(jwtService.isTokenValid(token, user(2L, "bob@example.com"))).isFalse();
    }

    @Test
    @DisplayName("an expired token is rejected")
    void rejectsExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, -1_000);
        UserPrincipal user = user(1L, "alice@example.com");

        String token = shortLived.generateToken(user);

        assertThat(shortLived.isTokenValid(token, user)).isFalse();
    }

    @Test
    @DisplayName("a token whose payload was swapped (bad signature) is rejected")
    void rejectsTamperedToken() {
        UserPrincipal alice = user(1L, "alice@example.com");
        UserPrincipal bob = user(2L, "bob@example.com");
        String[] aliceParts = jwtService.generateToken(alice).split("\\.");
        String[] bobParts = jwtService.generateToken(bob).split("\\.");

        // Alice's header + Bob's payload + Alice's signature: signature no longer matches.
        String forged = aliceParts[0] + "." + bobParts[1] + "." + aliceParts[2];

        assertThat(jwtService.isTokenValid(forged, bob)).isFalse();
    }

    @Test
    @DisplayName("garbage input is rejected without throwing")
    void rejectsGarbage() {
        assertThat(jwtService.isTokenValid("not-a-jwt", user(1L, "alice@example.com"))).isFalse();
    }

    @Test
    @DisplayName("a token signed with another secret is rejected")
    void rejectsTokenFromOtherSecret() {
        JwtService other = new JwtService("another-secret-key-another-secret-key-987654", 3_600_000);
        UserPrincipal user = user(1L, "alice@example.com");

        assertThat(jwtService.isTokenValid(other.generateToken(user), user)).isFalse();
    }

    @Test
    @DisplayName("a secret shorter than 32 characters is refused at start-up")
    void rejectsWeakSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 1000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
