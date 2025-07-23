package de.datev.refsys.aggregation.processing.service;

import com.mongodb.client.result.UpdateResult;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBaseException;
import de.datev.refsys.aggregation.processing.model.ImportData;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;

import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonImportServiceImpl implements CommonImportService {
    private final ImportExecutionService importExecutionService;
    private final ImportService importService;

    @Override
    public Mono<UpdateResult> doFullImport(Integer consultant, Integer client, Integer fiscalYearStart, Long baseVersion, Long deltaVersion) {
        return Mono.deferContextual(
                ctx -> importExecutionService.initializeFullImport(consultant, client, fiscalYearStart, baseVersion, deltaVersion, ctx)
                                             .flatMap(importData -> executeFullImport(importData).contextWrite(ctx))
                                             .onErrorResume(handleException(consultant, client, fiscalYearStart, ctx)).contextWrite(ctx)
                                             .onErrorResume(handleUnexpectedErrorsAndPropagate()).contextWrite(ctx));
    }

    @Override
    public Mono<ImportData> doFireAndForgetFullImport(Integer consultant, Integer client, Integer fiscalYearStart, Long baseVersion, Long deltaVersion) {
        return Mono.deferContextual(
                ctx -> importExecutionService.initializeFullImport(consultant, client, fiscalYearStart, baseVersion, deltaVersion, ctx)
                                             .onErrorResume(handleException(consultant, client, fiscalYearStart, ctx)).contextWrite(ctx)
                                             .onErrorResume(handleUnexpectedErrorsAndPropagate()).contextWrite(ctx)
                                             .publishOn(Schedulers.boundedElastic())
                                             .doOnNext(importData -> executeFullImport(importData)
                                                     .contextWrite(ctx)
                                                     .onErrorResume(handleException(consultant, client, fiscalYearStart, ctx)).contextWrite(ctx)
                                                     .onErrorResume(handleUnexpectedErrorsAndReturnEmpty()).contextWrite(ctx)
                                                     .subscribe()));
    }

    private Mono<UpdateResult> executeFullImport(ImportData importData) {
        Mono<Boolean> importAction = importService.executeFullImport(importData);
        return importExecutionService.executeImport(importData, importAction, LoggingUtil.FULL_IMPORT_SUCCESS_LOG);
    }

    private <T> Function<Throwable, Mono<T>> handleException(Integer consultant, Integer client, Integer fiscalYearStart, ContextView ctx) {
        return e -> importExecutionService.handleExceptionAndUpdateStateDoc(consultant, client, fiscalYearStart, ctx, LoggingUtil.INITIAL_LOAD_ERROR_LOG, e);
    }

    /**
     * Handle all errors that could happen during exception handling logic then return empty
     */
    private static <T> Function<Throwable, Mono<T>> handleUnexpectedErrorsAndReturnEmpty() {
        return e -> {
            logOnlyUnexpectedError(e);
            return Mono.empty();
        };
    }

    /**
     * Handle all errors that could happen during exception handling logic then propagate the error
     */
    private static <T> Function<Throwable, Mono<T>> handleUnexpectedErrorsAndPropagate() {
        return e -> {
            logOnlyUnexpectedError(e);
            return Mono.error(e);
        };
    }

    private static void logOnlyUnexpectedError(Throwable e) {
        if (!(e instanceof AggregationProcessingBaseException)) {
            log.error(ProcessingErrorMessageConstants.UNEXPECTED_ERROR_IN_EXCEPTION_HANDLING, e);
        }
    }
}
