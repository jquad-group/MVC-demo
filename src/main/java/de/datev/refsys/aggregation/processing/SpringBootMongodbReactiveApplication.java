package de.datev.refsys.aggregation.processing;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.reactive.config.EnableWebFlux;

@Slf4j
@EnableWebFlux
@SpringBootApplication
public class SpringBootMongodbReactiveApplication {

    @Autowired
    CircuitBreakerRegistry registry;

    @Value("${ref-sys.circuitbreaker-enabled:false}")
    boolean isCircuitBreakerEnabled;

    public static void main(String[] args) {
        SpringApplication.run(SpringBootMongodbReactiveApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        log.info(" APPLICATION STARTUP FINISHED");
        log.info(" Registered Circuitbreaker: {}", registry.getAllCircuitBreakers().size());
        if (!isCircuitBreakerEnabled) {
            registry.getAllCircuitBreakers().forEach(CircuitBreaker::transitionToDisabledState);
            log.info("Disabled all circuitbreakers");
        }
    }
}
