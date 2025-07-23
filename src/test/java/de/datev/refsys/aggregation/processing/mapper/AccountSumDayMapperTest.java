package de.datev.refsys.aggregation.processing.mapper;

import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountSumDayMapperTest {

    private final AccountSumDayMapper mapper = new AccountSumDayMapperImpl();

    @Test
    @DisplayName("Fiscal year month after mapping is 5 when fiscal year begin ist 20210101 and date is 20210501")
    void should_map_to_correct_fiscal_year_month_when_default_fiscal_year_start() {
        MovementDataDay movementDataDay = TestDataLoader.loadDBElement("json/collections/mapper/movementDataDaysDefault.json", MovementDataDay.class);
        List<AccountSumDay> accountSumDays = mapper.mapDbToApiModel(movementDataDay);
        assertThat(accountSumDays).isNotNull().hasSize(1);
        AccountSumDay accountSumDay = accountSumDays.get(0);
        AccountSumDay expectedAccountSumDay = TestDataLoader.load("json/acds-responses/expected/account-sum-days-default.json", AccountSumDay.class);
        assertThat(accountSumDay).isEqualTo(expectedAccountSumDay);
    }

    @Test
    @DisplayName("Fiscal year month after mapping is 0 when fiscal year begin ist 20210101 and date is opening balance")
    void should_map_to_correct_fiscal_year_month_when_default_fiscal_year_start_and_date_is_opening_balance() {
        MovementDataDay movementDataDay = TestDataLoader.loadDBElement("json/collections/mapper/movementDataDaysOpeningBalance.json", MovementDataDay.class);
        List<AccountSumDay> accountSumDays = mapper.mapDbToApiModel(movementDataDay);
        assertThat(accountSumDays).isNotNull().hasSize(1);
        AccountSumDay accountSumDay = accountSumDays.get(0);
        AccountSumDay expectedAccountSumDay = TestDataLoader.load("json/acds-responses/expected/account-sum-days-opening-balance.json", AccountSumDay.class);
        assertThat(accountSumDay).isEqualTo(expectedAccountSumDay);
    }

    @Test
    @DisplayName("Fiscal year month after mapping is 3 when fiscal year begin ist 20210301 and date is 20210501")
    void should_map_to_correct_fiscal_year_month_when_fiscal_year_start_is_in_march() {
        MovementDataDay movementDataDay = TestDataLoader.loadDBElement("json/collections/mapper/movementDataDaysFiscalYearStartMarch.json", MovementDataDay.class);
        List<AccountSumDay> accountSumDays = mapper.mapDbToApiModel(movementDataDay);
        assertThat(accountSumDays).isNotNull().hasSize(1);
        AccountSumDay accountSumDay = accountSumDays.get(0);
        AccountSumDay expectedAccountSumDay = TestDataLoader.load("json/acds-responses/expected/account-sum-days-fiscal-year-start-march.json", AccountSumDay.class);
        assertThat(accountSumDay).isEqualTo(expectedAccountSumDay);
    }

    @Test
    @DisplayName("Fiscal year month after mapping is 13 when fiscal year begin ist 20210315 and date is 20220314")
    void should_map_to_correct_fiscal_year_month_when_fiscal_year_start_is_in_march_and_date_is_in_march_next_year() {
        MovementDataDay movementDataDay = TestDataLoader.loadDBElement("json/collections/mapper/movementDataDaysThirteenthMonth.json", MovementDataDay.class);
        List<AccountSumDay> accountSumDays = mapper.mapDbToApiModel(movementDataDay);
        assertThat(accountSumDays).isNotNull().hasSize(1);
        AccountSumDay accountSumDay = accountSumDays.get(0);
        AccountSumDay expectedAccountSumDay = TestDataLoader.load("json/acds-responses/expected/account-sum-days-thirteenth-month.json", AccountSumDay.class);
        assertThat(accountSumDay).isEqualTo(expectedAccountSumDay);
    }

}