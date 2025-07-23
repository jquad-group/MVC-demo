package de.datev.refsys.aggregation.processing.repository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import de.datev.refsys.aggregation.document.model.MovementDataInventory;
import de.datev.refsys.aggregation.processing.config.TestMeterConfiguration;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.config.mongo.MongoSharedConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.mapper.InventoryDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.InventoryDbKeyFieldsMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.InventoryValueMapperImpl;
import de.datev.refsys.aggregation.processing.model.ExtendedMovementdataInventory;
import de.datev.refsys.aggregation.processing.model.InventoryDbKeyFields;
import de.datev.refsys.aggregation.processing.model.MongoIndex;
import de.datev.refsys.aggregation.processing.util.ClearDatabaseAnCreateIndexesBeforeEachTest;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.MongoHelperService;
import de.datev.refsys.aggregation.processing.util.ResetResilienceAfterEachTest;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_INVENTORIES;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FALSE_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_INVENTORY_NUMBER_2;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;

@ImportAutoConfiguration(TestMongoClientConfiguration.class)
@Import({ TestcontainersConfiguration.class, MongoSharedConfiguration.class, MovementDataInventoryRepository.class,
        InventoryDbKeyFieldsMapperImpl.class,
        InventoryValueMapperImpl.class, MongoHelperService.class  })
@ContextConfiguration(classes = { TestResilienceConfiguration.class, TestMeterConfiguration.class })
@DataMongoTest
@ActiveProfiles(TEST_PROFILE)
@ClearDatabaseAnCreateIndexesBeforeEachTest
@ResetResilienceAfterEachTest
class MovementDataInventoryRepositoryTest {
    private static final int EXPECTED_INITIAL_SIZE = 5;

    @Autowired
    private MovementDataInventoryRepository movementDataInventoryRepository;

    @Autowired
    private InventoryDbKeyFieldsMapper inventoryDbKeyFieldsMapper;

    @Autowired
    private MongoHelperService mongoHelperService;

    private List<MovementDataInventory> expectedMovementDataInventories;
    private MemoryAppender memoryAppender;
    private MemoryAppender memoryAppenderTraceLevel;

    @BeforeEach
    void setUP() {
        memoryAppenderTraceLevel = setupMemoryAppender(memoryAppenderTraceLevel, LoggingUtil.class, Level.TRACE);
        memoryAppender = setupMemoryAppender(memoryAppender, MovementDataInventoryRepository.class, Level.ERROR);
        expectedMovementDataInventories =
                TestDataLoader.loadMongoDBList("json/collections/repository/movementDataInventories.json", MovementDataInventory.class);
    }

    @Test
    @DisplayName(MOVEMENT_DATA_INVENTORIES + " Index Test")
    void should_insert_movement_data_inventories_and_verify_indexes() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMovementDataInventories(expectedMovementDataInventories);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.wasAcknowledged()).isTrue();
        assertThat(insertManyResult.getInsertedIds()).isNotNull();

        List<Document> documents = mongoHelperService.listMovementDataInventoriesIndexes();
        assertThat(documents).hasSize(2);
        MongoIndex mongoIdIndex = mongoHelperService.deserializeMongoIndex(documents.get(0).toBsonDocument().toJson());
        mongoHelperService.verifyIdIndex(mongoIdIndex);

        MongoIndex mongoIndex = mongoHelperService.deserializeMongoIndex(documents.get(1).toBsonDocument().toJson());
        mongoHelperService.verifyIndexForMovementDataInventories(mongoIndex);
    }

    @Test
    @DisplayName("Test MovementDataInventoryRepository delete by BusinessKey")
    void should_delete_many_if_given_correct_params_and_entries_exist() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMovementDataInventories(expectedMovementDataInventories);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_INITIAL_SIZE);

        DeleteResult deleteResult =
                movementDataInventoryRepository.deleteManyByBusinessKey(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();
        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.wasAcknowledged()).isTrue();
        int deletedCount = Math.toIntExact(deleteResult.getDeletedCount());
        assertThat(deletedCount).isEqualTo(3);

        List<MovementDataInventory> leftoverMovementDataInventories = mongoHelperService.findAllMovementDataInventories();
        assertThat(leftoverMovementDataInventories).hasSize(EXPECTED_INITIAL_SIZE - deletedCount);

        String expectedLog = LoggingUtil.MOVEMENT_DATA_INVENTORY_REPOSITORY_DELETE_MANY_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderTraceLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test MovementDataInventoryRepository delete nothing with incorrect parameters")
    void should_not_delete_any_if_given_incorrect_params() {
        InsertManyResult insertManyResult = mongoHelperService.insertManyMovementDataInventories(expectedMovementDataInventories);
        assertThat(insertManyResult).isNotNull();
        assertThat(insertManyResult.getInsertedIds()).hasSize(EXPECTED_INITIAL_SIZE);

        DeleteResult deleteResult =
                movementDataInventoryRepository.deleteManyByBusinessKey(TEST_FALSE_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START).block();
        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.wasAcknowledged()).isTrue();
        assertThat(deleteResult.getDeletedCount()).isZero();
        List<MovementDataInventory> leftoverMovementDataInventories = mongoHelperService.findAllMovementDataInventories();
        assertThat(leftoverMovementDataInventories).hasSize(EXPECTED_INITIAL_SIZE);

        String expectedLog = LoggingUtil.MOVEMENT_DATA_INVENTORY_REPOSITORY_DELETE_MANY_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderTraceLevel.search(expectedLog, Level.DEBUG);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test MovementDataInventoryRepository bulk upsert")
    void should_bulk_insert_without_data_present_in_the_collection() {
        List<ExtendedMovementdataInventory> extendedMovementdataInventories =
                TestDataLoader.loadList("json/collections/repository/extendedMovementdataInventoriesInput.json", new TypeReference<>() {});

        MasterdataContext masterdataContext2020 = TestDataLoader.load("json/acds-responses/wiremock/master-data-context-2020.json", MasterdataContext.class);
        MasterdataContext masterdataContext2021 = TestDataLoader.load("json/acds-responses/wiremock/master-data-context-2021.json", MasterdataContext.class);


        Map<InventoryDbKeyFields, List<ExtendedMovementdataInventory>> movementDataInventoriesMap = new LinkedHashMap<>();

        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext2021, extendedMovementdataInventories.get(0)),
                                       List.of(extendedMovementdataInventories.get(0)));
        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext2021, extendedMovementdataInventories.get(1)),
                                       List.of(extendedMovementdataInventories.get(1), extendedMovementdataInventories.get(2)));
        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext2021, extendedMovementdataInventories.get(3)),
                                       List.of(extendedMovementdataInventories.get(3), extendedMovementdataInventories.get(4)));
        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext2020, extendedMovementdataInventories.get(5)),
                                       List.of(extendedMovementdataInventories.get(5), extendedMovementdataInventories.get(6)));
        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext2020, extendedMovementdataInventories.get(7)),
                                       List.of(extendedMovementdataInventories.get(7)));

        BulkWriteResult insertResults = movementDataInventoryRepository.bulkUpsert(movementDataInventoriesMap).block();

        assertSuccessfulInsert(insertResults);

        List<MovementDataInventory> movementDataInventories = mongoHelperService.findAllMovementDataInventories();
        assertThat(movementDataInventories).hasSize(EXPECTED_INITIAL_SIZE);
        assertThat(movementDataInventories).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMovementDataInventories);

        String expectedLog = LoggingUtil.MOVEMENT_DATA_INVENTORY_REPOSITORY_BULK_UPSERT_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderTraceLevel.search(expectedLog, Level.TRACE);
        assertThat(logMessage).hasSize(1);
    }

    @Test
    @DisplayName("Test MovementDataInventoryRepository bulk update")
    void should_bulk_update_with_data_already_present_in_the_collection() {
        List<ExtendedMovementdataInventory> extendedMovementdataInventories =
                TestDataLoader.loadList("json/collections/repository/extendedMovementdataInventoriesInput.json", new TypeReference<>() {});

        MasterdataContext masterdataContext2020 = TestDataLoader.load("json/acds-responses/wiremock/master-data-context-2020.json", MasterdataContext.class);
        MasterdataContext masterdataContext2021 = TestDataLoader.load("json/acds-responses/wiremock/master-data-context-2021.json", MasterdataContext.class);


        Map<InventoryDbKeyFields, List<ExtendedMovementdataInventory>> movementDataInventoriesMap = new LinkedHashMap<>();

        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext2021, extendedMovementdataInventories.get(0)),
                                       List.of(extendedMovementdataInventories.get(0)));
        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext2021, extendedMovementdataInventories.get(1)),
                                       List.of(extendedMovementdataInventories.get(1), extendedMovementdataInventories.get(2)));
        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext2021, extendedMovementdataInventories.get(3)),
                                       List.of(extendedMovementdataInventories.get(3), extendedMovementdataInventories.get(4)));
        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext2020, extendedMovementdataInventories.get(5)),
                                       List.of(extendedMovementdataInventories.get(5), extendedMovementdataInventories.get(6)));
        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext2020, extendedMovementdataInventories.get(7)),
                                       List.of(extendedMovementdataInventories.get(7)));

        BulkWriteResult insertResults = movementDataInventoryRepository.bulkUpsert(movementDataInventoriesMap).block();

        assertSuccessfulInsert(insertResults);

        insertResults = movementDataInventoryRepository.bulkUpsert(movementDataInventoriesMap).block();

        assertSuccessfulUpdate(insertResults);

        List<MovementDataInventory> movementDataInventories = mongoHelperService.findAllMovementDataInventories();
        assertThat(movementDataInventories).hasSize(EXPECTED_INITIAL_SIZE);
        assertThat(movementDataInventories).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMovementDataInventories);

        String expectedLog = ProcessingErrorMessageConstants.DUPLICATE_INVENTORY_NUMBER_ERROR.replace("{}", TEST_INVENTORY_NUMBER_2);
        List<ILoggingEvent> errorList = memoryAppender.search(expectedLog);
        assertThat(errorList).isEmpty();

        String expectedTraceLog = LoggingUtil.MOVEMENT_DATA_INVENTORY_REPOSITORY_BULK_UPSERT_LOG.replace("{}ms", "");
        List<ILoggingEvent> logMessage = memoryAppenderTraceLevel.search(expectedTraceLog, Level.TRACE);
        assertThat(logMessage).hasSize(2);
    }

    @Test
    @DisplayName("Should log error message when duplicate inventory numbers exist in the same mongodb document")
    void should_log_error_message_when_duplicate_inventory_numbers_exist_in_same_document() {
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/wiremock/master-data-context-2021.json", MasterdataContext.class);
        List<ExtendedMovementdataInventory> extendedMovementdataInventories =
                TestDataLoader.loadList("json/collections/repository/extendedMovementdataInventoriesDuplicateInventoryNumber.json", new TypeReference<>() {});

        Map<InventoryDbKeyFields, List<ExtendedMovementdataInventory>> movementDataInventoriesMap = new LinkedHashMap<>();
        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext, extendedMovementdataInventories.get(0)),
                                       List.of(extendedMovementdataInventories.get(0)));
        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext, extendedMovementdataInventories.get(1)),
                                       List.of(extendedMovementdataInventories.get(1), extendedMovementdataInventories.get(2)));
        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext, extendedMovementdataInventories.get(3)),
                                       List.of(extendedMovementdataInventories.get(3), extendedMovementdataInventories.get(4)));
        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext, extendedMovementdataInventories.get(5)),
                                       List.of(extendedMovementdataInventories.get(5), extendedMovementdataInventories.get(6)));
        movementDataInventoriesMap.put(inventoryDbKeyFieldsMapper.mapToDbModel(masterdataContext, extendedMovementdataInventories.get(7)),
                                       List.of(extendedMovementdataInventories.get(7)));

        movementDataInventoryRepository.bulkUpsert(movementDataInventoriesMap).block();

        String expectedLog = ProcessingErrorMessageConstants.DUPLICATE_INVENTORY_NUMBER_ERROR.replace("{}", TEST_INVENTORY_NUMBER_2);
        List<ILoggingEvent> errorList = memoryAppender.search(expectedLog, Level.ERROR);
        assertThat(errorList).hasSize(1);
    }

    // TODO implement tests for retry and circuitbreaker and feature #242 has been merged

    private static void assertSuccessfulInsert(BulkWriteResult insertResults) {
        assertThat(insertResults).isNotNull();
        assertThat(insertResults.wasAcknowledged()).isTrue();
        assertThat(insertResults.getInsertedCount()).isZero();
        assertThat(insertResults.getDeletedCount()).isZero();
        assertThat(insertResults.getMatchedCount()).isZero();
        assertThat(insertResults.getModifiedCount()).isZero();
        assertThat(insertResults.getUpserts()).hasSize(EXPECTED_INITIAL_SIZE);
    }

    private static void assertSuccessfulUpdate(BulkWriteResult insertResults) {
        assertThat(insertResults).isNotNull();
        assertThat(insertResults.wasAcknowledged()).isTrue();
        assertThat(insertResults.getInsertedCount()).isZero();
        assertThat(insertResults.getDeletedCount()).isZero();
        assertThat(insertResults.getMatchedCount()).isEqualTo(EXPECTED_INITIAL_SIZE);
        assertThat(insertResults.getModifiedCount()).isZero();
        assertThat(insertResults.getUpserts()).isEmpty();
    }

}