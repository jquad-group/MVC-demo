package de.datev.refsys.aggregation.processing.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import de.datev.refsys.aggregation.document.model.ProblemInfo;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.boundry.event.ChangeEventProducer;
import de.datev.refsys.aggregation.processing.client.MasterDataClient;
import de.datev.refsys.aggregation.processing.config.InitialLoadConfiguration;
import de.datev.refsys.aggregation.processing.config.TestMeterConfiguration;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.config.mongo.MongoSharedConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallNoContentException;
import de.datev.refsys.aggregation.processing.exception.InitialLoadFailedException;
import de.datev.refsys.aggregation.processing.mapper.AccountDescriptionMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.AccountPurposeMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.InventoryMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.InventoryValueMapperImpl;
import de.datev.refsys.aggregation.processing.model.ImportData;
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
import de.datev.refsys.aggregation.processing.util.ClearDatabaseAnCreateIndexesBeforeEachTest;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.MongoHelperService;
import de.datev.refsys.aggregation.processing.util.ResetResilienceAfterEachTest;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static de.datev.refsys.aggregation.processing.util.TestUtil.EXCEPTION_MESSAGE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_BASE_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONTEXT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_END;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createProblemInfo;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createStateDoc;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ImportAutoConfiguration(TestMongoClientConfiguration.class)
@ContextConfiguration(classes = { TestMeterConfiguration.class, TestResilienceConfiguration.class })
@Import({ TestcontainersConfiguration.class, MongoSharedConfiguration.class, StateDocRepository.class, MasterDataRepository.class,
        MasterDataAccountRepository.class, MovementDataDayRepository.class, MovementDataMonthRepository.class,
        MovementDataPersonGroupDayRepository.class, MovementDataPersonGroupMonthRepository.class, MovementDataInventoryRepository.class,
        InitialLoadConfiguration.class, MongoHelperService.class, AccountDescriptionMapperImpl.class, AccountPurposeMapperImpl.class,
        InventoryMapperImpl.class, InventoryValueMapperImpl.class, CustomColumnStructureContentRepository.class, CustomReportStructureContentRepository.class})
@ResetResilienceAfterEachTest
@DataMongoTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles(TEST_PROFILE)
@ClearDatabaseAnCreateIndexesBeforeEachTest
class ImportExecutionServiceIT {

    @Autowired
    private MasterDataAccountRepository masterDataAccountRepository;

    @Autowired
    private MovementDataDayRepository movementDataDayRepository;

    @Autowired
    private MovementDataMonthRepository movementDataMonthRepository;

    @Autowired
    private MovementDataPersonGroupDayRepository personGroupDayRepository;

    @Autowired
    private MovementDataPersonGroupMonthRepository personGroupMonthRepository;

    @Autowired
    private MovementDataInventoryRepository movementDataInventoryRepository;

    @Autowired
    private InitialLoadConfiguration initialLoadConfiguration;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private MongoHelperService mongoHelperService;

    @Autowired
    private MasterDataRepository masterDataRepository;

    @Autowired
    private StateDocRepository stateDocRepository;

    @Autowired
    private CustomColumnStructureContentRepository customColumnStructureContentRepository;

    @Autowired
    private CustomReportStructureContentRepository customReportStructureContentRepository;

    @MockitoBean
    private MasterDataClient masterDataClient;

    @MockitoBean
    private ChangeEventProducer changeEventProducer;

    @Value("${ref-sys.update-schema.schema-version}")
    private int schemaVersion;

    private ImportExecutionService importExecutionService;
    private MemoryAppender memoryAppender;

    @BeforeEach
    void setUp() {
        importExecutionService = Mockito.spy(
                new ImportExecutionServiceImpl(stateDocRepository, masterDataRepository, masterDataAccountRepository, movementDataDayRepository,
                                               movementDataMonthRepository, personGroupDayRepository, personGroupMonthRepository,
                                               movementDataInventoryRepository, changeEventProducer, initialLoadConfiguration, masterDataClient,
                                               meterRegistry, customColumnStructureContentRepository, customReportStructureContentRepository));
        memoryAppender = setupMemoryAppender(memoryAppender, ImportExecutionServiceImpl.class, Level.DEBUG);
    }

    @Test
    @Order(1)
    @DisplayName("Should process only one initial import when five parallel requests were sent and stateDoc does exist")
    void should_process_only_one_initial_import_when_five_parallel_requests_were_sent_and_a_state_doc_does_exist() {
        // prepare
        StateDoc stateDoc = createStateDoc(StateDocState.BAD, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        mongoHelperService.insertOneStateDoc(stateDoc);
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        // execute
        OffsetDateTime timestampBefore = OffsetDateTime.now(Clock.tickSeconds(ZoneId.systemDefault())).minusSeconds(1);
        List<ImportData> result =
                Flux.merge(initializeFullImportAndAssertException(), initializeFullImportAndAssertException(), initializeFullImportAndAssertException(),
                           initializeFullImportAndAssertException(), initializeFullImportAndAssertException()).collectList().block();
        OffsetDateTime timestampAfter = OffsetDateTime.now(Clock.tickSeconds(ZoneId.systemDefault())).plusSeconds(1);
        // assert
        // 5 imports were triggered, but only 1 should be successful
        assertThat(result).hasSize(1);
        List<StateDoc> resultStateDocList = mongoHelperService.findAllStateDocs();
        assertThat(resultStateDocList).hasSize(1);
        StateDoc resultStateDoc = resultStateDocList.get(0);
        assertThat(resultStateDoc).isNotNull();
        assertThat(resultStateDoc.getProcessingError()).isNull();
        StateDoc expectedStateDoc = TestDataLoader.loadDBElement("json/collections/expected/import-service-it/init-statedoc.json", StateDoc.class);
        assertThat(resultStateDoc).usingRecursiveComparison().ignoringFieldsOfTypes(OffsetDateTime.class).isEqualTo(expectedStateDoc);
        assertThat(resultStateDoc.getStateTimestamp().withNano(0)).isBetween(timestampBefore, timestampAfter);
        assertThat(resultStateDoc.getCreatedTimestamp().withNano(0)).isBetween(timestampBefore, timestampAfter);
    }

    @Test
    @Order(2)
    @DisplayName("Should process only one initial import when five parallel requests were sent and a stateDoc does not exist")
    void should_process_only_one_initial_import_when_five_parallel_requests_were_sent_and_state_doc_does_not_exist() {
        // prepare
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        // execute
        OffsetDateTime timestampBefore = OffsetDateTime.now(Clock.tickSeconds(ZoneId.systemDefault())).minusSeconds(1);
        List<ImportData> result =
                Flux.merge(initializeFullImportAndAssertException(), initializeFullImportAndAssertException(), initializeFullImportAndAssertException(),
                           initializeFullImportAndAssertException(), initializeFullImportAndAssertException()).collectList().block();
        OffsetDateTime timestampAfter = OffsetDateTime.now(Clock.tickSeconds(ZoneId.systemDefault())).plusSeconds(1);
        // assert
        // 5 imports were triggered, but only 1 should be successful
        assertThat(result).hasSize(1);
        List<StateDoc> resultStateDocList = mongoHelperService.findAllStateDocs();
        assertThat(resultStateDocList).hasSize(1);
        StateDoc resultStateDoc = resultStateDocList.get(0);
        assertThat(resultStateDoc).isNotNull();
        assertThat(resultStateDoc.getProcessingError()).isNull();
        StateDoc expectedStateDoc = TestDataLoader.loadDBElement("json/collections/expected/import-service-it/init-statedoc.json", StateDoc.class);
        assertThat(resultStateDoc).usingRecursiveComparison().ignoringFieldsOfTypes(OffsetDateTime.class).isEqualTo(expectedStateDoc);
        assertThat(resultStateDoc.getStateTimestamp().withNano(0)).isBetween(timestampBefore, timestampAfter);
        assertThat(resultStateDoc.getCreatedTimestamp().withNano(0)).isBetween(timestampBefore, timestampAfter);
    }

    @Test
    @Order(3)
    @DisplayName("Deletes the specific import data and returns a HttpNoContent error when a HttpCallNoContentException error occurs in the MasterDataContext api")
    void should_delete_import_data_and_return_a_http_no_content_error_when_a_no_content_error_occurs() {
        // prepare
        StateDoc stateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        mongoHelperService.insertOneStateDoc(stateDoc);
        HttpCallNoContentException httpCallNoContentException =
                new HttpCallNoContentException(SourceError.ACDS, SourceEndpoint.MASTER_DATA_CONTEXT, HttpStatus.NO_CONTENT.value());
        // execute
        Throwable throwable = catchThrowable(
                () -> importExecutionService.handleExceptionAndUpdateStateDoc(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_CONTEXT,
                                                                              LoggingUtil.INITIAL_LOAD_ERROR_LOG, httpCallNoContentException).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(HttpCallNoContentException.class);
        HttpCallNoContentException expectedException = (HttpCallNoContentException) throwable;
        assertThat(expectedException).usingRecursiveAssertion().isEqualTo(httpCallNoContentException);
        List<StateDoc> resultStateDocList = mongoHelperService.findAllStateDocs();
        assertThat(resultStateDocList).isEmpty();
        List<ILoggingEvent> warnSearchResult = memoryAppender.search(ProcessingErrorMessageConstants.NO_MASTER_DATA_CONTEXT_IN_ACDS, Level.WARN);
        assertThat(warnSearchResult).hasSize(1);
        List<ILoggingEvent> infoSearchResult = memoryAppender.search(ProcessingErrorMessageConstants.MONGODB_WRITE_UNACKNOWLEDGED);
        assertThat(infoSearchResult).isEmpty();
    }

    @Test
    @Order(4)
    @DisplayName("Updates the StateDoc to INVALID state and returns a Mono error when a HttpCallBusinessException occurs")
    void should_update_state_doc_to_invalid_state_and_return_a_mono_error_when_a_http_call_business_error_occurs() {
        // prepare
        StateDoc stateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        mongoHelperService.insertOneStateDoc(stateDoc);
        ProblemInfo problemInfo = createProblemInfo();
        HttpCallBusinessException httpCallBusinessException =
                new HttpCallBusinessException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.ACCOUNT_SUM_DAYS, 555, problemInfo);
        // execute
        OffsetDateTime timestampBefore = OffsetDateTime.now(Clock.tickSeconds(ZoneId.systemDefault())).minusSeconds(1);
        Throwable throwable = catchThrowable(
                () -> importExecutionService.handleExceptionAndUpdateStateDoc(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_CONTEXT,
                                                                              LoggingUtil.INITIAL_LOAD_ERROR_LOG, httpCallBusinessException).block());
        OffsetDateTime timestampAfter = OffsetDateTime.now(Clock.tickSeconds(ZoneId.systemDefault())).plusSeconds(1);
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(HttpCallBusinessException.class);
        HttpCallBusinessException resultException = (HttpCallBusinessException) throwable;
        assertThat(resultException).usingRecursiveAssertion().isEqualTo(httpCallBusinessException);
        List<StateDoc> resultStateDocList = mongoHelperService.findAllStateDocs();
        assertThat(resultStateDocList).hasSize(1);
        StateDoc expectedStateDoc = TestDataLoader.loadDBElement("json/collections/expected/state-doc/http-call-business-state-doc.json", StateDoc.class);
        assertThat(resultStateDocList.get(0)).usingRecursiveComparison().ignoringFieldsOfTypes(OffsetDateTime.class).isEqualTo(expectedStateDoc);
        assertThat(resultStateDocList.get(0).getStateTimestamp().withNano(0)).isBetween(timestampBefore, timestampAfter);
        List<ILoggingEvent> firstSearchResult = memoryAppender.search(EXCEPTION_MESSAGE, Level.ERROR);
        assertThat(firstSearchResult).hasSize(1);
        List<ILoggingEvent> secondSearchResult = memoryAppender.search(ProcessingErrorMessageConstants.NO_STATE_DOC_EXISTS_ERROR);
        assertThat(secondSearchResult).isEmpty();
    }

    @Test
    @Order(5)
    @DisplayName("Updates the StateDoc to BAD state, increment retryCount to 2 and returns a Mono error when a AggregationProcessingBusinessException occurs twice")
    void should_update_state_doc_to_bad_state_increment_retry_count_and_return_a_mono_error_when_an_aggregation_business_error_occurs_teice() {
        // prepare
        StateDoc stateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        mongoHelperService.insertOneStateDoc(stateDoc);
        AggregationProcessingBusinessException businessException =
                new AggregationProcessingBusinessException(EXCEPTION_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        // execute
        Throwable firstCall = catchThrowable(
                () -> importExecutionService.handleExceptionAndUpdateStateDoc(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_CONTEXT,
                                                                              LoggingUtil.INITIAL_LOAD_ERROR_LOG, businessException).block());
        assertThat(firstCall).isNotNull().isInstanceOf(AggregationProcessingBusinessException.class);
        OffsetDateTime timestampBefore = OffsetDateTime.now(Clock.tickSeconds(ZoneId.systemDefault())).minusSeconds(1);
        Throwable throwable = catchThrowable(
                () -> importExecutionService.handleExceptionAndUpdateStateDoc(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_CONTEXT,
                                                                              LoggingUtil.INITIAL_LOAD_ERROR_LOG, businessException).block());
        OffsetDateTime timestampAfter = OffsetDateTime.now(Clock.tickSeconds(ZoneId.systemDefault())).plusSeconds(1);
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(AggregationProcessingBusinessException.class);
        AggregationProcessingBusinessException resultException = (AggregationProcessingBusinessException) throwable;
        assertThat(resultException).usingRecursiveAssertion().isEqualTo(businessException);
        List<StateDoc> resultStateDocList = mongoHelperService.findAllStateDocs();
        assertThat(resultStateDocList).hasSize(1);
        StateDoc expectedStateDoc = TestDataLoader.loadDBElement("json/collections/expected/state-doc/aggregation-business-state-doc.json", StateDoc.class);
        assertThat(resultStateDocList.get(0)).usingRecursiveComparison().ignoringFieldsOfTypes(OffsetDateTime.class).isEqualTo(expectedStateDoc);
        assertThat(resultStateDocList.get(0).getStateTimestamp().withNano(0)).isBetween(timestampBefore, timestampAfter);
        List<ILoggingEvent> firstSearchResult = memoryAppender.search(EXCEPTION_MESSAGE, Level.ERROR);
        assertThat(firstSearchResult).hasSize(2);
        List<ILoggingEvent> secondSearchResult = memoryAppender.search(ProcessingErrorMessageConstants.NO_STATE_DOC_EXISTS_ERROR);
        assertThat(secondSearchResult).isEmpty();
    }

    private Mono<ImportData> initializeFullImportAndAssertException() {
        return importExecutionService.initializeFullImport(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                           TEST_DELTA_VERSION, TEST_CONTEXT)
                                     .onErrorResume(throwable -> {
                                         assertThat(throwable).isInstanceOf(InitialLoadFailedException.class);
                                         return Mono.empty();
                                     });
    }

}