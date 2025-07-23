package de.datev.refsys.aggregation.processing.repository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.reactivestreams.client.MongoClient;
import de.datev.refsys.aggregation.document.model.CustomColumnStructureContent;
import de.datev.refsys.aggregation.processing.config.TestMeterConfiguration;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.config.mongo.MongoSharedConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import de.datev.refsys.aggregation.processing.model.MongoIndex;
import de.datev.refsys.aggregation.processing.util.ClearDatabaseAnCreateIndexesBeforeEachTest;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.MongoHelperService;
import de.datev.refsys.aggregation.processing.util.ResetResilienceAfterEachTest;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

import java.util.List;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.CUSTOM_COLUMN_STRUCTURE_CONTENTS;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2022_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ImportAutoConfiguration(TestMongoClientConfiguration.class)
@Import({ TestcontainersConfiguration.class, MongoSharedConfiguration.class, MongoHelperService.class })
@ContextConfiguration(classes = { TestResilienceConfiguration.class, TestMeterConfiguration.class})
@DataMongoTest
@ActiveProfiles(TEST_PROFILE)
@ClearDatabaseAnCreateIndexesBeforeEachTest
@ResetResilienceAfterEachTest
class CustomColumnStructureContentRepositoryTest {
    private static final int EXPECTED_INITIAL_SIZE = 2;

    @Autowired
    private MongoClient insertMongoClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private MongoHelperService mongoHelperService;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    List<CustomColumnStructureContent> expected;
    private CustomColumnStructureContentRepository customColumnStructureContentRepository;
    private MemoryAppender memoryAppender;

    @BeforeEach
    void setUp() {
        memoryAppender = setupMemoryAppender(memoryAppender, LoggingUtil.class, Level.DEBUG);
        customColumnStructureContentRepository =
                new CustomColumnStructureContentRepository(insertMongoClient, new SimpleMeterRegistry(), databaseName,
                                                           circuitBreakerRegistry, retryRegistry);
        expected =
                TestDataLoader.loadMongoDBList("json/collections/repository/customColumnStructureContents.json", CustomColumnStructureContent.class);
    }

    @Test
    @DisplayName(CUSTOM_COLUMN_STRUCTURE_CONTENTS + " Index Test")
    void should_insert_custom_column_structure_contents_and_verify_indexes() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyCustomColumnStructureContents(expected);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.wasAcknowledged()).isTrue();
        assertThat(insertManyResult.getInsertedIds()).isNotNull();

        List<Document> documents = mongoHelperService.listCustomColumnContentIndexes();
        assertThat(documents).hasSize(2);
        MongoIndex mongoIdIndex = mongoHelperService.deserializeMongoIndex(documents.get(0).toBsonDocument().toJson());
        mongoHelperService.verifyIdIndex(mongoIdIndex);

        MongoIndex mongoIndex = mongoHelperService.deserializeMongoIndex(documents.get(1).toBsonDocument().toJson());
        mongoHelperService.verifyIndexForCustomColumnStructureContent(mongoIndex);
    }

    @Test
    @DisplayName("Test CustomColumnStructureContentRepository delete by BusinessKey")
    void should_delete_many_if_given_correct_params_and_entries_exist() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyCustomColumnStructureContents(expected);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_INITIAL_SIZE);

        DeleteResult deleteResult =
                customColumnStructureContentRepository.deleteManyByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();

        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.wasAcknowledged()).isTrue();
        assertThat(deleteResult.getDeletedCount()).isEqualTo(2L);
        List<CustomColumnStructureContent> currentCustomColumnStructureContent = mongoHelperService.findAllCustomColumnStructureContents();
        assertThat(currentCustomColumnStructureContent).hasSize(EXPECTED_INITIAL_SIZE - (int) deleteResult.getDeletedCount());

        String expectedLog = LoggingUtil.CUSTOM_COLUMN_STRUCTURE_REPOSITORY_DELETE_MANY_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppender.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test CustomColumnStructureContentRepository delete by BusinessKey if entry does not exist")
    void should_delete_none_if_given_correct_params_and_entry_does_not_exist() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyCustomColumnStructureContents(expected);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_INITIAL_SIZE);

        DeleteResult deleteResult =
                customColumnStructureContentRepository.deleteManyByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2022_START).block();

        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.wasAcknowledged()).isTrue();
        assertThat(deleteResult.getDeletedCount()).isZero();
        List<CustomColumnStructureContent> currentCustomColumnStructureContent = mongoHelperService.findAllCustomColumnStructureContents();
        assertThat(currentCustomColumnStructureContent).hasSize(EXPECTED_INITIAL_SIZE);
        String expectedLog = LoggingUtil.CUSTOM_COLUMN_STRUCTURE_REPOSITORY_DELETE_MANY_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppender.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test CustomColumnStructureContentRepository bulk insert")
    void should_bulk_insert_if_given_correct_params() {
        BulkWriteResult bulkWriteResult =
                customColumnStructureContentRepository.bulkInsert(expected).block();
        assertSuccessfulInsert(bulkWriteResult);

        String expectedLog = LoggingUtil.CUSTOM_COLUMN_STRUCTURE_REPOSITORY_BULK_INSERT_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppender.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Tests resilience retry when an error occurs")
    void should_retry_when_the_retry_exception_is_thrown() {
        Retry retry = retryRegistry.retry(MONGODB_RETRY_INSTANCE_NAME);
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isZero();
        // insert and assert data
        BulkWriteResult bulkWriteResult = customColumnStructureContentRepository.bulkInsert(expected).block();
        assertSuccessfulInsert(bulkWriteResult);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        // insert again to cause an exception (because of index)
        Throwable throwable = catchThrowable(() -> customColumnStructureContentRepository.bulkInsert(expected).block());
        assertThat(throwable).isNotNull().isInstanceOf(MongoBulkWriteException.class);
        // assert failed retry
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(retry.getRetryConfig().getMaxAttempts());
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isZero();
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
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