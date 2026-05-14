package com.spendsmart.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.spendsmart.auth.config.JwtUtil;
import com.spendsmart.auth.entity.User;
import com.spendsmart.auth.repository.UserRepository;
import com.spendsmart.auth.serviceimpl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl.loginWithGoogle Unit Tests")
class AuthServiceGoogleLoginTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    /**
     * Test subclass that bypasses the real Google verifier and returns a
     * scriptable payload, so we can cover all branches of loginWithGoogle.
     */
    static class TestableAuthService extends AuthServiceImpl {
        GoogleIdToken.Payload payloadToReturn;
        RuntimeException toThrow;

        @Override
        protected GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
            if (toThrow != null) throw toThrow;
            return payloadToReturn;
        }
    }

    private TestableAuthService service;

    @BeforeEach
    void setUp() {
        service = new TestableAuthService();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(service, "googleClientId", "test-client-id");
    }

    private GoogleIdToken.Payload payloadFor(String email, boolean emailVerified, String name, String picture) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        if (email != null) payload.setEmail(email);
        payload.set("email_verified", emailVerified);
        if (name != null) payload.set("name", name);
        if (picture != null) payload.set("picture", picture);
        return payload;
    }

    @Test
    @DisplayName("loginWithGoogle: should throw when token is null")
    void shouldThrow_whenTokenNull() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.loginWithGoogle(null));
        assertTrue(ex.getMessage().contains("idToken is required"));
    }

    @Test
    @DisplayName("loginWithGoogle: should throw when token is blank")
    void shouldThrow_whenTokenBlank() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.loginWithGoogle("   "));
        assertTrue(ex.getMessage().contains("idToken is required"));
    }

    @Test
    @DisplayName("loginWithGoogle: should throw when email missing from payload")
    void shouldThrow_whenEmailMissing() {
        service.payloadToReturn = payloadFor(null, true, "X", null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.loginWithGoogle("abc"));
        assertTrue(ex.getMessage().contains("email is unavailable"));
    }

    @Test
    @DisplayName("loginWithGoogle: should throw when email_verified is false")
    void shouldThrow_whenEmailNotVerified() {
        service.payloadToReturn = payloadFor("a@b.com", false, "X", null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.loginWithGoogle("abc"));
        assertTrue(ex.getMessage().contains("not verified"));
    }

    @Test
    @DisplayName("loginWithGoogle: should return token for existing active GOOGLE user")
    void shouldReturnToken_forExistingGoogleUser() {
        User existing = new User();
        existing.setUserId(7);
        existing.setEmail("a@b.com");
        existing.setActive(true);
        existing.setProvider("GOOGLE");
        service.payloadToReturn = payloadFor("a@b.com", true, "Name", null);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));
        when(jwtUtil.generateToken("a@b.com", 7)).thenReturn("jwt-xyz");

        String token = service.loginWithGoogle("token");

        assertEquals("jwt-xyz", token);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("loginWithGoogle: should throw when existing user is deactivated")
    void shouldThrow_whenExistingUserDeactivated() {
        User existing = new User();
        existing.setEmail("a@b.com");
        existing.setActive(false);
        existing.setProvider("GOOGLE");
        service.payloadToReturn = payloadFor("a@b.com", true, "Name", null);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.loginWithGoogle("token"));
        assertTrue(ex.getMessage().contains("deactivated"));
    }

    @Test
    @DisplayName("loginWithGoogle: should throw when existing user is LOCAL provider")
    void shouldThrow_whenExistingUserIsLocal() {
        User existing = new User();
        existing.setEmail("a@b.com");
        existing.setActive(true);
        existing.setProvider("LOCAL");
        service.payloadToReturn = payloadFor("a@b.com", true, "Name", null);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.loginWithGoogle("token"));
        assertTrue(ex.getMessage().contains("LOCAL"));
    }

    @Test
    @DisplayName("loginWithGoogle: should auto-register a new GOOGLE user")
    void shouldRegister_whenUserNotFound() {
        service.payloadToReturn = payloadFor("new@b.com", true, "New Person", "http://pic");
        when(userRepository.findByEmail("new@b.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(42);
            return u;
        });
        when(jwtUtil.generateToken(eq("new@b.com"), anyInt())).thenReturn("jwt-new");

        String token = service.loginWithGoogle("token");

        assertEquals("jwt-new", token);
        verify(userRepository).save(argThat(u ->
                "GOOGLE".equals(u.getProvider()) &&
                "New Person".equals(u.getFullName()) &&
                "http://pic".equals(u.getAvatarUrl())));
    }

    @Test
    @DisplayName("loginWithGoogle: should default name when payload has no name claim")
    void shouldDefaultName_whenNameMissing() {
        service.payloadToReturn = payloadFor("new@b.com", true, null, null);
        when(userRepository.findByEmail("new@b.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(43);
            return u;
        });
        when(jwtUtil.generateToken(anyString(), anyInt())).thenReturn("tok");

        service.loginWithGoogle("token");

        verify(userRepository).save(argThat(u -> "Google User".equals(u.getFullName())));
    }

    @Test
    @DisplayName("loginWithGoogle: should propagate verifier exception")
    void shouldPropagate_whenVerifierFails() {
        service.toThrow = new RuntimeException("Invalid Google ID token");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.loginWithGoogle("token"));
        assertTrue(ex.getMessage().contains("Invalid Google ID token"));
    }
}
