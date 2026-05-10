package com.payment.api.circuitbreaker;

public enum CircuitBreakerState {
    CLOSED,    // normal operation — all requests pass through
    OPEN,      // failing — requests are rejected immediately
    HALF_OPEN  // testing recovery — one probe request allowed
}
