package de.datev.refsys.aggregation.processing.config.security;

import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.api.model.DeltaInfo;
import de.datev.refsys.aggregation.processing.boundry.controller.DeleteInventoryController;
import de.datev.refsys.aggregation.processing.boundry.controller.InitialLoadController;
import de.datev.refsys.aggregation.processing.boundry.controller.ProcessDeltaController;
import de.datev.refsys.aggregation.processing.boundry.controller.UpdateSchemaController;
import de.datev.refsys.aggregation.processing.boundry.controller.UpdateVersionController;
import de.datev.refsys.aggregation.processing.boundry.event.ChangeEventProducer;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.repository.StateDocRepository;
import de.datev.refsys.aggregation.processing.service.ImportService;
import de.datev.refsys.aggregation.processing.service.CommonImportService;
import de.datev.refsys.aggregation.processing.service.DeltaEventProcessingService;
import de.datev.refsys.aggregation.processing.service.ImportExecutionService;
import de.datev.refsys.aggregation.processing.service.CommonImportService;
import de.datev.refsys.aggregation.processing.service.ImportExecutionService;
import de.datev.refsys.aggregation.processing.service.UpdateSchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static de.datev.refsys.aggregation.processing.constant.ProfileConstants.TEST_SECURITY_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_END;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@WebFluxTest(value = { DeleteInventoryController.class, InitialLoadController.class, UpdateSchemaController.class, UpdateVersionController.class, ProcessDeltaController.class})
@Import({ WebSecurityConfiguration.class })
@ContextConfiguration(classes = { TestResilienceConfiguration.class})
@TestPropertySource(properties = { "ref-sys.basic-auth-admin.username=foo", "ref-sys.basic-auth-admin.password=bar" })
@ActiveProfiles(TEST_SECURITY_PROFILE)
class WebSecurityBasicAuthTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CommonImportService commonImportService;

    @MockitoBean
    private ImportExecutionService importExecutionService;

    @MockitoBean
    private UpdateSchemaService updateSchemaService;

    @MockitoBean
    private StateDocRepository stateDocRepository;

    @MockitoBean
    private DeltaEventProcessingService deltaEventProcessingService;

    @MockitoBean
    private ChangeEventProducer changeEventProducer;

    private final Consumer<HttpHeaders> basicAuthHeaders = headers -> headers.setBasicAuth("foo", "bar");

    @BeforeEach
    void setUp() {
        when(commonImportService.doFireAndForgetFullImport(anyInt(), anyInt(), anyInt(), anyLong(), anyLong())).thenReturn(Mono.empty());
        when(updateSchemaService.updateSchemaVersion(anyInt(), anyInt(), anyInt(), anyLong(), anyLong())).thenReturn(Mono.empty());
        when(importExecutionService.deleteImportData(anyInt(), anyInt(), anyInt()))
                .thenReturn(Flux.empty());
        when(deltaEventProcessingService.processDeltaEvent(anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any())).thenReturn(Mono.empty());
        StateDoc stateDoc = new StateDoc();
        stateDoc.setState(StateDocState.DONE);
        stateDoc.setBaseVersion(1L);
        stateDoc.setDeltaVersion(0L);
        when(stateDocRepository.findOneByBusinessKey(any(), any(), any())).thenReturn(Mono.just(stateDoc));
    }

    @ParameterizedTest
    @MethodSource("valueTestProvider")
    void shouldRReturnStatusWithBasicAuth(HttpMethod method, String path, int status) {
        // act & assert
        webTestClient.method(method)
                     .uri(uriFromPath(path))
                     .headers(basicAuthHeaders)
                     .contentType(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus().isEqualTo(status);
    }

    @ParameterizedTest
    @MethodSource("valueTestProvider")
    void shouldReturnUnauthorizedWithoutBasicAuth(HttpMethod method, String path, int status) {
        // act & assert
        webTestClient.method(method)
                     .uri(uriFromPath(path))
                     // no basicAuthHeaders
                     .exchange()
                     .expectStatus().isUnauthorized();
    }

    @Test
    void update_version_shouldReturnUnauthorizedWithoutBasicAuth() {
        HttpMethod method = HttpMethod.POST;
        String path = "/api/v1/aggregation-processing/consultants/{consultant}/clients/{client}/update-version";

        // act & assert
        DeltaInfo data = new DeltaInfo();
        data.setBaseVersion(1L);
        data.setDeltaVersion(0L);
        webTestClient.method(method)
                     .uri(uriFromPath(path))
                     .body(Mono.just(data),
                           DeltaInfo.class)
                     // no basicAuthHeaders
                     .exchange()
                     .expectStatus().isUnauthorized();
    }

    @Test
    void update_version_shouldReturnWithBasicAuth() {
        HttpMethod method = HttpMethod.POST;
        String path = "/api/v1/aggregation-processing/consultants/{consultant}/clients/{client}/update-version";

        // act & assert
        DeltaInfo data = new DeltaInfo();
        data.setBaseVersion(1L);
        data.setDeltaVersion(0L);
        webTestClient.method(method)
                     .uri(uriFromPath(path))
                     .headers(basicAuthHeaders)
                     .body(Mono.just(data),
                           DeltaInfo.class)
                     // no basicAuthHeaders
                     .exchange()
                     .expectStatus().is2xxSuccessful();
    }

    @ParameterizedTest
    @MethodSource("valueTestProvider")
    void shouldReturnUnauthorizedWithWrongBasicAuth(HttpMethod method, String path, int status) {
        // act & assert
        webTestClient.method(method)
                     .uri(uriFromPath(path))
                     .headers(headers -> headers.setBasicAuth("is", "wrong"))
                     .exchange()
                     .expectStatus().isUnauthorized();
    }

    static Stream<Arguments> valueTestProvider() {
        final String path = "/api/v1/aggregation-processing/consultants/{consultant}/clients/{client}";

        return Stream.of(
                Arguments.arguments(HttpMethod.POST, path + "/initial-load", 201),
                Arguments.arguments(HttpMethod.POST, path + "/update-schema", 204),
                Arguments.arguments(HttpMethod.DELETE, path + "/initial-load", 204),
                Arguments.arguments(HttpMethod.POST, path + "/process-delta", 204)
        );
    }

    static URI uriFromPath(String path) {
        return UriComponentsBuilder.fromPath(path)
                                   .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                   .build(TEST_CONSULTANT, TEST_CLIENT);
    }
}