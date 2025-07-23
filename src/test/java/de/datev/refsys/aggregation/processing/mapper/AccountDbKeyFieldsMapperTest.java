package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountDbKeyFieldsMapperTest {
    private AccountDbKeyFieldsMapper accountDbKeyFieldsMapper = new AccountDbKeyFieldsMapperImpl();

    @Test
    void should_map_null_to_null_accountDbKeyFieldsMapper() {
        assertThat(accountDbKeyFieldsMapper.accountSumDayToAccountDbKeyFields(null)).isNull();
        assertThat(accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(null)).isNull();
    }

    @Test
    void should_map_accountSumDay_to_accountDbKeyFields() {
        List<AccountSumDay> accountSumDays = TestDataLoader.loadNdJsonList("json/acds-responses/mapper/accountSumDays.ndjson", AccountSumDay.class);
        AccountDbKeyFields accountDbKeyFields = accountDbKeyFieldsMapper.accountSumDayToAccountDbKeyFields(accountSumDays.get(0));
        assertThat(accountDbKeyFields).isNotNull();
        assertThat(accountDbKeyFields.getAccountNumber()).isEqualTo(14500000);
        assertThat(accountDbKeyFields.getAccountingCommitted()).isTrue();
        assertThat(accountDbKeyFields.getAccountingReasonId()).isZero();
        assertThat(accountDbKeyFields.getAgricultureAndForestryAccountType()).isEqualTo(25);
        assertThat(accountDbKeyFields.getBusinessAssetsAssignment()).isZero();
        assertThat(accountDbKeyFields.getCost1()).isEqualTo("91");
        assertThat(accountDbKeyFields.getCost2()).isEqualTo("A1");
        assertThat(accountDbKeyFields.getRecordType()).isZero();
        assertThat(accountDbKeyFields.getRwShareholderId()).isEqualTo(UUID.fromString("9c18c30b-ac17-451f-81c7-1b9b26dd73fe"));
        assertThat(accountDbKeyFields.getTaxRate()).isEqualTo(0.19f);
        AccountDbKeyFields accountGroupDbKeyFields1 = accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(accountSumDays.get(1));
        assertThat(accountGroupDbKeyFields1).isNotNull();
        assertThat(accountGroupDbKeyFields1.getAccountNumber()).isEqualTo(2);
        assertThat(accountGroupDbKeyFields1.getAccountingCommitted()).isFalse();
        assertThat(accountGroupDbKeyFields1.getAccountingReasonId()).isEqualTo(1);
        assertThat(accountGroupDbKeyFields1.getAgricultureAndForestryAccountType()).isEqualTo(26);
        assertThat(accountGroupDbKeyFields1.getBusinessAssetsAssignment()).isEqualTo(1);
        assertThat(accountGroupDbKeyFields1.getCost1()).isEqualTo("92");
        assertThat(accountGroupDbKeyFields1.getCost2()).isEqualTo("A2");
        assertThat(accountGroupDbKeyFields1.getRecordType()).isEqualTo(1);
        assertThat(accountGroupDbKeyFields1.getRwShareholderId()).isEqualTo(UUID.fromString("9c18c30b-ac17-451f-81c7-1b9b26dd73fa"));
        assertThat(accountGroupDbKeyFields1.getTaxRate()).isEqualTo(0.18f);
        AccountDbKeyFields accountGroupDbKeyFields2 = accountDbKeyFieldsMapper.accountSumDayToGroupAccountDbKeyFields(accountSumDays.get(0));
        assertThat(accountGroupDbKeyFields2).isNotNull();
        assertThat(accountGroupDbKeyFields2.getAccountNumber()).isEqualTo(1);
    }
}
