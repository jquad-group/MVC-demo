package de.datev.refsys.aggregation.processing.repository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;
import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.processing.config.TestMeterConfiguration;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.config.mongo.MongoSharedConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;
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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.DAY_PREFIX;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_DAYS;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.IMPORT_MOVEMENT_DATA_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME;
import static de.datev.refsys.aggregation.processing.util.TestUtil.CALL_NOT_PERMITTED_CIRCUIT_BREAKER_ERROR;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_ACCOUNT_NUMBER_2;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_ACCOUNT_VALUE_DAY_1;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_ACCOUNT_VALUE_DAY_2;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_AMOUNT_CREDIT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_AMOUNT_DEBIT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FALSE_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FALSE_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_QUANTITY_CREDIT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_QUANTITY_DEBIT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_WEIGHT_CREDIT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_WEIGHT_DEBIT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.assertLogs;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createAccountDbKeyFields;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createMovementDataDay;
import static de.datev.refsys.aggregation.processing.util.TestUtil.searchCircuitBreakerLog;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ImportAutoConfiguration(TestMongoClientConfiguration.class)
@Import({ TestcontainersConfiguration.class, MongoSharedConfiguration.class, MovementDataDayRepository.class, MongoHelperService.class  })
@ContextConfiguration(classes = { TestResilienceConfiguration.class, TestMeterConfiguration.class })
@DataMongoTest
@ActiveProfiles(TEST_PROFILE)
@ClearDatabaseAnCreateIndexesBeforeEachTest
@ResetResilienceAfterEachTest
class MovementDataDayRepositoryTest {
    private static final int EXPECTED_INITIAL_SIZE = 4;

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

    private MovementDataDayRepository movementDataDayRepository;
    private List<MovementDataDay> expectedMovementDataDays;
    private MemoryAppender memoryAppender;
    private MemoryAppender memoryAppenderTraceLevel;

    @BeforeEach
    void setUp() {
        memoryAppender = setupMemoryAppender(memoryAppender, CircuitBreakerUtil.class, Level.WARN);
        memoryAppenderTraceLevel = setupMemoryAppender(memoryAppenderTraceLevel, LoggingUtil.class, Level.TRACE);
        movementDataDayRepository =
                new MovementDataDayRepository(insertMongoClient, updateMongoClient, new SimpleMeterRegistry(), databaseName, circuitBreakerRegistry,
                                              retryRegistry);
        expectedMovementDataDays = TestDataLoader.loadMongoDBList("json/collections/repository/movementDataDays.json", MovementDataDay.class);
        assertThat(expectedMovementDataDays).hasSize(EXPECTED_INITIAL_SIZE);
    }

    @Test
    @DisplayName(MOVEMENT_DATA_DAYS + " Index Test")
    void should_insert_movement_data_days_and_verify_indexes() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMovementDataDays(expectedMovementDataDays);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.wasAcknowledged()).isTrue();
        assertThat(insertManyResult.getInsertedIds()).isNotNull();

        List<Document> documents = mongoHelperService.listMovementDataDaysIndexes();
        assertThat(documents).hasSize(2);
        MongoIndex mongoIdIndex = mongoHelperService.deserializeMongoIndex(documents.get(0).toBsonDocument().toJson());
        mongoHelperService.verifyIdIndex(mongoIdIndex);

        MongoIndex mongoIndex = mongoHelperService.deserializeMongoIndex(documents.get(1).toBsonDocument().toJson());
        mongoHelperService.verifyIndexForMovementDataDayAndMonth(mongoIndex);
    }

    @Test
    @DisplayName("Test MovementDataDayRepository delete by BusinessKey")
    void should_delete_many_if_given_correct_params_and_entries_exist() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMovementDataDays(expectedMovementDataDays);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_INITIAL_SIZE);

        DeleteResult deleteResult =
                movementDataDayRepository.deleteManyByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();
        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.wasAcknowledged()).isTrue();
        int deletedCount = Math.toIntExact(deleteResult.getDeletedCount());
        assertThat(deletedCount).isEqualTo(2);
        List<MovementDataDay> currentMovementDataDay = mongoHelperService.findAllMovementDataDays();
        assertThat(currentMovementDataDay).hasSize(EXPECTED_INITIAL_SIZE - deletedCount);

        String expectedLog = LoggingUtil.MOVEMENT_DATA_DAY_REPOSITORY_DELETE_MANY_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderTraceLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test MovementDataDayRepository find All Movement Data Day For Account")
    void should_return_MovementDataforOneAccount_expectOneDocument_returned() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMovementDataDays(expectedMovementDataDays);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_INITIAL_SIZE);

        List<MovementDataDay> findResult =
                movementDataDayRepository.findAllMovementDataDayForAccount(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START,
                                                                           List.of(620000000, 710000000, 610000000, 510000000)).block();
        assertThat(findResult).as("should filter the 4 bookings to the two for the given fiscalyear 2021").hasSize(2);
        assertThat(findResult.get(0).getAccountNumber()).as("result should be ordered by account - first").isEqualTo(510000000);
        assertThat(findResult.get(1).getAccountNumber()).as("result should be ordered by account - second").isEqualTo(610000000);

        String expectedLog = LoggingUtil.MOVEMENT_DATA_DAY_REPOSITORY_FIND_ALL_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderTraceLevel.search(expectedLog, Level.TRACE);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test MovementDataDayRepository delete nothing with incorrect parameters")
    void should_not_delete_any_if_given_incorrect_params() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMovementDataDays(expectedMovementDataDays);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_INITIAL_SIZE);

        DeleteResult deleteResult =
                movementDataDayRepository.deleteManyByBusinessKey(TEST_FALSE_CONSULTANT, TEST_FALSE_CLIENT, TEST_FISCAL_YEAR_2021_START).block();
        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.wasAcknowledged()).isTrue();
        assertThat(deleteResult.getDeletedCount()).isZero();
        List<MovementDataDay> currentMovementDataDay = mongoHelperService.findAllMovementDataDays();
        assertThat(currentMovementDataDay).hasSize(EXPECTED_INITIAL_SIZE);

        String expectedLog = LoggingUtil.MOVEMENT_DATA_DAY_REPOSITORY_DELETE_MANY_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderTraceLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test MovementDataDayRepository bulk insert")
    void should_bulk_insert_if_correct_params_are_given() {
        Map<AccountDbKeyFields, MovementDataDay> accountDbKeyFieldsMovementDataDayMap = new LinkedHashMap<>();

        accountDbKeyFieldsMovementDataDayMap.put(createAccountDbKeyFields(expectedMovementDataDays.get(0)),
                                                 createMovementDataDay(expectedMovementDataDays.get(0)));
        accountDbKeyFieldsMovementDataDayMap.put(createAccountDbKeyFields(expectedMovementDataDays.get(1)),
                                                 createMovementDataDay(expectedMovementDataDays.get(1)));
        accountDbKeyFieldsMovementDataDayMap.put(createAccountDbKeyFields(expectedMovementDataDays.get(2)),
                                                 createMovementDataDay(expectedMovementDataDays.get(2)));
        accountDbKeyFieldsMovementDataDayMap.put(createAccountDbKeyFields(expectedMovementDataDays.get(3)),
                                                 createMovementDataDay(expectedMovementDataDays.get(3)));

        BulkWriteResult insertResults =
                movementDataDayRepository.bulkInsert(accountDbKeyFieldsMovementDataDayMap)
                                         .block();
        assertSuccessfulInsert(insertResults);

        List<MovementDataDay> updateList = accountDbKeyFieldsMovementDataDayMap.values().stream().toList();
        List<MovementDataDay> currentMovementDataDay = mongoHelperService.findAllMovementDataDays();
        assertThat(currentMovementDataDay).hasSize(4);
        assertThat(currentMovementDataDay).usingRecursiveFieldByFieldElementComparator().isEqualTo(updateList);

        String expectedLog = LoggingUtil.MOVEMENT_DATA_DAY_REPOSITORY_BULK_INSERT_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderTraceLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test MovementDataDayRepository bulk update")
    void should_bulk_update_if_correct_params_are_given() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMovementDataDays(expectedMovementDataDays);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_INITIAL_SIZE);

        Optional<MovementDataDay> expectedMovementDataDay =
                expectedMovementDataDays.stream().filter(cmdm -> cmdm.getAccountNumber().equals(TEST_ACCOUNT_NUMBER_2)).findFirst();
        assertThat(expectedMovementDataDay).isPresent();
        Map<String, AccountValue> initialValues = expectedMovementDataDay.get().getValues();

        Map<AccountDbKeyFields, MovementDataDay> accountDbKeyFieldsMovementDataDayMap = new LinkedHashMap<>();
        accountDbKeyFieldsMovementDataDayMap.put(new AccountDbKeyFields(), createMovementDataDay(expectedMovementDataDay.get()));
        ClientSession session = Mono.from(updateMongoClient.startSession()).block();
        assertThat(session).isNotNull();
        BulkWriteResult findResults =
                movementDataDayRepository.bulkUpsert(expectedMovementDataDay.get().getConsultant(), expectedMovementDataDay.get().getClient(),
                                                     expectedMovementDataDay.get().getFiscalYear(),
                                                     accountDbKeyFieldsMovementDataDayMap, session).block();
        session.clearTransactionContext();
        session.close();

        assertThat(findResults).isNotNull();
        assertThat(findResults.wasAcknowledged()).isTrue();
        assertThat(findResults.getInsertedCount()).isZero();
        assertThat(findResults.getDeletedCount()).isZero();
        assertThat(findResults.getMatchedCount()).isEqualTo(1);
        assertThat(findResults.getModifiedCount()).isEqualTo(1);

        List<MovementDataDay> currentMovementDataDay = mongoHelperService.findAllMovementDataDays();
        assertThat(currentMovementDataDay).hasSize(4);
        Optional<MovementDataDay> optionalMovementDataDay =
                currentMovementDataDay.stream().filter(cmdm -> cmdm.getAccountNumber().equals(TEST_ACCOUNT_NUMBER_2)).findFirst();
        assertThat(optionalMovementDataDay).isPresent();
        MovementDataDay actualMovementDataDay = optionalMovementDataDay.get();
        assertThat(actualMovementDataDay.getValues().get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_1).getAmountCredit()).isEqualTo(
                initialValues.get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_1).getAmountCredit() + TEST_AMOUNT_CREDIT);
        assertThat(actualMovementDataDay.getValues().get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_1).getAmountDebit()).isEqualTo(
                initialValues.get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_1).getAmountDebit() + TEST_AMOUNT_DEBIT);
        assertThat(actualMovementDataDay.getValues().get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_1).getWeightCredit()).isEqualTo(
                initialValues.get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_1).getWeightCredit() + TEST_WEIGHT_CREDIT);
        assertThat(actualMovementDataDay.getValues().get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_1).getWeightDebit()).isEqualTo(
                initialValues.get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_1).getWeightDebit() + TEST_WEIGHT_DEBIT);
        assertThat(actualMovementDataDay.getValues().get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_1).getQuantityCredit()).isEqualTo(
                initialValues.get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_1).getQuantityCredit() + TEST_QUANTITY_CREDIT);
        assertThat(actualMovementDataDay.getValues().get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_1).getQuantityDebit()).isEqualTo(
                initialValues.get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_1).getQuantityDebit() + TEST_QUANTITY_DEBIT);
        assertThat(actualMovementDataDay.getValues()).hasSize(2);
        assertThat(actualMovementDataDay.getValues().get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_2).getAmountCredit()).isEqualTo(
                TEST_AMOUNT_CREDIT);
        assertThat(actualMovementDataDay.getValues().get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_2).getAmountDebit()).isEqualTo(TEST_AMOUNT_DEBIT);
        assertThat(actualMovementDataDay.getValues().get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_2).getWeightCredit()).isEqualTo(
                TEST_WEIGHT_CREDIT);
        assertThat(actualMovementDataDay.getValues().get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_2).getWeightDebit()).isEqualTo(TEST_WEIGHT_DEBIT);
        assertThat(actualMovementDataDay.getValues().get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_2).getQuantityCredit()).isEqualTo(
                TEST_QUANTITY_CREDIT);
        assertThat(actualMovementDataDay.getValues().get(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_2).getQuantityDebit()).isEqualTo(
                TEST_QUANTITY_DEBIT);

        String expectedLog = LoggingUtil.MOVEMENT_DATA_DAY_REPOSITORY_BULK_UPDATE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderTraceLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Tests resilience retry when an error occurs")
    void should_retry_when_the_retry_exception_is_thrown() {
        Retry retry = retryRegistry.retry(MONGODB_RETRY_INSTANCE_NAME);
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isZero();
        // insert and assert data
        Map<AccountDbKeyFields, MovementDataDay> accountDbKeyFieldsMovementDataDayMap = new LinkedHashMap<>();
        accountDbKeyFieldsMovementDataDayMap.put(createAccountDbKeyFields(expectedMovementDataDays.get(0)),
                                                 createMovementDataDay(expectedMovementDataDays.get(0)));
        accountDbKeyFieldsMovementDataDayMap.put(createAccountDbKeyFields(expectedMovementDataDays.get(1)),
                                                 createMovementDataDay(expectedMovementDataDays.get(1)));
        accountDbKeyFieldsMovementDataDayMap.put(createAccountDbKeyFields(expectedMovementDataDays.get(2)),
                                                 createMovementDataDay(expectedMovementDataDays.get(2)));
        accountDbKeyFieldsMovementDataDayMap.put(createAccountDbKeyFields(expectedMovementDataDays.get(3)),
                                                 createMovementDataDay(expectedMovementDataDays.get(3)));
        BulkWriteResult insertResults = movementDataDayRepository.bulkInsert(accountDbKeyFieldsMovementDataDayMap).block();
        assertSuccessfulInsert(insertResults);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        // insert again to cause an exception (because of index)
        Throwable throwable = catchThrowable(() -> movementDataDayRepository.bulkInsert(accountDbKeyFieldsMovementDataDayMap).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoBulkWriteException.class);
        // assert failed retry
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(retry.getRetryConfig().getMaxAttempts());
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isZero();
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Tests resilience circuit breaker when an error occurs")
    void should_change_circuit_breaker_state_from_close_to_open_when_an_error_occurs() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(IMPORT_MOVEMENT_DATA_CIRCUIT_BREAKER);
        // insert and assert data
        Map<AccountDbKeyFields, MovementDataDay> accountDbKeyFieldsMovementDataDayMap = new LinkedHashMap<>();
        accountDbKeyFieldsMovementDataDayMap.put(createAccountDbKeyFields(expectedMovementDataDays.get(0)),
                                                 createMovementDataDay(expectedMovementDataDays.get(0)));
        accountDbKeyFieldsMovementDataDayMap.put(createAccountDbKeyFields(expectedMovementDataDays.get(1)),
                                                 createMovementDataDay(expectedMovementDataDays.get(1)));
        accountDbKeyFieldsMovementDataDayMap.put(createAccountDbKeyFields(expectedMovementDataDays.get(2)),
                                                 createMovementDataDay(expectedMovementDataDays.get(2)));
        accountDbKeyFieldsMovementDataDayMap.put(createAccountDbKeyFields(expectedMovementDataDays.get(3)),
                                                 createMovementDataDay(expectedMovementDataDays.get(3)));
        BulkWriteResult insertResults = movementDataDayRepository.bulkInsert(accountDbKeyFieldsMovementDataDayMap).block();
        assertSuccessfulInsert(insertResults);
        // assert state not changed (CLOSED)
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        // cause an exception
        Throwable throwable = catchThrowable(() -> movementDataDayRepository.bulkInsert(accountDbKeyFieldsMovementDataDayMap).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoBulkWriteException.class);
        // CLOSED -> OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        List<ILoggingEvent> closedToOpenLog = searchCircuitBreakerLog(circuitBreaker, memoryAppender, CircuitBreaker.State.CLOSED, CircuitBreaker.State.OPEN);
        assertLogs(closedToOpenLog, Level.WARN);
        // OPEN -> HALF OPEN
        Awaitility.await().timeout(20L, TimeUnit.SECONDS)
                  .untilAsserted(() -> assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN));
        List<ILoggingEvent> openToHalfOpenLog = searchCircuitBreakerLog(circuitBreaker, memoryAppender, CircuitBreaker.State.OPEN, CircuitBreaker.State.HALF_OPEN);
        assertLogs(openToHalfOpenLog, Level.WARN);
        // cause an exception again
        throwable = catchThrowable(() -> movementDataDayRepository.bulkInsert(accountDbKeyFieldsMovementDataDayMap).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoBulkWriteException.class);
        throwable = catchThrowable(() -> movementDataDayRepository.bulkInsert(accountDbKeyFieldsMovementDataDayMap).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoBulkWriteException.class);
        // HALF OPEN -> OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        List<ILoggingEvent> halfOpenToOpen = searchCircuitBreakerLog(circuitBreaker, memoryAppender, CircuitBreaker.State.HALF_OPEN, CircuitBreaker.State.OPEN);
        assertLogs(halfOpenToOpen, Level.WARN);
        // send 1 bad requests, expect CallNotPermittedException exception
        throwable = catchThrowable(() -> movementDataDayRepository.bulkInsert(accountDbKeyFieldsMovementDataDayMap).block());
        assertThat(throwable).isNotNull().isInstanceOf(CallNotPermittedException.class);
        CallNotPermittedException callNotPermittedException = (CallNotPermittedException) throwable;
        assertThat(callNotPermittedException.getMessage()).isEqualTo(String.format(CALL_NOT_PERMITTED_CIRCUIT_BREAKER_ERROR, IMPORT_MOVEMENT_DATA_CIRCUIT_BREAKER));
        assertThat(callNotPermittedException.getCausingCircuitBreakerName()).isEqualTo(IMPORT_MOVEMENT_DATA_CIRCUIT_BREAKER);
        // OPEN -> HALF OPEN
        Awaitility.await().timeout(20L, TimeUnit.SECONDS)
                  .untilAsserted(() -> assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN));
        // bulk insert again on empty collection
        mongoHelperService.deleteAllMovementDataDays();
        insertResults = movementDataDayRepository.bulkInsert(accountDbKeyFieldsMovementDataDayMap).block();
        assertSuccessfulInsert(insertResults);
        // assert State did not change (HALF OPEN)
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        // bulk insert again on empty collection
        mongoHelperService.deleteAllMovementDataDays();
        insertResults = movementDataDayRepository.bulkInsert(accountDbKeyFieldsMovementDataDayMap).block();
        assertSuccessfulInsert(insertResults);
        // HALF OPEN -> CLOSED
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        List<ILoggingEvent> halfOpenToClosedLog = searchCircuitBreakerLog(circuitBreaker, memoryAppender, CircuitBreaker.State.HALF_OPEN, CircuitBreaker.State.CLOSED);
        assertLogs(halfOpenToClosedLog, Level.WARN);
    }

    private static void assertSuccessfulInsert(BulkWriteResult insertResults) {
        assertThat(insertResults).isNotNull();
        assertThat(insertResults.wasAcknowledged()).isTrue();
        assertThat(insertResults.getInsertedCount()).isEqualTo(EXPECTED_INITIAL_SIZE);
        assertThat(insertResults.getDeletedCount()).isZero();
        assertThat(insertResults.getMatchedCount()).isZero();
        assertThat(insertResults.getModifiedCount()).isZero();
    }
}