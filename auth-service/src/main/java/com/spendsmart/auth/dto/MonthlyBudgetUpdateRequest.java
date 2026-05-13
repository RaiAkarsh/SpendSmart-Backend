package com.spendsmart.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "Monthly budget update request")
public class MonthlyBudgetUpdateRequest {

    @Min(value = 0, message = "Monthly budget cannot be negative")
    @Schema(example = "25000")
    private double monthlyBudget;

    public double getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(double monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }
}
