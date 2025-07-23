package de.datev.refsys.aggregation.processing.service;

import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.model.DateCorrection;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;

import java.time.format.DateTimeParseException;
import java.util.Objects;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.DAY_PREFIX;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MONTH_PREFIX;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.OPENING_BALANCE;
import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.MOVEMENT_DATA_INVALID_DATE_ERROR;
import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.MOVEMENT_DATA_INVALID_FISCAL_MONTH_ERROR;
import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.MOVEMENT_DATA_INVALID_FISCAL_MONTH_VALUE_ERROR;
import static org.assertj.core.api.Assertions.assertThat;

public class FiscalYearDateConversionStepDefinitions {
    private Integer movementDataDate, movementDataMonth, fiscalYearBegin, fiscalYearEnd;
    private String movementDataDayKey, movementDataMonthKey;
    private AccountSumDay accountSumDay;

    private AggregationProcessingBusinessException dayKeyExceptionResult = null;
    private AggregationProcessingBusinessException monthKeyExceptionResult = null;
    private DateTimeParseException dayDateTimeParseException = null;
    private DateTimeParseException monthDateTimeParseException = null;

    @Angenommen("^folgender Datumssachverhalt$")
    public void folgenderDatumssachverhalt(DataTable inputFiscalYearList) {
        inputFiscalYearList.asMaps(String.class, Integer.class).forEach(fiscalYear -> {
            fiscalYearBegin = fiscalYear.get("WJStart");
            fiscalYearEnd = fiscalYear.get("WJEnde");
        });
    }

    @Wenn("^folgendes Datum in der Kontobewegung enthalten ist$")
    public void folgendesDatumInDerKontobewegungEnthaltenIst(DataTable inputDateMonthList) {
        inputDateMonthList.asMaps(String.class, Integer.class).forEach(dateMonth -> {
            movementDataDate = dateMonth.get("Buchungsdatum");
            movementDataMonth = dateMonth.get("Stapel-Monat");
            accountSumDay = AccountSumDay.builder().date(movementDataDate).month(movementDataMonth).build();
            try {
                DateCorrection dateCorrection = new DateCorrection(accountSumDay, fiscalYearBegin, fiscalYearEnd);
                movementDataDayKey = CommonService.getDayKey(accountSumDay, dateCorrection);
            } catch (AggregationProcessingBusinessException e) {
                dayKeyExceptionResult = e;
            } catch (DateTimeParseException e) {
                dayDateTimeParseException = e;
            }
            try {
                DateCorrection dateCorrection = new DateCorrection(accountSumDay, fiscalYearBegin, fiscalYearEnd);
                movementDataMonthKey = CommonService.getMonthKey(accountSumDay, dateCorrection);
            } catch (AggregationProcessingBusinessException e) {
                monthKeyExceptionResult = e;
            } catch (DateTimeParseException e) {
                monthDateTimeParseException = e;
            }
        });

    }

    @Dann("^muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden$")
    public void mussAusAuswertungssichtFolgendeDatumskorrekturVorgenommenWerden(DataTable expectedDateMonthResultList) {
        expectedDateMonthResultList.asMaps(String.class, String.class).forEach(expectedDateMonth -> {
            String expectedDayKey = Objects.equals(expectedDateMonth.get("Datum"), OPENING_BALANCE) ? OPENING_BALANCE : DAY_PREFIX + expectedDateMonth.get("Datum");
            String expectedMonthKey = Objects.equals(expectedDateMonth.get("Monat"), OPENING_BALANCE) ? OPENING_BALANCE : MONTH_PREFIX + expectedDateMonth.get("Monat");
            assertThat(movementDataDayKey).isEqualTo(expectedDayKey);
            assertThat(movementDataMonthKey).isEqualTo(expectedMonthKey);
        });
    }

    @Dann("Buchung außerhalb des WJ Exception-Meldung")
    public void buchungAußerhalbDesWJExceptionMeldung() {
        assertThat(dayKeyExceptionResult).isNotNull().isInstanceOf(AggregationProcessingBusinessException.class);
        assertThat(monthKeyExceptionResult.getMessage()).isEqualTo(String.format(MOVEMENT_DATA_INVALID_DATE_ERROR, movementDataDate));
        assertThat(dayKeyExceptionResult).isNotNull().isInstanceOf(AggregationProcessingBusinessException.class);
        assertThat(monthKeyExceptionResult.getMessage()).isEqualTo(String.format(MOVEMENT_DATA_INVALID_DATE_ERROR, movementDataDate));
    }

    @Dann("Ungültiges Datumsformat Exception-Meldung")
    public void ungültigesDatumsformatExceptionMeldung() {
        assertThat(dayDateTimeParseException).isNotNull().isInstanceOf(DateTimeParseException.class);
        assertThat(dayDateTimeParseException.getMessage()).contains(String.format("Text '%s' could not be parsed:", movementDataDate));
        assertThat(monthDateTimeParseException).isNotNull().isInstanceOf(DateTimeParseException.class);
        assertThat(monthDateTimeParseException.getMessage()).contains(String.format("Text '%s' could not be parsed:", movementDataDate));
    }

    @Dann("Stapel-Monat außerhalb des WJ Exception-Meldung")
    public void stapelMonatAußerhalbDesWJExceptionMeldung() {
        assertThat(dayKeyExceptionResult).isNotNull().isInstanceOf(AggregationProcessingBusinessException.class);
        assertThat(dayKeyExceptionResult.getMessage()).isEqualTo(String.format(MOVEMENT_DATA_INVALID_FISCAL_MONTH_ERROR, movementDataMonth, accountSumDay));
        assertThat(monthKeyExceptionResult).isNotNull().isInstanceOf(AggregationProcessingBusinessException.class);
        assertThat(monthKeyExceptionResult.getMessage()).isEqualTo(String.format(MOVEMENT_DATA_INVALID_FISCAL_MONTH_ERROR, movementDataMonth, accountSumDay));
    }

    @Dann("Ungültiger Stapel-Monat Exception-Meldung")
    public void ungültigerMonatExceptionMeldung() {
        assertThat(dayKeyExceptionResult).isNotNull().isInstanceOf(AggregationProcessingBusinessException.class);
        assertThat(dayKeyExceptionResult.getMessage()).isEqualTo(String.format(MOVEMENT_DATA_INVALID_FISCAL_MONTH_VALUE_ERROR, movementDataMonth, accountSumDay));
        assertThat(monthKeyExceptionResult).isNotNull().isInstanceOf(AggregationProcessingBusinessException.class);
        assertThat(monthKeyExceptionResult.getMessage()).isEqualTo(String.format(MOVEMENT_DATA_INVALID_FISCAL_MONTH_VALUE_ERROR, movementDataMonth, accountSumDay));
    }
}
