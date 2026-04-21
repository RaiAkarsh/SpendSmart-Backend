package com.spendsmart.auth.service;

import com.spendsmart.auth.entity.User;

public interface AuthService {

    User register(User user);

    String login(String email, String password);

    User getUserById(int userId);

    User getUserByEmail(String email);

    User updateProfile(int userId, User updatedUser);

    void changePassword(int userId, String currentPassword, String newPassword);

    void updateCurrency(int userId, String currency);

    void updateMonthlyBudget(int userId, double monthlyBudget);

    void deactivateAccount(int userId);
}
