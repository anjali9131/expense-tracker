package com.expensetracker.controller;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseSummary;
import com.expensetracker.model.Expense;
import com.expensetracker.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@Tag(name = "Expenses", description = "Manage personal expenses")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @Operation(summary = "Add a new expense")
    @PostMapping
    public ResponseEntity<Expense> addExpense(@Valid @RequestBody ExpenseRequest request) {
        Expense created = service.addExpense(request);
        return ResponseEntity.created(URI.create("/api/expenses/" + created.getId())).body(created);
    }

    @Operation(summary = "List expenses, optionally filtered by category")
    @GetMapping
    public ResponseEntity<List<Expense>> getExpenses(
            @Parameter(description = "Case-insensitive category filter, e.g. Food")
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(service.getExpenses(category));
    }

    @Operation(summary = "Get a single expense by id")
    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getExpenseById(id));
    }

    @Operation(summary = "Overall total and per-category totals")
    @GetMapping("/summary")
    public ResponseEntity<ExpenseSummary> getSummary() {
        return ResponseEntity.ok(service.getSummary());
    }

    @Operation(summary = "Delete an expense by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        service.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
}
