package de.datev.refsys.aggregation.processing.boundry.controller;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import de.datev.refsys.aggregation.document.model.CustomColumnStructureContent;
import de.datev.refsys.aggregation.document.model.CustomReportStructureContent;
import de.datev.refsys.aggregation.document.model.MasterData;
import de.datev.refsys.aggregation.document.model.MasterDataAccount;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataInventory;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.processing.api.model.Problem;
import de.datev.refsys.aggregation.processing.configuration.TestApplicationInitializer;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import de.datev.refsys.aggregation.processing.configuration.WireMockTestConfiguration;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.util.ClearDatabaseAnCreateIndexesBeforeEachTest;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.MongoHelperService;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.UPDATE_SCHEMA_RESPONSE_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.UPDATE_SCHEMA_START_LOG;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.loadResourceAsString;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;

@ImportAutoConfiguration(TestMongoClientConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ContextConfiguration(initializers = TestApplicationInitializer.class, classes = { WireMockTestConfiguration.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ClearDatabaseAnCreateIndexesBeforeEachTest
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles(TEST_PROFILE)
@TestPropertySource(properties = {"ref-sys.update-schema.schema-version=4"})
class UpdateSchemaControllerIT {
    private static final String MASTER_DATA_CONTEXT_PATH =
            "/datev/api/v1/accounting-dataservices/consultants/" + TEST_CONSULTANT + "/clients/" + TEST_CLIENT + "/masterdata-context";
    private static final String CUSTOM_REPORTS_PATH =
            "/datev/api/v1/accounting-dataservices/consultants/" + TEST_CONSULTANT + "/clients/" + TEST_CLIENT + "/custom-report-structures";
    private static final String CUSTOM_COLUMNS_PATH =
            "/datev/api/v1/accounting-dataservices/consultants/" + TEST_CONSULTANT + "/clients/" + TEST_CLIENT + "/custom-column-structures";

    @Autowired
    private ApplicationContext context;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private MongoHelperService mongoHelperService;

    @Value("${ref-sys.update-schema.schema-version}")
    private Integer appSchemaVersion;

    private WebTestClient webTestClient;
    private MasterData expectedMasterData;
    private List<MasterDataAccount> expectedMasterDataAccounts;
    private List<MovementDataDay> expectedMovementDataDays;
    private List<MovementDataMonth> expectedMovementDataMonths;
    private List<MovementDataPersonGroupDay> expectedMovementDataPersonGroupDays;
    private List<MovementDataPersonGroupMonth> expectedMovementDataPersonGroupMonths;
    private List<MovementDataInventory> expectedMovementDataInventories;

    private MemoryAppender updateSchemaControllerMemoryAppender;
    private MemoryAppender loggingUtilMemoryAppender;

    @BeforeEach
    void setUp() {
        updateSchemaControllerMemoryAppender = setupMemoryAppender(updateSchemaControllerMemoryAppender, UpdateSchemaController.class, Level.INFO);
        loggingUtilMemoryAppender = setupMemoryAppender(loggingUtilMemoryAppender, LoggingUtil.class, Level.INFO);
        wireMockServer.resetMappings();
        webTestClient = WebTestClient.bindToApplicationContext(context).configureClient().baseUrl("/api/v1").build();
    }

    @Test
    @Order(1)
    @DisplayName("Updates schemaVersion, forceReftab, industryNo, customStructureInfos and returns 201 when data is present")
    void should_return_201_and_update_schema_version_when_data_exists() {
        // prepare
        insertCollectionsAndSetDataValues();
        Map<String, StringValuePattern> commonQueryParams = new HashMap<>();
        commonQueryParams.put("fiscal-year", equalTo(TEST_FISCAL_YEAR_2021_START.toString()));
        commonQueryParams.put("base-version", equalTo("4"));
        commonQueryParams.put("delta-version", equalTo("2"));
        ResponseDefinitionBuilder commonResponse = aResponse().withStatus(HttpStatus.OK.value())
                                                              .withTransformers("response-template")
                                                              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        MappingBuilder masterDataContextMapping = WireMock.get(urlPathMatching(MASTER_DATA_CONTEXT_PATH)).withQueryParams(commonQueryParams);
        ResponseDefinitionBuilder masterDataContextResponse =
                commonResponse.withBody(loadResourceAsString("json/acds-responses/wiremock/master-data-context-2021.json"));
        wireMockServer.stubFor(masterDataContextMapping.willReturn(masterDataContextResponse));
        StateDoc initialStateDoc = TestDataLoader.loadDBElement("json/collections/update-schema/stateDoc.json", StateDoc.class);
        Map<String, StringValuePattern> customStructuresQueryParams = new HashMap<>(commonQueryParams);
        customStructuresQueryParams.put("read-organisation-custom-data", equalTo("true"));
        MappingBuilder customReportsMapping = WireMock.get(urlPathMatching(CUSTOM_REPORTS_PATH)).withQueryParams(customStructuresQueryParams);
        ResponseDefinitionBuilder customReportsResponse =
                commonResponse.withBody(loadResourceAsString("json/acds-responses/wiremock/custom-report-structures.json"));
        wireMockServer.stubFor(customReportsMapping.willReturn(customReportsResponse));
        MappingBuilder customColumnMapping = WireMock.get(urlPathMatching(CUSTOM_COLUMNS_PATH)).withQueryParams(customStructuresQueryParams);
        ResponseDefinitionBuilder customColumnResponse =
                commonResponse.withBody(loadResourceAsString("json/acds-responses/wiremock/custom-column-structures.json"));
        wireMockServer.stubFor(customColumnMapping.willReturn(customColumnResponse));
        mongoHelperService.insertOneStateDoc(initialStateDoc);
        // execute
        OffsetDateTime timeStampBeforeSchemaUpdate = OffsetDateTime.now();
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/update-schema")
                                                         .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                         .queryParam("base-version", 4L)
                                                         .queryParam("delta-version", 2L)
                                                         .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange()
                     .expectStatus().isCreated();
        // assert
        Awaitility.await().timeout(10L, TimeUnit.SECONDS).untilAsserted(() -> {
            //---MasterData assertion---
            List<MasterData> masterDataListResult = mongoHelperService.findAllMasterData();
            assertThat(masterDataListResult).hasSize(1);
            MasterData masterDataResult = masterDataListResult.get(0);
            assertThat(masterDataResult).usingRecursiveComparison().isEqualTo(expectedMasterData);
            //---MasterDataAccounts assertion---
            List<MasterDataAccount> masterDataAccountListResult = mongoHelperService.findAllMasterDataAccounts();
            assertThat(masterDataAccountListResult).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMasterDataAccounts);
            //---MovementDataInventory assertions---
            List<MovementDataInventory> movementDataInventories = mongoHelperService.findAllMovementDataInventories();
            assertThat(movementDataInventories).hasSize(5);
            assertThat(movementDataInventories).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMovementDataInventories);
            //---MovementDataDay assertion---
            List<MovementDataDay> movementDataDays = mongoHelperService.findAllMovementDataDays();
            assertThat(movementDataDays).hasSize(28);
            assertThat(movementDataDays).usingRecursiveFieldByFieldElementComparator().hasSameElementsAs(expectedMovementDataDays);
            //---MovementDataMonth assertion---
            List<MovementDataMonth> movementDataMonth = mongoHelperService.findAllMovementDataMonths();
            assertThat(movementDataMonth).hasSize(28);
            assertThat(movementDataMonth).usingRecursiveFieldByFieldElementComparator().hasSameElementsAs(expectedMovementDataMonths);
            //---MovementDataPersonGroupDay assertion---
            List<MovementDataPersonGroupDay> movementDataPersonGroupDays = mongoHelperService.findAllMovementDataPersonGroupDays();
            assertThat(movementDataPersonGroupDays).hasSize(14);
            assertThat(movementDataPersonGroupDays).usingRecursiveFieldByFieldElementComparator()
                                                   .hasSameElementsAs(expectedMovementDataPersonGroupDays);
            //---MovementDataPersonGroupMonth assertion---
            List<MovementDataPersonGroupMonth> movementDataPersonGroupMonth = mongoHelperService.findAllMovementDataPersonGroupMonths();
            assertThat(movementDataPersonGroupMonth).hasSize(14);
            assertThat(movementDataPersonGroupMonth).usingRecursiveFieldByFieldElementComparator()
                                                    .hasSameElementsAs(expectedMovementDataPersonGroupMonths);
            //---CustomColumnStructureContents assertion---
            List<CustomColumnStructureContent> customColumnStructureContents = mongoHelperService.findAllCustomColumnStructureContents();
            List<CustomColumnStructureContent> expectedCustomColumnStructureContents =
                    TestDataLoader.loadMongoDBList("json/collections/update-schema/customColumnStructureContents.json",
                                                   CustomColumnStructureContent.class);
            assertThat(customColumnStructureContents).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedCustomColumnStructureContents);
            //---CustomReportStructureContents assertion---
            List<CustomReportStructureContent> customReportStructureContents = mongoHelperService.findAllCustomReportStructureContents();
            List<CustomReportStructureContent> expectedCustomReportStructureContents =
                    TestDataLoader.loadMongoDBList("json/collections/update-schema/customReportStructureContents.json",
                                                   CustomReportStructureContent.class);
            assertThat(customReportStructureContents).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedCustomReportStructureContents);
            //---StateDoc assertions---
            List<StateDoc> stateDocsAfterUpdate = mongoHelperService.findAllStateDocs();
            assertThat(stateDocsAfterUpdate).hasSize(1);
            StateDoc stateDocAfterUpdate = stateDocsAfterUpdate.get(0);
            RecursiveComparisonConfiguration comparisonConfiguration =
                    RecursiveComparisonConfiguration.builder()
                                                    .withIgnoredFields("stateTimestamp", "createdTimestamp", "schemaVersion",
                                                                       "forceReftabCurrentYear")
                                                    .build();
            assertThat(stateDocAfterUpdate).usingRecursiveComparison(comparisonConfiguration).isEqualTo(initialStateDoc);
            assertThat(stateDocAfterUpdate.getStateTimestamp()).isBefore(OffsetDateTime.now()).isAfter(timeStampBeforeSchemaUpdate);
            assertThat(stateDocAfterUpdate.getSchemaVersion()).isEqualTo(appSchemaVersion);
            assertThat(stateDocAfterUpdate.getForceReftabCurrentYear()).isEqualTo(masterDataResult.getContext().getForceReftabCurrentYear());


        });
        assertThat(updateSchemaControllerMemoryAppender.search(UPDATE_SCHEMA_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(UPDATE_SCHEMA_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).hasSize(1);
    }

    @Test
    @Order(2)
    @DisplayName("Returns 204 when no data exists")
    void should_return_204_and_not_create_state_doc_when_no_data_exists() {
        // execute
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/update-schema")
                                                         .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                         .queryParam("base-version", 4L)
                                                         .queryParam("delta-version", 2L)
                                                         .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange()
                     .expectStatus().isNoContent();
        // assert
        //---MasterData assertion---
        List<MasterData> masterDataListResult = mongoHelperService.findAllMasterData();
        assertThat(masterDataListResult).isEmpty();
        //---MasterDataAccounts assertion---
        List<MasterDataAccount> masterDataAccountListResult = mongoHelperService.findAllMasterDataAccounts();
        assertThat(masterDataAccountListResult).isEmpty();
        //---MovementDataInventory assertions---
        List<MovementDataInventory> movementDataInventories = mongoHelperService.findAllMovementDataInventories();
        assertThat(movementDataInventories).isEmpty();
        //---MovementDataDay assertion---
        List<MovementDataDay> movementDataDays = mongoHelperService.findAllMovementDataDays();
        assertThat(movementDataDays).isEmpty();
        //---MovementDataMonth assertion---
        List<MovementDataMonth> movementDataMonth = mongoHelperService.findAllMovementDataMonths();
        assertThat(movementDataMonth).isEmpty();
        //---MovementDataPersonGroupDay assertion---
        List<MovementDataPersonGroupDay> movementDataPersonGroupDays = mongoHelperService.findAllMovementDataPersonGroupDays();
        assertThat(movementDataPersonGroupDays).isEmpty();
        //---MovementDataPersonGroupMonth assertion---
        List<MovementDataPersonGroupMonth> movementDataPersonGroupMonth = mongoHelperService.findAllMovementDataPersonGroupMonths();
        assertThat(movementDataPersonGroupMonth).isEmpty();
        //---StateDoc assertions---
        List<StateDoc> stateDocsAfterUpdate = mongoHelperService.findAllStateDocs();
        assertThat(stateDocsAfterUpdate).isEmpty();
        assertThat(updateSchemaControllerMemoryAppender.search(UPDATE_SCHEMA_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(UPDATE_SCHEMA_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).hasSize(1);
    }

    @Test
    @Order(3)
    @DisplayName("Returns 409 when StateDocState is not in DONE state")
    void should_return_409_when_state_doc_state_is_not_in_done_state() {
        // prepare
        insertCollectionsAndSetDataValues();
        StateDoc initialStateDoc = TestDataLoader.loadDBElement("json/collections/update-schema/stateDocBad.json", StateDoc.class);
        mongoHelperService.insertOneStateDoc(initialStateDoc);
        // execute
        Problem problemResponse = webTestClient.post()
                                            .uri(uriBuilder -> uriBuilder.path(
                                                                                 "/aggregation-processing/consultants/{consultant}/clients/{client}/update-schema")
                                                                         .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                                         .queryParam("base-version", 4L)
                                                                         .queryParam("delta-version", 2L)
                                                                         .build(TEST_CONSULTANT, TEST_CLIENT))
                                            .exchange()
                                            .expectStatus()
                                            .isEqualTo(HttpStatus.CONFLICT)
                                            .expectBody(Problem.class)
                                            .returnResult()
                                            .getResponseBody();
        // assert
        assertThat(problemResponse).isNotNull();
        assertThat(problemResponse.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemResponse.getType()).isEqualTo(ProcessingServiceConstants.STATE_DOC_STATE_NOT_IN_DONE);
        assertThat(problemResponse.getTitle()).isEqualTo(HttpStatus.CONFLICT.getReasonPhrase());
        assertThat(problemResponse.getDetail()).isEqualTo(ProcessingErrorMessageConstants.STATE_DOC_NOT_IN_DONE_STATE);
        //---MasterData assertion---
        List<MasterData> masterDataListResult = mongoHelperService.findAllMasterData();
        assertThat(masterDataListResult).hasSize(1);
        MasterData masterDataResult = masterDataListResult.get(0);
        assertThat(masterDataResult).usingRecursiveComparison().isEqualTo(expectedMasterData);
        //---MasterDataAccounts assertion---
        List<MasterDataAccount> masterDataAccountListResult = mongoHelperService.findAllMasterDataAccounts();
        assertThat(masterDataAccountListResult).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMasterDataAccounts);
        //---MovementDataInventory assertions---
        List<MovementDataInventory> movementDataInventories = mongoHelperService.findAllMovementDataInventories();
        assertThat(movementDataInventories).hasSize(5);
        assertThat(movementDataInventories).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMovementDataInventories);
        //---MovementDataDay assertion---
        List<MovementDataDay> movementDataDays = mongoHelperService.findAllMovementDataDays();
        assertThat(movementDataDays).hasSize(28);
        assertThat(movementDataDays).usingRecursiveFieldByFieldElementComparator().hasSameElementsAs(expectedMovementDataDays);
        //---MovementDataMonth assertion---
        List<MovementDataMonth> movementDataMonth = mongoHelperService.findAllMovementDataMonths();
        assertThat(movementDataMonth).hasSize(28);
        assertThat(movementDataMonth).usingRecursiveFieldByFieldElementComparator().hasSameElementsAs(expectedMovementDataMonths);
        //---MovementDataPersonGroupDay assertion---
        List<MovementDataPersonGroupDay> movementDataPersonGroupDays = mongoHelperService.findAllMovementDataPersonGroupDays();
        assertThat(movementDataPersonGroupDays).hasSize(14);
        assertThat(movementDataPersonGroupDays).usingRecursiveFieldByFieldElementComparator()
                                               .hasSameElementsAs(expectedMovementDataPersonGroupDays);
        //---MovementDataPersonGroupMonth assertion---
        List<MovementDataPersonGroupMonth> movementDataPersonGroupMonth = mongoHelperService.findAllMovementDataPersonGroupMonths();
        assertThat(movementDataPersonGroupMonth).hasSize(14);
        assertThat(movementDataPersonGroupMonth).usingRecursiveFieldByFieldElementComparator()
                                                .hasSameElementsAs(expectedMovementDataPersonGroupMonths);
        //---StateDoc assertions---
        List<StateDoc> stateDocsAfterUpdate = mongoHelperService.findAllStateDocs();
        assertThat(stateDocsAfterUpdate).hasSize(1);
        StateDoc stateDocAfterUpdate = stateDocsAfterUpdate.get(0);
        assertThat(stateDocAfterUpdate).usingRecursiveComparison().isEqualTo(initialStateDoc);
        assertThat(updateSchemaControllerMemoryAppender.search(UPDATE_SCHEMA_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(UPDATE_SCHEMA_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).isEmpty();
    }

    private void insertCollectionsAndSetDataValues() {
        MasterData loadedMasterData = TestDataLoader.loadDBElement("json/collections/update-schema/masterData.json", MasterData.class);
        mongoHelperService.insertOneMasterData(loadedMasterData);
        List<MasterDataAccount> loadedMasterDataAccounts =
                TestDataLoader.loadMongoDBList("json/collections/update-schema/masterDataAccounts.json", MasterDataAccount.class);
        mongoHelperService.insertManyMasterDataAccounts(loadedMasterDataAccounts);
        List<MovementDataDay> loadedMovementDataDays =
                TestDataLoader.loadMongoDBList("json/collections/update-schema/movementDataDays.json", MovementDataDay.class);
        mongoHelperService.insertManyMovementDataDays(loadedMovementDataDays);
        List<MovementDataMonth> loadedMovementDataMonths =
                TestDataLoader.loadMongoDBList("json/collections/update-schema/movementDataMonths.json", MovementDataMonth.class);
        mongoHelperService.insertManyMovementDataMonths(loadedMovementDataMonths);
        List<MovementDataPersonGroupDay> loadedMovementDataPersonGroupDays =
                TestDataLoader.loadMongoDBList("json/collections/update-schema/movementDataPersonGroupDays.json", MovementDataPersonGroupDay.class);
        mongoHelperService.insertManyMovementDataPersonGroupDays(loadedMovementDataPersonGroupDays);
        List<MovementDataPersonGroupMonth> loadedMovementDataPersonGroupMonths =
                TestDataLoader.loadMongoDBList("json/collections/update-schema/movementDataPersonGroupMonths.json",
                                               MovementDataPersonGroupMonth.class);
        mongoHelperService.insertManyMovementDataPersonGroupMonths(loadedMovementDataPersonGroupMonths);
        List<MovementDataInventory> loadedMovementDataInventories =
                TestDataLoader.loadMongoDBList("json/collections/update-schema/movementDataInventories.json", MovementDataInventory.class);
        mongoHelperService.insertManyMovementDataInventories(loadedMovementDataInventories);
        this.expectedMasterData = loadedMasterData;
        this.expectedMasterDataAccounts = loadedMasterDataAccounts;
        this.expectedMovementDataDays = loadedMovementDataDays;
        this.expectedMovementDataMonths = loadedMovementDataMonths;
        this.expectedMovementDataPersonGroupDays = loadedMovementDataPersonGroupDays;
        this.expectedMovementDataPersonGroupMonths = loadedMovementDataPersonGroupMonths;
        this.expectedMovementDataInventories = loadedMovementDataInventories;
    }

}
