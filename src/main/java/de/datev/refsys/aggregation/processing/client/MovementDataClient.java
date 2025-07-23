package de.datev.refsys.aggregation.processing.client;

import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.generated.acds.api.AccountSumDaysApi;
import de.datev.refsys.generated.acds.api.MovementdataInventoriesApi;
import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.generated.acds.api.model.MovementdataInventory;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static de.datev.refsys.aggregation.processing.util.ExceptionUtil.handleWebClientException;

/**
 *
 */
@Component
public class MovementDataClient {
    private static final boolean AGGREGATED_PER_DAY = false;
    private final AccountSumDaysApi accountSumDaysApi;
    private final MovementdataInventoriesApi movementdataInventoriesApi;
    private final CircuitBreaker movementDataCircuitBreaker;
    private final CircuitBreaker inventoriesCircuitBreaker;
    private final Retry httpClientRetryInstance;

    public MovementDataClient(final AccountSumDaysApi accountSumDaysApi,
                              final MovementdataInventoriesApi movementdataInventoriesApi, final CircuitBreakerRegistry circuitBreakerRegistry,
                              final RetryRegistry retryRegistry) {
        this.accountSumDaysApi = accountSumDaysApi;
        this.movementdataInventoriesApi = movementdataInventoriesApi;
        this.movementDataCircuitBreaker = circuitBreakerRegistry.circuitBreaker(ProcessingServiceConstants.MOVEMENT_DATA_CLIENT_CIRCUIT_BREAKER);
        this.inventoriesCircuitBreaker = circuitBreakerRegistry.circuitBreaker(ProcessingServiceConstants.MOVEMENT_DATA_INVENTORIES_CIRCUIT_BREAKER);
        this.httpClientRetryInstance = retryRegistry.retry(ProcessingServiceConstants.HTTP_CLIENT_RETRY_INSTANCE_NAME);
    }

    /**
     * Returns AccountSumDays for the consultant client and fiscal year key in a specific version
     * <pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param mdc MasterDataContext
     * @return list of AccountSumDays
     */
    public Flux<AccountSumDay> getAccountSumDays(MasterdataContext mdc) {
        return Flux.deferContextual(
                           ctx -> accountSumDaysApi.getAccountSumDays(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(), mdc.getBaseVersion(),
                                                                      mdc.getDeltaVersion(), ProcessingServiceConstants.NEAR_TIME_DATA,
                                                                      AGGREGATED_PER_DAY, ctx.get(LoggingUtil.CORRELATION_ID_KEY),
                                                                      UUID.randomUUID().toString()))
                   .onErrorMap(throwable -> handleWebClientException(SourceEndpoint.ACCOUNT_SUM_DAYS, throwable))
                   .transformDeferred(CircuitBreakerOperator.of(movementDataCircuitBreaker))
                   .transformDeferred(RetryOperator.of(httpClientRetryInstance));
    }

    /**
     * Returns MovementdataInventories for the consultant client and fiscal year key in a specific version
     * <pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param mdc MasterDataContext
     * @return list of MovementdataInventories
     */
    public Flux<MovementdataInventory> getMovementDataInventories(MasterdataContext mdc) {
        return Flux.deferContextual(
                           ctx -> movementdataInventoriesApi.getMovementdataInventories(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(),
                                                                                        mdc.getBaseVersion(), mdc.getDeltaVersion(),
                                                                                        ProcessingServiceConstants.NEAR_TIME_DATA,
                                                                                        ctx.get(LoggingUtil.CORRELATION_ID_KEY),
                                                                                        UUID.randomUUID().toString()))
                   .onErrorMap(throwable -> handleWebClientException(SourceEndpoint.MOVEMENT_DATA_INVENTORIES, throwable))
                   .transformDeferred(CircuitBreakerOperator.of(inventoriesCircuitBreaker))
                   .transformDeferred(RetryOperator.of(httpClientRetryInstance));
    }

}
