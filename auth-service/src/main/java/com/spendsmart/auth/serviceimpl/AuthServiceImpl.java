package com.spendsmart.auth.serviceimpl;

import com.spendsmart.auth.config.JwtUtil;
import com.spendsmart.auth.entity.User;
import com.spendsmart.auth.repository.UserRepository;
import com.spendsmart.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

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
