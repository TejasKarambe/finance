package com.example.finance.service;

import com.example.finance.dto.response.AiInsightResponse;
import com.example.finance.dto.response.CategoryExpenseResponse;
import com.example.finance.dto.response.MonthlyReportResponse;
import com.example.finance.dto.response.SpendingInsightItem;
import com.example.finance.entity.RecurringTransaction;
import com.example.finance.entity.SavingsGoal;
import com.example.finance.repository.RecurringTransactionRepository;
import com.example.finance.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InsightService {

    private final ReportService reportService;
    private final RecurringTransactionRepository recurringRepository;
    private final SavingsGoalRepository goalRepository;

    public InsightService(
            ReportService reportService,
            RecurringTransactionRepository recurringRepository,
            SavingsGoalRepository goalRepository) {
        this.reportService = reportService;
        this.recurringRepository = recurringRepository;
        this.goalRepository = goalRepository;
    }

    @Transactional(readOnly = true)
    public AiInsightResponse generateSpendingInsights(Long userId) {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        LocalDate lastMonthDate = today.minusMonths(1);
        int prevYear = lastMonthDate.getYear();
        int prevMonth = lastMonthDate.getMonthValue();

        MonthlyReportResponse currentReport = reportService.getMonthlyReport(userId, currentYear, currentMonth);
        MonthlyReportResponse prevReport = reportService.getMonthlyReport(userId, prevYear, prevMonth);

        BigDecimal income = currentReport.totalIncome() != null ? currentReport.totalIncome() : BigDecimal.ZERO;
        BigDecimal expense = currentReport.totalExpense() != null ? currentReport.totalExpense() : BigDecimal.ZERO;
        double savingsRate = currentReport.savingsRate() != null ? currentReport.savingsRate() : 0.0;

        // 1. Calculate Recurring Burden
        List<RecurringTransaction> recurringList = recurringRepository.findByUserIdAndActiveTrueOrderByNextDueDateAsc(userId);
        BigDecimal recurringTotalMonthly = BigDecimal.ZERO;
        for (RecurringTransaction r : recurringList) {
            BigDecimal monthlyEq = switch (r.getFrequency()) {
                case DAILY -> r.getAmount().multiply(BigDecimal.valueOf(30));
                case WEEKLY -> r.getAmount().multiply(BigDecimal.valueOf(4.33));
                case BIWEEKLY -> r.getAmount().multiply(BigDecimal.valueOf(2.16));
                case MONTHLY -> r.getAmount();
                case QUARTERLY -> r.getAmount().divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
                case YEARLY -> r.getAmount().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            };
            recurringTotalMonthly = recurringTotalMonthly.add(monthlyEq);
        }

        double recurringRatio = 0.0;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            recurringRatio = recurringTotalMonthly.multiply(BigDecimal.valueOf(100))
                    .divide(income, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // 2. Identify Top Spending Category
        String topCategory = "None";
        BigDecimal topCategoryAmount = BigDecimal.ZERO;
        Map<String, BigDecimal> currentCatMap = new HashMap<>();
        if (currentReport.categories() != null) {
            for (CategoryExpenseResponse cat : currentReport.categories()) {
                currentCatMap.put(cat.category().toLowerCase(), cat.totalAmount());
                if (cat.totalAmount().compareTo(topCategoryAmount) > 0) {
                    topCategoryAmount = cat.totalAmount();
                    topCategory = cat.category();
                }
            }
        }

        Map<String, BigDecimal> prevCatMap = new HashMap<>();
        if (prevReport.categories() != null) {
            for (CategoryExpenseResponse cat : prevReport.categories()) {
                prevCatMap.put(cat.category().toLowerCase(), cat.totalAmount());
            }
        }

        // 3. Generate Specific Insights
        List<SpendingInsightItem> insightItems = new ArrayList<>();

        // Anomaly / Surge Detection
        for (Map.Entry<String, BigDecimal> entry : currentCatMap.entrySet()) {
            String catName = entry.getKey();
            BigDecimal currentAmt = entry.getValue();
            BigDecimal prevAmt = prevCatMap.getOrDefault(catName, BigDecimal.ZERO);

            if (prevAmt.compareTo(BigDecimal.valueOf(50)) > 0 && currentAmt.compareTo(prevAmt) > 0) {
                BigDecimal diff = currentAmt.subtract(prevAmt);
                double pctIncrease = diff.multiply(BigDecimal.valueOf(100))
                        .divide(prevAmt, 1, RoundingMode.HALF_UP)
                        .doubleValue();

                if (pctIncrease >= 20.0) {
                    String formattedName = Character.toUpperCase(catName.charAt(0)) + catName.substring(1);
                    insightItems.add(new SpendingInsightItem(
                            "surge-" + catName,
                            "ANOMALY_ALERT",
                            "Spending Surge: " + formattedName,
                            String.format("%s increased by %.0f%% (+$%.2f) compared to last month ($%.2f vs $%.2f).",
                                    formattedName, pctIncrease, diff, currentAmt, prevAmt),
                            diff.multiply(BigDecimal.valueOf(0.5)).setScale(2, RoundingMode.HALF_UP),
                            pctIncrease > 40 ? "HIGH" : "MEDIUM",
                            "Review recent transactions in " + formattedName + " to identify unnecessary or duplicate charges."
                    ));
                }
            }
        }

        // Discretionary Savings Opportunity
        if (topCategoryAmount.compareTo(BigDecimal.valueOf(150)) > 0 && !topCategory.equalsIgnoreCase("Housing") && !topCategory.equalsIgnoreCase("Rent")) {
            BigDecimal potentialSavings = topCategoryAmount.multiply(BigDecimal.valueOf(0.15)).setScale(2, RoundingMode.HALF_UP);
            insightItems.add(new SpendingInsightItem(
                    "savings-top-cat",
                    "SAVINGS_OPPORTUNITY",
                    "High Outlay on " + topCategory,
                    String.format("%s makes up your largest expense ($%.2f). Trimming this category by 15%% would save $%.2f each month.",
                            topCategory, topCategoryAmount, potentialSavings),
                    potentialSavings,
                    "HIGH",
                    "Set a category budget limit for " + topCategory + " to automatically track and cap this expenditure."
            ));
        }

        // Subscription & Fixed Overhead Insight
        if (recurringRatio > 35.0) {
            insightItems.add(new SpendingInsightItem(
                    "recurring-high",
                    "SUBSCRIPTION_LOAD",
                    "Heavy Recurring Commitments",
                    String.format("Recurring bills and subscriptions consume %.1f%% of your monthly income ($%.2f/mo). Financial experts recommend keeping fixed costs under 30%%.",
                            recurringRatio, recurringTotalMonthly),
                    recurringTotalMonthly.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP),
                    "HIGH",
                    "Audit your recurring subscriptions (gym, streaming, SaaS) and cancel unutilized services."
            ));
        } else if (!recurringList.isEmpty()) {
            insightItems.add(new SpendingInsightItem(
                    "recurring-balanced",
                    "POSITIVE_TREND",
                    "Controlled Fixed Overhead",
                    String.format("Your fixed commitments account for %.1f%% of income ($%.2f/mo), safely within healthy financial parameters.",
                            recurringRatio, recurringTotalMonthly),
                    BigDecimal.ZERO,
                    "LOW",
                    "Maintain this discipline as your income grows to maximize wealth accumulation."
            ));
        }

        // Savings Goal Acceleration
        List<SavingsGoal> activeGoals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (!activeGoals.isEmpty()) {
            SavingsGoal firstGoal = activeGoals.get(0);
            if (!firstGoal.isCompleted()) {
                BigDecimal remaining = firstGoal.getTargetAmount().subtract(firstGoal.getCurrentAmount());
                insightItems.add(new SpendingInsightItem(
                        "goal-accelerator",
                        "SAVINGS_OPPORTUNITY",
                        "Accelerate: " + firstGoal.getName(),
                        String.format("You have $%.2f remaining to reach your '%s' target. Redirecting 10%% of discretionary spending would fund this goal in record time.",
                                remaining, firstGoal.getName()),
                        BigDecimal.valueOf(100),
                        "MEDIUM",
                        "Deposit surplus cash flow into your '" + firstGoal.getName() + "' savings goal."
                ));
            }
        }

        // 4. Compute Composite Financial Health Score (0 - 100)
        int score = 60; // baseline

        if (savingsRate >= 30.0) score += 25;
        else if (savingsRate >= 20.0) score += 18;
        else if (savingsRate >= 10.0) score += 10;
        else if (savingsRate > 0) score += 5;
        else score -= 20; // negative cash flow penalty

        if (recurringRatio > 0 && recurringRatio <= 25.0) score += 10;
        else if (recurringRatio > 45.0) score -= 15;

        if (!activeGoals.isEmpty()) score += 5;

        score = Math.max(10, Math.min(100, score));

        String grade;
        String narrative;
        if (score >= 90) {
            grade = "A+";
            narrative = "Outstanding financial stability! Your exceptional savings rate and low overhead position you in the top tier of wealth builders.";
        } else if (score >= 80) {
            grade = "A";
            narrative = "Strong financial health. Cash flow is positive with disciplined expense tracking. Continue compounding your savings.";
        } else if (score >= 70) {
            grade = "B";
            narrative = "Good baseline with room for optimization. Trimming top discretionary categories will substantially accelerate your goals.";
        } else if (score >= 60) {
            grade = "C";
            narrative = "Moderate stability. Net cash flow is tight and recurring costs are pressuring monthly margins. Focus on expense reduction.";
        } else {
            grade = "D";
            narrative = "Attention required. Outflows exceed or closely match inflows. Immediate action recommended to eliminate non-essential expenses.";
        }

        return new AiInsightResponse(
                score,
                grade,
                narrative,
                savingsRate,
                topCategory,
                expense,
                recurringTotalMonthly,
                recurringRatio,
                insightItems
        );
    }
}
