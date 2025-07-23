package de.datev.refsys.aggregation.processing.client;

import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.util.ExceptionUtil;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.generated.acds.api.CustomColumnStructuresApi;
import de.datev.refsys.generated.acds.api.CustomReportStructuresApi;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client class for calling CustomStructures Api
 */
@Component
public class CustomStructuresClient {
    private final CustomReportStructuresApi customReportStructuresApi;
    private final CustomColumnStructuresApi customColumnStructuresApi;
    private final Retry httpClientRetryInstance;
    private final boolean iasdEnabled;

    public CustomStructuresClient(final CustomReportStructuresApi customReportStructuresApi,
                                  final CustomColumnStructuresApi customColumnStructuresApi, @Value("${ref-sys.iasd-enabled}") Boolean iasdEnabled,
                                  final RetryRegistry retryRegistry) {
        this.customReportStructuresApi = customReportStructuresApi;
        this.customColumnStructuresApi = customColumnStructuresApi;
        this.httpClientRetryInstance = retryRegistry.retry(ProcessingServiceConstants.HTTP_CLIENT_RETRY_INSTANCE_NAME);
        this.iasdEnabled = iasdEnabled;
    }

    public Mono<List<CustomReportStructure>> getCustomReportStructuresList(MasterdataContext mdc) {
        if (iasdEnabled) {
            boolean readOrganisationData = mdc.getComprehensiveConsultant() == null;
            return Flux.deferContextual(
                               ctx -> customReportStructuresApi.getCustomReportStructures(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(),
                                                                                          readOrganisationData, mdc.getBaseVersion(),
                                                                                          mdc.getDeltaVersion(),
                                                                                          ProcessingServiceConstants.NEAR_TIME_DATA,
                                                                                          ctx.get(LoggingUtil.CORRELATION_ID_KEY),
                                                                                          UUID.randomUUID().toString()))
                       .collectList()
                       .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.ACDS_GET_CUSTOM_REPORT_STRUCTURES_LOG))
                       .defaultIfEmpty(new ArrayList<>())
                       .onErrorMap(throwable -> ExceptionUtil.handleWebClientException(SourceEndpoint.CUSTOM_REPORT_STRUCTURES, throwable))
                       .transformDeferred(RetryOperator.of(httpClientRetryInstance));
        }
        return Mono.just(new ArrayList<>());
    }

    public Mono<List<CustomColumnStructure>> getCustomColumnStructuresList(MasterdataContext mdc) {
        if (iasdEnabled) {
            boolean readOrganisationData = mdc.getComprehensiveConsultant() == null;
            return Flux.deferContextual(
                               ctx -> customColumnStructuresApi.getCustomColumnStructures(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(),
                                                                                          readOrganisationData, mdc.getBaseVersion(),
                                                                                          mdc.getDeltaVersion(),
                                                                                          ProcessingServiceConstants.NEAR_TIME_DATA,
                                                                                          ctx.get(LoggingUtil.CORRELATION_ID_KEY),
                                                                                          UUID.randomUUID().toString()))
                       .collectList()
                       .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.ACDS_GET_CUSTOM_COLUMN_STRUCTURES_LOG))
                       .defaultIfEmpty(new ArrayList<>())
                       .onErrorMap(throwable -> ExceptionUtil.handleWebClientException(SourceEndpoint.CUSTOM_COLUMN_STRUCTURES, throwable))
                       .transformDeferred(RetryOperator.of(httpClientRetryInstance));
        }
        return Mono.just(new ArrayList<>());
    }
}
