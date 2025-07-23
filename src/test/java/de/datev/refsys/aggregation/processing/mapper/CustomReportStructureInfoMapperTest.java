package de.datev.refsys.aggregation.processing.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import de.datev.refsys.aggregation.document.model.CustomReportStructureInfo;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomReportStructureInfoMapperTest {
    
    private final CustomReportStructureInfoMapper customReportStructureInfoMapper = new CustomReportStructureInfoMapperImpl();

    @Test
    @DisplayName("Map CustomReportStructure to CustomReportStructureInfo")
    void should_map_custom_report_structure_to_custom_report_structure_info() {
        List<CustomReportStructureInfo> expectedCustomReportStructureInfoList =
                TestDataLoader.loadMongoDBList("json/collections/expected/mapper/customReportStructureInfo.json", CustomReportStructureInfo.class);

        List<CustomReportStructure> customReportStructureList =
                TestDataLoader.loadList("json/acds-responses/mapper/customReportStructures.json", new TypeReference<>() {
                });
        CustomReportStructureInfo customReportStructureInfo = customReportStructureInfoMapper.mapAcdsToDb(customReportStructureList.get(0));
        assertThat(customReportStructureInfo).usingRecursiveComparison().isEqualTo(expectedCustomReportStructureInfoList.get(0));
    }

    @Test
    @DisplayName("Map CustomReportStructureList to CustomReportStructureInfoList")
    void should_map_custom_report_structure_list_to_custom_report_structure_info_list() {
        List<CustomReportStructureInfo> expectedCustomReportStructureInfoList =
                TestDataLoader.loadMongoDBList("json/collections/expected/mapper/customReportStructureInfo.json", CustomReportStructureInfo.class);

        List<CustomReportStructure> customReportStructureList =
                TestDataLoader.loadList("json/acds-responses/mapper/customReportStructures.json", new TypeReference<>() {
                });
        List<CustomReportStructureInfo> customReportStructureInfoList = customReportStructureInfoMapper.mapAcdsToDb(customReportStructureList);
        assertThat(customReportStructureInfoList).usingRecursiveComparison().isEqualTo(expectedCustomReportStructureInfoList);
    }

    @Test
    @DisplayName("Map null to null list")
    void should_map_null_custom_report_structure_list_to_null() {
        List<CustomReportStructureInfo> customReportStructureInfoList = customReportStructureInfoMapper.mapAcdsToDb(
                (List<CustomReportStructure>) null);
        assertThat(customReportStructureInfoList).isNull();
    }

    @Test
    @DisplayName("Map null to null")
    void should_map_null_custom_report_structure_to_null() {
        CustomReportStructureInfo customReportStructureInfo = customReportStructureInfoMapper.mapAcdsToDb((CustomReportStructure) null);
        assertThat(customReportStructureInfo).isNull();
    }

    @Test
    @DisplayName("Map null value in CustomReportStructureList to null value in CustomReportStructureInfoList")
    void should_map_custom_report_structure_list_to_custom_report_structure_info_list2() {
        List<CustomReportStructureInfo> expectedCustomReportStructureInfoList =
                TestDataLoader.loadMongoDBList("json/collections/expected/mapper/customReportStructureInfo.json", CustomReportStructureInfo.class);
        expectedCustomReportStructureInfoList.get(0).setIndivNo(null);

        List<CustomReportStructure> customReportStructureList =
                TestDataLoader.loadList("json/acds-responses/mapper/customReportStructures.json", new TypeReference<>() {
                });
        customReportStructureList.get(0).setIndivNo(null);

        List<CustomReportStructureInfo> customReportStructureInfoList = customReportStructureInfoMapper.mapAcdsToDb(customReportStructureList);
        assertThat(customReportStructureInfoList).usingRecursiveComparison().isEqualTo(expectedCustomReportStructureInfoList);
    }
}