package de.datev.refsys.aggregation.processing.config.filter;

import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.TestUtil;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggingContextFilterTest {

    public static final String AGGREGATION_PROCESSING_TEMPLATE = "/aggregation-processing/consultants/{consultant}/clients/{client}/";
    private static Paths paths;
    private static final String OPENAPI_YAML_NAME = "openapi.yaml";

    @BeforeAll
    static void setUp() {
        SwaggerParseResult result = new OpenAPIParser().readLocation("src/main/resources/"+OPENAPI_YAML_NAME, null, null);
        OpenAPI api = result.getOpenAPI();
        paths = api.getPaths();
    }

    @Test
    @DisplayName("Check Resources for yamls")
    void check_if_multiple_openapi_yamls_exist() {
        File directory = new File("src/main/resources/");
        assertThat(directory).exists();
        File[] files = directory.listFiles();
        assertThat(files).isNotNull();
        List<File> yamlFiles = Arrays.stream(files).filter(fileToCheck -> fileToCheck.getName().endsWith(".yaml")).collect(Collectors.toList());
        assertThat(yamlFiles).hasSize(1);
        assertThat(yamlFiles.get(0)).hasFileName(OPENAPI_YAML_NAME);
    }

    @Test
    @DisplayName("path and query parameters should be put into the context")
    void should_put_path_and_query_params_into_context() {
        checkOpenApiPaths(paths);

        LoggingContextFilter loggingContextFilter = new LoggingContextFilter();
        WebFilterChain filterChain = filterExchange -> Mono.empty();
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

        queryParams.put("fiscal-year", List.of(TestUtil.TEST_FISCAL_YEAR_2020_START.toString()));
        queryParams.put("base-version", List.of(TestUtil.TEST_BASE_VERSION_UPDATE.toString()));
        queryParams.put("delta-version", List.of(TestUtil.TEST_DELTA_VERSION_UPDATE.toString()));

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.put("x-correlation-id", List.of(TestUtil.TEST_CORRELATION_ID));
        headers.put("X-Datev-Client-ID", List.of(TestUtil.TEST_DATEV_CLIENT_ID));
        String requestId = "1";
        headers.put("Request-Id", List.of(requestId));

        paths.keySet().forEach(pathItem -> {
            String url = pathItem.replace("{consultant}", TestUtil.TEST_CONSULTANT.toString()).replace("{client}", TestUtil.TEST_CLIENT.toString());
            MockServerWebExchange from = MockServerWebExchange.from(
                    MockServerHttpRequest.post(url).contentType(MediaType.APPLICATION_JSON).queryParams(queryParams).headers(headers));

            StepVerifier.create(loggingContextFilter.filter(from, filterChain)).expectAccessibleContext().assertThat(context -> {
                Optional<HashMap<String, String>> optionalLoggingContext = context.getOrEmpty(LoggingUtil.LOGGING_CONTEXT_KEY);
                assertThat(optionalLoggingContext).isPresent();
                HashMap<String, String> loggingContext = optionalLoggingContext.get();
                assertThat(loggingContext).containsEntry(LoggingUtil.CONSULTANT_KEY, TestUtil.TEST_CONSULTANT.toString());
                assertThat(loggingContext).containsEntry(LoggingUtil.CLIENT_KEY, TestUtil.TEST_CLIENT.toString());
                assertThat(loggingContext).containsEntry(LoggingUtil.FISCAL_YEAR_KEY, TestUtil.TEST_FISCAL_YEAR_2020_START.toString());
                assertThat(loggingContext).containsEntry(LoggingUtil.BASE_VERSION_KEY, TestUtil.TEST_BASE_VERSION_UPDATE.toString());
                assertThat(loggingContext).containsEntry(LoggingUtil.DELTA_VERSION_KEY, TestUtil.TEST_DELTA_VERSION_UPDATE.toString());
                assertThat(loggingContext).containsEntry(LoggingUtil.CORRELATION_ID_KEY, TestUtil.TEST_CORRELATION_ID);

                assertThat(loggingContext).containsEntry(LoggingUtil.CORRELATION_ID_KEY, TestUtil.TEST_CORRELATION_ID);
                assertThat(loggingContext).containsEntry(LoggingUtil.DATEV_CLIENT_ID_KEY, TestUtil.TEST_DATEV_CLIENT_ID);
                assertThat(loggingContext).containsEntry(LoggingUtil.REQUEST_ID_KEY, requestId);
            }).then().verifyComplete();
        });
    }

    @Test
    @DisplayName("base and delta version should be set to 0 if empty")
    void should_put_base_and_delta_version_as_0_if_empty() {
        checkOpenApiPaths(paths);

        LoggingContextFilter loggingContextFilter = new LoggingContextFilter();
        WebFilterChain filterChain = filterExchange -> Mono.empty();
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

        queryParams.put("fiscal-year", List.of(TestUtil.TEST_FISCAL_YEAR_2020_START.toString()));

        paths.keySet().forEach(pathItem -> {
            String url = pathItem.replace("{consultant}", TestUtil.TEST_CONSULTANT.toString()).replace("{client}", TestUtil.TEST_CLIENT.toString());
            MockServerWebExchange from =
                    MockServerWebExchange.from(MockServerHttpRequest.post(url).contentType(MediaType.APPLICATION_JSON).queryParams(queryParams));

            StepVerifier.create(loggingContextFilter.filter(from, filterChain)).expectAccessibleContext().assertThat(context -> {
                Optional<HashMap<String, String>> optionalLoggingContext = context.getOrEmpty(LoggingUtil.LOGGING_CONTEXT_KEY);
                assertThat(optionalLoggingContext).isPresent();
                HashMap<String, String> loggingContext = optionalLoggingContext.get();
                assertThat(loggingContext).containsEntry(LoggingUtil.CONSULTANT_KEY, TestUtil.TEST_CONSULTANT.toString());
                assertThat(loggingContext).containsEntry(LoggingUtil.CLIENT_KEY, TestUtil.TEST_CLIENT.toString());
                assertThat(loggingContext).containsEntry(LoggingUtil.FISCAL_YEAR_KEY, TestUtil.TEST_FISCAL_YEAR_2020_START.toString());
                assertThat(loggingContext).containsEntry(LoggingUtil.BASE_VERSION_KEY, LoggingContextFilter.DEFAULT_BASE_DELTA_VERSION);
                assertThat(loggingContext).containsEntry(LoggingUtil.DELTA_VERSION_KEY, LoggingContextFilter.DEFAULT_BASE_DELTA_VERSION);
                assertThat(loggingContext.get(LoggingUtil.CORRELATION_ID_KEY)).isNotNull();
            }).then().verifyComplete();
        });
    }

    private void checkOpenApiPaths(Paths paths) {
        assertThat(paths.keySet()).hasSize(4);
        PathItem pathInitialLoad = paths.get(AGGREGATION_PROCESSING_TEMPLATE + "initial-load");
        assertThat(pathInitialLoad).isNotNull();
        assertThat(pathInitialLoad.getPost().getParameters()).hasSize(7);
        List<String> postInitialLoadParams = pathInitialLoad.getPost().getParameters().stream().map(Parameter::getName).toList();
        assertParams(postInitialLoadParams);
        assertThat(pathInitialLoad.getDelete().getParameters()).hasSize(5);
        List<String> deleteInitialLoadParams = pathInitialLoad.getDelete().getParameters().stream().map(Parameter::getName).toList();
        assertCommonParams(deleteInitialLoadParams);

        PathItem pathProcessDelta = paths.get(AGGREGATION_PROCESSING_TEMPLATE + "process-delta");
        assertThat(pathProcessDelta).isNotNull();
        assertThat(pathProcessDelta.getPost().getParameters()).hasSize(7);
        List<String> postProcessDeltaSchemaParams = pathProcessDelta.getPost().getParameters().stream().map(Parameter::getName).toList();
        assertParams(postProcessDeltaSchemaParams);

        PathItem pathUpdateSchema = paths.get(AGGREGATION_PROCESSING_TEMPLATE + "update-schema");
        assertThat(pathUpdateSchema).isNotNull();
        assertThat(pathUpdateSchema.getPost().getParameters()).hasSize(7);
        List<String> postUpdateSchemaParams = pathUpdateSchema.getPost().getParameters().stream().map(Parameter::getName).toList();
        assertParams(postUpdateSchemaParams);

        PathItem pathUpdateVersion = paths.get(AGGREGATION_PROCESSING_TEMPLATE + "update-version");
        assertThat(pathUpdateVersion).isNotNull();
        assertThat(pathUpdateVersion.getPost().getParameters()).hasSize(5);
        List<String> postUpdateVersionParams = pathUpdateVersion.getPost().getParameters().stream().map(Parameter::getName).toList();
        assertCommonParams(postUpdateVersionParams);
    }

    private void assertParams(List<String> params) {
        assertCommonParams(params);
        assertThat(params).contains("base-version");
        assertThat(params).contains("delta-version");
    }

    private void assertCommonParams(List<String> params) {
        assertThat(params).contains("consultant");
        assertThat(params).contains("client");
        assertThat(params).contains("fiscal-year");
        assertThat(params).contains("x-correlation-id");
        assertThat(params).contains("Request-Id");
    }
}
