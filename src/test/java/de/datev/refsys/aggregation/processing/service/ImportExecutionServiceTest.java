package de.datev.refsys.aggregation.processing.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteError;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import de.datev.refsys.aggregation.document.model.ProblemInfo;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.boundry.event.ChangeEventProducer;
import de.datev.refsys.aggregation.processing.client.MasterDataClient;
import de.datev.refsys.aggregation.processing.config.InitialLoadConfiguration;
import de.datev.refsys.aggregation.processing.configuration.InitialLoadContextConfiguration;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallNoContentException;
import de.datev.refsys.aggregation.processing.exception.HttpCallTechnicalException;
import de.datev.refsys.aggregation.processing.exception.InitialLoadFailedException;
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
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.aggregation.processing.util.TestUtil;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.bson.BsonDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.ANOTHER_IMPORT_IN_PROGRESS;
import static de.datev.refsys.aggregation.processing.util.TestUtil.EXCEPTION_MESSAGE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_BASE_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONTEXT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_TYPE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createProblemInfo;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class, classes = InitialLoadContextConfiguration.class)
@ActiveProfiles(TestUtil.TEST_PROFILE)
class ImportExecutionServiceTest {

    @Autowired
    private InitialLoadConfiguration initialLoadConfiguration;

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
    private ChangeEventProducer changeEventProducer;

    private ImportExecutionService importExecutionService;
    private MemoryAppender memoryAppender;

    @BeforeEach
    void setUp() {
        this.memoryAppender = setupMemoryAppender(memoryAppender, ImportExecutionServiceImpl.class, Level.DEBUG);
        this.importExecutionService =
                new ImportExecutionServiceImpl(stateDocRepository, masterDataRepository, masterDataAccountRepository, movementDataDayRepository,
                                               movementDataMonthRepository, personGroupDayRepository, personGroupMonthRepository,
                                               movementDataInventoryRepository, changeEventProducer, initialLoadConfiguration, masterDataClient,
                                               new SimpleMeterRegistry(), customColumnStructureContentRepository, customReportStructureContentRepository);
        mockRepositoryDeletes();
    }

    @Test
    @DisplayName("Returns an import data object and initializes an import when the StateDoc is in DONE state")
    void should_return_an_import_data_object_when_state_doc_is_in_done_state() {
        // prepare
        StateDoc initialStateDoc = StateDoc.builder()
                                           .consultant(TEST_CONSULTANT)
                                           .client(TEST_CLIENT)
                                           .yearBegin(TEST_FISCAL_YEAR_2021_START)
                                           .state(StateDocState.DONE)
                                           .stateTimestamp(OffsetDateTime.now())
                                           .build();
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.just(initialStateDoc));
        StateDoc updatedStateDoc = StateDoc.builder()
                                           .consultant(TEST_CONSULTANT)
                                           .client(TEST_CLIENT)
                                           .yearBegin(TEST_FISCAL_YEAR_2021_START)
                                           .state(StateDocState.INIT)
                                           .stateTimestamp(OffsetDateTime.now())
                                           .build();
        when(stateDocRepository.updateToInitAndFindOne(any(), any(), any())).thenReturn(Mono.just(updatedStateDoc));
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        // execute
        ImportData importData = importExecutionService.initializeFullImport(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                                            TEST_DELTA_VERSION, TEST_CONTEXT).contextWrite(TestUtil.TEST_CONTEXT).block();
        // assert
        assertThat(importData).isNotNull();
        assertThat(importData.masterdataContext()).isNotNull().isEqualTo(masterDataContext);
        assertThat(importData.stateDoc()).isNotNull().isEqualTo(updatedStateDoc);
    }

    @Test
    @DisplayName("Returns an error that the initial load failed to start when the StateDoc is in INIT state and the maxImportDuration wasn't exceeded")
    void should_return_an_initial_load_failed_error_when_state_is_init_and_max_import_duration_was_not_exceeded() {
        // prepare
        StateDoc stateDoc = StateDoc.builder()
                                    .consultant(TEST_CONSULTANT)
                                    .client(TEST_CLIENT)
                                    .yearBegin(TEST_FISCAL_YEAR_2021_START)
                                    .state(StateDocState.INIT)
                                    .stateTimestamp(OffsetDateTime.now())
                                    .build();
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.just(stateDoc));
        // execute
        Throwable throwable = catchThrowable(
                () -> importExecutionService.initializeFullImport(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                                  TEST_DELTA_VERSION, TEST_CONTEXT)
                                            .contextWrite(TEST_CONTEXT)
                                            .block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(InitialLoadFailedException.class);
        InitialLoadFailedException initialLoadException = (InitialLoadFailedException) throwable;
        assertThat(initialLoadException.getHttpStatusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        String[] split = ANOTHER_IMPORT_IN_PROGRESS.split("%s");
        assertThat(split).hasSize(2);
        assertThat(initialLoadException.getMessage()).contains(split[0]);
        assertThat(initialLoadException.getMessage()).contains(split[1]);
    }

    @Test
    @DisplayName("Returns an import data object and initializes an import when the StateDoc is in INIT state and the maxImportDuration was exceeded")
    void should_return_an_import_data_object_when_state_doc_is_in_init_state_and_max_import_duration_was_exceeded() {
        // prepare
        StateDoc initialStateDoc = StateDoc.builder()
                                           .consultant(TEST_CONSULTANT)
                                           .client(TEST_CLIENT)
                                           .yearBegin(TEST_FISCAL_YEAR_2021_START)
                                           .state(StateDocState.INIT)
                                           .stateTimestamp(OffsetDateTime.now().minusSeconds(10))
                                           .build();
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.just(initialStateDoc));
        StateDoc updatedStateDoc = StateDoc.builder()
                                           .consultant(TEST_CONSULTANT)
                                           .client(TEST_CLIENT)
                                           .yearBegin(TEST_FISCAL_YEAR_2021_START)
                                           .state(StateDocState.INIT)
                                           .stateTimestamp(OffsetDateTime.now())
                                           .build();
        when(stateDocRepository.updateToInitAndFindOne(any(), any(), any())).thenReturn(Mono.just(updatedStateDoc));
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        // execute
        ImportData importData = importExecutionService.initializeFullImport(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                                            TEST_DELTA_VERSION, TEST_CONTEXT).contextWrite(TestUtil.TEST_CONTEXT).block();
        // assert
        assertThat(importData).isNotNull();
        assertThat(importData.masterdataContext()).isNotNull().isEqualTo(masterDataContext);
        assertThat(importData.stateDoc()).isNotNull().isEqualTo(updatedStateDoc);
    }

    @Test
    @DisplayName("Returns a http call error when masterDataContext api returns a bad request")
    void should_return_a_http_call_error_when_master_data_context_returns_bad_request() {
        // prepare
        StateDoc initialStateDoc = StateDoc.builder()
                                           .consultant(TEST_CONSULTANT)
                                           .client(TEST_CLIENT)
                                           .yearBegin(TEST_FISCAL_YEAR_2021_START)
                                           .state(StateDocState.INIT)
                                           .stateTimestamp(OffsetDateTime.now().minusSeconds(10))
                                           .build();
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.just(initialStateDoc));
        ProblemInfo problemInfo = createProblemInfo();
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(
                        new HttpCallTechnicalException(EXCEPTION_MESSAGE, SourceError.PROCESSING_SERVICE, SourceEndpoint.MASTER_DATA_CONTEXT,
                                                       HttpStatus.BAD_REQUEST.value(), problemInfo)));
        // execute
        Throwable throwable = catchThrowable(
                () -> importExecutionService.initializeFullImport(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                                  TEST_DELTA_VERSION, TEST_CONTEXT).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(HttpCallTechnicalException.class);
        HttpCallTechnicalException httpCallException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallException.getMessage()).isEqualTo(EXCEPTION_MESSAGE);
        assertThat(httpCallException.getSource()).isEqualTo(SourceError.PROCESSING_SERVICE);
        assertThat(httpCallException.getHttpStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(httpCallException.getSourceEndpoint().getValue()).hasToString(SourceEndpoint.MASTER_DATA_CONTEXT.getValue());
        assertThat(httpCallException.getProblemInfo()).isNotNull().usingRecursiveComparison().isEqualTo(problemInfo);
    }

    @Test
    @DisplayName("Returns the same error that the mongo api threw when an error happens that isn't MongoWriteException")
    void should_return_the_same_error_mongo_returned_when_insertion_has_an_error_that_is_not_a_mongo_exception() {
        // prepare
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.empty());
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        when(stateDocRepository.insertOne(any())).thenReturn(Mono.error(new RuntimeException(EXCEPTION_MESSAGE)));
        // execute
        Throwable throwable = catchThrowable(
                () -> importExecutionService.initializeFullImport(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                                  TEST_DELTA_VERSION, TEST_CONTEXT).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(RuntimeException.class);
        RuntimeException runtimeException = (RuntimeException) throwable;
        assertThat(runtimeException.getMessage()).isEqualTo(EXCEPTION_MESSAGE);
    }

    @Test
    @DisplayName("Returns the same error that the mongo api threw when a MongoWriteException error happens and the cause is not duplicate key")
    void should_return_the_same_error_mongo_returned_when_insertion_returns_a_mongo_exception_and_the_cause_is_not_duplicate_key() {
        // prepare
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.empty());
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        WriteError writeError = new WriteError(50, EXCEPTION_MESSAGE, new BsonDocument());
        when(stateDocRepository.insertOne(any())).thenReturn(Mono.error(new MongoWriteException(writeError, new ServerAddress(), new ArrayList<>())));
        // execute
        Throwable throwable = catchThrowable(
                () -> importExecutionService.initializeFullImport(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                                  TEST_DELTA_VERSION, TEST_CONTEXT).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(MongoWriteException.class);
        MongoWriteException mongoException = (MongoWriteException) throwable;
        assertThat(mongoException.getError().getMessage()).isEqualTo(EXCEPTION_MESSAGE);
        assertThat(mongoException.getError().getCategory()).isEqualTo(ErrorCategory.EXECUTION_TIMEOUT);
    }

    @Test
    @DisplayName("Returns an initial load failed error when a MongoWriteException error happens and the cause is duplicate key")
    void should_return_an_initial_load_failed_error_when_insertion_returns_a_mongo_exception_and_the_cause_is_duplicate_key() {
        // prepare
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.empty());
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        WriteError writeError = new WriteError(11000, EXCEPTION_MESSAGE, new BsonDocument());
        when(stateDocRepository.insertOne(any())).thenReturn(Mono.error(new MongoWriteException(writeError, new ServerAddress(), new ArrayList<>())));
        // execute
        Throwable throwable = catchThrowable(
                () -> importExecutionService.initializeFullImport(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                                  TEST_DELTA_VERSION, TEST_CONTEXT).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(InitialLoadFailedException.class);
        InitialLoadFailedException loadFailedException = (InitialLoadFailedException) throwable;
        assertThat(loadFailedException.getMessage()).isEqualTo(ProcessingErrorMessageConstants.PARALLEL_IMPORT);
        assertThat(loadFailedException.getHttpStatusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(loadFailedException.getCause()).isNotNull().isInstanceOf(MongoWriteException.class);
        MongoWriteException mongoWriteException = (MongoWriteException) loadFailedException.getCause();
        assertThat(mongoWriteException.getError().getMessage()).isEqualTo(EXCEPTION_MESSAGE);
        assertThat(mongoWriteException.getError().getCategory()).isEqualTo(ErrorCategory.DUPLICATE_KEY);
    }

    @Test
    @DisplayName("Returns an import data object and initializes a partial import when the StateDoc is in DONE state")
    void should_return_an_import_data_for_partial_import_object_when_state_doc_is_in_done_state() {
        // prepare
        StateDoc initialStateDoc = StateDoc.builder()
                                           .consultant(TEST_CONSULTANT)
                                           .client(TEST_CLIENT)
                                           .yearBegin(TEST_FISCAL_YEAR_2021_START)
                                           .state(StateDocState.DONE)
                                           .stateTimestamp(OffsetDateTime.now())
                                           .build();
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.just(initialStateDoc));
        StateDoc updatedStateDoc = StateDoc.builder()
                                           .consultant(TEST_CONSULTANT)
                                           .client(TEST_CLIENT)
                                           .yearBegin(TEST_FISCAL_YEAR_2021_START)
                                           .state(StateDocState.INIT)
                                           .stateTimestamp(OffsetDateTime.now())
                                           .build();
        when(stateDocRepository.updateToInitAndFindOne(any(), any(), any())).thenReturn(Mono.just(updatedStateDoc));
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        // execute
        ImportData importData = importExecutionService.initializePartialImport(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                                               TEST_DELTA_VERSION, TEST_CONTEXT).contextWrite(TestUtil.TEST_CONTEXT).block();
        // assert
        assertThat(importData).isNotNull();
        assertThat(importData.masterdataContext()).isNotNull().isEqualTo(masterDataContext);
        assertThat(importData.stateDoc()).isNotNull().isEqualTo(updatedStateDoc);
    }

    @Test
    @DisplayName("Returns an empty mono for a partial import when no state doc was found")
    void should_return_an_empty_mono_for_partial_import_when_no_state_doc_was_found() {
        // prepare
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.empty());
        // execute
        ImportData importData =
                importExecutionService.initializePartialImport(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                               TEST_DELTA_VERSION, TEST_CONTEXT).block();
        // assert
        assertThat(importData).isNull();
    }

    @Test
    @DisplayName("Logs a warn message and returns a InitialLoadFailed error when an InitialLoadFailedException error occurs")
    void should_log_warn_and_return_a_initial_load_failed_error_when_an_initial_load_fails() {
        // prepare
        InitialLoadFailedException initialLoadFailedException =
                new InitialLoadFailedException(EXCEPTION_MESSAGE, HttpStatus.CONFLICT.value(), TEST_TYPE);
        // execute
        Throwable throwable = catchThrowable(
                () -> importExecutionService.handleExceptionAndUpdateStateDoc(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_CONTEXT,
                                                                              LoggingUtil.INITIAL_LOAD_ERROR_LOG, initialLoadFailedException).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(InitialLoadFailedException.class);
        InitialLoadFailedException expectedException = (InitialLoadFailedException) throwable;
        assertThat(expectedException).usingRecursiveAssertion().isEqualTo(initialLoadFailedException);
        List<ILoggingEvent> searchResult = memoryAppender.search(EXCEPTION_MESSAGE, Level.WARN);
        assertThat(searchResult).hasSize(1);
    }

    @Test
    @DisplayName("Logs a warn message and returns a HttpNoContent error when an HttpCallNoContentException error occurs")
    void should_log_warn_and_return_a_http_no_content_error_when_a_no_content_error_occurs() {
        // prepare
        StateDoc initialStateDoc = StateDoc.builder()
                                           .consultant(TEST_CONSULTANT)
                                           .client(TEST_CLIENT)
                                           .yearBegin(TEST_FISCAL_YEAR_2021_START)
                                           .state(StateDocState.DONE)
                                           .stateTimestamp(OffsetDateTime.now())
                                           .build();
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.just(initialStateDoc));
        StateDoc updatedStateDoc = StateDoc.builder()
                                           .consultant(TEST_CONSULTANT)
                                           .client(TEST_CLIENT)
                                           .yearBegin(TEST_FISCAL_YEAR_2021_START)
                                           .state(StateDocState.INIT)
                                           .stateTimestamp(OffsetDateTime.now())
                                           .build();
        when(stateDocRepository.updateToInitAndFindOne(any(), any(), any())).thenReturn(Mono.just(updatedStateDoc));
        when(stateDocRepository.deleteOne(any(), any(), any())).thenReturn(Mono.just(DeleteResult.unacknowledged()));
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
        List<ILoggingEvent> warnSearchResult = memoryAppender.search(ProcessingErrorMessageConstants.NO_MASTER_DATA_CONTEXT_IN_ACDS, Level.WARN);
        assertThat(warnSearchResult).hasSize(1);
        List<ILoggingEvent> infoSearchResult = memoryAppender.search(ProcessingErrorMessageConstants.MONGODB_WRITE_UNACKNOWLEDGED, Level.INFO);
        assertThat(infoSearchResult).hasSize(1);
    }

    @Test
    @DisplayName("Logs an error message and returns a Mono error when an HttpCallBusinessException error occurs")
    void should_log_error_and_return_a_mono_error_when_an_http_call_business_error_occurs() {
        // prepare
        ProblemInfo problemInfo = createProblemInfo();
        HttpCallBusinessException httpCallBusinessException =
                new HttpCallBusinessException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.ACCOUNT_SUM_DAYS, 555, problemInfo);
        UpdateResult updateResult = UpdateResult.acknowledged(0, 0L, new BsonDocument());
        when(stateDocRepository.updateToUnsuccessfulState(any(), any(), any(), any(), any())).thenReturn(Mono.just(updateResult));
        // execute
        Throwable throwable = catchThrowable(
                () -> importExecutionService.handleExceptionAndUpdateStateDoc(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_CONTEXT,
                                                                              LoggingUtil.INITIAL_LOAD_ERROR_LOG, httpCallBusinessException).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(HttpCallBusinessException.class);
        HttpCallBusinessException resultException = (HttpCallBusinessException) throwable;
        assertThat(resultException).usingRecursiveAssertion().isEqualTo(httpCallBusinessException);
        List<ILoggingEvent> firstSearchResult = memoryAppender.search(EXCEPTION_MESSAGE, Level.ERROR);
        assertThat(firstSearchResult).hasSize(1);
        List<ILoggingEvent> secondSearchResult = memoryAppender.search(ProcessingErrorMessageConstants.NO_STATE_DOC_EXISTS_ERROR, Level.ERROR);
        assertThat(secondSearchResult).hasSize(1);
    }

    @Test
    @DisplayName("Logs an error message and returns a Mono error when an AggregationProcessingBusinessException error occurs")
    void should_log_error_and_return_a_mono_error_when_an_aggregation_processing_business_error_occurs() {
        // prepare
        AggregationProcessingBusinessException businessException =
                new AggregationProcessingBusinessException(EXCEPTION_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        UpdateResult updateResult = UpdateResult.acknowledged(0, 0L, new BsonDocument());
        when(stateDocRepository.updateToUnsuccessfulState(any(), any(), any(), any(), any())).thenReturn(Mono.just(updateResult));
        // execute
        Throwable throwable = catchThrowable(
                () -> importExecutionService.handleExceptionAndUpdateStateDoc(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_CONTEXT,
                                                                              LoggingUtil.INITIAL_LOAD_ERROR_LOG, businessException).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(AggregationProcessingBusinessException.class);
        AggregationProcessingBusinessException resultException = (AggregationProcessingBusinessException) throwable;
        assertThat(resultException).usingRecursiveAssertion().isEqualTo(businessException);
        List<ILoggingEvent> firstSearchResult = memoryAppender.search(EXCEPTION_MESSAGE, Level.ERROR);
        assertThat(firstSearchResult).hasSize(1);
        List<ILoggingEvent> secondSearchResult = memoryAppender.search(ProcessingErrorMessageConstants.NO_STATE_DOC_EXISTS_ERROR, Level.ERROR);
        assertThat(secondSearchResult).hasSize(1);
    }

    @Test
    @DisplayName("Logs an error message and returns a Mono error when an unknown exception error occurs")
    void should_log_error_and_return_a_mono_error_when_an_unknown_error_occurs() {
        // prepare
        RuntimeException runtimeException = new RuntimeException(EXCEPTION_MESSAGE);
        UpdateResult updateResult = UpdateResult.acknowledged(1, 0L, new BsonDocument());
        when(stateDocRepository.updateToUnsuccessfulState(any(), any(), any(), any(), any())).thenReturn(Mono.just(updateResult));
        // execute
        Throwable throwable = catchThrowable(
                () -> importExecutionService.handleExceptionAndUpdateStateDoc(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_CONTEXT,
                                                                              LoggingUtil.INITIAL_LOAD_ERROR_LOG, runtimeException).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(AggregationProcessingBusinessException.class);
        AggregationProcessingBusinessException resultException = (AggregationProcessingBusinessException) throwable;
        assertThat(resultException.getMessage()).isEqualTo(ProcessingErrorMessageConstants.IMPORT_ERROR);
        assertThat(resultException.getHttpStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(resultException.getCause()).usingRecursiveAssertion().isEqualTo(runtimeException);
        List<ILoggingEvent> firstSearchResult = memoryAppender.search(EXCEPTION_MESSAGE, Level.ERROR);
        assertThat(firstSearchResult).hasSize(1);
        List<ILoggingEvent> secondSearchResult = memoryAppender.search(ProcessingErrorMessageConstants.NO_STATE_DOC_EXISTS_ERROR);
        assertThat(secondSearchResult).isEmpty();
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
    }
  
}