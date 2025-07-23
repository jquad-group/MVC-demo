package de.datev.refsys.aggregation.processing.boundry.controller;

import ch.qos.logback.classic.Level;
import de.datev.refsys.aggregation.document.model.MasterData;
import de.datev.refsys.aggregation.document.model.MasterDataAccount;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.processing.api.model.AccountSumDayDelta;
import de.datev.refsys.aggregation.processing.api.model.DeltaRequest;
import de.datev.refsys.aggregation.processing.configuration.TestApplicationInitializer;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import de.datev.refsys.aggregation.processing.configuration.WireMockTestConfiguration;
import de.datev.refsys.aggregation.processing.service.DeltaEventProcessingServiceImpl;
import de.datev.refsys.aggregation.processing.util.ClearDatabaseAnCreateIndexesBeforeEachTest;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.MongoHelperService;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static de.datev.refsys.aggregation.processing.util.LoggingUtil.DELTA_EVENT_BATCH_START_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.DELTA_EVENT_BATCH_SUCCESS_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.MOVEMENT_DATA_DAY_REPOSITORY_FIND_ALL_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.PROCESS_DELTA_RESPONSE_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.PROCESS_DELTA_START_LOG;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;

@ImportAutoConfiguration(TestMongoClientConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ContextConfiguration(initializers = TestApplicationInitializer.class, classes = { WireMockTestConfiguration.class})
@ClearDatabaseAnCreateIndexesBeforeEachTest
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles(TEST_PROFILE)
class ProcessDeltaControllerIT {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private MongoHelperService mongoHelperService;

    private WebTestClient webTestClient;
    private MemoryAppender loggingUtilMemoryAppender;
    private MemoryAppender processDeltaMemoryAppender;
    private MemoryAppender deltaEventProcessingMemoryAppender;

    @BeforeEach
    void setup() {
        webTestClient = WebTestClient.bindToApplicationContext(context).configureClient().baseUrl("/api/v1").build();

        loggingUtilMemoryAppender = setupMemoryAppender(loggingUtilMemoryAppender, LoggingUtil.class, Level.TRACE);
        processDeltaMemoryAppender = setupMemoryAppender(processDeltaMemoryAppender, ProcessDeltaController.class, Level.INFO);
        deltaEventProcessingMemoryAppender = setupMemoryAppender(deltaEventProcessingMemoryAppender, DeltaEventProcessingServiceImpl.class, Level.DEBUG);
    }

    @Test
    @DisplayName("Should process delta event")
    void should_process_delta_event() {
        AccountSumDayDelta accountSumDayDelta1 = new AccountSumDayDelta(110000000, 5, 0, 1, 0, 0.19F, true, 20210501);
        accountSumDayDelta1.amountCredit(20.5D).amountDebit(20.5D).rwShareholderId(UUID.fromString("9c18c30b-ac17-451f-81c7-1b9b26dd73fe")).cost1("91").cost2("A2").agricultureAndForestryAccountType(25);

        AccountSumDayDelta accountSumDayDelta2 = new AccountSumDayDelta(110000000, 5, 0, 1, 0, 0.19F, true, 20210501);
        accountSumDayDelta2.amountCredit(20.5D).amountDebit(20.5D).cost1("91").cost2("A2").agricultureAndForestryAccountType(25);

        AccountSumDayDelta accountSumDayDelta3 = new AccountSumDayDelta(110000001, 5, 0, 1, 0, 0.19F, true, 20210502);
        accountSumDayDelta3.amountCredit(20.5D).amountDebit(20.5D).rwShareholderId(UUID.fromString("9c18c30b-ac17-451f-81c7-1b9b26dd73fe")).cost1("91").cost2("A2").agricultureAndForestryAccountType(25);

        AccountSumDayDelta accountSumDayDelta4 = new AccountSumDayDelta(110000002, 5, 0, 1, 0, 0.19F, true, 20210502);
        accountSumDayDelta4.amountCredit(100.0D).rwShareholderId(UUID.fromString("9c18c30b-ac17-451f-81c7-1b9b26dd73fe")).cost1("91").cost2("A2").agricultureAndForestryAccountType(25);

        List<AccountSumDayDelta> accountSumDayDeltaList = List.of(accountSumDayDelta1, accountSumDayDelta2, accountSumDayDelta3, accountSumDayDelta4);
        DeltaRequest deltaRequest = new DeltaRequest();
        deltaRequest.setAccountSumDayDeltas(accountSumDayDeltaList);
        deltaRequest.setContainsNearTimeData(true);

        List<MasterData> masterData = TestDataLoader.loadMongoDBList("json/collections/repository/delta/masterData.json", MasterData.class);
        mongoHelperService.insertManyMasterData(masterData);

        List<MasterDataAccount> masterDataAccounts = TestDataLoader.loadMongoDBList("json/collections/repository/delta/masterDataAccount.json", MasterDataAccount.class);
        mongoHelperService.insertManyMasterDataAccounts(masterDataAccounts);

        List<StateDoc> stateDocs = TestDataLoader.loadMongoDBList("json/collections/repository/delta/stateDoc.json", StateDoc.class);
        mongoHelperService.insertManyStateDocs(stateDocs);

        List<MovementDataDay> movementDataDays =
                TestDataLoader.loadMongoDBList("json/collections/repository/delta/movementDataDays.json", MovementDataDay.class);
        mongoHelperService.insertManyMovementDataDays(movementDataDays);

        List<MovementDataMonth> movementDataMonths =
                TestDataLoader.loadMongoDBList("json/collections/repository/delta/movementDataMonths.json", MovementDataMonth.class);
        mongoHelperService.insertManyMovementDataMonths(movementDataMonths);

        List<MovementDataPersonGroupDay> movementDataPersonGroupDays =
                TestDataLoader.loadMongoDBList("json/collections/repository/delta/movementDataPersonGroupDays.json",
                                               MovementDataPersonGroupDay.class);
        mongoHelperService.insertManyMovementDataPersonGroupDays(movementDataPersonGroupDays);

        List<MovementDataPersonGroupMonth> movementDataPersonGroupMonths =
                TestDataLoader.loadMongoDBList("json/collections/repository/delta/movementDataPersonGroupMonths.json",
                                               MovementDataPersonGroupMonth.class);
        mongoHelperService.insertManyMovementDataPersonGroupMonths(movementDataPersonGroupMonths);

        OffsetDateTime timeStampBeforeImport = OffsetDateTime.now();

        webTestClient.post()
                     .uri(uriBuilder -> uriBuilder.path(
                                                          "/aggregation-processing/consultants" +
                                                                  "/{consultant}/clients/{client"
                                                                  + "}/process-delta")
                                                  .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                  .queryParam("base-version", 1)
                                                  .queryParam("delta-version", 2)
                                                  .build(TEST_CONSULTANT, TEST_CLIENT))
                     .bodyValue(deltaRequest)
                     .exchange()
                     .expectStatus()
                     .is2xxSuccessful();
        Awaitility.await().timeout(20L, TimeUnit.SECONDS).untilAsserted(() -> {
            List<StateDoc> resultStateDocList = mongoHelperService.findAllStateDocs();
            assertThat(resultStateDocList).hasSize(1);
            StateDoc resultStateDoc = resultStateDocList.get(0);
            StateDoc expectedStateDocs = TestDataLoader.loadDBElement("json/collections/expected/delta/stateDoc.json", StateDoc.class);
            assertThat(resultStateDoc).usingRecursiveComparison().ignoringFieldsOfTypes(OffsetDateTime.class).isEqualTo(expectedStateDocs);
            assertThat(resultStateDoc.getStateTimestamp()).isAfter(timeStampBeforeImport);

            List<MasterData> resultMasterDataList = mongoHelperService.findAllMasterData();
            assertThat(resultMasterDataList).hasSize(1);
            MasterData expectedMasterData = TestDataLoader.loadDBElement("json/collections/expected/delta/masterData.json", MasterData.class);
            assertThat(resultMasterDataList.get(0)).usingRecursiveComparison().isEqualTo(expectedMasterData);

            List<MasterDataAccount> resultMasterDataAccounts = mongoHelperService.findAllMasterDataAccounts();
            assertThat(resultMasterDataAccounts).hasSize(3);
            List<MasterDataAccount> expectedMasterDataAccounts = TestDataLoader.loadMongoDBList("json/collections/expected/delta/masterDataAccount.json", MasterDataAccount.class);
            assertThat(resultMasterDataAccounts).usingRecursiveComparison().isEqualTo(expectedMasterDataAccounts);

            List<MovementDataDay> resultMovementDataDayList = mongoHelperService.findAllMovementDataDays();
            assertThat(resultMovementDataDayList).hasSize(4);
            List<MovementDataDay> expectedMovementDataDays =
                    TestDataLoader.loadMongoDBList("json/collections/expected/delta/movementDataDays.json", MovementDataDay.class);
            assertThat(resultMovementDataDayList).usingRecursiveComparison().isEqualTo(expectedMovementDataDays);

            List<MovementDataMonth> resultMovementDataMonthList = mongoHelperService.findAllMovementDataMonths();
            assertThat(resultMovementDataMonthList).hasSize(4);
            List<MovementDataMonth> expectedMovementDataMonths =
                    TestDataLoader.loadMongoDBList("json/collections/expected/delta/movementDataMonths.json", MovementDataMonth.class);
            assertThat(resultMovementDataMonthList).usingRecursiveComparison().isEqualTo(expectedMovementDataMonths);

            List<MovementDataPersonGroupDay> resultMovementDataPersonGroupDayList = mongoHelperService.findAllMovementDataPersonGroupDays();
            assertThat(resultMovementDataPersonGroupDayList).hasSize(2);
            MovementDataPersonGroupDay expectedMovementDataPersonGroupDays =
                    TestDataLoader.loadDBElement("json/collections/expected/delta/movementDataPersonGroupDays.json",
                                                   MovementDataPersonGroupDay.class);
            assertThat(resultMovementDataPersonGroupDayList.get(0)).usingRecursiveComparison().isEqualTo(expectedMovementDataPersonGroupDays);

            List<MovementDataPersonGroupMonth> resultMovementDataPersonGroupMonthList = mongoHelperService.findAllMovementDataPersonGroupMonths();
            assertThat(resultMovementDataPersonGroupMonthList).hasSize(2);
            MovementDataPersonGroupMonth expectedMovementDataPersonGroupMonths =
                    TestDataLoader.loadDBElement("json/collections/expected/delta/movementDataPersonGroupMonths.json",
                                                   MovementDataPersonGroupMonth.class);
            assertThat(resultMovementDataPersonGroupMonthList.get(0)).usingRecursiveComparison().isEqualTo(expectedMovementDataPersonGroupMonths);


        });
        assertThat(processDeltaMemoryAppender.search(PROCESS_DELTA_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(PROCESS_DELTA_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).hasSize(1);

        assertThat(deltaEventProcessingMemoryAppender.search(DELTA_EVENT_BATCH_START_LOG, Level.DEBUG)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(DELTA_EVENT_BATCH_SUCCESS_LOG.replace("{}ms", ""), Level.DEBUG)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(MOVEMENT_DATA_DAY_REPOSITORY_FIND_ALL_LOG.replace("{}ms", ""), Level.TRACE)).hasSize(3);
    }
}
