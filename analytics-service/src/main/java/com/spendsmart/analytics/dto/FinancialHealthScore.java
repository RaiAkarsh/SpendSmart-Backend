package com.spendsmart.analytics.dto;

import java.io.Serializable;

public class FinancialHealthScore implements Serializable {

    private static final long serialVersionUID = 1L;

    private int userId;
    private int totalScore;
    private String grade;
    private double savingsRate;
    private int savingsScore;
    private int budgetScore;
    private int consistencyScore;
    private String recommendation;


    public FinancialHealthScore() {}

    public FinancialHealthScore(int userId, double savingsRate,
                                int onTrackBudgets, int totalBudgets,
                                boolean isConsistent) {
        this.userId = userId;
        this.savingsRate = Math.round(savingsRate * 100.0) / 100.0;

        // Savings Score: max 40 points
        this.savingsScore = (int) Math.min(savingsRate, 40);

        // Budget Score: max 40 points
        this.budgetScore = totalBudgets > 0
                ? (int) (((double) onTrackBudgets / totalBudgets) * 40)
                : 40; // no budgets = no penalty

        // Consistency Score: max 20 points
        this.consistencyScore = isConsistent ? 20 : 10;

        // Total Score
        this.totalScore = Math.min(this.savingsScore + this.budgetScore + this.consistencyScore, 100);

        // Grade
        if (totalScore >= 90) { this.grade = "A+"; this.recommendation = "Excellent! Keep up your great financial habits."; }
        else if (totalScore >= 80) { this.grade = "A"; this.recommendation = "Very good! Consider increasing your savings rate slightly."; }
        else if (totalScore >= 70) { this.grade = "B"; this.recommendation = "Good progress! Review your budget categories for optimization."; }
        else if (totalScore >= 60) { this.grade = "C"; this.recommendation = "Fair. Try reducing discretionary spending by 10%."; }
        else if (totalScore >= 50) { this.grade = "D"; this.recommendation = "Needs improvement. Set stricter budget limits and track daily."; }
        else { this.grade = "F"; this.recommendation = "Spending exceeds income. Urgent: review all expenses and create a strict budget."; }
    }


    public int getUserId() { return userId; }
    public int getTotalScore() { return totalScore; }
    public String getGrade() { return grade; }
    public double getSavingsRate() { return savingsRate; }
    public int getSavingsScore() { return savingsScore; }
    public int getBudgetScore() { return budgetScore; }
    public int getConsistencyScore() { return consistencyScore; }
    public String getRecommendation() { return recommendation; }


    public void setUserId(int userId) { this.userId = userId; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    public void setGrade(String grade) { this.grade = grade; }
    public void setSavingsRate(double savingsRate) { this.savingsRate = savingsRate; }
    public void setSavingsScore(int savingsScore) { this.savingsScore = savingsScore; }
    public void setBudgetScore(int budgetScore) { this.budgetScore = budgetScore; }
    public void setConsistencyScore(int consistencyScore) { this.consistencyScore = consistencyScore; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    @Override
    public String toString() {
        return "FinancialHealthScore{userId=" + userId + ", score=" + totalScore +
               ", grade='" + grade + "'}";
    }
}
