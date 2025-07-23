package de.datev.refsys.aggregation.processing.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.tomakehurst.wiremock.WireMockServer;
import de.datev.refsys.aggregation.document.model.ProblemInfo;
import de.datev.refsys.aggregation.processing.client.test_model.EndpointInfo;
import de.datev.refsys.aggregation.processing.client.test_model.StatusCodeErrorInfo;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.configuration.ClientEndpointConfiguration;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallException;
import de.datev.refsys.aggregation.processing.exception.HttpCallNoContentException;
import de.datev.refsys.aggregation.processing.exception.HttpCallTechnicalException;
import de.datev.refsys.aggregation.processing.mapper.ProblemInfoMapper;
import de.datev.refsys.aggregation.processing.mapper.ProblemInfoMapperImpl;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
import de.datev.refsys.aggregation.processing.util.CircuitBreakerUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.ResetResilienceAfterEachTest;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.aggregation.processing.util.TestUtil;
import de.datev.refsys.generated.acds.api.AccountCaptionsApi;
import de.datev.refsys.generated.acds.api.AccountPurposeMappingsApi;
import de.datev.refsys.generated.acds.api.CollectiveAccountsApi;
import de.datev.refsys.generated.acds.api.MasterdataContextApi;
import de.datev.refsys.generated.acds.api.MasterdataInventoriesApi;
import de.datev.refsys.generated.acds.api.ShareholderApi;
import de.datev.refsys.generated.acds.api.TranslationApi;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.generated.acds.api.model.ProblemDetails;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.COLLECTIVE_ACCOUNTS_MOVEMENT_INVENTORIES_TRANSLATION_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.MASTER_DATA_CONTEXT_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.PURPOSE_MAPPINGS_CAPTIONS_MASTER_INVENTORIES_SHAREHOLDER_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.util.TestUtil.CALL_NOT_PERMITTED_CIRCUIT_BREAKER_ERROR;
import static de.datev.refsys.aggregation.processing.util.TestUtil.EXCEPTION_MESSAGE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_BASE_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CORRELATION_ID;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.assertLogs;
import static de.datev.refsys.aggregation.processing.util.TestUtil.searchCircuitBreakerLog;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class, classes = {ClientEndpointConfiguration.class, TestResilienceConfiguration.class})
@ResetResilienceAfterEachTest
@ActiveProfiles(TestUtil.TEST_PROFILE)
class MasterDataClientTest {
    private static final int BAD_CLIENT = 1;
    private static final String MASTERDATA_CONTEXT_ENDPOINT = "masterdata-context";
    private static final String ACCOUNT_CAPTIONS_ENDPOINT= "account-captions";
    private static final String ACCOUNT_PURPOSE_MAPPINGS_ENDPOINT= "account-purpose-mappings";
    private static final String COLLECTIVE_ACCOUNTS_ENDPOINT= "collective-accounts";
    private static final String SHAREHOLDER_ENDPOINT= "shareholder";
    private static final String TRANSLATION_ENDPOINT= "translation";
    private static final Map<String, EndpointInfo> ENDPOINT_INFO_MAP = getEndpointInfoMap();
    private static final Map<Integer, StatusCodeErrorInfo> STATUS_CODE_ERROR_INFO_MAP = TestUtil.getStatusCodeErrorInfoMap();
    private static final ProblemInfoMapper PROBLEM_INFO_MAPPER = new ProblemInfoMapperImpl();

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private MasterdataInventoriesApi masterdataInventoriesApi;

    @MockitoSpyBean
    private MasterdataContextApi masterdataContextApi;

    @MockitoSpyBean
    private AccountCaptionsApi accountCaptionsApi;

    @MockitoSpyBean
    private ShareholderApi shareholderApi;

    @MockitoSpyBean
    private AccountPurposeMappingsApi accountPurposeMappingsApi;

    @MockitoSpyBean
    private CollectiveAccountsApi collectiveAccountsApi;

    @MockitoSpyBean
    private TranslationApi translationApi;

    @Value("${resilience4j.circuitbreaker.configs.shared-config.slidingWindowSize}")
    private Integer slidingWindowSize;

    private MasterDataClient masterDataClient;
    private MemoryAppender memoryAppender;

    @BeforeEach
    void setUp() {
        memoryAppender = setupMemoryAppender(memoryAppender, CircuitBreakerUtil.class, Level.WARN);

        masterDataClient = new MasterDataClient(masterdataContextApi,
                accountCaptionsApi,
                shareholderApi,
                accountPurposeMappingsApi,
                collectiveAccountsApi,
                translationApi,
                masterdataInventoriesApi,
                circuitBreakerRegistry, retryRegistry);
        wireMockServer.resetRequests();

    }

    @Test
    void should_throw_HttpCallBusinessException_exception_when_getMasterDataContext_return_no_content_response() {
        var statusCode = 204;
        TestUtil.stubACDSResponse(wireMockServer, statusCode, "masterdata-context", Map.of("fiscal-year", TestUtil.TEST_FISCAL_YEAR_2022_START,
                "base-version", TestUtil.TEST_BASE_VERSION, "delta-version", TestUtil.TEST_DELTA_VERSION), "");
        Throwable throwable = catchThrowable(() -> masterDataClient.getMasterDataContext(TestUtil.TEST_CONSULTANT, TestUtil.TEST_CLIENT, TestUtil.TEST_FISCAL_YEAR_2022_START,
                                                                                         TestUtil.TEST_BASE_VERSION, TestUtil.TEST_DELTA_VERSION, TestUtil.TEST_CORRELATION_ID).block());
        assertThat(throwable).isInstanceOf(HttpCallNoContentException.class)
                .hasMessage(String.format(ProcessingErrorMessageConstants.NO_MASTER_DATA_CONTEXT_IN_ACDS, SourceEndpoint.MASTER_DATA_CONTEXT.getValue()));
        HttpCallNoContentException httpCallNoContentException = (HttpCallNoContentException) throwable;
        assertThat(httpCallNoContentException.getSource()).isEqualTo(SourceError.ACDS);
        assertThat(httpCallNoContentException.getSourceEndpoint()).isEqualTo(SourceEndpoint.MASTER_DATA_CONTEXT);
        assertThat(httpCallNoContentException.getHttpStatusCode()).isEqualTo(statusCode);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "404;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "400;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "500;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR},
            delimiter = ';')
    void should_throw_HttpCallTechnical_exception_when_getMasterDataContext_return_invalid_response(int statusCode, String exceptionMessage) {
        TestUtil.stubACDSResponse(wireMockServer, statusCode, "masterdata-context", Map.of("fiscal-year", TestUtil.TEST_FISCAL_YEAR_2022_START,
                "base-version", TestUtil.TEST_BASE_VERSION, "delta-version", TestUtil.TEST_DELTA_VERSION), "");
        Throwable throwable = catchThrowable(() -> masterDataClient.getMasterDataContext(TestUtil.TEST_CONSULTANT, TestUtil.TEST_CLIENT, TestUtil.TEST_FISCAL_YEAR_2022_START,
                                                                                         TestUtil.TEST_BASE_VERSION, TestUtil.TEST_DELTA_VERSION, TestUtil.TEST_CORRELATION_ID).block());
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class)
                .hasMessage(String.format(exceptionMessage, SourceEndpoint.MASTER_DATA_CONTEXT.getValue()));
        HttpCallTechnicalException httpCallTechnicalException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallTechnicalException.getSource()).isEqualTo(SourceError.ACDS);
        assertThat(httpCallTechnicalException.getSourceEndpoint()).isEqualTo(SourceEndpoint.MASTER_DATA_CONTEXT);
        assertThat(httpCallTechnicalException.getHttpStatusCode()).isEqualTo(statusCode);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "404;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "400;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "500;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR},
            delimiter = ';')
    void should_throw_HttpCallTechnical_exception_when_getAccountCaptions_return_invalid_response(int statusCode, String exceptionMessage) {
        TestUtil.stubACDSResponse(wireMockServer, statusCode, "account-captions", Map.of("fiscal-year", TestUtil.TEST_FISCAL_YEAR_2022_START,
                "base-version", TestUtil.TEST_BASE_VERSION, "delta-version", TestUtil.TEST_DELTA_VERSION,
                "account-system", TestUtil.TEST_ACCOUNT_SYSTEM, "industry-id", TestUtil.TEST_INDUSTRY_NO, "use-organisation-account-caption", false), "");
        Throwable throwable = catchThrowable(() -> masterDataClient.getAccountCaptions(TestUtil.createTestMasterdataContext()).contextWrite(TestUtil.TEST_CONTEXT).block());
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class)
                .hasMessage(String.format(exceptionMessage, SourceEndpoint.ACCOUNT_CAPTIONS.getValue()));
        HttpCallTechnicalException httpCallTechnicalException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallTechnicalException.getSource()).isEqualTo(SourceError.ACDS);
        assertThat(httpCallTechnicalException.getSourceEndpoint()).isEqualTo(SourceEndpoint.ACCOUNT_CAPTIONS);
        assertThat(httpCallTechnicalException.getHttpStatusCode()).isEqualTo(statusCode);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "404;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "400;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "500;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR},
            delimiter = ';')
    void should_throw_HttpCallTechnical_exception_when_getAccountPurposeMappings_return_invalid_response(int statusCode, String exceptionMessage) {
        TestUtil.stubACDSResponse(wireMockServer, statusCode, "account-purpose-mappings", Map.of("fiscal-year", TestUtil.TEST_FISCAL_YEAR_2022_START,
                "base-version", TestUtil.TEST_BASE_VERSION, "delta-version", TestUtil.TEST_DELTA_VERSION, "account-system", TestUtil.TEST_ACCOUNT_SYSTEM,
                "industry-id", TestUtil.TEST_INDUSTRY_NO), "");
        Throwable throwable = catchThrowable(() -> masterDataClient.getAccountPurposeMappings(TestUtil.createTestMasterdataContext()).contextWrite(TestUtil.TEST_CONTEXT).block());
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class)
                .hasMessage(String.format(exceptionMessage, SourceEndpoint.ACCOUNT_PURPOSE_MAPPINGS.getValue()));
        HttpCallTechnicalException httpCallTechnicalException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallTechnicalException.getSource()).isEqualTo(SourceError.ACDS);
        assertThat(httpCallTechnicalException.getSourceEndpoint()).isEqualTo(SourceEndpoint.ACCOUNT_PURPOSE_MAPPINGS);
        assertThat(httpCallTechnicalException.getHttpStatusCode()).isEqualTo(statusCode);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "404;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "400;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "500;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR},
            delimiter = ';')
    void should_throw_HttpCallTechnical_exception_when_getCollectiveAccounts_return_invalid_response(int statusCode, String exceptionMessage) {
        TestUtil.stubACDSResponse(wireMockServer, statusCode, "collective-accounts", Map.of("fiscal-year", TestUtil.TEST_FISCAL_YEAR_2022_START,
                "base-version", TestUtil.TEST_BASE_VERSION, "delta-version", TestUtil.TEST_DELTA_VERSION, "account-system", TestUtil.TEST_ACCOUNT_SYSTEM,
                "industry-id", TestUtil.TEST_INDUSTRY_NO, "account-length", TestUtil.TEST_ACCOUNT_LENGTH, "use-consultant-accounting-functions", false,
                "use-client-accounting-functions", false, "use-skr-following-year", false), "");
        Throwable throwable = catchThrowable(() -> masterDataClient.getCollectiveAccounts(TestUtil.createTestMasterdataContext()).contextWrite(TestUtil.TEST_CONTEXT).block());
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class)
                .hasMessage(String.format(exceptionMessage, SourceEndpoint.COLLECTIVE_ACCOUNTS.getValue()));
        HttpCallTechnicalException httpCallTechnicalException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallTechnicalException.getSource()).isEqualTo(SourceError.ACDS);
        assertThat(httpCallTechnicalException.getSourceEndpoint()).isEqualTo(SourceEndpoint.COLLECTIVE_ACCOUNTS);
        assertThat(httpCallTechnicalException.getHttpStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void should_throw_HttpCallBusiness_exception_when_getCollectiveAccounts_return_555_response() {
        TestUtil.stubACDSResponse(wireMockServer, 555, "collective-accounts", Map.of("fiscal-year", TestUtil.TEST_FISCAL_YEAR_2022_START,
                "base-version", TestUtil.TEST_BASE_VERSION, "delta-version", TestUtil.TEST_DELTA_VERSION, "account-system", TestUtil.TEST_ACCOUNT_SYSTEM,
                "industry-id", TestUtil.TEST_INDUSTRY_NO, "account-length", TestUtil.TEST_ACCOUNT_LENGTH, "use-consultant-accounting-functions", false,
                "use-client-accounting-functions", false, "use-skr-following-year", false), TestUtil.loadResourceAsString("json/acds-responses/wiremock/error-response/555-response.json"));
        Throwable throwable = catchThrowable(() -> masterDataClient.getCollectiveAccounts(TestUtil.createTestMasterdataContext()).contextWrite(TestUtil.TEST_CONTEXT).block());
        assertThat(throwable).isInstanceOf(HttpCallBusinessException.class)
                .hasMessage(String.format(ProcessingErrorMessageConstants.ACDS_BUSINESS_ERROR, SourceEndpoint.COLLECTIVE_ACCOUNTS.getValue()));
        HttpCallBusinessException httpCallBusinessException = (HttpCallBusinessException) throwable;
        assertThat(httpCallBusinessException.getSource()).isEqualTo(SourceError.ACDS);
        assertThat(httpCallBusinessException.getSourceEndpoint()).isEqualTo(SourceEndpoint.COLLECTIVE_ACCOUNTS);
        assertThat(httpCallBusinessException.getHttpStatusCode()).isEqualTo(555);

        ProblemDetails problemDetails = TestDataLoader.load("json/acds-responses/wiremock/error-response/555-response.json", ProblemDetails.class);
        ProblemInfo expectedProblemInfo = PROBLEM_INFO_MAPPER.problemDetailsToProblemInfo(problemDetails);
        assertThat(httpCallBusinessException.getProblemInfo()).usingRecursiveComparison().isEqualTo(expectedProblemInfo);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "404;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "400;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "500;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR},
            delimiter = ';')
    void should_throw_HttpCallTechnical_exception_when_getShareholderData_return_invalid_response(int statusCode, String exceptionMessage) {
        TestUtil.stubACDSResponse(wireMockServer, statusCode, "shareholder", Map.of("fiscal-year", TestUtil.TEST_FISCAL_YEAR_2022_START,
                "base-version", TestUtil.TEST_BASE_VERSION, "delta-version", TestUtil.TEST_DELTA_VERSION), "");
        Throwable throwable = catchThrowable(() -> masterDataClient.getShareholderData(TestUtil.createTestMasterdataContext()).contextWrite(TestUtil.TEST_CONTEXT).block());
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class)
                .hasMessage(String.format(exceptionMessage, SourceEndpoint.SHAREHOLDER.getValue()));
        HttpCallTechnicalException httpCallTechnicalException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallTechnicalException.getSource()).isEqualTo(SourceError.ACDS);
        assertThat(httpCallTechnicalException.getSourceEndpoint()).isEqualTo(SourceEndpoint.SHAREHOLDER);
        assertThat(httpCallTechnicalException.getHttpStatusCode()).isEqualTo(statusCode);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "404;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "400;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "500;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR},
            delimiter = ';')
    void should_throw_HttpCallTechnical_exception_when_getTranslation_return_invalid_response(int statusCode, String exceptionMessage) {
        TestUtil.stubACDSResponse(wireMockServer, statusCode, "translation", Map.of("fiscal-year", TestUtil.TEST_FISCAL_YEAR_2022_START,
                "base-version", TestUtil.TEST_BASE_VERSION, "delta-version", TestUtil.TEST_DELTA_VERSION, "use-previous-year-account-translation", false, "use-alternative-account-translation", false), "");
        Throwable throwable = catchThrowable(() -> masterDataClient.getTranslation(TestUtil.createTestMasterdataContext()).contextWrite(TestUtil.TEST_CONTEXT).block());
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class)
                .hasMessage(String.format(exceptionMessage, SourceEndpoint.TRANSLATION.getValue(), TestUtil.TEST_FISCAL_YEAR_2022_START));
        HttpCallTechnicalException httpCallTechnicalException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallTechnicalException.getSource()).isEqualTo(SourceError.ACDS);
        assertThat(httpCallTechnicalException.getSourceEndpoint()).isEqualTo(SourceEndpoint.TRANSLATION);
        assertThat(httpCallTechnicalException.getHttpStatusCode()).isEqualTo(statusCode);
    }

    @DisplayName("Test circuit breaker for recorded and retryable exception 500")
    @ParameterizedTest(name = "{index} => {0}")
    @CsvSource(value = {
            // MASTERDATA_CONTEXT_ENDPOINT,  TODO: masterDataContextCircuitBreaker anschalten
            ACCOUNT_CAPTIONS_ENDPOINT,
            ACCOUNT_PURPOSE_MAPPINGS_ENDPOINT,
            COLLECTIVE_ACCOUNTS_ENDPOINT,
            SHAREHOLDER_ENDPOINT,
            TRANSLATION_ENDPOINT,
    })
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
        IntStream.range(0, 2).forEachOrdered(n -> executeApiCall(mdc, endpoint));

        // assert state not changed (CLOSED)
        assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // send a bad request
        Throwable throwable = catchThrowable(() -> executeApiCall(mdcBad, endpoint));
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
        Throwable catchThrowable = catchThrowable(() -> executeApiCall(mdcBad, endpoint));
        assertThat(catchThrowable).isInstanceOf(CallNotPermittedException.class)
                  .hasMessage(String.format(CALL_NOT_PERMITTED_CIRCUIT_BREAKER_ERROR, generalCircuitBreaker.getName()));

        CallNotPermittedException callNotPermittedException = (CallNotPermittedException) catchThrowable;
        assertThat(callNotPermittedException.getCausingCircuitBreakerName()).isEqualTo(generalCircuitBreaker.getName());

        verifyNumberOfClientApiCalls(4, endpoint);

        // OPEN -> HALF OPEN
        Awaitility.await().timeout(20L, TimeUnit.SECONDS)
                  .untilAsserted(() -> assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN));
        List<ILoggingEvent> openToHalfOpenLog = searchCircuitBreakerLog(generalCircuitBreaker, memoryAppender, CircuitBreaker.State.OPEN, CircuitBreaker.State.HALF_OPEN);
        assertLogs(openToHalfOpenLog, Level.WARN);

        // send 1 good request
        IntStream.range(0, 2).forEachOrdered(n -> executeApiCall(mdc, endpoint));
        // HALF OPEN -> CLOSED
        assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        List<ILoggingEvent> halfOpenToClosedLog = searchCircuitBreakerLog(generalCircuitBreaker, memoryAppender, CircuitBreaker.State.HALF_OPEN, CircuitBreaker.State.CLOSED);
        assertLogs(halfOpenToClosedLog, Level.WARN);
    }

    @DisplayName("Test circuit breaker for unrecorded non retryable exceptions 555 400 404")
    @ParameterizedTest()
    @CsvSource(value = {
            MASTERDATA_CONTEXT_ENDPOINT + ";555;",
            MASTERDATA_CONTEXT_ENDPOINT + ";400",
            MASTERDATA_CONTEXT_ENDPOINT + ";404",
            ACCOUNT_CAPTIONS_ENDPOINT + ";555",
            ACCOUNT_CAPTIONS_ENDPOINT + ";400",
            ACCOUNT_CAPTIONS_ENDPOINT + ";404",
            ACCOUNT_PURPOSE_MAPPINGS_ENDPOINT + ";555",
            ACCOUNT_PURPOSE_MAPPINGS_ENDPOINT + ";400",
            ACCOUNT_PURPOSE_MAPPINGS_ENDPOINT + ";404",
            COLLECTIVE_ACCOUNTS_ENDPOINT + ";555",
            COLLECTIVE_ACCOUNTS_ENDPOINT + ";400",
            COLLECTIVE_ACCOUNTS_ENDPOINT + ";404",
            SHAREHOLDER_ENDPOINT + ";555",
            SHAREHOLDER_ENDPOINT + ";400",
            SHAREHOLDER_ENDPOINT + ";404",
            TRANSLATION_ENDPOINT + ";555",
            TRANSLATION_ENDPOINT + ";400",
            TRANSLATION_ENDPOINT + ";404",
    }, delimiter = ';')
    void circuit_breaker_unrecorded_non_retryable_exceptions_555_400_404_204(String endpoint, int statusCode) {
        EndpointInfo endpointInfo = ENDPOINT_INFO_MAP.get(endpoint);
        StatusCodeErrorInfo statusCodeErrorInfo = STATUS_CODE_ERROR_INFO_MAP.get(statusCode);

        TestUtil.stubAcdsClientApi(endpoint, TEST_CLIENT, endpointInfo.getBodyUrl(), 200, wireMockServer);
        TestUtil.stubAcdsClientApi(endpoint, BAD_CLIENT, null, statusCode, wireMockServer);
        CircuitBreaker generalCircuitBreaker = circuitBreakerRegistry.circuitBreaker(endpointInfo.getCircuitBreakerName());
        generalCircuitBreaker.transitionToClosedState();


        MasterdataContext mdc = TestUtil.getMasterdataContext(TEST_CLIENT);
        MasterdataContext mdcBad = TestUtil.getMasterdataContext(BAD_CLIENT);

        // send 2 good requests
        IntStream.range(0, 2).forEachOrdered(n -> executeApiCall(mdc, endpoint));

        // assert state not changed (CLOSED)
        assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // send many bad requests
        IntStream.range(0, slidingWindowSize).forEachOrdered(n -> {
            Throwable throwable = catchThrowable(() -> executeApiCall(mdcBad, endpoint));
            assertThat(throwable).isInstanceOf(statusCodeErrorInfo.getHttpCallException())
                      .hasMessage(statusCodeErrorInfo.getProcessingErrorMessage(endpointInfo.getServiceName()));
            HttpCallException httpCallException = (HttpCallException) throwable;
            assertThat(httpCallException.getSource()).isEqualTo(statusCodeErrorInfo.getSourceError());
            assertThat(httpCallException.getHttpStatusCode()).isEqualTo(statusCode);
            assertThat(httpCallException.getSourceEndpoint().getValue()).hasToString(endpointInfo.getServiceName());
        });

        // assert state not changed (CLOSED)
        assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        verifyNumberOfClientApiCalls(slidingWindowSize+2, endpoint);
    }


    @DisplayName("Test circuit breaker for acds statusCode 204 expect success")
    @ParameterizedTest(name = "{index} => {0}")
    @CsvSource(value = {
            ACCOUNT_CAPTIONS_ENDPOINT,
            ACCOUNT_PURPOSE_MAPPINGS_ENDPOINT,
            COLLECTIVE_ACCOUNTS_ENDPOINT,
            SHAREHOLDER_ENDPOINT,
            TRANSLATION_ENDPOINT,
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
        IntStream.range(0, 2).forEachOrdered(n -> executeApiCall(mdc, endpoint));

        // assert state not changed (CLOSED)
        assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // send number of bad requests
        IntStream.range(0, 5).forEachOrdered(n -> executeApiCall(mdcBad, endpoint));

        // assert state not changed (CLOSED)
        assertThat(generalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        verifyNumberOfClientApiCalls(7, endpoint);
    }

    @Test
    @DisplayName("Returns a business error when masterDataContext api has an unexpected error")
    void should_return_a_business_error_when_master_data_context_has_an_unexpected_error() {
        // prepare
        RuntimeException runtimeException = new RuntimeException(EXCEPTION_MESSAGE);
        when(masterdataContextApi.getMasterDataContext(anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any(), any(), any())).thenReturn(
                Mono.error(runtimeException));
        // execute
        Throwable throwable = catchThrowable(
                () -> masterDataClient.getMasterDataContext(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                            TEST_DELTA_VERSION, TEST_CORRELATION_ID).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(AggregationProcessingBusinessException.class);
        AggregationProcessingBusinessException businessException = (AggregationProcessingBusinessException) throwable;
        assertThat(businessException.getMessage()).isEqualTo(
                String.format(ProcessingErrorMessageConstants.UNEXPECTED_CLIENT_ERROR, SourceEndpoint.MASTER_DATA_CONTEXT.getValue()));
        assertThat(businessException.getHttpStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(businessException.getCause()).isNotNull().isInstanceOf(RuntimeException.class);
        RuntimeException expectedRuntimeException = (RuntimeException) businessException.getCause();
        assertThat(expectedRuntimeException).usingRecursiveAssertion().isEqualTo(runtimeException);
    }

    private void executeApiCall(MasterdataContext mdc, String endpoint) {
        switch (endpoint) {
        case MASTERDATA_CONTEXT_ENDPOINT -> masterDataClient.getMasterDataContext(mdc.getConsultant(), mdc.getClient(), TEST_FISCAL_YEAR_2021_START,
                                                                          TEST_BASE_VERSION, TEST_DELTA_VERSION, TEST_CORRELATION_ID)
                                                    .contextWrite(TestUtil.TEST_CONTEXT).block();
        case ACCOUNT_CAPTIONS_ENDPOINT -> masterDataClient.getAccountCaptions(mdc).contextWrite(TestUtil.TEST_CONTEXT).block();
        case ACCOUNT_PURPOSE_MAPPINGS_ENDPOINT -> masterDataClient.getAccountPurposeMappings(mdc).contextWrite(TestUtil.TEST_CONTEXT).block();
        case COLLECTIVE_ACCOUNTS_ENDPOINT -> masterDataClient.getCollectiveAccounts(mdc).contextWrite(TestUtil.TEST_CONTEXT).block();
        case SHAREHOLDER_ENDPOINT -> masterDataClient.getShareholderData(mdc).contextWrite(TestUtil.TEST_CONTEXT).block();
        case TRANSLATION_ENDPOINT -> masterDataClient.getTranslation(mdc).contextWrite(TestUtil.TEST_CONTEXT).block();
        default -> {
            break;
        }
        }
    }

    private void verifyNumberOfClientApiCalls(int wantedNumberOfInvocations, String endpoint) {
        switch (endpoint) {
        case MASTERDATA_CONTEXT_ENDPOINT -> verify(masterdataContextApi, times(wantedNumberOfInvocations))
                .getMasterDataContext(any(), any(), any(), any(), any(), any(), any(), any());
        case ACCOUNT_CAPTIONS_ENDPOINT -> verify(accountCaptionsApi, times(wantedNumberOfInvocations))
                .getAccountCaptions(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        case ACCOUNT_PURPOSE_MAPPINGS_ENDPOINT -> verify(accountPurposeMappingsApi, times(wantedNumberOfInvocations))
                .getAccountPurposeMappings(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        case COLLECTIVE_ACCOUNTS_ENDPOINT -> verify(collectiveAccountsApi, times(wantedNumberOfInvocations))
                .getCollectiveAccounts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        case SHAREHOLDER_ENDPOINT -> verify(shareholderApi, times(wantedNumberOfInvocations))
                .getShareholders(any(), any(), any(), any(), any(), any(), any(), any());
        case TRANSLATION_ENDPOINT -> verify(translationApi, times(wantedNumberOfInvocations))
                .getTranslation(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        default -> {
            break;
        }
        }
    }

    private static Map<String,EndpointInfo> getEndpointInfoMap() {
        Map<String,EndpointInfo> map = new HashMap<>();
        map.put(MASTERDATA_CONTEXT_ENDPOINT, new EndpointInfo("json/acds-responses/wiremock/master-data-context-2021.json", MASTER_DATA_CONTEXT_CIRCUIT_BREAKER, "MasterDataContext"));
        map.put(ACCOUNT_CAPTIONS_ENDPOINT, new EndpointInfo("json/acds-responses/wiremock/account-captions.ndjson",
                                                            PURPOSE_MAPPINGS_CAPTIONS_MASTER_INVENTORIES_SHAREHOLDER_CIRCUIT_BREAKER, "AccountCaptions"));
        map.put(ACCOUNT_PURPOSE_MAPPINGS_ENDPOINT, new EndpointInfo("json/acds-responses/wiremock/account-purpose-mappings.ndjson",
                                                                    PURPOSE_MAPPINGS_CAPTIONS_MASTER_INVENTORIES_SHAREHOLDER_CIRCUIT_BREAKER, "AccountPurposeMappings"));
        map.put(COLLECTIVE_ACCOUNTS_ENDPOINT, new EndpointInfo("json/acds-responses/wiremock/master-data/collective-accounts.ndjson",
                                                               COLLECTIVE_ACCOUNTS_MOVEMENT_INVENTORIES_TRANSLATION_CIRCUIT_BREAKER, "CollectiveAccounts"));
        map.put(SHAREHOLDER_ENDPOINT, new EndpointInfo("json/acds-responses/wiremock/shareholder-2021.json",
                                                       PURPOSE_MAPPINGS_CAPTIONS_MASTER_INVENTORIES_SHAREHOLDER_CIRCUIT_BREAKER, "Shareholder"));
        map.put(TRANSLATION_ENDPOINT, new EndpointInfo("json/acds-responses/wiremock/translation.json",
                                                       COLLECTIVE_ACCOUNTS_MOVEMENT_INVENTORIES_TRANSLATION_CIRCUIT_BREAKER, "Translation"));
        return map;
    }
}