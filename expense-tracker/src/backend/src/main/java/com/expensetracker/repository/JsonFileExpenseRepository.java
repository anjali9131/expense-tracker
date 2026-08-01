package com.expensetracker.repository;

import com.expensetracker.model.Expense;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Stores expenses in an in-memory map for fast reads, and mirrors every
 * mutation to a JSON file on disk so data survives an application restart.
 * No database is required per the assignment brief.
 *
 * A single ReentrantReadWriteLock keeps the in-memory map and the file in
 * sync under concurrent requests without needing a database transaction.
 */
@Repository
public class JsonFileExpenseRepository implements ExpenseRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonFileExpenseRepository.class);

    private final Map<Long, Expense> store = new LinkedHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(0);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ObjectMapper objectMapper;
    private final File dataFile;

    public JsonFileExpenseRepository(@Value("${app.data-file:expenses-data.json}") String dataFilePath) {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
        this.dataFile = new File(dataFilePath);
    }

    @PostConstruct
    void loadFromDisk() {
        lock.writeLock().lock();
        try {
            if (dataFile.exists()) {
                List<Expense> loaded = objectMapper.readValue(dataFile, new com.fasterxml.jackson.core.type.TypeReference<List<Expense>>() {
                });
                for (Expense expense : loaded) {
                    store.put(expense.getId(), expense);
                    idSequence.updateAndGet(current -> Math.max(current, expense.getId()));
                }
                log.info("Loaded {} expense(s) from {}", loaded.size(), dataFile.getAbsolutePath());
            } else {
                log.info("No existing data file at {} — starting with an empty ledger", dataFile.getAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("Could not read data file {} — starting with an empty ledger. Cause: {}",
                    dataFile.getAbsolutePath(), e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Expense save(Expense expense) {
        lock.writeLock().lock();
        try {
            if (expense.getId() == null) {
                expense.setId(idSequence.incrementAndGet());
            }
            store.put(expense.getId(), expense);
            persist();
            return expense;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<Expense> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(store.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Expense> findById(Long id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(store.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean deleteById(Long id) {
        lock.writeLock().lock();
        try {
            Expense removed = store.remove(id);
            if (removed != null) {
                persist();
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Must be called while holding the write lock. */
    private void persist() {
        try {
            objectMapper.writeValue(dataFile, new ArrayList<>(store.values()));
        } catch (IOException e) {
            log.error("Failed to persist expenses to {}: {}", dataFile.getAbsolutePath(), e.getMessage());
        }
    }
}
