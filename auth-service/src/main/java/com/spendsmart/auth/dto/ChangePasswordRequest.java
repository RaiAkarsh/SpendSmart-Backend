package com.spendsmart.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Password change request")
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Schema(example = "Password@123")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Schema(example = "NewPassword@123")
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
