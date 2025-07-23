package de.datev.refsys.aggregation.processing.config;

import de.datev.refsys.aggregation.processing.exception.HttpCallRetryExceptionPredicate;
import de.datev.refsys.aggregation.processing.exception.RetryMongodbExceptionPredicate;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;

import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.COLLECTIVE_ACCOUNTS_MOVEMENT_INVENTORIES_TRANSLATION_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.HTTP_CLIENT_RETRY_INSTANCE_NAME;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.IMPORT_MOVEMENT_DATA_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.MASTER_DATA_CONTEXT_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.MOVEMENT_DATA_CLIENT_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.PURPOSE_MAPPINGS_CAPTIONS_MASTER_INVENTORIES_SHAREHOLDER_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.STATE_DOC_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.getCircuitBreaker;

@TestConfiguration
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@ActiveProfiles(TEST_PROFILE)
public class TestResilienceConfiguration {

    @Value("${resilience4j.circuitbreaker.configs.shared-config.automatic-transition-from-open-to-half-open-enabled}")
    private Boolean automaticTransitionFromOpenToHalfOpenEnabled;
    @Value("${resilience4j.circuitbreaker.configs.shared-config.failure-rate-threshold}")
    private Float failureRateThreshold;
    @Value("${resilience4j.circuitbreaker.configs.shared-config.slidingWindowSize}")
    private Integer slidingWindowSize;
    @Value("${resilience4j.circuitbreaker.configs.shared-config.minimumNumberOfCalls}")
    private Integer minimumNumberOfCalls;
    @Value("${resilience4j.circuitbreaker.configs.shared-config.permitted-number-of-calls-in-half-open-state}")
    private Integer permittedNumberOfCallsInHalfOpenState;
    @Value("${resilience4j.circuitbreaker.configs.shared-config.waitDurationInOpenState}")
    private Long waitDurationInOpenState;

    @Value("${resilience4j.retry.configs.shared-retry-config.wait-duration}")
    private Integer waitDuration;
    @Value("${resilience4j.retry.configs.shared-retry-config.exponential-backoff-multiplier}")
    private Double exponentialBackoffMultiplier;
    @Value("${resilience4j.retry.configs.shared-retry-config.exponential-max-wait-duration}")
    private Integer exponentialMaxWaitDuration;
    @Value("${resilience4j.retry.configs.shared-retry-config.max-attempts}")
    private Integer maxAttempts;

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                                   .automaticTransitionFromOpenToHalfOpenEnabled(automaticTransitionFromOpenToHalfOpenEnabled)
                                   .failureRateThreshold(failureRateThreshold)
                                   .slidingWindowSize(slidingWindowSize)
                                   .minimumNumberOfCalls(minimumNumberOfCalls)
                                   .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
                                   .waitDurationInOpenState(Duration.ofMillis(waitDurationInOpenState))
                                   .build();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(final CircuitBreakerConfig circuitBreakerConfig) {
        CircuitBreakerConfig circuitBreakerConfigWithPredicate =
                CircuitBreakerConfig.from(circuitBreakerConfig).recordException(new HttpCallRetryExceptionPredicate()).build();
        RegistryStore<CircuitBreaker> circuitBreakerRegistryStore = new InMemoryRegistryStore<>();
        circuitBreakerRegistryStore.putIfAbsent(MASTER_DATA_CONTEXT_CIRCUIT_BREAKER, getCircuitBreaker(MASTER_DATA_CONTEXT_CIRCUIT_BREAKER, circuitBreakerConfigWithPredicate));
        circuitBreakerRegistryStore.putIfAbsent(PURPOSE_MAPPINGS_CAPTIONS_MASTER_INVENTORIES_SHAREHOLDER_CIRCUIT_BREAKER, getCircuitBreaker(PURPOSE_MAPPINGS_CAPTIONS_MASTER_INVENTORIES_SHAREHOLDER_CIRCUIT_BREAKER, circuitBreakerConfigWithPredicate));
        circuitBreakerRegistryStore.putIfAbsent(COLLECTIVE_ACCOUNTS_MOVEMENT_INVENTORIES_TRANSLATION_CIRCUIT_BREAKER, getCircuitBreaker(COLLECTIVE_ACCOUNTS_MOVEMENT_INVENTORIES_TRANSLATION_CIRCUIT_BREAKER, circuitBreakerConfigWithPredicate));
        circuitBreakerRegistryStore.putIfAbsent(MOVEMENT_DATA_CLIENT_CIRCUIT_BREAKER, getCircuitBreaker(MOVEMENT_DATA_CLIENT_CIRCUIT_BREAKER, circuitBreakerConfigWithPredicate));
        circuitBreakerRegistryStore.putIfAbsent(STATE_DOC_CIRCUIT_BREAKER, getCircuitBreaker(STATE_DOC_CIRCUIT_BREAKER, circuitBreakerConfig));
        circuitBreakerRegistryStore.putIfAbsent(IMPORT_MOVEMENT_DATA_CIRCUIT_BREAKER, getCircuitBreaker(IMPORT_MOVEMENT_DATA_CIRCUIT_BREAKER, circuitBreakerConfig));
        circuitBreakerRegistryStore.putIfAbsent(AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER, getCircuitBreaker(AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER, circuitBreakerConfig));
        return CircuitBreakerRegistry.custom().withRegistryStore(circuitBreakerRegistryStore).build();
    }

    @Bean
    public RetryConfig.Builder retryConfig() {
        return RetryConfig.custom()
                          .maxAttempts(maxAttempts)
                          .intervalFunction(
                                  IntervalFunction.ofExponentialBackoff(waitDuration, exponentialBackoffMultiplier, exponentialMaxWaitDuration));
    }

    @Bean
    public RetryConfig mongoRetryConfig(final RetryConfig.Builder retryConfig) {
        return retryConfig.ignoreExceptions(org.bson.BsonMaximumSizeExceededException.class).retryOnException(new RetryMongodbExceptionPredicate()).build();
    }

    @Bean
    public RetryConfig httpRetryConfig(final RetryConfig.Builder retryConfig) {
        return  retryConfig.retryOnException(new HttpCallRetryExceptionPredicate()).build();
    }

    @Bean
    public RetryRegistry retryRegistry(final RetryConfig mongoRetryConfig, final RetryConfig httpRetryConfig) {
        RegistryStore<Retry> retryRegistryStore = new InMemoryRegistryStore<>();
        retryRegistryStore.putIfAbsent(MONGODB_RETRY_INSTANCE_NAME, Retry.of(MONGODB_RETRY_INSTANCE_NAME, mongoRetryConfig));
        retryRegistryStore.putIfAbsent(HTTP_CLIENT_RETRY_INSTANCE_NAME, Retry.of(HTTP_CLIENT_RETRY_INSTANCE_NAME, httpRetryConfig));
        return RetryRegistry.custom().withRegistryStore(retryRegistryStore).build();
    }
}
