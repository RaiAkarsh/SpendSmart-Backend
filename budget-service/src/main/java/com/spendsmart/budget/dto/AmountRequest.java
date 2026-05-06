package com.spendsmart.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "Amount payload used to update budget spending")
public class AmountRequest {

    @Min(value = 0, message = "Amount cannot be negative")
    @Schema(example = "1500")
    private double amount;

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
