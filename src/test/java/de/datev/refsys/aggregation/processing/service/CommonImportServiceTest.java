package de.datev.refsys.aggregation.processing.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import de.datev.refsys.aggregation.processing.configuration.InitialLoadContextConfiguration;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallNoContentException;
import de.datev.refsys.aggregation.processing.exception.HttpCallTechnicalException;
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.TestUtil;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Stream;

import static de.datev.refsys.aggregation.processing.util.TestUtil.EXCEPTION_MESSAGE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_BASE_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createProblemInfo;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class, classes = InitialLoadContextConfiguration.class)
@ActiveProfiles(TestUtil.TEST_PROFILE)
class CommonImportServiceTest<T extends Throwable> {

    @MockitoBean
    private ImportExecutionService importExecutionService;

    @MockitoBean
    private ImportService importService;

    private CommonImportService commonImportService;
    private MemoryAppender memoryAppender;

    @BeforeEach
    void setUp() {
        this.memoryAppender = setupMemoryAppender(memoryAppender, CommonImportServiceImpl.class, Level.DEBUG);
        this.commonImportService = new CommonImportServiceImpl(importExecutionService, importService);
    }

    @ParameterizedTest
    @MethodSource("provideExceptions")
    @DisplayName("Handles an error and propagates the error for a full import")
    void should_handle_error_and_propagate_the_error_for_full_import(Exception inputException, String errorLogMessage, Class<T> clazz) {
        // prepare
        when(importExecutionService.initializeFullImport(anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any()))
                .thenReturn(Mono.error(inputException));
        when(importExecutionService.handleExceptionAndUpdateStateDoc(anyInt(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(Mono.error(inputException));
        // execute
        Throwable throwable = catchThrowable(
                () -> commonImportService.doFullImport(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                       TEST_DELTA_VERSION).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(clazz);
        T expectedException = (T) throwable;
        assertThat(expectedException).usingRecursiveAssertion().isEqualTo(inputException);
        if (errorLogMessage != null) {
            List<ILoggingEvent> searchResult = memoryAppender.search(errorLogMessage, Level.ERROR);
            assertThat(searchResult).hasSize(1);
        }
    }

    @ParameterizedTest
    @MethodSource("provideExceptions")
    @DisplayName("Handles an error and propagates the error for a fire and forget import")
    void should_handle_error_and_propagate_the_error_for_fire_and_forget_import(Exception inputException, String errorLogMessage, Class<T> clazz) {
        // prepare
        when(importExecutionService.initializeFullImport(anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any()))
                .thenReturn(Mono.error(inputException));
        when(importExecutionService.handleExceptionAndUpdateStateDoc(anyInt(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(Mono.error(inputException));
        // execute
        Throwable throwable = catchThrowable(
                () -> commonImportService.doFireAndForgetFullImport(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                                    TEST_DELTA_VERSION).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(clazz);
        T expectedException = (T) throwable;
        assertThat(expectedException).usingRecursiveAssertion().isEqualTo(inputException);
        if (errorLogMessage != null) {
            List<ILoggingEvent> searchResult = memoryAppender.search(errorLogMessage, Level.ERROR);
            assertThat(searchResult).hasSize(1);
        }
    }

    @Test
    void testEnhanceCustomColumnStructureSetsIsOrganisationData() {
        // Arrange: Erzeuge ein CustomColumnStructure-Objekt mit gültigen Werten
        CustomColumnStructure customColumn = new CustomColumnStructure();
        customColumn.setColumnStructureId(1);
        customColumn.setIndivNo(1);
        customColumn.setIndustryNo(45);
        customColumn.setSectionNo(2);
        customColumn.setNationalRight("DE");
        customColumn.setIsOrganisationData(false);
        // Setze einen nicht 0-wertigen copyFromComprehensiveConsultant
        customColumn.setCopyFromComprehensiveConsultant(100);

        // Act: Rufe die Validierungs-/Enhance-Methode auf (sofern diese paket-private ist)
        List<String> errors = CustomStructuresServiceImpl.validateAndEnhanceCustomColumnStructures(List.of(customColumn));

        // Assert: Es dürfen keine Fehler zurückgegeben werden und das Flag muss auf true gesetzt worden sein.
        assertThat(errors).isEmpty();
        assertThat(customColumn.getIsOrganisationData()).isTrue();
    }

    @Test
    void testEnhanceCustomReportStructureSetsIsOrganisationData() {
        // Arrange: Erzeuge ein CustomReportStructure-Objekt mit gültigen Werten
        CustomReportStructure customReport = new CustomReportStructure();
        customReport.setReportStructureId(1);
        customReport.setIndivNo(1);
        customReport.setIndustryNo(45);
        customReport.setSectionNo(2);
        customReport.setNationalRight("DE");
        customReport.setIsOrganisationData(false);
        // Setze einen nicht 0-wertigen copyFromComprehensiveConsultant
        customReport.setCopyFromComprehensiveConsultant(200);

        // Act: Rufe die Validierungs-/Enhance-Methode auf
        List<String> errors = CustomStructuresServiceImpl.validateAndEnhanceCustomReportStructures(List.of(customReport));

        // Assert: Es dürfen keine Fehler zurückgegeben werden und das Flag muss auf true gesetzt worden sein.
        assertThat(errors).isEmpty();
        assertThat(customReport.getIsOrganisationData()).isTrue();
    }

    private static Stream<Arguments> provideExceptions() {
        HttpCallNoContentException httpCallNoContentException =
                new HttpCallNoContentException(SourceError.ACDS, SourceEndpoint.MASTER_DATA_CONTEXT, HttpStatus.NO_CONTENT.value());
        HttpCallTechnicalException httpCallTechnicalException =
                new HttpCallTechnicalException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.MASTER_DATA_CONTEXT,
                                               HttpStatus.BAD_REQUEST.value(), createProblemInfo());
        HttpCallBusinessException httpCallBusinessException =
                new HttpCallBusinessException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.MASTER_DATA_CONTEXT, 555, createProblemInfo());
        AggregationProcessingBusinessException aggregationProcessingBusinessException =
                new AggregationProcessingBusinessException(EXCEPTION_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        NullPointerException nullPointerException = new NullPointerException(EXCEPTION_MESSAGE);
        return Stream.of(Arguments.of(httpCallNoContentException, null, HttpCallNoContentException.class),
                         Arguments.of(httpCallTechnicalException, null, HttpCallTechnicalException.class),
                         Arguments.of(httpCallBusinessException, null, HttpCallBusinessException.class),
                         Arguments.of(aggregationProcessingBusinessException, null, AggregationProcessingBusinessException.class),
                         Arguments.of(nullPointerException, ProcessingErrorMessageConstants.UNEXPECTED_ERROR_IN_EXCEPTION_HANDLING, NullPointerException.class));
    }
}