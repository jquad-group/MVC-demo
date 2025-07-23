package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.aggregation.document.model.AccountGroupValue;
import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.processing.model.PersonGroupAmountValues;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountValueMapperTest {

    private AccountValueMapper accountValueMapper = new AccountValueMapperImpl();

    @Test
    void should_map_null_to_null_accountValueMapper() {
        assertThat(accountValueMapper.accountSumDayToAccountValue(null)).isNull();
        assertThat(accountValueMapper.accountSumDayToAccountGroupValue(null, null)).isNull();
        assertThat(accountValueMapper.amountValuesToAccountGroupValue(null)).isNull();
    }

    @Test
    void should_map_accountSumDay_to_accountValue() {
        List<AccountSumDay> accountSumDays = TestDataLoader.loadNdJsonList("json/acds-responses/mapper/accountSumDays.ndjson", AccountSumDay.class);
        AccountValue accountSumDaysExpected = TestDataLoader.load("json/collections/expected/mapper/accountSumDayExpected.json", AccountValue.class);
        AccountValue accountValue = accountValueMapper.accountSumDayToAccountValue(accountSumDays.get(0));

        assertThat(accountValue).usingRecursiveComparison().isEqualTo(accountSumDaysExpected);
    }

    @Test
    void shouldMapAccountSumDayToAccountGroupValue() {
        List<AccountSumDay> accountSumDays = TestDataLoader.loadNdJsonList("json/acds-responses/mapper/accountSumDays.ndjson", AccountSumDay.class);

        AccountGroupValue accountSumDaysGroupExpected = TestDataLoader.load("json/collections/expected/mapper/accountSumDayGroupExpected.json", AccountGroupValue.class);

        PersonGroupAmountValues personGroupAmountValues = PersonGroupAmountValues.builder().amountCreditUnusual(1L).amountCreditUsual(1L).amountDebitUnusual(1L).amountDebitUsual(1L).build();
        AccountGroupValue accountValue = accountValueMapper.accountSumDayToAccountGroupValue(accountSumDays.get(0), personGroupAmountValues);

        assertThat(accountValue).usingRecursiveComparison().isEqualTo(accountSumDaysGroupExpected);
    }

    @Test
    void shouldMapGroupValueToAccountGroupValue() {
        AccountGroupValue accountSumDaysGroupExpected = TestDataLoader.load("json/collections/expected/mapper/accountGroupValueExpected.json", AccountGroupValue.class);

        PersonGroupAmountValues personGroupAmountValues = PersonGroupAmountValues.builder().amountCreditUnusual(1L).amountCreditUsual(1L).amountDebitUnusual(1L).amountDebitUsual(1L).build();
        AccountGroupValue accountValue = accountValueMapper.amountValuesToAccountGroupValue(personGroupAmountValues);

        assertThat(accountValue).usingRecursiveComparison().isEqualTo(accountSumDaysGroupExpected);
    }


}
