package de.datev.refsys.aggregation.processing.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import de.datev.refsys.generated.acds.api.model.AccountPurposeMapping;
import de.datev.refsys.aggregation.document.model.AccountPurpose;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountPurposeMapperTest {

    private final AccountPurposeMapper accountPurposeMapper = new AccountPurposeMapperImpl();

    @Test
    void should_map_null_to_null_shareholderRelationMapper2() {
        assertThat(accountPurposeMapper.mapAcdsToDb(null)).isNull();
        assertThat(accountPurposeMapper.mapAcdsToDbList(null)).isEqualTo(new ArrayList<>());
    }

    @Test
    void should_map_acds_to_dbAccountPurposeMappings() {
        List<AccountPurposeMapping> accountPurposeMappings = TestDataLoader.loadNdJsonList("json/acds-responses/accountPurposeMappings.ndjson", AccountPurposeMapping.class);
        List<AccountPurpose> accountPurposeExpected = TestDataLoader.loadList("json/collections/expected/mapper/accountPurposeMappingsExpected.json", new TypeReference<>() {
        });
        List<AccountPurpose> accountDescriptions = accountPurposeMapper.mapAcdsToDbList(accountPurposeMappings);

        assertThat(accountDescriptions).isNotEmpty();
        assertThat(accountDescriptions).usingRecursiveFieldByFieldElementComparator().containsAll(accountPurposeExpected);
    }
}
