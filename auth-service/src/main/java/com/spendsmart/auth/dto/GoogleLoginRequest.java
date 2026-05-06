package com.spendsmart.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Google OAuth login request")
public class GoogleLoginRequest {

    @NotBlank(message = "idToken is required")
    @Schema(example = "eyJhbGciOiJSUzI1NiIsImtpZCI6Ij...")
    private String idToken;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
