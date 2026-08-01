package com.expensetracker.repository;

import com.expensetracker.model.Expense;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository {

    Expense save(Expense expense);

    List<Expense> findAll();

    Optional<Expense> findById(Long id);

    boolean deleteById(Long id);
}
