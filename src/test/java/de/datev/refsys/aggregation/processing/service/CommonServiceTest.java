package de.datev.refsys.aggregation.processing.service;

import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.model.DateCorrection;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;

import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(TEST_PROFILE)
class CommonServiceTest {
    @ParameterizedTest
    @CsvSource({ "20210315, 20220314, 20210321, 1, d20210321",
            "20210315, 20220314, 20210221, 1, d20210315",
            "20210315, 20220314, 20210314, 1, d20210315",
            "20210315, 20220314, 20210314, 2, d20210401",
            "20210315, 20220314, 20210221, 3, d20210501",
            "20210315, 20210614, 20210321, 7, Exception",
            "20210115, 20210614, 20210229, 2, DayException",
            "20210315, 20220314, 20210321, 2, d20210401",
            "20210315, 20220314, 20210502, 2, d20210430",
            "20210315, 20220314, 20210502, 0, EB",
            "20210315, 20220314, 20210502, 3, d20210502",
            "20210315, 20220314, 20220202, 12, d20220202",
            "20210315, 20220314, 20220302, 13, d20220302",
            "20210315, 20220314, 20220321, 13, d20220314",
            "20210315, 20220314, 20220421, 13, d20220314",
            "20210101, 20211231, 20210120, 1, d20210120",
            "20210101, 20211231, 20210120, 2, d20210201",
            "20210101, 20211231, 20210220, 1, d20210131",
            "20210101, 20211231, 20200220, 0, EB" })
    void getDayKey(Integer fiscalYearBegin, Integer fiscalYearEnd, Integer date, Integer month, String expectedResult) {
        AccountSumDay accountSumDay = AccountSumDay.builder().date(date).month(month).build();
        if (expectedResult.equals("DayException")) {
            assertThatThrownBy(() -> new DateCorrection(accountSumDay, fiscalYearBegin, fiscalYearEnd))
                    .isInstanceOf(DateTimeParseException.class);
        } else {
            DateCorrection dateCorrection = new DateCorrection(accountSumDay, fiscalYearBegin, fiscalYearEnd);
            if (expectedResult.equals("Exception")) {
                assertThatThrownBy(() -> CommonService.getDayKey(accountSumDay, dateCorrection))
                        .isInstanceOf(AggregationProcessingBusinessException.class);
            } else {
                String dayKey = CommonService.getDayKey(accountSumDay, dateCorrection);
                assertThat(dayKey).isEqualTo(expectedResult);
            }
        }

    }

    @ParameterizedTest
    @CsvSource({ "20210101, 20211231, 20210114, 1, m1",
            "20210101, 20211231, 20210114, 0, EB" })
    void getMonthKey(Integer fiscalYearBegin, Integer fiscalYearEnd, Integer date, Integer month, String expectedResult) {
        AccountSumDay accountSumDay = AccountSumDay.builder().date(date).month(month).build();
        DateCorrection dateCorrection = new DateCorrection(accountSumDay, fiscalYearBegin, fiscalYearEnd);
        String monthKey = CommonService.getMonthKey(accountSumDay, dateCorrection);

        assertThat(monthKey).isEqualTo(expectedResult);
    }
}