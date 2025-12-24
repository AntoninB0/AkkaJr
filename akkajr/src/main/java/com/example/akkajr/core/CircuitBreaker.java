package com.example.akkajr.core;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class CircuitBreaker implements DeathWatch {
    private static final Logger LOGGER = Logger.getLogger(CircuitBreaker.class.getName());
    private final Map<Service, Integer> failureCount = new ConcurrentHashMap<>();

    @Override
    public void watch(Service service) {
        LOGGER.info("Surveillance Circuit Breaker pour : " + service.getName());
    }

    @Override
    public void unwatch(Service service) {
        failureCount.remove(service);
    }

    @Override
    public void onServiceTerminated(Service terminatedService) {
        LOGGER.severe("ALERT : Circuit Breaker ouvert pour " + terminatedService.getName());
    }

    @Override
    public Set<Service> getWatchedServices() {
        return failureCount.keySet();
    }

    public static class CircuitBreakerOpenException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}