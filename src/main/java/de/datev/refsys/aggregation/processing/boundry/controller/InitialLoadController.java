package de.datev.refsys.aggregation.processing.boundry.controller;

import de.datev.refsys.aggregation.processing.api.InitialLoadApi;
import de.datev.refsys.aggregation.processing.service.CommonImportService;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class InitialLoadController implements InitialLoadApi {
    private final CommonImportService commonImportService;

    @Override
    public Mono<Void> startInitialLoad(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion,
                                       String xCorrelationId, String requestId, ServerWebExchange exchange) {
        log.info(LoggingUtil.MARKER, LoggingUtil.INITIAL_LOAD_START_LOG);
        return commonImportService.doFireAndForgetFullImport(consultant, client, fiscalYear, baseVersion, deltaVersion)
                                  .thenReturn(true)
                                  .elapsed().map(LoggingUtil.logInfoWithDuration(LoggingUtil.INITIAL_LOAD_RESPONSE_LOG))
                                  .then();
    }
}