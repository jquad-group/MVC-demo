package de.datev.refsys.aggregation.processing.repository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.reactivestreams.client.MongoClient;
import de.datev.refsys.aggregation.document.model.MasterDataAccount;
import de.datev.refsys.aggregation.processing.config.TestMeterConfiguration;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.config.mongo.MongoSharedConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.mapper.AccountPurposeMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountPurposeMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.InventoryMapper;
import de.datev.refsys.aggregation.processing.mapper.InventoryMapperImpl;
import de.datev.refsys.aggregation.processing.model.MongoIndex;
import de.datev.refsys.aggregation.processing.util.CircuitBreakerUtil;
import de.datev.refsys.aggregation.processing.util.ClearDatabaseAnCreateIndexesBeforeEachTest;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.MongoHelperService;
import de.datev.refsys.aggregation.processing.util.ResetResilienceAfterEachTest;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.generated.acds.api.model.AccountCaption;
import de.datev.refsys.generated.acds.api.model.AccountPurposeMapping;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.generated.acds.api.model.MasterdataInventory;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.ThrowableAssert;
import org.awaitility.Awaitility;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MASTER_DATA_ACCOUNTS;
import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.ACCOUNT_NUMBER_CHECK_ERROR;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME;
import static de.datev.refsys.aggregation.processing.util.TestUtil.CALL_NOT_PERMITTED_CIRCUIT_BREAKER_ERROR;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_ACCOUNT_NUMBERS_SET;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_ACCOUNT_NUMBER_3;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_ACCOUNT_NUMBER_4;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_ACCOUNT_NUMBER_5;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_ACCOUNT_NUMBER_6;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FALSE_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.assertLogs;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createAccountDescription;
import static de.datev.refsys.aggregation.processing.util.TestUtil.searchCircuitBreakerLog;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ImportAutoConfiguration(TestMongoClientConfiguration.class)
@Import({ TestcontainersConfiguration.class, MongoSharedConfiguration.class, MasterDataAccountRepository.class, AccountPurposeMapperImpl.class,
        InventoryMapperImpl.class, MongoHelperService.class })
@ContextConfiguration(classes = { TestResilienceConfiguration.class, TestMeterConfiguration.class})
@DataMongoTest
@ActiveProfiles(TEST_PROFILE)
@ClearDatabaseAnCreateIndexesBeforeEachTest
@ResetResilienceAfterEachTest
class MasterDataAccountRepositoryTest {
    private static final int EXPECTED_INITIAL_SIZE = 4;

    @Autowired
    private AccountPurposeMapper accountPurposeMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private MongoClient insertMongoClient;

    @Qualifier("updateMongoClient")
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

    private MasterDataAccountRepository masterDataAccountRepository;
    private List<MasterDataAccount> expectedMasterDataAccounts;
    private MemoryAppender memoryAppender;
    private MemoryAppender memoryAppenderDebugLevel;

    @BeforeEach
    void setup() {
        memoryAppenderDebugLevel = setupMemoryAppender(memoryAppenderDebugLevel, LoggingUtil.class, Level.DEBUG);
        memoryAppender = setupMemoryAppender(memoryAppender, CircuitBreakerUtil.class, Level.WARN);
        masterDataAccountRepository =
                new MasterDataAccountRepository(insertMongoClient, updateMongoClient, new SimpleMeterRegistry(), accountPurposeMapper,
                                                inventoryMapper, databaseName, circuitBreakerRegistry, retryRegistry);
        expectedMasterDataAccounts = TestDataLoader.loadMongoDBList("json/collections/repository/masterDataAccounts.json", MasterDataAccount.class);
        assertThat(expectedMasterDataAccounts).hasSize(EXPECTED_INITIAL_SIZE);
    }

    @Test
    @DisplayName(MASTER_DATA_ACCOUNTS + " Index Test")
    void should_insert_master_data_accounts_and_verify_indexes() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMasterDataAccounts(expectedMasterDataAccounts);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.wasAcknowledged()).isTrue();
        assertThat(insertManyResult.getInsertedIds()).isNotNull();

        List<Document> documents = mongoHelperService.listMasterDataAccountsIndexes();
        assertThat(documents).hasSize(2);
        MongoIndex mongoIdIndex = mongoHelperService.deserializeMongoIndex(documents.get(0).toBsonDocument().toJson());
        mongoHelperService.verifyIdIndex(mongoIdIndex);

        MongoIndex mongoIndex = mongoHelperService.deserializeMongoIndex(documents.get(1).toBsonDocument().toJson());
        mongoHelperService.verifyIndexForMasterDataAccount(mongoIndex);
    }

    @Test
    @DisplayName("Test MasterDataAccountRepository find all by BusinessKey")
    void should_find_the_correct_data_when_given_correct_parameters() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMasterDataAccounts(expectedMasterDataAccounts);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_INITIAL_SIZE);

        List<MasterDataAccount> findResults = Mono.from(updateMongoClient.startSession())
                                                  .flatMap(cs -> masterDataAccountRepository
                                                          .findAllByBusinessKeyAndAccountNumbers(cs, TEST_CONSULTANT, TEST_CLIENT,
                                                                                                 TEST_FISCAL_YEAR_2021_START,
                                                                                                 TEST_ACCOUNT_NUMBERS_SET)
                                                          .collectList()).block();
        assertThat(findResults).isNotNull().hasSize(1);
        assertThat(expectedMasterDataAccounts).usingRecursiveFieldByFieldElementComparator().contains(findResults.get(0));
        assertThat(findResults.get(0).getConsultant()).isEqualTo(TEST_CONSULTANT);
        assertThat(findResults.get(0).getClient()).isEqualTo(TEST_CLIENT);
        assertThat(findResults.get(0).getYearBegin()).isEqualTo(TEST_FISCAL_YEAR_2021_START);
        assertThat(findResults.get(0).getAccountCaptions().stream().filter(caption -> TEST_ACCOUNT_NUMBERS_SET.contains(caption.getAccountNumber())).toList()).hasSize(2);

        String expectedLog = LoggingUtil.MASTER_DATA_ACCOUNT_REPOSITORY_FIND_ALL_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test MasterDataAccountRepository find nothing with incorrect parameters")
    void should_not_find_the_data_when_given_incorrect_parameters() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMasterDataAccounts(expectedMasterDataAccounts);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_INITIAL_SIZE);

        List<MasterDataAccount> findResults = Mono.from(updateMongoClient.startSession())
                                                  .flatMap(cs -> masterDataAccountRepository
                                                          .findAllByBusinessKeyAndAccountNumbers(cs, TEST_FALSE_CONSULTANT, TEST_CLIENT,
                                                                                                 TEST_FISCAL_YEAR_2021_START,
                                                                                                 TEST_ACCOUNT_NUMBERS_SET)
                                                          .collectList()).block();
        assertThat(findResults).isNotNull().isEmpty();

        String expectedLog = LoggingUtil.MASTER_DATA_ACCOUNT_REPOSITORY_FIND_ALL_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).isEmpty();
    }

    @Test
    @DisplayName("Test MasterDataAccountRepository delete by BusinessKey")
    void should_delete_entries_when_entries_exists() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMasterDataAccounts(expectedMasterDataAccounts);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_INITIAL_SIZE);

        DeleteResult deleteResult =
                masterDataAccountRepository.deleteManyByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();
        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.wasAcknowledged()).isTrue();
        int deletedCount = Math.toIntExact(deleteResult.getDeletedCount());
        assertThat(deletedCount).isEqualTo(3);
        List<MasterDataAccount> currentMasterDataAccounts = mongoHelperService.findAllMasterDataAccounts();
        assertThat(currentMasterDataAccounts).hasSize(EXPECTED_INITIAL_SIZE - deletedCount);

        String expectedLog = LoggingUtil.MASTER_DATA_ACCOUNT_REPOSITORY_DELETE_MANY_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test MasterDataAccountRepository delete nothing with incorrect parameters")
    void should_not_delete_any_entries_when_no_entries_to_params_exists() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMasterDataAccounts(expectedMasterDataAccounts);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_INITIAL_SIZE);

        DeleteResult deleteResult =
                masterDataAccountRepository.deleteManyByBusinessKey(TEST_FALSE_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();
        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.wasAcknowledged()).isTrue();
        assertThat(deleteResult.getDeletedCount()).isZero();
        List<MasterDataAccount> currentMasterDataAccounts = mongoHelperService.findAllMasterDataAccounts();
        assertThat(currentMasterDataAccounts).hasSize(EXPECTED_INITIAL_SIZE);

        String expectedLog = LoggingUtil.MASTER_DATA_ACCOUNT_REPOSITORY_DELETE_MANY_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test MasterDataAccountRepository bulk insert")
    void should_bulk_insert_if_given_correct_params() {
        MasterdataContext masterDataContext =
                TestDataLoader.load("json/acds-responses/repository/master-data-context-2021.json", MasterdataContext.class);
        List<AccountPurposeMapping> accountPurposeMappings =
                TestDataLoader.loadList("json/acds-responses/repository/account-purpose-mappings.json", new TypeReference<>() {
                });
        List<AccountCaption> accountCaptions =
                TestDataLoader.loadList("json/acds-responses/repository/account-captions.json", new TypeReference<>() {
                });
        List<MasterdataInventory> inventories =
                TestDataLoader.loadList("json/acds-responses/repository/master-data-inventories.json", new TypeReference<>() {
                });

        Set<Integer> emptySet = new HashSet<>();
        BulkWriteResult bulkWriteResult =
                masterDataAccountRepository.bulkInsert(masterDataContext, accountPurposeMappings, accountCaptions, inventories, emptySet).block();
        assertSuccessfulInsert(bulkWriteResult);

        String expectedLog = LoggingUtil.MASTER_DATA_ACCOUNT_REPOSITORY_BULK_INSERT_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test MasterDataAccountRepository bulk update")
    void should_bulk_update_if_correct_params_are_given() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMasterDataAccounts(expectedMasterDataAccounts);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_INITIAL_SIZE);

        List<MasterDataAccount> collectedList =
                expectedMasterDataAccounts.stream().filter(mda -> mda.getAccountNumberFrom().equals(TEST_ACCOUNT_NUMBER_3)
                        || mda.getAccountNumberFrom().equals(TEST_ACCOUNT_NUMBER_5)).toList();
        assertThat(collectedList).hasSize(2);

        MasterDataAccount masterDataAccountUpdate1 = collectedList.get(0);
        assertThat(masterDataAccountUpdate1.getUsed()).isFalse();
        masterDataAccountUpdate1.setAccountCaptions(
                List.of(createAccountDescription(TEST_ACCOUNT_NUMBER_3), createAccountDescription(TEST_ACCOUNT_NUMBER_4)));

        MasterDataAccount masterDataAccountUpdate2 = collectedList.get(1);
        assertThat(masterDataAccountUpdate2.getUsed()).isFalse();
        masterDataAccountUpdate2.setAccountCaptions(
                List.of(createAccountDescription(TEST_ACCOUNT_NUMBER_5), createAccountDescription(TEST_ACCOUNT_NUMBER_6)));

        BulkWriteResult bulkWriteResult = masterDataAccountRepository.bulkUpdate(List.of(masterDataAccountUpdate1, masterDataAccountUpdate2)).block();
        assertThat(bulkWriteResult).isNotNull();
        assertThat(bulkWriteResult.wasAcknowledged()).isTrue();
        assertThat(bulkWriteResult.getMatchedCount()).isEqualTo(2);
        assertThat(bulkWriteResult.getModifiedCount()).isEqualTo(2);
        assertThat(bulkWriteResult.getDeletedCount()).isZero();
        assertThat(bulkWriteResult.getInsertedCount()).isZero();

        List<MasterDataAccount> currentMasterDataAccountList = mongoHelperService.findAllMasterDataAccounts();
        assertThat(currentMasterDataAccountList).isNotNull();
        List<MasterDataAccount> currentList =
                currentMasterDataAccountList.stream().filter(mda -> mda.getAccountNumberFrom().equals(TEST_ACCOUNT_NUMBER_3)
                        || mda.getAccountNumberFrom().equals(TEST_ACCOUNT_NUMBER_5)).toList();

        assertThat(currentList).hasSize(2);
        MasterDataAccount firstEntry = currentList.get(0);
        MasterDataAccount secondEntry = currentList.get(1);

        assertThat(firstEntry.getUsed()).isTrue();
        assertThat(firstEntry.getAccountCaptions()).hasSize(2);
        assertThat(firstEntry.getAccountCaptions().get(0).getUsed()).isTrue();
        assertThat(firstEntry.getAccountCaptions().get(0).getAccountNumber()).isEqualTo(TEST_ACCOUNT_NUMBER_3);
        assertThat(firstEntry.getAccountCaptions().get(1).getUsed()).isTrue();
        assertThat(firstEntry.getAccountCaptions().get(1).getAccountNumber()).isEqualTo(TEST_ACCOUNT_NUMBER_4);

        assertThat(secondEntry.getUsed()).isTrue();
        assertThat(secondEntry.getAccountCaptions()).hasSize(2);
        assertThat(secondEntry.getAccountCaptions().get(0).getUsed()).isTrue();
        assertThat(secondEntry.getAccountCaptions().get(0).getAccountNumber()).isEqualTo(TEST_ACCOUNT_NUMBER_5);
        assertThat(secondEntry.getAccountCaptions().get(1).getUsed()).isTrue();
        assertThat(secondEntry.getAccountCaptions().get(1).getAccountNumber()).isEqualTo(TEST_ACCOUNT_NUMBER_6);

        String expectedLog = LoggingUtil.MASTER_DATA_ACCOUNT_REPOSITORY_BULK_UPDATE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderDebugLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Tests resilience retry when an error occurs")
    void should_retry_when_the_retry_exception_is_thrown() {
        Retry retry = retryRegistry.retry(MONGODB_RETRY_INSTANCE_NAME);
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isZero();
        // insert and assert data
        MasterdataContext masterDataContext =
                TestDataLoader.load("json/acds-responses/repository/master-data-context-2021.json", MasterdataContext.class);
        List<AccountPurposeMapping> accountPurposeMappings =
                TestDataLoader.loadList("json/acds-responses/repository/account-purpose-mappings.json", new TypeReference<>() {
                });
        List<AccountCaption> accountCaptions =
                TestDataLoader.loadList("json/acds-responses/repository/account-captions.json", new TypeReference<>() {
                });
        List<MasterdataInventory> inventories =
                TestDataLoader.loadList("json/acds-responses/repository/master-data-inventories.json", new TypeReference<>() {
                });
        BulkWriteResult insertResults =
                masterDataAccountRepository.bulkInsert(masterDataContext, accountPurposeMappings, accountCaptions, inventories, new HashSet<>()).block();
        assertSuccessfulInsert(insertResults);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        // insert again to cause an exception (because of index)
        Throwable throwable = catchThrowable(
                () -> masterDataAccountRepository.bulkInsert(masterDataContext, accountPurposeMappings, accountCaptions, inventories,  new HashSet<>()).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoBulkWriteException.class);
        // assert failed retry
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(retry.getRetryConfig().getMaxAttempts());
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isZero();
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Tests resilience circuit breaker when an error occurs")
    void should_change_circuit_breaker_state_from_close_to_open_when_an_error_occurs() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER);
        // insert and assert data
        MasterdataContext masterDataContext =
                TestDataLoader.load("json/acds-responses/repository/master-data-context-2021.json", MasterdataContext.class);
        List<AccountPurposeMapping> accountPurposeMappings =
                TestDataLoader.loadList("json/acds-responses/repository/account-purpose-mappings.json", new TypeReference<>() {
                });
        List<AccountCaption> accountCaptions =
                TestDataLoader.loadList("json/acds-responses/repository/account-captions.json", new TypeReference<>() {
                });
        List<MasterdataInventory> inventories =
                TestDataLoader.loadList("json/acds-responses/repository/master-data-inventories.json", new TypeReference<>() {
                });
        BulkWriteResult insertResults =
                masterDataAccountRepository.bulkInsert(masterDataContext, accountPurposeMappings, accountCaptions, inventories, new HashSet<>()).block();
        assertSuccessfulInsert(insertResults);
        // assert state not changed (CLOSED)
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        // cause an exception
        Throwable throwable = catchThrowable(
                () -> masterDataAccountRepository.bulkInsert(masterDataContext, accountPurposeMappings, accountCaptions, inventories, new HashSet<>()).block());
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
        throwable = catchThrowable(
                () -> masterDataAccountRepository.bulkInsert(masterDataContext, accountPurposeMappings, accountCaptions, inventories, new HashSet<>()).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoBulkWriteException.class);
        throwable = catchThrowable(
                () -> masterDataAccountRepository.bulkInsert(masterDataContext, accountPurposeMappings, accountCaptions, inventories, new HashSet<>()).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoBulkWriteException.class);
        // HALF OPEN -> OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        List<ILoggingEvent> halfOpenToOpen = searchCircuitBreakerLog(circuitBreaker, memoryAppender, CircuitBreaker.State.HALF_OPEN, CircuitBreaker.State.OPEN);
        assertLogs(halfOpenToOpen, Level.WARN);

        // send 1 bad requests, expect CallNotPermittedException exception
        throwable = catchThrowable(
                () -> masterDataAccountRepository.bulkInsert(masterDataContext, accountPurposeMappings, accountCaptions, inventories, new HashSet<>()).block());
        assertThat(throwable).isNotNull().isInstanceOf(CallNotPermittedException.class);
        CallNotPermittedException callNotPermittedException = (CallNotPermittedException) throwable;
        assertThat(callNotPermittedException.getMessage()).isEqualTo(String.format(CALL_NOT_PERMITTED_CIRCUIT_BREAKER_ERROR, AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER));
        assertThat(callNotPermittedException.getCausingCircuitBreakerName()).isEqualTo(AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER);
        // OPEN -> HALF OPEN
        Awaitility.await().timeout(20L, TimeUnit.SECONDS)
                  .untilAsserted(() -> assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN));
        // bulk insert again on empty collection
        mongoHelperService.deleteAllMasterDataAccounts();
        insertResults = masterDataAccountRepository.bulkInsert(masterDataContext, accountPurposeMappings, accountCaptions, inventories, new HashSet<>()).block();
        assertSuccessfulInsert(insertResults);
        // assert State did not change (HALF OPEN)
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        // bulk insert again on empty collection
        mongoHelperService.deleteAllMasterDataAccounts();
        insertResults = masterDataAccountRepository.bulkInsert(masterDataContext, accountPurposeMappings, accountCaptions, inventories, new HashSet<>()).block();
        assertSuccessfulInsert(insertResults);
        // HALF OPEN -> CLOSED
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        List<ILoggingEvent> halfOpenToClosedLog = searchCircuitBreakerLog(circuitBreaker, memoryAppender, CircuitBreaker.State.HALF_OPEN, CircuitBreaker.State.CLOSED);
        assertLogs(halfOpenToClosedLog, Level.WARN);
    }

    @Test
    @DisplayName("Test MasterDataAccountRepository bulk insert to throw and error when accountNumberFrom is bigger than accountNumberTo")
    void should_throw_error_if_given_wrong_params() {
        MasterdataContext masterDataContext =
                TestDataLoader.load("json/acds-responses/repository/master-data-context-2021.json", MasterdataContext.class);
        List<AccountPurposeMapping> accountPurposeMappings =
                TestDataLoader.loadList("json/acds-responses/repository/account-purpose-mappings-invalid.json", new TypeReference<>() {
                });
        List<AccountCaption> accountCaptions =
                TestDataLoader.loadList("json/acds-responses/repository/account-captions.json", new TypeReference<>() {
                });
        List<MasterdataInventory> inventories =
                TestDataLoader.loadList("json/acds-responses/repository/master-data-inventories.json", new TypeReference<>() {
                });

        Set<Integer> emptySet = new HashSet<>();
        Throwable throwable = ThrowableAssert.catchThrowable(() -> masterDataAccountRepository.bulkInsert(masterDataContext, accountPurposeMappings, accountCaptions, inventories, emptySet).block());
        assertThat(throwable).isNotNull().isInstanceOf(AggregationProcessingBusinessException.class).hasMessage(ACCOUNT_NUMBER_CHECK_ERROR);
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