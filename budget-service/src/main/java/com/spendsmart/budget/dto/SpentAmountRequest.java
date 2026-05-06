package com.spendsmart.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "Exact spent amount payload")
public class SpentAmountRequest {

    @Min(value = 0, message = "Spent amount cannot be negative")
    @Schema(example = "3200")
    private double spentAmount;

    public double getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(double spentAmount) {
        this.spentAmount = spentAmount;
    }
}
