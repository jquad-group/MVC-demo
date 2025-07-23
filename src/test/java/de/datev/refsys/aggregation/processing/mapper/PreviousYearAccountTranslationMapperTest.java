package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.Translation;
import de.datev.refsys.aggregation.document.model.PreviousYearAccountTranslation;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PreviousYearAccountTranslationMapperTest {

    private final PreviousYearAccountTranslationMapper previousYearAccountTranslationMapper = new PreviousYearAccountTranslationMapperImpl();

    @Test
    void should_map_null_to_null_additionalParametersMapper() {
        assertThat(previousYearAccountTranslationMapper.mapAcdsToDb(null)).isNull();
        assertThat(previousYearAccountTranslationMapper.mapAcdsToDbList(null)).isEqualTo(new ArrayList<>());
    }

    @Test
    void should_map_acds_to_dbTranslations() {
        List<PreviousYearAccountTranslation> previousYearAccountTranslationsExpected = TestDataLoader.loadMongoDBList("json/collections/expected/mapper/PreviousYearAccountTranslationExpected.json", PreviousYearAccountTranslation.class);
        Translation translation = TestDataLoader.load("json/acds-responses/mapper/translation.json", Translation.class);
        List<PreviousYearAccountTranslation> previousYearAccountTranslations = previousYearAccountTranslationMapper.mapAcdsToDbList(translation.getPreviousYearAccountTranslations());

        assertThat(previousYearAccountTranslations).isNotEmpty();
        assertThat(previousYearAccountTranslations).usingRecursiveComparison().isEqualTo(previousYearAccountTranslationsExpected);
    }

}
