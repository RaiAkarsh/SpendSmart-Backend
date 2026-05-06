package com.spendsmart.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Preferred currency update request")
public class CurrencyUpdateRequest {

    @NotBlank(message = "Currency is required")
    @Schema(example = "INR", allowableValues = {"INR", "USD", "EUR", "GBP"})
    private String currency;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
