package de.datev.refsys.aggregation.processing.boundry.controller;

import de.datev.refsys.aggregation.processing.api.InitialLoadApi;
import de.datev.refsys.aggregation.processing.service.CommonImportService;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class InitialLoadControllerMvc implements InitialLoadApi {
    private final CommonImportService commonImportService;

    @Override
    public ResponseEntity<Void> startInitialLoad(Integer consultant, Integer client, Integer fiscalYear, 
                                                Long baseVersion, Long deltaVersion,
                                                String xCorrelationId, String requestId, 
                                                HttpServletRequest request) {
        log.info(LoggingUtil.MARKER, LoggingUtil.INITIAL_LOAD_START_LOG);
        
        StopWatch stopWatch = StopWatch.createStarted();
        
        try {
            // Synchronous call to start the import process
            Boolean importResult = commonImportService.doFireAndForgetFullImport(
                consultant, client, fiscalYear, baseVersion, deltaVersion);
            
            // Manual timing and logging
            LoggingUtil.logInfoWithDuration(LoggingUtil.INITIAL_LOAD_RESPONSE_LOG)
                      .accept(stopWatch.getTotalTimeMillis());
            
            if (importResult != null && importResult) {
                // Return 202 Accepted for async processing
                return ResponseEntity.accepted().build();
            } else {
                log.error("Initial load failed for consultant={}, client={}, fiscalYear={}", 
                         consultant, client, fiscalYear);
                return ResponseEntity.internalServerError().build();
            }
            
        } catch (Exception e) {
            log.error("Initial load failed for consultant={}, client={}, fiscalYear={}", 
                     consultant, client, fiscalYear, e);
            throw e; // Let @ControllerAdvice handle exception mapping
        } finally {
            stopWatch.stop();
        }
    }
}