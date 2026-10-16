package com.example.finance.service;

import com.example.finance.dto.response.CategoryBreakdownResponse;
import com.example.finance.dto.response.DashboardSummaryResponse;
import com.example.finance.dto.response.MonthlyTrendResponse;
import com.example.finance.entity.TransactionType;
import com.example.finance.repository.AccountRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.projection.CategoryExpenseProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public DashboardService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public DashboardSummaryResponse getSummary(Long userId, Integer month, Integer year) {
        BigDecimal totalIncome;
        BigDecimal totalExpenses;

        if (month != null && year != null && month >= 1 && month <= 12 && year > 1900) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            totalIncome = transactionRepository.getTotalByTypeAndDateRangeAndUserId(userId, TransactionType.INCOME, start, end);
            totalExpenses = transactionRepository.getTotalByTypeAndDateRangeAndUserId(userId, TransactionType.EXPENSE, start, end);
        } else {
            totalIncome = transactionRepository.getTotalByTypeAndUserId(userId, TransactionType.INCOME);
            totalExpenses = transactionRepository.getTotalByTypeAndUserId(userId, TransactionType.EXPENSE);
        }

        BigDecimal netCashFlow = totalIncome.subtract(totalExpenses);
        BigDecimal totalBalance = accountRepository.getTotalBalanceByUserId(userId);
        int accountsCount = (int) accountRepository.countByUserId(userId);
        long transactionsCount = transactionRepository.countByAccountUserId(userId);

        return new DashboardSummaryResponse(
                totalIncome,
                totalExpenses,
                netCashFlow,
                totalBalance,
                accountsCount,
                transactionsCount,
                month,
                year
        );
    }

    public List<CategoryBreakdownResponse> getCategoryBreakdown(Long userId, Integer month, Integer year) {
        List<CategoryExpenseProjection> projections;
        if (month != null && year != null && month >= 1 && month <= 12 && year > 1900) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            projections = transactionRepository.getCategoryTotalsByDateRangeAndUserId(userId, TransactionType.EXPENSE, start, end);
        } else {
            projections = transactionRepository.getCategoryTotalsByAllTimeAndUserId(userId, TransactionType.EXPENSE);
        }

        BigDecimal totalSum = projections.stream()
                .map(CategoryExpenseProjection::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryBreakdownResponse> result = new ArrayList<>();
        for (CategoryExpenseProjection p : projections) {
            Double percentage = 0.0;
            if (totalSum.compareTo(BigDecimal.ZERO) > 0) {
                percentage = p.getTotalAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalSum, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            result.add(new CategoryBreakdownResponse(p.getCategory(), p.getTotalAmount(), percentage));
        }

        return result;
    }

    public List<MonthlyTrendResponse> getMonthlyTrend(Long userId, int pastMonths) {
        if (pastMonths <= 0 || pastMonths > 24) {
            pastMonths = 6;
        }

        YearMonth currentYm = YearMonth.now();
        List<MonthlyTrendResponse> trend = new ArrayList<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = pastMonths - 1; i >= 0; i--) {
            YearMonth targetYm = currentYm.minusMonths(i);
            LocalDate start = targetYm.atDay(1);
            LocalDate end = targetYm.atEndOfMonth();

            BigDecimal income = transactionRepository.getTotalByTypeAndDateRangeAndUserId(
                    userId, TransactionType.INCOME, start, end);
            BigDecimal expense = transactionRepository.getTotalByTypeAndDateRangeAndUserId(
                    userId, TransactionType.EXPENSE, start, end);
            BigDecimal net = income.subtract(expense);

            trend.add(new MonthlyTrendResponse(
                    targetYm.toString(),
                    targetYm.format(labelFormatter),
                    income,
                    expense,
                    net
            ));
        }

        return trend;
    }
}
