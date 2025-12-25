package com.example.akkajr.core.observability;

public final class Alert {
    private final String level;
    private final String code;
    private final String message;

    public Alert(String level, String code, String message) {
        this.level = level;
        this.code = code;
        this.message = message;
    }

    public String getLevel() {
        return level;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
