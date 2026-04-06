package edu.software.project.frontend.api;

public class ApiException extends RuntimeException {
    private final int statusCode;

    public ApiException(String message) {
        this(-1, message);
    }

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
