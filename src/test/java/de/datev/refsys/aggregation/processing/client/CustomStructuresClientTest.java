package de.datev.refsys.aggregation.processing.client;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.WireMockServer;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.configuration.ClientEndpointConfiguration;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.exception.HttpCallTechnicalException;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.ResetResilienceAfterEachTest;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.aggregation.processing.util.TestUtil;
import de.datev.refsys.generated.acds.api.CustomColumnStructuresApi;
import de.datev.refsys.generated.acds.api.CustomReportStructuresApi;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;

import static de.datev.refsys.aggregation.processing.util.LoggingUtil.ACDS_GET_CUSTOM_COLUMN_STRUCTURES_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.ACDS_GET_CUSTOM_REPORT_STRUCTURES_LOG;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class, classes = { ClientEndpointConfiguration.class, TestResilienceConfiguration.class})
@ResetResilienceAfterEachTest
@ActiveProfiles(TestUtil.TEST_PROFILE)
class CustomStructuresClientTest {

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private WireMockServer wireMockServer;

    @MockitoSpyBean
    private CustomColumnStructuresApi customColumnStructuresApi;

    @MockitoSpyBean
    private CustomReportStructuresApi customReportStructuresApi;

    private MemoryAppender memoryAppender;

    @BeforeEach
    void setUp() {
        memoryAppender = setupMemoryAppender(memoryAppender, LoggingUtil.class, Level.DEBUG);
        wireMockServer.resetRequests();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "404;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "400;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "500;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR},
            delimiter = ';')
    void should_throw_http_call_technical_exception_when_get_custom_column_structures_list_returns_invalid_response(int statusCode, String exceptionMessage) {
        CustomStructuresClient customStructuresClient =
                new CustomStructuresClient(customReportStructuresApi, customColumnStructuresApi, true, retryRegistry);
        TestUtil.stubACDSResponse(wireMockServer, statusCode, "custom-column-structures",
                                  Map.of("fiscal-year", TestUtil.TEST_FISCAL_YEAR_2021_START, "base-version", TestUtil.TEST_BASE_VERSION_6,
                                         "delta-version", TestUtil.TEST_DELTA_VERSION, "read-organisation-custom-data", true, "near-time-data", true),
                                  "");
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        Throwable throwable = catchThrowable(
                () -> customStructuresClient.getCustomColumnStructuresList(masterdataContext).contextWrite(TestUtil.TEST_CONTEXT).block());
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class)
                             .hasMessage(String.format(exceptionMessage, SourceEndpoint.CUSTOM_COLUMN_STRUCTURES.getValue()));
        HttpCallTechnicalException httpCallTechnicalException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallTechnicalException.getSource()).isEqualTo(SourceError.ACDS);
        assertThat(httpCallTechnicalException.getSourceEndpoint()).isEqualTo(SourceEndpoint.CUSTOM_COLUMN_STRUCTURES);
        assertThat(httpCallTechnicalException.getHttpStatusCode()).isEqualTo(statusCode);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "404;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "400;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR,
            "500;" + ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR},
            delimiter = ';')
    void should_throw_http_call_technical_exception_when_get_custom_report_structures_list_returns_invalid_response(int statusCode, String exceptionMessage) {
        CustomStructuresClient customStructuresClient =
                new CustomStructuresClient(customReportStructuresApi, customColumnStructuresApi, true, retryRegistry);
        TestUtil.stubACDSResponse(wireMockServer, statusCode, "custom-report-structures",
                                  Map.of("fiscal-year", TestUtil.TEST_FISCAL_YEAR_2021_START, "base-version", TestUtil.TEST_BASE_VERSION_6,
                                         "delta-version", TestUtil.TEST_DELTA_VERSION, "read-organisation-custom-data", true, "near-time-data", true),
                                  "");
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        Throwable throwable = catchThrowable(
                () -> customStructuresClient.getCustomReportStructuresList(masterdataContext).contextWrite(TestUtil.TEST_CONTEXT).block());
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class)
                             .hasMessage(String.format(exceptionMessage, SourceEndpoint.CUSTOM_REPORT_STRUCTURES.getValue()));
        HttpCallTechnicalException httpCallTechnicalException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallTechnicalException.getSource()).isEqualTo(SourceError.ACDS);
        assertThat(httpCallTechnicalException.getSourceEndpoint()).isEqualTo(SourceEndpoint.CUSTOM_REPORT_STRUCTURES);
        assertThat(httpCallTechnicalException.getHttpStatusCode()).isEqualTo(statusCode);
    }

    @Test
    @DisplayName("Should return empty CustomColumnStructures response when iasd flag is disabled")
    void should_return_empty_custom_column_structures_when_iasd_flag_is_disabled() {
        CustomStructuresClient customStructuresClient =
                new CustomStructuresClient(customReportStructuresApi, customColumnStructuresApi, false, retryRegistry);
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        List<CustomColumnStructure> customColumnStructures = customStructuresClient.getCustomColumnStructuresList(masterdataContext).block();
        assertThat(customColumnStructures).isEmpty();
        assertThat(memoryAppender.search(ACDS_GET_CUSTOM_COLUMN_STRUCTURES_LOG.replace("{}ms", ""))).isEmpty();
    }

    @Test
    @DisplayName("Should return empty CustomReportStructures response when iasd flag is disabled")
    void should_return_empty_custom_report_structures_when_iasd_flag_is_disabled() {
        CustomStructuresClient customStructuresClient =
                new CustomStructuresClient(customReportStructuresApi, customColumnStructuresApi, false, retryRegistry);
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        List<CustomReportStructure> customReportStructures = customStructuresClient.getCustomReportStructuresList(masterdataContext).block();
        assertThat(customReportStructures).isEmpty();
        assertThat(memoryAppender.search(ACDS_GET_CUSTOM_REPORT_STRUCTURES_LOG.replace("{}ms", ""))).isEmpty();
    }
}