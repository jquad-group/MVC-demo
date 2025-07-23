package de.datev.refsys.aggregation.processing.boundry.controller;

import de.datev.refsys.aggregation.processing.api.UpdateSchemaApi;
import de.datev.refsys.aggregation.processing.service.UpdateSchemaService;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class UpdateSchemaController implements UpdateSchemaApi {

    private final UpdateSchemaService updateSchemaService;

    @Override
    public Mono<Void> updateSchema(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion, String xCorrelationId,
                                   String requestId, ServerWebExchange exchange) {
        log.info(LoggingUtil.MARKER, LoggingUtil.UPDATE_SCHEMA_START_LOG);
        return updateSchemaService.updateSchemaVersion(consultant, client, fiscalYear, baseVersion, deltaVersion)
                                  .switchIfEmpty(Mono.defer(() -> {
                                      exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                                      return Mono.empty();
                                  }))
                                  .thenReturn(true)
                                  .elapsed().map(LoggingUtil.logInfoWithDuration(LoggingUtil.UPDATE_SCHEMA_RESPONSE_LOG))
                                  .then();
    }
}
