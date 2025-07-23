package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.aggregation.document.model.AdditionalParameters;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdditionalParametersMapperTest {
    private AdditionalParametersMapper additionalParametersMapper = new AdditionalParametersMapperImpl_();

    @Test
    void should_map_null_to_null_additionalParametersMapper() {
        assertThat(additionalParametersMapper.accountSumDayToAdditionalParameters(null)).isNull();
    }

    @Test
    void should_map_accountSumDay_to_additionalParams() {
        List<AccountSumDay> accountSumDays = TestDataLoader.loadNdJsonList("json/acds-responses/mapper/accountSumDays.ndjson", AccountSumDay.class);
        AdditionalParameters additionalParametersExpected = TestDataLoader.loadDBElement("json/collections/expected/mapper/additionalParametersExpected.json", AdditionalParameters.class);
        AdditionalParameters additionalParameters = additionalParametersMapper.accountSumDayToAdditionalParameters(accountSumDays.get(0));
        assertThat(additionalParameters).usingRecursiveComparison().isEqualTo(additionalParametersExpected);
    }
}
