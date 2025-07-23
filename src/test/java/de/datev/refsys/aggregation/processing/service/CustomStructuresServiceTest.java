package de.datev.refsys.aggregation.processing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import de.datev.refsys.aggregation.processing.client.CustomStructuresClient;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureContentMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureInfoMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureInfoMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureContentMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureInfoMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureInfoMapperImpl;
import de.datev.refsys.aggregation.processing.repository.CustomColumnStructureContentRepository;
import de.datev.refsys.aggregation.processing.repository.CustomReportStructureContentRepository;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.aggregation.processing.util.TestUtil;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.COLUMN_STRUCTURE_REQUIRED_FIELDS_ARE_NULL_ERROR;
import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.REPORT_STRUCTURE_REQUIRED_FIELDS_ARE_NULL_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(TestUtil.TEST_PROFILE)
class CustomStructuresServiceTest {
    private static final CustomColumnStructureInfoMapper CUSTOM_COLUMN_STRUCTURE_INFO_MAPPER = new CustomColumnStructureInfoMapperImpl();
    private static final CustomReportStructureInfoMapper CUSTOM_REPORT_STRUCTURE_INFO_MAPPER = new CustomReportStructureInfoMapperImpl();
    private static final CustomColumnStructureContentMapper CUSTOM_COLUMN_STRUCTURE_CONTENT_MAPPER = new CustomColumnStructureContentMapperImpl();
    private static final CustomReportStructureContentMapper CUSTOM_REPORT_STRUCTURE_CONTENT_MAPPER = new CustomReportStructureContentMapperImpl();

    @MockitoBean
    private CustomStructuresClient customStructuresClient;

    @MockitoBean
    private CustomReportStructureContentRepository customReportStructureContentRepository;

    @MockitoBean
    private CustomColumnStructureContentRepository customColumnStructureContentRepository;

    private CustomStructuresService customStructuresService;

    @BeforeEach
    void setUp() {
        customStructuresService = new CustomStructuresServiceImpl(customStructuresClient, customReportStructureContentRepository,
                                                                  customColumnStructureContentRepository, CUSTOM_REPORT_STRUCTURE_CONTENT_MAPPER,
                                                                  CUSTOM_COLUMN_STRUCTURE_CONTENT_MAPPER, CUSTOM_REPORT_STRUCTURE_INFO_MAPPER,
                                                                  CUSTOM_COLUMN_STRUCTURE_INFO_MAPPER);
    }


    @Test
    @DisplayName("Should throw an error when required attributes in customReportStructures are null")
    void should_throw_an_error_when_required_attributes_in_custom_report_structures_are_null() {
        // prepare
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        List<CustomColumnStructure> ccs = TestDataLoader.loadList("json/acds-responses/mapper/customColumnStructures.json", new TypeReference<>() {
        });
        when(customStructuresClient.getCustomColumnStructuresList(any())).thenReturn(Mono.just(ccs));

        List<CustomReportStructure> crs =
                TestDataLoader.loadList("json/acds-responses/mapper/customReportStructuresInvalid.json", new TypeReference<>() {
                });
        when(customStructuresClient.getCustomReportStructuresList(any())).thenReturn(Mono.just(crs));

        Throwable throwable = catchThrowable(() -> customStructuresService.getCustomStructures(masterDataContext).block());
        // assert
        assertThat(throwable).isInstanceOf(AggregationProcessingBusinessException.class)
                             .hasMessageStartingWith(REPORT_STRUCTURE_REQUIRED_FIELDS_ARE_NULL_ERROR,
                                                     "reportStructureId, indivNo, nationalRight, indivLevel");
        AggregationProcessingBusinessException httpCallFailedException = (AggregationProcessingBusinessException) throwable;
        assertThat(httpCallFailedException.getHttpStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("Should throw an error when required attributes in customColumnStructures are null")
    void should_throw_an_error_when_required_attributes_in_custom_column_structures_are_null() {
        // prepare
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        List<CustomColumnStructure> ccs =
                TestDataLoader.loadList("json/acds-responses/mapper/customColumnStructuresInvalid.json", new TypeReference<>() {
                });
        when(customStructuresClient.getCustomColumnStructuresList(any())).thenReturn(Mono.just(ccs));

        List<CustomReportStructure> crs = TestDataLoader.loadList("json/acds-responses/mapper/customReportStructures.json", new TypeReference<>() {
        });
        when(customStructuresClient.getCustomReportStructuresList(any())).thenReturn(Mono.just(crs));

        Throwable throwable = catchThrowable(() -> customStructuresService.getCustomStructures(masterDataContext).block());
        // assert
        assertThat(throwable).isInstanceOf(AggregationProcessingBusinessException.class)
                             .hasMessageStartingWith(COLUMN_STRUCTURE_REQUIRED_FIELDS_ARE_NULL_ERROR,
                                                     "columnStructureId, indivNo, nationalRight, indivLevel");
        AggregationProcessingBusinessException httpCallFailedException = (AggregationProcessingBusinessException) throwable;
        assertThat(httpCallFailedException.getHttpStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("Should not insert customColumnStructures when the list is empty")
    void should_not_insert_custom_column_structures_when_the_list_is_empty() {
        Void block = customStructuresService.insertCustomColumnStructureContents(Collections.emptyList()).block();
        assertThat(block).isNull();
        verify(customColumnStructureContentRepository, times(0)).bulkInsert(any());
    }

    @Test
    @DisplayName("Should not insert customReportStructures when the list is empty")
    void should_not_insert_custom_report_structures_when_the_list_is_empty() {
        Void block = customStructuresService.insertCustomReportStructureContents(Collections.emptyList()).block();
        assertThat(block).isNull();
        verify(customReportStructureContentRepository, times(0)).bulkInsert(any());
    }

}