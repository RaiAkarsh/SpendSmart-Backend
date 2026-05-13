package com.spendsmart.auth.serviceimpl;

import com.spendsmart.auth.config.JwtUtil;
import com.spendsmart.auth.entity.User;
import com.spendsmart.auth.repository.UserRepository;
import com.spendsmart.auth.service.AuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;


@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    // register
    @Override
    public User register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered: " + user.getEmail());
        }
        // Hash plain password
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        return userRepository.save(user);
    }

    //login
    @Override
    public String login(String email, String password) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("No account found with this email");
        }

        User user = optionalUser.get();

        if (!user.isActive()) {
            throw new RuntimeException("This account has been deactivated");
        }

        // BCrypt: rehash
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Incorrect password");
        }

        return jwtUtil.generateToken(user.getEmail(), user.getUserId());
    }

    @Override
    public String loginWithGoogle(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new RuntimeException("Google idToken is required");
        }

        GoogleIdToken.Payload payload = verifyGoogleToken(idToken);
        String email = payload.getEmail();

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Google account email is unavailable");
        }

        Object emailVerifiedClaim = payload.get("email_verified");
        if (!(emailVerifiedClaim instanceof Boolean) || !((Boolean) emailVerifiedClaim)) {
            throw new RuntimeException("Google email is not verified");
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        User user;

        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            if (!user.isActive()) {
                throw new RuntimeException("This account has been deactivated");
            }
            if (!"GOOGLE".equalsIgnoreCase(user.getProvider())) {
                throw new RuntimeException("This email is registered as LOCAL login. Use password login.");
            }
        } else {
            user = new User();
            user.setEmail(email);
            Object nameClaim = payload.get("name");
            user.setFullName(nameClaim instanceof String ? (String) nameClaim : "Google User");
            user.setAvatarUrl((String) payload.get("picture"));
            user.setProvider("GOOGLE");
            // Column is non-null; not used for OAuth logins.
            user.setPasswordHash(passwordEncoder.encode("GOOGLE_" + UUID.randomUUID()));
            user = userRepository.save(user);
        }

        return jwtUtil.generateToken(user.getEmail(), user.getUserId());
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Invalid Google ID token");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to verify Google token");
        }
    }

    //getUserById
    @Override
    public User getUserById(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    //getUserByEmail
    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    //updateProfile
    @Override
    public User updateProfile(int userId, User updatedUser) {
        User existing = getUserById(userId);
        existing.setFullName(updatedUser.getFullName());
        existing.setAvatarUrl(updatedUser.getAvatarUrl());
        existing.setTimezone(updatedUser.getTimezone());
        return userRepository.save(existing);
    }

    // changePassword
    @Override
    public void changePassword(int userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    //updateCurrency
    @Override
    public void updateCurrency(int userId, String currency) {
        User user = getUserById(userId);
        user.setCurrency(currency);
        userRepository.save(user);
    }

    //updateMonthlyBudget
    @Override
    public void updateMonthlyBudget(int userId, double monthlyBudget) {
        User user = getUserById(userId);
        user.setMonthlyBudget(monthlyBudget);
        userRepository.save(user);
    }

    //deactivateAccount
    @Override
    public void deactivateAccount(int userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }
}