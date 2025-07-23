package de.datev.refsys.aggregation.processing.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import de.datev.refsys.aggregation.document.model.CustomReportStructureContent;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomReportStructureContentMapperTest {
    private final CustomReportStructureContentMapper customReportStructureContentMapper = new CustomReportStructureContentMapperImpl();

    private List<CustomReportStructureContent> expectedCustomReportStructureContentList;

    private List<CustomReportStructure> customReportStructureList;

    @BeforeEach
    void setUp() {
        expectedCustomReportStructureContentList =
                TestDataLoader.loadMongoDBList("json/collections/repository/customReportStructureContents.json", CustomReportStructureContent.class);
        customReportStructureList =
                TestDataLoader.loadList("json/acds-responses/mapper/customReportStructures.json", new TypeReference<>() {
                });
    }
    @Test
    @DisplayName("Map null ACDS CustomReportStructureContent to null")
    void should_map_acds_custom_report_structure_content_to_null_when_not_present() {
        CustomReportStructureContent customReportStructureContent = customReportStructureContentMapper.mapAcdsToDb(null, null);
        assertThat(customReportStructureContent).isNull();
    }

    @Test
    @DisplayName("Map null ACDS CustomReportStructureContentList to default")
    void should_map_acds_custom_report_structure_content_list_to_default_when_not_present() {
        List<CustomReportStructureContent> customReportStructureContents = customReportStructureContentMapper.mapAcdsToDbList(null, null);
        assertThat(customReportStructureContents).isEmpty();
    }

    @Test
    @DisplayName("Map ACDS CustomReportStructureContents DB CustomReportStructureContents")
    void should_map_acds_custom_report_structure_contents_to_db_custom_report_structure_contents() {
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/mapper/masterDataContext.json", MasterdataContext.class);
        List<CustomReportStructureContent> customReportStructureContents =
                customReportStructureContentMapper.mapAcdsToDbList(customReportStructureList, masterdataContext);
        assertThat(customReportStructureContents).isNotNull()
                                                 .isNotEmpty()
                                                 .usingRecursiveComparison()
                                                 .ignoringCollectionOrder()
                                                 .isEqualTo(expectedCustomReportStructureContentList);
    }

    @Test
    @DisplayName("Map default numbers to null")
    void should_map_default_numbers_to_null() {
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/mapper/masterDataContext.json", MasterdataContext.class);
        CustomReportStructureContent customReportStructureContent =
                customReportStructureContentMapper.mapAcdsToDb(customReportStructureList.get(1), masterdataContext);
        assertThat(customReportStructureContent).isNotNull();
        assertThat(customReportStructureContent).usingRecursiveComparison().isEqualTo(expectedCustomReportStructureContentList.get(1));
    }
}