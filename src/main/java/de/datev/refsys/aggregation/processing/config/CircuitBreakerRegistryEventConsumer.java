package de.datev.refsys.aggregation.processing.config;

import de.datev.refsys.aggregation.processing.util.CircuitBreakerUtil;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import lombok.extern.slf4j.Slf4j;

/**
 * A class that implements {@link RegistryEventConsumer} to register an event consumer for a specified {@link CircuitBreaker}.
 */
@Slf4j
public class CircuitBreakerRegistryEventConsumer implements RegistryEventConsumer<CircuitBreaker> {

    /**
     * Register an onStateTransition event for each {@link CircuitBreaker} added via the {@link io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry},
     * which logs the status of a circuit breaker during transition from close to open state or from half-open to close state.
     *
     * @param entryAddedEvent {@link EntryAddedEvent} for a circuit breaker
     */
    @Override
    public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
        entryAddedEvent.getAddedEntry().getEventPublisher().onStateTransition(CircuitBreakerUtil::logOnStateTransition);
    }

    @Override
    public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
        // not needed
    }

    @Override
    public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
        // not needed
    }
}
