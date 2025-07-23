package de.datev.refsys.aggregation.processing.boundry.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.tomakehurst.wiremock.WireMockServer;
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
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.boundry.event.ChangeEventProducer;
import de.datev.refsys.aggregation.processing.boundry.event.ChangeEventProducerMock;
import de.datev.refsys.aggregation.processing.config.filter.LoggingContextFilter;
import de.datev.refsys.aggregation.processing.configuration.TestApplicationInitializer;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import de.datev.refsys.aggregation.processing.configuration.WireMockTestConfiguration;
import de.datev.refsys.aggregation.processing.service.ImportExecutionServiceImpl;
import de.datev.refsys.aggregation.processing.service.ImportServiceImpl;
import de.datev.refsys.aggregation.processing.util.ClearDatabaseAnCreateIndexesBeforeEachTest;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.MongoHelperService;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static de.datev.refsys.aggregation.processing.util.LoggingUtil.ACDS_GET_ACCOUNT_CAPTIONS_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.ACDS_GET_ACCOUNT_PURPOSE_MAPPINGS_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.ACDS_GET_COLLECTIVE_ACCOUNTS_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.ACDS_GET_CUSTOM_COLUMN_STRUCTURES_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.ACDS_GET_CUSTOM_REPORT_STRUCTURES_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.ACDS_GET_MASTERDATA_CONTEXT_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.ACDS_GET_MASTERDATA_INVENTORIES_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.ACDS_GET_SHAREHOLDERS_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.ACDS_GET_TRANSLATIONS_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.FULL_IMPORT_STARTED_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.FULL_IMPORT_SUCCESS_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.INITIAL_LOAD_RESPONSE_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.INITIAL_LOAD_START_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.MOVEMENT_DATA_DAYS_BATCH_START_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.MOVEMENT_DATA_DAYS_BATCH_SUCCESS_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.MOVEMENT_DATA_DAY_REPOSITORY_BULK_UPSERT_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.MOVEMENT_DATA_INVENTORIES_BATCH_START_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.MOVEMENT_DATA_INVENTORIES_BATCH_SUCCESS_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.MOVEMENT_DATA_INVENTORY_REPOSITORY_BULK_UPSERT_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.MOVEMENT_DATA_MONTHS_BATCH_START_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.MOVEMENT_DATA_MONTHS_BATCH_SUCCESS_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.MOVEMENT_DATA_MONTH_REPOSITORY_BULK_INSERT_LOG;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2020_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_END;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.WireMockServerSettings;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static de.datev.refsys.aggregation.processing.util.TestUtil.stubACDSMultipleYearsResponses;
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
class InitialLoadControllerIT {
    private static final String TARGET = "{}ms";
    private static final String REPLACEMENT = "";
    private static final String BASE_URL = "/datev/api/v1/accounting-dataservices/";
    private static final Map<String, WireMockServerSettings> wireMockServerSettingsMap = new HashMap<>();
    private static Map<String, String> accountSumDaysQueryParams;
    private static Map<String, String> collectiveQueryParams;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private MongoHelperService dBHelperService;

    private WebTestClient webTestClient;
    private MemoryAppender initialLoadControllerMemoryAppender;
    private MemoryAppender loggingUtilMemoryAppender;
    private MemoryAppender changeEventProducerMemoryAppender;
    private MemoryAppender importServiceMemoryAppender;
    private MemoryAppender importExecutionServiceMemoryAppender;

    @BeforeEach
    void setUp() {
        initialLoadControllerMemoryAppender = setupMemoryAppender(initialLoadControllerMemoryAppender, InitialLoadController.class, Level.INFO);
        loggingUtilMemoryAppender = setupMemoryAppender(loggingUtilMemoryAppender, LoggingUtil.class, Level.TRACE);
        changeEventProducerMemoryAppender = setupMemoryAppender(changeEventProducerMemoryAppender, ChangeEventProducerMock.class, Level.DEBUG);
        importServiceMemoryAppender = setupMemoryAppender(importServiceMemoryAppender, ImportServiceImpl.class, Level.DEBUG);
        importExecutionServiceMemoryAppender = setupMemoryAppender(importExecutionServiceMemoryAppender, ImportExecutionServiceImpl.class, Level.INFO);
        wireMockServer.resetMappings();
        webTestClient = WebTestClient.bindToApplicationContext(context).configureClient().baseUrl("/api/v1").build();
    }

    @BeforeAll
    static void beforeAll() {
        Map<String, String> masterdataContextQueryParams =
                Map.of("base-version", "4", "delta-version", "2", "near-time-data", "true");
        accountSumDaysQueryParams = Map.of("base-version", "4", "delta-version", "2", "near-time-data", "true");
        collectiveQueryParams =
                Map.of("base-version", "4", "delta-version", "2", "near-time-data", "true", "account-system", "3",
                       "industry-id", "0",
                       "account-length", "4", "use-consultant-accounting-functions", "false", "use-client-accounting-functions", "true",
                       "use-skr-following-year", "false");
        wireMockServerSettingsMap.put("master-data-context-2020",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/masterdata-context")
                                                            .queryParams(masterdataContextQueryParams)
                                                            .fiscalYears(List.of(TEST_FISCAL_YEAR_2020_START))
                                                            .httpStatus(HttpStatus.OK.value())
                                                            .path("json/acds-responses/wiremock/master-data-context-2020.json")
                                                            .mediaType(MediaType.APPLICATION_JSON_VALUE).build());
        wireMockServerSettingsMap.put("master-data-context-2021",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/masterdata-context")
                                                            .queryParams(masterdataContextQueryParams)
                                                            .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                            .httpStatus(HttpStatus.OK.value())
                                                            .path("json/acds-responses/wiremock/master-data-context-2021.json")
                                                            .mediaType(MediaType.APPLICATION_JSON_VALUE).build());
        wireMockServerSettingsMap.put("account-caption",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/account-captions").queryParams(
                                                                    Map.of("base-version", "4", "delta-version",
                                                                           "2", "near-time-data", "true",
                                                                           "account-system", "3",
                                                                           "industry-id", "0",
                                                                           "use-organisation-account-caption", "false", "culture-codes", "de-DE,"
                                                                                   + "en-GB")).fiscalYears(
                                                                    List.of(TEST_FISCAL_YEAR_2020_START, TEST_FISCAL_YEAR_2021_START)).httpStatus(HttpStatus.OK.value())
                                                            .path("json/acds-responses/wiremock/account-captions.ndjson")
                                                            .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("translation",
                                      WireMockServerSettings.builder().url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                   + TEST_CLIENT + "/translation").queryParams(
                                                                    Map.of("base-version", "4", "delta-version", "2",
                                                                           "use-previous-year-account" + "-translation", "false",
                                                                           "use-alternative" + "-account-translation", "false")).fiscalYears(
                                                                    List.of(TEST_FISCAL_YEAR_2020_START,
                                                                            TEST_FISCAL_YEAR_2021_START)).httpStatus(HttpStatus.OK.value())
                                                            .path("json/acds-responses/wiremock/translation.json")
                                                            .mediaType(MediaType.APPLICATION_JSON_VALUE).build());
        wireMockServerSettingsMap.put("account-purpose-mappings",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/account-purpose-mappings")
                                                            .queryParams(Map.of("base-version", "4", "delta-version", "2",
                                                                                "near-time-data", "true",
                                                                                "account-system", "3", "industry-id", "0",
                                                                                "use-refsys", "false")).fiscalYears(
                                                                    List.of(TEST_FISCAL_YEAR_2020_START, TEST_FISCAL_YEAR_2021_START)).httpStatus(HttpStatus.OK.value())
                                                            .path("json/acds-responses/wiremock/account-purpose-mappings.ndjson")
                                                            .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("shareholder-2021",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/shareholder").queryParams(
                                                                    Map.of("base-version", "4",
                                                                           "delta-version", "2",
                                                                           "near-time-data", "true"))
                                                            .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                            .httpStatus(HttpStatus.OK.value())
                                                            .path("json/acds-responses/wiremock/shareholder-2021.json")
                                                            .mediaType(MediaType.APPLICATION_JSON_VALUE).build());
        wireMockServerSettingsMap.put("shareholder-2020",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/shareholder").queryParams(
                                                                    Map.of("base-version", "4", "delta-version",
                                                                           "2", "near-time-data", "true"))
                                                            .fiscalYears(List.of(TEST_FISCAL_YEAR_2020_START))
                                                            .httpStatus(HttpStatus.OK.value())
                                                            .path("json/acds-responses/wiremock/shareholder-2020.json")
                                                            .mediaType(MediaType.APPLICATION_JSON_VALUE).build());
        wireMockServerSettingsMap.put("master-data-inventories", WireMockServerSettings.builder()
                                                                                       .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                    + TEST_CLIENT + "/masterdata-inventories")
                                                                                       .queryParams(Map.of("base-version", "4", "delta-version", "2",
                                                                                                           "near-time-data", "true"))
                                                                                       .fiscalYears(List.of(TEST_FISCAL_YEAR_2020_START,
                                                                                                            TEST_FISCAL_YEAR_2021_START))
                                                                                       .httpStatus(HttpStatus.OK.value())
                                                                                       .path("json/acds-responses/wiremock/master-data-inventories.json")
                                                                                       .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                                                                       .build());
        wireMockServerSettingsMap.put("movement-data-inventories-2020", WireMockServerSettings.builder()
                                                                                         .url(BASE_URL + "consultants/" + TEST_CONSULTANT
                                                                                                      + "/clients/" + TEST_CLIENT
                                                                                                      + "/movementdata-inventories")
                                                                                         .queryParams(
                                                                                                 Map.of("base-version", "4", "delta-version", "2",
                                                                                                        "near-time-data", "true"))
                                                                                         .fiscalYears(List.of(TEST_FISCAL_YEAR_2020_START))
                                                                                         .httpStatus(HttpStatus.OK.value())
                                                                                         .path("json/acds-responses/wiremock/movement-data-inventories-2020.json")
                                                                                         .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                                                                         .build());
        wireMockServerSettingsMap.put("movement-data-inventories-2021", WireMockServerSettings.builder()
                                                                                         .url(BASE_URL + "consultants/" + TEST_CONSULTANT
                                                                                                      + "/clients/" + TEST_CLIENT
                                                                                                      + "/movementdata-inventories")
                                                                                         .queryParams(
                                                                                                 Map.of("base-version", "4", "delta-version", "2",
                                                                                                        "near-time-data", "true"))
                                                                                         .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                                                         .httpStatus(HttpStatus.OK.value())
                                                                                         .path("json/acds-responses/wiremock/movement-data-inventories-2021.json")
                                                                                         .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                                                                         .build());
        wireMockServerSettingsMap.put("custom-report-structures", WireMockServerSettings.builder()
                                                                                        .url(BASE_URL + "consultants/" + TEST_CONSULTANT
                                                                                                     + "/clients/" + TEST_CLIENT
                                                                                                     + "/custom-report-structures")
                                                                                        .queryParams(
                                                                                                Map.of("base-version", "4", "delta-version", "2",
                                                                                                       "read-organisation-custom-data", "true"))
                                                                                        .fiscalYears(List.of(TEST_FISCAL_YEAR_2020_START,
                                                                                                             TEST_FISCAL_YEAR_2021_START))
                                                                                        .httpStatus(HttpStatus.OK.value())
                                                                                        .path("json/acds-responses/wiremock/custom-report-structures.json")
                                                                                        .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                                                                        .build());
        wireMockServerSettingsMap.put("custom-column-structures", WireMockServerSettings.builder()
                                                                                        .url(BASE_URL + "consultants/" + TEST_CONSULTANT
                                                                                                     + "/clients/" + TEST_CLIENT
                                                                                                     + "/custom-column-structures")
                                                                                        .queryParams(
                                                                                                Map.of("base-version", "4", "delta-version", "2",
                                                                                                       "read-organisation-custom-data", "true"))
                                                                                        .fiscalYears(List.of(TEST_FISCAL_YEAR_2020_START,
                                                                                                             TEST_FISCAL_YEAR_2021_START))
                                                                                        .httpStatus(HttpStatus.OK.value())
                                                                                        .path("json/acds-responses/wiremock/custom-column-structures.json")
                                                                                        .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                                                                        .build());
    }

    @Test
    @Order(1)
    @DisplayName("Master Data and Master Data Account Integration Test")
    void should_initiate_initial_import_and_verify_master_data_and_master_data_account() {
        wireMockServerSettingsMap.put("account-sum-days-2020",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/account-sum-days")
                                                            .queryParams(accountSumDaysQueryParams)
                                                            .fiscalYears(List.of(TEST_FISCAL_YEAR_2020_START))
                                                            .httpStatus(HttpStatus.OK.value())
                                                            .path("json/acds-responses/wiremock/master-data/account-sum-days-2020.ndjson")
                                                            .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("account-sum-days-2021",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/account-sum-days")
                                                            .queryParams(accountSumDaysQueryParams)
                                                            .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                            .httpStatus(HttpStatus.OK.value())
                                                            .path("json/acds-responses/wiremock/master-data/account-sum-days-2021.ndjson")
                                                            .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("collective-accounts-2020",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/collective-accounts")
                                                            .queryParams(collectiveQueryParams)
                                                            .fiscalYears(List.of(TEST_FISCAL_YEAR_2020_START))
                                                            .httpStatus(HttpStatus.OK.value())
                                                            .path("json/acds-responses/wiremock/master-data/collective-accounts.ndjson")
                                                            .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("collective-accounts-2021",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/collective-accounts")
                                                            .queryParams(collectiveQueryParams)
                                                            .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                            .httpStatus(HttpStatus.OK.value())
                                                            .path("json/acds-responses/wiremock/master-data/collective-accounts.ndjson")
                                                            .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        stubACDSMultipleYearsResponses(wireMockServer, wireMockServerSettingsMap);

        OffsetDateTime timeStampBeforeImport = OffsetDateTime.now();
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                         .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                         .queryParam("base-version", 4L)
                                                         .queryParam("delta-version", 2L)
                                                         .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange().expectStatus().is2xxSuccessful();
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                         .queryParam("fiscal-year", TEST_FISCAL_YEAR_2020_START)
                                                         .queryParam("base-version", 4L)
                                                         .queryParam("delta-version", 2L)
                                                         .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange().expectStatus().is2xxSuccessful();

        Awaitility.await().timeout(10L, TimeUnit.SECONDS).untilAsserted(() -> {
            //---MasterData assertion---
            List<MasterData> masterDataListResult = dBHelperService.findAllMasterData();
            assertThat(masterDataListResult).hasSize(2);
            List<MasterData> expectedMasterData =
                    TestDataLoader.loadMongoDBList("json/collections/expected/masterdata-collections/masterData.json", MasterData.class);
            assertThat(masterDataListResult).usingRecursiveFieldByFieldElementComparator().hasSameElementsAs(expectedMasterData);
            //---MasterDataAccount assertions----
            List<MasterDataAccount> masterDataAccountListResult = dBHelperService.findAllMasterDataAccounts();
            assertThat(masterDataAccountListResult).hasSize(14);
            List<MasterDataAccount> expectedMasterDataAccounts =
                    TestDataLoader.loadMongoDBList("json/collections/expected/masterdata-collections/masterDataAccounts.json",
                                                   MasterDataAccount.class);
            assertThat(masterDataAccountListResult).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMasterDataAccounts);
            //---MovementDataInventory assertions---
            List<MovementDataInventory> movementDataInventories = dBHelperService.findAllMovementDataInventories();
            assertThat(movementDataInventories).hasSize(10);
            List<MovementDataInventory> expectedMovementDataInventories =
                    TestDataLoader.loadMongoDBList("json/collections/expected/masterdata-collections/movementDataInventories.json",
                                                   MovementDataInventory.class);
            assertThat(movementDataInventories).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMovementDataInventories);
            //---StateDoc assertions---
            List<StateDoc> stateDocResult = dBHelperService.findAllStateDocs();
            List<StateDoc> expectedStateDoc =
                    TestDataLoader.loadMongoDBList("json/collections/expected/masterdata-collections/stateDoc.json", StateDoc.class);
            assertThat(stateDocResult).isNotNull();
            assertThat(expectedStateDoc).usingRecursiveFieldByFieldElementComparatorIgnoringFields("stateTimestamp", "createdTimestamp")
                                        .hasSameElementsAs(stateDocResult);
            stateDocResult.forEach(
                    stateDoc -> {
                        assertThat(stateDoc.getStateTimestamp()).isBefore(OffsetDateTime.now()).isAfter(timeStampBeforeImport);
                        assertThat(stateDoc.getBaseVersion()).isEqualTo(4);
                        assertThat(stateDoc.getDeltaVersion()).isEqualTo(2);
                    });
            List<CustomColumnStructureContent> customColumnStructureContents = dBHelperService.findAllCustomColumnStructureContents();
            List<CustomColumnStructureContent> expectedCustomColumnStructureContents =
                    TestDataLoader.loadMongoDBList("json/collections/expected/movementdata-collections/customColumnStructureContents.json",
                                                   CustomColumnStructureContent.class);
            assertThat(customColumnStructureContents).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedCustomColumnStructureContents);

            List<CustomReportStructureContent> customReportStructureContents = dBHelperService.findAllCustomReportStructureContents();
            List<CustomReportStructureContent> expectedCustomReportStructureContents =
                    TestDataLoader.loadMongoDBList("json/collections/expected/movementdata-collections/customReportStructureContents.json",
                                                   CustomReportStructureContent.class);
            assertThat(customReportStructureContents).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedCustomReportStructureContents);
        });
    }

    @Test
    @Order(2)
    @DisplayName("Movement Data Integration Test")
    void should_initiate_initial_import_and_verify_movement_data() {
        wireMockServerSettingsMap.put("account-sum-days-2020", WireMockServerSettings.builder()
                                                                                     .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                  + TEST_CLIENT + "/account-sum-days")
                                                                                     .queryParams(accountSumDaysQueryParams)
                                                                                     .fiscalYears(List.of(TEST_FISCAL_YEAR_2020_START))
                                                                                     .httpStatus(HttpStatus.OK.value())
                                                                                     .path("json/acds-responses/wiremock/movement-data/account-sum-days-2020.ndjson")
                                                                                     .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("account-sum-days-2021", WireMockServerSettings.builder()
                                                                                     .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                  + TEST_CLIENT + "/account-sum-days")
                                                                                     .queryParams(accountSumDaysQueryParams)
                                                                                     .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                                                     .httpStatus(HttpStatus.OK.value())
                                                                                     .path("json/acds-responses/wiremock/movement-data/account-sum-days-2021.ndjson")
                                                                                     .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("collective-accounts-2020", WireMockServerSettings.builder()
                                                                                        .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                     + TEST_CLIENT + "/collective-accounts")
                                                                                        .queryParams(collectiveQueryParams)
                                                                                        .fiscalYears(List.of(TEST_FISCAL_YEAR_2020_START))
                                                                                        .httpStatus(HttpStatus.OK.value())
                                                                                        .path("json/acds-responses/wiremock/movement-data"
                                                                                                      + "/collective-accounts-2020.ndjson")
                                                                                        .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("collective-accounts-2021", WireMockServerSettings.builder()
                                                                                        .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                     + TEST_CLIENT + "/collective-accounts")
                                                                                        .queryParams(collectiveQueryParams)
                                                                                        .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                                                        .httpStatus(HttpStatus.OK.value())
                                                                                        .path("json/acds-responses/wiremock/movement-data"
                                                                                                      + "/collective-accounts-2021.ndjson")
                                                                                        .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        stubACDSMultipleYearsResponses(wireMockServer, wireMockServerSettingsMap);
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                         .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                         .queryParam("base-version", 4L)
                                                         .queryParam("delta-version", 2L)
                                                         .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange().expectStatus().is2xxSuccessful();
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                         .queryParam("fiscal-year", TEST_FISCAL_YEAR_2020_START)
                                                         .queryParam("base-version", 4L)
                                                         .queryParam("delta-version", 2L)
                                                         .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange().expectStatus().is2xxSuccessful();

        Awaitility.await().timeout(20L, TimeUnit.SECONDS).untilAsserted(() -> {
            //---MovementDataDay assertion---
            List<MovementDataDay> movementDataDayListResult = dBHelperService.findAllMovementDataDays();
            assertThat(movementDataDayListResult).hasSize(57);
            List<MovementDataDay> movementDataDays2021Result =
                    movementDataDayListResult.stream().filter(movementDataDay -> movementDataDay.getFiscalYear().equals(TEST_FISCAL_YEAR_2021_START))
                                             .toList();
            assertThat(movementDataDays2021Result).hasSize(28);
            List<MovementDataDay> movementDataDays2020Result =
                    movementDataDayListResult.stream().filter(movementDataDay -> movementDataDay.getFiscalYear().equals(TEST_FISCAL_YEAR_2020_START))
                                             .toList();
            assertThat(movementDataDays2020Result).hasSize(29);
            List<MovementDataDay> expectedMovementDataDay =
                    TestDataLoader.loadMongoDBList("json/collections/expected/movementdata-collections/movementDataDays.json", MovementDataDay.class);
            movementDataDayListResult.sort((l1, l2) -> Comparator.comparing(MovementDataDay::getFiscalYear)
                                                                 .thenComparing(MovementDataDay::getAccountNumber)
                                                                 .thenComparing(MovementDataDay::getAccountingReasonId)
                                                                 .compare(l1, l2));
            expectedMovementDataDay.sort((l1, l2) -> Comparator.comparing(MovementDataDay::getFiscalYear)
                                                               .thenComparing(MovementDataDay::getAccountNumber)
                                                               .thenComparing(MovementDataDay::getAccountingReasonId)
                                                               .compare(l1, l2));
            assertThat(movementDataDayListResult).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMovementDataDay);
            //---MovementDataMonth assertion---
            List<MovementDataMonth> movementDataMonthListResult = dBHelperService.findAllMovementDataMonths();
            assertThat(movementDataMonthListResult).hasSize(57);
            List<MovementDataMonth> movementDataMonthsResult2021 = movementDataMonthListResult.stream()
                                                                                              .filter(movementDataDay -> movementDataDay.getFiscalYear()
                                                                                                                                        .equals(TEST_FISCAL_YEAR_2021_START))
                                                                                              .toList();
            assertThat(movementDataMonthsResult2021).hasSize(28);
            List<MovementDataMonth> movementDataMonthsResult2020 = movementDataMonthListResult.stream()
                                                                                              .filter(movementDataDay -> movementDataDay.getFiscalYear()
                                                                                                                                        .equals(TEST_FISCAL_YEAR_2020_START))
                                                                                              .toList();
            assertThat(movementDataMonthsResult2020).hasSize(29);
            List<MovementDataMonth> expectedMovementDataMonth =
                    TestDataLoader.loadMongoDBList("json/collections/expected/movementdata-collections/movementDataMonths.json",
                                                   MovementDataMonth.class);
            assertThat(movementDataMonthListResult).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMovementDataMonth);
            //---MovementDataPersonGroupDay assertion---
            List<MovementDataPersonGroupDay> movementDataPersonGroupDaysResult = dBHelperService.findAllMovementDataPersonGroupDays();
            assertThat(movementDataPersonGroupDaysResult).hasSize(25);
            List<MovementDataPersonGroupDay> movementDataPersonGroupDaysResult2021 = movementDataPersonGroupDaysResult.stream()
                                                                                                                      .filter(movementDataPersonGroupDay -> movementDataPersonGroupDay.getFiscalYear()
                                                                                                                                                                                      .equals(TEST_FISCAL_YEAR_2021_START))
                                                                                                                      .toList();
            assertThat(movementDataPersonGroupDaysResult2021).hasSize(14);
            List<MovementDataPersonGroupDay> movementDataPersonGroupDaysResult2020 = movementDataPersonGroupDaysResult.stream()
                                                                                                                      .filter(movementDataPersonGroupDay -> movementDataPersonGroupDay.getFiscalYear()
                                                                                                                                                                                      .equals(TEST_FISCAL_YEAR_2020_START))
                                                                                                                      .toList();
            assertThat(movementDataPersonGroupDaysResult2020).hasSize(11);
            List<MovementDataPersonGroupDay> expectedMovementDataPersonGroupDay =
                    TestDataLoader.loadMongoDBList("json/collections/expected/movementdata-collections/movementDataPersonGroupDays.json",
                                                   MovementDataPersonGroupDay.class);
            assertThat(movementDataPersonGroupDaysResult).usingRecursiveComparison()
                                                         .ignoringCollectionOrder()
                                                         .isEqualTo(expectedMovementDataPersonGroupDay);
            //---MovementDataPersonGroupMonth assertion---
            List<MovementDataPersonGroupMonth> movementDataPersonGroupMonthResult = dBHelperService.findAllMovementDataPersonGroupMonths();
            assertThat(movementDataPersonGroupMonthResult).hasSize(25);
            List<MovementDataPersonGroupMonth> movementDataPersonGroupMonthResult2021 = movementDataPersonGroupMonthResult.stream()
                                                                                                                          .filter(movementDataPersonGroupDay -> movementDataPersonGroupDay.getFiscalYear()
                                                                                                                                                                                          .equals(TEST_FISCAL_YEAR_2021_START))
                                                                                                                          .toList();
            assertThat(movementDataPersonGroupMonthResult2021).hasSize(14);
            List<MovementDataPersonGroupMonth> movementDataPersonGroupMonthResult2020 = movementDataPersonGroupMonthResult.stream()
                                                                                                                          .filter(movementDataPersonGroupDay -> movementDataPersonGroupDay.getFiscalYear()
                                                                                                                                                                                          .equals(TEST_FISCAL_YEAR_2020_START))
                                                                                                                          .toList();
            assertThat(movementDataPersonGroupMonthResult2020).hasSize(11);
            List<MovementDataPersonGroupMonth> expectedMovementDataPersonGroupMonth =
                    TestDataLoader.loadMongoDBList("json/collections/expected/movementdata-collections/movementDataPersonGroupMonths.json",
                                                   MovementDataPersonGroupMonth.class);
            assertThat(movementDataPersonGroupMonthResult).usingRecursiveComparison()
                                                          .ignoringCollectionOrder()
                                                          .isEqualTo(expectedMovementDataPersonGroupMonth);
            //---MasterData IndividualPersonAccountNumbers assertion---
            List<MasterData> masterDataListResult = dBHelperService.findAllMasterData();
            List<MasterData> expectedMasterData =
                    TestDataLoader.loadMongoDBList("json/collections/expected/movementdata-collections/masterData.json", MasterData.class);
            assertThat(masterDataListResult).usingRecursiveFieldByFieldElementComparator().hasSameElementsAs(expectedMasterData);
            //---MasterDataAccounts assertion---
            List<MasterDataAccount> masterDataAccountListResult = dBHelperService.findAllMasterDataAccounts();
            List<MasterDataAccount> expectedMasterDataAccounts =
                    TestDataLoader.loadMongoDBList("json/collections/expected/movementdata-collections/masterDataAccounts.json",
                                                   MasterDataAccount.class);
            assertThat(masterDataAccountListResult).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMasterDataAccounts);
            //---MovementDataInventory assertions---
            List<MovementDataInventory> movementDataInventories = dBHelperService.findAllMovementDataInventories();
            assertThat(movementDataInventories).hasSize(10);
            List<MovementDataInventory> expectedMovementDataInventories =
                    TestDataLoader.loadMongoDBList("json/collections/expected/movementdata-collections/movementDataInventories.json",
                                                   MovementDataInventory.class);
            assertThat(movementDataInventories).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMovementDataInventories);

            List<CustomColumnStructureContent> customColumnStructureContents = dBHelperService.findAllCustomColumnStructureContents();
            List<CustomColumnStructureContent> expectedCustomColumnStructureContents =
                    TestDataLoader.loadMongoDBList("json/collections/expected/movementdata-collections/customColumnStructureContents.json",
                                                   CustomColumnStructureContent.class);
            assertThat(customColumnStructureContents).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedCustomColumnStructureContents);

            List<CustomReportStructureContent> customReportStructureContents = dBHelperService.findAllCustomReportStructureContents();
            List<CustomReportStructureContent> expectedCustomReportStructureContents =
                    TestDataLoader.loadMongoDBList("json/collections/expected/movementdata-collections/customReportStructureContents.json",
                                                   CustomReportStructureContent.class);
            assertThat(customReportStructureContents).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedCustomReportStructureContents);
            assertLogs();
        });
    }

    @Test
    @Order(3)
    @DisplayName("Test initial import when statedoc is in INIT state")
    void should_not_start_initial_loading_when_statedoc_is_in_init() {
        wireMockServerSettingsMap.put("account-sum-days-2021", WireMockServerSettings.builder()
                                                                                     .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                  + TEST_CLIENT + "/account-sum-days")
                                                                                     .queryParams(accountSumDaysQueryParams)
                                                                                     .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                                                     .httpStatus(HttpStatus.OK.value())
                                                                                     .path("json/acds-responses/wiremock/movement-data/account-sum"
                                                                                                   + "-days-2021.ndjson")
                                                                                     .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("collective-accounts-2021", WireMockServerSettings.builder()
                                                                                        .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                     + TEST_CLIENT + "/collective-accounts")
                                                                                        .queryParams(collectiveQueryParams)
                                                                                        .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                                                        .httpStatus(HttpStatus.OK.value())
                                                                                        .path("json/acds-responses/wiremock/movement-data"
                                                                                                      + "/collective-accounts-2021.ndjson")
                                                                                        .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        stubACDSMultipleYearsResponses(wireMockServer, wireMockServerSettingsMap);
        OffsetDateTime firstImportTimeStamp =
                OffsetDateTime.parse(OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")));
        StateDoc firstImportStateDoc =
                StateDoc.builder().client(TEST_CLIENT).consultant(TEST_CONSULTANT).state(StateDocState.INIT).baseVersion(0L).deltaVersion(0L)
                        .stateTimestamp(firstImportTimeStamp).yearBegin(TEST_FISCAL_YEAR_2021_START).yearEnd(TEST_FISCAL_YEAR_2021_END).build();
        dBHelperService.insertOneStateDoc(firstImportStateDoc);
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                         .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START).build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT.value());
        // wait for the data import to begin
        Awaitility.await().pollDelay(2, TimeUnit.SECONDS).until(() -> true);
        List<StateDoc> secondImportStateDocListResult = dBHelperService.findAllStateDocs();
        assertThat(secondImportStateDocListResult).hasSize(1);
        assertThat(secondImportStateDocListResult.get(0).getStateTimestamp()).isEqualTo(firstImportTimeStamp);

    }

    @Test
    @Order(4)
    @DisplayName("Test initial import when statedoc is in REINIT state")
    void should_not_start_initial_loading_when_statedoc_is_in_reinit() {
        wireMockServerSettingsMap.put("account-sum-days-2021", WireMockServerSettings.builder()
                                                                                     .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                  + TEST_CLIENT + "/account-sum-days")
                                                                                     .queryParams(accountSumDaysQueryParams)
                                                                                     .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                                                     .httpStatus(HttpStatus.OK.value())
                                                                                     .path("json/acds-responses/wiremock/movement-data/account-sum"
                                                                                                   + "-days-2021.ndjson")
                                                                                     .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("collective-accounts-2021", WireMockServerSettings.builder()
                                                                                        .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                     + TEST_CLIENT + "/collective-accounts")
                                                                                        .queryParams(collectiveQueryParams)
                                                                                        .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                                                        .httpStatus(HttpStatus.OK.value())
                                                                                        .path("json/acds-responses/wiremock/movement-data"
                                                                                                      + "/collective-accounts-2021.ndjson")
                                                                                        .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        stubACDSMultipleYearsResponses(wireMockServer, wireMockServerSettingsMap);
        OffsetDateTime firstImportTimeStamp =
                OffsetDateTime.parse(OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")));
        StateDoc firstImportStateDoc = StateDoc.builder()
                                               .client(TEST_CLIENT)
                                               .consultant(TEST_CONSULTANT)
                                               .state(StateDocState.REINIT)
                                               .baseVersion(0L)
                                               .deltaVersion(0L)
                                               .stateTimestamp(firstImportTimeStamp)
                                               .yearBegin(TEST_FISCAL_YEAR_2021_START)
                                               .yearEnd(TEST_FISCAL_YEAR_2021_END).build();
        dBHelperService.insertOneStateDoc(firstImportStateDoc);
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                         .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START).build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT.value());
        // wait for the data import to begin
        Awaitility.await().pollDelay(2, TimeUnit.SECONDS).until(() -> true);
        List<StateDoc> secondImportStateDocListResult = dBHelperService.findAllStateDocs();
        assertThat(secondImportStateDocListResult).hasSize(1);
        assertThat(secondImportStateDocListResult.get(0).getStateTimestamp()).isEqualTo(firstImportTimeStamp);

    }

    @Test
    @Order(5)
    @DisplayName("Test  initial loading when statedoc is in init but import takes longer than maximum")
    void should_start_initial_loading_when_statedoc_is_in_init_but_import_takes_longer_than_maximum() {
        wireMockServerSettingsMap.put("account-sum-days-2021", WireMockServerSettings.builder()
                                                                                     .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                  + TEST_CLIENT + "/account-sum-days")
                                                                                     .queryParams(accountSumDaysQueryParams)
                                                                                     .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                                                     .httpStatus(HttpStatus.OK.value())
                                                                                     .path("json/acds-responses/wiremock/movement-data/account-sum"
                                                                                                   + "-days-2021.ndjson")
                                                                                     .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("collective-accounts-2021", WireMockServerSettings.builder()
                                                                                        .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                     + TEST_CLIENT + "/collective-accounts")
                                                                                        .queryParams(collectiveQueryParams)
                                                                                        .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                                                        .httpStatus(HttpStatus.OK.value())
                                                                                        .path("json/acds-responses/wiremock/movement-data"
                                                                                                      + "/collective-accounts-2021.ndjson")
                                                                                        .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        stubACDSMultipleYearsResponses(wireMockServer, wireMockServerSettingsMap);
        OffsetDateTime firstImportTimeStamp =
                OffsetDateTime.parse(OffsetDateTime.now().minusSeconds(6).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")));
        StateDoc firstImportStateDoc = StateDoc.builder()
                                               .client(TEST_CLIENT)
                                               .consultant(TEST_CONSULTANT)
                                               .state(StateDocState.INIT)
                                               .baseVersion(4L)
                                               .deltaVersion(2L)
                                               .stateTimestamp(firstImportTimeStamp)
                                               .createdTimestamp(firstImportTimeStamp)
                                               .yearBegin(TEST_FISCAL_YEAR_2021_START)
                                               .yearEnd(TEST_FISCAL_YEAR_2021_END).build();
        dBHelperService.insertOneStateDoc(firstImportStateDoc);
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                         .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                         .queryParam("base-version", 4L)
                                                         .queryParam("delta-version", 2L)
                                                         .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange().expectStatus().is2xxSuccessful();

        Awaitility.await().timeout(20L, TimeUnit.SECONDS).untilAsserted(() -> {
            List<MasterData> masterDataList = dBHelperService.findAllMasterData();
            assertThat(masterDataList).isNotEmpty();
            List<StateDoc> thirdImportStateDocListResult = dBHelperService.findAllStateDocs();
            assertThat(thirdImportStateDocListResult).hasSize(1);
            assertThat(thirdImportStateDocListResult.get(0).getStateTimestamp()).isAfter(firstImportTimeStamp);
        });
    }

    @Test
    @Order(6)
    @DisplayName("Movement Data Integration Test for Person Accounts with different AdditionalParams and usual to unusual change")
    void should_initiate_initial_import_and_verify_movement_data_for_person_accounts_with_different_additional_params() {
        wireMockServerSettingsMap.put("account-sum-days-2021", WireMockServerSettings.builder()
                                                                                     .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                  + TEST_CLIENT + "/account-sum-days")
                                                                                     .queryParams(accountSumDaysQueryParams)
                                                                                     .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                                                     .httpStatus(HttpStatus.OK.value())
                                                                                     .path("json/acds-responses/wiremock/movement-data/person"
                                                                                                   + "-account-sum-days-with-different-additional"
                                                                                                   + "-params.ndjson")
                                                                                     .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("collective-accounts-2021", WireMockServerSettings.builder()
                                                                                        .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                     + TEST_CLIENT + "/collective-accounts")
                                                                                        .queryParams(collectiveQueryParams)
                                                                                        .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                                                        .httpStatus(HttpStatus.OK.value())
                                                                                        .path("json/acds-responses/wiremock/movement-data"
                                                                                                      + "/collective-accounts-2021.ndjson")
                                                                                        .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        stubACDSMultipleYearsResponses(wireMockServer, wireMockServerSettingsMap);
        webTestClient.post()
                     .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                  .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                  .queryParam("base-version", 4L)
                                                  .queryParam("delta-version", 2L)
                                                  .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange()
                     .expectStatus()
                     .is2xxSuccessful();

        Awaitility.await().timeout(20L, TimeUnit.SECONDS).untilAsserted(() -> {
            //---MovementDataDay assertion---
            List<MovementDataDay> movementDataDayListResult = dBHelperService.findAllMovementDataDays();
            assertThat(movementDataDayListResult).hasSize(3);
            List<MovementDataDay> expectedMovementDataDay =
                    TestDataLoader.loadMongoDBList("json/collections/expected/persongroup-usual-to-unusual/movementDataDays.json",
                                                   MovementDataDay.class);
            assertThat(movementDataDayListResult).usingRecursiveFieldByFieldElementComparator().hasSameElementsAs(expectedMovementDataDay);
            //---MovementDataMonth assertion---
            List<MovementDataMonth> movementDataMonthListResult = dBHelperService.findAllMovementDataMonths();
            assertThat(movementDataMonthListResult).hasSize(3);
            List<MovementDataMonth> expectedMovementDataMonth =
                    TestDataLoader.loadMongoDBList("json/collections/expected/persongroup-usual-to-unusual/movementDataMonths.json",
                                                   MovementDataMonth.class);
            assertThat(movementDataMonthListResult).usingRecursiveFieldByFieldElementComparator().hasSameElementsAs(expectedMovementDataMonth);
            //---MovementDataPersonGroupDay assertion---
            List<MovementDataPersonGroupDay> movementDataPersonGroupDaysResult = dBHelperService.findAllMovementDataPersonGroupDays();
            assertThat(movementDataPersonGroupDaysResult).hasSize(3);
            List<MovementDataPersonGroupDay> expectedMovementDataPersonGroupDay =
                    TestDataLoader.loadMongoDBList("json/collections/expected/persongroup-usual-to-unusual/movementDataPersonGroupDays.json",
                                                   MovementDataPersonGroupDay.class);
            assertThat(movementDataPersonGroupDaysResult).usingRecursiveFieldByFieldElementComparator()
                                                         .hasSameElementsAs(expectedMovementDataPersonGroupDay);
            //---MovementDataPersonGroupMonth assertion---
            List<MovementDataPersonGroupMonth> movementDataPersonGroupMonthResult = dBHelperService.findAllMovementDataPersonGroupMonths();
            assertThat(movementDataPersonGroupMonthResult).hasSize(3);
            List<MovementDataPersonGroupMonth> expectedMovementDataPersonGroupMonth =
                    TestDataLoader.loadMongoDBList("json/collections/expected/persongroup-usual-to-unusual/movementDataPersonGroupMonths.json",
                                                   MovementDataPersonGroupMonth.class);
            assertThat(movementDataPersonGroupMonthResult).usingRecursiveFieldByFieldElementComparator()
                                                          .hasSameElementsAs(expectedMovementDataPersonGroupMonth);
            //---MasterData IndividualPersonAccountNumbers assertion---
            List<MasterData> masterDataListResult = dBHelperService.findAllMasterData();
            assertThat(masterDataListResult).isNotNull();
            List<MasterData> expectedMasterData =
                    TestDataLoader.loadMongoDBList("json/collections/expected/persongroup-usual-to-unusual/masterData.json", MasterData.class);
            assertThat(masterDataListResult).usingRecursiveFieldByFieldElementComparator().hasSameElementsAs(expectedMasterData);
            //---MasterDataAccounts assertion---
            List<MasterDataAccount> masterDataAccountListResult = dBHelperService.findAllMasterDataAccounts();
            assertThat(masterDataAccountListResult).isNotNull();
            List<MasterDataAccount> expectedMasterDataAccounts =
                    TestDataLoader.loadMongoDBList("json/collections/expected/persongroup-usual-to-unusual/masterDataAccounts.json",
                                                   MasterDataAccount.class);
            assertThat(masterDataAccountListResult).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMasterDataAccounts);
            //---MovementDataInventory assertions---
            List<MovementDataInventory> movementDataInventories = dBHelperService.findAllMovementDataInventories();
            assertThat(movementDataInventories).hasSize(5);
            List<MovementDataInventory> expectedMovementDataInventories =
                    TestDataLoader.loadMongoDBList("json/collections/expected/persongroup-usual-to-unusual/movementDataInventories.json",
                                                   MovementDataInventory.class);
            assertThat(movementDataInventories).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMovementDataInventories);

            List<CustomColumnStructureContent> customColumnStructureContents = dBHelperService.findAllCustomColumnStructureContents();
            List<CustomColumnStructureContent> expectedCustomColumnStructureContents =
                    TestDataLoader.loadMongoDBList("json/collections/integration/customColumnStructureContents.json",
                                                   CustomColumnStructureContent.class);
            assertThat(customColumnStructureContents).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedCustomColumnStructureContents);

            List<CustomReportStructureContent> customReportStructureContents = dBHelperService.findAllCustomReportStructureContents();
            List<CustomReportStructureContent> expectedCustomReportStructureContents =
                    TestDataLoader.loadMongoDBList("json/collections/integration/customReportStructureContents.json",
                                                   CustomReportStructureContent.class);
            assertThat(customReportStructureContents).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedCustomReportStructureContents);
        });
    }


    @Test
    @Order(8)
    @DisplayName("Movement Data Integration Test to verify that no AccountGroupValue is saved as Null")
    void should_initiate_initial_import_and_verify_that_no_AccoutGroupValue_as_null_is_saved() {
        wireMockServerSettingsMap.put("account-sum-days-2021", WireMockServerSettings.builder()
                                                                                     .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                  + TEST_CLIENT + "/account-sum-days")
                                                                                     .queryParams(accountSumDaysQueryParams)
                                                                                     .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                                                     .httpStatus(HttpStatus.OK.value())
                                                                                     .path("json/acds-responses/wiremock/movement-data/account-sum"
                                                                                                   + "-days-2021-usual-to-unusual.ndjson")
                                                                                     .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("collective-accounts-2021", WireMockServerSettings.builder()
                                                                                        .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                                                     + TEST_CLIENT + "/collective-accounts")
                                                                                        .queryParams(collectiveQueryParams)
                                                                                        .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                                                        .httpStatus(HttpStatus.OK.value())
                                                                                        .path("json/acds-responses/wiremock/movement-data"
                                                                                                      + "/collective-accounts-2021.ndjson")
                                                                                        .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        stubACDSMultipleYearsResponses(wireMockServer, wireMockServerSettingsMap);
        webTestClient.post()
                     .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                  .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                  .queryParam("base-version", 4L)
                                                  .queryParam("delta-version", 2L)
                                                  .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange()
                     .expectStatus()
                     .is2xxSuccessful();
        Awaitility.await().timeout(20L, TimeUnit.SECONDS).untilAsserted(() -> {
            //---MovementDataPersonGroupDay assertion---
            List<MovementDataPersonGroupDay> movementDataPersonGroupDaysResult = dBHelperService.findAllMovementDataPersonGroupDays();
            assertThat(movementDataPersonGroupDaysResult).hasSize(3);
            List<MovementDataPersonGroupDay> expectedMovementDataPersonGroupDay =
                    TestDataLoader.loadMongoDBList("json/collections/expected/accountGroupValue-not-null/movementDataPersonGroupDays.json",
                                                   MovementDataPersonGroupDay.class);
            assertThat(movementDataPersonGroupDaysResult).usingRecursiveFieldByFieldElementComparator()
                                                         .hasSameElementsAs(expectedMovementDataPersonGroupDay);
            //---MovementDataPersonGroupMonth assertion---
            List<MovementDataPersonGroupMonth> movementDataPersonGroupMonthResult = dBHelperService.findAllMovementDataPersonGroupMonths();
            assertThat(movementDataPersonGroupMonthResult).hasSize(3);
            List<MovementDataPersonGroupMonth> expectedMovementDataPersonGroupMonth =
                    TestDataLoader.loadMongoDBList("json/collections/expected/accountGroupValue-not-null/movementDataPersonGroupMonths.json",
                                                   MovementDataPersonGroupMonth.class);
            assertThat(movementDataPersonGroupMonthResult).usingRecursiveFieldByFieldElementComparator()
                                                          .hasSameElementsAs(expectedMovementDataPersonGroupMonth);
        });
    }

    @Test
    @Order(9)
    @DisplayName("Returns no content when no MasterDataContext is present in ACDS")
    void should_return_no_content_when_master_data_context_has_no_content() {
        Map<String, String> masterdataContextQueryParams =
                Map.of("base-version", "4", "delta-version", "2", "near-time-data", "true");
        wireMockServerSettingsMap.put("master-data-context-2021",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/masterdata-context")
                                                            .queryParams(masterdataContextQueryParams)
                                                            .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                            .httpStatus(HttpStatus.NO_CONTENT.value()).build());
        stubACDSMultipleYearsResponses(wireMockServer, wireMockServerSettingsMap);
        webTestClient.post()
                     .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                  .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                  .queryParam("base-version", 4L)
                                                  .queryParam("delta-version", 2L)
                                                  .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange()
                     .expectStatus()
                     .isNoContent();
    }

    @Test
    @Order(10)
    @DisplayName("Returns no content for delete when no data is present in mongo")
    void should_return_no_content_for_delete_when_no_data_is_present() {
        webTestClient.delete()
                     .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                  .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                  .queryParam("base-version", 4L)
                                                  .queryParam("delta-version", 2L)
                                                  .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange()
                     .expectStatus()
                     .isNoContent();
    }

    @Test
    @Order(11)
    @DisplayName("Check if the baseVersion and deltaVersion were updated in the LoggingContext when no query parameters were sent and they had default values")
    void should_update_and_log_base_version_and_delta_version_after_they_had_default_values() {
        // prepare
        wireMockServerSettingsMap.put("account-sum-days-2021",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/account-sum-days")
                                                            .queryParams(accountSumDaysQueryParams)
                                                            .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                            .httpStatus(HttpStatus.OK.value())
                                                            .path("json/acds-responses/wiremock/master-data/account-sum-days-2021.ndjson")
                                                            .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        wireMockServerSettingsMap.put("collective-accounts-2021",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/collective-accounts")
                                                            .queryParams(collectiveQueryParams)
                                                            .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                            .httpStatus(HttpStatus.OK.value())
                                                            .path("json/acds-responses/wiremock/master-data/collective-accounts.ndjson")
                                                            .mediaType(MediaType.APPLICATION_NDJSON_VALUE).build());
        Map<String, String> masterdataContextQueryParams =
                Map.of("base-version", "0", "delta-version", "0", "near-time-data", "true");
        String masterdataContextJsonPath = "json/acds-responses/wiremock/master-data-context-2021.json";
        wireMockServerSettingsMap.put("master-data-context-2021",
                                      WireMockServerSettings.builder()
                                                            .url(BASE_URL + "consultants/" + TEST_CONSULTANT + "/clients/"
                                                                         + TEST_CLIENT + "/masterdata-context")
                                                            .queryParams(masterdataContextQueryParams)
                                                            .fiscalYears(List.of(TEST_FISCAL_YEAR_2021_START))
                                                            .httpStatus(HttpStatus.OK.value())
                                                            .path(masterdataContextJsonPath)
                                                            .mediaType(MediaType.APPLICATION_JSON_VALUE).build());
        stubACDSMultipleYearsResponses(wireMockServer, wireMockServerSettingsMap);
        // execute without query parameters so the default base and delta version will be assigned
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                         .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                         .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange()
                     .expectStatus().is2xxSuccessful();
        // assert
        Awaitility.await().timeout(100L, TimeUnit.SECONDS).untilAsserted(() -> {
            // check if InitialLoad is finished
            List<StateDoc> stateDocResult = dBHelperService.findAllStateDocs();
            assertThat(stateDocResult).isNotNull().hasSize(1);
            assertThat(stateDocResult.get(0).getState()).isEqualTo(StateDocState.DONE);
            // assert that Logs have the correct baseVersion and deltaVersion
            List<ILoggingEvent> initialLoadReceived = initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG);
            assertThat(initialLoadReceived).isNotNull().hasSize(1);
            // should contain the base_version=0(default) because no base-version query parameter was passed when calling the InitialLoad endpoint
            assertThat(initialLoadReceived.get(0).getMDCPropertyMap()).containsEntry(LoggingUtil.BASE_VERSION_KEY,
                                                                                     LoggingContextFilter.DEFAULT_BASE_DELTA_VERSION);
            // should contain the delta_version=0(default) because no delta-version query parameter was passed when calling the InitialLoad endpoint
            assertThat(initialLoadReceived.get(0).getMDCPropertyMap()).containsEntry(LoggingUtil.DELTA_VERSION_KEY,
                                                                                     LoggingContextFilter.DEFAULT_BASE_DELTA_VERSION);
            List<ILoggingEvent> initialLoadStart = importExecutionServiceMemoryAppender.search(FULL_IMPORT_STARTED_LOG);
            assertThat(initialLoadStart).isNotNull().hasSize(1);
            // should contain the base_version=0(default) because no base-version query parameter was passed when calling the InitialLoad endpoint
            assertThat(initialLoadStart.get(0).getMDCPropertyMap()).containsEntry(LoggingUtil.BASE_VERSION_KEY,
                                                                                  LoggingContextFilter.DEFAULT_BASE_DELTA_VERSION);
            // should contain the delta_version=0(default) because no delta-version query parameter was passed when calling the InitialLoad endpoint
            assertThat(initialLoadStart.get(0).getMDCPropertyMap()).containsEntry(LoggingUtil.DELTA_VERSION_KEY,
                                                                                  LoggingContextFilter.DEFAULT_BASE_DELTA_VERSION);
            MasterdataContext masterdataContext = TestDataLoader.load(masterdataContextJsonPath, MasterdataContext.class);
            List<ILoggingEvent> initialLoadSuccess = loggingUtilMemoryAppender.search(FULL_IMPORT_SUCCESS_LOG.replace(TARGET, REPLACEMENT));
            assertThat(initialLoadSuccess).isNotNull().hasSize(1);
            // should contain the base_version=4 because it was updated from ACDS MasterdataContext
            assertThat(initialLoadSuccess.get(0).getMDCPropertyMap()).containsEntry(LoggingUtil.BASE_VERSION_KEY,
                                                                                    masterdataContext.getBaseVersion().toString());
            // should contain the delta_version=0 because it was updated from ACDS MasterdataContext
            assertThat(initialLoadSuccess.get(0).getMDCPropertyMap()).containsEntry(LoggingUtil.DELTA_VERSION_KEY,
                                                                                    masterdataContext.getDeltaVersion().toString());
            List<ILoggingEvent> changeEventSuccess =
                    changeEventProducerMemoryAppender.search(ChangeEventProducer.CHANGED_EVENT_SUCCESS_LOG.replace("{}", REPLACEMENT));
            assertThat(changeEventSuccess).isNotNull().hasSize(1);
            // should contain the base_version=4 because it was updated from ACDS MasterdataContext
            assertThat(changeEventSuccess.get(0).getMDCPropertyMap()).containsEntry(LoggingUtil.BASE_VERSION_KEY,
                                                                                    masterdataContext.getBaseVersion().toString());
            // should contain the delta_version=0 because it was updated from ACDS MasterdataContext
            assertThat(changeEventSuccess.get(0).getMDCPropertyMap()).containsEntry(LoggingUtil.DELTA_VERSION_KEY,
                                                                                    masterdataContext.getDeltaVersion().toString());
        });
    }

    private void assertLogs() {
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).hasSize(2);
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace(TARGET, REPLACEMENT), Level.INFO)).hasSize(2);
        assertThat(importExecutionServiceMemoryAppender.search(FULL_IMPORT_STARTED_LOG, Level.INFO)).hasSize(2);
        assertThat(loggingUtilMemoryAppender.search(FULL_IMPORT_SUCCESS_LOG.replace(TARGET, REPLACEMENT), Level.INFO)).hasSize(2);

        assertThat(loggingUtilMemoryAppender.search(ACDS_GET_ACCOUNT_CAPTIONS_LOG.replace(TARGET, REPLACEMENT), Level.DEBUG)).hasSize(2);
        assertThat(loggingUtilMemoryAppender.search(ACDS_GET_ACCOUNT_PURPOSE_MAPPINGS_LOG.replace(TARGET, REPLACEMENT), Level.DEBUG)).hasSize(2);
        assertThat(loggingUtilMemoryAppender.search(ACDS_GET_COLLECTIVE_ACCOUNTS_LOG.replace(TARGET, REPLACEMENT), Level.DEBUG)).hasSize(2);
        assertThat(loggingUtilMemoryAppender.search(ACDS_GET_CUSTOM_COLUMN_STRUCTURES_LOG.replace(TARGET, REPLACEMENT), Level.DEBUG)).hasSize(2);
        assertThat(loggingUtilMemoryAppender.search(ACDS_GET_CUSTOM_REPORT_STRUCTURES_LOG.replace(TARGET, REPLACEMENT), Level.DEBUG)).hasSize(2);
        assertThat(loggingUtilMemoryAppender.search(ACDS_GET_MASTERDATA_CONTEXT_LOG.replace(TARGET, REPLACEMENT), Level.DEBUG)).hasSize(2);
        assertThat(loggingUtilMemoryAppender.search(ACDS_GET_MASTERDATA_INVENTORIES_LOG.replace(TARGET, REPLACEMENT), Level.DEBUG)).hasSize(2);
        assertThat(loggingUtilMemoryAppender.search(ACDS_GET_SHAREHOLDERS_LOG.replace(TARGET, REPLACEMENT), Level.DEBUG)).hasSize(2);
        assertThat(loggingUtilMemoryAppender.search(ACDS_GET_TRANSLATIONS_LOG.replace(TARGET, REPLACEMENT), Level.DEBUG)).hasSize(2);

        assertThat(importServiceMemoryAppender.search(MOVEMENT_DATA_DAYS_BATCH_START_LOG, Level.DEBUG)).hasSize(2);
        assertThat(loggingUtilMemoryAppender.search(MOVEMENT_DATA_DAYS_BATCH_SUCCESS_LOG.replace(TARGET, REPLACEMENT), Level.DEBUG)).hasSize(2);

        assertThat(loggingUtilMemoryAppender.search(MOVEMENT_DATA_DAY_REPOSITORY_BULK_UPSERT_LOG.replace(TARGET, REPLACEMENT), Level.TRACE)).hasSize(
                15);

        assertThat(importServiceMemoryAppender.search(MOVEMENT_DATA_MONTHS_BATCH_START_LOG, Level.DEBUG)).hasSize(2);
        assertThat(loggingUtilMemoryAppender.search(MOVEMENT_DATA_MONTHS_BATCH_SUCCESS_LOG.replace(TARGET, REPLACEMENT), Level.DEBUG)).hasSize(2);

        assertThat(
                loggingUtilMemoryAppender.search(MOVEMENT_DATA_MONTH_REPOSITORY_BULK_INSERT_LOG.replace(TARGET, REPLACEMENT), Level.TRACE)).hasSize(
                23);

        assertThat(importServiceMemoryAppender.search(MOVEMENT_DATA_INVENTORIES_BATCH_START_LOG, Level.DEBUG)).hasSize(2);
        assertThat(loggingUtilMemoryAppender.search(MOVEMENT_DATA_INVENTORIES_BATCH_SUCCESS_LOG.replace(TARGET, REPLACEMENT), Level.DEBUG)).hasSize(
                2);

        assertThat(loggingUtilMemoryAppender.search(MOVEMENT_DATA_INVENTORY_REPOSITORY_BULK_UPSERT_LOG.replace(TARGET, REPLACEMENT),
                                                    Level.TRACE)).hasSize(4);
    }
}
