package com.expensetracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests that exercise the real Spring context, JSON (de)serialization,
 * validation, and the global exception handler through the HTTP layer.
 *
 * Each test uses its own unique category name so tests never interfere with
 * each other even though they share one running application context.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpenseControllerTest {

    private static Path tempDataFile;

    @DynamicPropertySource
    static void overrideDataFile(DynamicPropertyRegistry registry) throws Exception {
        tempDataFile = Files.createTempFile("expense-tracker-test", ".json");
        Files.deleteIfExists(tempDataFile); // let the app create it fresh
        registry.add("app.data-file", () -> tempDataFile.toString());
    }

    @AfterAll
    static void cleanUp() throws Exception {
        if (tempDataFile != null) {
            Files.deleteIfExists(tempDataFile);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void addExpense_withValidPayload_returns201WithLocationAndBody() throws Exception {
        String payload = """
                {
                  "title": "Groceries",
                  "amount": 45.50,
                  "category": "Food",
                  "date": "2026-07-01"
                }
                """;

        mockMvc.perform(post("/api/expenses")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/expenses/\\d+")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Groceries")))
                .andExpect(jsonPath("$.amount", comparesEqualTo(45.50)))
                .andExpect(jsonPath("$.category", is("Food")))
                .andExpect(jsonPath("$.date", is("2026-07-01")));
    }

    @Test
    void addExpense_missingTitle_returns400WithDetails() throws Exception {
        String payload = """
                {
                  "amount": 10.00,
                  "category": "Food",
                  "date": "2026-07-01"
                }
                """;

        mockMvc.perform(post("/api/expenses")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.details", not(empty())));
    }

    @Test
    void addExpense_negativeAmount_returns400() throws Exception {
        String payload = """
                {
                  "title": "Refund",
                  "amount": -5.00,
                  "category": "Food",
                  "date": "2026-07-01"
                }
                """;

        mockMvc.perform(post("/api/expenses")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getExpenses_filterByCategory_returnsOnlyMatching() throws Exception {
        String category = "IntegrationTransport";
        addExpense("Bus ticket", "2.50", category, "2026-07-02");
        addExpense("Taxi", "15.00", category, "2026-07-03");
        addExpense("Sandwich", "5.00", "IntegrationFood", "2026-07-03");

        mockMvc.perform(get("/api/expenses").param("category", category))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].category", everyItem(is(category))));
    }

    @Test
    void getExpenses_withoutFilter_includesAllExpenses() throws Exception {
        addExpense("One-off item", "1.00", "IntegrationMisc", "2026-07-04");

        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void getSummary_reflectsAddedExpenses() throws Exception {
        String category = "IntegrationSummaryCat";

        BigDecimal beforeTotal = readTotal();

        addExpense("Item A", "10.00", category, "2026-07-05");
        addExpense("Item B", "5.25", category, "2026-07-05");

        mockMvc.perform(get("/api/expenses/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount", comparesEqualTo(beforeTotal.add(new BigDecimal("15.25")).doubleValue())))
                .andExpect(jsonPath("$.totalsByCategory." + category, comparesEqualTo(15.25)));
    }

    @Test
    void deleteExpense_thenGetById_returns404() throws Exception {
        Long id = addExpense("Temporary", "9.99", "IntegrationDelete", "2026-07-06");

        mockMvc.perform(delete("/api/expenses/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/expenses/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void deleteExpense_unknownId_returns404() throws Exception {
        mockMvc.perform(delete("/api/expenses/{id}", 987_654_321L))
                .andExpect(status().isNotFound());
    }

    // --- helpers -------------------------------------------------------

    private Long addExpense(String title, String amount, String category, String date) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "amount", new BigDecimal(amount),
                "category", category,
                "date", date
        ));

        String response = mockMvc.perform(post("/api/expenses")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private BigDecimal readTotal() throws Exception {
        String response = mockMvc.perform(get("/api/expenses/summary"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new BigDecimal(objectMapper.readTree(response).get("totalAmount").asText());
    }
}
