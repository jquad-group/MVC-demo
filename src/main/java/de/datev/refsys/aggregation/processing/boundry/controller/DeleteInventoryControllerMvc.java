package de.datev.refsys.aggregation.processing.boundry.controller;

import de.datev.refsys.aggregation.processing.api.DeleteInventoryApi;
import de.datev.refsys.aggregation.processing.boundry.event.ChangeEventProducer;
import de.datev.refsys.aggregation.processing.service.ImportExecutionService;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import com.mongodb.client.result.DeleteResult;
import java.util.Set;

import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.MONGODB_WRITE_UNACKNOWLEDGED;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class DeleteInventoryControllerMvc implements DeleteInventoryApi {
    private final ImportExecutionService importExecutionService;
    private final ChangeEventProducer changeEventProducer;

    @Override
    public ResponseEntity<Void> deleteInventory(Integer consultant, Integer client, Integer fiscalYear, 
                                               String xCorrelationId, String requestId,
                                               HttpServletRequest request) {
        log.info(LoggingUtil.MARKER, LoggingUtil.DELETE_INVENTORY_START_LOG);
        
        StopWatch stopWatch = StopWatch.createStarted();
        
        try {
            // Synchronous service call to delete import data
            Set<DeleteResult> deleteResults = importExecutionService.deleteImportData(consultant, client, fiscalYear);
            
            // Manual timing and logging
            LoggingUtil.logInfoWithDuration(LoggingUtil.DELETE_INVENTORY_RESPONSE_LOG)
                      .accept(stopWatch.getTotalTimeMillis());
            
            // Check if any delete operation was unacknowledged
            boolean isUnacknowledged = deleteResults.stream().anyMatch(deleteResult -> !deleteResult.wasAcknowledged());
            if (isUnacknowledged) {
                throw new RuntimeException(MONGODB_WRITE_UNACKNOWLEDGED);
            }

            // Send Kafka notification if deletions occurred
            if (!deleteResults.isEmpty()) {
                changeEventProducer.sendMessage(consultant, client, fiscalYear, null, null);
            }

            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            log.error("Delete inventory failed for consultant={}, client={}, fiscalYear={}", 
                     consultant, client, fiscalYear, e);
            throw e; // Let @ControllerAdvice handle exception mapping
        } finally {
            stopWatch.stop();
        }
    }
}