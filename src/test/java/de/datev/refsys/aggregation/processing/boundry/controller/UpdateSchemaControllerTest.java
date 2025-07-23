package de.datev.refsys.aggregation.processing.boundry.controller;

import ch.qos.logback.classic.Level;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.service.UpdateSchemaService;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static de.datev.refsys.aggregation.processing.util.LoggingUtil.UPDATE_SCHEMA_RESPONSE_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.UPDATE_SCHEMA_START_LOG;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = { TestResilienceConfiguration.class })
@WebFluxTest(UpdateSchemaController.class)
@ActiveProfiles(TEST_PROFILE)
class UpdateSchemaControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UpdateSchemaService updateSchemaService;

    private MemoryAppender loggingUtilMemoryAppender;
    private MemoryAppender updateSchemaControllerMemoryAppender;

    @BeforeEach
    void setUp() {
        this.loggingUtilMemoryAppender = setupMemoryAppender(loggingUtilMemoryAppender, LoggingUtil.class, Level.INFO);
        this.updateSchemaControllerMemoryAppender = setupMemoryAppender(updateSchemaControllerMemoryAppender, UpdateSchemaController.class, Level.INFO);
    }

    @Test
    @DisplayName("UpdateSchema should be triggered and HttpStats should be 201 when data is present")
    void should_start_schema_update_and_return_201_when_data_is_present() {
        when(updateSchemaService.updateSchemaVersion(anyInt(), anyInt(), anyInt(), anyLong(), anyLong())).thenReturn(Mono.just(true));
        webTestClient.post()
                     .uri(uriBuilder -> uriBuilder.path("/api/v1/aggregation-processing/consultants/{consultant}/clients/{client}/update-schema")
                                                  .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                  .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange()
                     .expectStatus().isCreated();
        verify(updateSchemaService, times(1)).updateSchemaVersion(any(), any(), any(), any(), any());
        assertThat(updateSchemaControllerMemoryAppender.search(UPDATE_SCHEMA_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(UPDATE_SCHEMA_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).hasSize(1);
    }

    @Test
    @DisplayName("UpdateSchema shouldn't be triggered and HttpStats should be 204 when data isn't present")
    void should_not_start_schema_update_and_return_204_when_data_is_not_present() {
        when(updateSchemaService.updateSchemaVersion(anyInt(), anyInt(), anyInt(), anyLong(), anyLong())).thenReturn(Mono.empty());
        webTestClient.post()
                     .uri(uriBuilder -> uriBuilder.path("/api/v1/aggregation-processing/consultants/{consultant}/clients/{client}/update-schema")
                                                  .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                  .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange()
                     .expectStatus().isNoContent();
        verify(updateSchemaService, times(1)).updateSchemaVersion(any(), any(), any(), any(), any());
        assertThat(updateSchemaControllerMemoryAppender.search(UPDATE_SCHEMA_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(UPDATE_SCHEMA_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).hasSize(1);
    }
}