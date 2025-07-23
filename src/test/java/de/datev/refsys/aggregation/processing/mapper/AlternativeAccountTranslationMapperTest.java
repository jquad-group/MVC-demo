package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.Translation;
import de.datev.refsys.aggregation.document.model.AlternativeAccountTranslation;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlternativeAccountTranslationMapperTest {
    private final AlternativeAccountTranslationMapper alternativeAccountTranslationMapper = new AlternativeAccountTranslationMapperImpl();

    @Test
    void should_map_null_to_null_additionalParametersMapper() {
        assertThat(alternativeAccountTranslationMapper.mapAcdsToDb(null)).isNull();
        assertThat(alternativeAccountTranslationMapper.mapAcdsToDbList(null)).isEqualTo(new ArrayList<>());
    }

    @Test
    void should_map_acds_to_dbTranslations() {
        Translation translation = TestDataLoader.load("json/acds-responses/mapper/translation.json", Translation.class);
        List<AlternativeAccountTranslation> alternativeAccountTranslationsExpected = TestDataLoader.loadMongoDBList("json/collections/expected/mapper/AlternativeAccountTranslationExpected.json", AlternativeAccountTranslation.class);
        List<AlternativeAccountTranslation> alternativeAccountTranslations = alternativeAccountTranslationMapper.mapAcdsToDbList(translation.getAlternativeAccountTranslations());
        assertThat(alternativeAccountTranslations).usingRecursiveComparison().isEqualTo(alternativeAccountTranslationsExpected);
    }

}
