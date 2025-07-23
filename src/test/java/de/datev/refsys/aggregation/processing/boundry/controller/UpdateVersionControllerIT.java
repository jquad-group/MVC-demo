package de.datev.refsys.aggregation.processing.boundry.controller;

import ch.qos.logback.classic.Level;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.api.model.DeltaInfo;
import de.datev.refsys.aggregation.processing.configuration.TestApplicationInitializer;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import de.datev.refsys.aggregation.processing.configuration.WireMockTestConfiguration;
import de.datev.refsys.aggregation.processing.util.ClearDatabaseAnCreateIndexesBeforeEachTest;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.MongoHelperService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.OffsetDateTime;
import java.util.List;

import static de.datev.refsys.aggregation.processing.util.LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_VERSION_INFO_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.UPDATE_VERSION_RESPONSE_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.UPDATE_VERSION_START_LOG;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_END;
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
class UpdateVersionControllerIT {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private MongoHelperService dBHelperService;

    private WebTestClient webTestClient;
    private MemoryAppender loggingUtilMemoryAppender;
    private MemoryAppender updateVersionMemoryAppender;

    @BeforeEach
    void setup() {
        webTestClient = WebTestClient.bindToApplicationContext(context).configureClient().baseUrl("/api/v1").build();

        loggingUtilMemoryAppender = setupMemoryAppender(loggingUtilMemoryAppender, LoggingUtil.class, Level.DEBUG);
        updateVersionMemoryAppender = setupMemoryAppender(updateVersionMemoryAppender, UpdateVersionController.class, Level.INFO);
    }

    @Test
    @DisplayName("Should update deltaVersion")
    void should_update_version_if_correct_params_are_given() {
        DeltaInfo deltaInfo = new DeltaInfo();
        Long updatedVersion = 2L;
        deltaInfo.setBaseVersion(1L);
        deltaInfo.setDeltaVersion(updatedVersion);
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        StateDoc stateDoc = StateDoc.builder().consultant(TEST_CONSULTANT).client(TEST_CLIENT).yearBegin(TEST_FISCAL_YEAR_2021_START).yearEnd(TEST_FISCAL_YEAR_2021_END).baseVersion(1L).deltaVersion(1L).state(
                StateDocState.DONE).createdTimestamp(offsetDateTime).stateTimestamp(offsetDateTime).build();
        dBHelperService.insertOneStateDoc(stateDoc);
        webTestClient.post()
                     .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/update-version")
                                                  .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                  .build(TEST_CONSULTANT, TEST_CLIENT))
                     .bodyValue(deltaInfo)
                     .exchange()
                     .expectStatus()
                     .is2xxSuccessful();
        List<StateDoc> allStateDocs = dBHelperService.findAllStateDocs();
        assertThat(allStateDocs).isNotNull().hasSize(1);
        assertThat(allStateDocs.get(0).getDeltaVersion()).isEqualTo(updatedVersion);

        assertThat(updateVersionMemoryAppender.search(UPDATE_VERSION_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(UPDATE_VERSION_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(STATE_DOC_REPOSITORY_UPDATE_VERSION_INFO_LOG.replace("{}ms", ""), Level.DEBUG)).hasSize(1);
    }

    @Test
    @DisplayName("Should not update because delta-version Five")
    void should_not_update_because_delta_version_is_five() {
        DeltaInfo deltaInfo = new DeltaInfo();
        deltaInfo.setBaseVersion(1L);
        deltaInfo.setDeltaVersion(5L);
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        StateDoc stateDoc = StateDoc.builder().consultant(TEST_CONSULTANT).client(TEST_CLIENT).yearBegin(TEST_FISCAL_YEAR_2021_START).yearEnd(TEST_FISCAL_YEAR_2021_END).baseVersion(1L).deltaVersion(1L).state(
                StateDocState.DONE).createdTimestamp(offsetDateTime).stateTimestamp(offsetDateTime).build();
        dBHelperService.insertOneStateDoc(stateDoc);
        webTestClient.post()
                     .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/update-version")
                                                  .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                  .build(TEST_CONSULTANT, TEST_CLIENT))
                     .bodyValue(deltaInfo)
                     .exchange()
                     .expectStatus()
                     .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should not update because stateDoc not in done")
    void should_not_update_because_state_doc_not_in_done() {
        DeltaInfo deltaInfo = new DeltaInfo();
        deltaInfo.setBaseVersion(1L);
        deltaInfo.setDeltaVersion(1L);
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        StateDoc stateDoc = StateDoc.builder().consultant(TEST_CONSULTANT).client(TEST_CLIENT).yearBegin(TEST_FISCAL_YEAR_2021_START).yearEnd(TEST_FISCAL_YEAR_2021_END).baseVersion(1L).deltaVersion(1L).state(
                StateDocState.INIT).createdTimestamp(offsetDateTime).stateTimestamp(offsetDateTime).build();
        dBHelperService.insertOneStateDoc(stateDoc);
        webTestClient.post()
                     .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/update-version")
                                                  .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                  .build(TEST_CONSULTANT, TEST_CLIENT))
                     .bodyValue(deltaInfo)
                     .exchange()
                     .expectStatus()
                     .isEqualTo(HttpStatus.CONFLICT);
    }
}