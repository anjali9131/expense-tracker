package com.expensetracker.service;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseSummary;
import com.expensetracker.exception.ExpenseNotFoundException;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    public Expense addExpense(ExpenseRequest request) {
        Expense expense = new Expense(
                null,
                request.getTitle().trim(),
                request.getAmount(),
                request.getCategory().trim(),
                request.getDate()
        );
        return repository.save(expense);
    }

    /**
     * Returns all expenses, optionally filtered by category (case-insensitive).
     * Results are sorted by date, most recent first.
     */
    public List<Expense> getExpenses(String categoryFilter) {
        return repository.findAll().stream()
                .filter(e -> categoryFilter == null || categoryFilter.isBlank()
                        || e.getCategory().equalsIgnoreCase(categoryFilter.trim()))
                .sorted(Comparator.comparing(Expense::getDate).reversed())
                .collect(Collectors.toList());
    }

    public Expense getExpenseById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    public void deleteExpense(Long id) {
        boolean deleted = repository.deleteById(id);
        if (!deleted) {
            throw new ExpenseNotFoundException(id);
        }
    }

    /**
     * Overall total plus a breakdown per category. Category keys are sorted
     * alphabetically for a stable, predictable response.
     */
    public ExpenseSummary getSummary() {
        List<Expense> all = repository.findAll();

        BigDecimal total = all.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byCategory = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Expense expense : all) {
            byCategory.merge(expense.getCategory(), expense.getAmount(), BigDecimal::add);
        }

        return new ExpenseSummary(total, byCategory);
    }
}
