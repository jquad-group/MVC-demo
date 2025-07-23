package de.datev.refsys.aggregation.processing.service;

import com.mongodb.MongoWriteException;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import de.datev.refsys.aggregation.document.model.ProblemInfo;
import de.datev.refsys.aggregation.document.model.ProcessingError;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.boundry.event.ChangeEventProducer;
import de.datev.refsys.aggregation.processing.client.MasterDataClient;
import de.datev.refsys.aggregation.processing.config.InitialLoadConfiguration;
import de.datev.refsys.aggregation.processing.constant.MetricConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBaseException;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallException;
import de.datev.refsys.aggregation.processing.exception.HttpCallNoContentException;
import de.datev.refsys.aggregation.processing.exception.InitialLoadFailedException;
import de.datev.refsys.aggregation.processing.exception.RestWarnException;
import de.datev.refsys.aggregation.processing.model.ImportData;
import de.datev.refsys.aggregation.processing.repository.CustomColumnStructureContentRepository;
import de.datev.refsys.aggregation.processing.repository.CustomReportStructureContentRepository;
import de.datev.refsys.aggregation.processing.repository.MasterDataAccountRepository;
import de.datev.refsys.aggregation.processing.repository.MasterDataRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataInventoryRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataMonthRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupMonthRepository;
import de.datev.refsys.aggregation.processing.repository.StateDocRepository;
import de.datev.refsys.aggregation.processing.util.ExceptionUtil;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportExecutionServiceImpl implements ImportExecutionService {
    // the amount of repositories will be reduced after each refactoring of the import service, currently they are only used for deletion
    private final StateDocRepository stateDocRepository;
    private final MasterDataRepository masterDataRepository;
    private final MasterDataAccountRepository masterDataAccountRepository;
    private final MovementDataDayRepository movementDataDayRepository;
    private final MovementDataMonthRepository movementDataMonthRepository;
    private final MovementDataPersonGroupDayRepository personGroupDayRepository;
    private final MovementDataPersonGroupMonthRepository personGroupMonthRepository;
    private final MovementDataInventoryRepository movementDataInventoryRepository;
    private final ChangeEventProducer changeEventProducer;
    private final InitialLoadConfiguration initialLoadConfiguration;
    private final MasterDataClient masterDataClient;
    private final MeterRegistry meterRegistry;
    private final CustomColumnStructureContentRepository customColumnStructureContentRepository;
    private final CustomReportStructureContentRepository customReportStructureContentRepository;

    @Override
    public Mono<ImportData> initializeFullImport(Integer consultant, Integer client, Integer fiscalYearStart, Long baseVersion, Long deltaVersion,
                                                 ContextView contextView) {
        log.info(LoggingUtil.MARKER, LoggingUtil.FULL_IMPORT_STARTED_LOG);
        String correlationId = contextView.get(LoggingUtil.CORRELATION_ID_KEY);
        return checkStateDocStateAndDuration(consultant, client, fiscalYearStart, baseVersion, deltaVersion)
                .flatMap(stateDoc -> masterDataClient.getMasterDataContext(consultant, client, fiscalYearStart, baseVersion, deltaVersion,
                                                                           correlationId)
                                                     .contextWrite(contextView)
                                                     .name("importData-masterDataContext")
                                                     .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_ACDS)
                                                     .tap(Micrometer.metrics(meterRegistry))
                                                     .flatMap(mdc -> {
                                                         updateBaseAndDeltaVersionInLoggingContext(contextView, mdc);
                                                         return deleteCollectionsAndInitializeImport(mdc, stateDoc).contextWrite(contextView);
                                                     }));
    }

    @Override
    public Mono<ImportData> initializePartialImport(Integer consultant, Integer client, Integer fiscalYearStart, Long baseVersion, Long deltaVersion,
                                                    ContextView contextView) {
        log.info(LoggingUtil.MARKER, LoggingUtil.PARTIAL_IMPORT_STARTED_LOG);
        String correlationId = contextView.get(LoggingUtil.CORRELATION_ID_KEY);
        return checkStateDocStateAndDuration(consultant, client, fiscalYearStart, baseVersion, deltaVersion)
                .flatMap(stateDoc -> {
                    if (stateDoc.getState() == null) {
                        return Mono.empty();
                    }
                    if (stateDoc.getState() != StateDocState.DONE) {
                        return Mono.error(
                                new RestWarnException(ProcessingErrorMessageConstants.STATE_DOC_NOT_IN_DONE_STATE, HttpStatus.CONFLICT.value(),
                                                      ProcessingServiceConstants.STATE_DOC_STATE_NOT_IN_DONE));
                    }
                    return masterDataClient.getMasterDataContext(consultant, client, fiscalYearStart, baseVersion, deltaVersion, correlationId)
                                           .contextWrite(contextView)
                                           .name("importPartialData-masterDataContext")
                                           .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_ACDS)
                                           .tap(Micrometer.metrics(meterRegistry))
                                           .flatMap(mdc -> {
                                               updateBaseAndDeltaVersionInLoggingContext(contextView, mdc);
                                               return initializeFoundStateDoc(mdc.getBaseVersion(), mdc.getDeltaVersion(), stateDoc)
                                                       .flatMap(initializedStateDoc -> getImportData(mdc, initializedStateDoc));
                                           });
                });
    }

    @Override
    public Mono<UpdateResult> executeImport(ImportData importData, Mono<Boolean> importAction, String successMessage) {
        MasterdataContext mdc = importData.masterdataContext();
        return Mono.deferContextual(ctx -> importAction.flatMap(importResult -> finishImport(mdc).publishOn(Schedulers.boundedElastic())
                                                                                                 .doOnNext(updateResult -> sendChangeEvent(
                                                                                                         mdc).contextWrite(ctx).subscribe()))
                                                       .elapsed().map(LoggingUtil.logInfoWithDuration(successMessage)));
    }

    @Override
    public <T> Mono<T> handleExceptionAndUpdateStateDoc(Integer consultant, Integer client, Integer fiscalYearStart, ContextView contextView,
                                                        String errorLog, Throwable e) {
        if (e instanceof InitialLoadFailedException) {
            log.warn(LoggingUtil.MARKER, errorLog, e);
            return Mono.error(e);
        }

        String correlationId = contextView.get(LoggingUtil.CORRELATION_ID_KEY);
        ProcessingError processingError;
        if (e instanceof HttpCallException httpCallException) {
            if (e instanceof HttpCallNoContentException httpCallNoContentException) {
                return handleMasterDataNoContentException(consultant, client, fiscalYearStart, errorLog, httpCallNoContentException);
            }

            processingError = ExceptionUtil.getProcessingError(correlationId, httpCallException);
            StateDocState stateDocState = StateDocState.BAD;
            if (httpCallException instanceof HttpCallBusinessException) {
                stateDocState = StateDocState.INVALID_DATA;
            }
            log.error(LoggingUtil.MARKER, errorLog, e);
            return updateStateDocToUnsuccessfulState(consultant, client, fiscalYearStart, stateDocState, processingError).then(
                    Mono.error(httpCallException));
        }

        ProblemInfo problemInfo = ProblemInfo.builder()
                                             .title(ExceptionUtil.DEFAULT_PROBLEM_INFO_TITLE)
                                             .type(ExceptionUtil.DEFAULT_ERROR_SOURCE)
                                             .detail(e.getMessage())
                                             .build();
        processingError = ExceptionUtil.getDefaultProcessingError(correlationId, problemInfo);
        log.error(LoggingUtil.MARKER, errorLog, e);
        return updateStateDocToUnsuccessfulState(consultant, client, fiscalYearStart, StateDocState.BAD, processingError).then(
                e instanceof AggregationProcessingBaseException ?
                        Mono.error(e) :
                        Mono.error(new AggregationProcessingBusinessException(ProcessingErrorMessageConstants.IMPORT_ERROR,
                                                                              HttpStatus.INTERNAL_SERVER_ERROR.value(), e)));
    }

    @Override
    public Flux<DeleteResult> deleteImportData(Integer consultant, Integer client, Integer fiscalYear) {
        return checkStateDocStateAndDuration(consultant, client, fiscalYear, 0L, 0L)
                .flatMapMany(stateDoc -> {
                    if (stateDoc.getState() == null) {
                        return Mono.empty();
                    }
                    return initializeFoundStateDoc(stateDoc.getBaseVersion(), stateDoc.getDeltaVersion(), stateDoc)
                            .flatMapMany(updatedStateDoc -> deleteAllCollections(consultant, client, fiscalYear));
                })
                .name("deleteEvent-deleteAll")
                .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_DELETE)
                .tap(Micrometer.metrics(meterRegistry));
    }

    private static void updateBaseAndDeltaVersionInLoggingContext(ContextView contextView, MasterdataContext mdc) {
        Optional<Object> optionalLoggingContext = contextView.getOrEmpty(LoggingUtil.LOGGING_CONTEXT_KEY);
        if (optionalLoggingContext.isPresent()) {
            Map<String, String> loggingContext = (Map<String, String>) optionalLoggingContext.get();
            loggingContext.put(LoggingUtil.BASE_VERSION_KEY, mdc.getBaseVersion().toString());
            loggingContext.put(LoggingUtil.DELTA_VERSION_KEY, mdc.getDeltaVersion().toString());
        } else {
            log.warn("Could not find the logging context with key {}", LoggingUtil.LOGGING_CONTEXT_KEY);
        }
    }

    private Mono<Void> sendChangeEvent(MasterdataContext mdc) {
        return changeEventProducer.sendMessage(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(), mdc.getBaseVersion(), mdc.getDeltaVersion());
    }

    private Flux<DeleteResult> deleteAllCollections(Integer consultant, Integer client, Integer fiscalYear) {
        return Flux.merge(stateDocRepository.deleteOne(consultant, client, fiscalYear),
                          deleteCollections(consultant, client, fiscalYear));
    }

    private Mono<StateDoc> checkStateDocStateAndDuration(Integer consultant, Integer client, Integer fiscalYearStart, Long baseVersion,
                                                         Long deltaVersion) {
        return stateDocRepository.findOneByBusinessKey(consultant, client, fiscalYearStart)
                                 .defaultIfEmpty(
                                         StateDoc.builder().createdTimestamp(OffsetDateTime.now()).baseVersion(baseVersion).deltaVersion(deltaVersion)
                                                 .build())
                                 .mapNotNull(stateDoc -> {
                                     if (stateDoc != null && stateDoc.getState() != null && (stateDoc.getState() == StateDocState.INIT
                                             || stateDoc.getState() == StateDocState.REINIT)) {
                                         long importDuration = ChronoUnit.MILLIS.between(stateDoc.getStateTimestamp(), OffsetDateTime.now());
                                         if (importDuration <= initialLoadConfiguration.getMaxImportDurationInMs()) {
                                             throw new InitialLoadFailedException(
                                                     String.format(ProcessingErrorMessageConstants.ANOTHER_IMPORT_IN_PROGRESS, importDuration),
                                                     HttpStatus.CONFLICT.value(), ProcessingServiceConstants.IMPORT_IN_PROGRESS_TYPE);
                                         }
                                     }
                                     return stateDoc;
                                 });
    }

    private Mono<UpdateResult> finishImport(MasterdataContext mdc) {
        return stateDocRepository.updateToSuccessfulState(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(),
                                                          mdc.getBaseVersion(), mdc.getDeltaVersion());
    }

    private Mono<ImportData> deleteCollectionsAndInitializeImport(MasterdataContext mdc, StateDoc foundStateDoc) {
        return initializeStateDoc(mdc, foundStateDoc)
                .flatMap(stateDoc -> deleteCollections(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin())
                        .collectList()
                        .name("importData-deleteImportData")
                        .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_DELETE)
                        .tap(Micrometer.metrics(meterRegistry))
                        .flatMap(deleteResults -> getImportData(mdc, stateDoc)));
    }

    private Mono<ImportData> getImportData(MasterdataContext mdc, StateDoc stateDoc) {
        return Mono.just(new ImportData(mdc, stateDoc))
                   .name("importData-getImportData")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METHOD)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    private Mono<StateDoc> initializeStateDoc(MasterdataContext mdc, StateDoc foundStateDoc) {
        if (foundStateDoc.getState() != null) {
            return initializeFoundStateDoc(mdc.getBaseVersion(), mdc.getDeltaVersion(), foundStateDoc);
        }

        StateDoc newStateDoc = stateDocRepository.createNewStateDoc(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(), mdc.getYearEnd(),
                                                                    mdc.getBaseVersion(), mdc.getDeltaVersion(), mdc.getForceReftabCurrentYear());
        return stateDocRepository.insertOne(newStateDoc)
                                 .onErrorMap(ImportExecutionServiceImpl::handleStateDocInsertionError)
                                 .flatMap(insertOneResult -> Mono.just(newStateDoc));
    }

    private Mono<StateDoc> initializeFoundStateDoc(Long baseVersion, Long deltaVersion, StateDoc foundStateDoc) {
        return stateDocRepository.updateToInitAndFindOne(baseVersion, deltaVersion, foundStateDoc)
                                 .switchIfEmpty(Mono.error(new InitialLoadFailedException(ProcessingErrorMessageConstants.PARALLEL_IMPORT,
                                                                                          HttpStatus.CONFLICT.value(),
                                                                                          ProcessingServiceConstants.IMPORT_IN_PROGRESS_TYPE)));
    }

    private static Throwable handleStateDocInsertionError(Throwable throwable) {
        if (throwable instanceof MongoWriteException mongoWriteException &&
                mongoWriteException.getError().getCategory() == com.mongodb.ErrorCategory.DUPLICATE_KEY) {
            return new InitialLoadFailedException(ProcessingErrorMessageConstants.PARALLEL_IMPORT, HttpStatus.CONFLICT.value(),
                                                  ProcessingServiceConstants.IMPORT_IN_PROGRESS_TYPE, throwable);
        }
        return throwable;
    }

    // TODO temporary place here to remove all collections, after more refactoring each service should delete its own collection
    private Flux<DeleteResult> deleteCollections(Integer consultant, Integer client, Integer fiscalYear) {
        return Flux.merge(
                masterDataRepository.deleteOne(consultant, client, fiscalYear),
                masterDataAccountRepository.deleteManyByBusinessKey(consultant, client, fiscalYear),
                movementDataDayRepository.deleteManyByBusinessKey(consultant, client, fiscalYear),
                movementDataInventoryRepository.deleteManyByBusinessKey(consultant, client, fiscalYear),
                movementDataMonthRepository.deleteManyByBusinessKey(consultant, client, fiscalYear),
                personGroupDayRepository.deleteManyByBusinessKey(consultant, client, fiscalYear),
                personGroupMonthRepository.deleteManyByBusinessKey(consultant, client, fiscalYear),
                customColumnStructureContentRepository.deleteManyByBusinessKey(consultant, client, fiscalYear),
                customReportStructureContentRepository.deleteManyByBusinessKey(consultant, client, fiscalYear));
    }

    private <T> Mono<T> handleMasterDataNoContentException(Integer consultant, Integer client, Integer fiscalYear, String errorLog, Throwable e) {
        log.warn(LoggingUtil.MARKER, errorLog, e);
        return deleteAllCollections(consultant, client, fiscalYear).collect(Collectors.toSet())
                   .flatMap(deleteResults -> {
                       // log info if delete is not acknowledged
                       boolean isUnacknowledged = deleteResults.stream().anyMatch(deleteResult -> !deleteResult.wasAcknowledged());
                       if (isUnacknowledged) {
                           log.info(LoggingUtil.MARKER, ProcessingErrorMessageConstants.MONGODB_WRITE_UNACKNOWLEDGED);
                       }
                       return Mono.error(e);
                   });
    }

    private Mono<UpdateResult> updateStateDocToUnsuccessfulState(Integer consultant, Integer client, Integer fiscalYear, StateDocState stateDocState,
                                                                 ProcessingError processingError) {
        return stateDocRepository.updateToUnsuccessfulState(consultant, client, fiscalYear, stateDocState, processingError)
                                 .doOnNext(updateResult -> {
                                     if (updateResult.getMatchedCount() == 0) {
                                         log.error(ProcessingErrorMessageConstants.NO_STATE_DOC_EXISTS_ERROR);
                                     }
                                 });
    }
}
