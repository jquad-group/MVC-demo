package de.datev.refsys.aggregation.processing.util;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class CircuitBreakerUtil {
    public static final String CB_LOG = "%s: Transitioned from %S to %S";

    public void logOnStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        CircuitBreaker.StateTransition stateTransition = event.getStateTransition();
        CircuitBreaker.State fromState = stateTransition.getFromState();
        CircuitBreaker.State toState = stateTransition.getToState();
        String circuitBreakerName = event.getCircuitBreakerName();
        if (fromState == CircuitBreaker.State.CLOSED
                && toState == CircuitBreaker.State.OPEN) {
            log.warn(String.format(CB_LOG, circuitBreakerName, fromState, toState));
        }
        if (fromState == CircuitBreaker.State.OPEN
                && toState == CircuitBreaker.State.HALF_OPEN) {
            log.warn(String.format(CB_LOG, circuitBreakerName, fromState, toState));
        }
        if (fromState == CircuitBreaker.State.HALF_OPEN
                && toState == CircuitBreaker.State.OPEN) {
            log.warn(String.format(CB_LOG, circuitBreakerName, fromState, toState));
        }
        if (fromState == CircuitBreaker.State.HALF_OPEN
                && toState == CircuitBreaker.State.CLOSED) {
            log.warn(String.format(CB_LOG, circuitBreakerName, fromState, toState));
        }
    }
}
