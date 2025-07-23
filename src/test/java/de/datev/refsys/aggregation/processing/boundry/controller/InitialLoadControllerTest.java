package de.datev.refsys.aggregation.processing.boundry.controller;

import ch.qos.logback.classic.Level;
import de.datev.refsys.aggregation.document.model.ProblemInfo;
import de.datev.refsys.aggregation.processing.api.model.Problem;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallNoContentException;
import de.datev.refsys.aggregation.processing.exception.HttpCallTechnicalException;
import de.datev.refsys.aggregation.processing.exception.InitialLoadFailedException;
import de.datev.refsys.aggregation.processing.model.ImportData;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
import de.datev.refsys.aggregation.processing.service.CommonImportService;
import de.datev.refsys.aggregation.processing.service.ImportExecutionServiceImpl;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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

import static de.datev.refsys.aggregation.processing.util.LoggingUtil.INITIAL_LOAD_RESPONSE_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.INITIAL_LOAD_START_LOG;
import static de.datev.refsys.aggregation.processing.util.TestUtil.CALL_NOT_PERMITTED_CIRCUIT_BREAKER_ERROR;
import static de.datev.refsys.aggregation.processing.util.TestUtil.EXCEPTION_MESSAGE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_BASE_VERSION_UPDATE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION_UPDATE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_TYPE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createProblemInfo;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = { TestResilienceConfiguration.class})
@WebFluxTest(InitialLoadController.class)
@ActiveProfiles(TEST_PROFILE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InitialLoadControllerTest {
    private WebTestClient webTestClient;

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private CommonImportService commonImportService;

    private MemoryAppender loggingUtilMemoryAppender;
    private MemoryAppender initialLoadControllerMemoryAppender;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient
                .bindToApplicationContext(context)
                .configureClient()
                .baseUrl("/api/v1")
                .build();
        loggingUtilMemoryAppender = setupMemoryAppender(loggingUtilMemoryAppender, LoggingUtil.class, Level.INFO);
        initialLoadControllerMemoryAppender = setupMemoryAppender(initialLoadControllerMemoryAppender, InitialLoadController.class, Level.INFO);
    }

    @Test
    void should_return_500_when_unexpected_exception_is_thrown() {
        when(commonImportService.doFireAndForgetFullImport(anyInt(), anyInt(), anyInt(), anyLong(), anyLong())).thenThrow(new NullPointerException(EXCEPTION_MESSAGE));
        Problem problem = webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                        .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                        .build(TEST_CONSULTANT, TEST_CLIENT))
                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(Problem.class)
                .returnResult().getResponseBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).isEqualTo(EXCEPTION_MESSAGE);
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).isEmpty();
    }

    @Test
    void should_return_400_when_missingRequestValue_exception_is_thrown() {
        Problem problem = webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                        .build(TEST_CONSULTANT, TEST_CLIENT))
                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Problem.class)
                .returnResult().getResponseBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).isEqualTo("Required query parameter 'fiscal-year' is not present.");
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).isEmpty();
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).isEmpty();
    }

    @Test
    void should_return_400_when_serverWebInputException_exception_is_thrown() {
        Problem problem = webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                        .queryParam("fiscal-year", "wrong parameter")
                        .build(TEST_CONSULTANT, TEST_CLIENT))
                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Problem.class)
                .returnResult().getResponseBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).isEqualTo("Type mismatch.");
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).isEmpty();
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).isEmpty();
    }

    @Test
    void should_return_400_when_constraintViolationException_exception_is_thrown() {
        Problem problem = webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/1/clients/{client}/initial-load")
                        .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                        .build(TEST_CLIENT))
                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Problem.class)
                .returnResult().getResponseBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).isEqualTo("The consultant must be greater than or equal to 1000");
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).isEmpty();
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).isEmpty();
    }

    @Test
    void should_return_404_when_responseStatusException_exception_is_thrown() {
        Problem problem = webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/wrong-path")
                        .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                        .build(TEST_CONSULTANT, TEST_CLIENT))
                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(Problem.class)
                .returnResult().getResponseBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).isEmpty();
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).isEmpty();
    }

    @Test
    void should_return_custom_status_when_business_exception_is_thrown() {
        HttpStatus badRequest = HttpStatus.BAD_REQUEST;
        when(commonImportService.doFireAndForgetFullImport(anyInt(), anyInt(), anyInt(), anyLong(), anyLong())).thenThrow(
                new AggregationProcessingBusinessException(EXCEPTION_MESSAGE, badRequest.value()));
        Problem problem = webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                        .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                        .build(TEST_CONSULTANT, TEST_CLIENT))
                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Problem.class)
                .returnResult().getResponseBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).isEqualTo(EXCEPTION_MESSAGE);
        assertThat(problem.getStatus()).isEqualTo(badRequest.value());
        assertThat(problem.getTitle()).isEqualTo(badRequest.getReasonPhrase());
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).isEmpty();
    }

    @Test
    void should_return_custom_status_when_initialLoadFailed_exception_is_thrown() {
        HttpStatus responseStatus = HttpStatus.CONFLICT;
        when(commonImportService.doFireAndForgetFullImport(anyInt(), anyInt(), anyInt(), anyLong(), anyLong())).thenThrow(
                new InitialLoadFailedException(EXCEPTION_MESSAGE, responseStatus.value(), TEST_TYPE));
        Problem problem = webTestClient.post()
                                       .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                                    .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                                    .build(TEST_CONSULTANT, TEST_CLIENT))
                                       .accept(MediaType.APPLICATION_PROBLEM_JSON)
                                       .exchange()
                                       .expectStatus().isEqualTo(responseStatus.value())
                                       .expectBody(Problem.class)
                                       .returnResult().getResponseBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).isEqualTo(EXCEPTION_MESSAGE);
        assertThat(problem.getStatus()).isEqualTo(responseStatus.value());
        assertThat(problem.getTitle()).isEqualTo(responseStatus.getReasonPhrase());
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).isEmpty();
    }

    @Test
    void should_return_custom_callNotPermittedException_when_callNotPermittedException_is_thrown() {
        HttpStatus internalServerError = HttpStatus.INTERNAL_SERVER_ERROR;
        CircuitBreaker testCircuitBreaker = CircuitBreaker.ofDefaults("test");
        testCircuitBreaker.transitionToOpenState();

        when(commonImportService.doFireAndForgetFullImport(anyInt(), anyInt(), anyInt(), anyLong(), anyLong()))
                .thenThrow(CallNotPermittedException.createCallNotPermittedException(testCircuitBreaker));
        Problem problem = webTestClient.post()
                                       .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                                    .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                                    .build(TEST_CONSULTANT, TEST_CLIENT))
                                       .accept(MediaType.APPLICATION_PROBLEM_JSON)
                                       .exchange()
                                       .expectStatus().is5xxServerError()
                                       .expectBody(Problem.class)
                                       .returnResult().getResponseBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).isEqualTo(String.format(CALL_NOT_PERMITTED_CIRCUIT_BREAKER_ERROR,"test"));
        assertThat(problem.getStatus()).isEqualTo(internalServerError.value());
        assertThat(problem.getTitle()).isEqualTo(internalServerError.getReasonPhrase());
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).isEmpty();
    }


    @Test
    @DisplayName("Test InitialLoad Endpoint with default Base- and Delta-Version values")
    void should_use_default_values_for_base_and_delta_version_if_none_are_given_in_query() {
        when(commonImportService.doFireAndForgetFullImport(anyInt(), anyInt(), anyInt(), anyLong(), anyLong())).thenReturn(Mono.empty());
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                         .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START).build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange();
        verify(commonImportService, times(1)).doFireAndForgetFullImport(any(), any(), any(), eq(0L), eq(0L));
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).hasSize(1);
    }

    @Test
    @DisplayName("Test InitialLoad Endpoint with Base- and Delta-Version values in the query")
    void should_use_correct_values_for_base_and_delta_version_that_are_given_in_query() {
        when(commonImportService.doFireAndForgetFullImport(anyInt(), anyInt(), anyInt(), anyLong(), anyLong())).thenReturn(Mono.empty());
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                         .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                         .queryParam("base-version", TEST_DELTA_VERSION_UPDATE)
                                                         .queryParam("delta-version", TEST_BASE_VERSION_UPDATE)
                                                         .build(TEST_CONSULTANT, TEST_CLIENT))
                     .exchange();
        verify(commonImportService, times(1)).doFireAndForgetFullImport(any(), any(), any(), eq(TEST_DELTA_VERSION_UPDATE),
                                                                        eq(TEST_BASE_VERSION_UPDATE));
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).hasSize(1);
    }

    @Test
    @DisplayName("Test InitialLoad Endpoint when a HttpCallBusinessException occurs")
    void should_return_custom_status_and_problem_when_http_call_business_exception_is_thrown() {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemInfo problemInfo = createProblemInfo();
        when(commonImportService.doFireAndForgetFullImport(anyInt(), anyInt(), anyInt(), anyLong(), anyLong())).thenThrow(
                new HttpCallBusinessException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.MASTER_DATA_CONTEXT, httpStatus.value(),
                                              problemInfo));
        Problem problem = webTestClient.post()
                                       .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                                    .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                                    .build(TEST_CONSULTANT, TEST_CLIENT))
                                       .accept(MediaType.APPLICATION_PROBLEM_JSON)
                                       .exchange()
                                       .expectStatus().is5xxServerError()
                                       .expectBody(Problem.class)
                                       .returnResult().getResponseBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).isEqualTo(problemInfo.getDetail());
        assertThat(problem.getStatus()).isEqualTo(httpStatus.value());
        assertThat(problem.getTitle()).isEqualTo(problemInfo.getTitle());
        assertThat(problem.getType()).isEqualTo(problemInfo.getType());
        assertThat(problem.getInstance()).isEqualTo(problemInfo.getInstance());
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).isEmpty();
    }

    @Test
    @DisplayName("Test InitialLoad Endpoint when a HttpCallTechnicalException occurs")
    void should_return_custom_status_and_problem_when_http_call_technical_exception_is_thrown() {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemInfo problemInfo = createProblemInfo();
        when(commonImportService.doFireAndForgetFullImport(anyInt(), anyInt(), anyInt(), anyLong(), anyLong())).thenThrow(
                new HttpCallTechnicalException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.MASTER_DATA_CONTEXT, httpStatus.value(),
                                               problemInfo));
        Problem problem = webTestClient.post()
                                       .uri(uriBuilder -> uriBuilder.path("/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                                    .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                                    .build(TEST_CONSULTANT, TEST_CLIENT))
                                       .accept(MediaType.APPLICATION_PROBLEM_JSON)
                                       .exchange()
                                       .expectStatus().is5xxServerError()
                                       .expectBody(Problem.class)
                                       .returnResult().getResponseBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).isEqualTo(problemInfo.getDetail());
        assertThat(problem.getStatus()).isEqualTo(httpStatus.value());
        assertThat(problem.getTitle()).isEqualTo(problemInfo.getTitle());
        assertThat(problem.getType()).isEqualTo(problemInfo.getType());
        assertThat(problem.getInstance()).isEqualTo(problemInfo.getInstance());
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).isEmpty();
    }

    @Test
    @DisplayName("Test InitialLoad Endpoint when a HttpCallNoContentException occurs")
    void should_return_no_content_status_when_http_call_no_content_exception_is_thrown() {
        HttpStatus httpStatus = HttpStatus.NO_CONTENT;
        when(commonImportService.doFireAndForgetFullImport(anyInt(), anyInt(), anyInt(), anyLong(), anyLong())).thenThrow(
                new HttpCallNoContentException(SourceError.ACDS, SourceEndpoint.MASTER_DATA_CONTEXT, httpStatus.value()));
        EntityExchangeResult<Void> exchangeResult = webTestClient.post()
                                                                 .uri(uriBuilder -> uriBuilder.path(
                                                                                                      "/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                                                              .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                                                              .build(TEST_CONSULTANT, TEST_CLIENT))
                                                                 .exchange()
                                                                 .expectStatus().isNoContent()
                                                                 .expectBody()
                                                                 .isEmpty();
        assertThat(exchangeResult.getResponseBody()).isNull();
        assertThat(initialLoadControllerMemoryAppender.search(INITIAL_LOAD_START_LOG, Level.INFO)).hasSize(1);
        assertThat(loggingUtilMemoryAppender.search(INITIAL_LOAD_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).isEmpty();
    }
}
