package de.datev.refsys.aggregation.processing.util;

import de.datev.refsys.aggregation.processing.exception.HttpCallRetryExceptionPredicate;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.COLLECTIVE_ACCOUNTS_MOVEMENT_INVENTORIES_TRANSLATION_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.HTTP_CLIENT_RETRY_INSTANCE_NAME;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.IMPORT_MOVEMENT_DATA_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.MASTER_DATA_CONTEXT_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.MOVEMENT_DATA_CLIENT_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.PURPOSE_MAPPINGS_CAPTIONS_MASTER_INVENTORIES_SHAREHOLDER_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.STATE_DOC_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.util.TestUtil.getCircuitBreaker;

public class ResetResilienceExtension implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        ApplicationContext context = SpringExtension.getApplicationContext(extensionContext);
        CircuitBreakerRegistry circuitBreakerRegistry = context.getBean(CircuitBreakerRegistry.class);

        CircuitBreakerConfig circuitBreakerConfig = context.getBean(CircuitBreakerConfig.class);
        CircuitBreakerConfig circuitBreakerConfigWithPredicate =
                CircuitBreakerConfig.from(circuitBreakerConfig).recordException(new HttpCallRetryExceptionPredicate()).build();

        circuitBreakerRegistry.replace(MASTER_DATA_CONTEXT_CIRCUIT_BREAKER, getCircuitBreaker(MASTER_DATA_CONTEXT_CIRCUIT_BREAKER, circuitBreakerConfigWithPredicate));
        circuitBreakerRegistry.replace(PURPOSE_MAPPINGS_CAPTIONS_MASTER_INVENTORIES_SHAREHOLDER_CIRCUIT_BREAKER, getCircuitBreaker(PURPOSE_MAPPINGS_CAPTIONS_MASTER_INVENTORIES_SHAREHOLDER_CIRCUIT_BREAKER, circuitBreakerConfigWithPredicate));
        circuitBreakerRegistry.replace(COLLECTIVE_ACCOUNTS_MOVEMENT_INVENTORIES_TRANSLATION_CIRCUIT_BREAKER, getCircuitBreaker(COLLECTIVE_ACCOUNTS_MOVEMENT_INVENTORIES_TRANSLATION_CIRCUIT_BREAKER, circuitBreakerConfigWithPredicate));
        circuitBreakerRegistry.replace(MOVEMENT_DATA_CLIENT_CIRCUIT_BREAKER, getCircuitBreaker(MOVEMENT_DATA_CLIENT_CIRCUIT_BREAKER, circuitBreakerConfigWithPredicate));
        circuitBreakerRegistry.replace(IMPORT_MOVEMENT_DATA_CIRCUIT_BREAKER, getCircuitBreaker(IMPORT_MOVEMENT_DATA_CIRCUIT_BREAKER, circuitBreakerConfig));
        circuitBreakerRegistry.replace(AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER, getCircuitBreaker(AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER, circuitBreakerConfig));
        circuitBreakerRegistry.replace(STATE_DOC_CIRCUIT_BREAKER, getCircuitBreaker(STATE_DOC_CIRCUIT_BREAKER, circuitBreakerConfig));

        RetryRegistry retryRegistry = context.getBean(RetryRegistry.class);
        retryRegistry.replace(HTTP_CLIENT_RETRY_INSTANCE_NAME, Retry.of(HTTP_CLIENT_RETRY_INSTANCE_NAME, context.getBean("httpRetryConfig", RetryConfig.class)));
        retryRegistry.replace(MONGODB_RETRY_INSTANCE_NAME, Retry.of(MONGODB_RETRY_INSTANCE_NAME, context.getBean("mongoRetryConfig", RetryConfig.class)));
    }
}
