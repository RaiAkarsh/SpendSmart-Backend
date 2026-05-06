package com.spendsmart.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "Category budget limit update payload")
public class BudgetLimitRequest {

    @Min(value = 0, message = "Budget limit cannot be negative")
    @Schema(example = "5000")
    private double budgetLimit;

    public double getBudgetLimit() {
        return budgetLimit;
    }

    public void setBudgetLimit(double budgetLimit) {
        this.budgetLimit = budgetLimit;
    }
}
