package de.datev.refsys.aggregation.processing.boundry.controller;

import ch.qos.logback.classic.Level;
import com.mongodb.client.result.DeleteResult;
import de.datev.refsys.aggregation.processing.api.model.Problem;
import de.datev.refsys.aggregation.processing.boundry.event.ChangeEventProducer;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.config.security.WebSecurityConfiguration;
import de.datev.refsys.aggregation.processing.service.ImportExecutionService;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.MONGODB_WRITE_UNACKNOWLEDGED;
import static de.datev.refsys.aggregation.processing.constant.ProfileConstants.TEST_SECURITY_PROFILE;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.DELETE_INVENTORY_RESPONSE_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.DELETE_INVENTORY_START_LOG;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(value = DeleteInventoryController.class)
@ContextConfiguration(classes = { TestResilienceConfiguration.class })
@Import({ WebSecurityConfiguration.class })
@ActiveProfiles(TEST_SECURITY_PROFILE)
class DeleteInventoryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ImportExecutionService importExecutionService;

    @MockitoBean
    private ChangeEventProducer changeEventProducer;

    private final URI uri = UriComponentsBuilder.fromPath("/api/v1/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                                .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                                .build(TEST_CONSULTANT, TEST_CLIENT);
    private MemoryAppender memoryAppenderDeleteInventory;
    private MemoryAppender memoryAppenderLoggingUtil;

    @BeforeEach
    void setUp() {
        memoryAppenderDeleteInventory = setupMemoryAppender(memoryAppenderDeleteInventory, DeleteInventoryController.class, Level.INFO);
        memoryAppenderLoggingUtil = setupMemoryAppender(memoryAppenderLoggingUtil, LoggingUtil.class, Level.INFO);
        when(changeEventProducer.sendMessage(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
    }

    @Test
    @WithMockUser(username = "foo", roles = "INTERNAL_ADMIN")
    void shouldReturnSuccessfulIfNoDataFound() {
        // arrange
        when(importExecutionService.deleteImportData(anyInt(), anyInt(), anyInt()))
                .thenReturn(Flux.empty());

        // act & assert
        webTestClient.delete()
                     .uri(uri)
                     .exchange()
                     .expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

        verifyNoInteractions(changeEventProducer);
        assertThat(memoryAppenderDeleteInventory.search(DELETE_INVENTORY_START_LOG, Level.INFO)).hasSize(1);
        assertThat(memoryAppenderLoggingUtil.search(DELETE_INVENTORY_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).hasSize(1);
    }

    @Test
    @WithMockUser(username = "foo", roles = "INTERNAL_ADMIN")
    void shouldReturnFailureOnUnacknowledgedDeleteResult() {
        // arrange
        when(importExecutionService.deleteImportData(anyInt(), anyInt(), anyInt()))
                .thenReturn(Flux.just(acknowledged(), acknowledged(), unacknowledged()));

        // act & assert
        Problem responseBody = webTestClient.delete()
                                            .uri(uri)
                                            .exchange()
                                            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                                            .expectBody(Problem.class)
                                            .returnResult()
                                            .getResponseBody();

        assert responseBody != null;
        assertThat(responseBody.getDetail()).isEqualTo(MONGODB_WRITE_UNACKNOWLEDGED);

        verifyNoInteractions(changeEventProducer);
        assertThat(memoryAppenderDeleteInventory.search(DELETE_INVENTORY_START_LOG, Level.INFO)).hasSize(1);
        assertThat(memoryAppenderLoggingUtil.search(DELETE_INVENTORY_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).hasSize(1);
    }

    @Test
    @WithMockUser(username = "foo", roles = "INTERNAL_ADMIN")
    void shouldReturnSuccessfulIfAllAcknowledged() {
        // arrange
        when(importExecutionService.deleteImportData(anyInt(), anyInt(), anyInt()))
                .thenReturn(Flux.just(acknowledged(), acknowledged()));

        // act & assert
        webTestClient.delete()
                     .uri(uri)
                     .exchange()
                     .expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

        verify(changeEventProducer).sendMessage(any(), any(), any(), any(), any());
        assertThat(memoryAppenderDeleteInventory.search(DELETE_INVENTORY_START_LOG, Level.INFO)).hasSize(1);
        assertThat(memoryAppenderLoggingUtil.search(DELETE_INVENTORY_RESPONSE_LOG.replace("{}ms", ""), Level.INFO)).hasSize(1);
    }

    static DeleteResult acknowledged() {
        return DeleteResult.acknowledged(1);
    }

    static DeleteResult unacknowledged() {
        return DeleteResult.unacknowledged();
    }
}