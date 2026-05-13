package com.spendsmart.auth;

import com.spendsmart.auth.config.JwtUtil;
import com.spendsmart.auth.entity.User;
import com.spendsmart.auth.repository.UserRepository;
import com.spendsmart.auth.serviceimpl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create a standard test user used across multiple tests
        testUser = new User();
        testUser.setUserId(1);
        testUser.setFullName("Rahul Sharma");
        testUser.setEmail("rahul@test.com");
        testUser.setPasswordHash("hashedPassword123");
        testUser.setActive(true);
        testUser.setCurrency("INR");
    }


    @Test
    @DisplayName("register: should save and return user when email is new")
    void register_shouldSaveUser_whenEmailIsNew() {
        // ARRANGE
        when(userRepository.existsByEmail("rahul@test.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User input = new User();
        input.setEmail("rahul@test.com");
        input.setPasswordHash("plainPassword");
        input.setFullName("Rahul Sharma");

        // ACT
        User result = authService.register(input);

        // ASSERT
        assertNotNull(result);
        assertEquals("Rahul Sharma", result.getFullName());
        // Verify password was hashed before saving
        verify(passwordEncoder, times(1)).encode("plainPassword");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("register: should throw RuntimeException when email already exists")
    void register_shouldThrowException_whenEmailAlreadyExists() {
        // ARRANGE
        when(userRepository.existsByEmail("rahul@test.com")).thenReturn(true);

        User input = new User();
        input.setEmail("rahul@test.com");
        input.setPasswordHash("anyPassword");

        // ACT & ASSERT
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(input));
        assertTrue(ex.getMessage().contains("Email already registered"));
        // Verify save was NEVER called
        verify(userRepository, never()).save(any());
    }


    @Test
    @DisplayName("login: should return JWT token on valid credentials")
    void login_shouldReturnToken_whenCredentialsAreValid() {
        // ARRANGE
        when(userRepository.findByEmail("rahul@test.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("plainPassword", "hashedPassword123")).thenReturn(true);
        when(jwtUtil.generateToken("rahul@test.com", 1)).thenReturn("jwt.token.here");

        // ACT
        String token = authService.login("rahul@test.com", "plainPassword");

        // ASSERT
        assertEquals("jwt.token.here", token);
        verify(jwtUtil, times(1)).generateToken("rahul@test.com", 1);
    }

    @Test
    @DisplayName("login: should throw when email not found")
    void login_shouldThrow_whenEmailNotFound() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login("nobody@test.com", "anyPass"));
        assertTrue(ex.getMessage().contains("No account found"));
    }

    @Test
    @DisplayName("login: should throw when password is wrong")
    void login_shouldThrow_whenPasswordIsIncorrect() {
        when(userRepository.findByEmail("rahul@test.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword123")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login("rahul@test.com", "wrongPassword"));
        assertTrue(ex.getMessage().contains("Incorrect password"));
    }

    @Test
    @DisplayName("login: should throw when account is deactivated")
    void login_shouldThrow_whenAccountIsDeactivated() {
        testUser.setActive(false); // deactivate the account
        when(userRepository.findByEmail("rahul@test.com")).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login("rahul@test.com", "anyPass"));
        assertTrue(ex.getMessage().contains("deactivated"));
        // Password check should never happen for deactivated accounts
        verify(passwordEncoder, never()).matches(any(), any());
    }


    @Test
    @DisplayName("getUserById: should return user when found")
    void getUserById_shouldReturnUser_whenFound() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        User result = authService.getUserById(1);
        assertEquals(1, result.getUserId());
        assertEquals("Rahul Sharma", result.getFullName());
    }

    @Test
    @DisplayName("getUserById: should throw when user not found")
    void getUserById_shouldThrow_whenNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.getUserById(999));
        assertTrue(ex.getMessage().contains("not found"));
    }


    @Test
    @DisplayName("updateProfile: should update name and avatar only")
    void updateProfile_shouldUpdateOnlyEditableFields() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User updates = new User();
        updates.setFullName("Rahul Kumar Sharma");
        updates.setAvatarUrl("https://example.com/pic.jpg");
        updates.setTimezone("Asia/Kolkata");

        User result = authService.updateProfile(1, updates);

        verify(userRepository, times(1)).save(argThat(u ->
                u.getFullName().equals("Rahul Kumar Sharma")));
    }


    @Test
    @DisplayName("changePassword: should update hash when current password matches")
    void changePassword_shouldUpdate_whenCurrentPasswordMatches() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPass", "hashedPassword123")).thenReturn(true);
        when(passwordEncoder.encode("newPass456")).thenReturn("newHashedPass");
        when(userRepository.save(any())).thenReturn(testUser);

        assertDoesNotThrow(() -> authService.changePassword(1, "currentPass", "newPass456"));
        verify(passwordEncoder, times(1)).encode("newPass456");
    }

    @Test
    @DisplayName("changePassword: should throw when current password is wrong")
    void changePassword_shouldThrow_whenCurrentPasswordIsWrong() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongCurrent", "hashedPassword123")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.changePassword(1, "wrongCurrent", "newPass"));
        assertTrue(ex.getMessage().contains("incorrect"));
        verify(passwordEncoder, never()).encode(any());
    }


    @Test
    @DisplayName("deactivateAccount: should set isActive to false (soft delete)")
    void deactivateAccount_shouldSetIsActiveFalse() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        authService.deactivateAccount(1);

        // Verify save was called with isActive=false
        verify(userRepository).save(argThat(u -> !u.isActive()));
    }


    @Test
    @DisplayName("updateCurrency: should update currency field")
    void updateCurrency_shouldUpdateCurrencyField() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        assertDoesNotThrow(() -> authService.updateCurrency(1, "USD"));
        verify(userRepository).save(argThat(u -> "USD".equals(u.getCurrency())));
    }
}