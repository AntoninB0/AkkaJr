package com.example.akkajr.core;

public interface Supervisor {

    enum SupervisionMode {
        ONE_FOR_ONE,
        ALL_FOR_ONE
    }

    void handleFailure(String serviceId, Throwable cause);

    SupervisionMode getSupervisionMode();

    void setSupervisionMode(SupervisionMode mode);
}
