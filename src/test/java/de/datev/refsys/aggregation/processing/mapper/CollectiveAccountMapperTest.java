package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.CollectiveAccount;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CollectiveAccountMapperTest {

    private final CollectiveAccountMapper collectiveAccountMapper = new CollectiveAccountMapperImpl();

    @Test
    void should_map_null_to_null_additionalParametersMapper() {
        assertThat(collectiveAccountMapper.mapAcdsToDb(null)).isNull();
        assertThat(collectiveAccountMapper.mapAcdsToDbList(null)).isEqualTo(new ArrayList<>());
    }

    @Test
    void should_map_acds_to_dbCollectiveAccounts() {
        List<CollectiveAccount> input = TestDataLoader.loadNdJsonList("json/acds-responses/mapper/collectiveAccounts.ndjson", CollectiveAccount.class);

        List<de.datev.refsys.aggregation.document.model.CollectiveAccount> dbCollectiveAccountsExpected = //
                TestDataLoader.loadMongoDBList("json/collections/expected/mapper/CollectiveAccountExpected.json", de.datev.refsys.aggregation.document.model.CollectiveAccount.class);
        List<de.datev.refsys.aggregation.document.model.CollectiveAccount> dbCollectiveAccounts = collectiveAccountMapper.mapAcdsToDbList(input);

        assertThat(dbCollectiveAccounts).usingRecursiveComparison().isEqualTo(dbCollectiveAccountsExpected);
    }

}
