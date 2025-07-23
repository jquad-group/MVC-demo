package de.datev.refsys.aggregation.processing.repository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;
import de.datev.refsys.aggregation.document.model.MasterData;
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
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MASTER_DATA;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.MASTER_DATA_REPOSITORY_ADD_INDIVIDUAL_PERSON_ACCOUNTS_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.MASTER_DATA_REPOSITORY_UNSET_INDIVIDUAL_PERSON_ACCOUNTS_LOG;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FALSE_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FALSE_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FALSE_FISCAL_YEAR;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2020_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_INDUSTRY_ID;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;

@ImportAutoConfiguration(TestMongoClientConfiguration.class)
@Import({ TestcontainersConfiguration.class, MongoSharedConfiguration.class, MasterDataRepository.class, MasterData.class, MongoHelperService.class })
@ContextConfiguration(classes = { TestResilienceConfiguration.class, TestMeterConfiguration.class })
@DataMongoTest
@ActiveProfiles(TEST_PROFILE)
@ClearDatabaseAnCreateIndexesBeforeEachTest
@ResetResilienceAfterEachTest
class MasterDataRepositoryTest {

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

    @Mock
    private MeterRegistry meterRegistry;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    private final int expectedInitialSize = 2;
    private MasterDataRepository masterDataRepository;
    private List<MasterData> expectedMasterData;
    private MemoryAppender memoryAppender;

    @BeforeEach
    void setup() {
        memoryAppender = setupMemoryAppender(memoryAppender, LoggingUtil.class, Level.DEBUG);
        masterDataRepository =
                new MasterDataRepository(insertMongoClient, updateMongoClient, new SimpleMeterRegistry(), databaseName, circuitBreakerRegistry,
                                         retryRegistry);
        expectedMasterData = TestDataLoader.loadMongoDBList("json/collections/repository/masterData.json", MasterData.class);
        assertThat(expectedMasterData).hasSize(expectedInitialSize);
    }

    @Test
    @DisplayName(MASTER_DATA + " Index Test")
    void should_insert_master_data_and_verify_indexes() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMasterData(expectedMasterData);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.wasAcknowledged()).isTrue();
        assertThat(insertManyResult.getInsertedIds()).isNotNull();

        List<Document> documents = mongoHelperService.listMasterDataIndexes();
        assertThat(documents).hasSize(2);
        MongoIndex mongoIdIndex = mongoHelperService.deserializeMongoIndex(documents.get(0).toBsonDocument().toJson());
        mongoHelperService.verifyIdIndex(mongoIdIndex);

        MongoIndex mongoIndex = mongoHelperService.deserializeMongoIndex(documents.get(1).toBsonDocument().toJson());
        mongoHelperService.verifyIndexForStateDocAndMasterData(mongoIndex);
    }

    @Test
    @DisplayName("Test MasterDataRepository delete one")
    void should_delete_one_if_given_correct_params_and_entry_exists() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMasterData(expectedMasterData);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(expectedInitialSize);

        DeleteResult deleteResult = masterDataRepository.deleteOne(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2020_START).block();
        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.getDeletedCount()).isEqualTo(1);
        List<MasterData> currentMasterData = mongoHelperService.findAllMasterData();
        assertThat(currentMasterData).hasSize(expectedInitialSize - 1);

        String expectedLog = LoggingUtil.MASTER_DATA_REPOSITORY_DELETE_ONE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppender.search(expectedLog);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test MasterDataRepository delete nothing with incorrect parameters")
    void should_not_delete_one_if_given_incorrect_params_and_entry_does_not_exists() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMasterData(expectedMasterData);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(expectedInitialSize);

        DeleteResult deleteResult = masterDataRepository.deleteOne(TEST_CONSULTANT, TEST_FALSE_CLIENT, TEST_FISCAL_YEAR_2020_START).block();
        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.getDeletedCount()).isZero();
        List<MasterData> currentMasterData = mongoHelperService.findAllMasterData();
        assertThat(currentMasterData).hasSize(expectedInitialSize);
    }

    @Test
    @DisplayName("Test update containsNearTimeData, industryId and schemaVersion")
    void should_update_schema_version_industry_id_and_contains_near_time_data() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMasterData(expectedMasterData);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(expectedInitialSize);
        masterDataRepository.updateFromSchemaVersionOneToFour(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2020_START, true, TEST_INDUSTRY_ID,
                                                              Collections.emptyList(), Collections.emptyList())
                            .block();
        MasterData foundMasterData = masterDataRepository.findOneByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2020_START).block();
        MasterData expectedMasterDataUpdate =
                TestDataLoader.loadDBElement(
                        "json/collections/repository/update-schema-from-one-to-four-master-data-with-industry-id.json",
                        MasterData.class);
        assertThat(foundMasterData).usingRecursiveComparison().ignoringFieldsOfTypes(OffsetDateTime.class).isEqualTo(expectedMasterDataUpdate);
    }

    @Test
    @DisplayName("Test update containsNearTimeData, customStructureInfos and schemaVersion")
    void should_update_schema_version_custom_structure_infos_and_contains_near_time_data() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMasterData(expectedMasterData);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(expectedInitialSize);
        MasterData expectedMasterDataUpdate =
                TestDataLoader.loadDBElement(
                        "json/collections/repository/update-schema-from-one-to-four-master-data-with-custom-structure-infos.json",
                        MasterData.class);
        masterDataRepository.updateFromSchemaVersionOneToFour(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2020_START, true, null,
                                                              expectedMasterDataUpdate.getContext().getCustomReportStructureInfos(),
                                                              expectedMasterDataUpdate.getContext().getCustomColumnStructureInfos())
                            .block();
        MasterData foundMasterData = masterDataRepository.findOneByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2020_START).block();
        assertThat(foundMasterData).usingRecursiveComparison().ignoringFieldsOfTypes(OffsetDateTime.class).isEqualTo(expectedMasterDataUpdate);

        String expectedLogUpdateSchema = LoggingUtil.MASTER_DATA_REPOSITORY_UPDATE_FROM_SCHEMA_VERSION_ONE_TO_TWO_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessageUpdateSchema = memoryAppender.search(expectedLogUpdateSchema);
        assertThat(logMessageUpdateSchema).hasSize(1);

        String expectedLogFindOne = LoggingUtil.MASTER_DATA_REPOSITORY_FIND_ONE_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessageFindOne = memoryAppender.search(expectedLogFindOne);
        assertThat(logMessageFindOne).hasSize(1);
    }

    @Test
    @DisplayName("Test writeNearTimeDataFlag to change boolean flag if given correct params")
    void should_change_near_time_data_flag_when_given_correct_params() {
        ClientSession clientSession = Mono.from(updateMongoClient.startSession()).block();
        mongoHelperService.insertManyMasterData(expectedMasterData);
        List<MasterData> allMasterData = mongoHelperService.findAllMasterData();
        Optional<MasterData> masterDataEntry =
                allMasterData.stream().filter(masterData -> masterData.getYearBegin().equals(TEST_FISCAL_YEAR_2021_START)).findAny();
        assertThat(masterDataEntry.isPresent()).isTrue();
        assertThat(masterDataEntry.get().getContext().getContainsNearTimeData()).isNull();
        UpdateResult updateResult = masterDataRepository.writeNearTimeDataFlag(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, clientSession,
                                                                        updateMongoClient, true).block();

        assertThat(updateResult).isNotNull();
        assertThat(updateResult.getModifiedCount()).isEqualTo(1);
        List<MasterData> allMasterDataResult = mongoHelperService.findAllMasterData();
        Optional<MasterData> masterDataEntryResult =
                allMasterDataResult.stream().filter(masterData -> masterData.getYearBegin().equals(TEST_FISCAL_YEAR_2021_START)).findAny();
        assertThat(masterDataEntryResult.isPresent()).isTrue();
        assertThat(masterDataEntryResult.get().getContext().getContainsNearTimeData()).isTrue();

        assertThat(memoryAppender.search(LoggingUtil.MASTER_DATA_REPOSITORY_WRITE_NEAR_TIME_DATA_FLAG_LOG.replace("{}ms", ""), Level.DEBUG)).hasSize(1);
    }

    @ParameterizedTest
    @MethodSource("testQueryParams")
    @DisplayName("Test writeNearTimeDataFlag to not change boolean flag if given wrong params")
    void should_not_change_near_time_data_flag_when_given_wrong_params(Integer consultant, Integer client, Integer fiscalYear) {
        ClientSession clientSession = Mono.from(updateMongoClient.startSession()).block();
        mongoHelperService.insertManyMasterData(expectedMasterData);
        List<MasterData> allMasterData = mongoHelperService.findAllMasterData();
        Optional<MasterData> masterDataEntry =
                allMasterData.stream().filter(masterData -> masterData.getConsultant().equals(consultant) && masterData.getClient().equals(client) && masterData.getYearBegin().equals(fiscalYear)).findAny();
        assertThat(masterDataEntry.isPresent()).isFalse();

        UpdateResult updateResult = masterDataRepository.writeNearTimeDataFlag(consultant, client, fiscalYear, clientSession,
                                                                               updateMongoClient, true).block();

        assertThat(updateResult).isNotNull();
        assertThat(updateResult.wasAcknowledged()).isTrue();
        assertThat(updateResult.getModifiedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Test addIndividualPersonAccounts to add unique accounts to Set when given correct params")
    void should_add_unique_individual_person_accounts_when_given_correct_params() {
        ClientSession clientSession = Mono.from(updateMongoClient.startSession()).block();
        Optional<MasterData> masterData2021 =
                expectedMasterData.stream().filter(masterData -> masterData.getYearBegin().equals(TEST_FISCAL_YEAR_2021_START)).findFirst();
        assertThat(masterData2021.isPresent()).isTrue();
        masterData2021.get().setIndividualPersonAccountNumbers(List.of(1));
        mongoHelperService.insertManyMasterData(expectedMasterData);
        List<MasterData> allMasterData = mongoHelperService.findAllMasterData();
        Optional<MasterData> masterDataEntry =
                allMasterData.stream().filter(masterData -> masterData.getYearBegin().equals(TEST_FISCAL_YEAR_2021_START)).findAny();
        assertThat(masterDataEntry.isPresent()).isTrue();
        assertThat(masterDataEntry.get().getIndividualPersonAccountNumbers()).isNotNull().hasSize(1);

        Set<Integer> indivAccountNumbers = Set.of(1, 2);
        UpdateResult updateResult =
                masterDataRepository.addIndividualPersonAccounts(clientSession, TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START,
                                                                 indivAccountNumbers).block();

        assertThat(updateResult).isNotNull();
        assertThat(updateResult.wasAcknowledged()).isTrue();
        assertThat(updateResult.getModifiedCount()).isEqualTo(1);
        List<MasterData> allMasterDataResult = mongoHelperService.findAllMasterData();
        Optional<MasterData> masterDataEntryResult =
                allMasterDataResult.stream().filter(masterData -> masterData.getYearBegin().equals(TEST_FISCAL_YEAR_2021_START)).findAny();
        assertThat(masterDataEntryResult.isPresent()).isTrue();
        assertThat(masterDataEntryResult.get().getIndividualPersonAccountNumbers()).isNotEmpty().hasSize(2);

        assertThat(memoryAppender.search(MASTER_DATA_REPOSITORY_ADD_INDIVIDUAL_PERSON_ACCOUNTS_LOG.replace("{}ms", ""), Level.DEBUG)).hasSize(1);
    }

    @ParameterizedTest
    @MethodSource("testQueryParams")
    @DisplayName("Test addIndividualPersonAccounts to not add accounts when given wrong params")
    void should_not_add_individual_person_accounts_when_given_wrong_params(Integer consultant, Integer client, Integer fiscalYear) {
        ClientSession clientSession = Mono.from(updateMongoClient.startSession()).block();
        mongoHelperService.insertManyMasterData(expectedMasterData);
        List<MasterData> allMasterData = mongoHelperService.findAllMasterData();
        Optional<MasterData> masterDataEntry = allMasterData.stream()
                                                            .filter(masterData -> masterData.getConsultant().equals(consultant)
                                                                    && masterData.getClient().equals(client) && masterData.getYearBegin()
                                                                                                                          .equals(fiscalYear))
                                                            .findAny();
        assertThat(masterDataEntry.isPresent()).isFalse();

        Set<Integer> indivAccountNumbers = Set.of(1, 2);
        UpdateResult updateResult =
                masterDataRepository.addIndividualPersonAccounts(clientSession, consultant, client, fiscalYear, indivAccountNumbers).block();

        assertThat(updateResult).isNotNull();
        assertThat(updateResult.wasAcknowledged()).isTrue();
        assertThat(updateResult.getModifiedCount()).isZero();
    }

    @Test
    @DisplayName("Test unsetIndividualPersonAccounts to delete the field when given correct params")
    void should_unset_individual_person_accounts_when_given_correct_params() {
        ClientSession clientSession = Mono.from(updateMongoClient.startSession()).block();
        Optional<MasterData> masterData2021 =
                expectedMasterData.stream().filter(masterData -> masterData.getYearBegin().equals(TEST_FISCAL_YEAR_2021_START)).findFirst();
        assertThat(masterData2021.isPresent()).isTrue();
        masterData2021.get().setIndividualPersonAccountNumbers(List.of(1, 2));
        mongoHelperService.insertManyMasterData(expectedMasterData);
        List<MasterData> allMasterData = mongoHelperService.findAllMasterData();
        Optional<MasterData> masterDataEntry =
                allMasterData.stream().filter(masterData -> masterData.getYearBegin().equals(TEST_FISCAL_YEAR_2021_START)).findAny();
        assertThat(masterDataEntry.isPresent()).isTrue();
        assertThat(masterDataEntry.get().getIndividualPersonAccountNumbers()).isNotNull().hasSize(2);

        UpdateResult updateResult =
                masterDataRepository.unsetIndividualPersonAccounts(clientSession, TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();

        assertThat(updateResult).isNotNull();
        assertThat(updateResult.wasAcknowledged()).isTrue();
        assertThat(updateResult.getModifiedCount()).isEqualTo(1);
        List<MasterData> allMasterDataResult = mongoHelperService.findAllMasterData();
        Optional<MasterData> masterDataEntryResult =
                allMasterDataResult.stream().filter(masterData -> masterData.getYearBegin().equals(TEST_FISCAL_YEAR_2021_START)).findAny();
        assertThat(masterDataEntryResult.isPresent()).isTrue();
        assertThat(masterDataEntryResult.get().getIndividualPersonAccountNumbers()).isNull();

        assertThat(memoryAppender.search(MASTER_DATA_REPOSITORY_UNSET_INDIVIDUAL_PERSON_ACCOUNTS_LOG.replace("{}ms", ""), Level.DEBUG)).hasSize(1);
    }

    @ParameterizedTest
    @MethodSource("testQueryParams")
    @DisplayName("Test unsetIndividualPersonAccounts to not delete the field when given wrong params")
    void should_not_unset_individual_person_accounts_when_given_wrong_params(Integer consultant, Integer client, Integer fiscalYear) {
        ClientSession clientSession = Mono.from(updateMongoClient.startSession()).block();
        mongoHelperService.insertManyMasterData(expectedMasterData);

        List<MasterData> allMasterData = mongoHelperService.findAllMasterData();
        Optional<MasterData> masterDataEntry = allMasterData.stream()
                                                            .filter(masterData -> masterData.getConsultant().equals(consultant)
                                                                    && masterData.getClient().equals(client) && masterData.getYearBegin()
                                                                                                                          .equals(fiscalYear))
                                                            .findAny();
        assertThat(masterDataEntry.isPresent()).isFalse();

        UpdateResult updateResult =
                masterDataRepository.unsetIndividualPersonAccounts(clientSession, consultant, client, fiscalYear).block();

        assertThat(updateResult).isNotNull();
        assertThat(updateResult.wasAcknowledged()).isTrue();
        assertThat(updateResult.getModifiedCount()).isEqualTo(0);
    }

    private static Stream<Arguments> testQueryParams() {
        return Stream.of(
                Arguments.arguments(TEST_CONSULTANT, TEST_CLIENT, TEST_FALSE_FISCAL_YEAR),
                Arguments.arguments(TEST_CONSULTANT, TEST_FALSE_CLIENT, TEST_FISCAL_YEAR_2021_START),
                Arguments.arguments(TEST_FALSE_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START)
                        );
    }

    // TODO add retry and circuitbreaker tests once the MasterDataRepository.upsertOne methode has been replaced with MasterDataRepository.insertOne
}