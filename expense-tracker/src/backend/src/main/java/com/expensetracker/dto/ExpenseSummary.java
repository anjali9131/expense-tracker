package com.expensetracker.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response body for GET /api/expenses/summary.
 */
public class ExpenseSummary {

    private BigDecimal totalAmount;
    private Map<String, BigDecimal> totalsByCategory;

    public ExpenseSummary() {
    }

    public ExpenseSummary(BigDecimal totalAmount, Map<String, BigDecimal> totalsByCategory) {
        this.totalAmount = totalAmount;
        this.totalsByCategory = totalsByCategory;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Map<String, BigDecimal> getTotalsByCategory() {
        return totalsByCategory;
    }

    public void setTotalsByCategory(Map<String, BigDecimal> totalsByCategory) {
        this.totalsByCategory = totalsByCategory;
    }
}
