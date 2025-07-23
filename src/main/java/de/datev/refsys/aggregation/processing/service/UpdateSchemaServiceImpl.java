package de.datev.refsys.aggregation.processing.service;

import de.datev.refsys.aggregation.document.model.ProcessingError;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBaseException;
import de.datev.refsys.aggregation.processing.exception.HttpCallNoContentException;
import de.datev.refsys.aggregation.processing.model.CustomStructures;
import de.datev.refsys.aggregation.processing.model.ImportData;
import de.datev.refsys.aggregation.processing.repository.MasterDataRepository;
import de.datev.refsys.aggregation.processing.repository.StateDocRepository;
import de.datev.refsys.aggregation.processing.util.ExceptionUtil;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
public class UpdateSchemaServiceImpl implements UpdateSchemaService {
    private final int currentSchemaVersion;
    private final ImportService importService;
    private final ImportExecutionService importExecutionService;
    private final StateDocRepository stateDocRepository;
    private final MasterDataRepository masterDataRepository;
    private final CustomStructuresService customStructuresService;

    public UpdateSchemaServiceImpl(StateDocRepository stateDocRepository, MasterDataRepository masterDataRepository, ImportService importService,
                                   ImportExecutionService importExecutionService, CustomStructuresService customStructuresService,
                                   @Value("${ref-sys.update-schema.schema-version}") int schemaVersion) {
        this.stateDocRepository = stateDocRepository;
        this.masterDataRepository = masterDataRepository;
        this.importService = importService;
        this.importExecutionService = importExecutionService;
        this.currentSchemaVersion = schemaVersion;
        this.customStructuresService = customStructuresService;
    }

    @Override
    public Mono<Boolean> updateSchemaVersion(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion) {
        return Mono.deferContextual(
                ctx -> importExecutionService.initializePartialImport(consultant, client, fiscalYear, baseVersion, deltaVersion, ctx)
                                             .publishOn(Schedulers.boundedElastic())
                                             .doOnNext(processSchemaUpdate(consultant, client, fiscalYear, ctx))
                                             .flatMap(importData -> Mono.just(true))
                                             // fange hier gezielt HttpCallNoContentException ab
                                             .onErrorResume(HttpCallNoContentException.class, ex -> {
                                                 // hole KorrelationId (falls im Context hinterlegt)
                                                 String correlationId = ctx.getOrDefault(LoggingUtil.CORRELATION_ID_KEY, UUID.randomUUID().toString());
                                                 // baue daraus ein ProcessingError-Objekt
                                                 ProcessingError error = ExceptionUtil.getProcessingError(correlationId, ex);

                                                 // setze StateDoc auf BAD und werfe danach die ursprüngliche Ausnahme wieder
                                                 return stateDocRepository
                                                         .updateToUnsuccessfulState(
                                                                 consultant,
                                                                 client,
                                                                 fiscalYear,
                                                                 StateDocState.BAD,
                                                                 error)
                                                         .then(Mono.error(ex));
                                             })
                                   );
    }

    private Consumer<ImportData> processSchemaUpdate(Integer consultant, Integer client, Integer fiscalYear, ContextView ctx) {
        return importData -> {
            // if the schema_version field in StateDoc is null, then take 0 as default value
            Integer stateDocSchemaVersion = Optional.ofNullable(importData.stateDoc().getSchemaVersion()).orElse(0);
            MasterdataContext masterdataContext = importData.masterdataContext();
            if (!stateDocSchemaVersion.equals(currentSchemaVersion)) {
                Mono<Boolean> updateSchema;
                if (currentSchemaVersion == 4 && (stateDocSchemaVersion.equals(1) || stateDocSchemaVersion.equals(2)
                        || stateDocSchemaVersion.equals(3))) {
                    updateSchema = customStructuresService.getCustomStructures(masterdataContext)
                                                          .flatMap(cs -> updateFromSchemaVersionOneToFour(consultant, client, fiscalYear,
                                                                                                          masterdataContext, cs));
                } else {
                    updateSchema = importService.executeFullImport(importData);
                }
                executeUpdateSchema(importData, updateSchema, ctx);
            } else {
                log.info(LoggingUtil.MARKER, LoggingUtil.UPDATE_SCHEMA_NOT_NEEDED_LOG);
                stateDocRepository.updateToSuccessfulState(masterdataContext.getConsultant(),
                                                           masterdataContext.getClient(),
                                                           masterdataContext.getYearBegin(),
                                                           masterdataContext.getBaseVersion(),
                                                           masterdataContext.getDeltaVersion()).subscribe();
            }
        };
    }

    private Mono<Boolean> updateFromSchemaVersionOneToFour(Integer consultant, Integer client, Integer fiscalYear, MasterdataContext mdc,
                                                           CustomStructures customStructures) {
        return Flux.merge(stateDocRepository.updateFromSchemaVersionOneToFour(consultant, client, fiscalYear, mdc.getForceReftabCurrentYear()),
                          masterDataRepository.updateFromSchemaVersionOneToFour(consultant, client, fiscalYear, mdc.getContainsNearTimeData(),
                                                                                mdc.getIndustryId(), customStructures.customReportStructureInfos(),
                                                                                customStructures.customColumnStructureInfos()),
                          customStructuresService.insertCustomReportStructureContents(customStructures.customReportStructureContents()),
                          customStructuresService.insertCustomColumnStructureContents(customStructures.customColumnStructureContents()))
                   .collectList()
                   .flatMap(updateResult -> Mono.just(true));
    }

    private Disposable executeUpdateSchema(ImportData importData, Mono<Boolean> updateSchema, ContextView ctx) {
        return importExecutionService.executeImport(importData, updateSchema, LoggingUtil.PARTIAL_IMPORT_SUCCESS_LOG)
                                     .contextWrite(ctx)
                                     .onErrorResume(
                                             e -> importExecutionService.handleExceptionAndUpdateStateDoc(
                                                     importData.masterdataContext().getConsultant(),
                                                     importData.masterdataContext().getClient(),
                                                     importData.masterdataContext().getYearBegin(),
                                                     ctx, LoggingUtil.UPDATE_SCHEMA_ERROR_LOG, e)).contextWrite(ctx)
                                     .onErrorResume(e -> {
                                         if (!(e instanceof AggregationProcessingBaseException)) {
                                             log.error(ProcessingErrorMessageConstants.UNEXPECTED_ERROR_IN_EXCEPTION_HANDLING, e);
                                         }
                                         return Mono.empty();
                                     }).contextWrite(ctx)
                                     .subscribe();
    }
}
