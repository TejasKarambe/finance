package com.example.finance.service;

import com.example.finance.dto.response.*;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.projection.CategoryExpenseProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final BudgetService budgetService;
    private final RecurringTransactionService recurringService;
    private final SavingsGoalService goalService;

    public ReportService(
            TransactionRepository transactionRepository,
            AccountService accountService,
            BudgetService budgetService,
            RecurringTransactionService recurringService,
            SavingsGoalService goalService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.budgetService = budgetService;
        this.recurringService = recurringService;
        this.goalService = goalService;
    }

    public MonthlyReportResponse getMonthlyReport(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal totalIncome = transactionRepository.getTotalByTypeAndDateRangeAndUserId(
                userId, TransactionType.INCOME, start, end);
        BigDecimal totalExpense = transactionRepository.getTotalByTypeAndDateRangeAndUserId(
                userId, TransactionType.EXPENSE, start, end);

        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

        BigDecimal netAmount = totalIncome.subtract(totalExpense);

        Double savingsRate = 0.0;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netAmount.multiply(BigDecimal.valueOf(100))
                    .divide(totalIncome, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        List<CategoryExpenseResponse> categories = getCategoryExpenses(userId, year, month);

        return new MonthlyReportResponse(
                year,
                month,
                totalIncome,
                totalExpense,
                netAmount,
                savingsRate,
                categories
        );
    }

    public YearlyReportResponse getYearlyReport(Long userId, int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);

        BigDecimal totalIncome = transactionRepository.getTotalByTypeAndDateRangeAndUserId(
                userId, TransactionType.INCOME, startOfYear, endOfYear);
        BigDecimal totalExpense = transactionRepository.getTotalByTypeAndDateRangeAndUserId(
                userId, TransactionType.EXPENSE, startOfYear, endOfYear);

        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

        BigDecimal netAmount = totalIncome.subtract(totalExpense);

        Double savingsRate = 0.0;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netAmount.multiply(BigDecimal.valueOf(100))
                    .divide(totalIncome, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        List<MonthlyFlowItem> monthlyBreakdown = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            LocalDate mStart = ym.atDay(1);
            LocalDate mEnd = ym.atEndOfMonth();

            BigDecimal mIncome = transactionRepository.getTotalByTypeAndDateRangeAndUserId(
                    userId, TransactionType.INCOME, mStart, mEnd);
            BigDecimal mExpense = transactionRepository.getTotalByTypeAndDateRangeAndUserId(
                    userId, TransactionType.EXPENSE, mStart, mEnd);

            if (mIncome == null) mIncome = BigDecimal.ZERO;
            if (mExpense == null) mExpense = BigDecimal.ZERO;

            BigDecimal mNet = mIncome.subtract(mExpense);
            String monthName = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            monthlyBreakdown.add(new MonthlyFlowItem(m, monthName, mIncome, mExpense, mNet));
        }

        List<CategoryExpenseProjection> projections = transactionRepository.getCategoryTotalsByDateRangeAndUserId(
                userId, TransactionType.EXPENSE, startOfYear, endOfYear);

        BigDecimal categorySum = projections.stream()
                .map(CategoryExpenseProjection::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryExpenseResponse> topCategories = new ArrayList<>();
        for (CategoryExpenseProjection p : projections) {
            Double percentage = 0.0;
            if (categorySum.compareTo(BigDecimal.ZERO) > 0) {
                percentage = p.getTotalAmount().multiply(BigDecimal.valueOf(100))
                        .divide(categorySum, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            topCategories.add(new CategoryExpenseResponse(p.getCategory(), p.getTotalAmount(), percentage));
        }

        return new YearlyReportResponse(
                year,
                totalIncome,
                totalExpense,
                netAmount,
                savingsRate,
                monthlyBreakdown,
                topCategories
        );
    }

    public List<CategoryExpenseResponse> getCategoryExpenses(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<CategoryExpenseProjection> projections = transactionRepository.getCategoryTotalsByDateRangeAndUserId(
                userId, TransactionType.EXPENSE, start, end);

        BigDecimal totalSum = projections.stream()
                .map(CategoryExpenseProjection::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryExpenseResponse> result = new ArrayList<>();
        for (CategoryExpenseProjection p : projections) {
            Double percentage = 0.0;
            if (totalSum.compareTo(BigDecimal.ZERO) > 0) {
                percentage = p.getTotalAmount().multiply(BigDecimal.valueOf(100))
                        .divide(totalSum, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            result.add(new CategoryExpenseResponse(p.getCategory(), p.getTotalAmount(), percentage));
        }

        return result;
    }

    public void exportTransactionsCsv(Long userId, Writer writer) throws IOException {
        writer.write("Transaction ID,Date,Type,Category,Amount,Currency,Account,Payment Method,Description\n");

        List<Transaction> transactions = transactionRepository.findByAccountUserId(userId);
        transactions.sort(Comparator.comparing(Transaction::getTransactionDate).reversed());

        for (Transaction tx : transactions) {
            String currency = tx.getAccount() != null && tx.getAccount().getCurrency() != null
                    ? tx.getAccount().getCurrency() : "USD";
            String accountName = tx.getAccount() != null ? tx.getAccount().getName() : "N/A";
            String description = tx.getDescription() != null ? tx.getDescription() : "";
            String paymentMethod = tx.getPaymentMethod() != null ? tx.getPaymentMethod() : "OTHER";

            writer.write(String.format("%d,%s,%s,%s,%.2f,%s,\"%s\",%s,\"%s\"\n",
                    tx.getId(),
                    tx.getTransactionDate(),
                    tx.getType(),
                    escapeCsv(tx.getCategory()),
                    tx.getAmount(),
                    currency,
                    escapeCsv(accountName),
                    paymentMethod,
                    escapeCsv(description)
            ));
        }

        writer.flush();
    }

    private String escapeCsv(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }

    public FullDataBackupResponse getFullDataBackup(Long userId) {
        List<AccountResponse> accounts = accountService.getAccountsByUserId(userId);
        List<TransactionResponse> transactions = transactionRepository.findByAccountUserId(userId)
                .stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDate).reversed())
                .map(tx -> new TransactionResponse(
                        tx.getId(),
                        tx.getAmount(),
                        tx.getType(),
                        tx.getCategory(),
                        tx.getDescription(),
                        tx.getPaymentMethod(),
                        tx.getTransactionDate(),
                        tx.getCreatedAt(),
                        tx.getAccount() != null ? tx.getAccount().getId() : null,
                        tx.getAccount() != null ? tx.getAccount().getName() : "N/A",
                        userId
                ))
                .toList();
        List<BudgetResponse> budgets = budgetService.getBudgets(userId, null, null);
        List<RecurringTransactionResponse> recurring = recurringService.getRecurringTransactions(userId);
        List<SavingsGoalResponse> goals = goalService.getGoals(userId);

        return new FullDataBackupResponse(
                LocalDateTime.now(),
                userId,
                accounts,
                transactions,
                budgets,
                recurring,
                goals
        );
    }
}