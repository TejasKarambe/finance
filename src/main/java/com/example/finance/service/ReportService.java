package com.example.finance.service;

import com.example.finance.dto.response.CategoryExpenseResponse;
import com.example.finance.dto.response.MonthlyReportResponse;
import com.example.finance.entity.TransactionType;
import com.example.finance.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(
            TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public MonthlyReportResponse getMonthlyReport(
            int year,
            int month) {

        BigDecimal totalIncome = transactionRepository.getTotalByTypeAndMonth(
                TransactionType.INCOME,
                year,
                month);

        BigDecimal totalExpense = transactionRepository.getTotalByTypeAndMonth(
                TransactionType.EXPENSE,
                year,
                month);

        BigDecimal netAmount = totalIncome.subtract(totalExpense);

        return new MonthlyReportResponse(
                year,
                month,
                totalIncome,
                totalExpense,
                netAmount);
    }

    public List<CategoryExpenseResponse> getCategoryExpenses(
            int year,
            int month) {

        return transactionRepository
                .getCategoryTotals(
                        TransactionType.EXPENSE,
                        year,
                        month)
                .stream()
                .map(item -> new CategoryExpenseResponse(
                        item.getCategory(),
                        item.getTotalAmount()))
                .toList();
    }
}