package de.datev.refsys.aggregation.processing.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import de.datev.refsys.aggregation.document.model.CustomColumnStructureInfo;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomColumnStructureInfoMapperTest {

    private final CustomColumnStructureInfoMapper customColumnStructureInfoMapper = new CustomColumnStructureInfoMapperImpl();

    @Test
    @DisplayName("Map CustomColumnStructure to CustomColumnStructureInfo")
    void should_map_custom_column_structure_to_custom_column_structure_info() {
        List<CustomColumnStructureInfo> expectedCustomColumnStructureInfoList =
                TestDataLoader.loadMongoDBList("json/collections/expected/mapper/customColumnStructureInfo.json", CustomColumnStructureInfo.class);

        List<CustomColumnStructure> customColumnStructureList =
                TestDataLoader.loadList("json/acds-responses/mapper/customColumnStructures.json", new TypeReference<>() {
                });

        CustomColumnStructureInfo customColumnStructureInfo = customColumnStructureInfoMapper.mapAcdsToDb(customColumnStructureList.get(0));
        assertThat(customColumnStructureInfo).usingRecursiveComparison().isEqualTo(expectedCustomColumnStructureInfoList.get(0));
    }

    @Test
    @DisplayName("Map CustomColumnStructureList to CustomColumnStructureInfoList")
    void should_map_custom_column_structure_list_to_custom_column_structure_info_list() {
        List<CustomColumnStructureInfo> expectedCustomColumnStructureInfoList =
                TestDataLoader.loadMongoDBList("json/collections/expected/mapper/customColumnStructureInfo.json", CustomColumnStructureInfo.class);

        List<CustomColumnStructure> customColumnStructureList =
                TestDataLoader.loadList("json/acds-responses/mapper/customColumnStructures.json", new TypeReference<>() {
                });

        List<CustomColumnStructureInfo> customColumnStructureInfoList = customColumnStructureInfoMapper.mapAcdsToDb(customColumnStructureList);
        assertThat(customColumnStructureInfoList).usingRecursiveComparison().isEqualTo(expectedCustomColumnStructureInfoList);
    }

    @Test
    @DisplayName("Map null to null list")
    void should_map_null_custom_column_structure_list_to_null() {
        List<CustomColumnStructureInfo> customColumnStructureInfoList = customColumnStructureInfoMapper.mapAcdsToDb(
                (List<CustomColumnStructure>) null);
        assertThat(customColumnStructureInfoList).isNull();
    }

    @Test
    @DisplayName("Map null to null")
    void should_map_null_custom_column_structure_to_null() {
        CustomColumnStructureInfo customColumnStructureInfo = customColumnStructureInfoMapper.mapAcdsToDb(
                (CustomColumnStructure) null);
        assertThat(customColumnStructureInfo).isNull();
    }

    @Test
    @DisplayName("Map null value in CustomColumnStructureList to null value in CustomColumnStructureInfoList")
    void should_map_null_value_in_custom_column_structure_list_to_null_value_in_custom_column_structure_info_list() {
        List<CustomColumnStructureInfo> expectedCustomColumnStructureInfoList =
                TestDataLoader.loadMongoDBList("json/collections/expected/mapper/customColumnStructureInfo.json", CustomColumnStructureInfo.class);
        expectedCustomColumnStructureInfoList.get(0).setIndivNo(null);

        List<CustomColumnStructure> customColumnStructureList =
                TestDataLoader.loadList("json/acds-responses/mapper/customColumnStructures.json", new TypeReference<>() {
                });
        customColumnStructureList.get(0).setIndivNo(null);

        List<CustomColumnStructureInfo> customColumnStructureInfoList = customColumnStructureInfoMapper.mapAcdsToDb(customColumnStructureList);
        assertThat(customColumnStructureInfoList).usingRecursiveComparison().isEqualTo(expectedCustomColumnStructureInfoList);
    }

}