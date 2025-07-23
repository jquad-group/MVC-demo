package de.datev.refsys.aggregation.processing.boundry.controller;

import de.datev.refsys.aggregation.processing.api.DeleteInventoryApi;
import de.datev.refsys.aggregation.processing.boundry.event.ChangeEventProducer;
import de.datev.refsys.aggregation.processing.service.ImportExecutionService;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.MONGODB_WRITE_UNACKNOWLEDGED;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class DeleteInventoryController implements DeleteInventoryApi {
    private final ImportExecutionService importExecutionService;
    private final ChangeEventProducer changeEventProducer;

    @Override
    public Mono<Void> deleteInventory(Integer consultant, Integer client, Integer fiscalYear, String xCorrelationId, String requestId,
                                      ServerWebExchange exchange) {
        log.info(LoggingUtil.MARKER, LoggingUtil.DELETE_INVENTORY_START_LOG);
        return importExecutionService.deleteImportData(consultant, client, fiscalYear)
                                     .collect(Collectors.toSet())
                                     .elapsed().map(LoggingUtil.logInfoWithDuration(LoggingUtil.DELETE_INVENTORY_RESPONSE_LOG))
                                     .flatMap(deleteResults -> {
                                         // throw error if delete is not acknowledged
                                         boolean isUnacknowledged = deleteResults.stream().anyMatch(deleteResult -> !deleteResult.wasAcknowledged());
                                         if (isUnacknowledged) {
                                             return Mono.error(new RuntimeException(MONGODB_WRITE_UNACKNOWLEDGED));
                                         }

                                         // notify receivers
                                         if (!deleteResults.isEmpty()) {
                                             return changeEventProducer.sendMessage(consultant, client, fiscalYear, null, null);
                                         }

                                         return Mono.empty();
                                     });
    }
}