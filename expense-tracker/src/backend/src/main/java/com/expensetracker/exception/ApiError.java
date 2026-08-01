package com.expensetracker.exception;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error shape returned for every 4xx/5xx response so API clients
 * only ever need to handle one error format.
 */
public class ApiError {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private List<String> details;

    public ApiError() {
    }

    public ApiError(int status, String error, String message, List<String> details) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getDetails() {
        return details;
    }
}
