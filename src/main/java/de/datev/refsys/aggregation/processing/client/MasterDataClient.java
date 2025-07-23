package de.datev.refsys.aggregation.processing.client;

import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.exception.HttpCallNoContentException;
import de.datev.refsys.aggregation.processing.exception.HttpCallTechnicalException;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
import de.datev.refsys.aggregation.processing.util.ExceptionUtil;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.generated.acds.api.AccountCaptionsApi;
import de.datev.refsys.generated.acds.api.AccountPurposeMappingsApi;
import de.datev.refsys.generated.acds.api.CollectiveAccountsApi;
import de.datev.refsys.generated.acds.api.MasterdataContextApi;
import de.datev.refsys.generated.acds.api.MasterdataInventoriesApi;
import de.datev.refsys.generated.acds.api.ShareholderApi;
import de.datev.refsys.generated.acds.api.TranslationApi;
import de.datev.refsys.generated.acds.api.model.AccountCaption;
import de.datev.refsys.generated.acds.api.model.AccountPurposeMapping;
import de.datev.refsys.generated.acds.api.model.CollectiveAccount;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.generated.acds.api.model.MasterdataInventory;
import de.datev.refsys.generated.acds.api.model.ShareholderData;
import de.datev.refsys.generated.acds.api.model.Translation;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client class for calling MasterData Api in ACDS
 */
@Component
@Slf4j
public class MasterDataClient {
    private final MasterdataContextApi masterdataContextApi;
    private final AccountCaptionsApi accountCaptionsApi;
    private final ShareholderApi shareholderApi;
    private final AccountPurposeMappingsApi accountPurposeMappingsApi;
    private final CollectiveAccountsApi collectiveAccountsApi;
    private final TranslationApi translationApi;
    private final MasterdataInventoriesApi masterdataInventoriesApi;
    private final CircuitBreaker masterDataContextCircuitBreaker;
    private final CircuitBreaker purposeMappingsCaptionsInventoriesAndShareholderCircuitBreaker;
    private final CircuitBreaker collectiveAccountsAndTranslationCircuitBreaker;
    private final Retry httpClientRetryInstance;

    public MasterDataClient(final MasterdataContextApi masterdataContextApi,
                            final AccountCaptionsApi accountCaptionsApi,
                            final ShareholderApi shareholderApi,
                            final AccountPurposeMappingsApi accountPurposeMappingsApi,
                            final CollectiveAccountsApi collectiveAccountsApi,
                            final TranslationApi translationApi,
                            final MasterdataInventoriesApi masterdataInventoriesApi,
                            final CircuitBreakerRegistry circuitBreakerRegistry,
                            final RetryRegistry retryRegistry) {
        this.masterdataContextApi = masterdataContextApi;
        this.accountCaptionsApi = accountCaptionsApi;
        this.shareholderApi = shareholderApi;
        this.accountPurposeMappingsApi = accountPurposeMappingsApi;
        this.collectiveAccountsApi = collectiveAccountsApi;
        this.translationApi = translationApi;
        this.masterdataInventoriesApi = masterdataInventoriesApi;
        this.masterDataContextCircuitBreaker = circuitBreakerRegistry.circuitBreaker(ProcessingServiceConstants.MASTER_DATA_CONTEXT_CIRCUIT_BREAKER);
        this.purposeMappingsCaptionsInventoriesAndShareholderCircuitBreaker = circuitBreakerRegistry.circuitBreaker(
                ProcessingServiceConstants.PURPOSE_MAPPINGS_CAPTIONS_MASTER_INVENTORIES_SHAREHOLDER_CIRCUIT_BREAKER);
        this.collectiveAccountsAndTranslationCircuitBreaker = circuitBreakerRegistry.circuitBreaker(
                ProcessingServiceConstants.COLLECTIVE_ACCOUNTS_MOVEMENT_INVENTORIES_TRANSLATION_CIRCUIT_BREAKER);
        this.httpClientRetryInstance = retryRegistry.retry(ProcessingServiceConstants.HTTP_CLIENT_RETRY_INSTANCE_NAME);
    }

    /**
     * Returns MasterDataContext for the consultant, client and fiscal year key in a specific version
     * <pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param consultant   consultant number
     * @param client       client number
     * @param fiscalYear   fiscal year start
     * @param baseVersion  base version of the data
     * @param deltaVersion delta version of the data
     * @return a MasterdataContext
     */
    public Mono<MasterdataContext> getMasterDataContext(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion,
                                                        String correlationId) {
        return masterdataContextApi.getMasterDataContext(consultant, client, fiscalYear, baseVersion, deltaVersion,
                                                         ProcessingServiceConstants.NEAR_TIME_DATA, correlationId, UUID.randomUUID().toString())
                                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.ACDS_GET_MASTERDATA_CONTEXT_LOG))
                                   .switchIfEmpty(getMasterDataContextNoContentError())
                                   .onErrorMap(throwable -> {
                                       if (!(throwable instanceof HttpCallTechnicalException)) {
                                           return ExceptionUtil.handleWebClientException(SourceEndpoint.MASTER_DATA_CONTEXT, throwable);
                                       }
                                       return throwable;
                                   })
                                   // TODO: masterDataContextCircuitBreaker anschalten
                                   //.transformDeferred(CircuitBreakerOperator.of(masterDataContextCircuitBreaker))
                                   .transformDeferred(RetryOperator.of(httpClientRetryInstance));
    }

    /**
     * Returns AccountCaptions for the consultant client and fiscal year key in a specific version
     * <pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param mdc MasterDataContext
     * @return list of AccountCaptions
     */
    public Mono<List<AccountCaption>> getAccountCaptions(MasterdataContext mdc) {
        return Flux.deferContextual(
                           ctx -> accountCaptionsApi.getAccountCaptions(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(),
                                                                        mdc.getBaseVersion(),
                                                                        mdc.getDeltaVersion(), mdc.getAccountSystem(),
                                                                        Optional.ofNullable(mdc.getIndustryId()).orElse(0),
                                                                        mdc.getUseOrganisationAccountCaption(), mdc.getSupportedLanguages(),
                                                                        ProcessingServiceConstants.NEAR_TIME_DATA,
                                                                        ctx.get(LoggingUtil.CORRELATION_ID_KEY),
                                                                        UUID.randomUUID().toString()))
                   .collectList()
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.ACDS_GET_ACCOUNT_CAPTIONS_LOG))
                   .defaultIfEmpty(new ArrayList<>())
                   .onErrorMap(throwable -> ExceptionUtil.handleWebClientException(SourceEndpoint.ACCOUNT_CAPTIONS, throwable))
                   .transformDeferred(CircuitBreakerOperator.of(purposeMappingsCaptionsInventoriesAndShareholderCircuitBreaker))
                   .transformDeferred(RetryOperator.of(httpClientRetryInstance));
    }

    /**
     * Returns AccountPurposeMappings for the consultant client and fiscal year key in a specific version
     * <pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param mdc MasterDataContext
     * @return list of AccountPurposeMappings
     */
    public Mono<List<AccountPurposeMapping>> getAccountPurposeMappings(MasterdataContext mdc) {
        return Flux.deferContextual(
                           ctx -> accountPurposeMappingsApi.getAccountPurposeMappings(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(),
                                                                                      mdc.getBaseVersion(), mdc.getDeltaVersion(),
                                                                                      mdc.getAccountSystem(),
                                                                                      mdc.getIndustryNo(), ProcessingServiceConstants.NEAR_TIME_DATA,
                                                                                      mdc.getUseRefsys(), ctx.get(LoggingUtil.CORRELATION_ID_KEY),
                                                                                      UUID.randomUUID().toString()))
                   .collectList()
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.ACDS_GET_ACCOUNT_PURPOSE_MAPPINGS_LOG))
                   .defaultIfEmpty(new ArrayList<>())
                   .onErrorMap(throwable -> ExceptionUtil.handleWebClientException(SourceEndpoint.ACCOUNT_PURPOSE_MAPPINGS, throwable))
                   .transformDeferred(CircuitBreakerOperator.of(purposeMappingsCaptionsInventoriesAndShareholderCircuitBreaker))
                   .transformDeferred(RetryOperator.of(httpClientRetryInstance));
    }

    /**
     * Returns CollectiveAccounts for the consultant client and fiscal year key in a specific version
     * <pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param mdc MasterDataContext
     * @return list of CollectiveAccounts
     */
    public Mono<List<CollectiveAccount>> getCollectiveAccounts(MasterdataContext mdc) {
        return Flux.deferContextual(
                           ctx -> collectiveAccountsApi.getCollectiveAccounts(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(),
                                                                              mdc.getBaseVersion(),
                                                                              mdc.getDeltaVersion(), mdc.getAccountLength(), mdc.getAccountSystem(),
                                                                              mdc.getIndustryNo(), mdc.getUseConsultantAccountingFunctions(),
                                                                              mdc.getUseClientAccountingFunctions(), mdc.getUseSkrFollowingYear(),
                                                                              ProcessingServiceConstants.NEAR_TIME_DATA,
                                                                              ctx.get(LoggingUtil.CORRELATION_ID_KEY),
                                                                              UUID.randomUUID().toString()))
                   .collectList()
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.ACDS_GET_COLLECTIVE_ACCOUNTS_LOG))
                   .defaultIfEmpty(new ArrayList<>())
                   .onErrorMap(throwable -> ExceptionUtil.handleWebClientException(SourceEndpoint.COLLECTIVE_ACCOUNTS, throwable))
                   .transformDeferred(CircuitBreakerOperator.of(collectiveAccountsAndTranslationCircuitBreaker))
                   .transformDeferred(RetryOperator.of(httpClientRetryInstance));
    }

    /**
     * Returns ShareholderData for the consultant client and fiscal year key in a specific version
     * <pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param mdc MasterDataContext
     * @return ShareholderData object
     */
    public Mono<ShareholderData> getShareholderData(MasterdataContext mdc) {
        return Mono.deferContextual(
                           ctx -> shareholderApi.getShareholders(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(), mdc.getBaseVersion(),
                                                                 mdc.getDeltaVersion(), ProcessingServiceConstants.NEAR_TIME_DATA,
                                                                 ctx.get(LoggingUtil.CORRELATION_ID_KEY), UUID.randomUUID().toString())
                                                .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.ACDS_GET_SHAREHOLDERS_LOG)))
                   .defaultIfEmpty(new ShareholderData())
                   .onErrorMap(throwable -> ExceptionUtil.handleWebClientException(SourceEndpoint.SHAREHOLDER, throwable))
                   .transformDeferred(CircuitBreakerOperator.of(purposeMappingsCaptionsInventoriesAndShareholderCircuitBreaker))
                   .transformDeferred(RetryOperator.of(httpClientRetryInstance));
    }

    /**
     * Returns previous and alternate Translation for the consultant client and fiscal year key in a specific version
     * <pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param mdc MasterDataContext
     * @return Translation object
     */
    public Mono<Translation> getTranslation(MasterdataContext mdc) {
        return Mono.deferContextual(
                           ctx -> translationApi.getTranslation(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(), mdc.getBaseVersion(),
                                                                mdc.getDeltaVersion(), mdc.getUsePreviousYearAccountTranslation(),
                                                                mdc.getUseAlternativeAccountTranslation(), ProcessingServiceConstants.NEAR_TIME_DATA,
                                                                ctx.get(LoggingUtil.CORRELATION_ID_KEY), UUID.randomUUID().toString())
                                                .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.ACDS_GET_TRANSLATIONS_LOG)))
                   .defaultIfEmpty(new Translation())
                   .onErrorMap(throwable -> ExceptionUtil.handleWebClientException(SourceEndpoint.TRANSLATION, throwable))
                   .transformDeferred(CircuitBreakerOperator.of(collectiveAccountsAndTranslationCircuitBreaker))
                   .transformDeferred(RetryOperator.of(httpClientRetryInstance));
    }

    /**
     * Returns the Inventories (Masterdata)
     *
     * @param mdc MasterDataContext
     * @return list of MasterdataInventory
     */
    public Mono<List<MasterdataInventory>> getInventories(MasterdataContext mdc) {
        return Flux.deferContextual(ctx -> masterdataInventoriesApi.getMasterdataInventories(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(),
                                                                                             mdc.getBaseVersion(), mdc.getDeltaVersion(),
                                                                                             ProcessingServiceConstants.NEAR_TIME_DATA,
                                                                                             ctx.get(LoggingUtil.CORRELATION_ID_KEY),
                                                                                             UUID.randomUUID().toString()))
                   .collectList()
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.ACDS_GET_MASTERDATA_INVENTORIES_LOG))
                   .defaultIfEmpty(new ArrayList<>())
                   .onErrorMap(throwable -> ExceptionUtil.handleWebClientException(SourceEndpoint.MASTER_DATA_INVENTORIES, throwable))
                   .transformDeferred(CircuitBreakerOperator.of(purposeMappingsCaptionsInventoriesAndShareholderCircuitBreaker))
                   .transformDeferred(RetryOperator.of(httpClientRetryInstance));
    }

    private static Mono<MasterdataContext> getMasterDataContextNoContentError() {
        return Mono.error(new HttpCallNoContentException(SourceError.ACDS, SourceEndpoint.MASTER_DATA_CONTEXT, HttpStatus.NO_CONTENT.value()));
    }
}
