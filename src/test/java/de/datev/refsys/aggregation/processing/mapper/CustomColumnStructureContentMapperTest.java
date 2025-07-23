package de.datev.refsys.aggregation.processing.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import de.datev.refsys.aggregation.document.model.CustomColumnStructureContent;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomColumnStructureContentMapperTest {
    private final CustomColumnStructureContentMapper customColumnStructureContentMapper = new CustomColumnStructureContentMapperImpl();

    private List<CustomColumnStructureContent> expectedCustomColumnStructureContentList;

    private List<CustomColumnStructure> customColumnStructureList;

    @BeforeEach
    void setUp() {
        expectedCustomColumnStructureContentList =
                TestDataLoader.loadMongoDBList("json/collections/repository/customColumnStructureContents.json", CustomColumnStructureContent.class);
        customColumnStructureList =
                TestDataLoader.loadList("json/acds-responses/mapper/customColumnStructures.json", new TypeReference<>() {
                });
    }

    @Test
    @DisplayName("Map null ACDS CustomColumnStructureContent to null")
    void should_map_acds_custom_column_structure_content_to_null_when_not_present() {
        CustomColumnStructureContent customColumnStructureContent = customColumnStructureContentMapper.mapAcdsToDb(null, null);
        assertThat(customColumnStructureContent).isNull();
    }

    @Test
    @DisplayName("Map null ACDS CustomColumnStructureContentList to default")
    void should_map_acds_custom_column_structure_content_list_to_default_when_not_present() {
        List<CustomColumnStructureContent> customColumnStructureContents = customColumnStructureContentMapper.mapAcdsToDbList(null, null);
        assertThat(customColumnStructureContents).isEmpty();
    }

    @Test
    @DisplayName("Map ACDS CustomColumnStructureContents DB CustomColumnStructureContents")
    void should_map_acds_custom_column_structure_contents_to_db_custom_column_structure_contents() {
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/mapper/masterDataContext.json", MasterdataContext.class);
        List<CustomColumnStructureContent> customColumnStructureContents =
                customColumnStructureContentMapper.mapAcdsToDbList(customColumnStructureList, masterdataContext);
        assertThat(customColumnStructureContents).isNotNull()
                                                 .isNotEmpty()
                                                 .usingRecursiveComparison()
                                                 .ignoringCollectionOrder()
                                                 .isEqualTo(expectedCustomColumnStructureContentList);
    }

    @Test
    @DisplayName("Map default numbers to null")
    void should_map_default_numbers_to_null() {
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/mapper/masterDataContext.json", MasterdataContext.class);
        CustomColumnStructureContent customColumnStructureContent =
                customColumnStructureContentMapper.mapAcdsToDb(customColumnStructureList.get(1), masterdataContext);
        assertThat(customColumnStructureContent).isNotNull().usingRecursiveComparison().isEqualTo(expectedCustomColumnStructureContentList.get(1));
    }
}