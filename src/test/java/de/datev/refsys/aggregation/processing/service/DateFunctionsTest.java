package de.datev.refsys.aggregation.processing.service;

import de.datev.refsys.aggregation.processing.util.DateFunctions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DateFunctionsTest {

    @Test
    void given_date_with_month_5_should_return_month_8_for_WJM_4() {
        // given
        Integer yearBegin = 20210501;
        Integer monthOfWJ = 4;
        Integer expected = 20210801;

        // when
        Integer actual = DateFunctions.geFirstPossibleAccountingDateForMonth(yearBegin, monthOfWJ);
        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_date_with_month_1_should_return_month_1_for_WJM_1() {
        // given
        Integer yearBegin = 20210101;
        Integer monthOfWJ = 1;
        Integer expected = 20210101;

        // when
        Integer actual = DateFunctions.geFirstPossibleAccountingDateForMonth(yearBegin, monthOfWJ);
        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_date_with_month_5_should_return_month_2_of_next_year_for_WJM_10() {
        // given
        Integer yearBegin = 20210501;
        Integer monthOfWJ = 10;
        Integer expected = 20220201;

        // when
        Integer actual = DateFunctions.geFirstPossibleAccountingDateForMonth(yearBegin, monthOfWJ);
        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_date_with_month_5_and_day13_should_return_month_5_day_13_for_WJM_01() {
        // given
        Integer yearBegin = 20210513;
        Integer monthOfWJ = 1;
        Integer expected = 20210513;

        // when
        Integer actual = DateFunctions.geFirstPossibleAccountingDateForMonth(yearBegin, monthOfWJ);
        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_date_with_month_5_and_day13_should_return_month_6_day_01_for_WJM_02() {
        // given
        Integer yearBegin = 20210513;
        Integer monthOfWJ = 2;
        Integer expected = 20210601;

        // when
        Integer actual = DateFunctions.geFirstPossibleAccountingDateForMonth(yearBegin, monthOfWJ);
        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_date_with_month_5_should_return_month_05_day_01_for_WJM_00() {
        // given
        Integer yearBegin = 20210501;
        Integer monthOfWJ = 0;
        Integer expected = 20210501;

        // when
        Integer actual = DateFunctions.geFirstPossibleAccountingDateForMonth(yearBegin, monthOfWJ);
        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_date_with_month_1_should_return_month_1_next_year_for_WJM_13() {
        // given
        Integer yearBegin = 20210115;
        Integer monthOfWJ = 13;
        Integer expected = 20220101;

        // when
        Integer actual = DateFunctions.geFirstPossibleAccountingDateForMonth(yearBegin, monthOfWJ);
        // then
        assertThat(actual).isEqualTo(expected);
    }

}