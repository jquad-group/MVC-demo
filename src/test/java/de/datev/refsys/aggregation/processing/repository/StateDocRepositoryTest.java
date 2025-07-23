package de.datev.refsys.aggregation.processing.repository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mongodb.MongoWriteException;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClient;
import de.datev.refsys.aggregation.document.model.ProcessingError;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.config.TestMeterConfiguration;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.config.mongo.MongoSharedConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import de.datev.refsys.aggregation.processing.model.MongoIndex;
import de.datev.refsys.aggregation.processing.util.CircuitBreakerUtil;
import de.datev.refsys.aggregation.processing.util.ClearDatabaseAnCreateIndexesBeforeEachTest;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.MongoHelperService;
import de.datev.refsys.aggregation.processing.util.ResetResilienceAfterEachTest;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.awaitility.Awaitility;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.mongodb.ErrorCategory.DUPLICATE_KEY;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.STATE_DOC;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.STATE_DOC_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.util.TestUtil.CALL_NOT_PERMITTED_CIRCUIT_BREAKER_ERROR;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_BASE_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_BASE_VERSION_UPDATE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION_UPDATE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FALSE_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FALSE_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FALSE_FISCAL_YEAR;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_END;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2022_END;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.assertLogs;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createStateDoc;
import static de.datev.refsys.aggregation.processing.util.TestUtil.searchCircuitBreakerLog;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

@ImportAutoConfiguration(TestMongoClientConfiguration.class)
@Import({ TestcontainersConfiguration.class, MongoSharedConfiguration.class, StateDocRepository.class, MongoHelperService.class})
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class, classes = { TestResilienceConfiguration.class, TestMeterConfiguration.class })
@DataMongoTest
@ActiveProfiles(TEST_PROFILE)
@ClearDatabaseAnCreateIndexesBeforeEachTest
@ResetResilienceAfterEachTest
class StateDocRepositoryTest {
    private static final int EXPECTED_SIZE = 2;

    @Autowired
    private MongoClient insertMongoClient;

    @Autowired
    private MongoClient updateMongoClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private MongoHelperService mongoHelperService;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Value("${ref-sys.update-schema.schema-version}")
    private int schemaVersion;

    private StateDocRepository stateDocRepository;
    private List<StateDoc> expectedStateDoc;

    private MemoryAppender memoryAppender;
    private MemoryAppender memoryAppenderDebugLevel;

    @BeforeEach
    void setUp() {
        memoryAppenderDebugLevel = setupMemoryAppender(memoryAppenderDebugLevel, LoggingUtil.class, Level.DEBUG);
        memoryAppender = setupMemoryAppender(memoryAppender, CircuitBreakerUtil.class, Level.WARN);
        stateDocRepository =
                new StateDocRepository(insertMongoClient, new SimpleMeterRegistry(), databaseName, circuitBreakerRegistry, retryRegistry, schemaVersion);
        expectedStateDoc = TestDataLoader.loadMongoDBList("json/collections/expected/movementdata-collections/stateDoc.json",
                                                          StateDoc.class);
        assertThat(expectedStateDoc).hasSize(EXPECTED_SIZE);
    }

    @Test
    @DisplayName(STATE_DOC + " Index Test")
    void should_insert_state_doc_and_verify_indexes() {
        InsertOneResult insertOneResult = mongoHelperService.insertOneStateDoc(expectedStateDoc.get(0));
        assertThat(insertOneResult).isNotNull();
        assertThat(insertOneResult.wasAcknowledged()).isTrue();
        assertThat(insertOneResult.getInsertedId()).isNotNull();

        List<Document> documents = mongoHelperService.listStateDocIndexes();
        assertThat(documents).hasSize(2);
        MongoIndex mongoIdIndex = mongoHelperService.deserializeMongoIndex(documents.get(0).toBsonDocument().toJson());
        mongoHelperService.verifyIdIndex(mongoIdIndex);

        MongoIndex mongoIndex = mongoHelperService.deserializeMongoIndex(documents.get(1).toBsonDocument().toJson());
        mongoHelperService.verifyIndexForStateDocAndMasterData(mongoIndex);
    }


    @Test
    @DisplayName("Test StateDocRepository delete one")
    void should_delete_one_when_given_correct_params_and_entry_is_found() {
        InsertManyResult insertManyResult =  mongoHelperService.insertManyStateDocs(expectedStateDoc);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_SIZE);

        DeleteResult deleteResult =
                stateDocRepository.deleteOne(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();
        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.wasAcknowledged()).isTrue();
        assertThat(deleteResult.getDeletedCount()).isEqualTo(1);
        List<StateDoc> currentStateDoc = mongoHelperService.findAllStateDocs();
        assertThat(currentStateDoc).hasSize(EXPECTED_SIZE - 1);

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_DELETE_ONE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test StateDocRepository delete nothing with incorrect parameters")
    void should_not_delete_one_when_no_entry_is_found() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyStateDocs(expectedStateDoc);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_SIZE);

        DeleteResult deleteResult =
                stateDocRepository.deleteOne(TEST_FALSE_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();
        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.wasAcknowledged()).isTrue();
        assertThat(deleteResult.getDeletedCount()).isZero();
        List<StateDoc> currentStateDoc = mongoHelperService.findAllStateDocs();
        assertThat(currentStateDoc).hasSize(EXPECTED_SIZE);

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_DELETE_ONE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test StateDocRepository find by BusinessKey")
    void should_find_one_by_business_key_when_given_correct_params() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyStateDocs(expectedStateDoc);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_SIZE);

        StateDoc findResult =
                stateDocRepository.findOneByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();
        assertThat(findResult).isNotNull();
        assertThat(findResult.getConsultant()).isEqualTo(TEST_CONSULTANT);
        assertThat(findResult.getClient()).isEqualTo(TEST_CLIENT);
        assertThat(findResult.getYearBegin()).isEqualTo(TEST_FISCAL_YEAR_2021_START);

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_FIND_ONE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test StateDocRepository find nothing with incorrect parameters")
    void should_not_find_one_by_business_key_when_no_entry_exists_to_given_params() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyStateDocs(expectedStateDoc);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_SIZE);

        StateDoc findResult =
                stateDocRepository.findOneByBusinessKey(TEST_FALSE_CONSULTANT, TEST_FALSE_CLIENT, TEST_FISCAL_YEAR_2022_END).block();
        assertThat(findResult).isNull();

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_FIND_ONE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).isEmpty();
    }

    @Test
    @DisplayName("Test StateDocRepository update unsuccessful state")
    void should_update_to_unsuccessful_state_when_correct_params_are_given() {
        expectedStateDoc = TestDataLoader.loadMongoDBList("json/collections/expected/state-doc/unsuccessful-stateDoc.json", StateDoc.class);
        InsertManyResult insertManyResult = mongoHelperService.insertManyStateDocs(expectedStateDoc);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_SIZE);

        UpdateResult updateResult =
                stateDocRepository.updateToUnsuccessfulState(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, StateDocState.BAD,
                                                             ProcessingError.builder().build())
                                  .block();
        assertThat(updateResult).isNotNull();
        assertThat(updateResult.getMatchedCount()).isEqualTo(1);
        assertThat(updateResult.getModifiedCount()).isEqualTo(1);
        List<StateDoc> currentStateDoc = mongoHelperService.findAllStateDocs();
        assertThat(currentStateDoc).hasSize(EXPECTED_SIZE);
        Optional<StateDoc> updatedStateDoc = currentStateDoc.stream().filter(stateDoc -> stateDoc.getConsultant().equals(TEST_CONSULTANT)
                && stateDoc.getClient().equals(TEST_CLIENT) && stateDoc.getYearBegin().equals(TEST_FISCAL_YEAR_2021_START)).findFirst();
        assertThat(updatedStateDoc).isPresent();
        assertThat(updatedStateDoc.get().getBaseVersion()).isEqualTo(TEST_BASE_VERSION);
        assertThat(updatedStateDoc.get().getDeltaVersion()).isEqualTo(TEST_DELTA_VERSION);
        assertThat(updatedStateDoc.get().getSchemaVersion()).isZero();
        assertThat(updatedStateDoc.get().getState()).isEqualTo(StateDocState.BAD);

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_TO_UNSUCCESSFUL_STATE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test StateDocRepository update successful state")
    void should_update_to_successful_state_when_correct_params_are_given() {
        expectedStateDoc = TestDataLoader.loadMongoDBList("json/collections/expected/state-doc/successful-stateDoc.json", StateDoc.class);
        InsertManyResult insertManyResult = mongoHelperService.insertManyStateDocs(expectedStateDoc);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_SIZE);

        UpdateResult updateResult =
                stateDocRepository.updateToSuccessfulState(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION_UPDATE,
                                                           TEST_DELTA_VERSION_UPDATE).block();
        assertThat(updateResult).isNotNull();
        assertThat(updateResult.getMatchedCount()).isEqualTo(1);
        assertThat(updateResult.getModifiedCount()).isEqualTo(1);
        List<StateDoc> currentStateDoc = mongoHelperService.findAllStateDocs();
        assertThat(currentStateDoc).hasSize(EXPECTED_SIZE);
        Optional<StateDoc> updatedStateDoc = currentStateDoc.stream().filter(stateDoc -> stateDoc.getConsultant().equals(TEST_CONSULTANT)
                && stateDoc.getClient().equals(TEST_CLIENT) && stateDoc.getYearBegin().equals(TEST_FISCAL_YEAR_2021_START)).findFirst();
        assertThat(updatedStateDoc).isPresent();
        assertThat(updatedStateDoc.get().getBaseVersion()).isEqualTo(TEST_BASE_VERSION_UPDATE);
        assertThat(updatedStateDoc.get().getDeltaVersion()).isEqualTo(TEST_DELTA_VERSION_UPDATE);
        assertThat(updatedStateDoc.get().getState()).isEqualTo(StateDocState.DONE);
        assertThat(updatedStateDoc.get().getSchemaVersion()).isEqualTo(schemaVersion);

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_TO_SUCCESSFUL_STATE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test StateDocRepository update nothing with incorrect parameters")
    void should_not_update_successful_state_when_incorrect_params_are_given() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyStateDocs(expectedStateDoc);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_SIZE);

        UpdateResult result =
                stateDocRepository.updateToSuccessfulState(TEST_FALSE_CONSULTANT, TEST_FALSE_CLIENT, TEST_FALSE_FISCAL_YEAR, TEST_BASE_VERSION,
                                                           TEST_DELTA_VERSION).block();
        assertThat(result).isNotNull();
        assertThat(result.getMatchedCount()).isZero();
        assertThat(result.getModifiedCount()).isZero();
        List<StateDoc> currentStateDoc = mongoHelperService.findAllStateDocs();
        assertThat(currentStateDoc).hasSize(EXPECTED_SIZE);

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_TO_SUCCESSFUL_STATE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test StateDocRepository update statetimestamp when document do exists ")
    void should_update_timestamp_success() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyStateDocs(expectedStateDoc);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_SIZE);

        OffsetDateTime now = OffsetDateTime.now();
        UpdateResult updateResult = stateDocRepository.updateTimestamp(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, now).block();
        assertThat(updateResult).isNotNull();
        assertThat(updateResult.getMatchedCount()).isEqualTo(1);
        assertThat(updateResult.getModifiedCount()).isEqualTo(1);
        List<StateDoc> currentStateDoc = mongoHelperService.findAllStateDocs();
        assertThat(currentStateDoc).hasSize(EXPECTED_SIZE);
        Optional<StateDoc> updatedStateDoc = currentStateDoc.stream().filter(stateDoc -> stateDoc.getConsultant().equals(TEST_CONSULTANT)
                && stateDoc.getClient().equals(TEST_CLIENT) && stateDoc.getYearBegin().equals(TEST_FISCAL_YEAR_2021_START)).findFirst();
        assertThat(updatedStateDoc).isPresent();

        // assert close to due to different precision
        assertThat(updatedStateDoc.get().getStateTimestamp()).isCloseTo(now, within(10, ChronoUnit.MILLIS));

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_TIMESTAMP_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test StateDocRepository update statetimestamp when document do not exists ")
    void should_update_timestamp_failed() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyStateDocs(expectedStateDoc);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_SIZE);

        OffsetDateTime now = OffsetDateTime.now();
        UpdateResult updateResult = stateDocRepository.updateTimestamp(999, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, now).block();
        assertThat(updateResult).isNotNull();
        assertThat(updateResult.getMatchedCount()).isZero();
        assertThat(updateResult.getModifiedCount()).isZero();
        List<StateDoc> currentStateDoc = mongoHelperService.findAllStateDocs();
        assertThat(currentStateDoc).hasSize(EXPECTED_SIZE);
        Optional<StateDoc> updatedStateDoc = currentStateDoc.stream().filter(stateDoc -> stateDoc.getConsultant().equals(TEST_CONSULTANT)
                && stateDoc.getClient().equals(TEST_CLIENT) && stateDoc.getYearBegin().equals(TEST_FISCAL_YEAR_2021_START)).findFirst();
        assertThat(updatedStateDoc).isPresent();

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_TIMESTAMP_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test StateDocRepository insert one")
    void should_insert_one_successfully() {
        StateDoc stateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        InsertOneResult insertResult = stateDocRepository.insertOne(stateDoc).block();
        assertThat(insertResult).isNotNull();
        assertThat(insertResult.wasAcknowledged()).isTrue();
        List<StateDoc> currentStateDoc = mongoHelperService.findAllStateDocs();
        assertThat(currentStateDoc).hasSize(1);
        assertThat(currentStateDoc.get(0).getStateTimestamp().truncatedTo(ChronoUnit.MILLIS))
                .isAtSameInstantAs(stateDoc.getStateTimestamp().truncatedTo(ChronoUnit.MILLIS));
        assertThat(currentStateDoc.get(0)).usingRecursiveComparison()
                                          .ignoringFields("createdTimestamp","stateTimestamp")
                                          .isEqualTo(stateDoc);

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_INSERT_ONE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test StateDocRepository insert already existed document")
    void should_throw_exception_when_inserting_exist_document() {
        StateDoc stateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);

        InsertOneResult insertOneResult = stateDocRepository.insertOne(stateDoc).block();
        assertThat(insertOneResult).isNotNull();
        assertThat(insertOneResult.wasAcknowledged()).isTrue();

        Throwable throwable = catchThrowable(() -> stateDocRepository.insertOne(stateDoc).block());
        assertThat(throwable).isInstanceOf(MongoWriteException.class);
        MongoWriteException mongoWriteException = (MongoWriteException) throwable;
        assertThat(mongoWriteException.getError().getCategory()).isEqualTo(DUPLICATE_KEY);

        List<StateDoc> currentStateDoc = mongoHelperService.findAllStateDocs();
        assertThat(currentStateDoc).hasSize(1);
        assertThat(currentStateDoc.get(0).getState()).isEqualTo(StateDocState.INIT);
        assertThat(currentStateDoc.get(0).getClient()).isEqualTo(stateDoc.getClient());
        assertThat(currentStateDoc.get(0).getConsultant()).isEqualTo(stateDoc.getConsultant());
        assertThat(currentStateDoc.get(0).getYearBegin()).isEqualTo(stateDoc.getYearBegin());
        assertThat(currentStateDoc.get(0).getYearEnd()).isEqualTo(stateDoc.getYearEnd());

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_INSERT_ONE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("should update statedoc and return it")
    void should_update_and_find_one() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyStateDocs(expectedStateDoc);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_SIZE);

        StateDoc actualStateDoc = stateDocRepository.findOneByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();
        assertThat(actualStateDoc).isNotNull();

        StateDoc stateDocResult =
                stateDocRepository.updateToInitAndFindOne(TEST_BASE_VERSION_UPDATE, TEST_DELTA_VERSION_UPDATE, actualStateDoc).block();

        // Assert state doc before was returned
        assertThat(stateDocResult).isNotNull();
        assertThat(stateDocResult.getState()).isEqualTo(StateDocState.INIT);
        assertThat(stateDocResult.getClient()).isEqualTo(actualStateDoc.getClient());
        assertThat(stateDocResult.getConsultant()).isEqualTo(actualStateDoc.getConsultant());
        assertThat(stateDocResult.getYearBegin()).isEqualTo(actualStateDoc.getYearBegin());
        assertThat(stateDocResult.getYearEnd()).isEqualTo(actualStateDoc.getYearEnd());
        assertThat(stateDocResult.getStateTimestamp().truncatedTo(ChronoUnit.MILLIS)).isCloseTo(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS), within(1, ChronoUnit.SECONDS));
        assertThat(stateDocResult.getCreatedTimestamp().truncatedTo(ChronoUnit.MILLIS)).isCloseTo(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS), within(1, ChronoUnit.SECONDS));
        assertThat(stateDocResult.getSchemaVersion()).isEqualTo(schemaVersion);

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_TO_INIT_AND_FIND_ONE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test StateDocRepository find one and update when statedoc does not exist")
    void should_fail_update_statedoc_and_return_because_it_does_not_exist() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyStateDocs(expectedStateDoc);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_SIZE);

        StateDoc actualStateDoc = stateDocRepository.findOneByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();
        assertThat(actualStateDoc).isNotNull();

        actualStateDoc.setConsultant(TEST_FALSE_CONSULTANT);
        StateDoc stateDocResult =
                stateDocRepository.updateToInitAndFindOne(TEST_DELTA_VERSION_UPDATE, TEST_DELTA_VERSION_UPDATE, actualStateDoc).block();

        assertThat(stateDocResult).isNull();

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_TO_INIT_AND_FIND_ONE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).isEmpty();
    }

    @Test
    @DisplayName("Test StateDocRepository updateVersion info")
    void should_update_statedoc_and_return_because_it_does_not_exist() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyStateDocs(expectedStateDoc);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_SIZE);

        UpdateResult updateResult =
                Mono.from(updateMongoClient.startSession())
                    .flatMap(clientSession ->
                                     stateDocRepository.updateVersionInfo(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, clientSession,
                                                                          updateMongoClient, 4L, 2L)).block();

        assertThat(updateResult).as("update version info should return a result").isNotNull();
        assertThat(updateResult.getModifiedCount()).as("update version info should update exactly one statedoc").isEqualTo(1L);

        StateDoc actualStateDoc = stateDocRepository.findOneByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();

        assertThat(actualStateDoc).as("updated statedoc should exist").isNotNull();
        assertThat(actualStateDoc.getBaseVersion()).as("updated statedoc should contain expected base version").isEqualTo(4);
        assertThat(actualStateDoc.getDeltaVersion()).as("updated statedoc should contain expected delta version").isEqualTo(2);

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_VERSION_INFO_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Tests resilience retry when an error occurs")
        //retry test is a DataMongoTest because you can't mock Find Method so that it returns a Mono.error instead of FindPublisher
    void should_retry_when_the_retry_exception_is_thrown() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(STATE_DOC_CIRCUIT_BREAKER);
        //insert wrong stateDoc
        List<Document> stateDocDocuments = TestDataLoader.parseDocuments("json/collections/wrong-document/wrong-stateDoc.ndjson");
        StepVerifier.create(mongoHelperService.insertManyStateDocDocuments(stateDocDocuments)).expectNextCount(1).verifyComplete();
        Retry retry = retryRegistry.retry(MONGODB_RETRY_INSTANCE_NAME);
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isZero();
        StateDoc newStateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        Throwable throwable = catchThrowable(() -> stateDocRepository.insertOne(newStateDoc).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoWriteException.class);
        throwable = catchThrowable(() -> stateDocRepository.insertOne(newStateDoc).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoWriteException.class);
        long numberOfTotalCalls = retry.getMetrics().getNumberOfTotalCalls();
        assertThat(numberOfTotalCalls).isEqualTo(retry.getRetryConfig().getMaxAttempts());
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isZero();
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(2);
        //test MongoWriteException
        circuitBreaker.reset();
        mongoHelperService.deleteAllStateDocs();
        StateDoc stateDoc = createStateDoc(StateDocState.BAD, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        stateDocRepository.insertOne(stateDoc).block();
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(numberOfTotalCalls + 1);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isZero();
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(2);
        throwable = catchThrowable(() -> stateDocRepository.insertOne(stateDoc).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoWriteException.class);
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(numberOfTotalCalls + 2);
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isZero();
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(3);
    }

    @Test
    @DisplayName("Tests resilience circuit breaker when an error occurs")
        //CircuitBreaker test is a DataMongoTest because you can't mock Find Method so that it returns a Mono.error instead of FindPublisher
    void should_change_circuit_breaker_state_from_close_to_open_when_an_error_occurs() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(STATE_DOC_CIRCUIT_BREAKER);
        IntStream.range(0, 2)
                 .forEachOrdered(n -> stateDocRepository.findOneByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block());
        // assert state not changed (CLOSED)
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        //insert wrong stateDoc
        List<Document> stateDocDocuments = TestDataLoader.parseDocuments("json/collections/wrong-document/wrong-stateDoc.ndjson");
        StepVerifier.create(mongoHelperService.insertManyStateDocDocuments(stateDocDocuments)).expectNextCount(1).verifyComplete();

        StateDoc newStateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        Throwable throwable = catchThrowable(() -> stateDocRepository.insertOne(newStateDoc).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoWriteException.class);
        throwable = catchThrowable(() -> stateDocRepository.insertOne(newStateDoc).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoWriteException.class);

        // CLOSED -> OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        List<ILoggingEvent> closedToOpenLog = searchCircuitBreakerLog(circuitBreaker, memoryAppender, CircuitBreaker.State.CLOSED, CircuitBreaker.State.OPEN);
        assertLogs(closedToOpenLog, Level.WARN);

        // OPEN -> HALF OPEN
        Awaitility.await().timeout(20L, TimeUnit.SECONDS)
                  .untilAsserted(() -> assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN));
        List<ILoggingEvent> openToHalfOpenLog = searchCircuitBreakerLog(circuitBreaker, memoryAppender, CircuitBreaker.State.OPEN, CircuitBreaker.State.HALF_OPEN);
        assertLogs(openToHalfOpenLog, Level.WARN);

        // send 2 bad requests
        throwable = catchThrowable(() -> stateDocRepository.insertOne(newStateDoc).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoWriteException.class);
        throwable = catchThrowable(() -> stateDocRepository.insertOne(newStateDoc).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoWriteException.class);
        // HALF OPEN -> OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        List<ILoggingEvent> halfOpenToOpen = searchCircuitBreakerLog(circuitBreaker, memoryAppender, CircuitBreaker.State.HALF_OPEN, CircuitBreaker.State.OPEN);
        assertLogs(halfOpenToOpen, Level.WARN);

        // send 1 bad requests, expect CallNotPermittedException exception
        throwable = catchThrowable(() -> stateDocRepository.insertOne(newStateDoc).block());
        assertThat(throwable).isNotNull().isInstanceOf(CallNotPermittedException.class);
        CallNotPermittedException callNotPermittedException = (CallNotPermittedException) throwable;
        assertThat(callNotPermittedException.getMessage()).isEqualTo(String.format(CALL_NOT_PERMITTED_CIRCUIT_BREAKER_ERROR, STATE_DOC_CIRCUIT_BREAKER));
        // OPEN -> HALF OPEN
        Awaitility.await().timeout(20L, TimeUnit.SECONDS)
                  .untilAsserted(() -> assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN));
        //drop the collection
        mongoHelperService.deleteAllStateDocs();
        mongoHelperService.insertManyStateDocs(expectedStateDoc);
        // send 1 good request
        stateDocRepository.findOneByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();
        // assert State did not change (HALF OPEN)
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        // send second good request
        stateDocRepository.findOneByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();
        // HALF OPEN -> CLOSED
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        List<ILoggingEvent> halfOpenToClosedLog = searchCircuitBreakerLog(circuitBreaker, memoryAppender, CircuitBreaker.State.HALF_OPEN, CircuitBreaker.State.CLOSED);
        assertLogs(halfOpenToClosedLog, Level.WARN);
    }

    @Test
    @DisplayName("Test update forceReftabCurrentYear")
    void should_update_force_reftab_current_year() {
        StateDoc stateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, 0);
        InsertOneResult insertResult = mongoHelperService.insertOneStateDoc(stateDoc);
        assertThat(insertResult).isNotNull();
        assertThat(insertResult.wasAcknowledged()).isTrue();
        stateDocRepository.updateFromSchemaVersionOneToFour(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, true).block();
        List<StateDoc> foundStateDocs = mongoHelperService.findAllStateDocs();
        assertThat(foundStateDocs).hasSize(1);
        StateDoc expectedStateDocSchemaUpdate =
                TestDataLoader.loadDBElement("json/collections/expected/state-doc/update-schema-from-one-to-four-state-doc.json", StateDoc.class);
        assertThat(foundStateDocs.get(0)).usingRecursiveComparison()
                                         .ignoringFieldsOfTypes(OffsetDateTime.class)
                                         .isEqualTo(expectedStateDocSchemaUpdate);

        String expectedLog = LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_FROM_SCHEMA_VERSION_ONE_TO_TWO_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }
}