package com.expensetracker;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseSummary;
import com.expensetracker.exception.ExpenseNotFoundException;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for {@link ExpenseService}, isolated from Spring and from
 * disk I/O via a minimal in-memory fake repository.
 */
class ExpenseServiceTest {

    /** Simple in-memory stand-in for the JSON-file repository, used only in tests. */
    private static class FakeExpenseRepository implements ExpenseRepository {
        private final Map<Long, Expense> data = new LinkedHashMap<>();
        private final AtomicLong ids = new AtomicLong(0);

        @Override
        public Expense save(Expense expense) {
            if (expense.getId() == null) {
                expense.setId(ids.incrementAndGet());
            }
            data.put(expense.getId(), expense);
            return expense;
        }

        @Override
        public List<Expense> findAll() {
            return new ArrayList<>(data.values());
        }

        @Override
        public Optional<Expense> findById(Long id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public boolean deleteById(Long id) {
            return data.remove(id) != null;
        }
    }

    private ExpenseService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseService(new FakeExpenseRepository());
    }

    @Test
    void addExpense_assignsIdAndTrimsText() {
        ExpenseRequest request = new ExpenseRequest("  Groceries  ", new BigDecimal("45.50"), "  Food  ", LocalDate.of(2026, 7, 1));

        Expense created = service.addExpense(request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Groceries");
        assertThat(created.getCategory()).isEqualTo("Food");
        assertThat(created.getAmount()).isEqualByComparingTo("45.50");
    }

    @Test
    void getExpenses_filtersByCategoryCaseInsensitively() {
        service.addExpense(new ExpenseRequest("Bus ticket", new BigDecimal("2.50"), "Transport", LocalDate.now()));
        service.addExpense(new ExpenseRequest("Taxi", new BigDecimal("15.00"), "transport", LocalDate.now()));
        service.addExpense(new ExpenseRequest("Coffee", new BigDecimal("3.00"), "Food", LocalDate.now()));

        List<Expense> transport = service.getExpenses("Transport");

        assertThat(transport).hasSize(2);
        assertThat(transport).extracting(Expense::getTitle).containsExactlyInAnyOrder("Bus ticket", "Taxi");
    }

    @Test
    void getExpenses_withNoFilter_returnsEverything() {
        service.addExpense(new ExpenseRequest("Bus ticket", new BigDecimal("2.50"), "Transport", LocalDate.now()));
        service.addExpense(new ExpenseRequest("Coffee", new BigDecimal("3.00"), "Food", LocalDate.now()));

        assertThat(service.getExpenses(null)).hasSize(2);
        assertThat(service.getExpenses("")).hasSize(2);
    }

    @Test
    void getSummary_computesOverallAndPerCategoryTotals() {
        service.addExpense(new ExpenseRequest("Bus ticket", new BigDecimal("2.50"), "Transport", LocalDate.now()));
        service.addExpense(new ExpenseRequest("Taxi", new BigDecimal("15.00"), "Transport", LocalDate.now()));
        service.addExpense(new ExpenseRequest("Coffee", new BigDecimal("3.00"), "Food", LocalDate.now()));

        ExpenseSummary summary = service.getSummary();

        assertThat(summary.getTotalAmount()).isEqualByComparingTo("20.50");
        assertThat(summary.getTotalsByCategory().get("Transport")).isEqualByComparingTo("17.50");
        assertThat(summary.getTotalsByCategory().get("Food")).isEqualByComparingTo("3.00");
    }

    @Test
    void getSummary_onEmptyLedger_returnsZeroTotal() {
        ExpenseSummary summary = service.getSummary();

        assertThat(summary.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalsByCategory()).isEmpty();
    }

    @Test
    void deleteExpense_removesIt() {
        Expense created = service.addExpense(new ExpenseRequest("Coffee", new BigDecimal("3.00"), "Food", LocalDate.now()));

        service.deleteExpense(created.getId());

        assertThatThrownBy(() -> service.getExpenseById(created.getId()))
                .isInstanceOf(ExpenseNotFoundException.class);
    }

    @Test
    void deleteExpense_unknownId_throwsNotFound() {
        assertThatThrownBy(() -> service.deleteExpense(999_999L))
                .isInstanceOf(ExpenseNotFoundException.class)
                .hasMessageContaining("999999");
    }

    @Test
    void getExpenseById_unknownId_throwsNotFound() {
        assertThatThrownBy(() -> service.getExpenseById(999_999L))
                .isInstanceOf(ExpenseNotFoundException.class);
    }
}
