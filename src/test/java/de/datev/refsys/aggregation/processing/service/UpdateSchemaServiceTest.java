package de.datev.refsys.aggregation.processing.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mongodb.client.result.UpdateResult;
import de.datev.refsys.aggregation.document.model.ProcessingError;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.boundry.event.ChangeEventProducer;
import de.datev.refsys.aggregation.processing.client.MasterDataClient;
import de.datev.refsys.aggregation.processing.config.InitialLoadConfiguration;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallNoContentException;
import de.datev.refsys.aggregation.processing.model.CustomStructures;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
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
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.aggregation.processing.util.TestUtil;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.awaitility.Awaitility;
import org.bson.BsonDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static de.datev.refsys.aggregation.processing.util.TestUtil.EXCEPTION_MESSAGE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_BASE_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONTEXT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_END;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createStateDoc;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupErrorMemoryAppender;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(TestUtil.TEST_PROFILE)
class UpdateSchemaServiceTest {

    @MockitoBean
    private ImportService importService;

    @MockitoBean
    private StateDocRepository stateDocRepository;

    @MockitoBean
    private MasterDataRepository masterDataRepository;

    @MockitoBean
    private MasterDataAccountRepository masterDataAccountRepository;

    @MockitoBean
    private MovementDataDayRepository movementDataDayRepository;

    @MockitoBean
    private MovementDataMonthRepository movementDataMonthRepository;

    @MockitoBean
    private MovementDataPersonGroupDayRepository personGroupDayRepository;

    @MockitoBean
    private MovementDataPersonGroupMonthRepository personGroupMonthRepository;

    @MockitoBean
    private MovementDataInventoryRepository movementDataInventoryRepository;

    @MockitoBean
    private CustomColumnStructureContentRepository customColumnStructureContentRepository;

    @MockitoBean
    private CustomReportStructureContentRepository customReportStructureContentRepository;

    @MockitoBean
    private MasterDataClient masterDataClient;

    @MockitoBean
    private CustomStructuresService customStructuresService;

    @MockitoBean
    private ChangeEventProducer changeEventProducer;

    private ImportExecutionServiceImpl importExecutionService;
    private MemoryAppender errorMemoryAppender;
    private MemoryAppender updateSchemaServiceMemoryAppender;
    private MemoryAppender loggingUtilMemoryAppender;
    private MemoryAppender importExecutionServiceMemoryAppender;

    @BeforeEach
    void setUp() {
        when(changeEventProducer.sendMessage(anyInt(), anyInt(), anyInt(), anyLong(), any())).thenReturn(Mono.empty());
        this.importExecutionService =
                new ImportExecutionServiceImpl(stateDocRepository, masterDataRepository, masterDataAccountRepository, movementDataDayRepository,
                                               movementDataMonthRepository, personGroupDayRepository, personGroupMonthRepository,
                                               movementDataInventoryRepository, changeEventProducer, new InitialLoadConfiguration(), masterDataClient,
                                               new SimpleMeterRegistry(), customColumnStructureContentRepository,
                                               customReportStructureContentRepository);
        this.updateSchemaServiceMemoryAppender = setupMemoryAppender(updateSchemaServiceMemoryAppender, UpdateSchemaServiceImpl.class, Level.INFO);
        this.loggingUtilMemoryAppender = setupMemoryAppender(loggingUtilMemoryAppender, LoggingUtil.class, Level.DEBUG);
        this.importExecutionServiceMemoryAppender = setupMemoryAppender(importExecutionServiceMemoryAppender, ImportExecutionServiceImpl.class, Level.INFO);
        this.errorMemoryAppender = setupErrorMemoryAppender(errorMemoryAppender);
        UpdateResult updateResult = UpdateResult.acknowledged(1, 1L, new BsonDocument());
        when(stateDocRepository.updateToSuccessfulState(anyInt(), anyInt(), anyInt(), anyLong(), anyLong())).thenReturn(
                Mono.just(updateResult));
        mockRepositoryDeletes();
    }

    @Test
    @DisplayName("Should start full import if the schema version is out of date")
    void should_execute_full_import_if_schema_version_out_of_date() {
        // prepare
        int applicationSchemaVersion = 10;
        UpdateSchemaService updateSchemaService =
                new UpdateSchemaServiceImpl(stateDocRepository, masterDataRepository, importService, importExecutionService,
                                            customStructuresService, applicationSchemaVersion);
        int stateDocSchemaVersion = 0;
        StateDoc initialStateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, stateDocSchemaVersion);
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.just(initialStateDoc));
        StateDoc updatedStateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, stateDocSchemaVersion);
        when(stateDocRepository.updateToInitAndFindOne(any(), any(), any())).thenReturn(Mono.just(updatedStateDoc));
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        when(importService.executeFullImport(any())).thenReturn(Mono.just(true));
        // execute
        updateSchemaService.updateSchemaVersion(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION, TEST_DELTA_VERSION)
                           .contextWrite(TEST_CONTEXT)
                           .block();
        // assert
        Awaitility.await().timeout(20L, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(stateDocRepository, times(1)).updateToSuccessfulState(anyInt(), anyInt(), anyInt(), anyLong(), anyLong());
            verify(importService, times(1)).executeFullImport(any());
            verify(stateDocRepository, never()).updateFromSchemaVersionOneToFour(anyInt(), anyInt(), anyInt(), anyBoolean());
            verify(masterDataRepository, never()).updateFromSchemaVersionOneToFour(anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt(), any(),
                                                                                   any());
            assertThat(importExecutionServiceMemoryAppender.search(LoggingUtil.PARTIAL_IMPORT_STARTED_LOG, Level.INFO)).hasSize(1);
            List<ILoggingEvent> loggingUtilSearchResult = loggingUtilMemoryAppender.search(LoggingUtil.PARTIAL_IMPORT_SUCCESS_LOG.replace(" {}ms", ""), Level.INFO);
            assertThat(loggingUtilSearchResult).hasSize(1);
            // assert that no exceptions occurred since UpdateSchemaService.updateSchemaVersion is an async method call
            List<ILoggingEvent> errorSearchResult = errorMemoryAppender.search(Level.ERROR);
            assertThat(errorSearchResult).isEmpty();
        });
    }

    @Test
    @DisplayName("Should not execute schema update if the schema versions are the same")
    void should_not_execute_schema_update_if_schema_versions_are_same() {
        // prepare
        int applicationSchemaVersion = 0;
        UpdateSchemaService updateSchemaService =
                new UpdateSchemaServiceImpl(stateDocRepository, masterDataRepository, importService, importExecutionService,
                                            customStructuresService, applicationSchemaVersion);
        int stateDocSchemaVersion = 0;
        StateDoc initialStateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, stateDocSchemaVersion);
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.just(initialStateDoc));
        StateDoc updatedStateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, stateDocSchemaVersion);
        when(stateDocRepository.updateToInitAndFindOne(any(), any(), any())).thenReturn(Mono.just(updatedStateDoc));
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        // execute
        updateSchemaService.updateSchemaVersion(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION, TEST_DELTA_VERSION)
                           .contextWrite(TEST_CONTEXT)
                           .block();
        // assert
        Awaitility.await().timeout(20L, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(stateDocRepository, times(1)).updateToSuccessfulState(anyInt(), anyInt(), anyInt(), anyLong(), anyLong());
            verify(importService, never()).executeFullImport(any());
            verify(stateDocRepository, never()).updateFromSchemaVersionOneToFour(anyInt(), anyInt(), anyInt(), anyBoolean());
            verify(masterDataRepository, never()).updateFromSchemaVersionOneToFour(anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt(), any(),
                                                                                   any());
            List<ILoggingEvent> loggingUtilSearchResult =
                    updateSchemaServiceMemoryAppender.search(LoggingUtil.UPDATE_SCHEMA_NOT_NEEDED_LOG, Level.INFO);
            assertThat(loggingUtilSearchResult).hasSize(1);
            // assert that no exceptions occurred since UpdateSchemaService.updateSchemaVersion is an async method call
            List<ILoggingEvent> errorSearchResult = errorMemoryAppender.search(Level.ERROR);
            assertThat(errorSearchResult).isEmpty();
        });
    }

    @ParameterizedTest
    @CsvSource(value = { "1", "2", "3" })
    @DisplayName("Should update schema version from 1 to 4 or 2 to 4 or 3 to 4")
    void should_update_schema_version_from_one_or_two_or_three_to_four(int stateDocSchemaVersion) {
        // prepare
        int applicationSchemaVersion = 4;
        UpdateSchemaService updateSchemaService =
                new UpdateSchemaServiceImpl(stateDocRepository, masterDataRepository, importService, importExecutionService,
                                            customStructuresService, applicationSchemaVersion);
        StateDoc initialStateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, stateDocSchemaVersion);
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.just(initialStateDoc));
        StateDoc updatedStateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, stateDocSchemaVersion);
        when(stateDocRepository.updateToInitAndFindOne(any(), any(), any())).thenReturn(Mono.just(updatedStateDoc));
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        masterDataContext.containsNearTimeData(true);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        UpdateResult updateResult = UpdateResult.acknowledged(1, 1L, new BsonDocument());
        when(stateDocRepository.updateFromSchemaVersionOneToFour(anyInt(), anyInt(), anyInt(), anyBoolean())).thenReturn(Mono.just(updateResult));
        when(masterDataRepository.updateFromSchemaVersionOneToFour(anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt(), any(), any()))
                .thenReturn(Mono.just(updateResult));
        when(customStructuresService.insertCustomReportStructureContents(any())).thenReturn(Mono.empty());
        when(customStructuresService.insertCustomColumnStructureContents(any())).thenReturn(Mono.empty());
        // execute
        updateSchemaService.updateSchemaVersion(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION, TEST_DELTA_VERSION)
                           .contextWrite(TEST_CONTEXT)
                           .block();
        // assert
        Awaitility.await().timeout(20L, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(stateDocRepository, times(1)).updateToSuccessfulState(anyInt(), anyInt(), anyInt(), anyLong(), anyLong());
            verify(importService, never()).executeFullImport(any());
            verify(stateDocRepository, times(1)).updateFromSchemaVersionOneToFour(anyInt(), anyInt(), anyInt(), anyBoolean());
            verify(masterDataRepository, times(1)).updateFromSchemaVersionOneToFour(anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt(), any(),
                                                                                    any());
            List<ILoggingEvent> loggingUtilSearchResult = loggingUtilMemoryAppender.search(LoggingUtil.PARTIAL_IMPORT_SUCCESS_LOG.replace(" {}ms", ""), Level.INFO);
            assertThat(loggingUtilSearchResult).hasSize(1);
            // assert that no exceptions occurred since UpdateSchemaService.updateSchemaVersion is an async method call
            List<ILoggingEvent> errorSearchResult = errorMemoryAppender.search(Level.ERROR);
            assertThat(errorSearchResult).isEmpty();
        });
    }

    @Test
    @DisplayName("Should handle business errors during schema update")
    void should_handle_exception_during_update_schema_execution() {
        // prepare
        int applicationSchemaVersion = 10;
        UpdateSchemaService updateSchemaService =
                new UpdateSchemaServiceImpl(stateDocRepository, masterDataRepository, importService, importExecutionService,
                                            customStructuresService, applicationSchemaVersion);
        int stateDocSchemaVersion = 0;
        StateDoc initialStateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, stateDocSchemaVersion);
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.just(initialStateDoc));
        StateDoc updatedStateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, stateDocSchemaVersion);
        when(stateDocRepository.updateToInitAndFindOne(any(), any(), any())).thenReturn(Mono.just(updatedStateDoc));
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        when(importService.executeFullImport(any())).thenReturn(
                Mono.error(new AggregationProcessingBusinessException(EXCEPTION_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR.value())));
        UpdateResult updateResult = UpdateResult.acknowledged(1, 1L, new BsonDocument());
        when(stateDocRepository.updateToUnsuccessfulState(anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(Mono.just(updateResult));
        // execute
        updateSchemaService.updateSchemaVersion(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION, TEST_DELTA_VERSION)
                           .contextWrite(TEST_CONTEXT)
                           .block();
        // assert
        Awaitility.await().timeout(20L, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(stateDocRepository, never()).updateToSuccessfulState(anyInt(), anyInt(), anyInt(), anyLong(), anyLong());
            verify(importService, times(1)).executeFullImport(any());
            verify(stateDocRepository, never()).updateFromSchemaVersionOneToFour(anyInt(), anyInt(), anyInt(), anyBoolean());
            verify(masterDataRepository, never()).updateFromSchemaVersionOneToFour(anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt(), any(),
                                                                                   any());
            verify(stateDocRepository, times(1)).updateToUnsuccessfulState(anyInt(), anyInt(), anyInt(), any(), any());
            // assert that no exceptions occurred since UpdateSchemaService.updateSchemaVersion is an async method call
            List<ILoggingEvent> errorSearchResult = errorMemoryAppender.search(LoggingUtil.UPDATE_SCHEMA_ERROR_LOG, Level.ERROR);
            assertThat(errorSearchResult).hasSize(1);
        });
    }

    @Test
    @DisplayName("Should mark StateDoc as BAD and rethrow HttpCallNoContentException during schema update (non-null ContextView)")
    void should_handle_NoContentException_and_propagate_with_nonNullContext() {
        // vorbereiten
        int applicationSchemaVersion = 10;
        // ImportExecutionService mocken, damit initializePartialImport(...) nicht die reale Methode aufruft
        ImportExecutionService importExecutionServiceMock = mock(ImportExecutionService.class);

        UpdateSchemaService updateSchemaService =
                new UpdateSchemaServiceImpl(
                        stateDocRepository,
                        masterDataRepository,
                        importService,
                        importExecutionServiceMock,  // hier kommt der Mock rein
                        customStructuresService,
                        applicationSchemaVersion
                );

        // initializePartialImport() soll sofort eine HttpCallNoContentException werfen
        HttpCallNoContentException noContentEx = new HttpCallNoContentException(
                SourceError.ACDS,
                SourceEndpoint.MASTER_DATA_CONTEXT,
                HttpStatus.NO_CONTENT.value()
        );
        when(importExecutionServiceMock.initializePartialImport(
                anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any(ContextView.class)))
                .thenReturn(Mono.error(noContentEx));

        // Stub für updateToUnsuccessfulState, um ein "acknowledged" UpdateResult zu liefern
        UpdateResult mongoResult = UpdateResult.acknowledged(1L, 0L, null);
        when(stateDocRepository.updateToUnsuccessfulState(
                anyInt(), anyInt(), anyInt(), any(StateDocState.class), any(ProcessingError.class)))
                .thenReturn(Mono.just(mongoResult));

        // Reactor-Context mit einer Korrelations-ID
        Context reactorContext = Context.of(LoggingUtil.CORRELATION_ID_KEY, "test-correlation-id");

        // execute & assert: updateSchemaVersion() soll eine HttpCallNoContentException zurückgeben
        StepVerifier.create(
                            updateSchemaService.updateSchemaVersion(
                                                       TEST_CONSULTANT,
                                                       TEST_CLIENT,
                                                       TEST_FISCAL_YEAR_2021_START,
                                                       TEST_BASE_VERSION,
                                                       TEST_DELTA_VERSION
                                                                   )
                                               .contextWrite(reactorContext)
                           )
                    .expectErrorMatches(throwable ->
                                                throwable instanceof HttpCallNoContentException
                                                        && throwable == noContentEx)
                    .verify();

        // Verifizieren: updateToUnsuccessfulState wurde genau einmal aufgerufen, updateToSuccessfulState nie
        verify(stateDocRepository, times(1)).updateToUnsuccessfulState(
                eq(TEST_CONSULTANT),
                eq(TEST_CLIENT),
                eq(TEST_FISCAL_YEAR_2021_START),
                eq(StateDocState.BAD),
                any(ProcessingError.class));

        verify(stateDocRepository, never()).updateToSuccessfulState(anyInt(), anyInt(), anyInt(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Should handle unexpected errors during schema update exception handling")
    void should_handle_unexpected_exception_during_update_schema_exception_handling() {
        // prepare
        int applicationSchemaVersion = 10;
        UpdateSchemaService updateSchemaService =
                new UpdateSchemaServiceImpl(stateDocRepository, masterDataRepository, importService, importExecutionService,
                                            customStructuresService, applicationSchemaVersion);
        int stateDocSchemaVersion = 0;
        StateDoc initialStateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, stateDocSchemaVersion);
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.just(initialStateDoc));
        StateDoc updatedStateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, stateDocSchemaVersion);
        when(stateDocRepository.updateToInitAndFindOne(any(), any(), any())).thenReturn(Mono.just(updatedStateDoc));
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        when(importService.executeFullImport(any())).thenReturn(
                Mono.error(new AggregationProcessingBusinessException(EXCEPTION_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR.value())));
        when(stateDocRepository.updateToUnsuccessfulState(anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(
                Mono.error(new RuntimeException(EXCEPTION_MESSAGE)));
        // execute
        updateSchemaService.updateSchemaVersion(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION, TEST_DELTA_VERSION)
                           .contextWrite(TEST_CONTEXT)
                           .block();
        // assert
        Awaitility.await().timeout(5L, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(stateDocRepository, never()).updateToSuccessfulState(anyInt(), anyInt(), anyInt(), anyLong(), anyLong());
            verify(importService, times(1)).executeFullImport(any());
            verify(stateDocRepository, never()).updateFromSchemaVersionOneToFour(anyInt(), anyInt(), anyInt(), anyBoolean());
            verify(masterDataRepository, never()).updateFromSchemaVersionOneToFour(anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt(), any(),
                                                                                   any());
            verify(stateDocRepository, times(1)).updateToUnsuccessfulState(anyInt(), anyInt(), anyInt(), any(), any());
            // assert that no exceptions occurred since UpdateSchemaService.updateSchemaVersion is an async method call
            List<ILoggingEvent> errorSearchResult =
                    errorMemoryAppender.search(ProcessingErrorMessageConstants.UNEXPECTED_ERROR_IN_EXCEPTION_HANDLING, Level.ERROR);
            assertThat(errorSearchResult).hasSize(1);
        });
    }

    private void mockRepositoryDeletes() {
        when(masterDataRepository.deleteOne(any(), any(), any())).thenReturn(Mono.empty());
        when(masterDataAccountRepository.deleteManyByBusinessKey(any(), any(), any())).thenReturn(Mono.empty());
        when(movementDataDayRepository.deleteManyByBusinessKey(any(), any(), any())).thenReturn(Mono.empty());
        when(movementDataInventoryRepository.deleteManyByBusinessKey(any(), any(), any())).thenReturn(Mono.empty());
        when(movementDataMonthRepository.deleteManyByBusinessKey(any(), any(), any())).thenReturn(Mono.empty());
        when(personGroupDayRepository.deleteManyByBusinessKey(any(), any(), any())).thenReturn(Mono.empty());
        when(personGroupMonthRepository.deleteManyByBusinessKey(any(), any(), any())).thenReturn(Mono.empty());
        when(customColumnStructureContentRepository.deleteManyByBusinessKey(any(), any(), any())).thenReturn(Mono.empty());
        when(customReportStructureContentRepository.deleteManyByBusinessKey(any(), any(), any())).thenReturn(Mono.empty());
        when(customStructuresService.getCustomStructures(any())).thenReturn(
                Mono.just(new CustomStructures(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList())));
    }
}