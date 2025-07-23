package de.datev.refsys.aggregation.processing.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.tomakehurst.wiremock.WireMockServer;
import de.datev.refsys.aggregation.processing.client.test_model.EndpointInfo;
import de.datev.refsys.aggregation.processing.client.test_model.StatusCodeErrorInfo;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.configuration.ClientEndpointConfiguration;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.exception.HttpCallException;
import de.datev.refsys.aggregation.processing.exception.HttpCallTechnicalException;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
import de.datev.refsys.aggregation.processing.util.CircuitBreakerUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.ResetResilienceAfterEachTest;
import de.datev.refsys.aggregation.processing.util.TestUtil;
import de.datev.refsys.generated.acds.ApiClient;
import de.datev.refsys.generated.acds.api.AccountSumDaysApi;
import de.datev.refsys.generated.acds.api.MovementdataInventoriesApi;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.MOVEMENT_DATA_CLIENT_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.util.TestUtil.CALL_NOT_PERMITTED_CIRCUIT_BREAKER_ERROR;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.assertLogs;
import static de.datev.refsys.aggregation.processing.util.TestUtil.searchCircuitBreakerLog;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class, classes = {ClientEndpointConfiguration.class, TestResilienceConfiguration.class})
@ResetResilienceAfterEachTest
@ActiveProfiles(TestUtil.TEST_PROFILE)
class MovementDataClientTest {
    private static final int BAD_CLIENT = 1;
    private static final String ACCOUNT_SUM_DAYS_ENDPOINT = "account-sum-days";
    private static final String ACCOUNT_SUM_MONTHS_ENDPOINT = "account-sum-months";
    private static final Map<String, EndpointInfo> ENDPOINT_INFO_MAP = getEndpointInfoMap();
    private static final Map<Integer, StatusCodeErrorInfo> STATUS_CODE_ERROR_INFO_MAP = TestUtil.getStatusCodeErrorInfoMap();

    @Autowired
    private ApiClient sumDaysApiClient;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    @Qualifier("apiClient")
    private ApiClient apiClient;

    @MockitoSpyBean
    private AccountSumDaysApi accountSumDaysApi;

    @MockitoSpyBean
    private MovementdataInventoriesApi movementdataInventoriesApi;

    @Value("${ref-sys.client.acds.timeout-in-ms}")
    private int timeoutInMs;

    @Value("${resilience4j.circuitbreaker.configs.shared-config.slidingWindowSize}")
    private Integer slidingWindowSize;

    private MovementDataClient movementDataClient;

    private MemoryAppender memoryAppender;

    @BeforeEach
    void setUp() {
        memoryAppender = setupMemoryAppender(memoryAppender, CircuitBreakerUtil.class, Level.WARN);

        movementDataClient = new MovementDataClient(accountSumDaysApi, movementdataInventoriesApi, circuitBreakerRegistry, retryRegistry);
        wireMockServer.resetRequests();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "404;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "400;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "500;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR},
            delimiter = ';')
    void should_throw_HttpCallTechnical_exception_when_getAccountSumDays_return_invalid_response(int statusCode, String exceptionMessage) {
        TestUtil.stubACDSResponse(wireMockServer, statusCode, "account-sum-days", Map.of("fiscal-year", TestUtil.TEST_FISCAL_YEAR_2022_START,
                "base-version", TestUtil.TEST_BASE_VERSION, "delta-version", TestUtil.TEST_DELTA_VERSION), "");
        Throwable throwable = catchThrowable(() -> movementDataClient.getAccountSumDays(TestUtil.createTestMasterdataContext()).contextWrite(TestUtil.TEST_CONTEXT).collectList().block());
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class)
                .hasMessage(String.format(exceptionMessage, SourceEndpoint.ACCOUNT_SUM_DAYS.getValue()));
        HttpCallTechnicalException httpCallTechnicalException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallTechnicalException.getSource()).isEqualTo(SourceError.ACDS);
        assertThat(httpCallTechnicalException.getSourceEndpoint()).isEqualTo(SourceEndpoint.ACCOUNT_SUM_DAYS);
        assertThat(httpCallTechnicalException.getHttpStatusCode()).isEqualTo(statusCode);
    }

    @DisplayName("Test circuit breaker for recorded and retryable exception 500")
    @ParameterizedTest(name = "{index} => {0}")
    @CsvSource(value = {
            ACCOUNT_SUM_DAYS_ENDPOINT
    }, delimiter = ';')
    void circuit_breaker_recorded_retryable_exception(String endpoint) {
        EndpointInfo endpointInfo = ENDPOINT_INFO_MAP.get(endpoint);
        final int testStatusCode = 500;

        TestUtil.stubAcdsClientApi(endpoint, TEST_CLIENT, endpointInfo.getBodyUrl(), 200, wireMockServer);
        TestUtil.stubAcdsClientApi(endpoint, BAD_CLIENT, null, testStatusCode, wireMockServer);
        CircuitBreaker generalCircuitBreaker = circuitBreakerRegistry.circuitBreaker(endpointInfo.getCircuitBreakerName());
        generalCircuitBreaker.transitionToClosedState();


        MasterdataContext mdc = TestUtil.getMasterdataContext(TEST_CLIENT);
        MasterdataContext mdcBad = TestUtil.getMasterdataContext(BAD_CLIENT);

        // send 2 good requests
        IntStream.range(0, 2).forEachOrdered(n -> movementDataClient.getAccountSumDays(mdc).collectList().contextWrite(TestUtil.TEST_CONTEXT).block());


        // assert state not changed (CLOSED)
        assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // send a bad request
        Throwable throwable = catchThrowable(() -> movementDataClient.getAccountSumDays(mdcBad).collectList().contextWrite(TestUtil.TEST_CONTEXT).block());
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class)
                  .hasMessage(String.format(ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR, endpointInfo.getServiceName()));
        HttpCallTechnicalException httpCallTechnicalException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallTechnicalException.getSource()).isEqualTo(SourceError.ACDS);
        assertThat(httpCallTechnicalException.getHttpStatusCode()).isEqualTo(testStatusCode);
        assertThat(httpCallTechnicalException.getSourceEndpoint().getValue()).hasToString(endpointInfo.getServiceName());

        // CLOSED -> OPEN
        assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        List<ILoggingEvent> closedToOpenLog = searchCircuitBreakerLog(generalCircuitBreaker, memoryAppender, CircuitBreaker.State.CLOSED, CircuitBreaker.State.OPEN);
        assertLogs(closedToOpenLog, Level.WARN);

        // send 1 bad requests, expect CallNotPermittedException exception
        Throwable catchThrowable = catchThrowable(() -> movementDataClient.getAccountSumDays(mdcBad).collectList().contextWrite(TestUtil.TEST_CONTEXT).block());
        assertThat(catchThrowable).isInstanceOf(CallNotPermittedException.class)
                  .hasMessage(String.format(CALL_NOT_PERMITTED_CIRCUIT_BREAKER_ERROR, generalCircuitBreaker.getName()));

        CallNotPermittedException callNotPermittedException = (CallNotPermittedException) catchThrowable;
        assertThat(callNotPermittedException.getCausingCircuitBreakerName()).isEqualTo(generalCircuitBreaker.getName());

        verifyNumberOfClientApiCalls(4);

        // OPEN -> HALF OPEN
        Awaitility.await().timeout(20L, TimeUnit.SECONDS)
                  .untilAsserted(() -> assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN));
        List<ILoggingEvent> openToHalfOpenLog = searchCircuitBreakerLog(generalCircuitBreaker, memoryAppender, CircuitBreaker.State.OPEN, CircuitBreaker.State.HALF_OPEN);
        assertLogs(openToHalfOpenLog, Level.WARN);

        // send 1 good request
        IntStream.range(0, 2).forEachOrdered(n -> movementDataClient.getAccountSumDays(mdc).collectList().contextWrite(TestUtil.TEST_CONTEXT).block());
        // HALF OPEN -> CLOSED
        assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        List<ILoggingEvent> halfOpenToClosedLog = searchCircuitBreakerLog(generalCircuitBreaker, memoryAppender, CircuitBreaker.State.HALF_OPEN, CircuitBreaker.State.CLOSED);
        assertLogs(halfOpenToClosedLog, Level.WARN);
    }

    @DisplayName("Test circuit breaker for unrecorded non retryable exceptions 555 400 404")
    @ParameterizedTest(name = "{index} => {0}, Status Code:{1}")
    @CsvSource(value = {
            ACCOUNT_SUM_DAYS_ENDPOINT + ";555;",
            ACCOUNT_SUM_DAYS_ENDPOINT + ";400",
            ACCOUNT_SUM_DAYS_ENDPOINT + ";404"
    }, delimiter = ';')
    void circuit_breaker_unrecorded_non_retryable_exceptions_555_400_404(String endpoint, int statusCode) {
        EndpointInfo endpointInfo = ENDPOINT_INFO_MAP.get(endpoint);
        StatusCodeErrorInfo statusCodeErrorInfo = STATUS_CODE_ERROR_INFO_MAP.get(statusCode);

        TestUtil.stubAcdsClientApi(endpoint, TEST_CLIENT, endpointInfo.getBodyUrl(), 200, wireMockServer);
        TestUtil.stubAcdsClientApi(endpoint, BAD_CLIENT, null, statusCode, wireMockServer);
        CircuitBreaker generalCircuitBreaker = circuitBreakerRegistry.circuitBreaker(endpointInfo.getCircuitBreakerName());
        generalCircuitBreaker.transitionToClosedState();


        MasterdataContext mdc = TestUtil.getMasterdataContext(TEST_CLIENT);
        MasterdataContext mdcBad = TestUtil.getMasterdataContext(BAD_CLIENT);

        // send 2 good requests
        IntStream.range(0, 2).forEachOrdered(n -> movementDataClient.getAccountSumDays(mdc).collectList().contextWrite(TestUtil.TEST_CONTEXT).block());

        // assert state not changed (CLOSED)
        assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // send many bad requests
        IntStream.range(0, slidingWindowSize).forEachOrdered(n -> {
            Throwable throwable = catchThrowable(() -> movementDataClient.getAccountSumDays(mdcBad).collectList().contextWrite(TestUtil.TEST_CONTEXT).block());
            assertThat(throwable).isInstanceOf(statusCodeErrorInfo.getHttpCallException())
                      .hasMessage(statusCodeErrorInfo.getProcessingErrorMessage(endpointInfo.getServiceName()));
            HttpCallException httpCallException = (HttpCallException) throwable;
            assertThat(httpCallException.getSource()).isEqualTo(statusCodeErrorInfo.getSourceError());
            assertThat(httpCallException.getHttpStatusCode()).isEqualTo(statusCode);
            assertThat(httpCallException.getSourceEndpoint().getValue()).hasToString(endpointInfo.getServiceName());
        });

        // assert state not changed (CLOSED)
        assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        verifyNumberOfClientApiCalls(slidingWindowSize+2);
    }


    @DisplayName("Test circuit breaker for acds statusCode 204 expect success")
    @ParameterizedTest(name = "{index} => {0}")
    @CsvSource(value = {
            ACCOUNT_SUM_DAYS_ENDPOINT,
            ACCOUNT_SUM_MONTHS_ENDPOINT
    }, delimiter = ';')
    void circuit_breaker_acds_statusCode_204_expect_success(String endpoint) {
        EndpointInfo endpointInfo = ENDPOINT_INFO_MAP.get(endpoint);
        final int testStatusCode = 204;

        TestUtil.stubAcdsClientApi(endpoint, TEST_CLIENT, endpointInfo.getBodyUrl(), 200, wireMockServer);
        TestUtil.stubAcdsClientApi(endpoint, BAD_CLIENT, null, testStatusCode, wireMockServer);
        CircuitBreaker generalCircuitBreaker = circuitBreakerRegistry.circuitBreaker(endpointInfo.getCircuitBreakerName());
        generalCircuitBreaker.transitionToClosedState();


        MasterdataContext mdc = TestUtil.getMasterdataContext(TEST_CLIENT);
        MasterdataContext mdcBad = TestUtil.getMasterdataContext(BAD_CLIENT);

        // send 2 good requests
        IntStream.range(0, 2).forEachOrdered(n -> movementDataClient.getAccountSumDays(mdc).collectList().contextWrite(TestUtil.TEST_CONTEXT).block());

        // assert state not changed (CLOSED)
        assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // send number of bad requests
        IntStream.range(0, 5).forEachOrdered(n -> movementDataClient.getAccountSumDays(mdcBad).collectList().contextWrite(TestUtil.TEST_CONTEXT).block());

        // assert state not changed (CLOSED)
        assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        verifyNumberOfClientApiCalls(7);
    }

    @Test
    @DisplayName("Test timeout for accountSumDays endpoint")
    void test_timeout_of_accountsumDay_endpoint_should_return_webClientRequestException() {
        AccountSumDaysApi accountSumDaysApiWithTimeout = new ClientEndpointConfiguration().accountSumDaysApi(sumDaysApiClient);
        movementDataClient =
                new MovementDataClient(accountSumDaysApiWithTimeout, movementdataInventoriesApi, circuitBreakerRegistry, retryRegistry);

        EndpointInfo endpointInfo = ENDPOINT_INFO_MAP.get(ACCOUNT_SUM_DAYS_ENDPOINT);

        TestUtil.stubAcdsClientApiWithDelay(ACCOUNT_SUM_DAYS_ENDPOINT, TEST_CLIENT, endpointInfo.getBodyUrl(), 200, wireMockServer, timeoutInMs * 2);

        MasterdataContext mdc = TestUtil.getMasterdataContext(TEST_CLIENT);
        Throwable throwable = catchThrowable(() -> movementDataClient.getAccountSumDays(mdc).collectList().contextWrite(TestUtil.TEST_CONTEXT).block());

        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class);
        HttpCallTechnicalException exception = (HttpCallTechnicalException) throwable;
        assertThat(exception.getSource()).isEqualTo(SourceError.ACDS);
        assertThat(exception.getSourceEndpoint()).isEqualTo(SourceEndpoint.ACCOUNT_SUM_DAYS);
        assertThat(exception.getHttpStatusCode()).isEqualTo(HttpStatus.REQUEST_TIMEOUT.value());
        assertThat(exception.getCause()).isInstanceOf(WebClientRequestException.class);
        assertThat(exception).hasMessage(ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR, SourceEndpoint.ACCOUNT_SUM_DAYS.getValue());
    }

    private void verifyNumberOfClientApiCalls(int wantedNumberOfInvocations) {
        verify(accountSumDaysApi, times(wantedNumberOfInvocations)).getAccountSumDays(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private static Map<String,EndpointInfo> getEndpointInfoMap() {
        Map<String,EndpointInfo> map = new HashMap<>();
        map.put(ACCOUNT_SUM_DAYS_ENDPOINT, new EndpointInfo("json/acds-responses/wiremock/movement-data/account-sum-days-2021.ndjson", MOVEMENT_DATA_CLIENT_CIRCUIT_BREAKER, "AccountSumDays"));
        map.put(ACCOUNT_SUM_MONTHS_ENDPOINT, new EndpointInfo("json/acds-responses/wiremock/movement-data/account-sum-months-resilience.ndjson", MOVEMENT_DATA_CLIENT_CIRCUIT_BREAKER, "AccountSumMonths"));
        return map;
    }
}