package de.datev.refsys.aggregation.processing.util;

import ch.qos.logback.classic.Level;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakerUtilTest {
    public static final String MOCK_CIRCUIT_BREAKER = "mockCircuitBreaker";

    private MemoryAppender memoryAppender;
    private CircuitBreakerOnStateTransitionEvent event;

    @BeforeEach
    void setUp() {
        memoryAppender = setupMemoryAppender(memoryAppender, CircuitBreakerUtil.class, Level.WARN);
    }

    @ParameterizedTest
    @EnumSource(value = CircuitBreaker.StateTransition.class, names = { "CLOSED_TO_OPEN", "OPEN_TO_HALF_OPEN", "HALF_OPEN_TO_OPEN",
            "HALF_OPEN_TO_CLOSED" })
    void should_record_log_for_circuit_breaker_when_state_changed(CircuitBreaker.StateTransition stateTransition) {
        this.event = new CircuitBreakerOnStateTransitionEvent(MOCK_CIRCUIT_BREAKER, stateTransition);
        CircuitBreakerUtil.logOnStateTransition(event);
        assertThat(memoryAppender.search(event.getCircuitBreakerName())).hasSize(1);
        assertThat(memoryAppender.contains(event.getCircuitBreakerName(), Level.WARN)).isTrue();
    }

    /**
     * These states are not real transitions but are added to help with the test coverage and ensure that the correct logs are returned when the
     * circuitbreaker stays on the same state.
     */
    @ParameterizedTest
    @EnumSource(value = CircuitBreaker.StateTransition.class, names = { "CLOSED_TO_CLOSED", "OPEN_TO_OPEN", "HALF_OPEN_TO_HALF_OPEN" })
    void should_not_record_log_for_circuit_breaker_when_same_to_same_state(CircuitBreaker.StateTransition stateTransition) {
        this.event = new CircuitBreakerOnStateTransitionEvent(MOCK_CIRCUIT_BREAKER, stateTransition);
        CircuitBreakerUtil.logOnStateTransition(event);
        assertThat(memoryAppender.search(event.getCircuitBreakerName())).isEmpty();
    }
}