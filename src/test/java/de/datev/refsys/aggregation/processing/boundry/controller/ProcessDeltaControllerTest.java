package de.datev.refsys.aggregation.processing.boundry.controller;

import ch.qos.logback.classic.Level;
import de.datev.refsys.aggregation.processing.api.model.DeltaInfo;
import de.datev.refsys.aggregation.processing.api.model.Problem;
import de.datev.refsys.aggregation.processing.boundry.event.ChangeEventProducer;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.exception.RestExceptionHandler;
import de.datev.refsys.aggregation.processing.exception.RestWarnException;
import de.datev.refsys.aggregation.processing.service.DeltaEventProcessingService;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static de.datev.refsys.aggregation.processing.util.LoggingUtil.PROCESS_DELTA_RESPONSE_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.PROCESS_DELTA_START_LOG;
import static de.datev.refsys.aggregation.processing.util.TestUtil.EXCEPTION_MESSAGE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_BASE_VERSION_UPDATE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION_UPDATE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_TYPE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = { TestResilienceConfiguration.class })
@WebFluxTest(ProcessDeltaController.class)
@ActiveProfiles(TEST_PROFILE)
class ProcessDeltaControllerTest {

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private DeltaEventProcessingService deltaEventProcessingService;

    @MockitoBean
    private ChangeEventProducer changeEventProducer;

    private WebTestClient webTestClient;
    private MemoryAppender restExceptionMemoryAppender;
    private MemoryAppender loggingUtilMemoryAppender;
    private MemoryAppender processDeltaMemoryAppender;

    @BeforeEach
    void setUp() {
        when(changeEventProducer.sendMessage(anyInt(), anyInt(), anyInt(), anyLong(), any())).thenReturn(Mono.empty());
        this.restExceptionMemoryAppender = setupMemoryAppender(restExceptionMemoryAppender, RestExceptionHandler.class, Level.DEBUG);
        this.loggingUtilMemoryAppender = setupMemoryAppender(loggingUtilMemoryAppender, LoggingUtil.class, Level.INFO);
        this.processDeltaMemoryAppender = setupMemoryAppender(processDeltaMemoryAppender, ProcessDeltaController.class, Level.INFO);
        webTestClient = WebTestClient
                .bindToApplicationContext(context)
                .configureClient()
                .baseUrl("/api/v1")
                .build();
    }

    @Test
    @DisplayName("Returns a Problem response with status from the error when a RestWarnException occurs")
    void should_return_problem_when_a_rest_warn_exception_occurs() {
        HttpStatus expectedStatus = HttpStatus.BAD_REQUEST;
        RestWarnException restWarnException = new RestWarnException(EXCEPTION_MESSAGE, expectedStatus.value(), TEST_TYPE);
        when(deltaEventProcessingService.processDeltaEvent(anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any())).thenReturn(
                Mono.error(restWarnException));
        Problem problem = webTestClient.post()
                                       .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/process-delta")
                                                                    .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                                    .queryParam("base-version", TEST_BASE_VERSION_UPDATE)
                                                                    .queryParam("delta-version", TEST_DELTA_VERSION_UPDATE)
                                                                    .build(TEST_CONSULTANT, TEST_CLIENT))
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .accept(MediaType.APPLICATION_PROBLEM_JSON)
                                       .exchange()
                                       .expectStatus().isEqualTo(expectedStatus)
                                       .expectBody(Problem.class)
                                       .returnResult().getResponseBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getType()).isEqualTo(restWarnException.getType());
        assertThat(problem.getTitle()).isEqualTo(expectedStatus.getReasonPhrase());
        assertThat(problem.getDetail()).isEqualTo(restWarnException.getMessage());
        assertThat(processDeltaMemoryAppender.search(PROCESS_DELTA_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(PROCESS_DELTA_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).isEmpty();
    }

    @Test
    @DisplayName("Returns no content response when no stateDoc was found")
    void should_return_no_content_response_when_no_state_doc_was_found() {
        when(deltaEventProcessingService.processDeltaEvent(anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any())).thenReturn(Mono.empty());
        EntityExchangeResult<Void> exchangeResult =
                webTestClient.post()
                             .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/process-delta")
                                                          .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                          .queryParam("base-version", TEST_BASE_VERSION_UPDATE)
                                                          .queryParam("delta-version", TEST_DELTA_VERSION_UPDATE)
                                                          .build(TEST_CONSULTANT, TEST_CLIENT))
                             .contentType(MediaType.APPLICATION_JSON)
                             .exchange()
                             .expectStatus()
                             .isEqualTo(HttpStatus.NO_CONTENT)
                             .expectBody()
                             .isEmpty();
        assertThat(exchangeResult.getResponseBody()).isNull();
        assertThat(processDeltaMemoryAppender.search(PROCESS_DELTA_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(PROCESS_DELTA_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).hasSize(1);
    }

    @Test
    @DisplayName("Returns delta info and sends a kafka event when delta event processing was successful")
    void should_return_delta_info_and_send_a_kafka_event_when_delta_event_processing_was_successful() {
        DeltaInfo deltaInfo = new DeltaInfo(TEST_BASE_VERSION_UPDATE, TEST_DELTA_VERSION_UPDATE);
        when(deltaEventProcessingService.processDeltaEvent(anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any())).thenReturn(Mono.just(deltaInfo));
        EntityExchangeResult<DeltaInfo> exchangeResult =
                webTestClient.post()
                             .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/process-delta")
                                                          .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                          .queryParam("base-version", TEST_BASE_VERSION_UPDATE)
                                                          .queryParam("delta-version", TEST_DELTA_VERSION_UPDATE)
                                                          .build(TEST_CONSULTANT, TEST_CLIENT))
                             .contentType(MediaType.APPLICATION_JSON)
                             .exchange()
                             .expectStatus()
                             .isEqualTo(HttpStatus.CREATED)
                             .expectBody(DeltaInfo.class)
                             .returnResult();
        assertThat(exchangeResult.getResponseBody()).isNotNull().isEqualTo(deltaInfo);
        verify(changeEventProducer, times(1)).sendMessage(anyInt(), anyInt(), anyInt(), anyLong(), anyLong());
        assertThat(processDeltaMemoryAppender.search(PROCESS_DELTA_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(PROCESS_DELTA_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).hasSize(1);
    }
}