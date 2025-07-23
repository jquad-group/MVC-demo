package de.datev.refsys.aggregation.processing.boundry.controller;

import de.datev.refsys.aggregation.processing.api.ProcessDeltaApi;
import de.datev.refsys.aggregation.processing.api.model.DeltaInfo;
import de.datev.refsys.aggregation.processing.api.model.DeltaRequest;
import de.datev.refsys.aggregation.processing.boundry.event.ChangeEventProducer;
import de.datev.refsys.aggregation.processing.service.DeltaEventProcessingService;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ProcessDeltaController implements ProcessDeltaApi {

    private final DeltaEventProcessingService deltaEventProcessingService;
    private final ChangeEventProducer changeEventProducer;

    @Override
    public Mono<DeltaInfo> processDelta(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion, String xCorrelationId,
                                        String requestId, Mono<DeltaRequest> deltaRequest, ServerWebExchange exchange) {
        log.info(LoggingUtil.MARKER, LoggingUtil.PROCESS_DELTA_START_LOG);
        return Mono.deferContextual(ctx -> processDeltaEvent(consultant, client, fiscalYear, baseVersion, deltaVersion, deltaRequest, exchange, ctx))
                   .elapsed().map(LoggingUtil.logInfoWithDuration(LoggingUtil.PROCESS_DELTA_RESPONSE_LOG));
    }

    private Mono<DeltaInfo> processDeltaEvent(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion,
                                   Mono<DeltaRequest> deltaRequest, ServerWebExchange exchange, ContextView ctx) {
        return deltaEventProcessingService.processDeltaEvent(consultant, client, fiscalYear, baseVersion, deltaVersion, deltaRequest)
                                          .switchIfEmpty(Mono.defer(() -> {
                                              exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                                              return Mono.just(true)
                                                         .elapsed().map(LoggingUtil.logInfoWithDuration(LoggingUtil.PROCESS_DELTA_RESPONSE_LOG))
                                                         .then(Mono.empty());
                                          }))
                                          .publishOn(Schedulers.boundedElastic())
                                          .doOnNext(updateResult -> changeEventProducer.sendMessage(consultant, client, fiscalYear, baseVersion,
                                                                                                    deltaVersion).contextWrite(ctx).subscribe());
    }
}
