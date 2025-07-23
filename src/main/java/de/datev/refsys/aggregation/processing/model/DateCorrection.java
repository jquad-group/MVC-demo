package de.datev.refsys.aggregation.processing.model;

import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;

import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;

@Getter
@ToString
public class DateCorrection {
    private final LocalDate providedDate;
    private final LocalDate fiscalYearStart;
    private final LocalDate fiscalYearEnd;
    private final LocalDate calculatedCalenderMonth;

    public DateCorrection(AccountSumDay accountSumDay, Integer fiscalYearStart, Integer fiscalYearEnd) {
        this.providedDate = LocalDate.parse(String.valueOf(accountSumDay.getDate()), BASIC_ISO_DATE);
        this.fiscalYearStart = LocalDate.parse(String.valueOf(fiscalYearStart), BASIC_ISO_DATE);
        this.fiscalYearEnd = LocalDate.parse(String.valueOf(fiscalYearEnd), BASIC_ISO_DATE);
        this.calculatedCalenderMonth = this.fiscalYearStart.plusMonths(accountSumDay.getMonth() - 1L);
    }
}
